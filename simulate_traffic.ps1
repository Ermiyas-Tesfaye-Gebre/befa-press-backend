
$BaseUrl = "http://localhost:9090/api/v1"

# 1. Register a new user (New Subscriber)
$UserPayload = @{
    fullName = "Dynamic Test User"
    email = "dynamic.test." + (Get-Random) + "@befapress.com"
    password = "Password123"
    role = "USER"
} | ConvertTo-Json

Write-Host "Registering new user..."
try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -Body $UserPayload -ContentType "application/json"
    Write-Host "User Registered: $($response.message)"
} catch {
    Write-Host "Registration Failed: $_"
}

# 2. Track Page Hits (5 times)
$TrackPayload = @{
    entityType = "HOME"
    sessionId = "sim-session-" + (Get-Random)
    referrer = "direct"
    language = "en"
} | ConvertTo-Json

Write-Host "Tracking 5 page hits..."
for ($i=1; $i -le 5; $i++) {
    try {
        Invoke-RestMethod -Uri "$BaseUrl/analytics/track" -Method Post -Body $TrackPayload -ContentType "application/json"
        Write-Host "Hit $i recorded."
    } catch {
        Write-Host "Hit $i failed: $_"
    }
}
