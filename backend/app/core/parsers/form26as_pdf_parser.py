"""26AS PDF parser - extracts tables from Form 26AS PDF."""
import re
from decimal import Decimal
from typing import BinaryIO, List, Optional
import pdfplumber

from app.core.domain.canonical_26as import (
    Canonical26AS,
    Canonical26ASPartI,
    Canonical26ASPartII,
    Canonical26ASPartVI,
    Canonical26ASPartVII,
)


class Form26ASPDFParser:
    """Parse Form 26AS PDF with password protection."""
    
    def parse(self, file: BinaryIO, password: Optional[str] = None) -> Canonical26AS:
        """Extract and parse 26AS from PDF.
        
        Args:
            file: PDF file binary stream
            password: DOB in DDMMYYYY format (if encrypted)
            
        Returns:
            Canonical26AS with extracted parts
        """
        canonical = Canonical26AS(
            pan="",
            name="",
            assessment_year="",
            source_format="pdf"
        )
        
        # Track deductor context across pages
        parser_context = {
            "current_deductor": None,
            "current_tan": None,
            "deductor_summaries": {}  # TAN -> {total_amount, total_tax, total_deposited}
        }
        
        with pdfplumber.open(file, password=password) as pdf:
            full_text = ""
            
            for page in pdf.pages:
                text = page.extract_text() or ""
                full_text += text + "\n"
                
                tables = page.extract_tables()
                
                for table in tables:
                    if not table or len(table) < 2:
                        continue
                    
                    self._process_table(table, canonical, parser_context)
            
            self._extract_metadata(full_text, canonical)
            self._validate_totals(canonical, parser_context)
        
        return canonical
    
    def _extract_metadata(self, text: str, canonical: Canonical26AS):
        """Extract PAN, name, and AY from PDF text."""
        pan_match = re.search(r'PAN\s*:?\s*([A-Z]{5}[0-9]{4}[A-Z])', text)
        if pan_match:
            canonical.pan = pan_match.group(1)
        
        name_match = re.search(r'Name\s*:?\s*([A-Z][A-Z\s]+?)(?:\n|PAN)', text, re.IGNORECASE)
        if name_match:
            canonical.name = name_match.group(1).strip()
        
        ay_match = re.search(r'Assessment\s+Year\s*:?\s*(\d{4}-\d{2})', text, re.IGNORECASE)
        if ay_match:
            canonical.assessment_year = ay_match.group(1)
    
    def _process_table(self, table: List[List[str]], canonical: Canonical26AS, parser_context: dict):
        """Process a single table from the PDF."""
        if len(table) < 2:
            return
        
        for i, row in enumerate(table):
            if len(row) < 2:
                continue
            
            row_clean = [self._clean(cell) for cell in row]
            
            # Deductor summary row: Sr^Name^...^TAN^...^TotalAmt^TotalTax^TotalDep
            if row_clean[0] and row_clean[0].isdigit() and len(row_clean) > 5:
                if self._clean(row_clean[5]) and len(self._clean(row_clean[5])) == 10:
                    parser_context["current_deductor"] = row_clean[1]
                    parser_context["current_tan"] = row_clean[5]
                    
                    # Extract summary totals
                    if len(row_clean) >= 9:
                        parser_context["deductor_summaries"][row_clean[5]] = {
                            "total_amount": self._to_decimal(row_clean[6]) if len(row_clean) > 6 else Decimal("0"),
                            "total_tax": self._to_decimal(row_clean[7]) if len(row_clean) > 7 else Decimal("0"),
                            "total_deposited": self._to_decimal(row_clean[8]) if len(row_clean) > 8 else Decimal("0"),
                        }
                
                # Transaction detail row
                elif row_clean[1] and row_clean[1][0].isdigit():
                    if parser_context["current_deductor"] and parser_context["current_tan"]:
                        self._parse_transaction_row(row_clean, parser_context["current_deductor"], 
                                                    parser_context["current_tan"], canonical)
    
    def _is_part_i_summary(self, header: List[str]) -> bool:
        """Check if this is Part I summary table."""
        header_text = " ".join(header).lower()
        return ("deductor" in header_text or "name of deductor" in header_text) and \
               ("tan" in header_text) and \
               ("amount paid" in header_text or "amount credited" in header_text)
    
    def _is_part_i_transaction(self, header: List[str]) -> bool:
        """Check if this is Part I transaction table."""
        header_text = " ".join(header).lower()
        return ("section" in header_text) and \
               ("transaction date" in header_text or "date" in header_text) and \
               ("tax deducted" in header_text or "tds" in header_text)
    
    def _is_part_ii(self, header: List[str]) -> bool:
        """Check if this is Part II table."""
        header_text = " ".join(header).lower()
        return "15g" in header_text or "15h" in header_text or \
               ("deductor" in header_text and "certificate" in header_text)
    
    def _is_part_vi(self, header: List[str]) -> bool:
        """Check if this is Part VI TCS table."""
        header_text = " ".join(header).lower()
        return ("collector" in header_text) and \
               ("tax collected" in header_text or "tcs" in header_text)
    
    def _is_part_vii(self, header: List[str]) -> bool:
        """Check if this is Part VII refund table."""
        header_text = " ".join(header).lower()
        return ("refund" in header_text) and \
               ("assessment year" in header_text or "a.y" in header_text)
    
    
    def _validate_totals(self, canonical: Canonical26AS, parser_context: dict):
        """Validate calculated totals against summary totals."""
        if not parser_context["deductor_summaries"]:
            return
        
        # Group entries by TAN
        tan_entries = {}
        for entry in canonical.part_i:
            tan = entry.deductor_tan
            if tan not in tan_entries:
                tan_entries[tan] = []
            tan_entries[tan].append(entry)
        
        # Validate each deductor
        for tan, summary in parser_context["deductor_summaries"].items():
            if tan not in tan_entries:
                continue
            
            entries = tan_entries[tan]
            calc_amount = sum(e.amount_paid for e in entries)
            calc_tax = sum(e.tax_deducted for e in entries)
            calc_deposited = sum(e.tds_deposited for e in entries)
            
            # Allow small rounding differences (0.01)
            if abs(calc_amount - summary["total_amount"]) > Decimal("0.01"):
                print(f"WARNING: TAN {tan} amount mismatch - Summary: {summary['total_amount']}, Calculated: {calc_amount}")
            
            if abs(calc_tax - summary["total_tax"]) > Decimal("0.01"):
                print(f"WARNING: TAN {tan} tax mismatch - Summary: {summary['total_tax']}, Calculated: {calc_tax}")
            
            if abs(calc_deposited - summary["total_deposited"]) > Decimal("0.01"):
                print(f"WARNING: TAN {tan} deposited mismatch - Summary: {summary['total_deposited']}, Calculated: {calc_deposited}")
    
    def _parse_transaction_row(self, row: List[str], deductor: str, tan: str, canonical: Canonical26AS):
        """Parse a single transaction row."""
        try:
            section = row[1] if len(row) > 1 else ""
            if not section or not section[0].isdigit():
                return
            
            canonical.part_i.append(Canonical26ASPartI(
                deductor_name=deductor,
                deductor_tan=tan,
                section_code=section,
                transaction_date=row[2] if len(row) > 2 else "",
                amount_paid=self._to_decimal(row[6]) if len(row) > 6 else Decimal("0"),
                tax_deducted=self._to_decimal(row[7]) if len(row) > 7 else Decimal("0"),
                tds_deposited=self._to_decimal(row[8]) if len(row) > 8 else Decimal("0"),
                status_of_booking=row[3] if len(row) > 3 else None,
                date_of_booking=row[4] if len(row) > 4 else None,
                remarks=row[5] if len(row) > 5 else None,
            ))
        except (ValueError, IndexError):
            pass
    
    def _parse_part_i_transactions(self, rows: List[List[str]], canonical: Canonical26AS):
        """Parse Part I transaction detail rows."""
        last_deductor = None
        last_tan = None
        
        if canonical.part_i:
            last_entry = canonical.part_i[-1]
            last_deductor = last_entry.deductor_name
            last_tan = last_entry.deductor_tan
        
        for row in rows:
            if len(row) < 6:
                continue
            
            row = [self._clean(cell) for cell in row]
            
            if not row[0] or row[0].lower() in ["sr", "sr.", "no", "total", "section"]:
                continue
            
            try:
                section = row[1] if len(row) > 1 else ""
                if not section or not section[0].isdigit():
                    continue
                
                canonical.part_i.append(Canonical26ASPartI(
                    deductor_name=last_deductor or "",
                    deductor_tan=last_tan or "",
                    section_code=section,
                    transaction_date=row[2] if len(row) > 2 else "",
                    amount_paid=self._to_decimal(row[6]) if len(row) > 6 else Decimal("0"),
                    tax_deducted=self._to_decimal(row[7]) if len(row) > 7 else Decimal("0"),
                    tds_deposited=self._to_decimal(row[8]) if len(row) > 8 else Decimal("0"),
                    status_of_booking=row[3] if len(row) > 3 else None,
                    date_of_booking=row[4] if len(row) > 4 else None,
                    remarks=row[5] if len(row) > 5 else None,
                ))
            except (ValueError, IndexError):
                continue
    
    def _parse_part_ii(self, rows: List[List[str]], canonical: Canonical26AS):
        """Parse Part II 15G/15H rows."""
        for row in rows:
            if len(row) < 5:
                continue
            
            row = [self._clean(cell) for cell in row]
            
            if not row[0] or row[0].lower() in ["sr", "sr.", "no", "total"]:
                continue
            
            try:
                canonical.part_ii.append(Canonical26ASPartII(
                    deductor_name=row[1] if len(row) > 1 else "",
                    deductor_tan=row[2] if len(row) > 2 else "",
                    amount_paid=self._to_decimal(row[3]) if len(row) > 3 else Decimal("0"),
                    tax_deducted=self._to_decimal(row[4]) if len(row) > 4 else Decimal("0"),
                    tds_deposited=self._to_decimal(row[5]) if len(row) > 5 else Decimal("0"),
                ))
            except (ValueError, IndexError):
                continue
    
    def _parse_part_vi(self, rows: List[List[str]], canonical: Canonical26AS):
        """Parse Part VI TCS rows."""
        for row in rows:
            if len(row) < 5:
                continue
            
            row = [self._clean(cell) for cell in row]
            
            if not row[0] or row[0].lower() in ["sr", "sr.", "no", "total"]:
                continue
            
            try:
                canonical.part_vi.append(Canonical26ASPartVI(
                    collector_name=row[1] if len(row) > 1 else "",
                    collector_tan=row[2] if len(row) > 2 else "",
                    amount_paid=self._to_decimal(row[3]) if len(row) > 3 else Decimal("0"),
                    tax_collected=self._to_decimal(row[4]) if len(row) > 4 else Decimal("0"),
                    tcs_deposited=self._to_decimal(row[5]) if len(row) > 5 else Decimal("0"),
                ))
            except (ValueError, IndexError):
                continue
    
    def _parse_part_vii(self, rows: List[List[str]], canonical: Canonical26AS):
        """Parse Part VII refund rows."""
        for row in rows:
            if len(row) < 7:
                continue
            
            row = [self._clean(cell) for cell in row]
            
            if not row[0] or row[0].lower() in ["sr", "sr.", "no", "total"]:
                continue
            
            try:
                canonical.part_vii.append(Canonical26ASPartVII(
                    assessment_year=row[1] if len(row) > 1 else "",
                    mode=row[2] if len(row) > 2 else "",
                    refund_issued=row[3] if len(row) > 3 else "",
                    nature_of_refund=row[4] if len(row) > 4 else "",
                    amount_of_refund=self._to_decimal(row[5]) if len(row) > 5 else Decimal("0"),
                    interest=self._to_decimal(row[6]) if len(row) > 6 else Decimal("0"),
                    date_of_payment=row[7] if len(row) > 7 else "",
                    remarks=row[8] if len(row) > 8 else None,
                ))
            except (ValueError, IndexError):
                continue
    
    def _clean(self, text: Optional[str]) -> str:
        """Clean extracted text."""
        if not text:
            return ""
        return re.sub(r"\s+", " ", text.replace("\n", " ")).strip()
    
    def _to_decimal(self, value: str) -> Decimal:
        """Convert string to Decimal, handling commas."""
        if not value or value.strip() == "":
            return Decimal("0")
        
        cleaned = value.replace(",", "").strip()
        try:
            return Decimal(cleaned)
        except:
            return Decimal("0")
