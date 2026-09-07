# End-to-End API Test Suite for Singko de Bangko (Official Sprint Contract)
$AuthHeader = @{ 
    "Authorization" = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"
    "Content-Type"  = "application/json" 
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "   SINGKO DE BANGKO - MICROSERVICES E2E VERIFICATION   " -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan

# 1. Test Security - Missing Authorization Header (Expect 401)
Write-Host "[TEST 1] Missing Authorization Header (Expect 401 Unauthorized)..." -NoNewline
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8082/accounts/2001/balance" -Method Get -ErrorAction Stop
    Write-Host " FAILED (Expected 401, got 200)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host " PASSED (HTTP 401 Received)" -ForegroundColor Green
    } else {
        Write-Host " FAILED ($($_.Exception.Message))" -ForegroundColor Red
    }
}

# 2. Test Account Balance (Expect 200)
Write-Host "[TEST 2] GET /accounts/2001/balance..." -NoNewline
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8082/accounts/2001/balance" -Method Get -Headers $AuthHeader
    Write-Host " PASSED" -ForegroundColor Green
    Write-Host "         Acc No: $($res.accountNumber) | User: $($res.userId) | Balance: $($res.balance) $($res.currency) | Status: $($res.status)" -ForegroundColor Gray
} catch {
    Write-Host " FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Test Account Profile (Expect 200)
Write-Host "[TEST 3] GET /accounts/2001/profile..." -NoNewline
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8082/accounts/2001/profile" -Method Get -Headers $AuthHeader
    Write-Host " PASSED" -ForegroundColor Green
    Write-Host "         Owner: $($res.ownerName) | Email: $($res.email) | Type: $($res.accountType)" -ForegroundColor Gray
} catch {
    Write-Host " FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Test Internal Balance Adjustment (PUT /accounts/2001/adjust-balance)
Write-Host "[TEST 4] PUT /accounts/2001/adjust-balance (Debit 100 PHP)..." -NoNewline
try {
    $body = @{
        transactionReference = "TXN-TEST-001"
        amount = -100.00
        type = "DEBIT"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8082/accounts/2001/adjust-balance" -Method Put -Headers $AuthHeader -Body $body
    Write-Host " PASSED" -ForegroundColor Green
    Write-Host "         Account: $($res.accountId) | New Balance: $($res.newBalance)" -ForegroundColor Gray
} catch {
    Write-Host " FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 5. Test Fund Transfer via Transaction Service (Orchestrates Feign call to Account Service)
Write-Host "[TEST 5] POST /transactions/transfer (2001 -> 2002, 500 PHP)..." -NoNewline
try {
    $body = @{
        sourceAccountId = 2001
        destinationAccountId = 2002
        amount = 500.00
        currency = "PHP"
        description = "Sprint Demo Transfer"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8083/transactions/transfer" -Method Post -Headers $AuthHeader -Body $body
    Write-Host " PASSED (HTTP 201 Created)" -ForegroundColor Green
    Write-Host "         Txn ID: $($res.transactionId) | Status: $($res.status) | Amount: $($res.amount) $($res.currency)" -ForegroundColor Gray
} catch {
    Write-Host " FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 6. Test Transaction History
Write-Host "[TEST 6] GET /transactions/account/2001..." -NoNewline
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8083/transactions/account/2001" -Method Get -Headers $AuthHeader
    Write-Host " PASSED" -ForegroundColor Green
    Write-Host "         Retrieved $($res.Count) transaction(s) for Account 2001" -ForegroundColor Gray
} catch {
    Write-Host " FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 7. Test Insufficient Balance Handling (Expect 400 Bad Request)
Write-Host "[TEST 7] Transfer with Insufficient Balance (Expect 400)..." -NoNewline
try {
    $body = @{
        sourceAccountId = 2003 # Initial balance is only 500 PHP
        destinationAccountId = 2002
        amount = 999999.00
        currency = "PHP"
        description = "Overdraft attempt"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8083/transactions/transfer" -Method Post -Headers $AuthHeader -Body $body -ErrorAction Stop
    Write-Host " FAILED (Expected 400, got 200)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Write-Host " PASSED (HTTP 400 Received)" -ForegroundColor Green
    } else {
        Write-Host " FAILED ($($_.Exception.Message))" -ForegroundColor Red
    }
}

# 8. Test Non-Existent Account (Expect 404 Not Found)
Write-Host "[TEST 8] Transfer to Non-Existent Account 2999 (Expect 404)..." -NoNewline
try {
    $body = @{
        sourceAccountId = 2001
        destinationAccountId = 2999
        amount = 100.00
        currency = "PHP"
        description = "Invalid destination"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8083/transactions/transfer" -Method Post -Headers $AuthHeader -Body $body -ErrorAction Stop
    Write-Host " FAILED (Expected 404, got 200)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host " PASSED (HTTP 404 Received)" -ForegroundColor Green
    } else {
        Write-Host " FAILED ($($_.Exception.Message))" -ForegroundColor Red
    }
}

# 9. Test Zero/Negative Amount (Expect 400 Bad Request)
Write-Host "[TEST 9] Transfer with 0.00 Amount (Expect 400)..." -NoNewline
try {
    $body = @{
        sourceAccountId = 2001
        destinationAccountId = 2002
        amount = 0.00
        currency = "PHP"
        description = "Zero amount test"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8083/transactions/transfer" -Method Post -Headers $AuthHeader -Body $body -ErrorAction Stop
    Write-Host " FAILED (Expected 400, got 200)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Write-Host " PASSED (HTTP 400 Received)" -ForegroundColor Green
    } else {
        Write-Host " FAILED ($($_.Exception.Message))" -ForegroundColor Red
    }
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "                 TEST SUITE COMPLETED                   " -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan
