
# test_refresh.ps1
$ErrorActionPreference = "Stop"

function Test-Refresh {
    Write-Host "1. Logging in..."
    $loginUrl = "http://localhost:9090/api/v1/auth/login"
    $loginBody = @{
        email = "intellectual@befapress.com"
        password = "Writer@123"
    } | ConvertTo-Json

    try {
        $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
        Write-Host "Login Successful!"
    } catch {
        Write-Host "Login Failed: $($_.Exception.Message)"
        if ($_.Exception.Response) {
             $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
             Write-Host "Response: $($reader.ReadToEnd())"
        }
        return
    }

    $refreshToken = $loginResponse.refreshToken
    Write-Host "Got Refresh Token: $refreshToken"

    Write-Host "`n2. Refreshing Token..."
    $refreshUrl = "http://localhost:9090/api/v1/auth/refresh"
    $refreshBody = @{
        refreshToken = $refreshToken
    } | ConvertTo-Json

    try {
        $refreshResponse = Invoke-RestMethod -Uri $refreshUrl -Method Post -Body $refreshBody -ContentType "application/json"
        Write-Host "Refresh Successful!"
        Write-Host "New Access Token: $($refreshResponse.accessToken)"
    } catch {
        Write-Host "Refresh Failed: $($_.Exception.Message)"
        Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
        if ($_.Exception.Response) {
             $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
             Write-Host "Response Body: $($reader.ReadToEnd())"
        }
    }
}

Test-Refresh
