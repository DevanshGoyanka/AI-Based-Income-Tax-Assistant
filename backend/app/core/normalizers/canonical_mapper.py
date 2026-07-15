"""Canonical to Schedule mapper."""
from decimal import Decimal
from typing import List
from uuid import UUID, uuid4

from app.core.domain.canonical_models import (
    CanonicalIncome,
    CanonicalSalary,
    CanonicalHouseProperty,
    CanonicalInterest,
    CanonicalDividend,
    CanonicalTDS,
)
from app.core.domain.schedules.salary import ScheduleSalary, SalaryDetail
from app.core.domain.schedules.house_property import ScheduleHP, HPDetail
from app.core.domain.schedules.other_sources import (
    ScheduleOS,
    InterestDetail,
    DividendDetail,
)
from app.core.domain.schedules.tds import ScheduleTDS, TDSDetail
from app.core.domain.value_objects import Money


class CanonicalToScheduleMapper:
    """Map canonical income models to Schedule entities."""
    
    def map_to_schedules(
        self,
        canonical: CanonicalIncome,
        filing_id: UUID,
        ay: str,
    ) -> List:
        """Convert canonical income to schedule entities."""
        schedules = []
        
        # Map salary
        if canonical.salaries:
            salary_schedule = self._map_salary(
                canonical.salaries, filing_id, ay
            )
            schedules.append(salary_schedule)
        
        # Map house property
        if canonical.house_properties:
            hp_schedule = self._map_house_property(
                canonical.house_properties, filing_id, ay
            )
            schedules.append(hp_schedule)
        
        # Map other sources
        if canonical.interest_income or canonical.dividend_income or canonical.other_income > 0:
            os_schedule = self._map_other_sources(
                canonical.interest_income,
                canonical.dividend_income,
                canonical.other_income,
                filing_id,
                ay,
            )
            schedules.append(os_schedule)
        
        # Map TDS
        if canonical.tds_entries:
            tds_schedule = self._map_tds(
                canonical.tds_entries, filing_id, ay
            )
            schedules.append(tds_schedule)
        
        return schedules
    
    def _map_salary(
        self,
        salaries: List[CanonicalSalary],
        filing_id: UUID,
        ay: str,
    ) -> ScheduleSalary:
        """Map canonical salary to ScheduleSalary."""
        employers = [
            SalaryDetail(
                employer_name=sal.employer_name,
                employer_tan=sal.employer_tan,
                gross_salary=Money.from_rupees(sal.gross_salary),
                allowances_exempt=Money.from_rupees(sal.allowances_exempt),
                professional_tax=Money.from_rupees(sal.professional_tax),
                tds_deducted=Money.from_rupees(sal.tds_deducted),
                standard_deduction=Money.from_rupees(sal.standard_deduction),
            )
            for sal in salaries
        ]
        
        return ScheduleSalary(
            id=uuid4(),
            filing_id=filing_id,
            ay=ay,
            employers=employers,
        )
    
    def _map_house_property(
        self,
        properties: List[CanonicalHouseProperty],
        filing_id: UUID,
        ay: str,
    ) -> ScheduleHP:
        """Map canonical house property to ScheduleHP."""
        hp_details = [
            HPDetail(
                address=prop.address,
                ownership_share=prop.ownership_share,
                annual_rent=Money.from_rupees(prop.annual_rent),
                municipal_taxes=Money.from_rupees(prop.municipal_taxes),
                interest_paid=Money.from_rupees(prop.interest_paid),
                co_owners=prop.co_owners,
            )
            for prop in properties
        ]
        
        return ScheduleHP(
            id=uuid4(),
            filing_id=filing_id,
            ay=ay,
            properties=hp_details,
        )
    
    def _map_other_sources(
        self,
        interest_income: List[CanonicalInterest],
        dividend_income: List[CanonicalDividend],
        other_income: Decimal,
        filing_id: UUID,
        ay: str,
    ) -> ScheduleOS:
        """Map canonical other sources to ScheduleOS."""
        interest_details = [
            InterestDetail(
                bank_name=interest.bank_name,
                account_number=interest.account_number,
                interest_amount=Money.from_rupees(interest.interest_amount),
            )
            for interest in interest_income
        ]
        
        dividend_details = [
            DividendDetail(
                company_name=div.company_name,
                dividend_amount=Money.from_rupees(div.dividend_amount),
            )
            for div in dividend_income
        ]
        
        return ScheduleOS(
            id=uuid4(),
            filing_id=filing_id,
            ay=ay,
            interest_income=interest_details,
            dividend_income=dividend_details,
            other_income=Money.from_rupees(other_income),
        )
    
    def _map_tds(
        self,
        tds_entries: List[CanonicalTDS],
        filing_id: UUID,
        ay: str,
    ) -> ScheduleTDS:
        """Map canonical TDS to ScheduleTDS."""
        tds_details = [
            TDSDetail(
                deductor_name=tds.deductor_name,
                deductor_tan=tds.deductor_tan,
                section_code=tds.section_code,
                amount_paid=Money.from_rupees(tds.amount_paid),
                tax_deducted=Money.from_rupees(tds.tax_deducted),
                tax_deposited=Money.from_rupees(tds.tax_deposited),
                quarter=tds.quarter,
            )
            for tds in tds_entries
        ]
        
        return ScheduleTDS(
            id=uuid4(),
            filing_id=filing_id,
            ay=ay,
            tds_entries=tds_details,
        )
