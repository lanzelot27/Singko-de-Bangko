# Start Transaction Service on Port 8083
$MavenPath = "..\apache-maven-3.9.9\bin\mvn.cmd"
if (-not (Test-Path $MavenPath)) {
    $MavenPath = "mvn"
}

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " Starting Transaction Service (Port 8083)..." -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

& $MavenPath spring-boot:run -f .\transaction-service\pom.xml
