"""Test AIS parser with decrypted file."""
import json
from app.services.ais_parser import parse_ais_json

with open(r'C:\Users\Devansh\Downloads\123decrypted.json') as f:
    raw = json.load(f)

parsed = parse_ais_json(raw)

print(f'TDS Records: {len(parsed.tds_records)}')
print(f'Info Items: {len(parsed.info_items)}')
print(f'Personal: {parsed.personal_info.get("name")}')

if parsed.tds_records:
    print('\nFirst TDS record:')
    r = parsed.tds_records[0]
    print(f'  TAN: {r.deductor_tan}')
    print(f'  Name: {r.deductor_name}')
    print(f'  Section: {r.section_code}')
    print(f'  Amount: {r.amount_paid}')
    print(f'  Tax: {r.tax_deducted}')
    print(f'  Quarter: {r.quarter}')
