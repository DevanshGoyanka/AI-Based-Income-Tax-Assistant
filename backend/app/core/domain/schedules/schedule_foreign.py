"""Foreign asset schedules — ScheduleFA, ScheduleAL, ScheduleFSI, ScheduleTR1, Schedule5A.
Used in: ITR-2, ITR-3 only.
"""
from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal
from typing import List, Optional
from uuid import UUID, uuid4

from ..value_objects import Money
from .base import Schedule, ValidationError


# ─── ScheduleFA sub-schedules ─────────────────────────────────────────────────

@dataclass
class FAImmovableProperty:
    """ScheduleFA B1 — Immovable property outside India."""
    id: UUID = field(default_factory=uuid4)
    country_code: str = ""
    country_name: str = ""
    address: str = ""
    pin_code: str = ""
    date_of_acquisition: Optional[date] = None
    cost_of_acquisition: int = 0
    fair_market_value: int = 0
    total_value: int = 0
    income_accrued: int = 0
    nature_of_income: str = ""


@dataclass
class FAFinancialInterest:
    """ScheduleFA B2 — Financial interest in foreign entity."""
    id: UUID = field(default_factory=uuid4)
    country_code: str = ""
    country_name: str = ""
    name_of_entity: str = ""
    entity_address: str = ""
    nature_of_interest: str = ""  # Beneficial owner, shareholder, etc.
    date_of_acquisition: Optional[date] = None
    total_value: int = 0
    income_accrued: int = 0


@dataclass
class FACustodialAccount:
    """ScheduleFA B3 — Foreign custodial account."""
    id: UUID = field(default_factory=uuid4)
    country_code: str = ""
    country_name: str = ""
    name_of_bank: str = ""
    account_number: str = ""
    account_type: str = ""
    date_of_opening: Optional[date] = None
    balance_in_foreign_currency: int = 0
    income_accrued: int = 0


@dataclass
class FABankAccount:
    """ScheduleFA B4 — Foreign bank account."""
    id: UUID = field(default_factory=uuid4)
    country_code: str = ""
    country_name: str = ""
    name_of_bank: str = ""
    account_number: str = ""
    account_type: str = ""  # SB, CA, etc.
    date_of_opening: Optional[date] = None
    date_of_closing: Optional[date] = None
    peak_balance: int = 0
    closing_balance: int = 0
    interest_income: int = 0


@dataclass
class FAOtherAsset:
    """ScheduleFA B5 — Other foreign assets."""
    id: UUID = field(default_factory=uuid4)
    country_code: str = ""
    country_name: str = ""
    description: str = ""
    asset_type: str = ""  # Jewellery, art, etc.
    date_of_acquisition: Optional[date] = None
    cost_of_acquisition: int = 0
    total_value: int = 0
    income_accrued: int = 0


@dataclass
class FASigningAuthority:
    """ScheduleFA B6 — Signing authority over foreign account/asset."""
    id: UUID = field(default_factory=uuid4)
    country_code: str = ""
    country_name: str = ""
    name_of_bank_or_institution: str = ""
    account_number_or_asset_id: str = ""
    nature_of_authority: str = ""
    date_from: Optional[date] = None
    date_to: Optional[date] = None
    income_accrued: int = 0


@dataclass
class FATrustDetails:
    """ScheduleFA B7 — Trust / entity outside India where taxpayer is trustee/beneficiary."""
    id: UUID = field(default_factory=uuid4)
    country_code: str = ""
    country_name: str = ""
    name_of_trust: str = ""
    trust_address: str = ""
    nature_of_benefit: str = ""  # Settlor, Trustee, Beneficiary
    date_of_creation: Optional[date] = None
    total_value_of_interest: int = 0
    income_accrued: int = 0


@dataclass
class FAOtherSourceIncome:
    """ScheduleFA B8 — Other source income from outside India."""
    id: UUID = field(default_factory=uuid4)
    country_code: str = ""
    country_name: str = ""
    nature_of_income: str = ""
    amount: int = 0


