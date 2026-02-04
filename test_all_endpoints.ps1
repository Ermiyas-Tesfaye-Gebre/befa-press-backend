# Analytics API Test Script - All 31 Endpoints
# Run from: backend folder

$BaseUrl = "http://localhost:9090/api/v1/analytics"
$Results = @()

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Testing All 31 Analytics Endpoints" -ForegroundColor Cyan  
Write-Host "========================================`n" -ForegroundColor Cyan

# Define all endpoints
$Endpoints = @(
    # Dashboard & Overview (5)
    @{Num=1;  Method="GET";  Path="/overview"; Desc="Dashboard overview"},
    @{Num=2;  Method="GET";  Path="/metrics/views?period=7d"; Desc="Page views metric"},
    @{Num=3;  Method="GET";  Path="/metrics/session-duration"; Desc="Session duration"},
    @{Num=4;  Method="GET";  Path="/metrics/bounce-rate"; Desc="Bounce rate"},
    @{Num=5;  Method="GET";  Path="/metrics/subscribers"; Desc="Subscribers"},
    
    # Traffic & Trends (4)
    @{Num=6;  Method="GET";  Path="/traffic/daily?from=2026-01-01&to=2026-01-14"; Desc="Daily traffic"},
    @{Num=7;  Method="GET";  Path="/traffic/monthly?year=2026"; Desc="Monthly traffic"},
    @{Num=8;  Method="GET";  Path="/traffic/sources"; Desc="Traffic sources"},
    @{Num=9;  Method="GET";  Path="/traffic/realtime"; Desc="Realtime users"},
    
    # Content Performance (5)
    @{Num=10; Method="GET";  Path="/top-articles?limit=5"; Desc="Top articles"},
    @{Num=11; Method="GET";  Path="/top-authors?limit=5"; Desc="Top authors"},
    @{Num=12; Method="GET";  Path="/categories"; Desc="Category stats"},
    @{Num=13; Method="GET";  Path="/article/1/stats"; Desc="Article stats"},
    @{Num=14; Method="GET";  Path="/trending"; Desc="Trending"},
    
    # User Engagement (5)
    @{Num=15; Method="GET";  Path="/users/growth?period=30d"; Desc="User growth"},
    @{Num=16; Method="GET";  Path="/users/retention"; Desc="User retention"},
    @{Num=17; Method="GET";  Path="/comments/activity"; Desc="Comment activity"},
    @{Num=18; Method="GET";  Path="/comments/top-users"; Desc="Top commenters"},
    @{Num=19; Method="GET";  Path="/shares"; Desc="Share stats"},
    
    # Audience Demographics (4)
    @{Num=20; Method="GET";  Path="/audience/devices"; Desc="Device breakdown"},
    @{Num=21; Method="GET";  Path="/audience/geo"; Desc="Geographic data"},
    @{Num=22; Method="GET";  Path="/audience/languages"; Desc="Language stats"},
    @{Num=23; Method="GET";  Path="/audience/roles"; Desc="User roles"},
    
    # Technical & Ads (4)
    @{Num=24; Method="GET";  Path="/ads/performance"; Desc="Ad performance"},
    @{Num=25; Method="GET";  Path="/ads/ctr"; Desc="Ad CTR"},
    @{Num=26; Method="GET";  Path="/technical/page-load"; Desc="Page load time"},
    @{Num=27; Method="GET";  Path="/technical/errors"; Desc="Error tracking"},
    
    # Data Collection - Public (4)
    @{Num=28; Method="POST"; Path="/track"; Desc="Track page hit"; Body='{"entityType":"NEWS","entityId":1}'},
    @{Num=29; Method="POST"; Path="/track/scroll"; Desc="Track scroll"; Body='{"entityType":"NEWS","entityId":1,"scrollDepth":75}'},
    @{Num=30; Method="POST"; Path="/track/share"; Desc="Track share"; Body='{"entityType":"NEWS","entityId":1,"platform":"TWITTER"}'},
    @{Num=31; Method="POST"; Path="/track/session"; Desc="Track session"; Body='{"sessionId":"test-123","duration":120}'}
)

$PassCount = 0
$FailCount = 0

foreach ($ep in $Endpoints) {
    $url = "$BaseUrl$($ep.Path)"
    $status = "FAIL"
    $statusCode = 0
    $latency = 0
    
    try {
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        
        if ($ep.Method -eq "POST") {
            $response = Invoke-WebRequest -Uri $url -Method POST -Body $ep.Body -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
        } else {
            $response = Invoke-WebRequest -Uri $url -Method GET -UseBasicParsing -ErrorAction Stop
        }
        
        $stopwatch.Stop()
        $latency = $stopwatch.ElapsedMilliseconds
        $statusCode = $response.StatusCode
        
        if ($statusCode -eq 200) {
            $status = "PASS"
            $PassCount++
            Write-Host "[$($ep.Num.ToString().PadLeft(2))] PASS " -ForegroundColor Green -NoNewline
        } else {
            $FailCount++
            Write-Host "[$($ep.Num.ToString().PadLeft(2))] FAIL " -ForegroundColor Red -NoNewline
        }
    }
    catch {
        $stopwatch.Stop()
        $latency = $stopwatch.ElapsedMilliseconds
        $FailCount++
        
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        } else {
            $statusCode = 0
        }
        
        Write-Host "[$($ep.Num.ToString().PadLeft(2))] FAIL " -ForegroundColor Red -NoNewline
    }
    
    Write-Host "| $($ep.Method.PadRight(4)) | $($ep.Desc.PadRight(20)) | $($statusCode.ToString().PadLeft(3)) | ${latency}ms"
    
    $Results += [PSCustomObject]@{
        Num = $ep.Num
        Method = $ep.Method
        Path = $ep.Path
        Description = $ep.Desc
        Status = $status
        StatusCode = $statusCode
        Latency = $latency
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Results: $PassCount PASS / $FailCount FAIL" -ForegroundColor $(if ($FailCount -eq 0) {"Green"} else {"Yellow"})
Write-Host "========================================`n" -ForegroundColor Cyan

# Export results to file
$Results | Format-Table -AutoSize | Out-String | Out-File -FilePath "api_test_results.txt" -Encoding UTF8

# Show failed endpoints
if ($FailCount -gt 0) {
    Write-Host "Failed Endpoints:" -ForegroundColor Yellow
    $Results | Where-Object { $_.Status -eq "FAIL" } | ForEach-Object {
        Write-Host "  - [$($_.Num)] $($_.Path) ($($_.StatusCode))" -ForegroundColor Red
    }
}
