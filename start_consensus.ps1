# Consensus Module - Start All Nodes
# Run this script from the project root directory

Write-Host "Starting Raft Consensus Cluster..." -ForegroundColor Green
Write-Host ""

# Get the script directory
$consensusDir = Join-Path $PSScriptRoot "consensus"

# Start Node 1
Write-Host "Starting Node 1 on port 8071..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$consensusDir'; mvn spring-boot:run '-Dspring-boot.run.profiles=node1'"

Start-Sleep -Seconds 3

# Start Node 2
Write-Host "Starting Node 2 on port 8072..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$consensusDir'; mvn spring-boot:run '-Dspring-boot.run.profiles=node2'"

Start-Sleep -Seconds 3

# Start Node 3
Write-Host "Starting Node 3 on port 8073..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$consensusDir'; mvn spring-boot:run '-Dspring-boot.run.profiles=node3'"

Write-Host ""
Write-Host "All nodes starting! Wait 10-15 seconds for leader election." -ForegroundColor Green
Write-Host ""
Write-Host "Check status with:" -ForegroundColor Cyan
Write-Host "  curl http://localhost:8071/raft/status"
Write-Host "  curl http://localhost:8072/raft/status"
Write-Host "  curl http://localhost:8073/raft/status"
Write-Host ""
Write-Host "Submit a payment with:" -ForegroundColor Cyan
Write-Host '  curl -X POST http://localhost:8071/raft/submit -H "Content-Type: application/json" -d "{\"command\":\"PAYMENT:user1:100:USD\"}"'
