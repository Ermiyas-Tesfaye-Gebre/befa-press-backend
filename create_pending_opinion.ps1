$baseUrl = "http://localhost:9090/api/v1"
$loginUrl = "$baseUrl/auth/login"
$createOpinionUrl = "$baseUrl/intellectual/opinions"

# 1. Login as Intellectual
$loginBody = @{
    email    = "intellectual@befapress.com"
    password = "Writer@123"
} | ConvertTo-Json

Write-Host "Logging in as Intellectual..."
try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method POST -ContentType "application/json" -Body $loginBody
    
    if ($loginResponse.accessToken) {
        $token = $loginResponse.accessToken
        Write-Host "Login successful!"
        
        # 2. Create Pending Opinion
        $opinionBody = @{
            title   = "Test PENDING Opinion For Admin Review"
            content = "<p>This is a test opinion created to verify the admin pending list.</p>"
            excerpt = "Test excerpt for admin review."
            status  = "PENDING"
        } | ConvertTo-Json

        Write-Host "Creating PENDING opinion..."
        $headers = @{
            Authorization = "Bearer $token"
        }

        try {
            $opinionResponse = Invoke-RestMethod -Uri $createOpinionUrl -Method POST -Headers $headers -ContentType "application/json" -Body $opinionBody
            Write-Host "SUCCESS: Created Opinion ID $($opinionResponse.id) with status $($opinionResponse.status)"
        }
        catch {
            Write-Host "FAILED: Could not create opinion."
            Write-Host $_
            exit 1
        }

    }
    else {
        Write-Host "FAILED: Login failed."
        exit 1
    }
}
catch {
    Write-Host "FAILED: Login error."
    Write-Host $_
    exit 1
}
