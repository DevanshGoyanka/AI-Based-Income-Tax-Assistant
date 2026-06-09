import json

def parseAmt(s):
    if not s: return 0
    try: return int(float(str(s).replace(',', '').replace('₹', '').replace(' ', '')))
    except: return 0

with open(r'C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\import_prefill\XXXPG3482X_2024-25_AIS_decrypted.json', encoding='utf-8') as f:
    data = json.load(f)

partB = data.get('partB', {})

print('=' * 70)
print('COMPLETE AIS JSON vs DASHBOARD AUDIT')
print('=' * 70)
print()

# Part B2
print('━━━ PART B2 - SFT (ALL categories) ━━━')
for sec in partB.get('sections', []):
    if sec.get('sectionKey') != 'sft':
        continue
    for elem in sec.get('elements', []):
        cat = elem.get('title', '')
        l2 = elem.get('l2') or {}
        l1 = elem.get('l1') or {}
        l2data = l2.get('columnData') or []
        l1data = l1.get('columnData') or []
        l2labels = l2.get('columnLabel') or []
        l1labels = l1.get('columnLabel') or []

        print(f'\nCategory: "{cat}"')
        if l2data:
            row = l2data[0]
            for ci, v in enumerate(row):
                lbl = l2labels[ci] if ci < len(l2labels) else ''
                print(f'  L2: {lbl} = {v}')
        print(f'  L1 rows: {len(l1data)}')

        if l1data:
            col_map = {}
            for ci, col in enumerate(l1labels):
                col_map[ci] = col.get('field', col.get('name', ''))

            for ri, row in enumerate(l1data):
                row_d = {col_map[ci]: row[ci] if ci < len(row) else '' for ci in col_map}
                print(f'  L1[{ri}]: {json.dumps(row_d, ensure_ascii=False)}')

# Part B1 - summary
print()
print('━━━ PART B1 - TDS Summary ━━━')
for sec in partB.get('sections', []):
    if sec.get('sectionKey') != 'tdsTcs':
        continue
    for elem in sec.get('elements', []):
        l2 = elem.get('l2') or {}
        l1 = elem.get('l1') or {}
        l1data = l1.get('columnData') or []

        total_amt = sum(parseAmt(r[3] if len(r) > 3 else '') for r in l1data)
        total_tds = sum(parseAmt(r[4] if len(r) > 4 else '') for r in l1data)
        total_dep = sum(parseAmt(r[5] if len(r) > 5 else '') for r in l1data)
        active = sum(1 for r in l1data if len(r) > 6 and r[6] == 'Active')
        inactive = sum(1 for r in l1data if len(r) > 6 and r[6] == 'Inactive')

        print(f'  L2 Amount: {l2.get("columnData", [[""]])[0][5] if l2.get("columnData") else "N/A"}')
        print(f'  L1 SUM: amount={total_amt}, tds={total_tds}, deposited={total_dep}')
        print(f'  Active: {active}, Inactive: {inactive}')
        print(f'  Quarters: {sorted(set(r[1] if len(r) > 1 else "" for r in l1data))}')
        print(f'  Unique dates: {sorted(set(r[2] if len(r) > 2 else "" for r in l1data))}')

