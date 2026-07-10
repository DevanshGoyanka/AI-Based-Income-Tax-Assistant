"""Debug PDF extraction."""
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.services.pdf_decrypt import _pdfplumber_available, _extract_tables_pdfplumber, _decrypt_pdf

print('pdfplumber available:', _pdfplumber_available())

# Read and decrypt PDF
with open(r'C:\Users\Devansh\Desktop\E-FILE_karo\XXXPC5929X_2025-26_AIS.pdf', 'rb') as f:
    pdf_bytes = f.read()

print('PDF size:', len(pdf_bytes))

# Try decrypt
try:
    reader = _decrypt_pdf(pdf_bytes, 'COVPC5929M', '08022002')
    print('Decrypted successfully')
    print('Pages:', len(reader.pages))
    
    # Extract text from first page to understand structure
    page = reader.pages[0]
    text = page.extract_text() or ''
    print('\n--- Page 1 Text (first 2000 chars) ---')
    print(text[:2000])
    print('\n--- End of preview ---')
    
except Exception as e:
    print('Error:', e)
    import traceback
    traceback.print_exc()

# Try pdfplumber extraction
print('\n--- Trying pdfplumber ---')
try:
    tables = _extract_tables_pdfplumber(pdf_bytes, 'AIS')
    print('Tables found:', len(tables))
    for i, t in enumerate(tables[:5]):
        print(f'Table {i}: page={t["page"]}, headers={t["headers"][:5]}, rows={len(t["data"])}')
        if t['data']:
            print(f'  First row: {t["data"][0][:6]}')
except Exception as e:
    print('Error:', e)
    import traceback
    traceback.print_exc()
