# API Specification

**Version:** 1.0  
**Base URL:** `/api/v1`  
**Protocol:** REST + JSON  
**Authentication:** JWT Bearer Token

---

## Authentication

### POST /auth/login
Login with email and password.

**Request:**
```json
{
  "email": "ca@example.com",
  "password": "SecurePass123"
}
```

**Response:**
```json
{
  "token": "eyJhbGc...",
  "refresh_token": "refresh_eyJhbGc...",
  "user": {
    "id": "uuid",
    "email": "ca@example.com",
    "role": "CA",
    "full_name": "John Doe"
  }
}
```

### POST /auth/refresh
Refresh access token.

**Request:**
```json
{
  "refresh_token": "refresh_eyJhbGc..."
}
```

**Response:**
```json
{
  "token": "new_eyJhbGc..."
}
```

---

## Clients

### GET /clients
List all clients (paginated).

**Query Parameters:**
- `page` (default: 1)
- `per_page` (default: 50)
- `search` (PAN or name)
- `ay` (filter by assessment year)

**Response:**
```json
{
  "clients": [
    {
      "id": "uuid",
      "pan": "ABCDE1234F",
      "name": "Client Name",
      "dob": "1990-01-15",
      "email": "client@example.com",
      "mobile": "9876543210",
      "assigned_user_id": "uuid",
      "created_at": "2026-01-01T00:00:00Z"
    }
  ],
  "total": 150,
  "page": 1,
  "per_page": 50
}
```

### POST /clients
Create new client.

**Request:**
```json
{
  "pan": "ABCDE1234F",
  "name": "Client Name",
  "dob": "1990-01-15",
  "email": "client@example.com",
  "mobile": "9876543210",
  "address": {
    "line1": "123 Main St",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400001"
  }
}
```

### PUT /clients/{id}
Update client.

### DELETE /clients/{id}
Delete client (soft delete).

---

## Filings

### GET /filings/{client_id}
Get all filings for a client.

**Response:**
```json
{
  "filings": [
    {
      "id": "uuid",
      "client_id": "uuid",
      "ay": "2026-27",
      "regime": "new",
      "status": "COMPUTED",
      "created_at": "2026-01-01T00:00:00Z"
    }
  ]
}
```

### POST /filings
Create new filing.

**Request:**
```json
{
  "client_id": "uuid",
  "ay": "2026-27",
  "regime": "new"
}
```

### GET /filings/{id}
Get filing details.

**Response:**
```json
{
  "id": "uuid",
  "client_id": "uuid",
  "ay": "2026-27",
  "regime": "new",
  "status": "COMPUTED",
  "income": {
    "salary": [...],
    "house_property": [...],
    "capital_gains": [...],
    "other_sources": {...}
  },
  "deductions": {
    "section_80c": [...],
    "section_80d": {...}
  },
  "tds": [...],
  "computed_return": {
    "total_income": 1425000,
    "total_tax": 117000,
    "refund": 33000
  }
}
```

### PUT /filings/{id}
Update filing data.

---

## Computation

### POST /computation/calculate
Compute tax for a filing.

**Request:**
```json
{
  "filing_id": "uuid",
  "regime": "new"
}
```

**Response:**
```json
{
  "filing_id": "uuid",
  "regime": "new",
  "gross_total_income": 1500000,
  "total_deductions": 75000,
  "total_income": 1425000,
  "tax_before_rebate": 112500,
  "rebate_87a": 0,
  "surcharge": 0,
  "cess": 4500,
  "total_tax_liability": 117000,
  "tds": 150000,
  "tax_payable": 0,
  "refund": 33000,
  "interest": {
    "234a": 0,
    "234b": 0,
    "234c": 0
  },
  "slab_breakdown": [
    {
      "from": 0,
      "to": 400000,
      "rate": 0,
      "taxable": 400000,
      "tax": 0
    },
    {
      "from": 400000,
      "to": 800000,
      "rate": 5,
      "taxable": 400000,
      "tax": 20000
    }
  ]
}
```

### POST /computation/validate
Validate filing before submission.

**Request:**
```json
{
  "filing_id": "uuid"
}
```

**Response:**
```json
{
  "success": true,
  "errors": [
    {
      "field": "person.pan",
      "message": "Invalid PAN format",
      "severity": "BLOCKING",
      "itd_error_code": "PAN001"
    }
  ],
  "total_errors": 1
}
```

---

## Documents

### POST /documents/upload
Upload document (AIS/26AS/Form 16).

**Request:** multipart/form-data
- `file`: PDF/JSON/ZIP file
- `client_id`: UUID
- `ay`: Assessment year
- `doc_type`: "AIS" | "26AS" | "FORM16" | "PREFILL"

