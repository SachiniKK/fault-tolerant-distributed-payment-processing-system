Add-Type -AssemblyName System.Net.Http

Write-Host "=============================================" -ForegroundColor Red
Write-Host "       SYSTEM STRESS TEST (BREAK MODE)       " -ForegroundColor White -BackgroundColor DarkRed
Write-Host "=============================================" -ForegroundColor Red
Write-Host ""
Write-Host "Injecting 5,000 completely parallel requests with 0ms delay." -ForegroundColor Yellow
Write-Host "This will exhaust the Spring Boot Tomcat thread pool (Default: 200)." -ForegroundColor Yellow
Write-Host "You should see the UI freeze, errors cascade, and terminals overflow." -ForegroundColor Red
Write-Host ""
Start-Sleep -Seconds 3

# Setup a high-performance .NET HTTP Client to fire all requests simultaneously
[System.Net.ServicePointManager]::DefaultConnectionLimit = 5000
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(5)
$Url = "http://localhost:8090/api/payment"

$Tasks = [System.Collections.Generic.List[System.Threading.Tasks.Task]]::new()

try {
    for ($i=1; $i -le 5000; $i++) {
        $body = '{"amount":' + $i + ',"sourceAccount":"STRESS-ATTACKER","destinationAccount":"SYSTEM"}'
        $content = [System.Net.Http.StringContent]::new($body, [System.Text.Encoding]::UTF8, "application/json")
        
        # Fire asynchronously WITHOUT waiting for an answer
        $Tasks.Add($client.PostAsync($Url, $content))

        if ($i % 1000 -eq 0) {
            Write-Host "Launched $i concurrent requests..." -ForegroundColor DarkYellow
        }
    }

    Write-Host ""
    Write-Host "All 5,000 requests are entirely in-flight right now!" -ForegroundColor Red
    Write-Host "Waiting to see when the network drops or server timeouts happen..." -ForegroundColor Cyan

    # Wait for the chaos to resolve or crash
    [System.Threading.Tasks.Task]::WaitAll($Tasks.ToArray())
    
    Write-Host ""
    Write-Host "Somehow the server survived without dropping packets." -ForegroundColor Green
} catch {
    Write-Host ""
    Write-Host "💥 BREAKING POINT REACHED! 💥" -ForegroundColor White -BackgroundColor DarkRed
    Write-Host "The Gateway was physically overwhelmed by the traffic spike." -ForegroundColor Red
    Write-Host "Exception details below:" -ForegroundColor Red
    Write-Host $_.Exception.InnerException.Message -ForegroundColor Yellow
}
