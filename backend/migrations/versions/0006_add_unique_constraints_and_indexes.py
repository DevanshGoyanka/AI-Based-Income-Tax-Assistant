"""add unique constraints and composite indexes

Revision ID: 0006_add_unique_constraints_and_indexes
Revises: 0005_add_missing_foreign_keys
Create Date: 2026-07-14

Per audit report:
- Missing unique constraints to prevent duplicate imports
- Missing composite indexes for common query patterns
"""
from typing import Sequence, Union

from alembic import op


# revision identifiers, used by Alembic.
revision: str = '0006_add_unique_constraints_and_indexes'
down_revision: Union[str, None] = '0005_add_missing_foreign_keys'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # ---- Unique Constraints ----
    # Prevent duplicate imports for the same client+ay
    op.create_unique_constraint(
        'uq_form_26as_data_client_ay',
        'form_26as_data',
        ['client_id', 'ay']
    )
    op.create_unique_constraint(
        'uq_ais_data_client_ay_source',
        'ais_data',
        ['client_id', 'ay', 'source']
    )
    op.create_unique_constraint(
        'uq_tis_data_client_ay',
        'tis_data',
        ['client_id', 'ay']
    )
    op.create_unique_constraint(
        'uq_prefill_data_client_ay',
        'prefill_data',
        ['client_id', 'ay']
    )
    op.create_unique_constraint(
        'uq_loss_ledger_client_head_source_ay',
        'loss_ledger',
        ['client_id', 'head', 'source_ay']
    )
    op.create_unique_constraint(
        'uq_itr_form_data_filing_head_type',
        'itr_form_data',
        ['filing_id', 'head_type']
    )

    # ---- Composite Indexes ----
    # Snapshot lookup by client+AY (common dashboard query)
    op.create_index(
        'ix_snapshots_client_ay',
        'computed_return_snapshots',
        ['client_id', 'ay']
    )
    # Audit trail entity-scoped queries
    op.create_index(
        'ix_audit_trail_entity_entity_id',
        'audit_trail',
        ['entity', 'entity_id']
    )
    # TDS lookup by client+AY
    op.create_index(
        'ix_tds_deductors_client_ay',
        'tds_deductors',
        ['client_id', 'ay']
    )


def downgrade() -> None:
    # Drop indexes in reverse order
    op.drop_index('ix_tds_deductors_client_ay', table_name='tds_deductors')
    op.drop_index('ix_audit_trail_entity_entity_id', table_name='audit_trail')
    op.drop_index('ix_snapshots_client_ay', table_name='computed_return_snapshots')

    # Drop unique constraints in reverse order
    op.drop_constraint('uq_itr_form_data_filing_head_type', 'itr_form_data', type_='unique')
    op.drop_constraint('uq_loss_ledger_client_head_source_ay', 'loss_ledger', type_='unique')
    op.drop_constraint('uq_prefill_data_client_ay', 'prefill_data', type_='unique')
    op.drop_constraint('uq_tis_data_client_ay', 'tis_data', type_='unique')
    op.drop_constraint('uq_ais_data_client_ay_source', 'ais_data', type_='unique')
    op.drop_constraint('uq_form_26as_data_client_ay', 'form_26as_data', type_='unique')
