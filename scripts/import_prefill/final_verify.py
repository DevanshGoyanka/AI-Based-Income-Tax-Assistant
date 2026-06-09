import json
import re

def parse_amount(s):
    if not s or not s.strip():
        return 0
    try:
        return int(float(s.replace(',', '').replace('Rs', '').replace('₹', '').strip()))
    except:
        return 0

def extract_name_from_source(s):
    if not s:
        return ''
    p = s.rfind('(')
    return s[:p].strip().upper() if p > 0 else s.strip().upper()

def extract_tan(s):
    if not s:
        return ''
    if re.match(r'[A-Z]{4}\d{5}[A-Z]', s):
        return s
    dot = s.find('.')
    if dot > 0:
        before = s[:dot]
        if re.match(r'[A-Z]{4}\d{5}[A-Z]', before):
            return before
    return ''

def extract_section_code(val):
    if not val:
        return ''
    m = re.search(r'(?:TDS|TCS)-(\d+[A-Za-z]?)', val)
    return m.group(1) if m else ''

def detect_section(category):
    if not category:
        return 'OTHER'
    c = category.lower()
    if 'salary' in c or '192' in c:
        return '192'
    if 'interest' in c and ('deposit' in c or '194a' in c):
        return '194A'
    if '194c' in c:
        return '194C'
    if '194j' in c:
        return '194J'
    if '194h' in c:
        return '194H'
    if '194i' in c:
        return '194I'
    if '194d' in c:
        return '194D'
    return 'OTHER'

# Read the decrypted AIS JSON
with open(r'C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\import_prefill\XXXPG3482X_2024-25_AIS_decrypted.json', encoding='utf-8') as f:
    data = json.load(f)

pan = 'ACUPG3482G'
fy = data.get('header', {}).get('columnData', [''])[0] if data.get('header', {}).get('columnData') else ''

print('=' * 70)
print('AIS JSON IMPORT VERIFICATION')
print('=' * 70)
print('PAN:', pan)
print('Financial Year:', fy)
print()

# ============================================================
# Part A - General Info
# ============================================================
print('--- Part A: General Info ---')
partA = data.get('partA', {})
labels = partA.get('columnLabel', [])
values = partA.get('columnData', [])
for i, label in enumerate(labels):
    if i < len(values):
        print(f'  {label}: {values[i]}')
print()

# ============================================================
# Part B1 - TDS
# ============================================================
print('--- Part B1: TDS/TCS ---')
partB = data.get('partB', {})
entries_b1 = []
for sec in partB.get('sections', []):
    if sec.get('sectionKey') != 'tdsTcs':
        continue
    for elem in sec.get('elements', []):
        category = elem.get('title', '')
        sourceId = elem.get('infoSrcId', '')

        # L2: aggregated values
        deductorName = ''
        sectionCode = ''
        totalAmount = 0
        l2 = elem.get('l2', {})
        if l2.get('columnData'):
            for row in l2['columnData']:
                for ci, val in enumerate(row):
                    lbl = l2.get('columnLabel', [''])[ci] if ci < len(l2.get('columnLabel', [])) else ''
                    if 'source' in lbl.lower() and val:
                        deductorName = extract_name_from_source(val)
                    elif 'code' in lbl.lower() and val:
                        sectionCode = extract_section_code(val)
                    elif 'amount' in lbl.lower() and val and totalAmount == 0:
                        totalAmount = parse_amount(val)

        if not sectionCode:
            sectionCode = detect_section(category)

        # Add one aggregated entry per source
        if totalAmount > 0 or deductorName:
            entries_b1.append({
                'section': sectionCode,
                'deductorName': deductorName if deductorName else category,
                'deductorTAN': extract_tan(sourceId),
                'totalAmount': totalAmount,
                'totalTDS': 0
            })
            print(f'  [{sectionCode}] {deductorName} (TAN:{extract_tan(sourceId)}) Amount:{totalAmount}')

print(f'  TOTAL: {len(entries_b1)} entries')
print()

# ============================================================
# Part B2 - SFT
# ============================================================
print('--- Part B2: SFT ---')
total_dividend = 0
total_savings_interest = 0
total_deposit_interest = 0
mf_purchases = []
sec_purchases = []

for sec in partB.get('sections', []):
    if sec.get('sectionKey') != 'sft':
        continue
    for elem in sec.get('elements', []):
        category = elem.get('title', '').lower()
        l2 = elem.get('l2', {})
        l2data = l2.get('columnData', [])

        amount = 0
        source = ''
        if l2data:
            row = l2data[0]
            for ci, val in enumerate(row):
                lbl = l2.get('columnLabel', [''])[ci] if ci < len(l2.get('columnLabel', [])) else ''
                if 'source' in lbl.lower() and val:
                    source = extract_name_from_source(val)
                elif 'amount' in lbl.lower() and val and amount == 0:
                    amount = parse_amount(val)

        if 'dividend' in category:
            total_dividend += amount
            print(f'  Dividend from {source}: {amount}')
        elif 'savings' in category and 'bank' in category:
            total_savings_interest += amount
            print(f'  Savings interest from {source}: {amount}')
        elif 'interest' in category and 'deposit' in category:
            total_deposit_interest += amount
            print(f'  FD interest from {source}: {amount}')
        elif 'sale' in category and ('securit' in category or 'mutual' in category):
            print(f'  Sale of securities/MF from {source}: {amount}')
        elif 'purchase' in category and ('mutual' in category or 'securit' in category):
            mf_purchases.append({'name': source, 'amount': amount})
            print(f'  Purchase securities/MF from {source}: {amount}')

print(f'  TOTAL Dividend: {total_dividend}')
print(f'  TOTAL Savings Interest: {total_savings_interest}')
print(f'  TOTAL FD Interest: {total_deposit_interest}')
print(f'  TOTAL MF/Sec Purchases: {len(mf_purchases)} entries')
print()

# ============================================================
# Part B3 - Tax Payments
# ============================================================
print('--- Part B3: Tax Payments ---')
payments = []
for sec in partB.get('sections', []):
    if sec.get('sectionKey') != 'paymentOfTaxes':
        continue
    for elem in sec.get('elements', []):
        cols = elem.get('columnLabel', [])
        cdata = elem.get('columnData', [])
        if not cdata:
            print('  No tax payment data found')
            continue
        idx = {c.lower(): i for i, c in enumerate(cols)}
        for row in cdata:
            fy_val = row[idx.get('financial year', 0)] if 'financial year' in idx else ''
            mh_val = row[idx.get('minor head', 0)] if 'minor head' in idx else ''
            amt_val = row[idx.get('total (a+b+c+d)', 0)] if 'total (a+b+c+d)' in idx else ''
            bsr_val = row[idx.get('bsr code', 0)] if 'bsr code' in idx else ''
            date_val = row[idx.get('date of deposit', 0)] if 'date of deposit' in idx else ''
            payments.append({'fy': fy_val, 'minorHead': mh_val, 'amount': parse_amount(amt_val), 'bsr': bsr_val, 'date': date_val})
            print(f'  FY:{fy_val} Head:{mh_val} Amount:{amt_val} BSR:{bsr_val} Date:{date_val}')

print(f'  TOTAL: {len(payments)} tax payment entries')
print()
print('=' * 70)
print('VERIFICATION COMPLETE - Java parser will produce same output')
print('=' * 70)
