"""Manual test script for AIS/TIS import endpoints.

Usage:
    python test_ais_tis.py

Requirements:
    - Backend server running on http://localhost:8000
    - Sample encrypted AIS JSON file
    - Sample password-protected AIS PDF
    - Sample password-protected TIS PDF
    - Valid PAN and DOB for decryption
"""
import requests
import json
from pathlib import Path

BASE_URL = "http://localhost:8000/api/v1"

# ─── Configuration ───────────────────────────────────────────────────────────
print("Enter credentials for login:")
email = input("Email: ").strip()
password = input("Password: ").strip()
PAN = input("Enter PAN (10 chars): ").strip()
DOB = input("Enter DOB (DDMMYYYY, e.g., 25051969): ").strip()

print("\n" + "="*70)
print("AIS/TIS Import Test")
print("="*70)

# ─── Login ───────────────────────────────────────────────────────────────────
print("\n[0/3] Logging in...")
try:
    login_response = requests.post(
        f"{BASE_URL}/auth/login",
        json={"email": email, "password": password},
        timeout=10
    )
    if login_response.status_code != 200:
        print(f"  ✗ Login failed: {login_response.status_code}")
        print(f"    {login_response.text}")
        exit(1)
    
    token = login_response.json().get("access_token")
    if not token:
        print("  ✗ No access token in response")
        exit(1)
    
    print(f"  ✓ Logged in successfully")
    headers = {"Authorization": f"Bearer {token}"}
except Exception as e:
    print(f"  ✗ Login error: {e}")
    exit(1)


def test_ais_json():
    """Test encrypted AIS JSON import."""
    print("\n[1/3] Testing AIS JSON import...")
    file_path = input("  Enter path to encrypted AIS .json file (or 'skip'): ").strip()
    
    if file_path.lower() == 'skip':
        print("  ⊘ Skipped")
        return
    
    if not Path(file_path).exists():
        print(f"  ✗ File not found: {file_path}")
        return
    
    try:
        with open(file_path, 'rb') as f:
            files = {'file': ('ais.json', f, 'application/json')}
            data = {'pan': PAN, 'dob': DOB, 'client_id': '1'}
            
            response = requests.post(
                f"{BASE_URL}/prefill/ais/json",
                files=files,
                data=data,
                headers=headers,
                timeout=30
            )
        
        if response.status_code == 200:
            result = response.json()
            print(f"  ✓ Success!")
            print(f"    TDS Records: {len(result.get('tds_records', []))}")
            print(f"    AIS Info Items: {len(result.get('ais_info_items', []))}")
            
            if result.get('tds_records'):
                print(f"\n  Sample TDS Record:")
                tds = result['tds_records'][0]
                print(f"    Deductor: {tds.get('deductor_name')}")
                print(f"    Amount: ₹{tds.get('tds_deposited', 0):,.2f}")
                print(f"    TAN: {tds.get('deductor_tan')}")
            
            if result.get('ais_info_items'):
                print(f"\n  Sample AIS Item:")
                item = result['ais_info_items'][0]
                print(f"    Type: {item.get('info_type')}")
                print(f"    Value: {item.get('value')}")
        else:
            print(f"  ✗ Failed: {response.status_code}")
            print(f"    {response.text}")
    
    except Exception as e:
        print(f"  ✗ Error: {e}")


def test_ais_pdf():
    """Test password-protected AIS PDF import."""
    print("\n[2/3] Testing AIS PDF import...")
    file_path = input("  Enter path to AIS .pdf file (or 'skip'): ").strip()
    
    if file_path.lower() == 'skip':
        print("  ⊘ Skipped")
        return
    
    if not Path(file_path).exists():
        print(f"  ✗ File not found: {file_path}")
        return
    
    try:
        with open(file_path, 'rb') as f:
            files = {'file': ('ais.pdf', f, 'application/pdf')}
            data = {'pan': PAN, 'dob': DOB, 'client_id': '1'}
            
            response = requests.post(
                f"{BASE_URL}/prefill/ais/pdf",
                files=files,
                data=data,
                headers=headers,
                timeout=60
            )
        
        if response.status_code == 200:
            result = response.json()
            print(f"  ✓ Success!")
            print(f"    Pages extracted: {result.get('pages_extracted', 0)}")
            print(f"    Tables found: {result.get('tables_count', 0)}")
            print(f"    TDS Records: {len(result.get('tds_records', []))}")
            
            if result.get('raw_tables'):
                print(f"\n  First table preview:")
                table = result['raw_tables'][0]
                print(f"    Rows: {len(table)}")
                if table:
                    print(f"    Headers: {list(table[0].keys())}")
        else:
            print(f"  ✗ Failed: {response.status_code}")
            print(f"    {response.text}")
    
    except Exception as e:
        print(f"  ✗ Error: {e}")


def test_tis_pdf():
    """Test password-protected TIS PDF import."""
    print("\n[3/3] Testing TIS PDF import...")
    file_path = input("  Enter path to TIS .pdf file (or 'skip'): ").strip()
    
    if file_path.lower() == 'skip':
        print("  ⊘ Skipped")
        return
    
    if not Path(file_path).exists():
        print(f"  ✗ File not found: {file_path}")
        return
    
    try:
        with open(file_path, 'rb') as f:
            files = {'file': ('tis.pdf', f, 'application/pdf')}
            data = {'pan': PAN, 'dob': DOB, 'client_id': '1'}
            
            response = requests.post(
                f"{BASE_URL}/prefill/tis/pdf",
                files=files,
                data=data,
                headers=headers,
                timeout=60
            )
        
        if response.status_code == 200:
            result = response.json()
            print(f"  ✓ Success!")
            print(f"    Pages extracted: {result.get('pages_extracted', 0)}")
            print(f"    Tables found: {result.get('tables_count', 0)}")
            print(f"    TDS Records: {len(result.get('tds_records', []))}")
            print(f"    Tax Payments: {len(result.get('tax_payments', []))}")
            
            if result.get('tax_payments'):
                print(f"\n  Sample Tax Payment:")
                payment = result['tax_payments'][0]
                print(f"    BSR Code: {payment.get('bsr_code')}")
                print(f"    Amount: ₹{payment.get('amount', 0):,.2f}")
                print(f"    Date: {payment.get('payment_date')}")
        else:
            print(f"  ✗ Failed: {response.status_code}")
            print(f"    {response.text}")
    
    except Exception as e:
        print(f"  ✗ Error: {e}")


if __name__ == "__main__":
    try:
        test_ais_json()
        test_ais_pdf()
        test_tis_pdf()
        
        print("\n" + "="*70)
        print("Tests completed!")
        print("="*70)
    
    except KeyboardInterrupt:
        print("\n\nTests cancelled by user")
