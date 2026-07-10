"""Debug script to inspect decrypted AIS JSON structure."""
import sys
from pathlib import Path
from app.services.ais_decrypt import decrypt_ais

if len(sys.argv) < 4:
    print("Usage: python debug_ais.py <file_path> <pan> <dob>")
    sys.exit(1)

file_path = sys.argv[1]
pan = sys.argv[2]
dob = sys.argv[3]

raw = Path(file_path).read_bytes()
decrypted = decrypt_ais(raw, pan, dob)

print("Top-level keys:")
for key in decrypted.keys():
    val = decrypted[key]
    if isinstance(val, list):
        print(f"  {key}: list with {len(val)} items")
    elif isinstance(val, dict):
        print(f"  {key}: dict with keys {list(val.keys())}")
    else:
        print(f"  {key}: {type(val).__name__}")

print("\nFull structure (first 2000 chars):")
import json
print(json.dumps(decrypted, indent=2)[:2000])
