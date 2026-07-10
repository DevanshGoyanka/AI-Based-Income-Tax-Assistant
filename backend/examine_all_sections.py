import json

with open(r'C:\Users\Devansh\Downloads\123decrypted.json') as f:
    data = json.load(f)

sections = data.get('partB', {}).get('sections', [])

for section in sections:
    key = section.get('sectionKey', '')
    title = section.get('title', '')
    elements = section.get('elements', [])
    
    print(f'\n{key}: {title}')
    print(f'  Total elements: {len(elements)}')
    
    for elem in elements[:2]:
        elem_title = elem.get('title', '')
        l1 = elem.get('l1', {})
        l2 = elem.get('l2', {})
        
        # Get info code from l2
        l2_rows = l2.get('columnData', [])
        info_code = ''
        if l2_rows and len(l2_rows[0]) > 1:
            info_code = l2_rows[0][1]
        
        print(f'    - {elem_title} ({info_code})')
        
        # Show sample fields
        labels = l1.get('columnLabel', [])
        if labels:
            fields = [lbl.get('field', '') for lbl in labels[:5] if isinstance(lbl, dict)]
            print(f'      Fields: {", ".join(fields)}')
