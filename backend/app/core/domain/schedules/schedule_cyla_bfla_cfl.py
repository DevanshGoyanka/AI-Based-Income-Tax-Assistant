"""Schedule CYLA — Current Year Loss Adjustment.
Inter-head loss setoff within the same AY (Section 71).
Used in: ITR-2, ITR-3, ITR-4 (not ITR-1).
"""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import List, Optional
from uuid import UUID, uuid4

from ..value_objects import Money
from .base import Schedule, ValidationError


@dataclass
class CYLALossSetoff:
    """Loss setoff from a single income head."""
    income_of_current_year: int = 0       # Positive income from the head
    loss_of_current_year: int = 0         # Loss to set off against this head
    income_after_setoff: int = 0          # income_of_current_year + loss_of_current_year

    def net_income(self) -> int:
        return max(0, self.income_of_current_year + self.loss_of_current_year)


@dataclass
class CYLACGSetoff:
    """CG-specific loss setoff (STCG and LTCG tracked separately)."""
    stcg_income: int = 0
    stcg_loss: int = 0
    stcg_after_setoff: int = 0
    ltcg_income: int = 0
    ltcg_loss: int = 0
    ltcg_after_setoff: int = 0

    def total_income(self) -> int:
        return self.stcg_after_setoff + self.ltcg_after_setoff


