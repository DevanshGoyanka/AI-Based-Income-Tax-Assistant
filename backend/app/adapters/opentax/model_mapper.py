"""Model mapper — ERP schedules to OpenTax FilingModel Pydantic objects."""
from typing import List, Optional
from decimal import Decimal

from app.core.domain.schedules.base import Schedule
from app.core.domain.schedules.salary import ScheduleSalary, SalaryDetail
from app.core.domain.schedules.house_property import ScheduleHP
from app.core.domain.schedules.schedule_via import ScheduleVIA, Section80C, Section80D
from app.core.domain.schedules.tds import ScheduleTDS

# Vendored OpenTax models — all use camelCase aliases internally
from app.adapters.opentax.vendor.filing.models.filing_model import FilingModel
from app.adapters.opentax.vendor.filing.models.person_model import PersonModel
from app.adapters.opentax.vendor.filing.models.employer_model import EmployerModel
from app.adapters.opentax.vendor.filing.models.salary_model import SalaryModel
from app.adapters.opentax.vendor.filing.models.salary_deduction_16_model import SalaryDeduction16Model
from app.adapters.opentax.vendor.filing.models.house_property_model import HousePropertyModel
from app.adapters.opentax.vendor.filing.models.property_model import PropertyModel
from app.adapters.opentax.vendor.filing.models.deduction_80c_model import Deduction80CModel
from app.adapters.opentax.vendor.filing.models.deduction_80d_model import Deduction80DModel
from app.adapters.opentax.vendor.filing.models.tds_model import TDSModel
from app.adapters.opentax.vendor.filing.models.chapter_via_deduction import ChapterVIADeductions, DeductionAmount
from app.adapters.opentax.vendor.filing.models.deduction_80d_model import (
    Deduction80DModel, Deduction80DHealthInsuranceModel, Deduction80DPreventiveCheckupModel
)