**Response:**
```json
{
  "document_id": "uuid",
  "filename": "ais_2026.pdf",
  "size_bytes": 245678,
  "status": "UPLOADED"
}
```

### POST /documents/{id}/parse
Parse uploaded document (async).

**Response:**
```json
{
  "task_id": "uuid",
  "status": "PROCESSING"
}
```

### GET /documents/{id}/status
Check parsing status.

**Response:**
```json
{
  "document_id": "uuid",
  "status": "COMPLETED",
  "parsed_data": {
    "salary": [...],
    "tds": [...],
    "interest": [...]
  },
  "merge_report": {
    "new_records": 15,
    "updated_records": 3,
    "duplicates_skipped": 2
  }
}
```

---

## Imports

### POST /imports/ais
Import AIS data.

**Request:** multipart/form-data
- `file`: AIS JSON or PDF
- `password`: Decryption password
- `client_id`: UUID
- `ay`: Assessment year

### POST /imports/form26as
Import Form 26AS.

**Request:** multipart/form-data
- `file`: 26AS ZIP or PDF
- `password`: DOB in DDMMYYYY format
- `client_id`: UUID
- `ay`: Assessment year

### POST /imports/bulk
Bulk import clients from Excel.

**Request:** multipart/form-data
- `file`: Excel file (.xlsx)

**Response:**
```json
{
  "job_id": "uuid",
  "status": "PROCESSING",
  "total_rows": 100
}
```

### GET /imports/bulk/{job_id}
Check bulk import status.

**Response:**
```json
{
  "job_id": "uuid",
  "status": "COMPLETED",
  "total_rows": 100,
  "success_count": 95,
  "failed_count": 5,
  "errors": [
    {
      "row": 12,
      "error": "Invalid PAN format"
    }
  ]
}
```

---

## Snapshots

### GET /snapshots/{filing_id}
Get all snapshots for a filing.

**Response:**
```json
{
  "snapshots": [
    {
      "id": "uuid",
      "filing_id": "uuid",
      "snapshot_type": "auto",
      "rule_version": "AY-2026-27",
      "created_at": "2026-01-01T10:00:00Z",
      "payload": {...}
    }
  ]
}
```

### POST /snapshots
Create manual snapshot.

**Request:**
```json
{
  "filing_id": "uuid",
  "snapshot_type": "manual",
  "note": "Before final submission"
}
```

### GET /snapshots/diff/{id1}/{id2}
Compare two snapshots.

**Response:**
```json
{
  "changes": [
    {
      "field": "deductions.section_80c",
      "old_value": 150000,
      "new_value": 140000
    }
  ]
}
```

---

## Dashboard

### GET /dashboard/summary
Get dashboard summary.

**Response:**
```json
{
  "total_clients": 250,
  "total_filings": 180,
  "filings_by_status": {
    "draft": 50,
    "computed": 80,
    "submitted": 40,
    "filed": 10
  },
  "filings_by_ay": {
    "2025-26": 100,
    "2026-27": 80
  },
  "total_refunds": 12500000,
  "total_tax_payable": 8500000
}
```

---

## Error Responses

All errors follow this format:

```json
{
  "error": "ErrorType",
  "message": "Human-readable message",
  "details": {...},
  "timestamp": "2026-01-01T00:00:00Z"
}
```

**HTTP Status Codes:**
- 400: Bad Request (validation failed)
- 401: Unauthorized (invalid/expired token)
- 403: Forbidden (insufficient permissions)
- 404: Not Found
- 422: Unprocessable Entity (business logic error)
- 500: Internal Server Error

---

## Rate Limiting

- 100 requests/minute per user
- 1000 requests/hour per user
- Bulk operations count as 1 request

**Response Headers:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1609459200
```

**Exceeded:**
```json
{
  "error": "RateLimitExceeded",
  "message": "Rate limit exceeded. Try again in 30 seconds.",
  "retry_after": 30
}
```

---

## Pagination

All list endpoints support pagination:

**Query Parameters:**
- `page`: Page number (1-indexed)
- `per_page`: Items per page (max 100)
- `sort`: Sort field
- `order`: "asc" | "desc"

**Response:**
```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "per_page": 50,
    "total": 250,
    "total_pages": 5,
    "has_next": true,
    "has_prev": false
  }
}
```

---

## Webhooks (Future)

### POST /webhooks
Register webhook.

**Request:**
```json
{
  "url": "https://example.com/webhook",
  "events": ["filing.computed", "filing.submitted"],
  "secret": "webhook_secret"
}
```

**Webhook Payload:**
```json
{
  "event": "filing.computed",
  "filing_id": "uuid",
  "timestamp": "2026-01-01T00:00:00Z",
  "data": {...}
}
```

---

**API Version:** v1  
**Last Updated:** 2026-07-10
