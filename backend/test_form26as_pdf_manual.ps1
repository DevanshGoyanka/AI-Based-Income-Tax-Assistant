# Test Form 26AS PDF Import
# Usage: Run this script from PowerShell

$API = "http://localhost:8001/api/v1"
$EMAIL = "test@example.com"
$PASSWORD = "Test123!@#"

# ============================================
# STEP 1: Login and get token
# ============================================
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "STEP 1: Login" -ForegroundColor Yellow
Write-Host "=" * 60 -ForegroundColor Cyan

$loginBody = @{
    email = $EMAIL
    password = $PASSWORD
}

try {
    $loginResp = Invoke-RestMethod -Uri "$API/auth/login" -Method POST -Body ($loginBody | ConvertTo-Json) -ContentType "application/json"
    $token = $loginResp.access_token
    Write-Host "Logged in successfully!" -ForegroundColor Green
    Write-Host "Token: $($token.Substring(0, 20))..." -ForegroundColor Gray
} catch {
    Write-Host "Login failed: $_" -ForegroundColor Red
    exit 1
}

$headers = @{
    Authorization = "Bearer $token"
}

# ============================================
# STEP 2: Select PDF file
# ============================================
Write-Host ""
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "STEP 2: Select PDF File" -ForegroundColor Yellow
Write-Host "=" * 60 -ForegroundColor Cyan

# Check for available PDF files
$pdfDir = "C:\Users\Devansh\Desktop\E-FILE_karo"
$downloadsDir = "C:\Users\Devansh\Downloads"

$pdfFiles = @()
$pdfFiles += Get-ChildItem $pdfDir -Filter "*.pdf" -ErrorAction SilentlyContinue
$pdfFiles += Get-ChildItem $downloadsDir -Filter "*COVPC5929M*.pdf" -ErrorAction SilentlyContinue

