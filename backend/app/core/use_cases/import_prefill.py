"""Import prefill use case."""
from typing import BinaryIO
from uuid import UUID

from app.core.interfaces.filing_repository import IFilingRepository
from app.core.normalizers.prefill_normalizer import PrefillNormalizer
from app.core.normalizers.canonical_mapper import CanonicalToScheduleMapper
from app.core.domain.filing import Filing, FilingStatus
from app.core.domain.value_objects import AssessmentYear, TaxRegime


class ImportPrefillUseCase:
    """Use case for importing prefill JSON."""
    
    def __init__(
        self,
        filing_repo: IFilingRepository,
        normalizer: PrefillNormalizer,
        mapper: CanonicalToScheduleMapper,
    ):
        self.filing_repo = filing_repo
        self.normalizer = normalizer
        self.mapper = mapper
    
    async def execute(
        self,
        client_id: UUID,
        raw_data: dict,
        ay: str,
    ) -> dict:
        """Import prefill JSON and update filing.
        
        Flow:
        1. Parse raw JSON to canonical models
        2. Map canonical models to Schedule entities
        3. Load or create Filing aggregate
        4. Merge schedules into Filing
        5. Persist Filing
        """
        # Step 1: Normalize raw data
        canonical_income = self.normalizer.normalize(raw_data)
        
        # Step 2: Load or create filing
        ay_obj = AssessmentYear(ay)
        filing = await self.filing_repo.get_by_client_and_ay(client_id, ay_obj)
        
        if not filing:
            from uuid import uuid4
            filing = Filing(
                id=uuid4(),
                client_id=client_id,
                ay=ay_obj,
                regime=TaxRegime("new"),
                status=FilingStatus(FilingStatus.DRAFT),
                schedules={},
            )
        
        # Step 3: Map canonical to schedules
        schedules = self.mapper.map_to_schedules(
            canonical_income,
            filing.id,
            ay,
        )
        
        # Step 4: Merge schedules into filing
        for schedule in schedules:
            filing.add_schedule(schedule)
        
        # Step 5: Mark as imported and persist
        filing.mark_imported()
        await self.filing_repo.save(filing)
        
        # Return summary
        return {
            "imported": 1,
            "skipped": 0,
            "errors": [],
            "extracted": {
                "assessment_year": ay,
                "schedules_created": [type(s).__name__ for s in schedules],
                "gti": float(filing.compute_gti()),
            }
        }
