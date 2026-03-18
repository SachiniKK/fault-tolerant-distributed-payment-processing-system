Write-Host "Starting 3 payment nodes..." -ForegroundColor Cyan
 
if (!(Test-Path "logs")) { New-Item -ItemType Directory -Path "logs" }
 
cd fault-tolerance
Write-Host "Building project..." -ForegroundColor Gray
mvn package -DskipTests -q
 
Write-Host "Starting Node 1 (8081)..."
Start-Process java -ArgumentList "-jar target/fault-tolerance-1.0-SNAPSHOT.jar --spring.profiles.active=node1" -RedirectStandardOutput "../logs/node1.log" -WindowStyle Minimized
 
Start-Sleep -s 2
 
Write-Host "Starting Node 2 (8082)..."
Start-Process java -ArgumentList "-jar target/fault-tolerance-1.0-SNAPSHOT.jar --spring.profiles.active=node2" -RedirectStandardOutput "../logs/node2.log" -WindowStyle Minimized
 
Start-Sleep -s 2
 
Write-Host "Starting Node 3 (8083)..."
Start-Process java -ArgumentList "-jar target/fault-tolerance-1.0-SNAPSHOT.jar --spring.profiles.active=node3" -RedirectStandardOutput "../logs/node3.log" -WindowStyle Minimized
 
Write-Host "Waiting for nodes to stabilize..." -ForegroundColor Yellow
Start-Sleep -s 15
 
Write-Host "`n--- Cluster Status ---" -ForegroundColor Green
try {
    Invoke-RestMethod -Uri "http://localhost:8081/fault/status" | ConvertTo-Json
} catch {
    Write-Host "Node 1 is still starting or unreachable." -ForegroundColor Red
}
 
Write-Host "`nAll nodes are running in the background. Check ./logs for details."
