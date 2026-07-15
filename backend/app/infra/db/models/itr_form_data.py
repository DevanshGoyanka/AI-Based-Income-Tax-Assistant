"""ITR form data - stores schedule payloads."""
from __future__ import annotations
import uuid
from datetime import datetime
from sqlalchemy import String, DateTime, Integer, func, UniqueConstraint, ForeignKey
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column
from app.infra.db.base import Base


class ITRFormData(Base):
    __tablename__ = "itr_form_data"
    __table_args__ = (
        UniqueConstraint("filing_id", "head_type", name="uq_form_data_filing_head"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    filing_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), 
        ForeignKey("itr_filings.id", ondelete="CASCADE"),
        nullable=False, 
        index=True
    )
    head_type: Mapped[str] = mapped_column(String(30), nullable=False)
    payload: Mapped[dict] = mapped_column(JSONB, nullable=False)
    version: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)
