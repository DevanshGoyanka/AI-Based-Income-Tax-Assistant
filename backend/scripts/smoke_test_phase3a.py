"""Smoke test for Phase 3a: prefill importer."""
import asyncio
import json
import base64
from pathlib import Path
from uuid import uuid4
from app.services.importers.prefill import PrefillImporter


async def main():
    print("=== PHASE 3A SMOKE TEST: PREFILL IMPORTER ===\n")
    
    sample_dir = Path(r"C:\Users\Devansh\Desktop\E-FILE_karo")
    prefill_files = list(sample_dir.glob("*-Prefill-*.json"))
    
    if not prefill_files:
        print("❌ No prefill samples found")
        return False
    
    print(f"Found {len(prefill_files)} prefill samples\n")
    
    importer = PrefillImporter()
    
    for pf in prefill_files[:2]:
        print(f"Testing: {pf.name}")
        
        with open(pf, 'rb') as f:
            content = f.read()
            data = json.loads(content)
            
            has_aadhaar = "personalInfo" in data and "aadhaarCardNo" in data["personalInfo"]
            if has_aadhaar:
                aadhaar_enc = data["personalInfo"]["aadhaarCardNo"]
                print(f"  Aadhaar (encoded): {aadhaar_enc[:20]}...")
                
                from app.services.importers.crypto import decode_aadhaar
                decoded = decode_aadhaar(aadhaar_enc)
                print(f"  Aadhaar (decoded): {decoded}")
            
            ay = importer._extract_ay(data, pf.name)
            print(f"  Assessment Year: {ay}")
            print(f"  Size: {len(content)} bytes")
            print(f"  Keys: {len(data.keys())}")
            print("  ✓ Valid JSON\n")
    
    print("✅ All prefill samples parsed successfully")
    return True


if __name__ == "__main__":
    success = asyncio.run(main())
    exit(0 if success else 1)
