"""Schedule domain entities."""
from .base import Schedule, ValidationError
from .salary import ScheduleSalary, SalaryDetail
from .house_property import ScheduleHP
from .tds import ScheduleTDS
from .other_sources import ScheduleOS
from .schedule_via import ScheduleVIA, Section80C, Section80D
from .schedule_it import ScheduleIT, AdvanceTaxPayment, SelfAssessmentTax
from .schedule_ba import ScheduleBA, BankAccount
from .schedule_cg import ScheduleCG, CGTransaction
from .schedule_tcs import ScheduleTCS, TCSDetail

__all__ = [
    "Schedule",
    "ValidationError",
    "ScheduleSalary",
    "SalaryDetail",
    "ScheduleHP",
    "ScheduleTDS",
    "ScheduleOS",
    "ScheduleVIA",
    "Section80C",
    "Section80D",
    "ScheduleIT",
    "AdvanceTaxPayment",
    "SelfAssessmentTax",
    "ScheduleBA",
    "BankAccount",
    "ScheduleCG",
    "CGTransaction",
    "ScheduleTCS",
    "TCSDetail",
]
