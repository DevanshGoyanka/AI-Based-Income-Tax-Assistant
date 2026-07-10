"""Prefill JSON importer - plain JSON from ITD portal."""
import json
from typing import BinaryIO
from uuid import UUID
from sqlalchemy.ext.asyncio import AsyncSession
from app.infra.db.models.client import Client
from app.services.importers.crypto import decode_aadhaar


class PrefillImporter:
    """Import ITD prefill JSON (plain JSON with base64 Aadhaar field)."""
    
    async def import_file(
        self,
        db: AsyncSession,
        client_id: UUID,
        file: BinaryIO,
        filename: str
    ) -> dict:
        """Import prefill JSON and store raw data.
        
        Returns: {"imported": int, "skipped": int, "errors": list[str], "extracted": dict}
        """
        content = file.read()
        try:
            data = json.loads(content.decode('utf-8'))
        except (json.JSONDecodeError, UnicodeDecodeError) as e:
            return {"imported": 0, "skipped": 0, "errors": [f"Invalid JSON: {e}"], "extracted": {}}
        
        # Auto-decode Aadhaar if present
        if "personalInfo" in data and "aadhaarCardNo" in data["personalInfo"]:
            aadhaar_b64 = data["personalInfo"]["aadhaarCardNo"]
            data["personalInfo"]["aadhaarCardNo"] = decode_aadhaar(aadhaar_b64)
        
        # Extract AY from data or filename
        ay = self._extract_ay(data, filename)
        
        # Store in prefill_data table
        from app.infra.db.models.prefill_data import PrefillData
        
        # Check if already imported for this client+AY
        from sqlalchemy import select, delete
        existing = await db.execute(
            select(PrefillData).where(
                PrefillData.client_id == client_id,
                PrefillData.ay == ay
            )
        )
        if existing.scalar_one_or_none():
            await db.execute(
                delete(PrefillData).where(
                    PrefillData.client_id == client_id,
                    PrefillData.ay == ay
                )
            )
        
        prefill_record = PrefillData(
            client_id=client_id,
            ay=ay,
            raw_json=data
        )
        db.add(prefill_record)

        # Sync clients.name, dob, mobile, email from personalInfo
        if "personalInfo" in data:
            pi = data["personalInfo"]
            client_row = await db.get(Client, client_id)
            if client_row:
                name_parts = pi.get("assesseeName", {})
                last_name = name_parts.get("surName") or name_parts.get("surNameOrOrgName") or ""
                full_name = " ".join(filter(None, [
                    name_parts.get("firstName", ""),
                    name_parts.get("middleName", ""),
                    last_name
                ])).strip()
                if full_name:
                    client_row.name = full_name
                dob_str = pi.get("dob", "")
                if dob_str:
                    from datetime import date as _date
                    try:
                        client_row.dob = _date.fromisoformat(dob_str)
                    except (ValueError, TypeError):
                        pass
                addr = pi.get("address", {}) or {}
                mob = addr.get("mobileNo") or pi.get("mobileNo")
                if mob:
                    client_row.mobile = str(mob)
                email = addr.get("emailAddress") or pi.get("emailAddress")
                if email:
                    client_row.email = email

        await db.commit()
        
        # Extract summary for response
        extracted = self._extract_summary(data, ay)
        
        return {"imported": 1, "skipped": 0, "errors": [], "extracted": extracted}
    
    def _extract_ay(self, data: dict, filename: str) -> str:
        """Extract AY from data or filename. Default to 2026-27."""
        if "filingStatus" in data and "AssessmentYear" in data["filingStatus"]:
            return data["filingStatus"]["AssessmentYear"]
        
        # Parse from filename like ACUPG3482G-Prefill-2025-...
        import re
        match = re.search(r'-(\d{4})-', filename)
        if match:
            fy_start = int(match.group(1))
            return f"{fy_start}-{str(fy_start + 1)[-2:]}"
        
        return "2026-27"
    
    def _extract_summary(self, data: dict, ay: str) -> dict:
        """Extract key fields from prefill JSON for response."""
        summary = {"assessment_year": ay}
        
        # Personal info
        if "personalInfo" in data:
            pi = data["personalInfo"]
            addr = pi.get("address", {}) or {}
            name_parts = pi.get("assesseeName", {})
            # Handle both 'surName' and 'surNameOrOrgName' key formats
            last_name = name_parts.get("surName") or name_parts.get("surNameOrOrgName") or ""
            full_name = " ".join(filter(None, [
                name_parts.get("firstName", ""),
                name_parts.get("middleName", ""),
                last_name
            ])).strip()
            summary["personal_info"] = {
                "pan": pi.get("pan", ""),
                "name": full_name,
                "dob": pi.get("dob", ""),
                "aadhaar": pi.get("aadhaarCardNo", ""),
                "email": addr.get("emailAddress", "") or pi.get("emailAddress", ""),
                "mobile": addr.get("mobileNo", "") or pi.get("mobileNo", ""),
            }
        
        # Form 26AS data
        if "form26as" in data:
            f26 = data["form26as"]
            summary["form26as"] = {
                "tds_salary_count": len(f26.get("tdsOnSalaries", {}).get("tdsOnSalary", [])),
                "tds_other_count": len(f26.get("tdsOnOthThanSals", {}).get("tdSonOthThanSal", [])),
                "tax_payments_count": len(f26.get("taxPayments", {}).get("taxPayment", [])),
            }
        
        # Form 24Q (salary details)
        if "form24q" in data:
            f24 = data["form24q"]
            salaries = f24.get("salaries", {}).get("salary", [])
            summary["form24q"] = {
                "employers_count": len(salaries),
                "total_salary": f24.get("incomeDeductions", {}).get("salary", 0),
                "standard_deduction": f24.get("incomeDeductions", {}).get("deductionUs16Ia", 0),
            }
        
        # Insights (derived data)
        if "insights" in data:
            ins = data["insights"]
            summary["insights"] = {
                "savings_interest": ins.get("intrstFrmSavingBank", 0),
                "term_deposit_interest": ins.get("intrstFrmTermDeposit", 0),
                "dividend": ins.get("scheduleOS", {}).get("incOthThanOwnRaceHorse", {}).get("dividendGross", 0),
            }
        
        # Bank accounts
        if "bankAccountDtls" in data:
            banks = []
            for bd in data["bankAccountDtls"]:
                for ab in bd.get("addtnlBankDetails", []):
                    banks.append({
                        "ifsc": ab.get("ifsccode", ""),
                        "account_no": ab.get("bankAccountNo", ""),
                        "bank_name": ab.get("bankName", ""),
                    })
            summary["bank_accounts"] = banks
        
        return summary
    
    def _get_full_name(self, name_obj: dict) -> str:
        """Construct full name from name object."""
        parts = [
            name_obj.get("firstName", ""),
            name_obj.get("middleName", ""),
            name_obj.get("surNameOrOrgName", ""),
        ]
        return " ".join(p for p in parts if p).strip()
