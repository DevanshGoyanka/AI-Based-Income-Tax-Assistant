import json

data = json.load(open('XXXPG3482X_2024-25_AIS_decrypted.json', encoding='utf-8'))

partB = data.get('partB', {})
sections = partB.get('sections', [])

for i, sec in enumerate(sections):
    title = sec.get('title', '?')
    sectionKey = sec.get('sectionKey', '?')
    print('=' * 70)
    print('SECTION', i+1, '-', sectionKey, '-', title)
    print('=' * 70)

    elements = sec.get('elements', [])
    subSections = sec.get('subSections', [])

    if elements:
        print('Total elements:', len(elements))
        for j, elem in enumerate(elements):
            elemTitle = elem.get('title', '')
            print()
            print('  Element', j+1, ':', repr(elemTitle))

            # Print l1 (original/highest) info
            l1 = elem.get('l1', {})
            if l1:
                l1Labels = l1.get('columnLabel', [])
                l1Data = l1.get('columnData', [])
                if l1Labels:
                    print('    L1 columnLabels:', l1Labels)
                    if l1Data:
                        print('    L1 columnData (', len(l1Data), 'rows):')
                        for row in l1Data[:5]:
                            print('      ', row)

            # Print l2 (processed/system) info
            l2 = elem.get('l2', {})
            if l2:
                l2Labels = l2.get('columnLabel', [])
                l2Data = l2.get('columnData', [])
                if l2Labels:
                    print('    L2 columnLabels:', l2Labels)
                    if l2Data:
                        print('    L2 columnData (', len(l2Data), 'rows):')
                        for row in l2Data[:5]:
                            print('      ', row)

            infoSrc = elem.get('infoSrcId', '')
            if infoSrc:
                print('    infoSrcId:', infoSrc)

            # Top-level columnLabel/columnData (for sections like B3)
            topLabels = elem.get('columnLabel', [])
            topData = elem.get('columnData', [])
            if topLabels:
                print('    Top columnLabels:', topLabels)
                if topData:
                    print('    Top columnData:', topData)

    if subSections:
        print('SubSections:')
        for j, sub in enumerate(subSections):
            print('  SubSection', j+1, ':', sub.get('title', '?'))
            subElems = sub.get('elements', [])
            print('  Elements count:', len(subElems))
            for elem in subElems[:2]:
                print('    Elem:', elem.get('title', '?'))
                l2 = elem.get('l2', {})
                if l2:
                    print('      L2:', l2.get('columnLabel', []))
                    print('      L2 data:', l2.get('columnData', []))

    print()
    if i >= 1:  # Only show first 2 sections in detail to avoid too much output
        break
