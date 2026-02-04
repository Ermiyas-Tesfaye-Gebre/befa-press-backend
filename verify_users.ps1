
$BaseUrl = "http://localhost:9090/api/v1"
$AdminEmail = "admin@befapress.com"
$AdminPassword = "Admin@123"

# 1. Login to get token
$LoginPayload = @{
    email = $AdminEmail
    password = $AdminPassword
} | ConvertTo-Json

try {
    $LoginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Post -Body $LoginPayload -ContentType "application/json"
    $Token = $LoginResponse.accessToken
    Write-Host "Admin logged in successfully."
} catch {
    Write-Host "Login Failed: $_"
    exit
}

$Headers = @{
    "Authorization" = "Bearer $Token"
}

# 2. Get Users sorted by createdAt desc
try {
    $UsersResponse = Invoke-RestMethod -Uri "$BaseUrl/admin/users?page=0&size=20&sort=createdAt,desc" -Method Get -Headers $Headers
    Write-Host "Raw Response Content:"
    $UsersResponse | ConvertTo-Json -Depth 2
    $Users = $UsersResponse.content

    Write-Host "`nRecent Users:"
    foreach ($u in $Users) {
        Write-Host "ID: $($u.id) | Name: $($u.fullName) | Email: $($u.email) | CreatedAt: $($u.createdAt)"
    }
} catch {
    Write-Host "Failed to get users: $_"
}
