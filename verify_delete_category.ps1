$baseUrl = "http://localhost:9090/api/v1"
$loginUrl = "$baseUrl/auth/login"
$deleteUrl = "$baseUrl/admin/categories/1"

# 1. Login as Admin
$loginBody = @{
    email    = "admin@befapress.com"
    password = "Admin@123"
} | ConvertTo-Json

Write-Host "Logging in..."
try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method POST -ContentType "application/json" -Body $loginBody
    $token = $loginResponse.accessToken
    Write-Host "Login successful."

    # 2. Try to Delete Category 1
    Write-Host "Attempting DELETE $deleteUrl..."
    $headers = @{
        Authorization = "Bearer $token"
    }

    try {
        Invoke-RestMethod -Uri $deleteUrl -Method DELETE -Headers $headers
        Write-Host "SUCCESS (Unexpected): Category was deleted?"
    }
    catch {
        # Check if it is the expected 400 error
        $params = $_.Exception.Response
        $statusCode = $params.StatusCode.value__
        
        if ($statusCode -eq 400) {
            Write-Host "SUCCESS (Expected): Received 400 Bad Request."
            $reader = New-Object System.IO.StreamReader($params.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Response Body: $responseBody"
        }
        else {
            Write-Host "FAILURE: Received status code $statusCode"
            $reader = New-Object System.IO.StreamReader($params.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Response Body: $responseBody"
        }
    }

}
catch {
    Write-Host "Login Failed or other error: $_"
}