@dataclass
class ScheduleCYLA(Schedule[dict]):
    """Schedule CYLA — Current Year Loss Adjustment.
    
    Loss setoff order (Rule 4):
    1. Salary loss set off against HP, OS, BP, CG gains
    2. HP loss set off against salary, OS, BP, CG gains
    3. CG loss (STCG) set off against any positive income
    4. CG loss (LTCG) set off against any positive income
    5. OS loss (non-race horse) set off against any positive income
    6. Race horse loss set off against race horse income only
    
    Maximum setoff: loss can reduce income to zero but not create negative.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    # Income / Loss per head
    salary: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    hp: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    bp_excl_spec_prof: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    speculative_inc: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    specified_inc: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    stcg_20_pct: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    stcg_30_pct: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    stcg_applicable_rate: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    stcg_dtaa_rate: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    ltcg_12_5_pct: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    ltcg_dtaa_rate: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    os_excl_race_horse: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    os_race_horse: CYLALossSetoff = field(default_factory=CYLALossSetoff)
    os_dtaa: CYLALossSetoff = field(default_factory=CYLALossSetoff)

    # Totals
    total_loss_available: int = 0
    total_loss_set_off: int = 0
    loss_remaining_after_setoff: int = 0

    def total_income_after_setoff(self) -> int:
        """Total income after inter-head loss setoff."""
        return (
            self.salary.income_after_setoff +
            self.hp.income_after_setoff +
            self.bp_excl_spec_prof.income_after_setoff +
            self.speculative_inc.income_after_setoff +
            self.specified_inc.income_after_setoff +
            self.stcg_20_pct.income_after_setoff +
            self.stcg_30_pct.income_after_setoff +
            self.stcg_applicable_rate.income_after_setoff +
            self.stcg_dtaa_rate.income_after_setoff +
            self.ltcg_12_5_pct.income_after_setoff +
            self.ltcg_dtaa_rate.income_after_setoff +
            self.os_excl_race_horse.income_after_setoff +
            self.os_race_horse.income_after_setoff +
            self.os_dtaa.income_after_setoff
        )

    def compute_income(self) -> Decimal:
        return Decimal(self.total_income_after_setoff())

    def to_itr_json(self) -> dict:
        return {
            "ScheduleCYLA": {
                "Salary": {
                    "IncOfCurYrUnderThatHead": self.salary.income_of_current_year,
                    "HPlossCurYrSetOff": self.salary.loss_of_current_year,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.salary.income_after_setoff,
                },
                "HP": {
                    "IncOfCurYrUnderThatHead": self.hp.income_of_current_year,
                    "HPlossCurYrSetOff": self.hp.loss_of_current_year,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.hp.income_after_setoff,
                },
                "BusProfExclSpecProf": {
                    "IncOfCurYrUnderThatHead": self.bp_excl_spec_prof.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": self.bp_excl_spec_prof.loss_of_current_year,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.bp_excl_spec_prof.income_after_setoff,
                },
                "SpeculativeInc": {
                    "IncOfCurYrUnderThatHead": self.speculative_inc.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.speculative_inc.income_after_setoff,
                },
                "SpecifiedInc": {
                    "IncOfCurYrUnderThatHead": self.specified_inc.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.specified_inc.income_after_setoff,
                },
                "STCG20Per": {
                    "IncOfCurYrUnderThatHead": self.stcg_20_pct.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.stcg_20_pct.income_after_setoff,
                },
                "STCG30Per": {
                    "IncOfCurYrUnderThatHead": self.stcg_30_pct.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.stcg_30_pct.income_after_setoff,
                },
                "STCGAppRate": {
                    "IncOfCurYrUnderThatHead": self.stcg_applicable_rate.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.stcg_applicable_rate.income_after_setoff,
                },
                "STCGDTAARate": {
                    "IncOfCurYrUnderThatHead": self.stcg_dtaa_rate.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.stcg_dtaa_rate.income_after_setoff,
                },
                "LTCG12_5Per": {
                    "IncOfCurYrUnderThatHead": self.ltcg_12_5_pct.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.ltcg_12_5_pct.income_after_setoff,
                },
                "LTCGDTAARate": {
                    "IncOfCurYrUnderThatHead": self.ltcg_dtaa_rate.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.ltcg_dtaa_rate.income_after_setoff,
                },
                "OthSrcExclRaceHorse": {
                    "IncOfCurYrUnderThatHead": self.os_excl_race_horse.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": self.os_excl_race_horse.loss_of_current_year,
                    "IncOfCurYrAfterSetOff": self.os_excl_race_horse.income_after_setoff,
                },
                "OthSrcRaceHorse": {
                    "IncOfCurYrUnderThatHead": self.os_race_horse.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.os_race_horse.income_after_setoff,
                },
                "IncOSDTAA": {
                    "IncOfCurYrUnderThatHead": self.os_dtaa.income_of_current_year,
                    "HPlossCurYrSetOff": 0,
                    "BusLossSetOff": 0,
                    "OthSrcLossNoRaceHorseSetOff": 0,
                    "IncOfCurYrAfterSetOff": self.os_dtaa.income_after_setoff,
                },
                "TotalCurYr": {"TotalLoss": self.total_loss_available},
                "TotalLossSetOff": {"TotalLossSetOff": self.total_loss_set_off},
                "LossRemAftSetOff": {"LossRemaining": self.loss_remaining_after_setoff},
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        # Validate no negative income after setoff
        for head_name, head in [
            ("salary", self.salary), ("hp", self.hp),
            ("bp_excl_spec_prof", self.bp_excl_spec_prof),
            ("os_excl_race_horse", self.os_excl_race_horse),
        ]:
            if head.income_after_setoff < 0:
                errors.append(ValidationError(
                    field=f"ScheduleCYLA.{head_name}",
                    message=f"Income after setoff cannot be negative for {head_name}",
                    severity="BLOCKING"
                ))
        # Race horse loss can only set off race horse income
        if self.os_race_horse.loss_of_current_year > 0 and self.os_race_horse.income_of_current_year == 0:
            if self.os_race_horse.income_after_setoff != self.os_race_horse.loss_of_current_year:
                pass  # Already in correct format
        return errors


@dataclass
class ScheduleBFLA(Schedule[dict]):
    """Schedule BFLA — Brought Forward Loss Adjustment.
    
    Setoff of carried-forward losses from previous AYs against current year income.
    Used in: ITR-2, ITR-3, ITR-4.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    # BFL from previous years per head
    bfl_salary: int = 0
    bfl_hp: int = 0
    bfl_bp_excl_spec_prof: int = 0
    bfl_speculative_inc: int = 0
    bfl_specified_inc: int = 0
    bfl_stcg_20_pct: int = 0
    bfl_stcg_30_pct: int = 0
    bfl_stcg_applicable_rate: int = 0
    bfl_stcg_dtaa_rate: int = 0
    bfl_ltcg_12_5_pct: int = 0
    bfl_ltcg_dtaa_rate: int = 0
    bfl_os_excl_race_horse: int = 0
    bfl_os_race_horse: int = 0

    # Setoff used this year
    setoff_salary: int = 0
    setoff_hp: int = 0
    setoff_bp: int = 0
    setoff_speculative: int = 0
    setoff_specified: int = 0
    setoff_stcg_20_pct: int = 0
    setoff_stcg_30_pct: int = 0
    setoff_stcg_applicable_rate: int = 0
    setoff_stcg_dtaa_rate: int = 0
    setoff_ltcg_12_5_pct: int = 0
    setoff_ltcg_dtaa_rate: int = 0
    setoff_os_excl_race_horse: int = 0
    setoff_os_race_horse: int = 0

    # Income after BFLA setoff (from CYLA)
    income_from_cyla_salary: int = 0
    income_from_cyla_hp: int = 0
    income_from_cyla_bp: int = 0
    income_from_cyla_stcg: int = 0
    income_from_cyla_ltcg: int = 0
    income_from_cyla_os: int = 0

    # Totals
    total_bfl_loss: int = 0
    total_bfl_setoff: int = 0
    bfl_remaining: int = 0
    income_after_bfla: int = 0

    def compute_income(self) -> Decimal:
        return Decimal(self.income_after_bfla)

    def to_itr_json(self) -> dict:
        return {
            "ScheduleBFLA": {
                "Salary": {
                    "IncOfCurYrUndHeadFromCYLA": self.income_from_cyla_salary,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_salary,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": max(0, self.income_from_cyla_salary - self.bfl_salary),
                },
                "HP": {
                    "IncOfCurYrUndHeadFromCYLA": self.income_from_cyla_hp,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_hp,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": max(0, self.income_from_cyla_hp - self.bfl_hp),
                },
                "BusProfExclSpecProf": {
                    "IncOfCurYrUndHeadFromCYLA": self.income_from_cyla_bp,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_bp_excl_spec_prof,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": max(0, self.income_from_cyla_bp - self.bfl_bp_excl_spec_prof),
                },
                "SpeculativeInc": {
                    "IncOfCurYrUndHeadFromCYLA": 0,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_speculative_inc,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": 0,
                },
                "SpecifiedInc": {
                    "IncOfCurYrUndHeadFromCYLA": 0,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_specified_inc,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": 0,
                },
                "STCG20Per": {
                    "IncOfCurYrUndHeadFromCYLA": 0,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_stcg_20_pct,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": 0,
                },
                "STCG30Per": {
                    "IncOfCurYrUndHeadFromCYLA": 0,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_stcg_30_pct,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": 0,
                },
                "STCGAppRate": {
                    "IncOfCurYrUndHeadFromCYLA": 0,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_stcg_applicable_rate,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": 0,
                },
                "STCGDTAARate": {
                    "IncOfCurYrUndHeadFromCYLA": 0,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_stcg_dtaa_rate,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": 0,
                },
                "LTCG12_5Per": {
                    "IncOfCurYrUndHeadFromCYLA": self.income_from_cyla_ltcg,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_ltcg_12_5_pct,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": max(0, self.income_from_cyla_ltcg - self.bfl_ltcg_12_5_pct),
                },
                "LTCGDTAARate": {
                    "IncOfCurYrUndHeadFromCYLA": 0,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_ltcg_dtaa_rate,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": 0,
                },
                "OthSrcExclRaceHorse": {
                    "IncOfCurYrUndHeadFromCYLA": self.income_from_cyla_os,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_os_excl_race_horse,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": max(0, self.income_from_cyla_os - self.bfl_os_excl_race_horse),
                },
                "OthSrcRaceHorse": {
                    "IncOfCurYrUndHeadFromCYLA": 0,
                    "BFlossPrevYrUndSameHeadSetOff": self.bfl_os_race_horse,
                    "BFUnabsorbedDeprSetOff": 0,
                    "BFAllUs35Cl4SetOff": 0,
                    "IncOfCurYrAfterSetOffBFLosses": 0,
                },
                "IncOSDTAA": {"IncOfCurYrUndHeadFromCYLA": 0, "BFlossPrevYrUndSameHeadSetOff": 0, "BFUnabsorbedDeprSetOff": 0, "BFAllUs35Cl4SetOff": 0, "IncOfCurYrAfterSetOffBFLosses": 0},
                "TotalBFLossSetOff": {"TotalBFLoss": self.total_bfl_loss, "TotalBFLossSetOff": self.total_bfl_setoff, "BFLossRemaining": self.bfl_remaining},
                "IncomeOfCurrYrAftCYLABFLA": self.income_after_bfla,
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        # BFL setoff cannot exceed BFL amount
        if self.setoff_salary > self.bfl_salary:
            errors.append(ValidationError("ScheduleBFLA.salary", "Setoff exceeds BFL amount", "BLOCKING"))
        if self.setoff_hp > self.bfl_hp:
            errors.append(ValidationError("ScheduleBFLA.hp", "Setoff exceeds BFL amount", "BLOCKING"))
        return errors


@dataclass
class CFLLossEntry:
    """Single CFL entry — loss brought forward from a specific AY."""
    assessment_year: str = ""   # e.g. "2023-24"
    loss_type: str = ""          # "STCG", "LTCG", "HP", "OS", "BP", "SpecBus"
    original_ay: str = ""        # AY in which loss occurred
    amount: int = 0              # Loss amount


@dataclass
class ScheduleCFL(Schedule[dict]):
    """Schedule CFL — Carry Forward Losses.
    
    Tracks losses that remain after CYLA and BFLA setoff,
    and can be carried forward to subsequent years.
    
    Loss carry-forward limits:
    - STCG: 8 years
    - LTCG: Indefinite (permanent)
    - HP loss: 8 years
    - OS loss: 8 years (except race horse: 4 years)
    - Business loss: 8 years
    - Speculative business: 4 years
    
    Used in: ITR-2, ITR-3, ITR-4.
    """
    id: UUID = field(default_factory=uuid4)
    filing_id: UUID = field(default_factory=uuid4)
    ay: str = "2026-27"

    # Losses from previous years by AY
    losses_by_ay: List[CFLLossEntry] = field(default_factory=list)

    # Current year losses after CYLA+BFLA
    current_year_stcg: int = 0
    current_year_ltcg: int = 0
    current_year_hp: int = 0
    current_year_os: int = 0
    current_year_bp: int = 0
    current_year_spec_bus: int = 0

    # Utilization under Section 71 (inter-head setoff already done)
    utilized_under_s71: int = 0

    # Summary totals
    total_bfl_losses: int = 0
    total_current_year_loss: int = 0
    total_loss_available: int = 0
    total_loss_set_off: int = 0
    loss_remaining_carry_forward: int = 0

    def compute_income(self) -> Decimal:
        # CFL doesn't add income, it tracks losses
        return Decimal(0)

    def to_itr_json(self) -> dict:
        entries = []
        for entry in self.losses_by_ay:
            entries.append({
                "AY": entry.assessment_year,
                "LossType": entry.loss_type,
                "OriginalAY": entry.original_ay,
                "LossAmt": entry.amount,
            })
        return {
            "ScheduleCFL": {
                "LossCFFromPrevYrToAY": entries,
                "TotalOfBFLossesEarlierYrs": {"TotalBFLoss": self.total_bfl_losses},
                "AdjTotBFLossInBFLA": {"TotalAdjBFLoss": self.total_loss_set_off},
                "LossCFCurrentAssmntYear": {
                    "STCG": self.current_year_stcg,
                    "LTCG": self.current_year_ltcg,
                    "HP": self.current_year_hp,
                    "OS": self.current_year_os,
                    "BusProf": self.current_year_bp,
                    "SpecBus": self.current_year_spec_bus,
                },
                "CurrentAYloss": {"TotalCurrentYearLoss": self.total_current_year_loss},
                "TotalLossCFSummary": {
                    "TotalLossAvailable": self.total_loss_available,
                    "LossAllowedSetOff": self.total_loss_set_off,
                    "LossRemaining": self.loss_remaining_carry_forward,
                },
            }
        }

    def validate(self) -> List[ValidationError]:
        errors = []
        # LTCG losses can be carried forward indefinitely
        # Other losses have 8-year limit from AY 2020-21
        current_ay = self.ay
        for entry in self.losses_by_ay:
            if entry.loss_type in ("STCG", "HP", "OS", "BP") and entry.original_ay:
                # Check 8-year rule
                try:
                    orig_year = int(entry.original_ay.split("-")[0])
                    curr_year = int(current_ay.split("-")[0])
                    if curr_year - orig_year > 8:
                        errors.append(ValidationError(
                            field=f"ScheduleCFL.{entry.loss_type}",
                            message=f"{entry.loss_type} loss from AY {entry.original_ay} cannot be carried beyond 8 years",
                            severity="BLOCKING"
                        ))
                except (ValueError, IndexError):
                    pass
        return errors
