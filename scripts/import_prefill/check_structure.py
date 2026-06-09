import json

with open(r'C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\import_prefill\XXXPG3482X_2024-25_AIS_decrypted.json', encoding='utf-8') as f:
    data = json.load(f)

partA = data.get('partA', {})
labels = partA.get('columnLabel', [])
values = partA.get('columnData', [])

print('columnData structure:')
print('  is array:', isinstance(values, list))
if values:
    print('  count:', len(values))
    print('  first item type:', type(values[0]))
    if isinstance(values[0], list):
        print('  first item length:', len(values[0]))
        print('  first item:', values[0])
    elif isinstance(values[0], dict):
        print('  first item keys:', list(values[0].keys()))
        print('  first item:', values[0])
    else:
        print('  first item:', values[0])
    if len(values) > 1:
        print('  second item:', values[1])
        if isinstance(values[1], list):
            print('  second item length:', len(values[1]))
            print('  second item:', values[1])

print()
print('L1 columnLabel structure (first few):')
partB = data.get('partB', {})
sections = partB.get('sections', [])
for sec in sections:
    if sec.get('sectionKey') == 'tdsTcs':
        for elem in sec.get('elements', [])[:1]:
            l1 = elem.get('l1', {})
            l1Labels = l1.get('columnLabel', [])
            l1Data = l1.get('columnData', [])
            print('  L1 label[0]:', l1Labels[0] if l1Labels else None)
            print('  L1 data[0]:', l1Data[0] if l1Data else None)
            print('  L1 data type[0]:', type(l1Data[0]) if l1Data else None)
            if l1Data and isinstance(l1Data[0], list):
                print('  L1 data[0] len:', len(l1Data[0]))
        break
