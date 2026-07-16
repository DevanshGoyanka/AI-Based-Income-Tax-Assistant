"""Detailed 80E, 80EE, 80EEA, 80EEB, 80DDB, 80DD, 80U schedules.
Used in: ITR-2, ITR-3, ITR-4.
"""
from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal
from typing import List, Optional
from uuid import UUID, uuid4

from ..value_objects import Money
from .base import ValidationError


# ─── 80E — Education Loan ───────────────────────────────────────────────────

@dataclass
class Section80EEntry:
    """80E — Education loan interest (no limit, up to 8 years)."""
    id: UUID = field(default_factory=uuid4)
    bank_name: str = ""
    loan_account_number: str = ""
    date_of_loan: Optional[date] = None
    student_name: str = ""
    relationship: str = ""          # Self, Spouse, Child
    course_name: str = ""
    interest_paid_fy: int = 0      # Interest paid in this FY
    total_interest_claimed: int = 0  # Cumulative interest claimed (up to 8 years)
    years_elapsed: int = 0          # Number of years since loan started

    def eligible_deduction(self) -> int:
        # Can claim up to 8 years from first repayment
        if self.years_elapsed > 8:
            return 0
        return self.interest_paid_fy

    def validate(self) -> List[ValidationError]:
        errors = []
        if self.years_elapsed > 8:
            errors.append(ValidationError(
                field="Section80E.years_elapsed",
                message="80E deduction allowed for maximum 8 assessment years",
                severity="WARNING"
            ))
        return errors


# ─── 80EE — First Home Loan ─────────────────────────────────────────────────

@dataclass
class Section80EEEntry:
    """80EE — Interest on first home loan (₹50,000 cap)."""
    id: UUID = field(default_factory=uuid4)
    bank_name: str = ""
    loan_account_number: str = ""
    date_of_loan: Optional[date] = None
    loan_amount: int = 0            # Must be ≤₹35,00,000
    interest_paid_fy: int = 0
    property_value: int = 0
    stamp_duty_value: int = 0

    def eligible_deduction(self) -> int:
        # Loan ≤₹35L, interest ≤₹50,000
        if self.loan_amount > 35_000_000:
            return 0
        return min(self.interest_paid_fy, 50_000)

    def validate(self) -> List[ValidationError]:
        errors = []
        if self.loan_amount > 35_000_000:
            errors.append(ValidationError(
                field="Section80EE.loan_amount",
                message="80EE applicable only for loan ≤₹35,00,000",
                severity="WARNING"
            ))
        return errors


# ─── 80EEA — Affordable Housing ─────────────────────────────────────────────

@dataclass
class Section80EEAEntry:
    """80EEA — Interest on affordable housing loan (₹1,50,000 cap)."""
    id: UUID = field(default_factory=uuid4)
    bank_name: str = ""
    loan_account_number: str = ""
    date_of_loan: Optional[date] = None
    interest_paid_fy: int = 0
    stamp_duty_value: int = 0        # Must be ≤₹45,00,000

    def eligible_deduction(self) -> int:
        if self.stamp_duty_value > 45_000_000:
            return 0
        return min(self.interest_paid_fy, 150_000)

    def validate(self) -> List[ValidationError]:
        errors = []
        if self.stamp_duty_value > 45_000_000:
            errors.append(ValidationError(
                field="Section80EEA.stamp_duty_value",
                message="80EEA applicable only for stamp duty value ≤₹45,00,000",
                severity="WARNING"
            ))
        return errors


# ─── 80EEB — Electric Vehicle ────────────────────────────────────────────────

@dataclass
class Section80EEBEntry:
    """80EEB — Interest on electric vehicle loan (₹1,50,000 cap)."""
    id: UUID = field(default_factory=uuid4)
    bank_name: str = ""
    loan_account_number: str = ""
    date_of_loan: Optional[date] = None
    interest_paid_fy: int = 0
    vehicle_registration_number: str = ""
    vehicle_type: str = ""  # "EV Car", "EV Two Wheeler"

    def eligible_deduction(self) -> int:
        return min(self.interest_paid_fy, 150_000)

    def validate(self) -> List[ValidationError]:
        return []


# ─── 80DDB — Medical Treatment ───────────────────────────────────────────────

DISEASE_LIMITS = {
    "CANCER": 1_00_000,
    "AIDS": 1_00_000,
    "HEMOPHILIA": 1_00_000,
    "THALASSEMIA": 1_00_000,
    "KIDNEY_FAILURE": 1_00_000,
    "PARKINSON": 1_00_000,
    "OTHER": 40_000,
}


