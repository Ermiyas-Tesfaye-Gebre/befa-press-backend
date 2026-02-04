$headers = @{
    "Content-Type" = "application/json"
}

$loginBody = @{
    email    = "intellectual@befapress.com"
    password = "Writer@123"
} | ConvertTo-Json

try {
    Write-Host "Logging in..."
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/auth/login" -Method Post -Headers $headers -Body $loginBody
    $token = $loginResponse.accessToken
    Write-Host "Login successful. Token obtained."
}
catch {
    Write-Host "Login Failed!"
    Write-Host $_
    exit
}

$commentHeaders = @{
    "Content-Type"  = "application/json"
    "Authorization" = "Bearer $token"
}

$commentBody = @{
    content  = "Testing comment " + (Get-Date).ToString()
    parentId = $null
} | ConvertTo-Json

try {
    Write-Host "Posting comment..."
    $response = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/comments/news/1" -Method Post -Headers $commentHeaders -Body $commentBody
    Write-Host "Comment posted successfully!"
    $response
}
catch {
    Write-Host "Post Comment Failed!"
    Write-Host "Status Code: " $_.Exception.Response.StatusCode.value__
    
    # Try to read the error stream
    $stream = $_.Exception.Response.GetResponseStream()
    if ($stream) {
        $reader = New-Object System.IO.StreamReader($stream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error Body: $errorBody"
    }
}
