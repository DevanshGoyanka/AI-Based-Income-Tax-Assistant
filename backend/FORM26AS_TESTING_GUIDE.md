# Form 26AS Upload Testing Guide

## Prerequisites

1. **PostgreSQL Database** running on localhost:5432
   - Database: `itr_filing_db`
   - User: `postgres`
   - Password: `Dsg@1234`

2. **Test User Account**
   - Email: `test@example.com`
   - Password: `Test123!@#`

---

## Option 1: Run Backend with Docker Compose (Recommended)

### Start all services:
```powershell
cd C:\Users\Devansh\Desktop\ITR-FilingWebsite-main
docker-compose up -d
```

### View logs:
```powershell
docker-compose logs -f backend
```

### Stop services:
```powershell
docker-compose down
```

---

## Option 2: Run Backend Locally (Without Docker)

### Install dependencies:
```powershell
cd C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\backend
pip install -r requirements.txt
```

### Run database migrations:
```powershell
alembic upgrade head
```

### Start the backend:
```powershell
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

---

## Testing Form 26AS Upload

### Using the provided test script:

1. **Edit the test script** - Update these variables at the top:
```powershell
$PDF_FILE = "C:\path\to\your\form26as.pdf"
$ZIP_FILE = "C:\path\to\your\form26as.zip"
```

2. **Run the test script:**
```powershell
cd C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\backend
.\test_form26as_upload.ps1
```

### Manual Testing with curl:

#### 1. Login and get token:
```powershell
$BACKEND = "http://localhost:8001/api/v1"

# Login
$login = Invoke-RestMethod -Uri "$BACKEND/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"test@example.com","password":"Test123!@#"}'
$token = $login.access_token
```

#### 2. Get a client ID:
```powershell
$clients = Invoke-RestMethod -Uri "$BACKEND/clients" -Method GET -Headers @{Authorization="Bearer $token"}
$clientId = $clients[0].id
Write-Host "Using Client ID: $clientId"
```

#### 3. Upload Form 26AS PDF:
```powershell
# PDF password format: DDMMYYYY (e.g., 08022002 for 08-02-2002)
$dob = "08022002"

$pdf = Invoke-RestMethod -Uri "$BACKEND/imports/form26as/$clientId" -Method POST -Headers @{Authorization="Bearer $token"} -Form @{
    file = Get-Item "C:\path\to\form26as.pdf"
    dob = $dob
    file_type = "pdf"
}
$pdf | ConvertTo-Json -Depth 5
```

#### 4. Upload Form 26AS ZIP:
```powershell
# ZIP password format: DDMMYYYY (same as PDF)
$dob = "08022002"

$zip = Invoke-RestMethod -Uri "$BACKEND/imports/form26as/$clientId" -Method POST -Headers @{Authorization="Bearer $token"} -Form @{
    file = Get-Item "C:\path\to\form26as.zip"
    dob = $dob
    file_type = "zip"
}
$zip | ConvertTo-Json -Depth 5
```

#### 5. View imported data:
```powershell
# Get all Form 26AS records for the client
$form26as = Invoke-RestMethod -Uri "$BACKEND/imports/form26as/$clientId" -Method GET -Headers @{Authorization="Bearer $token"}
$form26as | ConvertTo-Json -Depth 10
```

---

## Python Script for Direct Database Query:

```powershell
cd C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\backend

python -c "
from app.infra.db import SessionLocal
from app.infra.db.models.form26as_data import Form26ASData
from app.infra.db.models.client import Client

db = SessionLocal()
try:
    # Get all Form 26AS records
    records = db.query(Form26ASData).all()
    print(f'Found {len(records)} Form 26AS records')
    for r in records:
        print(f'  Client: {r.client_id}, AY: {r.ay}, PAN: {r.pan}')
        print(f'  Summary: {r.parsed_summary}')
finally:
    db.close()
"
```

---

## Troubleshooting

### "Connection refused" error
- Backend is not running. Start it with: `docker-compose up -d` or `uvicorn app.main:app --port 8001`

### "Authentication error"
- Token expired or invalid. Re-run login step.

### "PDF/ZIP password incorrect"
- Form 26AS files are password-protected with your DOB in DDMMYYYY format
- Example: DOB 08-Feb-2002 → Password: `08022002`

### "Database connection error"
- Check PostgreSQL is running: `docker ps` (if using Docker)
- Or verify local PostgreSQL: `pg_isready -h localhost -p 5432`

### Check backend health:
```powershell
Invoke-RestMethod -Uri "http://localhost:8001/api/v1/health" -Method GET
```

---

## Expected Response Format:

```json
{
  "imported": 1,
  "skipped": 0,
  "errors": [],
  "extracted": {
    "assessment_year": "2026-2027",
    "header": {
      "pan": "COVPC5929M",
      "name": "ASSESSEE NAME",
      "financial_year": "2025-2026"
    },
    "parts": {
      "part1_tds_deductors": 2,
      "part2_tds_15g15h": 0,
      "part3_transactions_194b_194r_194s_194ba": 0,
      "part4_tds_194ia_194ib_194m_194s_seller": 0,
      "part5_transactions_26qe_seller": 0,
      "part6_tcs_collectors": 0,
      "part7_paid_refunds": 0,
      "part8_tds_194ia_194ib_194m_194s_buyer": 0,
      "part9_transactions_26qe_buyer": 0,
      "part10_tds_tcs_defaults": 0
    },
    "totals": {
      "tds_deductors_count": 2,
      "total_tds_deducted": 207555.00,
      "total_tds_deposited": 207555.00,
      "total_amount_paid_credited": 2189285.60
    },
    "tds_deductors_count": 2,
    "total_tds_deducted": 207555.00,
    "total_amount": 2189285.60
  }
}
```