@dataclass
class Section80DDBEntry:
    """80DDB — Medical treatment of dependent (₹40,000 or ₹1,00,000)."""
    id: UUID = field(default_factory=uuid4)
    patient_name: str = ""
    relationship: str = ""  # Self, Spouse, Child, Dependent
    disease_type: str = ""  # Cancer, AIDS, Hemophilia, etc.
    pan_of_dependent: str = ""
    aadhaar_of_dependent: str = ""
    treating_doctor_name: str = ""
    hospital_name: str = ""
    treatment_expenses: int = 0
    deduction_claimed: int = 0

    def compute_deduction(self) -> int:
        limit = DISEASE_LIMITS.get(self.disease_type, DISEASE_LIMITS["OTHER"])
        self.deduction_claimed = min(self.treatment_expenses, limit)
        return self.deduction_claimed


# ─── 80DD / 80U — Disability Deductions ─────────────────────────────────────

@dataclass
class Section80DDEntry:
    """80DD — Maintenance / medical treatment of dependent with disability."""
    id: UUID = field(default_factory=uuid4)
    dependent_name: str = ""
    relationship: str = ""
    disability_type: str = ""      # Physical, Mental
    pan_of_dependent: str = ""
    aadhaar_of_dependent: str = ""
    udid_number: str = ""           # Unique Disability ID
    disability_percentage: int = 0  # 40-100%
    is_severe: bool = False         # ≥80% = severe disability
    form_10ia_date: Optional[date] = None
    deduction_claimed: int = 0

    def compute_deduction(self) -> int:
        if self.disability_percentage < 40:
            return 0
        limit = 125_000 if self.is_severe else 75_000
        self.deduction_claimed = limit
        return self.deduction_claimed


@dataclass
class Section80UEntry:
    """80U — Self with disability."""
    id: UUID = field(default_factory=uuid4)
    assessee_name: str = ""
    disability_type: str = ""
    udid_number: str = ""
    disability_percentage: int = 0  # 40-100%
    is_severe: bool = False
    form_10ia_date: Optional[date] = None
    deduction_claimed: int = 0

    def compute_deduction(self) -> int:
        if self.disability_percentage < 40:
            return 0
        limit = 125_000 if self.is_severe else 75_000
        self.deduction_claimed = limit
        return self.deduction_claimed


# ─── Combined 80E/EE/EEA/EEB/80DDB/80DD/80U ──────────────────────────────

@dataclass
class Schedule80Loans:
    """Combined 80E/EE/EEA/EEB deductions."""
    education_loans: List[Section80EEntry] = field(default_factory=list)
    first_home_loans: List[Section80EEEntry] = field(default_factory=list)
    affordable_housing_loans: List[Section80EEAEntry] = field(default_factory=list)
    ev_loans: List[Section80EEBEntry] = field(default_factory=list)

    def total_education(self) -> int:
        return sum(e.eligible_deduction() for e in self.education_loans)

    def total_first_home(self) -> int:
        return sum(e.eligible_deduction() for e in self.first_home_loans)

    def total_affordable_housing(self) -> int:
        return sum(e.eligible_deduction() for e in self.affordable_housing_loans)

    def total_ev(self) -> int:
        return sum(e.eligible_deduction() for e in self.ev_loans)

    def total(self) -> int:
        return (self.total_education() + self.total_first_home() +
                self.total_affordable_housing() + self.total_ev())

    def to_itr_json(self) -> dict:
        return {
            "Schedule80E": {
                "EducationLoanDetail": [
                    {"BankName": e.bank_name, "InterestPaid": e.interest_paid_fy, "StudentName": e.student_name}
                    for e in self.education_loans
                ],
                "TotalAmount": self.total_education(),
            },
            "Schedule80EE": {
                "FirstHomeLoanDetail": [
                    {"BankName": e.bank_name, "InterestPaid": e.interest_paid_fy, "LoanAmount": e.loan_amount}
                    for e in self.first_home_loans
                ],
                "TotalAmount": self.total_first_home(),
            },
            "Schedule80EEA": {
                "AffordableHousingDetail": [
                    {"BankName": e.bank_name, "InterestPaid": e.interest_paid_fy, "PropertyValue": e.stamp_duty_value}
                    for e in self.affordable_housing_loans
                ],
                "TotalAmount": self.total_affordable_housing(),
            },
            "Schedule80EEB": {
                "EVLoanDetail": [
                    {"BankName": e.bank_name, "InterestPaid": e.interest_paid_fy, "VehicleRegNo": e.vehicle_registration_number}
                    for e in self.ev_loans
                ],
                "TotalAmount": self.total_ev(),
            },
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        for e in self.education_loans:
            errors.extend(e.validate())
        for e in self.first_home_loans:
            errors.extend(e.validate())
        return errors
