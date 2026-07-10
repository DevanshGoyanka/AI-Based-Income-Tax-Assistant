# Migration Plan

**Version:** 1.0  
**Timeline:** 12 weeks  
**Risk Level:** Medium

---

## Overview

Migrate from current ERP system to OpenTax-integrated architecture with zero downtime and full data preservation.

---

## Pre-Migration Assessment

### Current State Inventory
1. **Database:** PostgreSQL with 487 tables/views
2. **Active Users:** 25 CAs, 150 clients
3. **Data Volume:** 2.5GB database, 15GB documents
4. **Current AYs:** 2024-25, 2025-26, 2026-27
5. **Filed Returns:** 450 (must preserve)

### Migration Scope
- ✅ All client records (PAN, DOB, contact)
- ✅ All filed returns (immutable)
- ✅ All import data (AIS, 26AS, Form 16)
- ✅ All TDS records
- ✅ All documents (PDFs, JSONs)
- ⚠️ Draft filings (validate/discard invalid)
- ❌ Old debug/test data (clean up)

---

## Migration Strategy

### Approach: Blue-Green Deployment

```
Current System (Blue)
    │
    ├─── Phase 1-5: Build Green in parallel
    │
    ▼
Green System Ready
    │
    ├─── Phase 6: Pilot (10 clients on Green)
    │
    ├─── Phase 7: Data sync Blue→Green (nightly)
    │
    ▼
Cutover Weekend
    │
    ├─── Friday 6pm: Freeze Blue (read-only)
    ├─── Saturday: Final data sync
    ├─── Sunday: Switch DNS to Green
    │
    ▼
Green System Live
    │
    └─── Blue kept as backup (7 days)
```

---

## Phase 1: Schema Migration (Week 7)

**Objective:** Update database schema with new tables/constraints.

### Tasks
1. **Backup production database**
   ```bash
   pg_dump -Fc production_db > backup_pre_migration.dump
   ```

2. **Apply migrations in staging**
   ```bash
   alembic upgrade head
   ```

3. **Test with production copy**
   ```bash
   # Restore production copy to staging
   pg_restore -d staging_db backup_pre_migration.dump
   
   # Run migrations
   alembic upgrade head
   
   # Validate data integrity
   python scripts/validate_migration.py
   ```

4. **Apply to production (maintenance window)**
   - Schedule: Saturday 2am-4am
   - Downtime: <30 minutes
   - Rollback plan: Restore from backup

### New Tables Created
- `rule_versions`
- `filing_snapshots_metadata`

### Modified Tables
- Add foreign keys to all tables
- Add unique constraints
- Add composite indexes

### Validation Queries
```sql
-- Verify all foreign keys
SELECT COUNT(*) FROM information_schema.table_constraints 
WHERE constraint_type = 'FOREIGN KEY';

-- Check for orphaned records
SELECT COUNT(*) FROM itr_filings f
LEFT JOIN clients c ON f.client_id = c.id
WHERE c.id IS NULL;

-- Verify unique constraints
SELECT COUNT(*) FROM information_schema.table_constraints 
WHERE constraint_type = 'UNIQUE';
```

---

## Phase 2: Data Cleaning (Week 8)

**Objective:** Clean up invalid/duplicate data before migration.

### Tasks

1. **Remove duplicate AIS imports**
   ```sql
   -- Find duplicates
   SELECT client_id, ay, COUNT(*) 
   FROM ais_data 
   GROUP BY client_id, ay 
   HAVING COUNT(*) > 1;
   
   -- Keep latest, delete rest
   DELETE FROM ais_data a1
   USING ais_data a2
   WHERE a1.client_id = a2.client_id
     AND a1.ay = a2.ay
     AND a1.imported_at < a2.imported_at;
   ```

2. **Fix invalid PANs**
   ```sql
   -- Find invalid PAN formats
   SELECT id, pan FROM clients 
   WHERE pan !~ '^[A-Z]{5}[0-9]{4}[A-Z]$';
   
   -- Manual review required
   ```

3. **Consolidate TDS records**
   ```sql
   -- Merge duplicate TDS by TAN+section+amount
   WITH duplicates AS (
     SELECT 
       client_id, ay, deductor_tan, section_code, 
       SUM(tax_deducted) as total_tds,
       MIN(id) as keep_id
     FROM tds_deductors
     GROUP BY client_id, ay, deductor_tan, section_code
     HAVING COUNT(*) > 1
   )
   UPDATE tds_deductors t
   SET tax_deducted = d.total_tds
   FROM duplicates d
   WHERE t.id = d.keep_id;
   
   DELETE FROM tds_deductors
   WHERE id NOT IN (SELECT keep_id FROM duplicates);
   ```

