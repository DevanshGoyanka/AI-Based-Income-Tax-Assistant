# Risk Register

**Version:** 1.0  
**Last Updated:** 2026-07-10  
**Owner:** Technical Architect

---

## High-Priority Risks

### R001: OpenTax Breaking Changes
**Category:** Technical  
**Probability:** Medium (40%)  
**Impact:** High  
**Risk Score:** 8/10

**Description:**  
OpenTax is community OSS. API changes could break adapter layer.

**Indicators:**
- New OpenTax release published
- Breaking changes in tax_calculation_service.py
- Changed Pydantic model structure

**Mitigation:**
1. Pin to specific commit SHA (not version tag)
2. Monitor OpenTax repo for releases
3. Test updates in staging before production
4. Maintain fork for emergency patches
5. Quarterly review of OpenTax updates

**Contingency:**
- Freeze OpenTax version indefinitely
- Fork permanently and maintain ourselves
- Estimated effort: 40 hours/year maintenance

---

### R002: Tax Rule Changes Mid-Year
**Category:** Legal/Compliance  
**Probability:** Low (10%)  
**Impact:** Critical  
**Risk Score:** 7/10

**Description:**  
Government announces ITR schema or tax rule changes after system is live.

**Indicators:**
- Finance Act amendments
- ITD notifications
- New circular from CBDT

**Mitigation:**
1. Rule engine versioned by AY
2. Backward compatibility built-in
3. Hot-swap rule updates without deployment
4. Subscribe to ITD notifications
5. Legal team reviews monthly

**Contingency:**
- Emergency rule update process (2-hour SLA)
- Manual override mechanism for CA
- Roll back to previous AY rules

---

### R003: Database Migration Failure
**Category:** Technical  
**Probability:** Low (15%)  
**Impact:** High  
**Risk Score:** 6/10

**Description:**
Adding foreign keys to production database fails or locks tables.

**Indicators:**
- Migration timeout
- Deadlock detected
- Constraint violation errors

**Mitigation:**
1. Test migrations on production copy
2. Create FKs with NOT VALID initially
3. Validate constraints async
4. Use blue-green deployment
5. Backup before migration

**Contingency:**
- Immediate rollback via Alembic downgrade
- Restore from backup (< 5 min)
- Manual cleanup of orphaned records

---

### R004: ITR JSON Portal Rejection
**Category:** Compliance  
**Probability:** Medium (30%)  
**Impact:** Critical  
**Risk Score:** 9/10

**Description:**
Generated ITR JSON fails ITD portal validation, blocking all filings.

**Indicators:**
- ITD returns error on upload
- Schema validation failure
- Missing required fields

**Mitigation:**
1. Validate against ITD schema before submit
2. Test with ITD test portal weekly
3. Golden test cases cover edge cases
4. Pilot with 10 clients before rollout
5. OpenTax community validation

**Contingency:**
- Manual JSON editing by CA
- Export to ClearTax/other tool
- Rollback to previous version
- Emergency fix within 24 hours

---

### R005: Performance Degradation at Scale
**Category:** Technical  
**Probability:** Medium (35%)  
**Impact:** Medium  
**Risk Score:** 6/10

**Description:**
System slows down with >500 clients or during peak filing season.

**Indicators:**
- API response time >500ms
- Database query time >200ms
- CPU usage >80%
- Memory usage >85%

**Mitigation:**
1. Load testing before production (100 users)
2. Database query optimization
3. Redis caching layer
4. Connection pooling (20 connections)
5. Async processing for heavy tasks

**Contingency:**
- Horizontal scaling (add more workers)
- Upgrade database instance
- Rate limiting per user
- Queue non-critical tasks

---

## Medium-Priority Risks

### R006: Duplicate Data Import
**Category:** Data Quality  
**Probability:** Medium (40%)  
**Impact:** Medium  
**Risk Score:** 5/10

**Description:**
AIS/26AS imported multiple times creates duplicate TDS records.

**Mitigation:**
- Canonical normalizer with duplicate detection
- Unique constraints on (client_id, ay, source)
- Merge logic based on TAN + section + amount
- Warning UI when duplicate detected

**Contingency:**
- Manual deduplication by CA
- SQL cleanup script
- Rollback import operation

