import json
from app.services.ais_parser import parse_ais_json

with open(r'C:\Users\Devansh\Downloads\123decrypted.json') as f:
    data = json.load(f)

result = parse_ais_json(data)

print(f'TDS Records: {len(result.tds_records)}')
print(f'Info Items: {len(result.info_items)}')
print(f'\nInfo Items by type:')
types = {}
for item in result.info_items:
    types[item.info_type] = types.get(item.info_type, 0) + 1
print(types)

print(f'\nFirst 3 Info Items:')
for i, item in enumerate(result.info_items[:3]):
    print(f'\n  [{i+1}] Type: {item.info_type}')
    print(f'      Desc: {item.description}')
    print(f'      Amount: {item.amount}')
    print(f'      Section: {item.section_code}')
    print(f'      Date: {item.transaction_date}')
