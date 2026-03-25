Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  DISTRIBUTED SYSTEM LOAD GENERATOR  " -ForegroundColor White -BackgroundColor DarkCyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Injecting 100 random payment processing requests..." -ForegroundColor Yellow
Write-Host "Switch to your Integrated Dashboard UI immediately to watch the feed!" -ForegroundColor Green
Write-Host ""

# Tight loop to simulate high traffic load against the Gateway
for ($i=1; $i -le 100; $i++) {
    $amount = Get-Random -Minimum 15 -Maximum 9999
    $sourceVal = Get-Random -Minimum 10000 -Maximum 99999
    
    $body = @{
        amount = $amount
        sourceAccount = "USER-ACC-$sourceVal"
        destinationAccount = "MERCHANT-STORE"
    } | ConvertTo-Json

    try {
        # Fast REST call
        $response = Invoke-RestMethod -Uri "http://localhost:8090/api/payment" -Method Post -Body $body -ContentType "application/json"
        
        if ($response.status -eq "SUCCESS") {
            Write-Host "[SUCCESS] Payment $i ($$amount) processed by Raft Term $($response.raftTerm) and replicated." -ForegroundColor Green
        } else {
            Write-Host "[WARNING] Payment $i ($$amount) returned status: $($response.status)" -ForegroundColor Yellow
        }
        
    } catch {
        Write-Host "[ERROR] Gateway rejected Payment $i. Is it running?" -ForegroundColor Red
    }
    
    # Introduce a tiny random micro-delay to simulate organic user traffic (0.1 to 0.4 seconds)
    $delay = Get-Random -Minimum 100 -Maximum 400
    Start-Sleep -Milliseconds $delay
}

Write-Host ""
Write-Host "Load test complete!" -ForegroundColor Cyan
