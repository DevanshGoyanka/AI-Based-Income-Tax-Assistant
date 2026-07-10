$BACKEND_URL = "http://localhost:8001/api/v1"
$curl = "C:\Windows\System32\curl.exe"

# === CONFIGURATION - EDIT THIS TO TEST DIFFERENT FILES ===
$filePath = "C:\Users\Devansh\Desktop\E-FILE_karo\ACUPG3482G-Prefill-2025-14_31_2026_18_50.json"
# ===========================================================

# Extract PAN from filename
$fileName = Split-Path $filePath -Leaf
if ($fileName -match '^([A-Z]{5}\d{4}[A-Z])-') {
    $filePan = $Matches[1].ToUpper()
} else {
    Write-Host "[ERROR] Cannot extract PAN from filename: $fileName" -ForegroundColor Red
    exit 1
}

Write-Host "=== PREFILL UPLOAD TEST ===" -ForegroundColor Cyan
Write-Host "File: $fileName" -ForegroundColor Cyan
Write-Host "PAN:  $filePan" -ForegroundColor Cyan
Write-Host ""

# 1. Login
Write-Host "1. Logging in..." -ForegroundColor Yellow
$loginJson = & $curl -s -X POST "$BACKEND_URL/auth/login" -H "Content-Type: application/json" -d '{\"email\":\"test@example.com\",\"password\":\"Test123!@#\"}'
$token = ($loginJson | ConvertFrom-Json).access_token
Write-Host "   [OK] Authenticated" -ForegroundColor Green
Write-Host ""

# 2. Find or create client with the file's PAN
Write-Host "2. Finding/creating client with PAN $filePan..." -ForegroundColor Yellow
$clientsJson = & $curl -s -X GET "$BACKEND_URL/clients" -H "Authorization: Bearer $token"
$clientsArray = $clientsJson | ConvertFrom-Json
$existingClient = $clientsArray | Where-Object { $_.pan -eq $filePan } | Select-Object -First 1

if ($existingClient) {
    $clientId = $existingClient.id
    Write-Host "   [OK] Found existing client: $clientId" -ForegroundColor Green
} else {
    # Get name from JSON
    $jsonContent = Get-Content $filePath -Raw | ConvertFrom-Json
    $firstName = $jsonContent.personalInfo.assesseeName.firstName
    $middleName = $jsonContent.personalInfo.assesseeName.middleName
    $lastName = $jsonContent.personalInfo.assesseeName.surName
    $fullName = "$firstName $middleName $lastName".Trim() -replace '\s+', ' '
    $email = $jsonContent.personalInfo.emailAddress.emailAddress
    $mobile = $jsonContent.personalInfo.emailAddress.mobileNo
    
    $body = @{
        pan = $filePan
        name = $fullName
        email = $email
        mobile = $mobile
    } | ConvertTo-Json -Compress
    
    $bodyEscaped = $body.Replace('"', '\"')
    $clientJson = & $curl -s -X POST "$BACKEND_URL/clients" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "`"$bodyEscaped`""
    $newClient = $clientJson | ConvertFrom-Json
    $clientId = $newClient.id
    Write-Host "   [OK] Created new client: $clientId" -ForegroundColor Green
}
Write-Host ""

# 3. Upload prefill
Write-Host "3. Uploading prefill JSON..." -ForegroundColor Yellow
$uploadJson = & $curl -s -X POST "$BACKEND_URL/imports/prefill/$clientId" -H "Authorization: Bearer $token" -F "file=@$filePath"
Write-Host "   [OK] Upload complete" -ForegroundColor Green
Write-Host ""

# Parse response with regex (PowerShell 5.1 compatible)
function Extract-JsonValue {
    param([string]$Json, [string]$Key)
    if ($Json -match "\`"$Key\`":\s*\`"([^\`"]+)\`"") {
        return $Matches[1]
    } elseif ($Json -match "\`"$Key\`":\s*([0-9]+)") {
        return $Matches[1]
    }
    return $null
}

$ay = Extract-JsonValue $uploadJson "assessment_year"
$pan = Extract-JsonValue $uploadJson "pan"
$name = Extract-JsonValue $uploadJson "name"
$dob = Extract-JsonValue $uploadJson "dob"
$aadhaar = Extract-JsonValue $uploadJson "aadhaar"
$mobile = Extract-JsonValue $uploadJson "mobile"

Write-Host "4. EXTRACTED DATA:" -ForegroundColor Yellow
Write-Host "   Assessment Year: $ay" -ForegroundColor Green
Write-Host ""
Write-Host "   Personal Info:" -ForegroundColor Cyan
Write-Host "     PAN:     $pan" -ForegroundColor White
Write-Host "     Name:    $name" -ForegroundColor White
Write-Host "     DOB:     $dob" -ForegroundColor White
Write-Host "     Aadhaar: $aadhaar" -ForegroundColor White
Write-Host "     Mobile:  $mobile" -ForegroundColor White
Write-Host ""

$tdsOther = Extract-JsonValue $uploadJson "tds_other_count"
$tdsSalary = Extract-JsonValue $uploadJson "tds_salary_count"
$taxPay = Extract-JsonValue $uploadJson "tax_payments_count"
$savings = Extract-JsonValue $uploadJson "savings_interest"
$deposit = Extract-JsonValue $uploadJson "term_deposit_interest"
$dividend = Extract-JsonValue $uploadJson "dividend"

Write-Host "   Form 26AS:" -ForegroundColor Cyan
Write-Host "     TDS on Salary:   $tdsSalary entries" -ForegroundColor White
Write-Host "     TDS on Other:    $tdsOther entries" -ForegroundColor White
Write-Host "     Tax Payments:    $taxPay entries" -ForegroundColor White
Write-Host ""
Write-Host "   Insights:" -ForegroundColor Cyan
Write-Host "     Savings Interest:      Rs.$savings" -ForegroundColor White
Write-Host "     Term Deposit Interest: Rs.$deposit" -ForegroundColor White
Write-Host "     Dividend:              Rs.$dividend" -ForegroundColor White
Write-Host ""

$bankMatches = [regex]::Matches($uploadJson, '\"bank_name\":\s*\"([^\"]+)\"')
Write-Host "   Bank Accounts: $($bankMatches.Count) accounts" -ForegroundColor Cyan
$i = 1
foreach ($m in $bankMatches) {
    Write-Host "     $i. $($m.Groups[1].Value)" -ForegroundColor White
    $i++
}

Write-Host ""
Write-Host "=== Run show_extracted_details.py $filePan to see full DB record ===" -ForegroundColor Magenta
Write-Host "=== TEST COMPLETED ===" -ForegroundColor Green
