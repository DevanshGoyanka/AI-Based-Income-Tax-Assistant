import json

data = json.load(open('XXXPG3482X_2024-25_AIS_decrypted.json', encoding='utf-8'))

print('=== PART B (sections) ===')
partB = data.get('partB', {})
print('partB keys:', list(partB.keys()))
print()

sections = partB.get('sections', [])
print('Number of sections:', len(sections))
print()

for i, sec in enumerate(sections):
    title = sec.get('title', '?')
    print('--- Section', i+1, ':', title, '---')
    print('Keys:', list(sec.keys()))

    cols = sec.get('columnLabel', [])
    colData = sec.get('columnData', [])

    if cols and colData:
        print('Columns (', len(cols), '):', cols)
        print('Row count:', len(colData))
        if colData and len(colData) > 0:
            print('First row:', colData[0])
            if len(colData) > 1:
                print('Second row:', colData[1])
            if len(colData) > 2:
                print('Third row:', colData[2])
    print()

print()
print('=== REJECTED FEEDBACKS ===')
rf = data.get('rejectedFeedbacks', [])
print('Count:', len(rf))
if rf:
    print('First item keys:', list(rf[0].keys()) if isinstance(rf[0], dict) else type(rf[0]))
    print('First item:', json.dumps(rf[0], indent=2)[:1000])

print()
print('=== FOOTER ===')
footer = data.get('footer', {})
print(json.dumps(footer, indent=2))
