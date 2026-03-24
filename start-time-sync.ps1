Write-Host "Starting Time-Sync Cluster..." -ForegroundColor Green
Write-Host ""

$timeDir = Join-Path $PSScriptRoot "time-sync"

# Node 1
Write-Host "Starting Time-Sync Node 1 on port 8084..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$timeDir'; mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8084 --app.node.id=node1'"

Start-Sleep -Seconds 3

# Node 2
Write-Host "Starting Time-Sync Node 2 on port 8085..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$timeDir'; mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8085 --app.node.id=node2'"

Start-Sleep -Seconds 3

# Node 3
Write-Host "Starting Time-Sync Node 3 on port 8086..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$timeDir'; mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8086 --app.node.id=node3'"

Write-Host ""
Write-Host "Time-Sync cluster started!" -ForegroundColor Green
