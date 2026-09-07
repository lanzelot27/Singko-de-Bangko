# Singko de Bangko - Official Sprint Test Cases Verification
# Verifies TC-ACC-01, TC-ACC-02, TC-ACC-03, TC-TXN-01, TC-TXN-02, TC-TXN-03

$SecretHex = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"

function New-SingkoJwt {
    param (
        [string]$UserId = "1001",
        [string]$Email = "juan.delacruz@neobank.com"
    )
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $exp = $now + 3600
    $headerJson = '{"alg":"HS256","typ":"JWT"}'
    $payloadJson = "{""sub"":""$UserId"",""email"":""$Email"",""iat"":$now,""exp"":$exp}"
    
    $encHeader = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($headerJson)).TrimEnd('=').Replace('+','-').Replace('/','_')
    $encPayload = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($payloadJson)).TrimEnd('=').Replace('+','-').Replace('/','_')
    $signingInput = "$encHeader.$encPayload"
    
    $keyBytes = [byte[]]::new($SecretHex.Length / 2)
    for ($i = 0; $i -lt $SecretHex.Length; $i += 2) {
        $keyBytes[$i / 2] = [Convert]::ToByte($SecretHex.Substring($i, 2), 16)
    }
    
    $hmac = [System.Security.Cryptography.HMACSHA256]::new($keyBytes)
    $sigBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($signingInput))
    $encSig = [Convert]::ToBase64String($sigBytes).TrimEnd('=').Replace('+','-').Replace('/','_')
    
    return "$signingInput.$encSig"
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "   SINGKO DE BANGKO - TEST CASES (TC-ACC & TC-TXN)     " -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan

# -------------------------------------------------------------
# TC-ACC-01: Authenticated Balance Inquiry (Technical)
# -------------------------------------------------------------
Write-Host "[TC-ACC-01] Authenticated Balance Inquiry (Technical)..." -NoNewline
$juanToken = New-SingkoJwt -UserId "1001" -Email "juan.delacruz@neobank.com"
$headersJuan = @{ "Authorization" = "Bearer $juanToken"; "Content-Type" = "application/json" }

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8082/accounts/2001/balance" -Method Get -Headers $headersJuan
    Write-Host " PASSED (HTTP 200 OK)" -ForegroundColor Green
    Write-Host "            Account: $($res.accountId) | Holder User ID: $($res.userId) | Balance: $($res.balance) $($res.currency)" -ForegroundColor Gray
} catch {
    Write-Host " FAILED ($($_.Exception.Message))" -ForegroundColor Red
}

# -------------------------------------------------------------
# TC-ACC-02: Identity Context from JWT Claims (Code Quality)
# -------------------------------------------------------------
Write-Host "[TC-ACC-02] Identity Context from JWT Claims (Code Quality)..." -NoNewline
$mariaToken = New-SingkoJwt -UserId "1002" -Email "maria.santos@neobank.com"
$headersMaria = @{ "Authorization" = "Bearer $mariaToken"; "Content-Type" = "application/json" }

try {
    # Maria (User 1002) attempting to query Juan's account 2001
    $res = Invoke-RestMethod -Uri "http://localhost:8082/accounts/2001/balance" -Method Get -Headers $headersMaria -ErrorAction Stop
    Write-Host " FAILED (Expected 403 Forbidden, got 200 OK)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 403) {
        Write-Host " PASSED (HTTP 403 Forbidden correctly rejected unauthorized user)" -ForegroundColor Green
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $err = $reader.ReadToEnd() | ConvertFrom-Json
        Write-Host "            Reason: $($err.message)" -ForegroundColor Gray
    } else {
        Write-Host " FAILED ($($_.Exception.Message))" -ForegroundColor Red
    }
}

