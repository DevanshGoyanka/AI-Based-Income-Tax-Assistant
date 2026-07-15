"""OpenTax tax engine adapter - compatibility layer."""
from typing import List
from uuid import UUID

from app.core.domain.schedules.base import Schedule
from app.core.domain.schedules.salary import ScheduleSalary
from app.core.domain.schedules.schedule_via import ScheduleVIA
from app.core.domain.value_objects import TaxRegime, AssessmentYear
from app.core.domain.computed_return import ComputedReturn
from app.core.interfaces.tax_engine import ITaxEngine
from app.adapters.opentax.vendor.filing.tax_calculation.tax_calculation_service import TaxCalculationService
from .model_mapper import ScheduleToOpenTaxMapper
from .response_mapper import OpenTaxToERPMapper


class OpenTaxAdapter(ITaxEngine):
    """OpenTax compatibility layer.
    
    OpenTax is ONE implementation of ITaxEngine (not the core).
    Can be swapped for CustomTaxEngine or CommercialTaxEngine.
    """
    
    def __init__(self):
        self.tax_service = TaxCalculationService()
        self.mapper = ScheduleToOpenTaxMapper()
        self.response_mapper = OpenTaxToERPMapper()
    
    async def compute_tax(
        self,
        schedules: List[Schedule],
        regime: TaxRegime,
        ay: AssessmentYear
    ) -> ComputedReturn:
        """Compute tax using OpenTax engine.
        
        IMPORTANT: OpenTax never accesses ERP database or models.
        All data passed as pure schedules.
        """
        # Map ERP Schedules to OpenTax format
        filing_dict = self.mapper.map_schedules(schedules, regime.value, ay.value)
        
        # Call vendored OpenTax (async method)
        result = await self.tax_service.calculate(filing_dict)
        
        # Map OpenTax result back to ERP ComputedReturn
        # Extract filing_id from first schedule
        filing_id = schedules[0].filing_id if schedules else None
        
        computed = self.response_mapper.map_computation(
            filing_id=filing_id,
            client_id=None,  # TODO: pass client_id
            pan="",  # TODO: pass from filing
            ay=ay.value,
            itr_form="ITR1",
            opentax_result=result,
            regime=regime.value
        )
        
        return computed
    
    def explain_computation(
        self,
        computed: ComputedReturn
    ) -> List[dict]:
        """Generate computation explanation for CA review."""
        return computed.explanation or []
