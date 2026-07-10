import json

with open(r'C:\Users\Devansh\Downloads\123decrypted.json') as f:
    data = json.load(f)

sections = data.get('partB', {}).get('sections', [])

for section in sections:
    key = section.get('sectionKey', '')
    if key == 'sft':
        elements = section.get('elements', [])
        if elements:
            elem = elements[0]
            print('SFT Element structure:')
            print(f'  title: {elem.get("title")}')
            print(f'  infoSrcId: {elem.get("infoSrcId")}')
            
            # Check l1 data
            l1 = elem.get('l1', {})
            print(f'\n  l1 columns:')
            for label in l1.get('columnLabel', [])[:5]:
                if isinstance(label, dict):
                    print(f'    - {label.get("field")}: {label.get("name")}')
            
            print(f'\n  l1 first row sample:')
            rows = l1.get('columnData', [])
            if rows:
                print(f'    {rows[0][:8]}')
            
            # Check l2 summary
            l2 = elem.get('l2', {})
            print(f'\n  l2 summary:')
            l2_rows = l2.get('columnData', [])
            if l2_rows:
                print(f'    {l2_rows[0]}')
        break
