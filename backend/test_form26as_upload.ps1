# === Form 26AS Upload Test Script ===
# Save this file as test_form26as_upload.ps1 in the backend folder
# Run with: .\test_form26as_upload.ps1

# === CONFIGURATION - EDIT THESE PATHS ===
$PDF_FILE = "C:\path\to\your\form26as.pdf"
$ZIP_FILE = "C:\path\to\your\form26as.zip"
$BACKEND_URL = "http://localhost:8001/api/v1"
# ==========================================

$curl = "C:\Windows\System32\curl.exe"

# Check if files exist
if (-not (Test-Path $PDF_FILE)) {
    Write-Host "[ERROR] PDF file not found: $PDF_FILE" -ForegroundColor Red
    Write-Host "        Please update the `$PDF_FILE variable with the actual path" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $ZIP_FILE)) {
    Write-Host "[ERROR] ZIP file not found: $ZIP_FILE" -ForegroundColor Red
    Write-Host "        Please update the `$ZIP_FILE variable with the actual path" -ForegroundColor Yellow
    exit 1
}

Write-Host "=== Form 26AS Upload Test ===" -ForegroundColor Cyan
Write-Host "PDF:  $PDF_FILE" -ForegroundColor Cyan
Write-Host "ZIP:  $ZIP_FILE" -ForegroundColor Cyan
Write-Host ""

# 1. Login
Write-Host "1. Logging in..." -ForegroundColor Yellow
$loginJson = & $curl -s -X POST "$BACKEND_URL/auth/login" -H "Content-Type: application/json" -d '{"email":"test@example.com","password":"Test123!@#"}'
$loginResult = $loginJson | ConvertFrom-Json

if (-not $loginResult.access_token) {
    Write-Host "   [ERROR] Login failed. Response: $loginJson" -ForegroundColor Red
    exit 1
}

$token = $loginResult.access_token
Write-Host "   [OK] Authenticated" -ForegroundColor Green
Write-Host ""

# 2. Get or create a test client
Write-Host "2. Getting/creating test client..." -ForegroundColor Yellow
$clientsJson = & $curl -s -X GET "$BACKEND_URL/clients" -H "Authorization: Bearer $token"
$clientsArray = $clientsJson | ConvertFrom-Json

