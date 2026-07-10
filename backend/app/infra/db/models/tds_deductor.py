"""TDS deductor entries from Form 26AS."""
from __future__ import annotations
import uuid
from datetime import datetime
from sqlalchemy import String, DateTime, func
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column
from app.infra.db.base import Base


class TDSDeductor(Base):
    __tablename__ = "tds_deductors"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    client_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False, index=True)
    ay: Mapped[str] = mapped_column(String(7), nullable=False, index=True)
    deductor_tan: Mapped[str] = mapped_column(String(10), nullable=False, index=True)
    deductor_name: Mapped[str] = mapped_column(String(255), nullable=False)
    section_code: Mapped[str] = mapped_column(String(10), nullable=False)  # 192, 194, etc.
    amount_paid: Mapped[int] = mapped_column(default=0, nullable=False)  # in rupees
    tax_deducted: Mapped[int] = mapped_column(default=0, nullable=False)  # in rupees
    tax_deposited: Mapped[int] = mapped_column(default=0, nullable=False)
    quarter: Mapped[str] = mapped_column(String(5), nullable=True)
    receipt_no: Mapped[str | None] = mapped_column(String(50), nullable=True)
    extra_data: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
