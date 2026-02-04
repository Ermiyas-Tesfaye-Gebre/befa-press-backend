# verify_dashboard.ps1

$baseUrl = "http://localhost:9090/api/v1"
$adminEmail = "admin@befapress.com"
$adminPassword = "Admin@123" 

function Get-AuthToken {
    param (
        [string]$email,
        [string]$password
    )
    $loginUrl = "$baseUrl/auth/login"
    $body = @{
        email = $email
        password = $password
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $body -ContentType "application/json"
        return $response.accessToken
    } catch {
        Write-Error "Login failed: $_"
        if ($_.Exception.Response) {
             $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
             $responseBody = $reader.ReadToEnd()
             Write-Host "Error Response: $responseBody" -ForegroundColor Red
        }
        return $null
    }
}

function Test-Endpoint {
    param (
        [string]$url,
        [string]$token
    )
    $headers = @{
        Authorization = "Bearer $token"
    }

    try {
        Write-Host "Testing $url ..." -ForegroundColor Cyan
        $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers
        Write-Host "Success!" -ForegroundColor Green
        $json = $response | ConvertTo-Json -Depth 5
        Write-Output $json
    } catch {
        Write-Error "Request failed: $_"
    }
}

# 1. Login
Write-Host "Logging in as Admin..." -ForegroundColor Yellow
$token = Get-AuthToken -email $adminEmail -password $adminPassword

if ($token) {
    # 2. Test Dashboard Stats
    Test-Endpoint -url "$baseUrl/admin/dashboard/stats" -token $token

    # 3. Test Recent Activity
    Test-Endpoint -url "$baseUrl/admin/dashboard/activity" -token $token
} else {
    Write-Host "Cannot proceed without token." -ForegroundColor Red
}
