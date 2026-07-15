"""add_foreign_keys_and_constraints

Revision ID: aa8526dc8b3d
Revises: 9976106a1337
Create Date: 2026-07-10 17:29:35.937148

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'aa8526dc8b3d'
down_revision: Union[str, None] = '9976106a1337'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add foreign key constraints for client_id references
    op.create_foreign_key(
        'fk_form_26as_data_client_id',
        'form_26as_data', 'clients',
        ['client_id'], ['id'],
        ondelete='CASCADE'
    )
    
    op.create_foreign_key(
        'fk_ais_data_client_id',
        'ais_data', 'clients',
        ['client_id'], ['id'],
        ondelete='CASCADE'
    )
    
    op.create_foreign_key(
        'fk_tds_records_client_id',
        'tds_records', 'clients',
        ['client_id'], ['id'],
        ondelete='CASCADE'
    )
    
    # Add foreign key constraints for imported_by references (users table)
    op.create_foreign_key(
        'fk_form_26as_data_imported_by',
        'form_26as_data', 'users',
        ['imported_by'], ['id'],
        ondelete='SET NULL'
    )
    
    op.create_foreign_key(
        'fk_ais_data_imported_by',
        'ais_data', 'users',
        ['imported_by'], ['id'],
        ondelete='SET NULL'
    )
    
    op.create_foreign_key(
        'fk_tds_records_imported_by',
        'tds_records', 'users',
        ['imported_by'], ['id'],
        ondelete='SET NULL'
    )


def downgrade() -> None:
    # Drop foreign keys in reverse order
    op.drop_constraint('fk_tds_records_imported_by', 'tds_records', type_='foreignkey')
    op.drop_constraint('fk_ais_data_imported_by', 'ais_data', type_='foreignkey')
    op.drop_constraint('fk_form_26as_data_imported_by', 'form_26as_data', type_='foreignkey')
    
    op.drop_constraint('fk_tds_records_client_id', 'tds_records', type_='foreignkey')
    op.drop_constraint('fk_ais_data_client_id', 'ais_data', type_='foreignkey')
    op.drop_constraint('fk_form_26as_data_client_id', 'form_26as_data', type_='foreignkey')
