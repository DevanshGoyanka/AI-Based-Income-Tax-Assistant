# ITR ERP Client Creation Test Script
# PowerShell script to create 10 test clients

$API_BASE = "http://localhost:8080/api"
$authToken = ""

# Login
Write-Host "=== ITR ERP Client Creation Test ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Step 1: Logging in..." -ForegroundColor Yellow

$loginBody = @{
    email = "dev@test.com"
    password = "password123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$API_BASE/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $authToken = $loginResponse.token
    Write-Host "✓ Login successful" -ForegroundColor Green
} catch {
    Write-Host "✗ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Generate random PAN
function Get-RandomPAN {
    $letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    $pan = ""
    for ($i = 0; $i -lt 5; $i++) {
        $pan += $letters[(Get-Random -Maximum 26)]
    }
    $pan += (Get-Random -Minimum 1000 -Maximum 10000)
    $pan += $letters[(Get-Random -Maximum 26)]
    return $pan
}

# Client data
$firstNames = @("Rajesh", "Priya", "Amit", "Neha", "Sanjay", "Kavita", "Vikram", "Anjali", "Rahul", "Pooja")
$lastNames = @("Kumar", "Sharma", "Patel", "Gupta", "Singh", "Verma", "Reddy", "Nair", "Mehta", "Joshi")
$itrTypes = @("ITR-1", "ITR-2", "ITR-3", "ITR-4")

# Create 10 clients
Write-Host ""
Write-Host "Step 2: Creating 10 clients..." -ForegroundColor Yellow
$createdClients = @()
$headers = @{
    "Authorization" = "Bearer $authToken"
    "Content-Type" = "application/json"
}

for ($i = 0; $i -lt 10; $i++) {
    $firstName = $firstNames[$i % $firstNames.Length]
    $lastName = $lastNames[(Get-Random -Maximum $lastNames.Length)]
    $pan = Get-RandomPAN
    $itrType = $itrTypes[$i % 4]
    
    $clientData = @{
        name = "$firstName $lastName"
        pan = $pan
        email = "$($firstName.ToLower()).$($lastName.ToLower())$i@example.com"
        mobile = "+91 $(Get-Random -Minimum 7000000000 -Maximum 10000000000)"
        aadhaar = "$(Get-Random -Minimum 100000000000 -Maximum 1000000000000)"
        dob = "19$(Get-Random -Minimum 60 -Maximum 95)-$(Get-Random -Minimum 1 -Maximum 13).ToString('00')-$(Get-Random -Minimum 1 -Maximum 29).ToString('00')"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$API_BASE/clients" -Method Post -Body $clientData -Headers $headers
        $createdClients += @{
            id = $response.id
            name = $response.name
            pan = $response.pan
            itrType = $itrType
        }
        Write-Host "  ✓ Client $($i+1)/10: $firstName $lastName ($pan) - $itrType" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ Failed to create client $($i+1): $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Verify clients
Write-Host ""
Write-Host "Step 3: Verifying clients in database..." -ForegroundColor Yellow
try {
    $allClients = Invoke-RestMethod -Uri "$API_BASE/clients" -Method Get -Headers $headers
    Write-Host "✓ Total clients in database: $($allClients.Length)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to retrieve clients: $($_.Exception.Message)" -ForegroundColor Red
}

# Test ITR data retrieval
Write-Host ""
Write-Host "Step 4: Testing ITR data retrieval..." -ForegroundColor Yellow
$successCount = 0
foreach ($client in $createdClients) {
    try {
        $itrData = Invoke-RestMethod -Uri "$API_BASE/clients/$($client.id)/itr/2025-26" -Method Get -Headers $headers
        Write-Host "  ✓ Retrieved ITR data for $($client.name)" -ForegroundColor Green
        $successCount++
    } catch {
        Write-Host "  ✗ Failed for $($client.name): $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Final Report
Write-Host ""
Write-Host "=== FINAL REPORT ===" -ForegroundColor Cyan
Write-Host "Clients created: $($createdClients.Count)" -ForegroundColor White
Write-Host "ITR data retrieval success: $successCount/$($createdClients.Count)" -ForegroundColor White
Write-Host ""
Write-Host "Clients by ITR Type:" -ForegroundColor White
$itrCounts = $createdClients | Group-Object -Property itrType
foreach ($group in $itrCounts) {
    Write-Host "  $($group.Name): $($group.Count) clients" -ForegroundColor White
}
Write-Host ""
Write-Host "✓ Test completed!" -ForegroundColor Green
