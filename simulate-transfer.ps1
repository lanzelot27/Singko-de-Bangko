param (
    [long]$Source = 2001,
    [long]$Target = 2999,
    [decimal]$Amount = 500.00,
    [string]$Description = "Sprint Demo Simulation Transfer"
)

$headers = @{ 
    "Authorization" = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"
    "Content-Type"  = "application/json" 
}

$body = @{
    sourceAccountId      = $Source
    destinationAccountId = $Target
    amount               = $Amount
    currency             = "PHP"
    description          = $Description
} | ConvertTo-Json

Write-Host "`n>>> Initiating Transfer Simulation..." -ForegroundColor Cyan
Write-Host "    Source Account : $Source" -ForegroundColor Yellow
Write-Host "    Target Account : $Target" -ForegroundColor Yellow
Write-Host "    Transfer Amount: $Amount PHP" -ForegroundColor Yellow
Write-Host "    Description    : $Description" -ForegroundColor Yellow
Write-Host "--------------------------------------------------------" -ForegroundColor Gray

try {
    $res = Invoke-RestMethod -Uri "http://localhost:8083/transactions/transfer" -Method Post -Headers $headers -Body $body
    Write-Host "[SUCCESS 201 CREATED]" -ForegroundColor Green
    Write-Host "  Transaction ID : $($res.transactionId)" -ForegroundColor White
    Write-Host "  Source Account : $($res.sourceAccountId)" -ForegroundColor White
    Write-Host "  Target Account : $($res.destinationAccountId)" -ForegroundColor White
    Write-Host "  Transferred    : $($res.amount) $($res.currency)" -ForegroundColor White
    Write-Host "  Status         : $($res.status)" -ForegroundColor Green
    Write-Host "  Description    : $($res.description)" -ForegroundColor White
    Write-Host "  Timestamp      : $($res.timestamp)" -ForegroundColor Gray
} catch {
    Write-Host "[TRANSACTION REJECTED]" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $rawError = $reader.ReadToEnd()
        try {
            $errorJson = $rawError | ConvertFrom-Json
            Write-Host "  HTTP Status : $statusCode ($($errorJson.error))" -ForegroundColor Red
            Write-Host "  Reason      : $($errorJson.message)" -ForegroundColor Yellow
            Write-Host "  Path        : $($errorJson.path)" -ForegroundColor Gray
            Write-Host "  Timestamp   : $($errorJson.timestamp)" -ForegroundColor Gray
        } catch {
            Write-Host "  Raw Error   : $rawError" -ForegroundColor Red
        }
    } else {
        Write-Host "  Error       : $($_.Exception.Message)" -ForegroundColor Red
    }
}
Write-Host "--------------------------------------------------------`n" -ForegroundColor Gray
