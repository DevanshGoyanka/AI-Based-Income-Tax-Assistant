"""Compare COVPC5929M-2026.zip files."""
import sys
sys.path.insert(0, '.')
from app.services.importers.form26as import Form26ASImporter
import os

importer = Form26ASImporter()

files = [
    r"C:\Users\Devansh\Desktop\E-FILE_karo\COVPC5929M-2026.zip",
    r"C:\Users\Devansh\Downloads\COVPC5929M-2026.zip",
]

for f in files:
    print(f"File: {f}")
    print(f"Size: {os.path.getsize(f)} bytes")
    
    with open(f, 'rb') as fh:
        data = fh.read()
    
    # Try extraction
    password = b'08022002'
    txt = importer._extract_from_zip(data, password)
    print(f"TXT length: {len(txt)}")
    print(f"First 150 chars: {txt[:150]}")
    print()
