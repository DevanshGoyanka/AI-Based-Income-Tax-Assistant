# Form 26AS Test Script - Run this file directly
# Save and run: .\run_form26as_test.ps1

$ErrorActionPreference = "Stop"

$BACKEND = "http://localhost:8001/api/v1"
$EMAIL = "test@example.com"
$PASSWORD = "Test123!@#"
$CLIENT_ID = "2fe92273-a57f-4b2b-8867-d6b2916c6680"  # YASH UMESH CHANDAK

Write-Host "=== Form 26AS Test Script ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Login
Write-Host "[1/5] Logging in..." -ForegroundColor Yellow
$loginBody = @{
    email = $EMAIL
    password = $PASSWORD
} | ConvertTo-Json -Compress

$loginResult = Invoke-RestMethod -Uri "$BACKEND/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
$token = $loginResult.access_token

if (-not $token) {
    Write-Host "   [ERROR] Login failed: $loginResult" -ForegroundColor Red
    exit 1
}
Write-Host "   [OK] Logged in successfully" -ForegroundColor Green

# Step 2: Use YASH CHANDAK's client
Write-Host "[2/5] Using YASH CHANDAK's client..." -ForegroundColor Yellow
$clientId = $CLIENT_ID
Write-Host "   [OK] Client ID: $clientId" -ForegroundColor Green

# Step 3: Ask for file path
Write-Host ""
Write-Host "[3/5] File Information" -ForegroundColor Yellow
$filePath = Read-Host "   Enter full path to Form 26AS file (PDF or ZIP)"
$filePath = $filePath.Trim('"')

if (-not (Test-Path $filePath)) {
    Write-Host "   [ERROR] File not found: $filePath" -ForegroundColor Red
    exit 1
}

$fileName = Split-Path $filePath -Leaf
$extension = [System.IO.Path]::GetExtension($filePath).ToLower()

if ($extension -eq ".pdf") {
    $fileType = "pdf"
    Write-Host "   Detected: PDF file" -ForegroundColor Cyan
} elseif ($extension -eq ".zip") {
    $fileType = "zip"
    Write-Host "   Detected: ZIP file" -ForegroundColor Cyan
} else {
    Write-Host "   [ERROR] Unsupported file type. Use PDF or ZIP." -ForegroundColor Red
    exit 1
}

# Step 4: Ask for DOB password
Write-Host ""
Write-Host "[4/5] Password (DOB in DDMMYYYY format)" -ForegroundColor Yellow
$dob = Read-Host "   Enter DOB (e.g., 08022002 for 08-Feb-2002)"
if (-not $dob) {
    $dob = "08022002"
    Write-Host "   Using default: $dob" -ForegroundColor Gray
}

# Step 5: Upload file
Write-Host ""
Write-Host "[5/5] Uploading Form 26AS..." -ForegroundColor Yellow
Write-Host "   Client ID: $clientId" -ForegroundColor Gray
Write-Host "   File: $fileName" -ForegroundColor Gray
Write-Host "   File Type: $fileType" -ForegroundColor Gray
Write-Host "   Password: $dob" -ForegroundColor Gray
Write-Host ""

# Use curl.exe for multipart form upload
$tempBat = "$env:TEMP\upload_form26as_$PID.bat"
$tempOut = "$env:TEMP\upload_result_$PID.json"

$curlCmd = "curl.exe -s -X POST `"$BACKEND/imports/form26as/$clientId`" -H `"Authorization: Bearer $token`" -F `"file=@$filePath`" -F `"dob=$dob`" -F `"file_type=$fileType`" -o `"$tempOut`" 2>&1"
$curlCmd | Out-File -FilePath $tempBat -Encoding ASCII
Invoke-Expression "& $tempBat"

if (Test-Path $tempOut) {
    $uploadResult = Get-Content $tempOut -Raw
    Remove-Item $tempOut -Force -ErrorAction SilentlyContinue
    Remove-Item $tempBat -Force -ErrorAction SilentlyContinue
} else {
    $uploadResult = "Error: No response received"
}

Write-Host ""
Write-Host "=== UPLOAD RESPONSE ===" -ForegroundColor Cyan
Write-Host ""

