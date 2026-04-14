# ITR ERP Client Creation Test
$API_BASE = "http://localhost:8080/api"

Write-Host "=== ITR ERP Test ===" -ForegroundColor Cyan

# Login
$loginBody = '{"email":"dev@test.com","password":"password123"}'
try {
    $login = Invoke-RestMethod -Uri "$API_BASE/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $login.token
    Write-Host "Login: OK" -ForegroundColor Green
} catch {
    Write-Host "Login: FAILED" -ForegroundColor Red
    exit
}

$headers = @{"Authorization"="Bearer $token";"Content-Type"="application/json"}

# Create 10 clients
$names = @("Rajesh Kumar","Priya Sharma","Amit Patel","Neha Gupta","Sanjay Singh","Kavita Verma","Vikram Reddy","Anjali Nair","Rahul Mehta","Pooja Joshi")
$itrs = @("ITR-1","ITR-2","ITR-3","ITR-4")
$created = 0

Write-Host "`nCreating clients..." -ForegroundColor Yellow
for($i=0; $i -lt 10; $i++) {
    $pan = -join ((65..90) | Get-Random -Count 5 | ForEach-Object {[char]$_})
    $pan += Get-Random -Minimum 1000 -Maximum 10000
    $pan += [char](Get-Random -Minimum 65 -Maximum 91)
    
    $body = @{
        name=$names[$i]
        pan=$pan
        email="test$i@example.com"
        mobile="+91 9876543210"
    } | ConvertTo-Json
    
    try {
        $r = Invoke-RestMethod -Uri "$API_BASE/clients" -Method Post -Body $body -Headers $headers
        Write-Host "  $($i+1). $($names[$i]) ($pan) - $($itrs[$i%4])" -ForegroundColor Green
        $created++
    } catch {
        Write-Host "  $($i+1). FAILED" -ForegroundColor Red
    }
}

# Get all clients
try {
    $all = Invoke-RestMethod -Uri "$API_BASE/clients" -Method Get -Headers $headers
    Write-Host "`nTotal in DB: $($all.Length)" -ForegroundColor Cyan
} catch {}

Write-Host "`n=== RESULT ===" -ForegroundColor Cyan
Write-Host "Created: $created/10" -ForegroundColor White
Write-Host "Test: COMPLETE" -ForegroundColor Green