if ($pdfFiles.Count -eq 0) {
    Write-Host "No PDF files found. Please place a Form 26AS PDF file in:" -ForegroundColor Yellow
    Write-Host "  - $pdfDir" -ForegroundColor Gray
    Write-Host "  - $downloadsDir" -ForegroundColor Gray
    
    # Let user specify path
    $pdfPath = Read-Host "Enter full path to Form 26AS PDF file"
    if (-not (Test-Path $pdfPath)) {
        Write-Host "File not found: $pdfPath" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Available Form 26AS PDF files:" -ForegroundColor Green
    $pdfFiles | ForEach-Object -Begin { $i = 1 } -Process {
        Write-Host "  [$i] $($_.Name)" -ForegroundColor White
        $i++
    }
    
    $selection = Read-Host "Select a file number (or enter path)"
    
    if ($selection -match "^\d+$") {
        $pdfPath = $pdfFiles[[int]$selection - 1].FullName
    } else {
        $pdfPath = $selection
    }
}

Write-Host ""
Write-Host "Selected file: $pdfPath" -ForegroundColor Cyan
$fileBytes = [System.IO.File]::ReadAllBytes($pdfPath)
$fileName = Split-Path $pdfPath -Leaf

# ============================================
# STEP 3: Get client info
# ============================================
Write-Host ""
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "STEP 3: Get Client Info" -ForegroundColor Yellow
Write-Host "=" * 60 -ForegroundColor Cyan

# Client ID for YASH CHANDAK (test user)
$clientId = "2fe92273-a57f-4b2b-8867-d6b2916c6680"

Write-Host "Client ID: $clientId" -ForegroundColor Cyan

# ============================================
# STEP 4: Note about encryption
# ============================================
Write-Host ""
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "STEP 4: Encryption Note" -ForegroundColor Yellow
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "PDF files are NOT encrypted (only ZIP files use DOB password)" -ForegroundColor Green
Write-Host "Skipping password prompt..." -ForegroundColor Gray

# ============================================
# STEP 5: Enter Assessment Year
# ============================================
Write-Host ""
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "STEP 5: Enter Assessment Year" -ForegroundColor Yellow
Write-Host "=" * 60 -ForegroundColor Cyan

Write-Host "Common options:" -ForegroundColor Gray
Write-Host "  1. 2025-26" -ForegroundColor White
Write-Host "  2. 2026-27" -ForegroundColor White

$aySelection = Read-Host "Assessment Year (e.g., 2025-26)"
if (-not $aySelection) {
    $aySelection = "2025-26"
}

Write-Host "Assessment Year: $aySelection" -ForegroundColor Cyan

# ============================================
# STEP 6: Upload and Import
# ============================================
Write-Host ""
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "STEP 6: Uploading and Importing..." -ForegroundColor Yellow
Write-Host "=" * 60 -ForegroundColor Cyan

try {
    # Create multipart form data
    $boundary = [System.Guid]::NewGuid().ToString()
    
    # Build the multipart body
    $bodyParts = @()
    
    # Add file part
    $filePart = "--$boundary`r`n"
    $filePart += "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"`r`n"
    $filePart += "Content-Type: application/pdf`r`n`r`n"
    $filePartBytes = [System.Text.Encoding]::UTF8.GetBytes($filePart)
    
    # Combine file bytes
    $fileData = $fileBytes
    $endBoundary = [System.Text.Encoding]::UTF8.GetBytes("`r`n--$boundary--`r`n")
    
    $requestBody = New-Object System.IO.MemoryStream
    $requestBody.Write($filePartBytes, 0, $filePartBytes.Length)
    $requestBody.Write($fileData, 0, $fileData.Length)
    $requestBody.Write($endBoundary, 0, $endBoundary.Length)
    
    # Add AY field
    $ayPart = "--$boundary`r`n"
    $ayPart += "Content-Disposition: form-data; name=`"ay`"`r`n`r`n"
    $ayPart += "$aySelection"
    $ayPartBytes = [System.Text.Encoding]::UTF8.GetBytes($ayPart)
    
    # Rebuild with AY (no password for PDF files)
    $requestBody = New-Object System.IO.MemoryStream
    $requestBody.Write($filePartBytes, 0, $filePartBytes.Length)
    $requestBody.Write($fileData, 0, $fileData.Length)
    $requestBody.Write($ayPartBytes, 0, $ayPartBytes.Length)
    $requestBody.Write($endBoundary, 0, $endBoundary.Length)
    
    $headers["Content-Type"] = "multipart/form-data; boundary=$boundary"
    
    $uploadResp = Invoke-RestMethod -Uri "$API/imports/form26as/$clientId" -Method POST -Headers $headers -Body $requestBody.ToArray()
    
    Write-Host ""
    Write-Host "=" * 60 -ForegroundColor Cyan
    Write-Host "RESULT" -ForegroundColor Yellow
    Write-Host "=" * 60 -ForegroundColor Cyan
    
    Write-Host "Imported: $($uploadResp.imported)" -ForegroundColor Green
    Write-Host "Skipped: $($uploadResp.skipped)" -ForegroundColor Yellow
    Write-Host "Errors: $($uploadResp.errors.Count)" -ForegroundColor $(if ($uploadResp.errors.Count -eq 0) { "Green" } else { "Red" })
    
    if ($uploadResp.extracted) {
        $ext = $uploadResp.extracted
        Write-Host ""
        Write-Host "Extracted Data:" -ForegroundColor Cyan
        Write-Host "  Assessment Year: $($ext.assessment_year)" -ForegroundColor White
        Write-Host "  PAN: $($ext.header.pan)" -ForegroundColor White
        Write-Host "  Name: $($ext.header.name)" -ForegroundColor White
        
        $parts = $ext.parts
        Write-Host ""
        Write-Host "Parts Found:" -ForegroundColor Cyan
        Write-Host "  Part 1 (TDS Deductors): $($parts.part1_tds_deductors)" -ForegroundColor White
        Write-Host "  Part 2 (TDS 15G/15H): $($parts.part2_tds_15g15h)" -ForegroundColor White
        Write-Host "  Part 3 (S.194B/R/S): $($parts.part3_transactions_194b_194r_194s_194ba)" -ForegroundColor White
        Write-Host "  Part 4 (S.194IA/IB/M): $($parts.part4_tds_194ia_194ib_194m_194s_seller)" -ForegroundColor White
        Write-Host "  Part 5 (26QE Seller): $($parts.part5_transactions_26qe_seller)" -ForegroundColor White
        Write-Host "  Part 6 (TCS): $($parts.part6_tcs_collectors)" -ForegroundColor White
        Write-Host "  Part 7 (Refunds): $($parts.part7_paid_refunds)" -ForegroundColor White
        Write-Host "  Part 8 (TDS Sold Property): $($parts.part8_tds_194ia_seller)" -ForegroundColor White
        Write-Host "  Part 9 (TDS Rent): $($parts.part9_tds_194m_tenant)" -ForegroundColor White
        Write-Host "  Part 10 (Salary): $($parts.part10_salary)" -ForegroundColor White
        
        if ($ext.deductors) {
            Write-Host ""
            Write-Host "Deductors:" -ForegroundColor Cyan
            $totalTds = 0
            foreach ($d in $ext.deductors) {
                Write-Host "  $($d.sr_no). $($d.deductor_name)" -ForegroundColor White
                Write-Host "     TAN: $($d.tan) | Amount: Rs.$($d.total_amount | ForEach-Object { '{0:N2}' -f $_ }) | TDS: Rs.$($d.total_tds | ForEach-Object { '{0:N2}' -f $_ })" -ForegroundColor Gray
                if ($d.transactions) {
                    Write-Host "     Transactions: $($d.transactions.Count)" -ForegroundColor Gray
                }
                $totalTds += $d.total_tds
            }
            Write-Host ""
            Write-Host "Total TDS: Rs.$($totalTds | ForEach-Object { '{0:N2}' -f $_ })" -ForegroundColor Green
        }
    }
    
} catch {
    Write-Host ""
    Write-Host "ERROR: $_" -ForegroundColor Red
    Write-Host "Details: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "HTTP Status: $statusCode" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "Test Complete!" -ForegroundColor Yellow
Write-Host "=" * 60 -ForegroundColor Cyan
