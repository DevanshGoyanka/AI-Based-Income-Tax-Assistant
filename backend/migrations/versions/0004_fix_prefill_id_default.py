"""fix prefill data id default

Revision ID: 0004_fix_prefill
Revises: aa8526dc8b3d
Create Date: 2026-07-14

Per audit: prefill_data.id has no server_default, causing errors if None is passed.
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '0004_fix_prefill'
down_revision: Union[str, None] = 'aa8526dc8b3d'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Step 1: Remove any existing server_default (idempotent if none exists)
    op.alter_column(
        'prefill_data', 'id',
        server_default=None,
        existing_type=sa.UUID(),
        existing_nullable=False
    )
    # Step 2: Add gen_random_uuid() as server_default
    op.alter_column(
        'prefill_data', 'id',
        server_default=sa.text('gen_random_uuid()'),
        existing_type=sa.UUID(),
        existing_nullable=False
    )


def downgrade() -> None:
    # Remove server_default (revert to application-generated only)
    op.alter_column(
        'prefill_data', 'id',
        existing_type=sa.UUID(),
        nullable=False,
        server_default=None
    )
