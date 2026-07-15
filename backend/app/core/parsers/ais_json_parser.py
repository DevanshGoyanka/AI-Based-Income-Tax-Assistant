"""AIS JSON parser - decrypts and parses encrypted AIS JSON files."""
import json
from typing import BinaryIO, Optional
from decimal import Decimal

from app.core.domain.canonical_ais import (
    CanonicalAIS,
    CanonicalAISPersonalInfo,
    CanonicalAISTDS,
    CanonicalAISSFT,
    CanonicalAISTaxPayment,
    CanonicalAISDemandRefund,
)
from app.services.importers.crypto import decrypt_ais_json


class AISJSONParser:
    """Parse encrypted AIS JSON files."""
    
    def parse(self, file: BinaryIO, password: str) -> CanonicalAIS:
        """Parse encrypted AIS JSON.
        
        Args:
            file: Binary file stream
            password: DOB in DDMMYYYY format
            
        Returns:
            Canonical AIS data
        """
        encrypted_text = file.read().decode('utf-8')
        data = decrypt_ais_json(encrypted_text, password)
        
        return self._parse_decrypted(data)
    
    def _parse_decrypted(self, data: dict) -> CanonicalAIS:
        """Parse decrypted ITD AIS JSON structure."""
        
        personal_info = self._extract_personal_info(data)
        ay = self._extract_ay(data)
        
        canonical = CanonicalAIS(
            personal_info=personal_info,
            assessment_year=ay,
            source_format="json"
        )
        
        part_b = data.get("partB", {})
        sections = part_b.get("sections", [])
        
        for section in sections:
            section_key = section.get("sectionKey", "")
            
            if section_key == "tdsTcs":
                self._parse_tds_section(section, canonical)
            elif section_key == "sft":
                self._parse_sft_section(section, canonical)
            elif section_key == "paymentOfTaxes":
                self._parse_tax_payments_section(section, canonical)
            elif section_key == "demandAndRefund":
                self._parse_demands_refunds_section(section, canonical)
        
        return canonical
    
    def _extract_personal_info(self, data: dict) -> CanonicalAISPersonalInfo:
        """Extract personal information from partA."""
        part_a = data.get("partA", {})
        col_data = part_a.get("columnData", [])
        
        return CanonicalAISPersonalInfo(
            pan=col_data[0] if len(col_data) > 0 else "",
            aadhaar=col_data[1] if len(col_data) > 1 else None,
            name=col_data[2] if len(col_data) > 2 else "",
            dob=col_data[3] if len(col_data) > 3 else None,
            mobile=col_data[4] if len(col_data) > 4 else None,
            email=col_data[5] if len(col_data) > 5 else None,
            address=col_data[6] if len(col_data) > 6 else None
        )
    
    def _extract_ay(self, data: dict) -> str:
        """Extract assessment year from header."""
        header = data.get("header", {})
        col_data = header.get("columnData", [])
        return col_data[0] if col_data else "2025-26"
    
    def _parse_tds_section(self, section: dict, canonical: CanonicalAIS):
        """Parse TDS/TCS section from ITD format.
        Column structure: [Category, Code, Description, Source, Count, Amount, CategoryCode, DerivedAmount, QualifiesFor]
        """
        for element in section.get("elements", []):
            title = element.get("title", "")
            l2 = element.get("l2", {})
            col_data = l2.get("columnData", [])
            
            for row in col_data:
                if not row or len(row) < 6:
                    continue
                
                info_category = row[0] if len(row) > 0 else ""
                info_code = row[1] if len(row) > 1 else ""
                info_description = row[2] if len(row) > 2 else ""
                deductor_source = row[3] if len(row) > 3 else ""
                count = int(row[4]) if len(row) > 4 and row[4] else 0
                amount_str = str(row[5]) if len(row) > 5 else "0"
                
                section_code = info_code.replace("TDS-", "").replace("TCS-", "") if info_code else ""
                amount = self._parse_indian_number(amount_str)
                
                deductor_name = deductor_source.split("(")[0].strip() if "(" in deductor_source else deductor_source
                deductor_tan = deductor_source.split("(")[1].replace(")", "").strip() if "(" in deductor_source else ""
                
                canonical.tds_records.append(CanonicalAISTDS(
                    deductor_name=deductor_name,
                    deductor_tan=deductor_tan,
                    section_code=section_code,
                    amount_paid=amount,
                    tax_deducted=amount,
                    tds_deposited=amount,
                    information_category=info_category,
                    information_code=info_code,
                    information_description=info_description,
                    count=count,
                    status="reported",
                    source="ais"
                ))
    
    def _parse_sft_section(self, section: dict, canonical: CanonicalAIS):
        """Parse SFT section from ITD format.
        Column structure: [Category, Code, Description, Source, Count, Amount, CategoryCode, DerivedAmount, QualifiesFor]
        """
        for element in section.get("elements", []):
            title = element.get("title", "").lower()
            l2 = element.get("l2", {})
            col_data = l2.get("columnData", [])
            
            for row in col_data:
                if not row or len(row) < 6:
                    continue
                
                info_category = row[0] if len(row) > 0 else ""
                info_code = row[1] if len(row) > 1 else ""
                info_description = row[2] if len(row) > 2 else ""
                payer_source = row[3] if len(row) > 3 else ""
                count = int(row[4]) if len(row) > 4 and row[4] else 0
                amount_str = str(row[5]) if len(row) > 5 else "0"
                
                payer_name = payer_source.split("(")[0].strip() if "(" in payer_source else payer_source
                
                parts = payer_source.split("(")
                payer_pan = None
                if len(parts) > 1:
                    pan_part = parts[-1].replace(")", "").strip()
                    if len(pan_part) == 10 and pan_part[0:5].isalpha():
                        payer_pan = pan_part
                
                amount = self._parse_indian_number(amount_str)
                
                canonical.sft_records.append(CanonicalAISSFT(
                    transaction_type=title,
                    payer_name=payer_name,
                    payer_pan=payer_pan,
                    information_category=info_category,
                    information_code=info_code,
                    information_description=info_description,
                    count=count,
                    amount=amount,
                    source="ais"
                ))
    
    def _parse_tax_payments_section(self, section: dict, canonical: CanonicalAIS):
        """Parse tax payments section.
        Structure: Direct columnData array without l2 wrapper.
        Columns: [FY/AY, MajorHead, MinorHead, Tax, Surcharge, Cess, Others, Total, BSR, Date, ChallanNo, CIN]
        """
        for element in section.get("elements", []):
            col_data = element.get("columnData", [])
            
            for row in col_data:
                if not row or len(row) < 12:
                    continue
                
                fy_ay = row[0] if len(row) > 0 else ""
                major_head = row[1] if len(row) > 1 else ""
                minor_head = row[2] if len(row) > 2 else ""
                tax = self._parse_indian_number(str(row[3])) if len(row) > 3 else Decimal("0")
                surcharge = self._parse_indian_number(str(row[4])) if len(row) > 4 else Decimal("0")
                cess = self._parse_indian_number(str(row[5])) if len(row) > 5 else Decimal("0")
                others = self._parse_indian_number(str(row[6])) if len(row) > 6 else Decimal("0")
                total = self._parse_indian_number(str(row[7])) if len(row) > 7 else Decimal("0")
                bsr_code = str(row[8]) if len(row) > 8 else ""
                date = str(row[9]) if len(row) > 9 else ""
                challan_no = str(row[10]) if len(row) > 10 else ""
                cin = str(row[11]) if len(row) > 11 else ""
                
                canonical.tax_payments.append(CanonicalAISTaxPayment(
                    bsr_code=bsr_code,
                    challan_serial_no=challan_no,
                    challan_date=date,
                    amount=total,
                    major_head=major_head,
                    minor_head=minor_head,
                    financial_year=fy_ay,
                    tax_amount=tax,
                    surcharge=surcharge,
                    cess=cess,
                    others=others,
                    cin=cin,
                    status="reported",
                    source="ais"
                ))
    
    def _parse_demands_refunds_section(self, section: dict, canonical: CanonicalAIS):
        """Parse demands/refunds section."""
        pass
    
    def _parse_indian_number(self, value_str: str) -> Decimal:
        """Parse Indian number format (12,90,073.00)."""
        if not value_str:
            return Decimal("0")
        cleaned = str(value_str).replace(",", "").strip()
        try:
            return Decimal(cleaned)
        except:
            return Decimal("0")
    
    @staticmethod
    def _to_decimal(value) -> Decimal:
        """Convert value to Decimal."""
        if value is None or value == "":
            return Decimal("0")
        if isinstance(value, Decimal):
            return value
        try:
            return Decimal(str(value).replace(",", ""))
        except:
            return Decimal("0")
