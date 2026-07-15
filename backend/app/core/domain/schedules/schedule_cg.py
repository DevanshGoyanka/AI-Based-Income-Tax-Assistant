"""Schedule CG - Capital Gains.

Implements Schedule CG per Income Tax Act:
- Section 111A: STCG on listed equity/MF @ 15%/20% (STT paid)
- Section 112A: LTCG on listed equity/MF @ 10%/12.5% (>₹1.25L exempt)
- Section 112: LTCG @ 20% with/without indexation
- Section 115BB: Lottery/winnings @ 30%
- Section 115BBH: VDA/crypto @ 30%
- Section 115BBE: Unexplained income @ 60%

CG is taxed at SPECIAL RATES, separate from slab income.
"""
from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal
from typing import List, Optional
from uuid import UUID

from ..value_objects import Money
from .base import Schedule, ValidationError


# ─── Asset Types ───────────────────────────────────────────────────────────────

CGAssetType = str  # Literal["listed_equity", "unlisted_equity", "property", "bonds", "gold", "crypto", "other"]
CGSection = str    # Literal["111A", "112A", "112", "115BB", "115BBH", "115BBE", "normal"]


@dataclass
class CGTransaction:
    """Capital gains transaction with full details per ITR form."""
    
    # ── Required fields (no defaults) ─────────────────────────────────────────
    asset_type: str  # "listed_equity", "unlisted_equity", "property", "bonds", "gold", "crypto", "lottery", "other"
    purchase_date: date
    sale_date: date
    sale_price: Money
    purchase_price: Money
    
    # ── Optional fields (have defaults) ───────────────────────────────────────
    section: str = ""  # 111A, 112A, 112, 115BB, 115BBH, 115BBE
    transfer_expenses: Money = field(default_factory=lambda: Money.from_rupees(0))
    indexed_cost: Optional[Money] = None  # For indexation benefit (property)
    stt_paid: bool = False  # Required for 111A/112A
    fair_market_value: Optional[Money] = None  # For RSUs, inherited assets
    broker_name: str = ""
    description: str = ""
    
    def holding_period_days(self) -> int:
        """Calculate holding period in days."""
        return (self.sale_date - self.purchase_date).days
    
    def is_long_term(self) -> bool:
        """Check if long-term based on asset type and holding period per IT Act."""
        days = self.holding_period_days()
        if self.asset_type in ["listed_equity", "equity_mf"]:
            return days > 365
        elif self.asset_type in ["property", "land", "building"]:
            return days > 730  # 24 months
        else:
            return days > 365  # Most other assets
    
    def determine_section(self) -> str:
        """Determine the applicable section based on asset type and STT."""
        if self.asset_type == "lottery":
            return "115BB"
        elif self.asset_type == "crypto":
            return "115BBH"
        elif self.asset_type == "unexplained":
            return "115BBE"
        
        is_long = self.is_long_term()
        
        if self.asset_type == "listed_equity" and self.stt_paid:
            return "112A" if is_long else "111A"
        elif self.asset_type == "listed_equity" and not self.stt_paid:
            return "112" if is_long else "normal"
        elif is_long:
            return "112"
        else:
            return "normal"
    
    def compute_gain(self) -> Decimal:
        """Compute capital gain/loss.
        
        For inherited assets, FMV on date of acquisition is used.
        For RSUs, FMV is considered as cost.
        """
        # Determine cost basis
        if self.fair_market_value and self.asset_type in ["rsu", "esop", "inherited"]:
            cost_basis = self.fair_market_value.to_rupees()
        else:
            cost_basis = self.indexed_cost.to_rupees() if self.indexed_cost else self.purchase_price.to_rupees()
        
        # Capital gain = Sale price - Expenses - Cost basis
        gain = (
            self.sale_price.to_rupees()
            - self.transfer_expenses.to_rupees()
            - Decimal(str(cost_basis))
        )
        return gain
    
    def get_rate(self) -> Decimal:
        """Get the applicable tax rate for this transaction."""
        section = self.section or self.determine_section()
        
        rates = {
            "111A": Decimal("0.15"),    # 15% for STCG on listed equity (STT paid)
            "112A": Decimal("0.125"),    # 12.5% for LTCG on listed equity (AY 2026-27)
            "112": Decimal("0.20"),      # 20% with/without indexation
            "115BB": Decimal("0.30"),    # 30% lottery/winnings
            "115BBH": Decimal("0.30"),   # 30% VDA/crypto
            "115BBE": Decimal("0.60"),   # 60% unexplained income
            "normal": Decimal("0"),      # At slab rates (not computed here)
        }
        return rates.get(section, Decimal("0"))


