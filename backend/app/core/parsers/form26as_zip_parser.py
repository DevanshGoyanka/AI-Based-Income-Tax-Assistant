"""26AS ZIP parser - extracts password-protected TXT file from TRACES."""
import io
import re
from decimal import Decimal
from typing import BinaryIO, Dict, List
import pyzipper

from app.core.domain.canonical_26as import (
    Canonical26AS,
    Canonical26ASPartI,
    Canonical26ASPartII,
    Canonical26ASPartIII,
    Canonical26ASPartIV,
    Canonical26ASPartV,
    Canonical26ASPartVI,
    Canonical26ASPartVII,
    Canonical26ASPartVIII,
    Canonical26ASPartIX,
    Canonical26ASPartX,
)


class Form26ASZipParser:
    """Parse password-protected ZIP containing ^-delimited TXT file."""
    
    DELIMITER = "^"
    
    def parse(self, file: BinaryIO, password: str) -> Canonical26AS:
        """Extract and parse 26AS from password-protected ZIP.
        
        Args:
            file: ZIP file binary stream
            password: DOB in DDMMYYYY format
            
        Returns:
            Canonical26AS with all parts extracted
        """
        txt_content = self._extract_txt_from_zip(file, password)
        return self._parse_txt(txt_content)
    
    def _extract_txt_from_zip(self, file: BinaryIO, password: str) -> str:
        """Extract TXT file from password-protected ZIP."""
        with pyzipper.AESZipFile(file) as zf:
            zf.setpassword(password.encode())
            
            txt_files = [name for name in zf.namelist() if name.endswith('.txt')]
            if not txt_files:
                raise ValueError("No TXT file found in ZIP")
            
            with zf.open(txt_files[0]) as txt_file:
                return txt_file.read().decode('utf-8-sig')
    
    def _parse_txt(self, content: str) -> Canonical26AS:
        """Parse ^-delimited TXT content."""
        lines = content.strip().split('\n')
        
        canonical = Canonical26AS(
            pan="",
            name="",
            assessment_year="",
            source_format="zip"
        )
        
        current_part = None
        current_deductor_name = None
        current_deductor_tan = None
        
        for line in lines:
            line = line.strip()
            if not line or line == self.DELIMITER:
                continue
            
            fields = [f.strip() for f in line.split(self.DELIMITER)]
            
            if "PART-I" in line:
                current_part = "PART-I"
                continue
            
            if "PART-II" in line:
                current_part = "PART-II"
                continue
            
            if "PART-VI" in line:
                current_part = "PART-VI"
                continue
            
            # Metadata line: Date^PAN^Status^FY^AY^Name^Address...
            if len(fields) >= 7 and fields[1] and len(fields[1]) == 10 and fields[1][5].isdigit():
                canonical.pan = fields[1]
                canonical.name = fields[5]
                canonical.assessment_year = fields[4]
                continue
            
            if current_part == "PART-I":
                # Check if this is a deductor summary row or transaction row
                # Summary: Sr^Name^TAN^^^^^TotalAmt^TotalTax^TotalDep (TAN at index 2)
                # Transaction: ^Sr^Section^Date^Status^BookDate^Remarks^Amt^Tax^Dep (empty first field)
                
                if fields[0] and fields[0].isdigit() and len(fields) > 2 and len(fields[2]) == 10:
                    # Deductor summary row
                    current_deductor_name = fields[1]
                    current_deductor_tan = fields[2]
                
                elif not fields[0] and len(fields) > 1 and fields[1] and fields[1].isdigit():
                    # Transaction detail row
                    entry = self._parse_part_i_transaction(fields, current_deductor_name, current_deductor_tan)
                    if entry:
                        canonical.part_i.append(entry)
            
            elif current_part == "PART-II":
                if fields and fields[0] and not any(h in fields[0].lower() for h in ["sr", "deductor", "name of"]):
                    entry = self._parse_part_ii(fields)
                    if entry:
                        canonical.part_ii.append(entry)
            
            elif current_part == "PART-III":
                if fields and fields[0] and not any(h in fields[0].lower() for h in ["sr", "acknowledgement", "ack"]):
                    entry = self._parse_part_iii(fields)
                    if entry:
                        canonical.part_iii.append(entry)
            
            elif current_part == "PART-IV":
                if fields and fields[0] and not any(h in fields[0].lower() for h in ["sr", "acknowledgement", "ack"]):
                    entry = self._parse_part_iv(fields)
                    if entry:
                        canonical.part_iv.append(entry)
            
            elif current_part == "PART-V":
                if fields and fields[0] and not any(h in fields[0].lower() for h in ["sr", "acknowledgement", "ack"]):
                    entry = self._parse_part_v(fields)
                    if entry:
                        canonical.part_v.append(entry)
            
            elif current_part == "PART-VI":
                if fields and fields[0] and not any(h in fields[0].lower() for h in ["sr", "collector", "name of"]):
                    entry = self._parse_part_vi(fields)
                    if entry:
                        canonical.part_vi.append(entry)
            
            elif current_part == "PART-VII":
                if fields and fields[0] and not any(h in fields[0].lower() for h in ["sr", "assessment", "a.y"]):
                    entry = self._parse_part_vii(fields)
                    if entry:
                        canonical.part_vii.append(entry)
            
            elif current_part == "PART-VIII":
                if fields and fields[0] and not any(h in fields[0].lower() for h in ["sr", "acknowledgement", "ack"]):
                    entry = self._parse_part_viii(fields)
                    if entry:
                        canonical.part_viii.append(entry)
            
            elif current_part == "PART-IX":
                if fields and fields[0] and not any(h in fields[0].lower() for h in ["sr", "acknowledgement", "ack"]):
                    entry = self._parse_part_ix(fields)
                    if entry:
                        canonical.part_ix.append(entry)
            
            elif current_part == "PART-X":
                if fields and fields[0] and not any(h in fields[0].lower() for h in ["sr", "financial"]):
                    entry = self._parse_part_x(fields)
                    if entry:
                        canonical.part_x.append(entry)
        
        return canonical
    
    def _parse_part_i_transaction(self, fields: List[str], deductor_name: str, deductor_tan: str) -> Canonical26ASPartI:
        """Parse Part I TDS transaction record.
        Format: ^Sr^Section^Date^Status^BookDate^Remarks^Amt^Tax^Dep
        """
        if len(fields) < 9:
            return None
        
        try:
            return Canonical26ASPartI(
                deductor_name=deductor_name or "",
                deductor_tan=deductor_tan or "",
                section_code=fields[2],
                transaction_date=fields[3],
                amount_paid=self._to_decimal(fields[7]),
                tax_deducted=self._to_decimal(fields[8]),
                tds_deposited=self._to_decimal(fields[9]) if len(fields) > 9 else Decimal("0"),
                status_of_booking=fields[4] if len(fields) > 4 else None,
                date_of_booking=fields[5] if len(fields) > 5 else None,
                remarks=fields[6] if len(fields) > 6 and fields[6] != "-" else None,
            )
        except (ValueError, IndexError):
            return None
    
    def _parse_part_ii(self, fields: List[str]) -> Canonical26ASPartII:
        """Parse Part II 15G/15H record."""
        if len(fields) < 5 or not fields[0] or fields[0].lower() in ["deductor", "sr_no", "sr. no.", "name of deductor"]:
            return None
        
        try:
            return Canonical26ASPartII(
                deductor_name=fields[0],
                deductor_tan=fields[1],
                amount_paid=self._to_decimal(fields[2]),
                tax_deducted=self._to_decimal(fields[3]),
                tds_deposited=self._to_decimal(fields[4]),
            )
        except (ValueError, IndexError):
            return None
    
    def _parse_part_iii(self, fields: List[str]) -> Canonical26ASPartIII:
        """Parse Part III transaction record."""
        if len(fields) < 4 or fields[0] in ["ACK_NO", "SR_NO"]:
            return None
        
        try:
            return Canonical26ASPartIII(
                acknowledgement_number=fields[0],
                deductor_name=fields[1],
                deductor_pan=fields[2],
                amount_paid=self._to_decimal(fields[3]),
            )
        except (ValueError, IndexError):
            return None
    
    def _parse_part_iv(self, fields: List[str]) -> Canonical26ASPartIV:
        """Parse Part IV seller/landlord TDS record."""
        if len(fields) < 6 or fields[0] in ["ACK_NO", "SR_NO"]:
            return None
        
        try:
            return Canonical26ASPartIV(
                acknowledgement_number=fields[0],
                deductor_name=fields[1],
                deductor_pan=fields[2],
                transaction_date=fields[3],
                transaction_amount=self._to_decimal(fields[4]),
                tds_deposited=self._to_decimal(fields[5]),
            )
        except (ValueError, IndexError):
            return None
    
    def _parse_part_v(self, fields: List[str]) -> Canonical26ASPartV:
        """Parse Part V VDA transaction record."""
        if len(fields) < 5 or fields[0] in ["ACK_NO", "SR_NO"]:
            return None
        
        try:
            return Canonical26ASPartV(
                acknowledgement_number=fields[0],
                buyer_name=fields[1],
                buyer_pan=fields[2],
                transaction_date=fields[3],
                transaction_amount=self._to_decimal(fields[4]),
            )
        except (ValueError, IndexError):
            return None
    
    def _parse_part_vi(self, fields: List[str]) -> Canonical26ASPartVI:
        """Parse Part VI TCS record."""
        if len(fields) < 5 or not fields[0] or fields[0].lower() in ["collector", "sr_no", "sr. no.", "name of collector"]:
            return None
        
        if not fields[1] or len(fields[1]) != 10:
            return None
        
        try:
            return Canonical26ASPartVI(
                collector_name=fields[0],
                collector_tan=fields[1],
                section_code=fields[2] if len(fields) > 2 else None,
                amount_paid=self._to_decimal(fields[3]) if len(fields) > 3 else Decimal("0"),
                tax_collected=self._to_decimal(fields[4]) if len(fields) > 4 else Decimal("0"),
                tcs_deposited=self._to_decimal(fields[5]) if len(fields) > 5 else Decimal("0"),
            )
        except (ValueError, IndexError):
            return None
    
    def _parse_part_vii(self, fields: List[str]) -> Canonical26ASPartVII:
        """Parse Part VII refund record."""
        if len(fields) < 8 or fields[0] in ["AY", "SR_NO"]:
            return None
        
        try:
            return Canonical26ASPartVII(
                assessment_year=fields[0],
                mode=fields[1],
                refund_issued=fields[2],
                nature_of_refund=fields[3],
                amount_of_refund=self._to_decimal(fields[4]),
                interest=self._to_decimal(fields[5]),
                date_of_payment=fields[6],
                remarks=fields[7] if len(fields) > 7 else None,
            )
        except (ValueError, IndexError):
            return None
    
    def _parse_part_viii(self, fields: List[str]) -> Canonical26ASPartVIII:
        """Parse Part VIII buyer/tenant TDS record."""
        if len(fields) < 7 or fields[0] in ["ACK_NO", "SR_NO"]:
            return None
        
        try:
            return Canonical26ASPartVIII(
                acknowledgement_number=fields[0],
                deductee_name=fields[1],
                deductee_pan=fields[2],
                transaction_date=fields[3],
                transaction_amount=self._to_decimal(fields[4]),
                tds_deposited=self._to_decimal(fields[5]),
                amount_deposited_other_than_tds=self._to_decimal(fields[6]),
            )
        except (ValueError, IndexError):
            return None
    
    def _parse_part_ix(self, fields: List[str]) -> Canonical26ASPartIX:
        """Parse Part IX VDA seller record."""
        if len(fields) < 6 or fields[0] in ["ACK_NO", "SR_NO"]:
            return None
        
        try:
            return Canonical26ASPartIX(
                acknowledgement_number=fields[0],
                seller_name=fields[1],
                seller_pan=fields[2],
                transaction_date=fields[3],
                transaction_amount=self._to_decimal(fields[4]),
                amount_deposited_other_than_tds=self._to_decimal(fields[5]),
            )
        except (ValueError, IndexError):
            return None
    
    def _parse_part_x(self, fields: List[str]) -> Canonical26ASPartX:
        """Parse Part X defaults record."""
        if len(fields) < 8 or fields[0] in ["FY", "SR_NO"]:
            return None
        
        try:
            return Canonical26ASPartX(
                financial_year=fields[0],
                short_payment=self._to_decimal(fields[1]),
                short_deduction_collection=self._to_decimal(fields[2]),
                interest_on_tds_tcs_payments_default=self._to_decimal(fields[3]),
                interest_on_tds_tcs_deduction_default=self._to_decimal(fields[4]),
                late_filing_fee_234e=self._to_decimal(fields[5]),
                interest_220_2=self._to_decimal(fields[6]),
                total_default=self._to_decimal(fields[7]),
            )
        except (ValueError, IndexError):
            return None
    
    def _to_decimal(self, value: str) -> Decimal:
        """Convert string to Decimal, handling commas."""
        if not value or value.strip() == "":
            return Decimal("0")
        
        cleaned = value.replace(",", "").strip()
        try:
            return Decimal(cleaned)
        except:
            return Decimal("0")