class ScheduleToOpenTaxMapper:
    """Maps ERP domain schedules to OpenTax FilingModel."""

    def map_schedules(
        self,
        schedules: List[Schedule],
        regime: str,
        ay: str
    ) -> FilingModel:
        """Convert ERP Schedules to an OpenTax FilingModel.

        The FilingModel is a Pydantic object that is passed directly to
        TaxCalculationService.calculate() (which is async and also returns
        a FilingModel with tax_computation populated).

        Args:
            schedules: List of domain schedules
            regime: "old" or "new"
            ay: Assessment year

        Returns:
            FilingModel ready for OpenTax computation
        """
        salary_list: List[SalaryModel] = []
        hp_list: List[PropertyModel] = []
        section_80c_items: List[Deduction80CModel] = []
        section_80d: Optional[Deduction80DModel] = None
        tds_list: List[TDSModel] = []
        filing_id = 1  # OpenTax uses int for filing_id; set to placeholder

        for schedule in schedules:
            if isinstance(schedule, ScheduleSalary):
                salary_list.extend(self._map_salary(schedule))
            elif isinstance(schedule, ScheduleHP):
                hp_list.extend(self._map_house_property(schedule))
            elif isinstance(schedule, ScheduleVIA):
                c80c, c80d = self._map_deductions(schedule)
                section_80c_items.extend(c80c)
                if c80d is not None:
                    section_80d = c80d
            elif isinstance(schedule, ScheduleTDS):
                tds_list.extend(self._map_tds(schedule))

        # Build ChapterVIADeductions for both regimes
        # section_80c total (capped at 150000 by ERP ScheduleVIA)
        c80c_claimed = int(sum(item.amount for item in section_80c_items)) if section_80c_items else 0
        chapter_via_new = ChapterVIADeductions(
            section_80c=DeductionAmount(claimed=c80c_claimed, max_allowed=150000, allowed=c80c_claimed),
            section_80d=DeductionAmount(
                claimed=int(section_80d.total()) if section_80d else 0,
                max_allowed=50000,
                allowed=0,  # will be computed by OpenTax
            ),
        )
        chapter_via_old = chapter_via_new  # same deductions for now

        # Build section_80c list for FilingModel.section_80c field
        filing_section_80c = [
            Deduction80CModel(
                filing_id=1,
                description=item.description,
                amount=item.amount,
            )
            for item in section_80c_items
        ] if section_80c_items else []

        filing = FilingModel(
            filing_id=filing_id,
            assessment_year=ay,
            regime=regime,
            salary=salary_list,
            house_property=hp_list,
            tds=tds_list,
            section_80c=filing_section_80c,
            section_80d=section_80d,
            chapterVIADeductionsNew=chapter_via_new,
            chapterVIADeductionsOld=chapter_via_old,
            bank_account=[],
            interest_income=[],
            capital_gains_securities=None,
            foreign_income=None,
            agricultural_income=None,
            dividend_income=None,
            equity_compensation_income=None,
            other_deductions=None,
            immovable_assets=[],
            financial_assets=[],
            other_assets=[],
            liabilities=[],
            investment_firm_llp_aop=[],
            tcs=[],
            advance_tax=[],
            form16_metadata=[],
        )

        return filing

    def _map_salary(self, schedule: ScheduleSalary) -> List[SalaryModel]:
        """Map salary schedule to OpenTax SalaryModel list."""
        result: List[SalaryModel] = []
        for emp in schedule.employers:
            employer = EmployerModel(
                filing_id=1,
                employer_name=emp.employer_name,
                tan_number=emp.employer_tan,
            )
            salary_deduction = SalaryDeduction16Model(
                standard_deduction=Decimal(str(emp.standard_deduction.to_rupees()))
                if emp.standard_deduction
                else Decimal("75000"),
                professional_tax=Decimal(str(emp.professional_tax.to_rupees()))
                if emp.professional_tax
                else Decimal("0"),
            )
            salary = SalaryModel(
                employer=employer,
                salary_deduction_16=salary_deduction,
            )
            result.append(salary)
        return result

    def _map_house_property(self, schedule: ScheduleHP) -> List[PropertyModel]:
        """Map house property schedule to OpenTax PropertyModel list."""
        result: List[PropertyModel] = []
        for prop in schedule.properties:
            hp = HousePropertyModel(
                filing_id=1,
                property_type="self_occupied",  # default; could be "let_out"
                annual_rent_received=Decimal(str(prop.annual_rent.to_rupees())),
                municipal_taxes_paid=Decimal("0"),
                interest_on_housing_loan=Decimal(str(prop.interest_paid.to_rupees())),
            )
            pm = PropertyModel(property=hp)
            result.append(pm)
        return result

    def _map_deductions(
        self, schedule: ScheduleVIA
    ) -> tuple[List[Deduction80CModel], Optional[Deduction80DModel]]:
        """Map Chapter VI-A deductions from a ScheduleVIA."""
        c80c: List[Deduction80CModel] = self._map_section_80c(schedule.section_80c)

        c80d: Optional[Deduction80DModel] = None
        if schedule.section_80d is not None:
            c80d = self._map_section_80d(schedule.section_80d)

        return c80c, c80d

    def _map_section_80c(self, s80c: Section80C) -> List[Deduction80CModel]:
        """Map ERP Section80C to OpenTax Deduction80CModel list."""
        items: List[Deduction80CModel] = []
        mapping = [
            ("life_insurance", "Life Insurance Premium"),
            ("ppf", "PPF Account"),
            ("elss", "ELSS Mutual Funds"),
            ("nsc", "National Savings Certificate"),
            ("epf", "EPF Contribution"),
            ("vpf", "Voluntary Provident Fund"),
            ("nps", "NPS Tier-1"),
            ("tuition_fees", "Children Tuition Fees"),
            ("home_loan_principal", "Home Loan Principal Repayment"),
            ("sukanya_samriddhi", "Sukanya Samriddhi Yojana"),
        ]
        for field, label in mapping:
            val = getattr(s80c, field, None)
            if val and val.to_rupees() > 0:
                items.append(Deduction80CModel(
                    filing_id=1,
                    description=label,
                    amount=Decimal(str(val.to_rupees())),
                ))
        return items

    def _map_section_80d(self, s80d: Section80D) -> Deduction80DModel:
        """Map ERP Section80D to OpenTax Deduction80DModel.

        OpenTax Deduction80DModel has list fields:
          health_insurance: list[Deduction80DHealthInsuranceModel]
          preventive_checkup: list[Deduction80DPreventiveCheckupModel]
          medical_expenditure: list[Deduction80DMedicalExpenditureModel]
        """
        health_ins = Deduction80DHealthInsuranceModel(
            self_amount=Decimal(str(s80d.self_family.to_rupees())),
            family_amount=Decimal(str(s80d.parents.to_rupees())),
            preventive_checkup=Decimal(str(s80d.preventive_checkup.to_rupees())),
        )
        preventive = Deduction80DPreventiveCheckupModel(
            self_amount=Decimal(str(s80d.preventive_checkup.to_rupees())),
        )
        return Deduction80DModel(
            filing_id=1,
            health_insurance=[health_ins],
            preventive_checkup=[preventive],
            medical_expenditure=[],
        )

    def _map_tds(self, schedule: ScheduleTDS) -> List[TDSModel]:
        """Map ERP ScheduleTDS to OpenTax TDSModel list."""
        from app.core.domain.schedules.tds import TDSDetail
        result: List[TDSModel] = []
        for entry in schedule.tds_entries:
            result.append(TDSModel(
                deductor_name=entry.deductor_name,
                tan=entry.deductor_tan or "",
                section=entry.section_code or "",
                amount=Decimal(str(entry.amount_paid.to_rupees())),
                receipt_date="",  # TDSModel requires a date field; set to empty
            ))
        return result
