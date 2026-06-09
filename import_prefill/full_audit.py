import json

with open(r'C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\import_prefill\XXXPG3482X_2024-25_AIS_decrypted.json', encoding='utf-8') as f:
    data = json.load(f)

print('=' * 80)
print('COMPLETE AIS JSON DATA EXTRACTION AUDIT')
print('PAN:', data.get('metadata', {}).get('loggedInPan'))
print('FY:', data.get('header', {}).get('columnData', [''])[0])
print('=' * 80)
print()

# ---- Part A ----
print('━' * 40)
print('PART A: GENERAL INFORMATION')
print('━' * 40)
partA = data.get('partA', {})
labels = partA.get('columnLabel', [])
values = partA.get('columnData', [])
for i, lbl in enumerate(labels):
    val = values[i] if i < len(values) else 'N/A'
    print(f'  {lbl}')
    print(f'    → {val}')
print()

# ---- Part B ----
partB = data.get('partB', {})
print('━' * 40)
print('PART B: SECTION-BY-SECTION')
print('━' * 40)

for sec in partB.get('sections', []):
    sk = sec.get('sectionKey', '')
    title = sec.get('title', '')
    print()
    print(f'[{sk}] {title}')

    # B1 / B2 / B7 use 'elements'
    for elem in sec.get('elements', []):
        if elem is None:
            continue
        cat = elem.get('title', '')
        srcId = elem.get('infoSrcId', '')
        l1src = elem.get('l1Src', '')

        l2 = elem.get('l2') or {}
        l2labels = l2.get('columnLabel', [])
        l2data = l2.get('columnData', [])

        print(f'  ─ Category: "{cat}"')
        print(f'    Source ID: {srcId}')
        print(f'    L1 Source: {l1src}')

        if l2data:
            row = l2data[0]
            for ci in range(min(len(l2labels), len(row))):
                print(f'    L2[{ci}] {l2labels[ci]}: {row[ci]}')

        l1 = elem.get('l1') or {}
        l1labels = l1.get('columnLabel', [])
        l1data = l1.get('columnData', [])
        print(f'    L1 rows: {len(l1data)}')

        # Show first few L1 rows
        for ri, row in enumerate(l1data[:3]):
            print(f'    L1 row[{ri}]: {row}')

        print()

    # B3 uses direct columnLabel/columnData
    elem_direct = sec.get('elements', [{}])[0] if sec.get('elements') else {}
    topLabels = elem_direct.get('columnLabel', [])
    topData = elem_direct.get('columnData', [])

    if topLabels and not sec.get('elements', [{}])[0].get('l2'):
        print(f'  Direct table columns: {topLabels}')
        print(f'  Data rows: {len(topData)}')
        for row in topData[:5]:
            print(f'    {row}')
        print()

    # B4 uses subSections
    for sub in sec.get('subSections', []):
        print(f'  SubSection: {sub.get("title", "")}')
        for elem in (sub.get('elements') or []):
            if elem is None:
                continue
            l2 = elem.get('l2') or {}
            if l2.get('columnData'):
                for row in l2['columnData']:
                    print(f'    {row}')
        print()

print()
print('━' * 40)
print('REJECTED FEEDBACKS')
print('━' * 40)
rf = data.get('rejectedFeedbacks') or []
print(f'Count: {len(rf)}')
if rf:
    print(json.dumps(rf[0], indent=2))

print()
print('━' * 40)
print('FOOTER')
print('━' * 40)
footer = data.get('footer', {})
print(json.dumps(footer, indent=2))
