$ErrorActionPreference = "SilentlyContinue"
$BaseUrl = "http://localhost:9090/api/v1"
$AnalyticsBase = "$BaseUrl/analytics"
$AdminEmail = "admin@befapress.com"
$AdminPassword = "Admin@123"

function Log-Result {
    param($Name, $Method, $Url, $StatusCode, $Latency, $Result)
    $color = if ($Result -eq "PASS") { "Green" } else { "Red" }
    Write-Host "$Result | $Method $Url | Status: $StatusCode | Latency: $($Latency)ms" -ForegroundColor $color
    if ($Result -eq "FAIL") {
        Write-Host "    -> Failed check for $Name" -ForegroundColor DarkGray
    }
}

function Get-AuthToken {
    try {
        $body = @{ email = $AdminEmail; password = $AdminPassword } | ConvertTo-Json
        $response = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Post -Body $body -ContentType "application/json"
        return $response.token
    } catch {
        Write-Host "Login Failed: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

function Test-Endpoint {
    param($Name, $Method, $Url, $Token, $Body=$null)
    
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
            ContentType = "application/json"
            ErrorAction = "Stop"
        }
        if ($Body) { $params.Body = ($Body | ConvertTo-Json) }
        
        $response = Invoke-RestMethod @params
        $sw.Stop()
        Log-Result -Name $Name -Method $Method -Url $Url -StatusCode 200 -Latency $sw.ElapsedMilliseconds -Result "PASS"
        return $true
    } catch {
        $sw.Stop()
        $status = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
        Log-Result -Name $Name -Method $Method -Url $Url -StatusCode $status -Latency $sw.ElapsedMilliseconds -Result "FAIL"
        return $false
    }
}

Write-Host "`n--- Starting API Performance & Security Test ---`n"

# 1. Login
$token = Get-AuthToken
if (-not $token) { Write-Host "Proceeding without token..." -ForegroundColor Yellow }

# 2. Test Endpoints
$endpoints = @(
    # Dashboard
    @{ Name="Overview"; Method="GET"; Url="$AnalyticsBase/overview" },
    @{ Name="Metric Views"; Method="GET"; Url="$AnalyticsBase/metrics/views" },
    @{ Name="Metric Duration"; Method="GET"; Url="$AnalyticsBase/metrics/session-duration" },
    
    # Traffic
    @{ Name="Daily Traffic"; Method="GET"; Url="$AnalyticsBase/traffic/daily" },
    @{ Name="Realtime Users"; Method="GET"; Url="$AnalyticsBase/traffic/realtime" },
    
    # Content
    @{ Name="Top Articles"; Method="GET"; Url="$AnalyticsBase/top-articles" },
    @{ Name="Top Authors"; Method="GET"; Url="$AnalyticsBase/top-authors" },
    
    # Engagement
    @{ Name="User Growth"; Method="GET"; Url="$AnalyticsBase/users/growth" },
    @{ Name="Shares"; Method="GET"; Url="$AnalyticsBase/shares" },
    
    # Audience
    @{ Name="Device Breakdown"; Method="GET"; Url="$AnalyticsBase/audience/devices" },
    @{ Name="Geo Distribution"; Method="GET"; Url="$AnalyticsBase/audience/geo" }
)

foreach ($ep in $endpoints) {
    Test-Endpoint -Name $ep.Name -Method $ep.Method -Url $ep.Url -Token $token
}

# 3. Test Tracking (POST)
$trackBody = @{ entityType="HOME"; entityId=0; sessionId="test-ps-session"; language="en" }
Test-Endpoint -Name "Track Page Hit" -Method "POST" -Url "$AnalyticsBase/track" -Token $token -Body $trackBody

# 4. Security Check (Unauthenticated)
Write-Host "`n--- Security Check (Unauthenticated Access) ---`n"
try {
    Invoke-RestMethod -Uri "$AnalyticsBase/overview" -Method Get -ErrorAction Stop
    Write-Host "WARN: Unauthenticated access ALLOWED (200 OK)" -ForegroundColor Yellow
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 403 -or $code -eq 401) {
        Write-Host "PASS: Unauthenticated access BLOCKED ($code)" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Unexpected status $code" -ForegroundColor Red
    }
}

# 5. Public Tracking Security Check
Write-Host "`n--- Public Tracking Security Check ---`n"
try {
    # Try tracking without token
    Invoke-RestMethod -Uri "$AnalyticsBase/track" -Method Post -Body ($trackBody | ConvertTo-Json) -ContentType "application/json" -ErrorAction Stop
    Write-Host "PASS: Public tracking ALLOWED (200 OK)" -ForegroundColor Green
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 403 -or $code -eq 401) {
        Write-Host "FAIL: Public tracking BLOCKED ($code) - Should be allowed!" -ForegroundColor Red
    } else {
        Write-Host "INFO: Tracking returned $code" -ForegroundColor Yellow
    }
}
