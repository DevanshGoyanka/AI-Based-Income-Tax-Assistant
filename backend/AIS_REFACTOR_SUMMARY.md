# AIS Import Refactor - Complete Rewrite

**Date:** 2026-07-10  
**Status:** ✅ Complete  
**Architecture:** Hexagonal/Clean Architecture

---

## What Was Done

Completely rewrote the AIS (Annual Information Statement) import system from scratch following the hexagonal architecture principles documented in `docs/TO_OPENTAX/`.

### Files Created

1. **Domain Model** - `app/core/domain/canonical_ais.py`
   - `CanonicalAIS` - Root aggregate
   - `CanonicalAISTDS` - TDS records
   - `CanonicalAISSFT` - Specified Financial Transactions
   - `CanonicalAISTaxPayment` - Tax payment records
   - `CanonicalAISDemandRefund` - Demand/refund records
   - `CanonicalAISPersonalInfo` - Personal information

2. **Parsers** - `app/core/parsers/`
   - `ais_json_parser.py` - Decrypts and parses encrypted AIS JSON
   - `ais_pdf_parser.py` - Decrypts and parses password-protected AIS PDF

3. **Use Case** - `app/core/use_cases/import_ais.py`
   - `ImportAISUseCase` - Orchestrates import for both JSON and PDF

4. **API Endpoint** - `app/api/v1/imports.py`
   - New unified `/imports/ais/{client_id}` endpoint (handles both JSON and PDF)

### Files Deleted

- `app/services/importers/ais_pdf.py`
- `app/services/ais_pdf_parser.py`
- `app/services/ais_organizer.py`
- `app/services/ais_parser.py`

### Files Updated

- `app/api/v1/imports.py` - Replaced old AIS PDF endpoint with unified AIS endpoint
- `app/api/v1/prefill.py` - Updated to use new `AISJSONParser`

---

## Architecture Compliance

### Hexagonal Architecture Layers

```
┌─────────────────────────────────────────────┐
│  API Layer (FastAPI)                        │
│  - POST /imports/ais/{client_id}            │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Use Case Layer                             │
│  - ImportAISUseCase                         │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Domain Layer (Canonical Models)            │
│  - CanonicalAIS (aggregate)                 │
│  - CanonicalAISTDS                          │
│  - CanonicalAISSFT                          │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  Parser Layer (Adapters)                    │
│  - AISJSONParser                            │
│  - AISPDFParser                             │
└─────────────────────────────────────────────┘
```

### Key Principles Applied

1. **Domain-Driven Design**: Canonical domain models at the core
2. **Dependency Inversion**: Use cases depend on domain abstractions
3. **Single Responsibility**: Each parser handles one format
4. **Clean Separation**: No infrastructure concerns in domain layer

---

## Features

### JSON Import
- Decrypts AES-256-CBC encrypted AIS JSON files
- Password: DOB in DDMMYYYY format
- Parses all AIS sections: TDS, SFT, Tax Payments, Demands/Refunds

### PDF Import
- Decrypts password-protected AIS PDFs
- Password: PAN (lowercase) + DOB (DDMMYYYY)
- Extracts tables using pdfplumber
- Parses personal info and all transaction tables

### Unified API
```bash
POST /imports/ais/{client_id}
Content-Type: multipart/form-data

file: <ais.json or ais.pdf>
password: <DDMMYYYY>
pan: <PANXXXXXX> (required for PDF)
```

---

## Testing

### Fixture Test
```bash
python backend/scripts/test_ais_fixture.py
```

**Result:** ✅ Successfully parsed fixture AIS JSON
- PAN: XYZAB5678C
- TDS Records: 2 (Rs.180,000 total)
- SFT Records: 3 (Interest: 2, Dividend: 1)

### Real File Testing
For real AIS files, DOB passwords are required from `WINAMN DATA.xlsx`.

---

## Migration Notes

### Breaking Changes
- Old endpoints removed (replaced with unified endpoint)
- Old parser classes no longer available
- Response structure changed to use canonical model

### For Existing Code
If code references old parsers:
```python
# OLD (deleted)
from app.services.ais_parser import parse_ais_json

# NEW
from app.core.parsers.ais_json_parser import AISJSONParser
parser = AISJSONParser()
canonical = parser.parse(file, password)
```

### Database Impact
- No schema changes required
- `AISData.raw_json` still stores decrypted JSON
- `AISData.parsed_summary` updated with new structure

---

## Next Steps

1. **Integration with Filing**: Map `CanonicalAIS` → `Schedule` entities
2. **Deduplication**: Implement smart merge logic for multiple AIS imports
3. **Validation**: Add ITD schema validation for parsed data
4. **Testing**: Write integration tests with real encrypted files
5. **UI**: Update frontend to use new unified endpoint

---

## Benefits

1. **Clean Architecture**: Easy to swap parsers or add new formats
2. **Testable**: Domain logic isolated from infrastructure
3. **Maintainable**: Single responsibility per class
4. **Extensible**: Easy to add Form 26AS, TIS, or other formats
5. **Type Safe**: Full type hints with dataclasses

---

**Implementation Time:** ~2 hours  
**Lines of Code:** ~600 (domain + parsers + use case)  
**Test Coverage:** Fixture test passing, needs real file tests
