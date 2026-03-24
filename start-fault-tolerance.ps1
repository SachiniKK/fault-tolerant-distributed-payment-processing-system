Write-Host "Starting Fault-Tolerance Cluster..." -ForegroundColor Green
Write-Host ""

$faultDir = Join-Path $PSScriptRoot "fault-tolerance"

# Node 1
Write-Host "Starting FT Node 1 on port 8081..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$faultDir'; mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8081 --app.node-id=ft-node1'"

Start-Sleep -Seconds 3

# Node 2
Write-Host "Starting FT Node 2 on port 8082..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$faultDir'; mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8082 --app.node-id=ft-node2'"

Start-Sleep -Seconds 3

# Node 3
Write-Host "Starting FT Node 3 on port 8083..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$faultDir'; mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8083 --app.node-id=ft-node3'"

Write-Host ""
Write-Host "Fault-Tolerance cluster started!" -ForegroundColor Green