"""
Test script for AIS PDF import functionality.
Reads PAN and DOB from WINAMN DATA.xlsx and tests AIS PDF parsing.
"""
import sys
import os
import pandas as pd
import json
from pathlib import Path

# Add backend to path
backend_path = Path(__file__).parent / 'backend'
sys.path.insert(0, str(backend_path))

from app.services.ais_parser import parse_ais_json


def load_client_data(excel_path):
    """Load PAN and DOB data from Excel file."""
    df = pd.read_excel(excel_path)
    
    # Clean column names
    df.columns = df.columns.str.strip()
    
    # Parse dates properly
    df['DOB/DOI/DOF'] = pd.to_datetime(df['DOB/DOI/DOF'], errors='coerce')
    
    # Format dates as YYYY-MM-DD strings
    df['DOB_formatted'] = df['DOB/DOI/DOF'].dt.strftime('%Y-%m-%d')
    
    # Clean PAN - remove spaces
    df['PAN_cleaned'] = df['PAN'].str.replace(' ', '', regex=False)
    
    return df


def test_ais_import(pdf_path, excel_path):
    """
    Test AIS PDF import with automatic PAN and DOB lookup.
    
    Args:
        pdf_path: Path to AIS PDF file
        excel_path: Path to Excel file with client data
    """
    print("=" * 70)
    print("AIS PDF IMPORT TEST")
    print("=" * 70)
    
    # Check if PDF exists
    if not os.path.exists(pdf_path):
        print(f"\n❌ ERROR: PDF file not found: {pdf_path}")
        return
    
    # Load client data
    print(f"\n📊 Loading client data from: {excel_path}")
    try:
        df = load_client_data(excel_path)
        print(f"✅ Loaded {len(df)} client records")
    except Exception as e:
        print(f"❌ ERROR loading Excel: {e}")
        return
    
    # Load and parse AIS JSON
    print(f"\n📄 Loading AIS JSON: {pdf_path}")
    
    try:
        # Load JSON file (assuming it's a JSON file, not PDF)
        with open(pdf_path, 'r', encoding='utf-8') as f:
            raw_json = json.load(f)
        
        # Parse the AIS JSON
        result = parse_ais_json(raw_json)
        
        if not result:
            print("❌ ERROR: Parser returned empty result")
            return
        
        # Extract PAN from parsed data (result is AISParsedData object)
        parsed_pan = result.personal_info.pan if hasattr(result, 'personal_info') else None
        
        print(f"\n✅ PDF parsed successfully")
        print(f"📋 Extracted PAN from PDF: {parsed_pan}")
        
        # Find matching client in Excel
        if parsed_pan:
            matching_client = df[df['PAN_cleaned'] == parsed_pan]
            
            if not matching_client.empty:
                client = matching_client.iloc[0]
                print(f"\n🔍 Found matching client in database:")
                print(f"   Name: {client['Name']}")
                print(f"   PAN: {client['PAN']}")
                print(f"   DOB: {client['DOB_formatted']}")
            else:
                print(f"\n⚠️  WARNING: No matching client found in Excel for PAN: {parsed_pan}")
        
        # Display parsed data structure
        print("\n" + "=" * 70)
        print("EXTRACTED DATA STRUCTURE")
        print("=" * 70)
        
        # Display personal info
        if hasattr(result, 'personal_info'):
            pi = result.personal_info
            print("\n👤 Personal Information:")
            print(f"   PAN: {pi.pan}")
            print(f"   Name: {pi.name}")
            print(f"   Address: {pi.address}")
            print(f"   Mobile: {pi.mobile}")
        
        # Display TDS records
        if hasattr(result, 'tds_records') and result.tds_records:
            print(f"\n🧾 TDS Records: {len(result.tds_records)} entries")
            total_tds = sum(rec.tds_amount for rec in result.tds_records)
            print(f"   Total TDS: ₹{total_tds:,.2f}")
            print(f"   First 3 deductors:")
            for rec in result.tds_records[:3]:
                print(f"      - {rec.deductor_name}: ₹{rec.tds_amount:,.2f}")
        
        # Display SFT records
        if hasattr(result, 'sft_records') and result.sft_records:
            print(f"\n🏦 SFT Records: {len(result.sft_records)} entries")
            for rec in result.sft_records[:3]:
                print(f"      - {rec.report_type}: {rec.info_items}")
        
        # Display tax payments
        if hasattr(result, 'tax_payments') and result.tax_payments:
            print(f"\n💳 Tax Payments: {len(result.tax_payments)} entries")
            total_tax = sum(rec.tax_amount for rec in result.tax_payments)
            print(f"   Total Tax Paid: ₹{total_tax:,.2f}")
        
        # Display demands/refunds
        if hasattr(result, 'demands_refunds') and result.demands_refunds:
            print(f"\n📊 Demands/Refunds: {len(result.demands_refunds)} entries")
        
        # Convert result to dict for JSON serialization
        output_file = pdf_path.replace('.json', '_parsed.json')
        result_dict = {
            'personal_info': {
                'pan': result.personal_info.pan,
                'name': result.personal_info.name,
                'address': result.personal_info.address,
                'mobile': result.personal_info.mobile,
            },
            'tds_records_count': len(result.tds_records) if hasattr(result, 'tds_records') else 0,
            'sft_records_count': len(result.sft_records) if hasattr(result, 'sft_records') else 0,
            'tax_payments_count': len(result.tax_payments) if hasattr(result, 'tax_payments') else 0,
        }
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(result_dict, f, indent=2, ensure_ascii=False)
        
        print(f"\n💾 Full parsed data saved to: {output_file}")
        print("\n✅ TEST COMPLETED SUCCESSFULLY")
        
    except Exception as e:
        print(f"\n❌ ERROR during parsing: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python test_ais_import.py <path_to_ais_json>")
        print("\nExample:")
        print("  python test_ais_import.py \"C:/Users/Devansh/Desktop/AIS_Sample.json\"")
        sys.exit(1)
    
    json_path = sys.argv[1]
    excel_path = r"C:\Users\Devansh\Desktop\WINAMN DATA.xlsx"
    
    test_ais_import(json_path, excel_path)
