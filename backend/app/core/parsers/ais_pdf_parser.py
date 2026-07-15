"""AIS PDF parser - extracts tables from password-protected AIS PDFs."""
import re
from typing import BinaryIO, Optional, List, Dict, Any
from decimal import Decimal
import pdfplumber

from app.core.domain.canonical_ais import (
    CanonicalAIS,
    CanonicalAISPersonalInfo,
    CanonicalAISTDS,
    CanonicalAISSFT,
    CanonicalAISTaxPayment,
    CanonicalAISDemandRefund,
)


class AISPDFParser:
    """Parse password-protected AIS PDF files with table-based extraction.
    
    Password format: PAN (lowercase) + DOB (DDMMYYYY)
    Example: aaaaa1234a21011991
    
    AIS Structure:
    - Part A: General information (non-tabular header)
    - Part B1: TDS - Summary rows followed by quarterly detail tables
    - Part B2: SFT - Multiple categories with summary + detail tables
    - Part B3: Tax payments
    - Part B4: Demands and refunds
    """
    
    def parse(self, file: BinaryIO, pan: str, dob: str) -> CanonicalAIS:
        """Parse AIS PDF with password.
        
        Args:
            file: Binary file stream
            pan: PAN number (will be lowercased for password)
            dob: Date of birth in DDMMYYYY format
            
        Returns:
            Canonical AIS data
        """
        password = f"{pan.lower()}{dob}"
        
        canonical = CanonicalAIS(
            personal_info=CanonicalAISPersonalInfo(pan=pan.upper(), name=""),
            assessment_year="",
            source_format="pdf"
        )
        
        # Context tracking for multi-row parsing
        parser_context = {
            "current_tds_summary": None,  # Track current TDS deductor context
            "current_sft_summary": None,  # Track current SFT transaction context
            "in_detail_section": False,   # Are we in a detail table?
        }
        
        with pdfplumber.open(file, password=password) as pdf:
            full_text = ""
            
            for page in pdf.pages:
                text = page.extract_text() or ""
                full_text += text + "\n"
                
                tables = page.extract_tables()
                
                for table in tables:
                    if not table or len(table) < 1:
                        continue
                    
                    self._process_table(table, canonical, parser_context)
            
            self._extract_metadata(full_text, canonical)
        
        return canonical
    
    def _extract_metadata(self, text: str, canonical: CanonicalAIS):
        """Extract Part A personal information from PDF text."""
        lines = text.split('\n')
        
        for i, line in enumerate(lines):
            if 'Permanent Account Number' in line or 'PAN' in line:
                pan_match = re.search(r'([A-Z]{5}[0-9]{4}[A-Z])', line)
                if pan_match:
                    canonical.personal_info.pan = pan_match.group(1)
            
            if 'Name of Assessee' in line or (i > 0 and 'PAN' in lines[i-1]):
                name_match = re.search(r'([A-Z]{5}[0-9]{4}[A-Z])\s+[X\s]+\d+\s+([A-Z\s]+)', line)
                if name_match:
                    canonical.personal_info.name = name_match.group(2).strip()
            
            if 'Date of Birth' in line:
                dob_match = re.search(r'(\d{2}/\d{2}/\d{4})', line)
                if dob_match:
                    canonical.personal_info.dob = dob_match.group(1)
                
                mobile_match = re.search(r'(\d{10})', line)
                if mobile_match:
                    canonical.personal_info.mobile = mobile_match.group(1)
                
                email_match = re.search(r'([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})', line)
                if email_match:
                    canonical.personal_info.email = email_match.group(1)
            
            if 'Address' in line and i < len(lines) - 1:
                canonical.personal_info.address = lines[i+1].strip()
            
            if 'Financial Year' in line:
                fy_match = re.search(r'(\d{4}-\d{2})', line)
                if fy_match:
                    canonical.assessment_year = fy_match.group(1)
    
    def _process_table(self, table: List[List[str]], canonical: CanonicalAIS, parser_context: dict):
        """Process a single extracted table."""
        if not table or len(table) < 1:
            return
        
        header_row = self._clean_row(table[0]) if table else []
        
        # Check if this table contains BOTH summary and detail rows (combined table)
        if len(table) > 2:
            second_row = self._clean_row(table[1]) if len(table) > 1 else []
            third_row = self._clean_row(table[2]) if len(table) > 2 else []
            
            # Pattern: Row1=header, Row2=summary data, Row3=detail header, Row4+=detail rows
            if (self._is_tds_summary_row(header_row) and 
                second_row and second_row[0].isdigit() and
                self._is_tds_detail_row(third_row)):
                # Combined TDS table
                self._parse_combined_tds_table(table, canonical, parser_context)
                return
            
            if (self._is_sft_summary_row(header_row) and 
                second_row and second_row[0].isdigit() and
                self._is_sft_detail_row(third_row)):
                # Combined SFT table
                self._parse_combined_sft_table(table, canonical, parser_context)
                return
        
        # Standalone tables
        if self._is_tax_payment_table(header_row):
            self._parse_tax_payment_table(table, canonical)
        elif self._is_refund_table(header_row):
            self._parse_refund_table(table, canonical)
    
    def _is_tds_summary_row(self, header: List[str]) -> bool:
        """Check if this is Part B1 TDS summary table."""
        h = " ".join(header).lower()
        return "information code" in h and "information source" in h and "count" in h
    
    def _is_tds_detail_row(self, header: List[str]) -> bool:
        """Check if this is Part B1 TDS detail (quarterly) table."""
        h = " ".join(header).lower()
        return "quarter" in h and "amount paid" in h and "tds deducted" in h
    
    def _is_sft_summary_row(self, header: List[str]) -> bool:
        """Check if this is Part B2 SFT summary table."""
        h = " ".join(header).lower()
        return "information code" in h and "information source" in h and ("sft" in h or "count" in h)
    
    def _is_sft_detail_row(self, header: List[str]) -> bool:
        """Check if this is Part B2 SFT detail table."""
        h = " ".join(header).lower()
        return ("reported on" in h or "date of sale" in h or "quarter" in h) and \
               ("amount" in h or "interest amount" in h or "dividend amount" in h)
    
    def _is_tax_payment_table(self, header: List[str]) -> bool:
        """Check if this is Part B3 tax payment table."""
        h = " ".join(header).lower()
        return "bsr code" in h and "challan" in h and "major head" in h
    
    def _is_refund_table(self, header: List[str]) -> bool:
        """Check if this is Part B4 refund table."""
        h = " ".join(header).lower()
        return "refund" in h and ("mode" in h or "nature" in h)
    
    def _parse_combined_tds_table(self, table: List[List[str]], canonical: CanonicalAIS, parser_context: dict):
        """Parse combined TDS table (summary + details in one table)."""
        if len(table) < 4:
            return
        
        # Row 2 contains summary data
        summary_row = self._clean_row(table[1])
        
        # Column mapping: [0]=SR, [1]=CODE, [3]=DESC, [5]=SOURCE, [8]=COUNT, [9]=AMOUNT
        info_code = summary_row[1] if len(summary_row) > 1 else ""
        info_desc = summary_row[3] if len(summary_row) > 3 else ""
        source = summary_row[5] if len(summary_row) > 5 else ""
        
        deductor_name, tan = self._extract_name_and_tan(source)
        section = self._extract_section_from_code(info_code)
        category = self._extract_category_from_desc(info_desc)
        
        # Row 3: Detail header
        # Row 4+: Detail rows
        for i in range(3, len(table)):
            row = self._clean_row(table[i])
            if len(row) < 6 or not row[0] or not row[0].isdigit():
                continue
            
            try:
                canonical.tds_records.append(CanonicalAISTDS(
                    deductor_name=deductor_name,
                    deductor_tan=tan,
                    section_code=section,
                    date_of_deduction=row[2] if len(row) > 2 else "",
                    amount_paid=self._to_decimal(row[4]) if len(row) > 4 else Decimal("0"),
                    tax_deducted=self._to_decimal(row[6]) if len(row) > 6 else Decimal("0"),
                    tds_deposited=self._to_decimal(row[7]) if len(row) > 7 else Decimal("0"),
                    quarter=row[1] if len(row) > 1 else None,
                    status=row[10] if len(row) > 10 else None,
                    information_code=info_code,
                    information_description=info_desc,
                    information_category=category,
                    count=1,
                    source="ais"
                ))
            except (ValueError, IndexError):
                continue
    
    def _parse_combined_sft_table(self, table: List[List[str]], canonical: CanonicalAIS, parser_context: dict):
        """Parse combined SFT table (summary + details in one table)."""
        if len(table) < 4:
            return
        
        # Row 2 contains summary data
        summary_row = self._clean_row(table[1])
        
        # Column mapping varies but typically: [0]=SR, [1]=CODE, [4 or 2]=DESC, [5 or 3]=SOURCE
        info_code = summary_row[1] if len(summary_row) > 1 else ""
        
        # Description is in column 4 or 2 (whichever exists and is longer)
        info_desc = summary_row[4] if len(summary_row) > 4 and summary_row[4] else \
                    summary_row[2] if len(summary_row) > 2 else ""
        
        # Source is in column 5 or 3 (has PAN in parens)
        source = summary_row[5] if len(summary_row) > 5 and summary_row[5] else \
                 summary_row[3] if len(summary_row) > 3 else ""
        
        payer_name, payer_pan = self._extract_name_and_pan(source)
        transaction_type = self._infer_sft_transaction_type(info_code, info_desc)
        category = self._extract_category_from_desc(info_desc)
        
        # Row 3: Detail header - find amount column
        detail_header = self._clean_row(table[2]) if len(table) > 2 else []
        amount_col_idx = -1
        for idx, cell in enumerate(detail_header):
            if cell and any(kw in cell.lower() for kw in 
                           ["amount", "interest amount", "dividend amount", "consideration", 
                            "purchase amount", "sale price", "sales consideration"]):
                amount_col_idx = idx
                break
        
        # Row 4+: Detail rows
        for i in range(3, len(table)):
            row = self._clean_row(table[i])
            if len(row) < 2 or not row[0] or not row[0].isdigit():
                continue
            
            try:
                amount = Decimal("0")
                reported_on = row[1] if len(row) > 1 else ""
                
                # Try amount column from header first
                if amount_col_idx > 0 and len(row) > amount_col_idx:
                    amount = self._to_decimal(row[amount_col_idx])
                
                # Fallback: find reasonable amount (exclude account numbers)
                if amount == Decimal("0"):
                    for cell in row[2:]:
                        amt = self._to_decimal(cell)
                        if Decimal("0") < amt < Decimal("1000000000"):
                            if amt > amount:
                                amount = amt
                
                canonical.sft_records.append(CanonicalAISSFT(
                    transaction_type=transaction_type,
                    payer_name=payer_name,
                    payer_pan=payer_pan,
                    amount=amount,
                    date=reported_on,
                    information_code=info_code,
                    information_description=info_desc,
                    information_category=category,
                    count=1,
                    source="ais"
                ))
            except (ValueError, IndexError):
                continue
    
    def _parse_tds_summary_and_details(self, table: List[List[str]], canonical: CanonicalAIS, parser_context: dict):
        """Parse Part B1 TDS summary rows.
        Format: SR.NO | INFO CODE | INFO DESC | INFO SOURCE | COUNT | AMOUNT
        """
        for i, row in enumerate(table):
            if i == 0:
                continue
            
            row = self._clean_row(row)
            if len(row) < 5 or not row[0] or not row[0].isdigit():
                continue
            
            info_code = row[1] if len(row) > 1 else ""
            info_desc = row[2] if len(row) > 2 else ""
            source = row[3] if len(row) > 3 else ""
            count = int(row[4]) if len(row) > 4 and row[4].isdigit() else 0
            amount = self._to_decimal(row[5]) if len(row) > 5 else Decimal("0")
            
            deductor_name, tan = self._extract_name_and_tan(source)
            section = self._extract_section_from_code(info_code)
            category = self._extract_category_from_desc(info_desc)
            
            parser_context["current_tds_summary"] = {
                "deductor_name": deductor_name,
                "tan": tan,
                "section": section,
                "info_code": info_code,
                "info_desc": info_desc,
                "category": category,
                "total_count": count,
                "total_amount": amount
            }
    
    def _parse_tds_detail_rows(self, table: List[List[str]], canonical: CanonicalAIS, parser_context: dict):
        """Parse Part B1 TDS quarterly detail rows.
        Format: SR.NO | QUARTER | DATE | AMOUNT PAID | TDS DEDUCTED | TDS DEPOSITED | STATUS
        """
        if not parser_context.get("current_tds_summary"):
            return
        
        summary = parser_context["current_tds_summary"]
        
        for i, row in enumerate(table):
            if i == 0:
                continue
            
            row = self._clean_row(row)
            if len(row) < 6 or not row[0] or not row[0].isdigit():
                continue
            
            try:
                canonical.tds_records.append(CanonicalAISTDS(
                    deductor_name=summary["deductor_name"],
                    deductor_tan=summary["tan"],
                    section_code=summary["section"],
                    transaction_date=row[2] if len(row) > 2 else "",
                    amount_paid=self._to_decimal(row[3]) if len(row) > 3 else Decimal("0"),
                    tax_deducted=self._to_decimal(row[4]) if len(row) > 4 else Decimal("0"),
                    tds_deposited=self._to_decimal(row[5]) if len(row) > 5 else Decimal("0"),
                    quarter=row[1] if len(row) > 1 else None,
                    status=row[6] if len(row) > 6 else None,
                    information_code=summary["info_code"],
                    information_description=summary["info_desc"],
                    information_category=summary["category"],
                    count=1,
                    source="ais"
                ))
            except (ValueError, IndexError):
                continue
    
    def _parse_sft_summary_and_details(self, table: List[List[str]], canonical: CanonicalAIS, parser_context: dict):
        """Parse Part B2 SFT summary rows.
        Format: SR.NO | INFO CODE | INFO DESC | INFO SOURCE | COUNT | AMOUNT
        """
        for i, row in enumerate(table):
            if i == 0:
                continue
            
            row = self._clean_row(row)
            if len(row) < 5 or not row[0] or not row[0].isdigit():
                continue
            
            info_code = row[1] if len(row) > 1 else ""
            info_desc = row[2] if len(row) > 2 else ""
            source = row[3] if len(row) > 3 else ""
            count = int(row[4]) if len(row) > 4 and row[4].isdigit() else 0
            amount = self._to_decimal(row[5]) if len(row) > 5 else Decimal("0")
            
            payer_name, payer_pan = self._extract_name_and_pan(source)
            transaction_type = self._infer_sft_transaction_type(info_code, info_desc)
            category = self._extract_category_from_desc(info_desc)
            
            parser_context["current_sft_summary"] = {
                "payer_name": payer_name,
                "payer_pan": payer_pan,
                "info_code": info_code,
                "info_desc": info_desc,
                "transaction_type": transaction_type,
                "category": category,
                "total_count": count,
                "total_amount": amount
            }
    
    def _parse_sft_detail_rows(self, table: List[List[str]], canonical: CanonicalAIS, parser_context: dict):
        """Parse Part B2 SFT detail rows (varied structure per transaction type)."""
        if not parser_context.get("current_sft_summary"):
            return
        
        summary = parser_context["current_sft_summary"]
        
        for i, row in enumerate(table):
            if i == 0:
                continue
            
            row = self._clean_row(row)
            if len(row) < 2 or not row[0] or not row[0].isdigit():
                continue
            
            try:
                amount = Decimal("0")
                reported_on = ""
                
                if "dividend" in summary["transaction_type"].lower():
                    reported_on = row[1] if len(row) > 1 else ""
                    amount = self._to_decimal(row[2]) if len(row) > 2 else Decimal("0")
                elif "interest" in summary["transaction_type"].lower():
                    reported_on = row[1] if len(row) > 1 else ""
                    amount = self._to_decimal(row[4]) if len(row) > 4 else self._to_decimal(row[3])
                elif "purchase" in summary["transaction_type"].lower() or "sale" in summary["transaction_type"].lower():
                    reported_on = row[1] if len(row) > 1 else ""
                    for cell in row[2:]:
                        dec = self._to_decimal(cell)
                        if dec > amount:
                            amount = dec
                
                canonical.sft_records.append(CanonicalAISSFT(
                    transaction_type=summary["transaction_type"],
                    payer_name=summary["payer_name"],
                    payer_pan=summary["payer_pan"],
                    amount=amount,
                    date=reported_on,
                    information_code=summary["info_code"],
                    information_description=summary["info_desc"],
                    information_category=summary["category"],
                    count=1,
                    source="ais"
                ))
            except (ValueError, IndexError):
                continue
    
    def _parse_tax_payment_table(self, table: List[List[str]], canonical: CanonicalAIS):
        """Parse Part B3 tax payment table.
        Format: SR | FY | MAJOR | MINOR | TAX | SURCHARGE | CESS | OTHERS | TOTAL | BSR | DATE | CHALLAN | CIN
        """
        for i, row in enumerate(table):
            if i == 0:
                continue
            
            row = self._clean_row(row)
            if len(row) < 12 or not row[0] or not row[0].isdigit():
                continue
            
            try:
                canonical.tax_payments.append(CanonicalAISTaxPayment(
                    financial_year=row[1] if len(row) > 1 else "",
                    major_head=row[2] if len(row) > 2 else "",
                    minor_head=row[3] if len(row) > 3 else "",
                    tax_amount=self._to_decimal(row[4]) if len(row) > 4 else Decimal("0"),
                    surcharge=self._to_decimal(row[5]) if len(row) > 5 else Decimal("0"),
                    cess=self._to_decimal(row[6]) if len(row) > 6 else Decimal("0"),
                    others=self._to_decimal(row[7]) if len(row) > 7 else Decimal("0"),
                    amount=self._to_decimal(row[8]) if len(row) > 8 else Decimal("0"),
                    bsr_code=row[9] if len(row) > 9 else "",
                    challan_date=row[10] if len(row) > 10 else "",
                    challan_serial_no=row[11] if len(row) > 11 else "",
                    cin=row[12] if len(row) > 12 else None,
                    source="ais"
                ))
            except (ValueError, IndexError):
                continue
    
    def _parse_refund_table(self, table: List[List[str]], canonical: CanonicalAIS):
        """Parse Part B4 refund/demand table.
        Format: SR.NO | AY | MODE | NATURE | AMOUNT | DATE | STATUS
        """
        for i, row in enumerate(table):
            if i == 0:
                continue
            
            row = self._clean_row(row)
            if len(row) < 4 or not row[0] or not row[0].isdigit():
                continue
            
            try:
                # Determine if demand or refund based on nature/mode column
                amount = self._to_decimal(row[4]) if len(row) > 4 else Decimal("0")
                nature = (row[3] if len(row) > 3 else "").lower()
                
                is_demand = "demand" in nature
                
                canonical.demands_refunds.append(CanonicalAISDemandRefund(
                    assessment_year=row[1] if len(row) > 1 else "",
                    demand_amount=amount if is_demand else Decimal("0"),
                    refund_amount=amount if not is_demand else Decimal("0"),
                    date=row[5] if len(row) > 5 else None,
                    status=row[6] if len(row) > 6 else "reported",
                    source="ais"
                ))
            except (ValueError, IndexError):
                continue
    
    
    def _extract_name_and_tan(self, source: str) -> tuple:
        """Extract deductor name and TAN from source string.
        Format: 'NAME (TAN)' or 'NAME'
        """
        match = re.search(r'(.+?)\s*\(([A-Z]{4}\d{5}[A-Z])\)', source)
        if match:
            return match.group(1).strip(), match.group(2).strip()
        return source.strip(), ""
    
    def _extract_name_and_pan(self, source: str) -> tuple:
        """Extract payer name and PAN from source string.
        Format: 'NAME (PAN)' or 'NAME (PAN.CODE)'
        """
        match = re.search(r'(.+?)\s*\(([A-Z]{5}\d{4}[A-Z])', source)
        if match:
            return match.group(1).strip(), match.group(2).strip()
        return source.strip(), None
    
    def _extract_section_from_code(self, code: str) -> str:
        """Extract section from info code like 'TDS-194A' -> '194A'."""
        match = re.search(r'(19[0-9][A-Z]?)', code)
        return match.group(1) if match else ""
    
    def _extract_category_from_desc(self, desc: str) -> str:
        """Extract category from description."""
        if "salary" in desc.lower():
            return "Salary"
        elif "interest" in desc.lower():
            return "Interest from deposit"
        elif "dividend" in desc.lower():
            return "Dividend"
        elif "rent" in desc.lower():
            return "Rent"
        elif "commission" in desc.lower():
            return "Commission"
        elif "professional" in desc.lower():
            return "Professional fees"
        return "Other"
    
    def _infer_sft_transaction_type(self, code: str, desc: str) -> str:
        """Infer SFT transaction type from code and description."""
        desc_lower = desc.lower()
        
        if "dividend" in desc_lower:
            return "dividend"
        elif "interest" in desc_lower:
            if "saving" in desc_lower:
                return "interest_savings"
            elif "term" in desc_lower or "deposit" in desc_lower:
                return "interest_deposit"
            return "interest"
        elif "mutual fund" in desc_lower or "sft-18" in code.lower():
            if "purchase" in desc_lower:
                return "mutual_fund_purchase"
            elif "sale" in desc_lower:
                return "mutual_fund_sale"
            return "mutual_fund"
        elif "securities" in desc_lower or "sft-17" in code.lower():
            if "purchase" in desc_lower:
                return "securities_purchase"
            elif "sale" in desc_lower:
                return "securities_sale"
            return "securities"
        elif "sft-005" in code.lower():
            return "time_deposit_purchase"
        
        return "other"
    
    def _clean_row(self, row: List[str]) -> List[str]:
        """Clean row data."""
        return [self._clean_cell(cell) for cell in row]
    
    def _clean_cell(self, cell: Optional[str]) -> str:
        """Clean cell text."""
        if not cell:
            return ""
        return re.sub(r'\s+', ' ', cell.strip())
    
    @staticmethod
    def _to_decimal(value) -> Decimal:
        """Convert value to Decimal, handling Indian number format."""
        if value is None or value == "":
            return Decimal("0")
        if isinstance(value, Decimal):
            return value
        try:
            cleaned = str(value).replace(",", "").strip()
            return Decimal(cleaned) if cleaned else Decimal("0")
        except:
            return Decimal("0")