---

### R007: Security Breach
**Category:** Security  
**Probability:** Low (20%)  
**Impact:** Critical  
**Risk Score:** 7/10

**Description:**
Unauthorized access to client data or PAN information.

**Mitigation:**
- JWT authentication with 30-min expiry
- RBAC with least privilege
- Encrypted PAN storage
- Audit trail for all access
- Regular security audits

**Contingency:**
- Immediate password reset all users
- Revoke all JWT tokens
- Notify affected clients (GDPR)
- Forensic investigation

---

### R008: OpenTax Computation Error
**Category:** Technical  
**Probability:** Low (15%)  
**Impact:** High  
**Risk Score:** 6/10

**Description:**
OpenTax produces incorrect tax calculation, not caught by validation.

**Mitigation:**
- Golden test cases (20 scenarios)
- Parallel computation with existing system
- CA review before submission
- Sanity checks (cess = 4% of tax+surcharge)
- Community peer review

**Contingency:**
- Manual calculation override
- Rollback to previous computation
- Report issue to OpenTax community
- Emergency patch if critical

---

### R009: Vendor Lock-in to OpenTax
**Category:** Strategic  
**Probability:** Medium (30%)  
**Impact:** Medium  
**Risk Score:** 5/10

**Description:**
Deep dependency on OpenTax makes migration difficult.

**Mitigation:**
- Adapter pattern isolates OpenTax
- ITaxEngine interface allows swapping
- Canonical model decoupled from OpenTax
- Document integration points
- Annual evaluation of alternatives

**Contingency:**
- Build custom tax engine (400 hours)
- Migrate to commercial solution
- Hybrid approach (OpenTax + custom)

---

### R010: CA Training Resistance
**Category:** People  
**Probability:** Medium (35%)  
**Impact:** Medium  
**Risk Score:** 5/10

**Description:**
CAs resist new system, prefer manual Excel workflow.

**Mitigation:**
- Involve CAs in pilot
- Comprehensive training materials
- In-person training sessions
- Gradual rollout (10 clients → 50 → all)
- Show time savings (5 min vs 30 min)

**Contingency:**
- Extended parallel run with old system
- Dedicated support hotline
- On-site assistance during filing season

---

## Low-Priority Risks

### R011: Frontend Browser Compatibility
**Probability:** Low (15%)  
**Impact:** Low  
**Risk Score:** 2/10

**Mitigation:** Test on Chrome, Firefox, Safari, Edge

### R012: Third-Party API Downtime
**Probability:** Low (10%)  
**Impact:** Medium  
**Risk Score:** 3/10

**Mitigation:** Queue requests, retry with exponential backoff

### R013: Data Loss During Import
**Probability:** Low (5%)  
**Impact:** High  
**Risk Score:** 4/10

**Mitigation:** Atomic transactions, snapshot before import

### R014: Insufficient Documentation
**Probability:** Medium (30%)  
**Impact:** Low  
**Risk Score:** 3/10

**Mitigation:** Inline code docs, API docs, user guide

### R015: Dependency Vulnerabilities
**Probability:** Medium (40%)  
**Impact:** Medium  
**Risk Score:** 5/10

**Mitigation:** Dependabot alerts, monthly security updates

---

## Risk Matrix

```
Impact
  │
C │     R004 R007
r │     R002
i │
t │ R001 R003 R008
i │     R009 R005
c │
a │ R006 R010
l │
  │
M │     R013 R012 R015
e │
d │ R014
i │
u │
m │ R011
  │
L │
o │
w │
  └─────────────────────────
    Low  Med  High  Critical
         Probability
```

---

## Risk Review Schedule

**Weekly:** Review high-priority risks (R001-R005)  
**Monthly:** Review all risks, update probabilities  
**Quarterly:** Deep dive, lessons learned  
**Annual:** Complete risk assessment refresh

---

## Escalation Path

**Severity 1 (Critical):** Immediate escalation to CTO  
**Severity 2 (High):** Escalate within 4 hours  
**Severity 3 (Medium):** Escalate within 24 hours  
**Severity 4 (Low):** Handle in normal workflow

---

**Status:** ACTIVE MONITORING  
**Next Review:** 2026-07-17
