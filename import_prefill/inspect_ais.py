content = open('XXXPG3482X_2024-25_AIS_08062026.json', 'r', encoding='utf-8').read().strip()
print('Total length:', len(content))
print('First 100 chars:', repr(content[:100]))
print('IV (0-32):', repr(content[:32]))
print('Salt (32-64):', repr(content[32:64]))
print('Is hex (first 64)?:', all(c in '0123456789abcdefABCDEF' for c in content[:64]))
print('Last 50 chars:', repr(content[-50:]))
print()
if content.startswith('{') or content.startswith('['):
    print('Format: Plain JSON')
elif content.startswith('"'):
    print('Format: JSON string wrapper')
else:
    print('Format: Raw encrypted string')
    print('First char hex value:', hex(ord(content[0])))