# -------------------------------------------------------------
# TC-ACC-03: Tampered Signature Rejected (Security)
# -------------------------------------------------------------
Write-Host "[TC-ACC-03] Tampered Signature Rejected (Security)..." -NoNewline
# Tamper signature by replacing last 4 characters
$tamperedToken = $juanToken.Substring(0, $juanToken.Length - 4) + "X9Z1"
$headersTampered = @{ "Authorization" = "Bearer $tamperedToken"; "Content-Type" = "application/json" }

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8082/accounts/2001/balance" -Method Get -Headers $headersTampered -ErrorAction Stop
    Write-Host " FAILED (Expected 401 Unauthorized, got 200 OK)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host " PASSED (HTTP 401 Unauthorized correctly rejected tampered signature)" -ForegroundColor Green
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $err = $reader.ReadToEnd() | ConvertFrom-Json
        Write-Host "            Reason: $($err.message)" -ForegroundColor Gray
    } else {
        Write-Host " FAILED ($($_.Exception.Message))" -ForegroundColor Red
    }
}

# -------------------------------------------------------------
# TC-TXN-01: Peer-to-Peer Fund Transfer (Technical)
# -------------------------------------------------------------
Write-Host "[TC-TXN-01] Peer-to-Peer Fund Transfer (Technical)..." -NoNewline
$transferBody = @{
    sourceAccountId      = 2001
    destinationAccountId = 2002
    amount               = 100.00
    currency             = "PHP"
    description          = "P2P Lunch Payment"
} | ConvertTo-Json

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8083/transactions/transfer" -Method Post -Headers $headersJuan -Body $transferBody
    Write-Host " PASSED (HTTP 201 Created)" -ForegroundColor Green
    Write-Host "            Txn ID: $($res.transactionId) | From: $($res.sourceAccountId) -> To: $($res.destinationAccountId) | Status: $($res.status) | Amount: $($res.amount) $($res.currency)" -ForegroundColor Gray
} catch {
    Write-Host " FAILED ($($_.Exception.Message))" -ForegroundColor Red
}

# -------------------------------------------------------------
# TC-TXN-02: Insufficient Funds Validation (Problem Solving)
# -------------------------------------------------------------
Write-Host "[TC-TXN-02] Insufficient Funds Validation (Problem Solving)..." -NoNewline
$overdraftBody = @{
    sourceAccountId      = 2001 # Balance is approx 15000 PHP
    destinationAccountId = 2002
    amount               = 999999.00
    currency             = "PHP"
    description          = "Overdraft attempt"
} | ConvertTo-Json

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8083/transactions/transfer" -Method Post -Headers $headersJuan -Body $overdraftBody -ErrorAction Stop
    Write-Host " FAILED (Expected 400 Bad Request, got 200 OK)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Write-Host " PASSED (HTTP 400 Bad Request correctly prevented overdraft)" -ForegroundColor Green
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $err = $reader.ReadToEnd() | ConvertFrom-Json
        Write-Host "            Reason: $($err.message)" -ForegroundColor Gray
    } else {
        Write-Host " FAILED ($($_.Exception.Message))" -ForegroundColor Red
    }
}

# -------------------------------------------------------------
# TC-TXN-03: Invalid Target Account Rejection (Code Quality)
# -------------------------------------------------------------
Write-Host "[TC-TXN-03] Invalid Target Account Rejection (Code Quality)..." -NoNewline
$invalidTargetBody = @{
    sourceAccountId      = 2001
    destinationAccountId = 2999 # Does not exist
    amount               = 50.00
    currency             = "PHP"
    description          = "Invalid target test"
} | ConvertTo-Json

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8083/transactions/transfer" -Method Post -Headers $headersJuan -Body $invalidTargetBody -ErrorAction Stop
    Write-Host " FAILED (Expected 404 Not Found, got 200 OK)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host " PASSED (HTTP 404 Not Found correctly rejected missing target)" -ForegroundColor Green
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $err = $reader.ReadToEnd() | ConvertFrom-Json
        Write-Host "            Reason: $($err.message)" -ForegroundColor Gray
    } else {
        Write-Host " FAILED ($($_.Exception.Message))" -ForegroundColor Red
    }
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "              ALL 6 TEST CASES VERIFIED                 " -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan
