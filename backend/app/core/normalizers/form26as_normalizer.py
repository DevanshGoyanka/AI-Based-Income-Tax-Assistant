"""Form 26AS to canonical income normalizer."""
from decimal import Decimal
from typing import List

from app.core.domain.canonical_models import (
    CanonicalTDS,
    CanonicalAdvanceTax,
)
from app.core.domain.canonical_26as import (
    Canonical26AS,
    Canonical26ASPartI,
    Canonical26ASPartVI,
)


class Form26ASNormalizer:
    """Normalize Form 26AS to canonical income models."""
    
    def normalize_to_tds(self, form26as: Canonical26AS) -> List[CanonicalTDS]:
        """Extract TDS entries from Part I and Part II."""
        tds_list = []
        
        for entry in form26as.part_i:
            tds_list.append(CanonicalTDS(
                deductor_name=entry.deductor_name,
                deductor_tan=entry.deductor_tan,
                section_code=entry.section_code or "192",
                amount_paid=entry.amount_paid,
                tax_deducted=entry.tax_deducted,
                tax_deposited=entry.tds_deposited,
                quarter="",
            ))
        
        for entry in form26as.part_ii:
            tds_list.append(CanonicalTDS(
                deductor_name=entry.deductor_name,
                deductor_tan=entry.deductor_tan,
                section_code="15G/15H",
                amount_paid=entry.amount_paid,
                tax_deducted=entry.tax_deducted,
                tax_deposited=entry.tds_deposited,
                quarter="",
            ))
        
        return tds_list
    
    def normalize_to_tcs(self, form26as: Canonical26AS) -> List[dict]:
        """Extract TCS entries from Part VI."""
        tcs_list = []
        
        for entry in form26as.part_vi:
            tcs_list.append({
                "collector_name": entry.collector_name,
                "collector_tan": entry.collector_tan,
                "section_code": entry.section_code or "206C",
                "amount_paid": entry.amount_paid,
                "tax_collected": entry.tax_collected,
                "tcs_deposited": entry.tcs_deposited,
            })
        
        return tcs_list
    
    def normalize_to_refunds(self, form26as: Canonical26AS) -> List[dict]:
        """Extract refund information from Part VII."""
        refunds = []
        
        for entry in form26as.part_vii:
            refunds.append({
                "assessment_year": entry.assessment_year,
                "mode": entry.mode,
                "refund_issued": entry.refund_issued,
                "nature": entry.nature_of_refund,
                "amount": entry.amount_of_refund,
                "interest": entry.interest,
                "date_of_payment": entry.date_of_payment,
                "remarks": entry.remarks,
            })
        
        return refunds
