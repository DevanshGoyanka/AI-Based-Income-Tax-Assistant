"""ITR filing record per (client, AY)."""
from __future__ import annotations
import uuid
from datetime import datetime
from sqlalchemy import String, DateTime, func, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column
from app.infra.db.base import Base


class ITRFiling(Base):
    __tablename__ = "itr_filings"
    __table_args__ = (UniqueConstraint("client_id", "ay", name="uq_filing_client_ay"),)

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    client_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False, index=True)
    ay: Mapped[str] = mapped_column(String(7), nullable=False, index=True)  # "2026-27"
    itr_form: Mapped[str] = mapped_column(String(10), nullable=False)
    regime: Mapped[str] = mapped_column(String(10), nullable=False)
    status: Mapped[str] = mapped_column(String(30), default="draft", nullable=False)
    # draft | computed | exported | filed | acknowledged
    json_hash: Mapped[str | None] = mapped_column(String(64), nullable=True)
    json_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    pdf_path: Mapped[str | None] = mapped_column(String(500), nullable=True)
    filed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    ack_no: Mapped[str | None] = mapped_column(String(20), nullable=True)
    extra_data: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)
    created_by: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), nullable=True)
