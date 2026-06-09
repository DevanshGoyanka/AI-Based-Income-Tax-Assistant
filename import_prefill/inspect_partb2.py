import json

data = json.load(open('XXXPG3482X_2024-25_AIS_decrypted.json', encoding='utf-8'))

partB = data.get('partB', {})
sections = partB.get('sections', [])

for i, sec in enumerate(sections):
    title = sec.get('title', '?')
    sectionKey = sec.get('sectionKey', '?')
    print('--- Section', i+1, ':', title, '---')
    print('SectionKey:', sectionKey)

    # Check for elements (B1, B2, B3, B7)
    elements = sec.get('elements', [])
    subSections = sec.get('subSections', [])

    if elements:
        print('Elements count:', len(elements))
        for j, elem in enumerate(elements[:3]):
            print('  Elem', j+1, 'keys:', list(elem.keys()))
            # Print first few keys
            for k, v in elem.items():
                if isinstance(v, str):
                    print('   ', k, ':', repr(v[:100]))
                elif isinstance(v, (int, float)):
                    print('   ', k, ':', v)
                elif isinstance(v, list):
                    print('   ', k, ': [list of', len(v), ']')
                elif isinstance(v, dict):
                    print('   ', k, ': {dict keys:', list(v.keys()), '}')
                else:
                    print('   ', k, ':', type(v).__name__)
            print()

    if subSections:
        print('SubSections count:', len(subSections))
        for j, sub in enumerate(subSections[:2]):
            print('  Sub', j+1, 'keys:', list(sub.keys()))
            print()

    print()
