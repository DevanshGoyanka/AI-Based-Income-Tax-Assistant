"""add missing foreign keys

Revision ID: 0005_add_missing_foreign_keys
Revises: 0004_fix_prefill
Create Date: 2026-07-14

Per audit: Missing FKs on itr_filings, clients, computed_return_snapshots,
documents, itr_form_data, prefill_data.

Existing migration aa8526dc8b3d only covered form_26as_data, ais_data, tds_records.
This migration adds the remaining constraints.
"""
from typing import Sequence, Union

from alembic import op


# revision identifiers, used by Alembic.
revision: str = '0005_add_missing_foreign_keys'
down_revision: Union[str, None] = '0004_fix_prefill'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # itr_filings FKs
    op.create_foreign_key(
        'fk_itr_filings_client_id',
        'itr_filings', 'clients',
        ['client_id'], ['id'],
        ondelete='CASCADE'
    )
    op.create_foreign_key(
        'fk_itr_filings_created_by',
        'itr_filings', 'users',
        ['created_by'], ['id'],
        ondelete='SET NULL'
    )

    # clients FKs
    op.create_foreign_key(
        'fk_clients_assigned_user_id',
        'clients', 'users',
        ['assigned_user_id'], ['id'],
        ondelete='SET NULL'
    )
    op.create_foreign_key(
        'fk_clients_created_by',
        'clients', 'users',
        ['created_by'], ['id'],
        ondelete='SET NULL'
    )

    # computed_return_snapshots FKs
    op.create_foreign_key(
        'fk_computed_return_snapshots_client_id',
        'computed_return_snapshots', 'clients',
        ['client_id'], ['id'],
        ondelete='CASCADE'
    )
    op.create_foreign_key(
        'fk_computed_return_snapshots_created_by',
        'computed_return_snapshots', 'users',
        ['created_by'], ['id'],
        ondelete='SET NULL'
    )

    # itr_form_data FKs
    op.create_foreign_key(
        'fk_itr_form_data_filing_id',
        'itr_form_data', 'itr_filings',
        ['filing_id'], ['id'],
        ondelete='CASCADE'
    )
    op.create_foreign_key(
        'fk_itr_form_data_created_by',
        'itr_form_data', 'users',
        ['created_by'], ['id'],
        ondelete='SET NULL'
    )

    # documents FKs
    op.create_foreign_key(
        'fk_documents_client_id',
        'documents', 'clients',
        ['client_id'], ['id'],
        ondelete='CASCADE'
    )
    op.create_foreign_key(
        'fk_documents_filing_id',
        'documents', 'itr_filings',
        ['filing_id'], ['id'],
        ondelete='SET NULL'
    )
    op.create_foreign_key(
        'fk_documents_uploaded_by',
        'documents', 'users',
        ['uploaded_by'], ['id'],
        ondelete='SET NULL'
    )

    # prefill_data FK
    op.create_foreign_key(
        'fk_prefill_data_client_id',
        'prefill_data', 'clients',
        ['client_id'], ['id'],
        ondelete='CASCADE'
    )


def downgrade() -> None:
    # Drop in reverse order
    op.drop_constraint('fk_prefill_data_client_id', 'prefill_data', type_='foreignkey')

    op.drop_constraint('fk_documents_uploaded_by', 'documents', type_='foreignkey')
    op.drop_constraint('fk_documents_filing_id', 'documents', type_='foreignkey')
    op.drop_constraint('fk_documents_client_id', 'documents', type_='foreignkey')

    op.drop_constraint('fk_itr_form_data_created_by', 'itr_form_data', type_='foreignkey')
    op.drop_constraint('fk_itr_form_data_filing_id', 'itr_form_data', type_='foreignkey')

    op.drop_constraint('fk_computed_return_snapshots_created_by', 'computed_return_snapshots', type_='foreignkey')
    op.drop_constraint('fk_computed_return_snapshots_client_id', 'computed_return_snapshots', type_='foreignkey')

    op.drop_constraint('fk_clients_created_by', 'clients', type_='foreignkey')
    op.drop_constraint('fk_clients_assigned_user_id', 'clients', type_='foreignkey')

    op.drop_constraint('fk_itr_filings_created_by', 'itr_filings', type_='foreignkey')
    op.drop_constraint('fk_itr_filings_client_id', 'itr_filings', type_='foreignkey')
