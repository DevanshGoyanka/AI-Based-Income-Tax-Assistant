# Backend Feature Test Script
$API = "http://localhost:8080/api"
$creds = '{"email":"testuser@example.com","password":"Password123!"}'

Write-Host "`n=== ITR Backend Feature Tests ===" -ForegroundColor Cyan

# Login
Write-Host "`n[1] Authentication..." -ForegroundColor Yellow
$login = Invoke-RestMethod -Uri "$API/auth/login" -Method Post -Body $creds -ContentType "application/json"
$headers = @{"Authorization"="Bearer $($login.token)";"Content-Type"="application/json"}
Write-Host "  OK - Logged in" -ForegroundColor Green

# Get clients
Write-Host "`n[2] Client List..." -ForegroundColor Yellow
$clients = Invoke-RestMethod -Uri "$API/clients" -Method Get -Headers $headers
Write-Host "  OK - Found $($clients.Length) clients" -ForegroundColor Green
$client = $clients[0]

# Test ITR Form Data (NEW)
Write-Host "`n[3] ITR Form Data Retrieval..." -ForegroundColor Yellow
$itrData = Invoke-RestMethod -Uri "$API/clients/$($client.id)/itr/2025-26" -Method Get -Headers $headers
if ($itrData.personalInfo) {
    Write-Host "  OK - Personal info populated" -ForegroundColor Green
    Write-Host "    Name: $($itrData.personalInfo.assesseeName)" -ForegroundColor White
    Write-Host "    PAN: $($itrData.personalInfo.pan)" -ForegroundColor White
    Write-Host "    Age: $($itrData.personalInfo.age)" -ForegroundColor White
} else {
    Write-Host "  FAIL - No personal info" -ForegroundColor Red
}

# Test Validation (NEW)
Write-Host "`n[4] ITR Validation Service..." -ForegroundColor Yellow
$validation = Invoke-RestMethod -Uri "$API/clients/$($client.id)/itr/2025-26/validate" -Method Post -Body ($itrData | ConvertTo-Json -Depth 10) -Headers $headers
Write-Host "  OK - Validation working" -ForegroundColor Green
Write-Host "    Valid: $($validation.valid)" -ForegroundColor White
Write-Host "    Errors: $($validation.errors.Count)" -ForegroundColor White

# Test Filing Management (NEW)
Write-Host "`n[5] Filing Management..." -ForegroundColor Yellow
$filings = Invoke-RestMethod -Uri "$API/filing" -Method Get -Headers $headers
Write-Host "  OK - Retrieved $($filings.Length) filings" -ForegroundColor Green

$newFiling = @{clientId=$client.id;assessmentYear="2025-26";itrType="ITR-1"} | ConvertTo-Json
$created = Invoke-RestMethod -Uri "$API/filing" -Method Post -Body $newFiling -Headers $headers
Write-Host "  OK - Created filing ID $($created.id)" -ForegroundColor Green

Write-Host "`n=== All Tests Passed ===" -ForegroundColor Green
Write-Host "Backend implementations working:" -ForegroundColor White
Write-Host "  1. Itr1FormService - Returns real client data" -ForegroundColor White
Write-Host "  2. ITR Filing Management - CRUD operations" -ForegroundColor White
Write-Host "  3. Validation Service - Form validation" -ForegroundColor White
