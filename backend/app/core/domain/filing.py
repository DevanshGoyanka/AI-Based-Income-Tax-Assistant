"""Filing aggregate - container for schedules."""
from dataclasses import dataclass, field
from decimal import Decimal
from typing import Dict, List, TypeVar, Type
from uuid import UUID

from .schedules.base import Schedule, ValidationError
from .value_objects import AssessmentYear, TaxRegime

T = TypeVar('T', bound=Schedule)


@dataclass
class FilingStatus:
    """Filing status enum."""
    value: str
    
    DRAFT = "draft"
    IMPORTED = "imported"
    COMPUTED = "computed"
    VALIDATING = "validating"
    VALIDATION_FAILED = "validation_failed"
    VALIDATED = "validated"
    SUBMITTED = "submitted"
    ACKNOWLEDGED = "acknowledged"
    FILED = "filed"


@dataclass
class Filing:
    """Filing aggregate - manages schedules for tax return."""
    
    id: UUID
    client_id: UUID
    ay: AssessmentYear
    regime: TaxRegime
    status: FilingStatus
    schedules: Dict[str, Schedule] = field(default_factory=dict)
    
    def add_schedule(self, schedule: Schedule) -> None:
        """Add or replace schedule."""
        schedule_type = type(schedule).__name__
        self.schedules[schedule_type] = schedule
    
    def get_schedule(self, schedule_type: Type[T]) -> T | None:
        """Type-safe schedule retrieval."""
        return self.schedules.get(schedule_type.__name__)
    
    def remove_schedule(self, schedule_type: Type[Schedule]) -> None:
        """Remove schedule."""
        schedule_name = schedule_type.__name__
        if schedule_name in self.schedules:
            del self.schedules[schedule_name]
    
    def compute_gti(self) -> Decimal:
        """Compute Gross Total Income from all income schedules."""
        income_schedule_types = [
            "ScheduleSalary", "ScheduleHP", 
            "ScheduleCG", "ScheduleOS", "ScheduleBP"
        ]
        
        total = Decimal("0")
        for sched_type in income_schedule_types:
            if sched_type in self.schedules:
                total += self.schedules[sched_type].compute_income()
        
        return total
    
    def validate_all(self) -> List[ValidationError]:
        """Validate all schedules."""
        all_errors = []
        for schedule in self.schedules.values():
            all_errors.extend(schedule.validate())
        return all_errors
    
    def mark_imported(self) -> None:
        """Transition to IMPORTED state."""
        if self.status.value == FilingStatus.DRAFT:
            self.status = FilingStatus(FilingStatus.IMPORTED)
    
    def mark_computed(self) -> None:
        """Transition to COMPUTED state."""
        if self.status.value in (FilingStatus.IMPORTED, FilingStatus.DRAFT):
            self.status = FilingStatus(FilingStatus.COMPUTED)