4. **Archive old draft filings**
   ```sql
   -- Archive drafts older than 2 years
   INSERT INTO archived_filings 
   SELECT * FROM itr_filings
   WHERE status = 'draft' 
     AND created_at < NOW() - INTERVAL '2 years';
   
   DELETE FROM itr_filings
   WHERE status = 'draft' 
     AND created_at < NOW() - INTERVAL '2 years';
   ```

---

## Phase 3: Code Deployment (Week 9)

**Objective:** Deploy new OpenTax-integrated code to staging.

### Tasks

1. **Deploy to staging environment**
   ```bash
   # Build Docker image
   docker build -t itr-erp:green .
   
   # Deploy to staging
   docker-compose -f docker-compose.staging.yml up -d
   
   # Run smoke tests
   pytest tests/integration/ --env=staging
   ```

2. **Configure feature flags**
   ```python
   # settings.py
   ENABLE_OPENTAX = os.getenv("ENABLE_OPENTAX", "false") == "true"
   OPENTAX_PILOT_CLIENTS = [
       "uuid1", "uuid2", "uuid3"  # 10 pilot clients
   ]
   ```

3. **Seed rule versions**
   ```bash
   python scripts/seed_rules.py --ay 2024-25
   python scripts/seed_rules.py --ay 2025-26
   python scripts/seed_rules.py --ay 2026-27
   ```

---

## Phase 4: Data Migration Script (Week 10)

**Objective:** Create automated migration script for production data.

### Migration Script

```python
# scripts/migrate_to_green.py

import asyncio
from sqlalchemy import create_engine
from app.adapters.opentax.model_mapper import ERPToOpenTaxMapper

async def migrate_all():
    """Migrate all data from blue to green schema."""
    
    # 1. Migrate users (no change)
    print("Migrating users...")
    await migrate_users()
    
    # 2. Migrate clients (no change)
    print("Migrating clients...")
    await migrate_clients()
    
    # 3. Migrate filed returns (preserve snapshots)
    print("Migrating filed returns...")
    await migrate_filed_returns()
    
    # 4. Migrate import data (AIS, 26AS)
    print("Migrating import data...")
    await migrate_import_data()
    
    # 5. Migrate documents
    print("Migrating documents...")
    await migrate_documents()
    
    # 6. Recompute draft filings with OpenTax
    print("Recomputing draft filings...")
    await recompute_drafts()
    
    print("Migration complete!")

async def migrate_filed_returns():
    """Migrate filed returns, preserve as immutable snapshots."""
    filed = await get_filed_returns()
    
    for filing in filed:
        # Create snapshot from old computation
        snapshot = ComputedReturn.create(
            filing_id=filing.id,
            regime=filing.regime,
            payload=filing.old_computation_data,
            rule_version=f"LEGACY-{filing.ay}"
        )
        snapshot.is_locked = True
        snapshot.filed_at = filing.filed_at
        snapshot.ack_no = filing.ack_no
        
        await snapshot_repo.save(snapshot)

async def recompute_drafts():
    """Recompute draft filings using OpenTax."""
    drafts = await get_draft_filings()
    
    for filing in drafts:
        try:
            # Compute via OpenTax
            computed = await compute_engine.compute(
                filing, filing.regime
            )
            
            # Save new snapshot
            await snapshot_repo.save(computed)
            
            print(f"✓ Recomputed filing {filing.id}")
        except Exception as e:
            print(f"✗ Failed filing {filing.id}: {e}")
            # Flag for manual review
```

### Run Migration

```bash
# Dry run (validation only)
python scripts/migrate_to_green.py --dry-run

# Actual migration
python scripts/migrate_to_green.py --execute

# Validate results
python scripts/validate_migration.py
```

---

## Phase 5: Pilot Testing (Week 11)

**Objective:** Test green system with 10 pilot clients.

### Pilot Client Selection
- 3 simple (salary only)
- 3 medium (salary + HP)
- 2 complex (salary + HP + CG)
- 2 edge cases (senior citizen, high surcharge)

### Pilot Process

1. **Week 1: Compute & validate**
   - Recompute all pilot returns via OpenTax
   - Compare with old system results
   - CA reviews differences

2. **Week 2: File test returns**
   - Generate ITR JSON via OpenTax
   - Upload to ITD test portal
   - Verify acceptance

3. **Week 3: Gather feedback**
   - CA satisfaction survey
   - Document issues/bugs
   - Fix critical issues