try {
    $uploadJson = $uploadResult | ConvertFrom-Json
    
    # Pretty print the full response
    $uploadJson | ConvertTo-Json -Depth 20 | Write-Host
    
    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "                         EXTRACTED DATA SUMMARY                                " -ForegroundColor Cyan
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host ""
    
    if ($uploadJson.extracted) {
        $ext = $uploadJson.extracted
        
        # Header Information
        if ($ext.header) {
            Write-Host "HEADER INFORMATION" -ForegroundColor Yellow
            Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
            Write-Host "  PAN:              $($ext.header.pan)"
            Write-Host "  Name:             $($ext.header.name)"
            Write-Host "  Assessment Year:  $($ext.header.assessment_year)"
            Write-Host "  Financial Year:   $($ext.header.financial_year)"
            Write-Host "  Status:           $($ext.header.current_status)"
            Write-Host "  Address:          $($ext.header.address_line_1)"
            Write-Host "  City:             $($ext.header.address_line_4)"
            Write-Host "  State:            $($ext.header.state_code)"
            Write-Host "  PIN Code:         $($ext.header.pin_code)"
            Write-Host ""
        }
        
        # TDS Summary
        if ($null -ne $ext.tds_deductors_count) {
            Write-Host "TDS SUMMARY" -ForegroundColor Yellow
            Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
            Write-Host "  Deductors Count:  $($ext.tds_deductors_count)"
            $tdsStr = "{0:N2}" -f $ext.total_tds_deducted
            Write-Host "  Total TDS:        Rs.$tdsStr"
            $tdsDepStr = "{0:N2}" -f $ext.total_tds_deposited
            Write-Host "  TDS Deposited:    Rs.$tdsDepStr"
            $amtStr = "{0:N2}" -f $ext.total_amount
            Write-Host "  Total Amount:     Rs.$amtStr"
            Write-Host ""
        }
        
        # TDS Entries Details
        if ($ext.tds_entries) {
            Write-Host "TDS ENTRIES ($($ext.tds_entries.Count) deductors)" -ForegroundColor Yellow
            Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
            
            $srNo = 1
            foreach ($entry in $ext.tds_entries) {
                Write-Host ""
                Write-Host "  [$srNo] $($entry.deductor_name)" -ForegroundColor Cyan
                Write-Host "      TAN:              $($entry.tan)"
                $txnAmt = "{0:N2}" -f $entry.total_amount
                Write-Host "      Total Amount:     Rs.$txnAmt"
                $txnTds = "{0:N2}" -f $entry.total_tds
                Write-Host "      Total TDS:       Rs.$txnTds"
                $txnDep = "{0:N2}" -f $entry.total_tds_deposited
                Write-Host "      TDS Deposited:   Rs.$txnDep"
                
                if ($entry.transactions -and $entry.transactions.Count -gt 0) {
                    Write-Host "      Transactions:     $($entry.transactions.Count) entries" -ForegroundColor Gray
                    foreach ($txn in $entry.transactions) {
                        Write-Host "        - Section: $($txn.section), Amount: Rs.$([math]::Round($txn.amount, 2)), TDS: Rs.$([math]::Round($txn.tax_deducted, 2)), Date: $($txn.transaction_date)" -ForegroundColor Gray
                    }
                }
                $srNo++
            }
            Write-Host ""
        }
        
        # Parts Breakdown
        if ($ext.parts) {
            Write-Host "PARTS BREAKDOWN" -ForegroundColor Yellow
            Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
            Write-Host "  Part 1  - TDS Deductors (194/192/193):         $($ext.parts.part1_tds_deductors)"
            Write-Host "  Part 2  - TDS 15G/15H:                         $($ext.parts.part2_tds_15g15h)"
            Write-Host "  Part 3  - 194B/194R/194S/194BA Transactions:   $($ext.parts.part3_transactions_194b_194r_194s_194ba)"
            Write-Host "  Part 4  - 194IA/194IB/194M/194S (Seller):      $($ext.parts.part4_tds_194ia_194ib_194m_194s_seller)"
            Write-Host "  Part 5  - 26QE Seller Transactions:            $($ext.parts.part5_transactions_26qe_seller)"
            Write-Host "  Part 6  - TCS Collectors:                      $($ext.parts.part6_tcs_collectors)"
            Write-Host "  Part 7  - Paid Refunds:                        $($ext.parts.part7_paid_refunds)"
            Write-Host "  Part 8  - 194IA/194IB/194M/194S (Buyer):       $($ext.parts.part8_tds_194ia_194ib_194m_194s_buyer)"
            Write-Host "  Part 9  - 26QE Buyer Transactions:             $($ext.parts.part9_transactions_26qe_buyer)"
            Write-Host "  Part 10 - TDS/TCS Defaults:                    $($ext.parts.part10_tds_tcs_defaults)"
            Write-Host ""
        }
    }
    
    if ($uploadJson.imported -eq 1) {
        Write-Host "================================================================================" -ForegroundColor Green
        Write-Host "                         STATUS: IMPORT SUCCESSFUL                              " -ForegroundColor Green
        Write-Host "================================================================================" -ForegroundColor Green
    } elseif ($uploadJson.errors) {
        Write-Host "================================================================================" -ForegroundColor Red
        Write-Host "                         STATUS: IMPORT FAILED                                  " -ForegroundColor Red
        Write-Host "================================================================================" -ForegroundColor Red
        Write-Host "Errors:" -ForegroundColor Red
        $uploadJson.errors | ForEach-Object { Write-Host "   - $_" -ForegroundColor Red }
    }
    
} catch {
    Write-Host "Raw Response:" -ForegroundColor White
    Write-Host $uploadResult -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== TEST COMPLETE ===" -ForegroundColor Green
