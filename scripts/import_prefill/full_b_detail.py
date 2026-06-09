import json

with open(r'C:\Users\Devansh\Desktop\ITR-FilingWebsite-main\import_prefill\XXXPG3482X_2024-25_AIS_decrypted.json', encoding='utf-8') as f:
    data = json.load(f)

partB = data.get('partB', {})
sections = partB.get('sections', [])

total_dividend = 0
total_savings = 0
total_fd = 0
total_sft = 0

for sec in sections:
    sk = sec.get('sectionKey', '')
    title = sec.get('title', '')

    if sk == 'tdsTcs':
        print('B1 TDS/TCS:')
        print('=' * 70)
        for elem in (sec.get('elements') or []):
            if elem is None:
                continue
            l2 = elem.get('l2') or {}
            l2data = l2.get('columnData') or []
            l2labels = l2.get('columnLabel') or []
            l1data = (elem.get('l1') or {}).get('columnData') or []
            l1labels = (elem.get('l1') or {}).get('columnLabel') or []

            cat = elem.get('title', '')
            src_id = elem.get('infoSrcId', '')

            print(f'Category: {cat}')
            print(f'  infoSrcId: {src_id}')

            if l2data:
                row = l2data[0]
                d = {}
                for ci, val in enumerate(row):
                    lbl = l2labels[ci] if ci < len(l2labels) else ''
                    d[ci] = (lbl, val)
                print(f'  L2 data:')
                for k, v in d.items():
                    print(f'    [{k}] {v[0]}: {v[1]}')

            # L1 column fields
            l1_fields = {}
            if l1labels:
                for ci, col in enumerate(l1labels):
                    fn = col.get('field', '')
                    nm = col.get('name', '')
                    l1_fields[ci] = (fn, nm)
                print(f'  L1 columns ({len(l1_fields)}):')
                for k, v in sorted(l1_fields.items()):
                    print(f'    [{k}] field={v[0]}, name={v[1]}')

            print(f'  L1 rows: {len(l1data)}')
            for ri, row in enumerate(l1data):
                row_d = {}
                for ci, val in enumerate(row):
                    if ci in l1_fields:
                        row_d[l1_fields[ci][0]] = val
                print(f'    row[{ri}]: {row_d}')
            print()

    elif sk == 'sft':
        print('B2 SFT:')
        print('=' * 70)
        for elem in (sec.get('elements') or []):
            if elem is None:
                continue
            l2 = elem.get('l2') or {}
            l2data = l2.get('columnData') or []
            l2labels = l2.get('columnLabel') or []
            l1data = (elem.get('l1') or {}).get('columnData') or []
            l1labels = (elem.get('l1') or {}).get('columnLabel') or []

            cat = elem.get('title', '')
            src_id = elem.get('infoSrcId', '')

            print(f'Category: {cat}')
            print(f'  infoSrcId: {src_id}')

            if l2data:
                row = l2data[0]
                d = {}
                for ci, val in enumerate(row):
                    lbl = l2labels[ci] if ci < len(l2labels) else ''
                    d[ci] = (lbl, val)
                print(f'  L2 data:')
                for k, v in sorted(d.items()):
                    print(f'    [{k}] {v[0]}: {v[1]}')

            l1_fields = {}
            if l1labels:
                for ci, col in enumerate(l1labels):
                    fn = col.get('field', '')
                    nm = col.get('name', '')
                    l1_fields[ci] = (fn, nm)
                print(f'  L1 columns ({len(l1_fields)}):')
                for k, v in sorted(l1_fields.items()):
                    print(f'    [{k}] field={v[0]}, name={v[1]}')

            print(f'  L1 rows: {len(l1data)}')
            for ri, row in enumerate(l1data[:3]):
                row_d = {}
                for ci, val in enumerate(row):
                    if ci in l1_fields:
                        row_d[l1_fields[ci][0]] = val
                print(f'    row[{ri}]: {row_d}')
            print()

    elif sk == 'paymentOfTaxes':
        print('B3 Tax Payments:')
        print('=' * 70)
        for elem in (sec.get('elements') or []):
            cols = elem.get('columnLabel') or []
            rows = elem.get('columnData') or []
            print(f'Columns: {cols}')
            print(f'Data rows: {len(rows)}')
            for r in rows:
                print(f'  {r}')
        print()

    elif sk == 'demandAndRefund':
        print('B4 Demand & Refund:')
        print('=' * 70)
        for sub in sec.get('subSections') or []:
            print(f'SubSection: {sub.get("title")}')
            for elem in (sub.get('elements') or []):
                if elem is None:
                    continue
                l2 = elem.get('l2') or {}
                l2data = l2.get('columnData') or []
                l2labels = l2.get('columnLabel') or []
                l1data = (elem.get('l1') or {}).get('columnData') or []
                l1labels = (elem.get('l1') or {}).get('columnLabel') or []

                if l2data:
                    print(f'  L2 columns: {l2labels}')
                    for r in l2data:
                        print(f'    {r}')
                print(f'  L1 rows: {len(l1data)}')
                for r in l1data[:3]:
                    print(f'    {r}')
        print()

    elif sk == 'other-info':
        print('B7 Other Info:')
        print('=' * 70)
        for elem in (sec.get('elements') or []):
            if elem is None:
                continue
            l2 = elem.get('l2') or {}
            l2data = l2.get('columnData') or []
            l2labels = l2.get('columnLabel') or []
            if l2data:
                print(f'  L2 columns: {l2labels}')
                for r in l2data:
                    print(f'    {r}')
        print()

print('=' * 70)
print('FOOTER:')
footer = data.get('footer') or {}
print(json.dumps(footer, indent=2))
