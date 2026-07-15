"""SQLAlchemy filing repository implementation."""
from typing import List, Optional
from uuid import UUID
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
import json

from app.core.interfaces.filing_repository import IFilingRepository
from app.core.domain.filing import Filing, FilingStatus
from app.core.domain.value_objects import AssessmentYear, TaxRegime
from app.core.domain.schedules.base import Schedule
from app.infra.db.models.itr_filing import ITRFiling
from app.infra.db.models.itr_form_data import ITRFormData


class SQLAlchemyFilingRepository(IFilingRepository):
    """SQLAlchemy implementation of filing repository."""
    
    def __init__(self, session: AsyncSession):
        self.session = session
    
    async def get_by_id(self, filing_id: UUID) -> Optional[Filing]:
        """Get filing by ID."""
        stmt = select(ITRFiling).where(ITRFiling.id == filing_id)
        result = await self.session.execute(stmt)
        model = result.scalar_one_or_none()
        
        if not model:
            return None
        
        return await self._to_entity(model)
    
    async def get_by_client_and_ay(
        self, client_id: UUID, ay: AssessmentYear
    ) -> Optional[Filing]:
        """Get filing by client and assessment year."""
        stmt = select(ITRFiling).where(
            ITRFiling.client_id == client_id,
            ITRFiling.ay == ay.value
        )
        result = await self.session.execute(stmt)
        model = result.scalar_one_or_none()
        
        if not model:
            return None
        
        return await self._to_entity(model)
    
    async def save(self, filing: Filing) -> None:
        """Save filing (create or update)."""
        stmt = select(ITRFiling).where(ITRFiling.id == filing.id)
        result = await self.session.execute(stmt)
        model = result.scalar_one_or_none()
        
        if model:
            # Update existing
            model.status = filing.status.value
            model.regime = filing.regime.value
        else:
            # Create new
            model = ITRFiling(
                id=filing.id,
                client_id=filing.client_id,
                ay=filing.ay.value,
                regime=filing.regime.value,
                status=filing.status.value,
            )
            self.session.add(model)
        
        # Save schedules to itr_form_data
        await self._save_schedules(filing)
        
        await self.session.commit()
    
    async def delete(self, filing_id: UUID) -> None:
        """Delete filing."""
        stmt = select(ITRFiling).where(ITRFiling.id == filing_id)
        result = await self.session.execute(stmt)
        model = result.scalar_one_or_none()
        
        if model:
            await self.session.delete(model)
            await self.session.commit()
    
    async def list_by_client(self, client_id: UUID) -> List[Filing]:
        """List all filings for a client."""
        stmt = select(ITRFiling).where(ITRFiling.client_id == client_id)
        result = await self.session.execute(stmt)
        models = result.scalars().all()
        
        filings = []
        for model in models:
            filing = await self._to_entity(model)
            filings.append(filing)
        
        return filings
    
    async def _to_entity(self, model: ITRFiling) -> Filing:
        """Convert SQLAlchemy model to domain entity."""
        filing = Filing(
            id=model.id,
            client_id=model.client_id,
            ay=AssessmentYear(model.ay),
            regime=TaxRegime(model.regime),
            status=FilingStatus(model.status),
            schedules={},
        )
        
        # Load schedules from itr_form_data
        await self._load_schedules(filing)
        
        return filing
    
    async def _save_schedules(self, filing: Filing) -> None:
        """Save schedules to itr_form_data table."""
        for schedule_type, schedule in filing.schedules.items():
            # Serialize schedule to JSON
            schedule_data = self._serialize_schedule(schedule)
            
            # Check if exists
            stmt = select(ITRFormData).where(
                ITRFormData.filing_id == filing.id,
                ITRFormData.head_type == schedule_type
            )
            result = await self.session.execute(stmt)
            form_data = result.scalar_one_or_none()
            
            if form_data:
                form_data.payload = schedule_data
                form_data.version += 1
            else:
                form_data = ITRFormData(
                    filing_id=filing.id,
                    head_type=schedule_type,
                    payload=schedule_data,
                    version=1,
                )
                self.session.add(form_data)
    
    async def _load_schedules(self, filing: Filing) -> None:
        """Load schedules from itr_form_data table."""
        stmt = select(ITRFormData).where(ITRFormData.filing_id == filing.id)
        result = await self.session.execute(stmt)
        form_data_list = result.scalars().all()
        
        for form_data in form_data_list:
            schedule = self._deserialize_schedule(
                form_data.head_type,
                form_data.payload,
                filing.id,
                filing.ay.value,
            )
            if schedule:
                filing.add_schedule(schedule)
    
    def _serialize_schedule(self, schedule: Schedule) -> dict:
        """Serialize schedule to JSON-compatible dict."""
        # Convert schedule to dict for storage
        import dataclasses
        return {
            "id": str(schedule.id),
            "filing_id": str(schedule.filing_id),
            "ay": schedule.ay,
            "data": schedule.to_itr_json(),
        }
    
    def _deserialize_schedule(
        self, schedule_type: str, payload: dict, filing_id: UUID, ay: str
    ) -> Optional[Schedule]:
        """Deserialize schedule from JSON."""
        # TODO: Implement proper deserialization based on schedule type
        # For now, return None to avoid errors
        return None