@dataclass 
class CGRateBucket:
    """A single rate bucket for CG computation."""
    section: str
    description: str
    rate: Decimal
    income: int  # Exempt threshold already deducted where applicable
    taxable_income: int
    tax: int
    
    def to_dict(self) -> dict:
        return {
            "section": self.section,
            "description": self.description,
            "rate": float(self.rate),
            "income": self.income,
            "taxable_income": self.taxable_income,
            "tax": self.tax,
        }


@dataclass
class CGExemptionThreshold:
    """Exemption thresholds for CG sections."""
    SECTION_112A_EXEMPT: int = 1_25_000  # ₹1.25L exempt for 112A LTCG


@dataclass
class ScheduleCG(Schedule[List[CGTransaction]]):
    """Schedule CG - Capital Gains (ITR-2+).
    
    CG income is taxed at special rates, separate from slab income.
    Intra-CG loss setoff is allowed (STCG loss setoff against STCG only,
    LTCG loss setoff against LTCG only, with exceptions).
    """
    
    id: UUID  # from Schedule base
    filing_id: UUID  # from Schedule base
    ay: str  # from Schedule base
    transactions: List[CGTransaction] = field(default_factory=list)
    
    def compute_income(self) -> Decimal:
        """Total CG (all rate buckets combined)."""
        return sum(txn.compute_gain() for txn in self.transactions if txn.compute_gain() > 0)
    
    def compute_loss(self) -> Decimal:
        """Total CG losses."""
        loss = Decimal("0")
        for txn in self.transactions:
            g = txn.compute_gain()
            if g < 0:
                loss += abs(g)
        return loss
    
    def categorize_by_rate_bucket(self) -> dict[str, int]:
        """Categorize gains by rate bucket (STCG/LTCG) and section.
        
        Returns dict like:
        {
            "stcg_111a_15_pct": 50000,
            "stcg_20_pct": 30000,
            "stcg_slab": 20000,  # At slab rates
            "ltcg_112a_12_5_pct": 200000,
            "ltcg_112_20_pct": 150000,
            "lottery_30_pct": 10000,
            "vda_30_pct": 5000,
        }
        """
        buckets = {
            "stcg_111a_15_pct": Decimal("0"),
            "stcg_20_pct": Decimal("0"),
            "stcg_applicable_rate": Decimal("0"),  # STCG at slab rates
            "ltcg_112a_12_5_pct": Decimal("0"),
            "ltcg_10_pct": Decimal("0"),
            "ltcg_12_5_pct_other": Decimal("0"),
            "ltcg_20_pct": Decimal("0"),
            "lottery_30_pct": Decimal("0"),
            "vda_30_pct": Decimal("0"),
            "online_gaming_30_pct": Decimal("0"),
            "unexplained_60_pct": Decimal("0"),
        }
        
        for txn in self.transactions:
            gain = txn.compute_gain()
            if gain <= 0:
                continue
                
            section = txn.section or txn.determine_section()
            is_long = txn.is_long_term()
            
            if section == "111A":
                buckets["stcg_111a_15_pct"] += gain
            elif section == "112A":
                # AY 2026-27: 12.5% (was 10% before FY 2024-25)
                buckets["ltcg_112a_12_5_pct"] += gain
            elif section == "112":
                if is_long:
                    buckets["ltcg_20_pct"] += gain
                else:
                    buckets["stcg_20_pct"] += gain
            elif section == "115BB":
                buckets["lottery_30_pct"] += gain
            elif section == "115BBH":
                buckets["vda_30_pct"] += gain
            elif section == "115BBE":
                buckets["unexplained_60_pct"] += gain
            else:
                # "normal" - taxed at slab rates
                if is_long:
                    buckets["ltcg_12_5_pct_other"] += gain  # Treat as 12.5% for simplicity
                else:
                    buckets["stcg_applicable_rate"] += gain
        
        # Convert to int
        return {k: int(v) for k, v in buckets.items()}
    
    def compute_rate_buckets(self, exempt_threshold: int = 1_25_000) -> List[CGRateBucket]:
        """Compute tax for each rate bucket.
        
        Args:
            exempt_threshold: Exemption limit for 112A (₹1.25L)
        """
        buckets = self.categorize_by_rate_bucket()
        results = []
        
        # STCG 111A @ 15%
        income = buckets["stcg_111a_15_pct"]
        if income > 0:
            results.append(CGRateBucket(
                section="111A",
                description="STCG on listed equity/MF @ 15% (STT paid)",
                rate=Decimal("0.15"),
                income=income,
                taxable_income=income,
                tax=int(Decimal(income) * Decimal("0.15")),
            ))
        
        # STCG 20%
        income = buckets["stcg_20_pct"]
        if income > 0:
            results.append(CGRateBucket(
                section="111A_20",
                description="STCG on securities @ 20%",
                rate=Decimal("0.20"),
                income=income,
                taxable_income=income,
                tax=int(Decimal(income) * Decimal("0.20")),
            ))
        
        # STCG at slab rates
        income = buckets["stcg_applicable_rate"]
        if income > 0:
            results.append(CGRateBucket(
                section="STCG_SLAB",
                description="STCG taxed at slab rates",
                rate=Decimal("0"),  # Rate depends on total income
                income=income,
                taxable_income=income,
                tax=0,  # Computed separately in slab calculation
            ))
        
        # LTCG 112A @ 12.5% (with exempt threshold)
        income = buckets["ltcg_112a_12_5_pct"]
        if income > 0:
            taxable = max(0, income - exempt_threshold)
            results.append(CGRateBucket(
                section="112A",
                description=f"LTCG on listed equity/MF @ 12.5% (₹{exempt_threshold:,} exempt)",
                rate=Decimal("0.125"),
                income=income,
                taxable_income=taxable,
                tax=int(Decimal(taxable) * Decimal("0.125")),
            ))
        
        # LTCG 10% (pre-FY 2024-25)
        income = buckets["ltcg_10_pct"]
        if income > 0:
            taxable = max(0, income - exempt_threshold)
            results.append(CGRateBucket(
                section="112A_10",
                description=f"LTCG @ 10% (pre-FY 2024-25, ₹{exempt_threshold:,} exempt)",
                rate=Decimal("0.10"),
                income=income,
                taxable_income=taxable,
                tax=int(Decimal(taxable) * Decimal("0.10")),
            ))
        
        # LTCG 12.5% other
        income = buckets["ltcg_12_5_pct_other"]
        if income > 0:
            results.append(CGRateBucket(
                section="112_12.5",
                description="LTCG @ 12.5% other capital assets",
                rate=Decimal("0.125"),
                income=income,
                taxable_income=income,
                tax=int(Decimal(income) * Decimal("0.125")),
            ))
        
        # LTCG 112 @ 20%
        income = buckets["ltcg_20_pct"]
        if income > 0:
            results.append(CGRateBucket(
                section="112",
                description="LTCG @ 20% (with/without indexation)",
                rate=Decimal("0.20"),
                income=income,
                taxable_income=income,
                tax=int(Decimal(income) * Decimal("0.20")),
            ))
        
        # Lottery 115BB @ 30%
        income = buckets["lottery_30_pct"]
        if income > 0:
            results.append(CGRateBucket(
                section="115BB",
                description="Winnings/lottery @ 30%",
                rate=Decimal("0.30"),
                income=income,
                taxable_income=income,
                tax=int(Decimal(income) * Decimal("0.30")),
            ))
        
        # VDA 115BBH @ 30%
        income = buckets["vda_30_pct"]
        if income > 0:
            results.append(CGRateBucket(
                section="115BBH",
                description="VDA/Crypto @ 30%",
                rate=Decimal("0.30"),
                income=income,
                taxable_income=income,
                tax=int(Decimal(income) * Decimal("0.30")),
            ))
        
        # Online Gaming 115BBJ @ 30%
        income = buckets["online_gaming_30_pct"]
        if income > 0:
            results.append(CGRateBucket(
                section="115BBJ",
                description="Online Gaming @ 30%",
                rate=Decimal("0.30"),
                income=income,
                taxable_income=income,
                tax=int(Decimal(income) * Decimal("0.30")),
            ))
        
        # Unexplained 115BBE @ 60%
        income = buckets["unexplained_60_pct"]
        if income > 0:
            results.append(CGRateBucket(
                section="115BBE",
                description="Unexplained income @ 60%",
                rate=Decimal("0.60"),
                income=income,
                taxable_income=income,
                tax=int(Decimal(income) * Decimal("0.60")),
            ))
        
        return results
    
    def compute_special_rate_tax(self, exempt_threshold: int = 1_25_000) -> tuple[int, List[CGRateBucket]]:
        """Compute total CG tax at special rates.
        
        Returns:
            Tuple of (total_tax, list of rate buckets)
        """
        buckets = self.compute_rate_buckets(exempt_threshold)
        total_tax = sum(b.tax for b in buckets)
        return total_tax, buckets
    
    def get_totals(self) -> dict:
        """Get aggregate totals for CG."""
        buckets = self.categorize_by_rate_bucket()
        
        stcg = (
            buckets["stcg_111a_15_pct"] +
            buckets["stcg_20_pct"] +
            buckets["stcg_applicable_rate"]
        )
        ltcg = (
            buckets["ltcg_112a_12_5_pct"] +
            buckets["ltcg_10_pct"] +
            buckets["ltcg_12_5_pct_other"] +
            buckets["ltcg_20_pct"]
        )
        special_rate = (
            buckets["lottery_30_pct"] +
            buckets["vda_30_pct"] +
            buckets["online_gaming_30_pct"] +
            buckets["unexplained_60_pct"]
        )
        
        return {
            "short_term": stcg,
            "long_term": ltcg,
            "special_rate": special_rate,
            "total": stcg + ltcg + special_rate,
        }
    
    def to_itr_json(self) -> dict:
        """Export to ITR JSON format per ITR-2 schema."""
        return {
            "ScheduleCG": {
                "ShortTermCapitalGains": [
                    {
                        "AssetType": txn.asset_type,
                        "PurchaseDate": txn.purchase_date.isoformat(),
                        "SaleDate": txn.sale_date.isoformat(),
                        "SalePrice": int(txn.sale_price.to_rupees()),
                        "PurchasePrice": int(txn.purchase_price.to_rupees()),
                        "TransferExpenses": int(txn.transfer_expenses.to_rupees()),
                        "STTPaid": txn.stt_paid,
                        "FairMarketValue": int(txn.fair_market_value.to_rupees()) if txn.fair_market_value else None,
                        "Section": txn.section or txn.determine_section(),
                        "Gain": int(txn.compute_gain()),
                    }
                    for txn in self.transactions if not txn.is_long_term()
                ],
                "LongTermCapitalGains": [
                    {
                        "AssetType": txn.asset_type,
                        "PurchaseDate": txn.purchase_date.isoformat(),
                        "SaleDate": txn.sale_date.isoformat(),
                        "SalePrice": int(txn.sale_price.to_rupees()),
                        "PurchasePrice": int(txn.purchase_price.to_rupees()),
                        "IndexedCost": int(txn.indexed_cost.to_rupees()) if txn.indexed_cost else None,
                        "TransferExpenses": int(txn.transfer_expenses.to_rupees()),
                        "STTPaid": txn.stt_paid,
                        "FairMarketValue": int(txn.fair_market_value.to_rupees()) if txn.fair_market_value else None,
                        "Section": txn.section or txn.determine_section(),
                        "Gain": int(txn.compute_gain()),
                    }
                    for txn in self.transactions if txn.is_long_term()
                ],
                "TotalShortTermCapitalGains": self.get_totals()["short_term"],
                "TotalLongTermCapitalGains": self.get_totals()["long_term"],
            }
        }
    
    def validate(self) -> List[ValidationError]:
        """Validate capital gains transactions per IT rules."""
        errors = []
        
        for i, txn in enumerate(self.transactions):
            # Sale date must be after purchase
            if txn.sale_date <= txn.purchase_date:
                errors.append(ValidationError(
                    field=f"ScheduleCG.Transactions[{i}]",
                    message="Sale date must be after purchase date",
                    severity="BLOCKING"
                ))
            
            # Sale price must be positive
            if txn.sale_price.to_rupees() <= 0:
                errors.append(ValidationError(
                    field=f"ScheduleCG.Transactions[{i}].SalePrice",
                    message="Sale price must be positive",
                    severity="BLOCKING"
                ))
            
            # 112A requires STT paid
            if txn.asset_type == "listed_equity":
                section = txn.section or txn.determine_section()
                if section == "112A" and not txn.stt_paid:
                    errors.append(ValidationError(
                        field=f"ScheduleCG.Transactions[{i}].STTPaid",
                        message="Section 112A requires STT to have been paid",
                        severity="WARNING"
                    ))
            
            # Crypto must use 115BBH
            if txn.asset_type == "crypto" and txn.section not in ["", "115BBH"]:
                errors.append(ValidationError(
                    field=f"ScheduleCG.Transactions[{i}].Section",
                    message="Crypto gains must be under section 115BBH",
                    severity="WARNING"
                ))
        
        return errors
