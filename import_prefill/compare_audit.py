import json

def parseAmt(s):
    if not s: return 0
    try: return int(float(str(s).replace(',', '').replace('₹', '').replace(' ', '')))
    except: return 0

with open(r'C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\import_prefill\XXXPG3482X_2024-25_AIS_decrypted.json', encoding='utf-8') as f:
    data = json.load(f)

partB = data.get('partB', {})

print('=' * 80)
print('AIS JSON vs DASHBOARD - EXTRACTION GAP ANALYSIS')
print('=' * 80)
print()

# Part A
print('━━━ PART A - General Info ━━━')
partA = data.get('partA', {})
for i, lbl in enumerate(partA.get('columnLabel', [])):
    val = partA.get('columnData', [''])[i] if i < len(partA.get('columnData', [])) else 'N/A'
    status = '✅' if val and val != 'N/A' else '❌'
    print(f'  {status} {lbl}: {val}')
print()

# Part B1
print('━━━ PART B1 - TDS/TCS ━━━')
for sec in partB.get('sections', []):
    if sec.get('sectionKey') != 'tdsTcs':
        continue
    for elem in sec.get('elements', []):
        cat = elem.get('title', '')
        l2 = elem.get('l2') or {}
        l1 = elem.get('l1') or {}
        l2data = l2.get('columnData') or []
        l1data = l1.get('columnData') or []
        l2labels = l2.get('columnLabel') or []
        l1labels = l1.get('columnLabel') or []

        print(f'Category: {cat}')
        if l2data:
            row = l2data[0]
            for ci, v in enumerate(row):
                lbl = l2labels[ci] if ci < len(l2labels) else ''
                print(f'  L2 {lbl}: {v}')
        print(f'  L1 rows: {len(l1data)}')
        if l1data:
            col_map = {}
            for ci, col in enumerate(l1labels):
                col_map[ci] = col.get('field', col.get('name', ''))
            total_amt = sum(parseAmt(row[3] if len(row) > 3 else '') for row in l1data)
            total_tds = sum(parseAmt(row[4] if len(row) > 4 else '') for row in l1data)
            total_dep = sum(parseAmt(row[5] if len(row) > 5 else '') for row in l1data)
            print(f'  L1 SUM: amount={total_amt}, tds={total_tds}, deposited={total_dep}')
            print(f'  L1 quarters: {sorted(set(row[1] if len(row) > 1 else '' for row in l1data))}')
            print(f'  L1 dates: {sorted(set(row[2] if len(row) > 2 else '' for row in l1data))}')
            statuses = {}
            feedbacks = {}
            for row in l1data:
                s = row[6] if len(row) > 6 else ''
                f = row[7] if len(row) > 7 else ''
                statuses[s] = statuses.get(s, 0) + 1
                feedbacks[f] = feedbacks.get(f, 0) + 1
            print(f'  L1 statuses: {statuses}')
            print(f'  L1 feedbacks: {feedbacks}')
        print()

# Part B2
print('━━━ PART B2 - SFT ━━━')
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

        print(f'Category: {cat}')
        if l2data:
            row = l2data[0]
            for ci, v in enumerate(row):
                lbl = l2labels[ci] if ci < len(l2labels) else ''
                print(f'  L2 {lbl}: {v}')
        print(f'  L1 rows: {len(l1data)}')
        if l1data:
            col_map = {}
            for ci, col in enumerate(l1labels):
                col_map[ci] = col.get('field', col.get('name', ''))
            for ri, row in enumerate(l1data[:5]):
                row_d = {col_map[ci]: row[ci] if ci < len(row) else '' for ci in col_map}
                print(f'  L1 row[{ri}]: {json.dumps(row_d, ensure_ascii=False)}')
        print()

# Part B3
print('━━━ PART B3 - Tax Payments ━━━')
for sec in partB.get('sections', []):
    if sec.get('sectionKey') != 'paymentOfTaxes':
        continue
    for elem in sec.get('elements', []):
        cols = elem.get('columnLabel') or []
        rows = elem.get('columnData') or []
        print(f'  Columns ({len(cols)}): {cols}')
        print(f'  Data rows: {len(rows)}')
        if not rows:
            print('  ⚠️  NO TAX PAYMENT DATA')
print()

# Summary
print('━━━ DASHBOARD vs AIS GAP SUMMARY ━━━')
print()
print('DASHBOARD CURRENTLY SHOWS:')
print('  Personal Info: ✅')
print('  TDS Entry #1: 194A, SBI, MUMS89569E, Income=224329, TDS=137077')
print('  SB Interest: ₹0 (❌ should be ₹1,306)')
print('  FD Interest: ₹0 (❌ should be ₹2,27,691)')
print('  Dividend: ₹125 ✅')
print('  Bank Interest Details: "No bank accounts added" (❌ no multi-entry)')
print('  Capital Gains: NOT VISIBLE (❌ property sale ₹50L, MF sale ₹3L missing)')
print('  TDS Individual Transactions: NOT VISIBLE (❌ 110 transactions not shown)')
print()
print('MISSING FROM DASHBOARD:')
print('  1. Bank Interest Details - need multi-entry per bank (SBI, BoM, Akola Urban)')
print('  2. Capital Gains - property sale (50L), MF sale (3L)')
print('  3. TDS Individual transactions - 110 rows with TSN, date, amount, status')
print('  4. MF Purchases - 4 AMC entries')
print('  5. Tax Payments - 0 rows but structure exists')
print()
print('ROOT CAUSE: Auto-populate only sets flat legacy fields (InterestSB, InterestFD)')
print('           It does NOT create multi-entry BankAccountEntry, CapitalGainEntry,')
print('           TDS transaction rows, or MF purchase entries.')
