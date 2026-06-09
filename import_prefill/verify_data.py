import json

with open(r'C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\import_prefill\XXXPG3482X_2024-25_AIS_decrypted.json', encoding='utf-8') as f:
    data = json.load(f)

print('=== TESTING JAVA PARSER LOGIC ON DECRYPTED JSON ===')
print()

# Part A - General Info
partA = data.get('partA', {})
labels = partA.get('columnLabel', [])
values = partA.get('columnData', [])

print('PART A - General Info:')
if values:
    row = values[0]
    for i, label in enumerate(labels):
        if i < len(row):
            print('  ' + label + ': ' + str(row[i]))

print()

# Part B1 - TDS
partB = data.get('partB', {})
sections = partB.get('sections', [])

for sec in sections:
    if sec.get('sectionKey') == 'tdsTcs':
        print('PART B1 - TDS/TCS:')
        for elem in sec.get('elements', []):
            title = elem.get('title', '')
            l1 = elem.get('l1', {})
            l2 = elem.get('l2', {})
            l2data = l2.get('columnData', [])
            l1data = l1.get('columnData', [])
            print('  Category: ' + title)
            print('    L2 rows (aggregated): ' + str(len(l2data)))
            print('    L1 rows (individual): ' + str(len(l1data)))
            if l2data:
                print('    L2 Labels: ' + str(l2.get('columnLabel', [])))
                print('    L2 sample: ' + str(l2data[0]))
        break

print()

# Part B2 - SFT
for sec in sections:
    if sec.get('sectionKey') == 'sft':
        print('PART B2 - SFT:')
        for elem in sec.get('elements', []):
            title = elem.get('title', '')
            l2 = elem.get('l2', {})
            l2data = l2.get('columnData', [])
            amount = l2data[0][5] if l2data and len(l2data[0]) > 5 else 'N/A'
            source = l2data[0][3] if l2data and len(l2data[0]) > 3 else 'N/A'
            l1 = elem.get('l1', {})
            l1data = l1.get('columnData', [])
            print('  ' + title + ': amount=' + str(amount) + ', source=' + str(source) + ', L1 rows=' + str(len(l1data)))
        break

print()

# Part B3 - Tax Payments
for sec in sections:
    if sec.get('sectionKey') == 'paymentOfTaxes':
        print('PART B3 - Tax Payments:')
        for elem in sec.get('elements', []):
            cols = elem.get('columnLabel', [])
            cdata = elem.get('columnData', [])
            print('  Labels: ' + str(cols))
            print('  Data rows: ' + str(len(cdata)))
        break

print()
print('=== VERIFICATION COMPLETE ===')
