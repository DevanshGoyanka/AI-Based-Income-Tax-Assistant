"""Loss ledger for CFL/BFLA tracking."""
from __future__ import annotations
import uuid
from datetime import datetime
from sqlalchemy import String, Integer, DateTime, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from app.infra.db.base import Base


class LossLedger(Base):
    __tablename__ = "loss_ledger"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    client_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False, index=True)
    pan: Mapped[str] = mapped_column(String(10), nullable=False, index=True)
    head: Mapped[str] = mapped_column(String(30), nullable=False)
    # house_property | business | speculation | capital_gain_st | capital_gain_lt | vda | unspecified
    source_ay: Mapped[str] = mapped_column(String(7), nullable=False)
    original_amount: Mapped[int] = mapped_column(default=0, nullable=False)  # in rupees (negative)
    set_off_against: Mapped[int] = mapped_column(default=0, nullable=False)  # in rupees
    cf_years_remaining: Mapped[int] = mapped_column(default=8, nullable=False)
    is_active: Mapped[bool] = mapped_column(default=True, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)
