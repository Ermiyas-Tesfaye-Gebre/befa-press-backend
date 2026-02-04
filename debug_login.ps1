try {
    $response = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/user/me" -Method Get
    Write-Host "Unauthenticated check success (unexpected)"
} catch {
    Write-Host "Unauthenticated check: $($_.Exception.Message)"
}

$body = @{
    email = "admin@befapress.com"
    password = "Admin@123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/auth/login" -Method Post -Body $body -ContentType "application/json"
    Write-Host "Login Success!"
    Write-Host "Token: $($response.token)"
} catch {
    Write-Host "Login Failed!"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "Status Description: $($_.Exception.Response.StatusDescription)"
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Body: $($reader.ReadToEnd())"
}
