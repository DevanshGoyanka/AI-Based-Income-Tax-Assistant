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
from .schedule_cyla_bfla_cfl import ScheduleCYLA, ScheduleBFLA, ScheduleCFL, CYLALossSetoff, CFLLossEntry
from .schedule_ei import ScheduleEI, ScheduleUs24B
from .schedule_foreign import (
    ScheduleFA, ScheduleAL, ScheduleFSI, ScheduleTR1, Schedule5A2014,
    FAImmovableProperty, FAFinancialInterest, FACustodialAccount, FABankAccount,
    FAOtherAsset, FASigningAuthority, FATrustDetails, FAOtherSourceIncome,
    ImmovableAssetDetail, MovableAssetDetail, AssetLiabilityDetail,
    FSICountryIncome, TR1Entry, PreviousEmploymentDetail
)
from .schedule_si_if_pti import ScheduleSI, ScheduleIF, SchedulePTI, ScheduleTPSA, SIEntry, PartnerFirmDetail, PTIEntry, TPSATaxPayment
from .schedule_amt import ScheduleAMT, ScheduleAMTC
from .schedule_10aa import Schedule10AA
from .schedule_bp import (
    ScheduleBP, BusinessPresumptiveDetail, ProfessionPresumptiveDetail, TransportPresumptiveDetail,
    BusinessDetailITR3, ManufacturingAccount, TradingAccount, ScheduleDPM, ScheduleDOA, ScheduleDCG,
    DepreciationItem, DeductUs35, ScheduleESR
)
from .schedule_80c_detail import Section80CEntry, Schedule80C
from .schedule_80d_detail import Section80DEntry, Schedule80D as Schedule80DDetail
from .schedule_80g_detail import Schedule80G, DoneeWithPAN, Schedule80GGA, Section80GGAEntry, Schedule80GGC, Section80GGCEntry
from .schedule_80_loan_disability import (
    Section80EEntry, Section80EEEntry, Section80EEAEntry, Section80EEBEntry,
    Section80DDBEntry, Section80DDEntry, Section80UEntry, Schedule80Loans
)

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
    "ScheduleCYLA",
    "ScheduleBFLA",
    "ScheduleCFL",
    "CYLALossSetoff",
    "CFLLossEntry",
    "ScheduleEI",
    "ScheduleUs24B",
    "ScheduleFA",
    "ScheduleAL",
    "ScheduleFSI",
    "ScheduleTR1",
    "Schedule5A2014",
    "FAImmovableProperty",
    "FAFinancialInterest",
    "FACustodialAccount",
    "FABankAccount",
    "FAOtherAsset",
    "FASigningAuthority",
    "FATrustDetails",
    "FAOtherSourceIncome",
    "ImmovableAssetDetail",
    "MovableAssetDetail",
    "AssetLiabilityDetail",
    "FSICountryIncome",
    "TR1Entry",
    "PreviousEmploymentDetail",
    "ScheduleSI",
    "ScheduleIF",
    "SchedulePTI",
    "ScheduleTPSA",
    "SIEntry",
    "PartnerFirmDetail",
    "PTIEntry",
    "TPSATaxPayment",
    "ScheduleAMT",
    "ScheduleAMTC",
    "Schedule10AA",
    "ScheduleBP",
    "BusinessPresumptiveDetail",
    "ProfessionPresumptiveDetail",
    "TransportPresumptiveDetail",
    "BusinessDetailITR3",
    "ManufacturingAccount",
    "TradingAccount",
    "ScheduleDPM",
    "ScheduleDOA",
    "ScheduleDCG",
    "DepreciationItem",
    "DeductUs35",
    "ScheduleESR",
    "Section80CEntry",
    "Schedule80C",
    "Section80DEntry",
    "Schedule80DDetail",
    "Schedule80G",
    "DoneeWithPAN",
    "Schedule80GGA",
    "Section80GGAEntry",
    "Schedule80GGC",
    "Section80GGCEntry",
    "Section80EEntry",
    "Section80EEEntry",
    "Section80EEAEntry",
    "Section80EEBEntry",
    "Section80DDBEntry",
    "Section80DDEntry",
    "Section80UEntry",
    "Schedule80Loans",
]