@dataclass
class ScheduleFA(Schedule[dict]):
    """Schedule FA — Foreign Assets.
    
    Mandatory disclosure of foreign assets under Black Money Act / Section 139(9).
    Used in: ITR-2, ITR-3.
    
    Sub-schedules:
    - B1: Immovable property
    - B2: Financial interest in foreign entity
    - B3: Custodial account
    - B4: Bank account
    - B5: Other assets
    - B6: Signing authority
    - B7: Trust details
    - B8: Other source income outside India
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    # Whether any foreign asset exists
    has_foreign_asset: bool = True

    # Sub-schedules
    immovable_properties: List[FAImmovableProperty] = field(default_factory=list)
    financial_interests: List[FAFinancialInterest] = field(default_factory=list)
    custodial_accounts: List[FACustodialAccount] = field(default_factory=list)
    bank_accounts: List[FABankAccount] = field(default_factory=list)
    other_assets: List[FAOtherAsset] = field(default_factory=list)
    signing_authorities: List[FASigningAuthority] = field(default_factory=list)
    trust_details: List[FATrustDetails] = field(default_factory=list)
    other_source_incomes: List[FAOtherSourceIncome] = field(default_factory=list)

    def total_foreign_income(self) -> int:
        return (
            sum(a.income_accrued for a in self.immovable_properties) +
            sum(a.income_accrued for a in self.financial_interests) +
            sum(a.income_accrued for a in self.custodial_accounts) +
            sum(a.income_accrued for a in self.bank_accounts) +
            sum(a.income_accrued for a in self.other_assets) +
            sum(a.income_accrued for a in self.signing_authorities) +
            sum(a.income_accrued for a in self.trust_details) +
            sum(a.amount for a in self.other_source_incomes)
        )

    def compute_income(self) -> Decimal:
        return Decimal(self.total_foreign_income())

    def to_itr_json(self) -> dict:
        return {
            "ScheduleFA": {
                "DetailsImmovableProperty": [
                    {
                        "Country": a.country_code,
                        "CountryName": a.country_name,
                        "Address": a.address,
                        "PinCode": a.pin_code,
                        "DateOfAcquisition": str(a.date_of_acquisition) if a.date_of_acquisition else "",
                        "CostOfAcquisition": a.cost_of_acquisition,
                        "FairMarketValue": a.fair_market_value,
                        "TotalValue": a.total_value,
                        "IncomeAccrued": a.income_accrued,
                        "NatureOfIncome": a.nature_of_income,
                    }
                    for a in self.immovable_properties
                ],
                "DetailsFinancialInterest": [
                    {
                        "Country": a.country_code,
                        "CountryName": a.country_name,
                        "NameOfEntity": a.name_of_entity,
                        "EntityAddress": a.entity_address,
                        "NatureOfInterest": a.nature_of_interest,
                        "DateOfAcquisition": str(a.date_of_acquisition) if a.date_of_acquisition else "",
                        "TotalValue": a.total_value,
                        "IncomeAccrued": a.income_accrued,
                    }
                    for a in self.financial_interests
                ],
                "DtlsForeignCustodialAcc": [
                    {
                        "Country": a.country_code,
                        "CountryName": a.country_name,
                        "NameOfBank": a.name_of_bank,
                        "AccountNumber": a.account_number,
                        "AccountType": a.account_type,
                        "DateOfOpening": str(a.date_of_opening) if a.date_of_opening else "",
                        "BalanceInFC": a.balance_in_foreign_currency,
                        "IncomeAccrued": a.income_accrued,
                    }
                    for a in self.custodial_accounts
                ],
                "DetailsForiegnBank": [
                    {
                        "Country": a.country_code,
                        "CountryName": a.country_name,
                        "NameOfBank": a.name_of_bank,
                        "AccountNumber": a.account_number,
                        "AccountType": a.account_type,
                        "DateOfOpening": str(a.date_of_opening) if a.date_of_opening else "",
                        "DateOfClosing": str(a.date_of_closing) if a.date_of_closing else "",
                        "PeakBalance": a.peak_balance,
                        "ClosingBalance": a.closing_balance,
                        "InterestIncome": a.interest_income,
                    }
                    for a in self.bank_accounts
                ],
                "DetailsOthAssets": [
                    {
                        "Country": a.country_code,
                        "CountryName": a.country_name,
                        "Description": a.description,
                        "AssetType": a.asset_type,
                        "DateOfAcquisition": str(a.date_of_acquisition) if a.date_of_acquisition else "",
                        "CostOfAcquisition": a.cost_of_acquisition,
                        "TotalValue": a.total_value,
                        "IncomeAccrued": a.income_accrued,
                    }
                    for a in self.other_assets
                ],
                "DetailsOfAccntsHvngSigningAuth": [
                    {
                        "Country": a.country_code,
                        "CountryName": a.country_name,
                        "NameOfBankOrInstitution": a.name_of_bank_or_institution,
                        "AccountNumberOrAssetId": a.account_number_or_asset_id,
                        "NatureOfAuthority": a.nature_of_authority,
                        "DateFrom": str(a.date_from) if a.date_from else "",
                        "DateTo": str(a.date_to) if a.date_to else "",
                        "IncomeAccrued": a.income_accrued,
                    }
                    for a in self.signing_authorities
                ],
                "DetailsOfTrustOutIndiaTrustee": [
                    {
                        "Country": a.country_code,
                        "CountryName": a.country_name,
                        "NameOfTrust": a.name_of_trust,
                        "TrustAddress": a.trust_address,
                        "NatureOfBenefit": a.nature_of_benefit,
                        "DateOfCreation": str(a.date_of_creation) if a.date_of_creation else "",
                        "TotalValueOfInterest": a.total_value_of_interest,
                        "IncomeAccrued": a.income_accrued,
                    }
                    for a in self.trust_details
                ],
                "DetailsOfOthSourcesIncOutsideIndia": [
                    {
                        "Country": a.country_code,
                        "CountryName": a.country_name,
                        "NatureOfIncome": a.nature_of_income,
                        "Amount": a.amount,
                    }
                    for a in self.other_source_incomes
                ],
                "TotalForeignAssetIncome": self.total_foreign_income(),
                "TotalForeignAssetValue": sum(a.total_value for a in self.immovable_properties) +
                                         sum(a.total_value for a in self.financial_interests) +
                                         sum(a.total_value for a in self.other_assets) +
                                         sum(a.total_value_of_interest for a in self.trust_details),
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        if not self.has_foreign_asset and (
            self.immovable_properties or self.financial_interests or self.bank_accounts
        ):
            errors.append(ValidationError(
                field="ScheduleFA.has_foreign_asset",
                message="has_foreign_asset=False but asset details are provided",
                severity="BLOCKING"
            ))
        for i, acc in enumerate(self.bank_accounts):
            if acc.date_of_closing and acc.date_of_opening and acc.date_of_closing < acc.date_of_opening:
                errors.append(ValidationError(
                    field=f"ScheduleFA.bank_accounts[{i}].date_of_closing",
                    message="Account closing date cannot be before opening date",
                    severity="BLOCKING"
                ))
        return errors


# ─── ScheduleAL ──────────────────────────────────────────────────────────────

@dataclass
class ImmovableAssetDetail:
    """Immovable property asset detail for ScheduleAL."""
    description: str = ""
    address: str = ""
    cost_of_acquisition: int = 0
    fair_market_value: int = 0
    immovable_asset_type: str = ""  # Land, Building, Both


@dataclass
class MovableAssetDetail:
    """Movable assets for ScheduleAL."""
    jewellery: int = 0
    cash_in_hand: int = 0
    bank_deposits: int = 0
    shares_and_securities: int = 0
    insurance_policies: int = 0
    loans_given: int = 0
    motor_vehicles: int = 0
    other_movable_assets: int = 0


@dataclass
class AssetLiabilityDetail:
    """Interest in asset for ScheduleAL."""
    description: str = ""
    nature_of_interest: str = ""  # Co-owner, Beneficiary, etc.
    share_percentage: float = 0.0


@dataclass
class ScheduleAL(Schedule[dict]):
    """Schedule AL — Assets and Liabilities.
    
    Required when total income > ₹50L.
    Used in: ITR-2, ITR-3.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    immovable_assets: List[ImmovableAssetDetail] = field(default_factory=list)
    movable_assets: MovableAssetDetail = field(default_factory=MovableAssetDetail)
    has_interest_in_aop: bool = False  # Interest in association of persons
    interests_in_assets: List[AssetLiabilityDetail] = field(default_factory=list)
    liability_in_related_assets: int = 0  # Liability on assets reported above

    def total_immovable_value(self) -> int:
        return sum(a.fair_market_value for a in self.immovable_assets)

    def total_movable_value(self) -> int:
        m = self.movable_assets
        return (m.jewellery + m.cash_in_hand + m.bank_deposits +
                m.shares_and_securities + m.insurance_policies +
                m.loans_given + m.motor_vehicles + m.other_movable_assets)

    def total_assets(self) -> int:
        return self.total_immovable_value() + self.total_movable_value()

    def compute_income(self) -> Decimal:
        return Decimal(0)  # AL is disclosure, not income computation

    def to_itr_json(self) -> dict:
        return {
            "ScheduleAL": {
                "ImmovableDetails": [
                    {
                        "Description": a.description,
                        "Address": a.address,
                        "CostOfAcquisition": a.cost_of_acquisition,
                        "FairMarketValue": a.fair_market_value,
                        "ImmovableAssetType": a.immovable_asset_type,
                    }
                    for a in self.immovable_assets
                ],
                "MovableAsset": {
                    "Jewellery": self.movable_assets.jewellery,
                    "CashInHand": self.movable_assets.cash_in_hand,
                    "BankDeposits": self.movable_assets.bank_deposits,
                    "SharesSecurities": self.movable_assets.shares_and_securities,
                    "InsurancePolicies": self.movable_assets.insurance_policies,
                    "LoansGiven": self.movable_assets.loans_given,
                    "MotorVehicles": self.movable_assets.motor_vehicles,
                    "OtherMovableAssets": self.movable_assets.other_movable_assets,
                    "TotalMovableAssets": self.total_movable_value(),
                },
                "InterstAOPFlag": "Y" if self.has_interest_in_aop else "N",
                "InterestHeldInaAsset": [
                    {"Description": a.description, "NatureOfInterest": a.nature_of_interest, "Share": int(a.share_percentage * 100)}
                    for a in self.interests_in_assets
                ],
                "LiabilityInRelatAssets": self.liability_in_related_assets,
                "TotalAssets": self.total_assets(),
                "NetAssets": self.total_assets() - self.liability_in_related_assets,
            }
        }

    def validate(self) -> List[ValidationError]:
        return []