# Use first client or create one
if ($clientsArray.Count -gt 0) {
    $client = $clientsArray[0]
    $clientId = $client.id
    $clientPan = $client.pan
    Write-Host "   [OK] Using existing client: $clientId (PAN: $clientPan)" -ForegroundColor Green
} else {
    # Create a test client
    $body = @{
        pan = "COVPC5929M"
        name = "Test User"
        email = "test@example.com"
        mobile = "9876543210"
    } | ConvertTo-Json -Compress
    
    $bodyEscaped = $body.Replace('"', '\"')
    $newClientJson = & $curl -s -X POST "$BACKEND_URL/clients" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "`"$bodyEscaped`""
    $newClient = $newClientJson | ConvertFrom-Json
    $clientId = $newClient.id
    $clientPan = "COVPC5929M"
    Write-Host "   [OK] Created new client: $clientId" -ForegroundColor Green
}
Write-Host ""

# Function to extract JSON values (PowerShell 5.1 compatible)
function Extract-JsonValue {
    param([string]$Json, [string]$Key)
    if ($Json -match "`"$Key`":\s*`"([^`"]+)`"") {
        return $Matches[1]
    } elseif ($Json -match "`"$Key`":\s*([0-9]+\.?[0-9]*)") {
        return $Matches[1]
    } elseif ($Json -match "`"$Key`":\s*(true|false)") {
        return $Matches[1]
    } elseif ($Json -match "`"$Key`":\s*(null)") {
        return $null
    }
    return $null
}

# 3. Test PDF Upload (Form 26AS PDF)
Write-Host "3. Testing Form 26AS PDF Upload..." -ForegroundColor Yellow
Write-Host "   File: $(Split-Path $PDF_FILE -Leaf)" -ForegroundColor Gray
Write-Host ""

# PDF password is typically DDMMYYYY format of DOB
# Ask user for DOB if not known
$DOB = Read-Host "   Enter DOB for PDF password (DD-MM-YYYY format, e.g., 08-02-2002)"
if (-not $DOB) {
    $DOB = "08-02-2002"  # Default for testing
    Write-Host "   Using default DOB: $DOB" -ForegroundColor Gray
}

Write-Host "   Uploading PDF with DOB password: $DOB" -ForegroundColor Gray
$pdfResponse = & $curl -s -X POST "$BACKEND_URL/imports/form26as/$clientId" `
    -H "Authorization: Bearer $token" `
    -F "file=@$PDF_FILE" `
    -F "dob=$DOB" `
    -F "file_type=pdf"

Write-Host ""
Write-Host "   Response:" -ForegroundColor Gray
Write-Host $pdfResponse -ForegroundColor White
Write-Host ""

$pdfImported = Extract-JsonValue $pdfResponse "imported"
$pdfErrors = Extract-JsonValue $pdfResponse "errors"

if ($pdfImported -eq "1") {
    Write-Host "   [OK] PDF imported successfully!" -ForegroundColor Green
} elseif ($pdfErrors -and $pdfErrors -ne "[]") {
    Write-Host "   [WARN] PDF import had errors: $pdfErrors" -ForegroundColor Yellow
} else {
    Write-Host "   [INFO] PDF import response received" -ForegroundColor Cyan
}
Write-Host ""

# 4. Test ZIP Upload (Form 26AS ZIP)
Write-Host "4. Testing Form 26AS ZIP Upload..." -ForegroundColor Yellow
Write-Host "   File: $(Split-Path $ZIP_FILE -Leaf)" -ForegroundColor Gray
Write-Host ""

$DOB = Read-Host "   Enter DOB for ZIP password (DDMMYYYY format, e.g., 08022002)"
if (-not $DOB) {
    $DOB = "08022002"  # Default for testing
    Write-Host "   Using default DOB: $DOB" -ForegroundColor Gray
}

Write-Host "   Uploading ZIP with DOB password: $DOB" -ForegroundColor Gray
$zipResponse = & $curl -s -X POST "$BACKEND_URL/imports/form26as/$clientId" `
    -H "Authorization: Bearer $token" `
    -F "file=@$ZIP_FILE" `
    -F "dob=$DOB" `
    -F "file_type=zip"

Write-Host ""
Write-Host "   Response:" -ForegroundColor Gray
Write-Host $zipResponse -ForegroundColor White
Write-Host ""

$zipImported = Extract-JsonValue $zipResponse "imported"
$zipErrors = Extract-JsonValue $zipResponse "errors"

if ($zipImported -eq "1") {
    Write-Host "   [OK] ZIP imported successfully!" -ForegroundColor Green
    
    # Extract and display summary
    Write-Host ""
    Write-Host "5. EXTRACTED SUMMARY:" -ForegroundColor Yellow
    
    $tdsCount = Extract-JsonValue $zipResponse "tds_deductors_count"
    $totalTds = Extract-JsonValue $zipResponse "total_tds_deducted"
    $totalAmount = Extract-JsonValue $zipResponse "total_amount"
    
    Write-Host "   TDS Deductors Count: $tdsCount" -ForegroundColor Cyan
    Write-Host "   Total TDS Deducted:  Rs.$totalTds" -ForegroundColor Cyan
    Write-Host "   Total Amount:        Rs.$totalAmount" -ForegroundColor Cyan
} elseif ($zipErrors -and $zipErrors -ne "[]") {
    Write-Host "   [WARN] ZIP import had errors: $zipErrors" -ForegroundColor Yellow
} else {
    Write-Host "   [INFO] ZIP import response received" -ForegroundColor Cyan
}
Write-Host ""

Write-Host "=== TEST COMPLETED ===" -ForegroundColor Green
Write-Host ""
Write-Host "To view the full extracted data in database, run:" -ForegroundColor Gray
Write-Host "   python -c `"from app.infra.db import SessionLocal; from app.infra.db.models import Form26ASData, Client; db = SessionLocal(); c = db.query(Client).first(); print(c.name, c.pan)`"" -ForegroundColor White