### Success Criteria
- ✅ All 10 returns computed correctly (±₹100 tolerance)
- ✅ All ITR JSONs pass ITD validation
- ✅ Zero portal rejections
- ✅ CA satisfaction ≥8/10

---

## Phase 6: Cutover (Week 12, Weekend)

**Objective:** Switch production traffic from blue to green.

### Friday 6pm: Pre-Cutover
```bash
# 1. Freeze blue system (read-only mode)
UPDATE system_config SET read_only = true;

# 2. Final backup
pg_dump -Fc production_db > final_backup.dump

# 3. Notify all users
# Email: System maintenance Saturday 2am-6am
```

### Saturday 2am: Cutover Window

**Timeline:**
- 2:00am: Start final data sync
- 2:30am: Run migration script
- 3:00am: Validate data integrity
- 3:30am: Smoke tests
- 4:00am: Switch DNS to green
- 4:30am: Monitor logs/metrics
- 5:00am: Declare success or rollback

**Cutover Steps:**
```bash
# 1. Final data sync blue → green
python scripts/sync_blue_to_green.py

# 2. Run validation
python scripts/validate_migration.py
# Expected: 0 errors

# 3. Smoke tests on green
pytest tests/smoke/ --env=production-green

# 4. Switch load balancer
# Point production domain to green servers
# Old: itr-blue.example.com
# New: itr-green.example.com

# 5. Monitor
tail -f /var/log/app/green.log
# Watch for errors

# 6. If success: Update DNS
# production.example.com → itr-green.example.com

# 7. If failure: Rollback
# production.example.com → itr-blue.example.com
```

---

## Phase 7: Post-Cutover (Week 13)

**Objective:** Monitor green, decommission blue.

### Day 1-3: Intensive Monitoring
- Monitor error rates (target <0.1%)
- Monitor API response times (target <200ms p95)
- Watch for data inconsistencies
- Hotline for CA issues

### Day 4-7: Validation
- Verify all computations correct
- Check ITR JSON exports
- Confirm no data loss
- CA satisfaction check

### Day 8+: Blue Decommission
- Keep blue running (read-only) for 7 days
- After 7 days: Shut down blue servers
- Archive blue database for 90 days
- Delete blue after 90 days

---

## Rollback Plan

### Trigger Conditions
- Critical bug affecting >10% of users
- Data corruption detected
- ITR portal rejection rate >5%
- CA unable to work (system unusable)

### Rollback Process
```bash
# 1. Switch DNS back to blue
production.example.com → itr-blue.example.com

# 2. Re-enable writes on blue
UPDATE system_config SET read_only = false;

# 3. Notify users
# Email: System restored, maintenance rescheduled

# 4. Sync data green → blue (if any created on green)
python scripts/sync_green_to_blue.py

# 5. Post-mortem
# Document what went wrong
# Fix issues before retry
```

**Rollback Time:** <15 minutes (DNS switch)

---

## Data Validation Checklist

**Pre-Migration:**
- [ ] All clients have valid PAN
- [ ] No orphaned filings
- [ ] No duplicate TDS records
- [ ] All filed returns have ack_no
- [ ] All documents have valid paths

**Post-Migration:**
- [ ] Row count matches (clients, filings)
- [ ] All filed returns preserved
- [ ] All TDS records migrated
- [ ] Recomputed drafts match old ±₹100
- [ ] All foreign keys valid

**Validation Queries:**
```sql
-- Row counts match
SELECT 'clients', COUNT(*) FROM clients UNION ALL
SELECT 'filings', COUNT(*) FROM itr_filings UNION ALL
SELECT 'tds', COUNT(*) FROM tds_deductors;

-- No orphans
SELECT COUNT(*) FROM itr_filings f
LEFT JOIN clients c ON f.client_id = c.id
WHERE c.id IS NULL;

-- All filed returns have snapshots
SELECT COUNT(*) FROM itr_filings f
LEFT JOIN computed_return_snapshots s ON f.id = s.filing_id
WHERE f.status IN ('submitted', 'filed') AND s.id IS NULL;
```

---

## Communication Plan

### Week Before Cutover
- Email all CAs: Maintenance window scheduled
- Slack announcement: No filing Saturday
- Banner on app: Maintenance Saturday 2am-6am

### Day of Cutover
- 1:55am: Email: Maintenance starting
- 4:00am: Email: Maintenance complete
- 4:30am: Slack: Green system live

### Post-Cutover
- Monday: Survey CAs for feedback
- Weekly: Status update (first month)
- Monthly: Migration retrospective

---

**Migration Status:** PLAN APPROVED  
**Cutover Date:** TBD (Week 12)  
**Risk Assessment:** Medium (Blue-green reduces risk)
