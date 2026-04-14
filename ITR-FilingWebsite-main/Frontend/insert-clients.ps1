# Insert 10 test clients via API
$API = "http://localhost:8080/api"

# Register user first (in case not exists), then login
Write-Host "Registering/Logging in..." -ForegroundColor Cyan

$creds = '{"email":"testuser@example.com","password":"Password123!"}'

# Try register first
try {
    Invoke-RestMethod -Uri "$API/auth/register" -Method Post -Body $creds -ContentType "application/json" | Out-Null
    Write-Host "User registered" -ForegroundColor Green
} catch {
    Write-Host "User already exists, proceeding to login..." -ForegroundColor Yellow
}

# Login
try {
    $login = Invoke-RestMethod -Uri "$API/auth/login" -Method Post -Body $creds -ContentType "application/json"
    $headers = @{"Authorization"="Bearer $($login.token)";"Content-Type"="application/json"}
    Write-Host "Login: OK" -ForegroundColor Green
} catch {
    Write-Host "Login FAILED: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

Write-Host "`nInserting 10 test clients..." -ForegroundColor Cyan

$clients = @(
    @{name="Rajesh Kumar";pan="ABCDE1234F";email="rajesh.kumar@example.com";mobile="+91 9876543210";aadhaar="123456789012";dob="1985-03-15"},
    @{name="Priya Sharma";pan="XYZAB5678C";email="priya.sharma@example.com";mobile="+91 9876543211";aadhaar="234567890123";dob="1990-07-22"},
    @{name="Amit Patel";pan="PQRST9012G";email="amit.patel@example.com";mobile="+91 9876543212";aadhaar="345678901234";dob="1982-11-08"},
    @{name="Neha Gupta";pan="LMNOP3456H";email="neha.gupta@example.com";mobile="+91 9876543213";aadhaar="456789012345";dob="1988-05-30"},
    @{name="Sanjay Singh";pan="DEFGH7890I";email="sanjay.singh@example.com";mobile="+91 9876543214";aadhaar="567890123456";dob="1975-09-12"},
    @{name="Kavita Verma";pan="JKLMN2345J";email="kavita.verma@example.com";mobile="+91 9876543215";aadhaar="678901234567";dob="1992-01-25"},
    @{name="Vikram Reddy";pan="STUVW6789K";email="vikram.reddy@example.com";mobile="+91 9876543216";aadhaar="789012345678";dob="1980-06-18"},
    @{name="Anjali Nair";pan="BCDEF0123L";email="anjali.nair@example.com";mobile="+91 9876543217";aadhaar="890123456789";dob="1995-12-03"},
    @{name="Rahul Mehta";pan="GHIJK4567M";email="rahul.mehta@example.com";mobile="+91 9876543218";aadhaar="901234567890";dob="1987-04-27"},
    @{name="Pooja Joshi";pan="NOPQR8901N";email="pooja.joshi@example.com";mobile="+91 9876543219";aadhaar="012345678901";dob="1993-08-14"}
)

$itrs = @("ITR-1","ITR-2","ITR-3","ITR-4")
$created = @()

foreach($i in 0..9) {
    $c = $clients[$i]
    $json = $c | ConvertTo-Json
    try {
        $r = Invoke-RestMethod -Uri "$API/clients" -Method Post -Body $json -Headers $headers
        $created += @{id=$r.id;name=$c.name;pan=$c.pan;itr=$itrs[$i%4]}
        Write-Host "  [$($i+1)/10] OK: $($c.name) (ID:$($r.id)) - $($itrs[$i%4])" -ForegroundColor Green
    } catch {
        $errMsg = $_.Exception.Message
        Write-Host "  [$($i+1)/10] FAILED: $errMsg" -ForegroundColor Red
    }
}

# Verify all clients
Write-Host "`nVerifying clients in database..." -ForegroundColor Cyan
try {
    $all = Invoke-RestMethod -Uri "$API/clients" -Method Get -Headers $headers
    Write-Host "Total clients in DB: $($all.Length)" -ForegroundColor Green
    $all | ForEach-Object { Write-Host "  - $($_.name) ($($_.pan)) ID:$($_.id)" -ForegroundColor White }
} catch {
    Write-Host "FAILED to list clients: $($_.Exception.Message)" -ForegroundColor Red
}

# Test ITR data retrieval
Write-Host "`nTesting ITR data retrieval..." -ForegroundColor Cyan
$itrSuccess = 0
foreach($c in $created) {
    try {
        $itr = Invoke-RestMethod -Uri "$API/clients/$($c.id)/itr/2025-26" -Method Get -Headers $headers
        Write-Host "  ITR data for $($c.name): OK (personalInfo: $($null -ne $itr.personalInfo))" -ForegroundColor Green
        $itrSuccess++
    } catch {
        Write-Host "  ITR data for $($c.name): FAILED" -ForegroundColor Red
    }
}

# Dashboard stats test
Write-Host "`nTesting dashboard stats..." -ForegroundColor Cyan
try {
    $stats = Invoke-RestMethod -Uri "$API/dashboard/stats" -Method Get -Headers $headers
    Write-Host "  Stats: total=$($stats.total), filed=$($stats.filed)" -ForegroundColor Green
} catch {
    Write-Host "  Dashboard stats: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Filing list test
Write-Host "`nTesting filing list..." -ForegroundColor Cyan
try {
    $filings = Invoke-RestMethod -Uri "$API/filing" -Method Get -Headers $headers
    Write-Host "  Filings: $($filings.Length) found" -ForegroundColor Green
} catch {
    Write-Host "  Filing list: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Final Report
Write-Host "`n==============================" -ForegroundColor Cyan
Write-Host "       FINAL TEST REPORT      " -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan
Write-Host "Clients created:     $($created.Count)/10"
Write-Host "ITR data retrieval:  $itrSuccess/$($created.Count)"
Write-Host ""
Write-Host "Clients by ITR Type:" 
$created | Group-Object itr | ForEach-Object { Write-Host "  $($_.Name): $($_.Count) clients" }
Write-Host "==============================" -ForegroundColor Cyan
