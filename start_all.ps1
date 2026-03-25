Write-Host "Starting FULL Distributed System..." -ForegroundColor Green

# Write-Host "Starting ZooKeeper..." -ForegroundColor Cyan
# docker-compose up -d
# Start-Sleep -Seconds 5

Write-Host "Starting Consensus Cluster..." -ForegroundColor Cyan
.\start_consensus.ps1
Start-Sleep -Seconds 5

Write-Host "Starting Fault Tolerance Cluster..." -ForegroundColor Cyan
.\start-fault-tolerance.ps1
Start-Sleep -Seconds 5

Write-Host "Starting Replication Cluster..." -ForegroundColor Cyan
.\start-replication.ps1
Start-Sleep -Seconds 5

Write-Host "Starting Time Sync Cluster..." -ForegroundColor Cyan
.\start-time-sync.ps1

Write-Host ""
Write-Host "All clusters started successfully!" -ForegroundColor Green

Write-Host "Starting Payment Gateway..." -ForegroundColor Cyan
cd payment-gateway
mvn spring-boot:run
