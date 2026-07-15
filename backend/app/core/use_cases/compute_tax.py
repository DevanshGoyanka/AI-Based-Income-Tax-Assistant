"""Compute tax use case."""
from datetime import date
from typing import Optional
from uuid import UUID

from app.core.domain.computed_return import ComputedReturn
from app.core.domain.filing import FilingStatus
from app.core.interfaces.filing_repository import IFilingRepository
from app.core.interfaces.client_repository import IClientRepository
from app.core.interfaces.tax_engine import ITaxEngine


class ComputeTaxUseCase:
    """Compute tax for a filing using our owned TaxEngine.

    DO NOT call OpenTax at runtime. Use ITaxEngine (TaxEngineAdapter) which
    delegates to our owned app.core.services.tax_engine.TaxEngine.
    """

    def __init__(
        self,
        filing_repo: IFilingRepository,
        client_repo: IClientRepository,
        tax_engine: ITaxEngine,
    ):
        self.filing_repo = filing_repo
        self.client_repo = client_repo
        self.tax_engine = tax_engine

    async def execute(
        self,
        filing_id: UUID,
        regime: Optional[str] = None,
        filing_date: Optional[date] = None,
    ) -> ComputedReturn:
        """Compute tax for filing.

        Args:
            filing_id: Filing ID.
            regime: Tax regime override (optional). Defaults to filing's regime.
            filing_date: Date of filing (for 234A/234F interest calculation).
                         Defaults to today.

        Returns:
            ComputedReturn with tax computation results.

        Raises:
            ValueError: If filing not found.
        """
        filing = await self.filing_repo.get_by_id(filing_id)
        if not filing:
            raise ValueError(f"Filing {filing_id} not found")

        # Resolve regime
        tax_regime = regime or filing.regime.value

        # Fetch client for DOB/PAN
        dob: Optional[date] = None
        pan = ""
        client = await self.client_repo.get_by_id(filing.client_id)
        if client:
            if client.dob:
                try:
                    dob = date.fromisoformat(client.dob)
                except (ValueError, TypeError):
                    pass
            pan = client.pan

        # Extract schedules from filing
        schedules = list(filing.schedules.values())

        # Compute tax via owned TaxEngine (via TaxEngineAdapter → ITaxEngine)
        computed = await self.tax_engine.compute_tax(
            schedules=schedules,
            regime=tax_regime,
            ay=filing.ay.value,
            dob=dob,
            pan=pan,
            filing_id=filing.id,
            client_id=filing.client_id,
            filing_date=filing_date,
        )

        # Update filing status
        filing.status = FilingStatus(FilingStatus.COMPUTED)
        await self.filing_repo.save(filing)

        return computed
