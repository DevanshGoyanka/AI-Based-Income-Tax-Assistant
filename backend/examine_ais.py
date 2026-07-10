import json

with open(r'C:\Users\Devansh\Downloads\123decrypted.json') as f:
    data = json.load(f)

sections = data.get('partB', {}).get('sections', [])
print(f'Total sections: {len(sections)}\n')

for s in sections:
    key = s.get('sectionKey', '')
    title = s.get('title', '')
    elements = s.get('elements', [])
    print(f'{key}: {title}')
    print(f'  Elements: {len(elements)}')
    
    if elements:
        for i, elem in enumerate(elements[:3]):
            elem_title = elem.get('title', '')
            print(f'    [{i}] {elem_title}')
    print()
