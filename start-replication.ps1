Write-Host "Starting Replication Cluster..." -ForegroundColor Green
Write-Host ""

$repDir = Join-Path $PSScriptRoot "replication"

# Node 1
Write-Host "Starting Replication Node 1 on port 6001..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$repDir'; mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=6001'"

Start-Sleep -Seconds 3

# Node 2
Write-Host "Starting Replication Node 2 on port 6002..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$repDir'; mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=6002'"

Start-Sleep -Seconds 3

# Node 3
Write-Host "Starting Replication Node 3 on port 6003..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$repDir'; mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=6003'"

Write-Host ""
Write-Host "Replication cluster started!" -ForegroundColor Green