"""AIS PDF importer - encrypted PDF with PAN+DOB password."""
import io
import re
from typing import BinaryIO
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, delete
from app.infra.db.models.ais_data import AISData
from app.services.ais_pdf_parser import parse_ais_pdf_tables


class AISPDFImporter:
    """Import encrypted AIS PDF files."""

    def _extract_ay_from_filename(self, filename: str) -> str | None:
        """Extract assessment year from filename."""
        match = re.search(r'(\d{4})-(\d{2,4})', filename)
        if match:
            year1 = match.group(1)
            year2 = match.group(2)
            if len(year2) == 2:
                year2 = year1[:2] + year2
            return f"{year1}-{year2}"
        return None

    async def import_file(
        self,
        db: AsyncSession,
        client_id: UUID,
        file: BinaryIO,
        filename: str,
        pan: str,
        dob: str
    ) -> dict:
        """Import AIS PDF file.
        
        Args:
            db: Database session
            client_id: Client UUID
            file: Binary file stream
            filename: Original filename
            pan: PAN number (for password)
            dob: Date of birth in DDMMYYYY format (for password)
        
        Returns:
            Dict with success status, counts, and errors
        """
        errors = []
        warnings = []

        try:
            # Read PDF content
            content = file.read()
            
            # Parse AIS PDF
            result = parse_ais_pdf_tables(content, pan, dob)
            
            if result.get("error"):
                errors.append(result["error"])
                return {"success": False, "errors": errors, "warnings": warnings}
            
            # Collect warnings
            if result.get("warnings"):
                warnings.extend(result["warnings"])
            
            # Delete existing AIS data for this client
            await db.execute(delete(AISData).where(AISData.client_id == client_id))
            
            # Determine assessment year from filename or current date
            ay = self._extract_ay_from_filename(filename) or "2025-26"
            
            # Store parsed data
            ais_record = AISData(
                client_id=client_id,
                ay=ay,
                pan=result.get("personal_info", {}).get("pan", pan),
                source="AIS-PDF",
                raw_json=result,
                parsed_summary={
                    "tds_salary_count": len(result.get("tds_salary", [])),
                    "tds_others_count": len(result.get("tds_others", [])),
                    "sft_dividend_count": len(result.get("sft_dividend", [])),
                    "sft_interest_count": len(result.get("sft_interest", [])),
                    "sft_mutual_fund_count": len(result.get("sft_mutual_fund", [])),
                    "sft_other_count": len(result.get("sft_other", [])),
                    "tax_payments_count": len(result.get("tax_payments", [])),
                    "refunds_count": len(result.get("refunds", []))
                }
            )
            
            db.add(ais_record)
            await db.commit()
            
            # Count records
            counts = {
                "tds_salary": len(result.get("tds_salary", [])),
                "tds_others": len(result.get("tds_others", [])),
                "sft_dividend": len(result.get("sft_dividend", [])),
                "sft_interest": len(result.get("sft_interest", [])),
                "sft_mutual_fund": len(result.get("sft_mutual_fund", [])),
                "sft_other": len(result.get("sft_other", [])),
                "tax_payments": len(result.get("tax_payments", [])),
                "refunds": len(result.get("refunds", []))
            }
            
            return {
                "success": True,
                "message": f"AIS PDF imported successfully",
                "counts": counts,
                "warnings": warnings,
                "errors": []
            }
            
        except Exception as e:
            errors.append(f"Failed to import AIS PDF: {str(e)}")
            return {"success": False, "errors": errors, "warnings": warnings}
