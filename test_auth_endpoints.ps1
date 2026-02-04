# Analytics API Test Script - With Authentication
# Tests all 31 endpoints with proper admin login

$BaseUrl = "http://localhost:9090/api/v1"
$Token = $null

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Analytics API Test (Authenticated)" -ForegroundColor Cyan  
Write-Host "========================================`n" -ForegroundColor Cyan

# Step 1: Login as admin
Write-Host "Step 1: Authenticating as admin..." -ForegroundColor Yellow
try {
    $loginBody = @{
        email    = "admin@befapress.com"
        password = "Admin@123"
    } | ConvertTo-Json
    
    $loginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 30
    $Token = $loginResponse.accessToken
    Write-Host "SUCCESS: Got JWT token (${($Token.Length)} chars)" -ForegroundColor Green
}
catch {
    Write-Host "FAILED: Could not login - $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Continuing with unauthenticated tests..." -ForegroundColor Yellow
}

# Step 2: Test all endpoints
$Headers = @{}
if ($Token) {
    $Headers["Authorization"] = "Bearer $Token"
}

$Endpoints = @(
    # Dashboard & Overview (5)
    @{Num = 1; Path = "/analytics/overview"; Desc = "Dashboard overview" },
    @{Num = 2; Path = "/analytics/metrics/views?period=7d"; Desc = "Page views" },
    @{Num = 3; Path = "/analytics/metrics/session-duration"; Desc = "Session duration" },
    @{Num = 4; Path = "/analytics/metrics/bounce-rate"; Desc = "Bounce rate" },
    @{Num = 5; Path = "/analytics/metrics/subscribers"; Desc = "Subscribers" },
    
    # Traffic & Trends (4)
    @{Num = 6; Path = "/analytics/traffic/daily?from=2026-01-01&to=2026-01-14"; Desc = "Daily traffic" },
    @{Num = 7; Path = "/analytics/traffic/monthly?year=2026"; Desc = "Monthly traffic" },
    @{Num = 8; Path = "/analytics/traffic/sources"; Desc = "Traffic sources" },
    @{Num = 9; Path = "/analytics/traffic/realtime"; Desc = "Realtime" },
    
    # Content Performance (5)
    @{Num = 10; Path = "/analytics/top-articles?limit=5"; Desc = "Top articles" },
    @{Num = 11; Path = "/analytics/top-authors?limit=5"; Desc = "Top authors" },
    @{Num = 12; Path = "/analytics/categories"; Desc = "Categories" },
    @{Num = 13; Path = "/analytics/article/1/stats"; Desc = "Article stats" },
    @{Num = 14; Path = "/analytics/trending"; Desc = "Trending" },
    
    # User Engagement (5)
    @{Num = 15; Path = "/analytics/users/growth?period=30d"; Desc = "User growth" },
    @{Num = 16; Path = "/analytics/users/retention"; Desc = "Retention" },
    @{Num = 17; Path = "/analytics/comments/activity"; Desc = "Comments" },
    @{Num = 18; Path = "/analytics/comments/top-users"; Desc = "Top commenters" },
    @{Num = 19; Path = "/analytics/shares"; Desc = "Shares" },
    
    # Audience (4)
    @{Num = 20; Path = "/analytics/audience/devices"; Desc = "Devices" },
    @{Num = 21; Path = "/analytics/audience/geo"; Desc = "Geographic" },
    @{Num = 22; Path = "/analytics/audience/languages"; Desc = "Languages" },
    @{Num = 23; Path = "/analytics/audience/roles"; Desc = "Roles" },
    
    # Technical & Ads (4)
    @{Num = 24; Path = "/analytics/ads/performance"; Desc = "Ad performance" },
    @{Num = 25; Path = "/analytics/ads/ctr"; Desc = "Ad CTR" },
    @{Num = 26; Path = "/analytics/technical/page-load"; Desc = "Page load" },
    @{Num = 27; Path = "/analytics/technical/errors"; Desc = "Errors" }
)

$PassCount = 0
$FailCount = 0
$Results = @()

Write-Host "`nStep 2: Testing GET endpoints..." -ForegroundColor Yellow

foreach ($ep in $Endpoints) {
    $url = "$BaseUrl$($ep.Path)"
    $status = "FAIL"
    $code = 0
    $latency = 0
    
    try {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $resp = Invoke-WebRequest -Uri $url -Method GET -Headers $Headers -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
        $sw.Stop()
        $latency = $sw.ElapsedMilliseconds
        $code = $resp.StatusCode
        
        if ($code -eq 200) {
            $status = "PASS"
            $PassCount++
            Write-Host "[$($ep.Num.ToString().PadLeft(2))] PASS | $($ep.Desc.PadRight(18)) | ${latency}ms" -ForegroundColor Green
        }
    }
    catch {
        $sw.Stop()
        $latency = $sw.ElapsedMilliseconds
        $FailCount++
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
        }
        Write-Host "[$($ep.Num.ToString().PadLeft(2))] FAIL | $($ep.Desc.PadRight(18)) | $code | ${latency}ms" -ForegroundColor Red
    }
    
    $Results += [PSCustomObject]@{Num = $ep.Num; Path = $ep.Path; Status = $status; Code = $code; Latency = $latency }
}

# Step 3: Test POST endpoints (public)
Write-Host "`nStep 3: Testing POST endpoints (public)..." -ForegroundColor Yellow

$PostEndpoints = @(
    @{Num = 28; Path = "/analytics/track"; Body = '{"entityType":"NEWS","entityId":1}'; Desc = "Track hit" },
    @{Num = 29; Path = "/analytics/track/scroll"; Body = '{"entityType":"NEWS","entityId":1,"scrollDepth":75}'; Desc = "Track scroll" },
    @{Num = 30; Path = "/analytics/track/share"; Body = '{"entityType":"NEWS","entityId":1,"platform":"TWITTER"}'; Desc = "Track share" },
    @{Num = 31; Path = "/analytics/track/session"; Body = '{"sessionId":"test123","duration":120}'; Desc = "Track session" }
)

foreach ($ep in $PostEndpoints) {
    $url = "$BaseUrl$($ep.Path)"
    try {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $resp = Invoke-WebRequest -Uri $url -Method POST -Body $ep.Body -ContentType "application/json" -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
        $sw.Stop()
        $PassCount++
        Write-Host "[$($ep.Num)] PASS | $($ep.Desc.PadRight(18)) | $($sw.ElapsedMilliseconds)ms" -ForegroundColor Green
        $Results += [PSCustomObject]@{Num = $ep.Num; Path = $ep.Path; Status = "PASS"; Code = $resp.StatusCode; Latency = $sw.ElapsedMilliseconds }
    }
    catch {
        $FailCount++
        $code = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        Write-Host "[$($ep.Num)] FAIL | $($ep.Desc.PadRight(18)) | $code" -ForegroundColor Red
        $Results += [PSCustomObject]@{Num = $ep.Num; Path = $ep.Path; Status = "FAIL"; Code = $code; Latency = 0 }
    }
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  RESULTS: $PassCount PASS / $FailCount FAIL / 31 TOTAL" -ForegroundColor $(if ($FailCount -eq 0) { "Green" } else { "Yellow" })
Write-Host "========================================" -ForegroundColor Cyan

# Save results
$Results | Export-Csv -Path "endpoint_test_results.csv" -NoTypeInformation
Write-Host "Results saved to endpoint_test_results.csv"
