"""Examine full AIS structure."""
import json
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.services.ais_decrypt import decrypt_ais

data = open(r'C:\Users\Devansh\Downloads\XXXPC5929X_2025-26_AIS_11062026.json', 'rb').read()
ais = decrypt_ais(data, 'COVPC5929M', '08022002')

# Show top-level structure
print("Top-level keys:", list(ais.keys()))
print()

# Show partB structure
partb = ais.get('partB', {})
print("PartB keys:", list(partb.keys()))

sections = partb.get('sections', [])
print(f'\nTotal sections: {len(sections)}')

for s in sections:
    section_key = s.get("sectionKey", "")
    title = s.get("title", "")
    print(f'\n--- Section: {section_key} ---')
    print(f'Title: {title}')
    print(f'Elements: {len(s.get("elements", []))}')
    
    for i, elem in enumerate(s.get('elements', [])):
        print(f'\n  Element [{i}]: {elem.get("title")}')
        print(f'  infoSrcId: {elem.get("infoSrcId")}')
        
        # Show l2 structure (summary)
        l2 = elem.get('l2', {})
        l2_rows = l2.get('columnData', [])
        if l2_rows:
            print(f'  L2 columns: {len(l2_rows[0])}')
            print(f'  L2 row[0]: {l2_rows[0]}')
        
        # Show l1 structure (transactions)
        l1 = elem.get('l1', {})
        l1_labels = l1.get('columnLabel', [])
        l1_rows = l1.get('columnData', [])
        print(f'  L1 labels: {len(l1_labels)}, L1 rows: {len(l1_rows)}')
        
        if l1_labels:
            label_info = []
            for lbl in l1_labels[:15]:
                if isinstance(lbl, dict):
                    label_info.append(f"{lbl.get('field')}:{lbl.get('name')}")
                else:
                    label_info.append(str(lbl))
            print(f'  L1 label fields: {label_info}')
        
        if l1_rows:
            print(f'  L1 first row ({len(l1_rows[0])} cols): {l1_rows[0]}')
