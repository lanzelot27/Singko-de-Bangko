# Start Account Service on Port 8082
$MavenPath = "..\apache-maven-3.9.9\bin\mvn.cmd"
if (-not (Test-Path $MavenPath)) {
    $MavenPath = "mvn"
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Starting Account Service (Port 8082)..." -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

& $MavenPath spring-boot:run -f .\account-service\pom.xml
