"""Debug all AIS PDF tables."""
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.services.pdf_decrypt import _decrypt_pdf, _extract_tables_pdfplumber, _build_pdf_password

# Read and decrypt PDF
with open(r'C:\Users\Devansh\Desktop\E-FILE_karo\XXXPC5929X_2025-26_AIS.pdf', 'rb') as f:
    pdf_bytes = f.read()

print(f'PDF size: {len(pdf_bytes):,} bytes')

reader = _decrypt_pdf(pdf_bytes, 'COVPC5929M', '08022002')
password = _build_pdf_password('COVPC5929M', '08022002')

tables = _extract_tables_pdfplumber(pdf_bytes, 'AIS', password=password)
print(f'\nTotal tables: {len(tables)}\n')

for i, t in enumerate(tables):
    print(f'{"="*80}')
    print(f'TABLE {i+1} (Page {t["page"]}):')
    print(f'Headers: {t["headers"]}')
    print(f'Rows: {len(t["data"])}')
    for j, row in enumerate(t['data'][:5]):
        print(f'  Row {j+1}: {row}')
    if len(t['data']) > 5:
        print(f'  ... and {len(t["data"]) - 5} more rows')
    print()
