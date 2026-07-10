"""Parse COVPC5929M-2026.zip content."""
import sys
sys.path.insert(0, '.')
from app.services.importers.form26as import Form26ASImporter

importer = Form26ASImporter()

with open(r"C:\Users\Devansh\Desktop\E-FILE_karo\COVPC5929M-2026.zip", 'rb') as fh:
    data = fh.read()

# Extract and parse
password = b'08022002'
txt = importer._extract_from_zip(data, password)
parsed = importer._parse_26as_text(txt)

print("Keys:", list(parsed.keys()))
print()
print("Header:", parsed.get("header", {}))
print()
print("Part 1 TDS deductors:", parsed.get("part1_tds_deductors", 0))
for d in parsed.get("part1_tds", []):
    print(f"  {d.get('deductor_name')} (TAN: {d.get('tan')}) - TDS: {d.get('tax_deducted')}")
print()
print("Source:", parsed.get("source"))