# ─── ScheduleFSI ──────────────────────────────────────────────────────────────

@dataclass
class FSICountryIncome:
    """FSI entry per country — foreign source income."""
    id: UUID = field(default_factory=uuid4)
    country_code: str = ""
    country_name: str = ""
    income_head: str = ""  # Salary, HP, CG, OS, BP
    gross_foreign_income: int = 0
    dt_revenue_or_country_code: str = ""  # DTAA rate (%) or country for non-DTAA
    tax_paid_outside_india: int = 0
    tax_payable_in_india: int = 0  # Tax if same income in India
    tax_relief_available: int = 0
    dt_aa_relief: str = ""  # Section 90/90A/91

    def compute_relief(self) -> int:
        """DTAA relief = min(tax paid outside, tax payable in India)."""
        return min(self.tax_paid_outside_india, self.tax_payable_in_india)


@dataclass
class ScheduleFSI(Schedule[dict]):
    """Schedule FSI — Foreign Source Income.
    
    Disclosure of income earned outside India for DTAA relief claims.
    Used in: ITR-2, ITR-3.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    country_incomes: List[FSICountryIncome] = field(default_factory=list)

    def total_foreign_income(self) -> int:
        return sum(c.gross_foreign_income for c in self.country_incomes)

    def total_tax_paid(self) -> int:
        return sum(c.tax_paid_outside_india for c in self.country_incomes)

    def total_relief(self) -> int:
        return sum(c.compute_relief() for c in self.country_incomes)

    def compute_income(self) -> Decimal:
        return Decimal(self.total_foreign_income())

    def to_itr_json(self) -> dict:
        return {
            "ScheduleFSI": {
                "TotalForeignInc": self.total_foreign_income(),
                "TotalTaxPaid": self.total_tax_paid(),
                "TotalDTARelief": self.total_relief(),
                "FSIIncType": [
                    {
                        "CountryCode": c.country_code,
                        "CountryName": c.country_name,
                        "IncFrmOutsideInd": c.gross_foreign_income,
                        "TaxPaidOutsideInd": c.tax_paid_outside_india,
                        "TaxPayableinInd": c.tax_payable_in_india,
                        "TaxReliefinInd": c.tax_relief_available,
                        "DTAAReliefUs90or90A": c.dt_aa_relief,
                    }
                    for c in self.country_incomes
                ],
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        for i, c in enumerate(self.country_incomes):
            if c.tax_paid_outside_india > c.tax_payable_in_india * 2:
                errors.append(ValidationError(
                    field=f"ScheduleFSI[{i}].tax_paid_outside_india",
                    message="Tax paid outside India seems unusually high vs India tax liability",
                    severity="WARNING"
                ))
        return errors


# ─── ScheduleTR1 ─────────────────────────────────────────────────────────────

@dataclass
class TR1Entry:
    """Single TR1 entry — Tax Relief under DTAA."""
    id: UUID = field(default_factory=uuid4)
    country_code: str = ""
    country_name: str = ""
    agreement_type: str = ""  # DTAA, TRC
    relief_section: str = ""  # 90, 90A, 91
    income_type: str = ""     # Salary, HP, CG, OS, BP
    gross_income: int = 0
    foreign_tax_paid: int = 0
    tax_credit_claimed: int = 0


@dataclass
class ScheduleTR1(Schedule[dict]):
    """Schedule TR1 — Tax Relief under Section 90/90A/91.
    
    Used in: ITR-2, ITR-3.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    entries: List[TR1Entry] = field(default_factory=list)

    def total_tax_relief(self) -> int:
        return sum(e.tax_credit_claimed for e in self.entries)

    def compute_income(self) -> Decimal:
        return Decimal(sum(e.gross_income for e in self.entries))

    def to_itr_json(self) -> dict:
        return {
            "ScheduleTR1": {
                "TR1Detail": [
                    {
                        "CountryCode": e.country_code,
                        "AgreementCountry": e.country_name,
                        "AgreementType": e.agreement_type,
                        "ReliefSection": e.relief_section,
                        "IncomeType": e.income_type,
                        "GrossIncome": e.gross_income,
                        "ForeignTaxPaid": e.foreign_tax_paid,
                        "TaxCreditClaimed": e.tax_credit_claimed,
                    }
                    for e in self.entries
                ],
                "TotalTaxRelief": self.total_tax_relief(),
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        for i, e in enumerate(self.entries):
            if e.tax_credit_claimed > e.foreign_tax_paid:
                errors.append(ValidationError(
                    field=f"ScheduleTR1[{i}].tax_credit_claimed",
                    message="Tax credit claimed cannot exceed foreign tax paid",
                    severity="BLOCKING"
                ))
        return errors


# ─── Schedule5A2014 ──────────────────────────────────────────────────────────

@dataclass
class PreviousEmploymentDetail:
    """Previous employment details for Schedule 5A."""
    employer_name: str = ""
    employer_address: str = ""
    employer_country: str = ""
    employer_country_code: str = ""
    nature_of_employment: str = ""
    period_from: str = ""  # "MM/YYYY"
    period_to: str = ""    # "MM/YYYY"
    remuneration: int = 0


@dataclass
class Schedule5A2014(Schedule[dict]):
    """Schedule 5A — Information about previous employer (Sec 5A).
    
    For computation of salary income when taxpayer had foreign employment
    in previous years — used for grandfathering provisions.
    
    Used in: ITR-2, ITR-3.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    assessee_name: str = ""
    father_name: str = ""
    date_of_birth: Optional[date] = None
    aadhaar_or_uid: str = ""
    pan: str = ""
    residential_status: str = ""  # Resident, NOR, NRI

    previous_employments: List[PreviousEmploymentDetail] = field(default_factory=list)

    def compute_income(self) -> Decimal:
        return Decimal(sum(e.remuneration for e in self.previous_employments))

    def to_itr_json(self) -> dict:
        return {
            "Sch5AIncType": {
                "AssesseeName": self.assessee_name,
                "FatherName": self.father_name,
                "DateOfBirth": str(self.date_of_birth) if self.date_of_birth else "",
                "AadhaarOrUID": self.aadhaar_or_uid,
                "PAN": self.pan,
                "ResidentialStatus": self.residential_status,
                "PrevEmploymentDetail": [
                    {
                        "EmployerName": e.employer_name,
                        "EmployerAddress": e.employer_address,
                        "EmployerCountry": e.employer_country,
                        "NatureOfEmployment": e.nature_of_employment,
                        "Period": f"{e.period_from} to {e.period_to}",
                        "Remuneration": e.remuneration,
                    }
                    for e in self.previous_employments
                ],
            }
        }

    def validate(self) -> List[ValidationError]:
        return []