print()
print('━━━ DASHBOARD vs AIS - WHAT IS/WAS SHOWN ━━━')
print()
print('✅ WORKING:')
print('  TDS Entry #1: 194A, SBI, MUMS89569E, Income=224329, TDS=137077')
print('  Bank Interest Details: 4 entries (SBI SB=1150, BoM SB=156, SBI FD=224332, Akola Urban FD=3359)')
print('  Savings Interest (legacy): 1150+156 = 1306')
print('  Deposit Interest (legacy): 224332+3359 = 227691')
print('  Capital Gains: 4 entries (2 MF sales - HDFC, 2 Equity - HDFC)')
print('  Dividend: 125')
print()
print('❌ NOT SHOWN (still missing):')
print('  1. Property Sale: Joint Sub Registrar Akola, ₹50,00,000 (Land/Building) → NOT in CG tab')
print('     → AIS has it in SFT category "Sale of land or building" but NOT appearing')
print('  2. TDS Unique Transaction No: TSNs NOT populated in TDS entry')
print('  3. TDS Certificate No: NOT populated')
print('  4. TDS Deduction Date: NOT populated (date of last credit)')
print('  5. MF Purchases: 9 entries (Franklin, ICICI, HDFC, Sundaram) → NOT shown anywhere')
print('  6. TDS Individual transactions: 110 rows → NOT shown (only aggregated entry)')
print()
print('⚠️  WRONG VALUES:')
print('  Legacy SB Interest: shows 156 instead of 1306')
print('    → Only Akola Urban FD (3359) was added to FD, not SBI FD (224332)')
print('    → Savings: only BoM (156) instead of SBI+BoM (1150+156=1306)')
print('  Legacy FD Interest: shows 3359 instead of 227691')
print('    → Only Akola Urban FD was added to legacy FD, SBI FD was overwritten')
print('    → reason: result.put("interestFD") called TWICE for same key, last wins = 3359')
print()
print('━━━ ROOT CAUSE ANALYSIS ━━━')
print()
print('PROBLEM 1 - Property Sale not in CG:')
print('  AIS category: "Sale of land or building"')
print('  Parsed to: propertySales list (separate from mfSales)')
print('  merge at end: b2.getSecuritiesSale().addAll(b2.getPropertySales())')
print('  BUT PrefillController checks: if (!propList.isEmpty())')
print('  → propertySales IS populated (1 entry, 50L), but CG entry might not be creating')
print('  → Need to check if prefilled AIS still has propertySales after JSON round-trip')
print()
print('PROBLEM 2 - Legacy interest wrong:')
print('  Code does: result.put("interestSB", ...) INSIDE the loop')
print('  → savingsInterest has 2 entries (SBI 1150, BoM 156)')
print('  → Last one wins = 156 (overwrites SBI 1150)')
print('  → Should ACCUMULATE: interestSB += value')
print()
print('PROBLEM 3 - MF Purchases not shown:')
print('  AIS has 9 entries across 4 AMC (Franklin, ICICI Prudential, HDFC, Sundaram)')
print('  PrefillController only logs them, does NOT populate any frontend field')
print('  → No field in ITR form for MF purchases (they are informational)')
print()
print('PROBLEM 4 - TDS transaction details:')
print('  110 individual transactions in tdsTransactions[]')
print('  PrefillController only reads tdsEntries[] (1 aggregated)')
print('  → TDSEntry type has uniqueTransactionNo, deductionDate fields')
print('  → But prefilled AIS only has 1 aggregated entry, not 110 transactions')
print('  → Should populate uniqueTransactionNo from first/active transaction TSN')
print()
print('━━━ EXTRACTOR BUG ━━━')
print()
print('In AISJsonImportService parsePartB2():')
print('  Category check: category.contains("sale") && category.contains("land")')
print('  But AIS JSON has: "Sale of land or building (SFT-18(Sale))"')
print('  Does it contain "land"? YES')
print('  Does it contain "securit" or "mutual"? NO')
print('  → Goes to PROPERTY branch ✅')
print('  BUT: propertySales adds entries, merge at end SHOULD add to securitiesSale')
print('  → Check: does AIS JSON round-trip preserve propertySales?')
print('  → Jackson serialization might drop null lists or empty lists')
print()
print('Actually wait - the Dashboard DOES show 4 CG entries:')
print('  HDFC Balanced Advantage Fund - 2 entries (Equity + MF classification)')
print('  These are from MF SALE of securities (SFT-18(Sale))')
print('  Cost: 74830.07 (purchase of units), Sale: 216961.64')
print('  → These match the SFT MF Sale data! ✅')
print()
print('MISSING: Property Sale (₹50L) - need to check why propertySales not merging')
