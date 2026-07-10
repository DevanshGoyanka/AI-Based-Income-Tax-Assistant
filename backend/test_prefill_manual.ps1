# Manual test script for prefill upload API

$BACKEND_URL = "http://localhost:8001/api/v1"

Write-Host "=== PREFILL UPLOAD MANUAL TEST ===" -ForegroundColor Cyan
Write-Host ""

# 1. Login
Write-Host "1. Logging in..." -ForegroundColor Yellow
$loginResponse = Invoke-RestMethod -Uri "$BACKEND_URL/auth/login" -Method POST -ContentType "application/json" -Body (@{
    email = "test@example.com"
    password = "Test123!@#"
} | ConvertTo-Json)
$token = $loginResponse.access_token
Write-Host "   Token: $($token.Substring(0,20))..." -ForegroundColor Green
Write-Host ""

# 2. Create test client
Write-Host "2. Creating test client..." -ForegroundColor Yellow
$clientResponse = Invoke-RestMethod -Uri "$BACKEND_URL/clients" -Method POST `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType "application/json" `
    -Body (@{
        pan = "ACUPG3482G"
        name = "SUNIT RAMASHANKAR GOYANKA"
        email = "sunitgoyanka@gmail.com"
        mobile = "9422772675"
    } | ConvertTo-Json)
$clientId = $clientResponse.id
Write-Host "   Client ID: $clientId" -ForegroundColor Green
Write-Host ""

# 3. Upload prefill JSON
Write-Host "3. Uploading prefill JSON..." -ForegroundColor Yellow
$filePath = "C:\Users\Devansh\Desktop\E-FILE_karo\ACUPG3482G-Prefill-2025-14_31_2026_18_50.json"
$boundary = [System.Guid]::NewGuid().ToString()
$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileContent = [System.Text.Encoding]::GetEncoding('iso-8859-1').GetString($fileBytes)

$bodyLines = @(
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"ACUPG3482G-Prefill-2025-14_31_2026_18_50.json`"",
    "Content-Type: application/json",
    "",
    $fileContent,
    "--$boundary--"
)
$body = $bodyLines -join "`r`n"

$uploadResponse = Invoke-RestMethod -Uri "$BACKEND_URL/imports/prefill/$clientId" -Method POST `
    -Headers @{ 
        Authorization = "Bearer $token"
        "Content-Type" = "multipart/form-data; boundary=$boundary"
    } `
    -Body $body

Write-Host "   Status: $($uploadResponse.status)" -ForegroundColor Green
Write-Host "   Assessment Year: $($uploadResponse.assessment_year)" -ForegroundColor Green
Write-Host ""

# 4. Display extracted data
Write-Host "4. Extracted Data:" -ForegroundColor Yellow
$pi = $uploadResponse.personal_info
Write-Host "   PAN: $($pi.pan)" -ForegroundColor Cyan
Write-Host "   Name: $($pi.name)" -ForegroundColor Cyan
Write-Host "   DOB: $($pi.dob)" -ForegroundColor Cyan
Write-Host "   Aadhaar: $($pi.aadhaar)" -ForegroundColor Cyan
Write-Host "   Mobile: $($pi.mobile)" -ForegroundColor Cyan
Write-Host ""

if ($uploadResponse.form26as) {
    $f26 = $uploadResponse.form26as
    Write-Host "   Form 26AS:" -ForegroundColor Cyan
    Write-Host "     TDS on Salary: $($f26.tds_salary_count) entries" -ForegroundColor White
    Write-Host "     TDS on Other: $($f26.tds_other_count) entries" -ForegroundColor White
    Write-Host "     Tax Payments: $($f26.tax_payments_count) entries" -ForegroundColor White
    Write-Host ""
}

if ($uploadResponse.insights) {
    $ins = $uploadResponse.insights
    Write-Host "   Insights:" -ForegroundColor Cyan
    Write-Host "     Savings Interest: Rs.$($ins.savings_interest)" -ForegroundColor White
    Write-Host "     Term Deposit Interest: Rs.$($ins.term_deposit_interest)" -ForegroundColor White
    Write-Host "     Dividend: Rs.$($ins.dividend)" -ForegroundColor White
    Write-Host ""
}

if ($uploadResponse.bank_accounts) {
    Write-Host "   Bank Accounts: $($uploadResponse.bank_accounts.Count) accounts" -ForegroundColor Cyan
    $uploadResponse.bank_accounts | ForEach-Object -Begin { $i = 1 } -Process {
        Write-Host "     $i. $($_.bank_name) - $($_.ifsc)" -ForegroundColor White
        $i++
    }
}

Write-Host ""
Write-Host "=== TEST COMPLETED ===" -ForegroundColor Green
