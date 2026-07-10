"""Initial schema - users, clients, filings, snapshots, audit.

Revision ID: 0001_init
Revises:
Create Date: 2026-07-07
"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision: str = "0001_init"
down_revision: Union[str, None] = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto")

    op.create_table(
        "users",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("email", sa.String(255), nullable=False, unique=True),
        sa.Column("password_hash", sa.String(255), nullable=False),
        sa.Column("full_name", sa.String(255), nullable=False),
        sa.Column("role", sa.String(20), nullable=False, server_default="staff"),
        sa.Column("is_active", sa.Boolean, nullable=False, server_default=sa.true()),
        sa.Column("mfa_secret", sa.String(64), nullable=True),
        sa.Column("last_login", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "clients",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("pan", sa.String(10), nullable=False, index=True),
        sa.Column("name", sa.String(255), nullable=False),
        sa.Column("dob", sa.Date, nullable=True),
        sa.Column("father_name", sa.String(255), nullable=True),
        sa.Column("mobile", sa.String(15), nullable=True),
        sa.Column("email", sa.String(255), nullable=True),
        sa.Column("address", sa.String(500), nullable=True),
        sa.Column("aadhaar_masked", sa.String(14), nullable=True),
        sa.Column("category", sa.String(20), nullable=False, server_default="individual"),
        sa.Column("tags", postgresql.JSONB, nullable=True),
        sa.Column("family_group_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("extra_data", postgresql.JSONB, nullable=True),
        sa.Column("assigned_user_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "itr_filings",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("client_id", postgresql.UUID(as_uuid=True), nullable=False, index=True),
        sa.Column("ay", sa.String(7), nullable=False, index=True),
        sa.Column("itr_form", sa.String(10), nullable=False),
        sa.Column("regime", sa.String(10), nullable=False),
        sa.Column("status", sa.String(30), nullable=False, server_default="draft"),
        sa.Column("json_hash", sa.String(64), nullable=True),
        sa.Column("json_path", sa.String(500), nullable=True),
        sa.Column("pdf_path", sa.String(500), nullable=True),
        sa.Column("filed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("ack_no", sa.String(20), nullable=True),
        sa.Column("extra_data", postgresql.JSONB, nullable=True),
        sa.Column("created_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
        sa.UniqueConstraint("client_id", "ay", name="uq_filing_client_ay"),
    )

    op.create_table(
        "computed_return_snapshots",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("client_id", postgresql.UUID(as_uuid=True), nullable=False, index=True),
        sa.Column("pan", sa.String(10), nullable=False, index=True),
        sa.Column("ay", sa.String(7), nullable=False, index=True),
        sa.Column("itr_form", sa.String(10), nullable=False),
        sa.Column("regime", sa.String(10), nullable=False),
        sa.Column("rule_version", sa.String(20), nullable=False),
        sa.Column("payload", postgresql.JSONB, nullable=False),
        sa.Column("snapshot_hash", sa.String(64), nullable=False, unique=True),
        sa.Column("is_locked", sa.Boolean, nullable=False, server_default=sa.true()),
        sa.Column("filed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("ack_no", sa.String(20), nullable=True),
        sa.Column("created_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "itr_form_data",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("filing_id", postgresql.UUID(as_uuid=True), nullable=False, index=True),
        sa.Column("head_type", sa.String(30), nullable=False),
        sa.Column("payload", postgresql.JSONB, nullable=False),
        sa.Column("version", sa.Integer, nullable=False, server_default="1"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "tds_deductors",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("client_id", postgresql.UUID(as_uuid=True), nullable=False, index=True),
        sa.Column("ay", sa.String(7), nullable=False, index=True),
        sa.Column("deductor_tan", sa.String(10), nullable=False, index=True),
        sa.Column("deductor_name", sa.String(255), nullable=False),
        sa.Column("section_code", sa.String(10), nullable=False),
        sa.Column("amount_paid", sa.Integer, nullable=False, server_default="0"),
        sa.Column("tax_deducted", sa.Integer, nullable=False, server_default="0"),
        sa.Column("tax_deposited", sa.Integer, nullable=False, server_default="0"),
        sa.Column("quarter", sa.String(5), nullable=True),
        sa.Column("receipt_no", sa.String(50), nullable=True),
        sa.Column("extra_data", postgresql.JSONB, nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "form_26as_data",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("client_id", postgresql.UUID(as_uuid=True), nullable=False, index=True),
        sa.Column("ay", sa.String(7), nullable=False, index=True),
        sa.Column("pan", sa.String(10), nullable=False),
        sa.Column("raw_json", postgresql.JSONB, nullable=False),
        sa.Column("parsed_summary", postgresql.JSONB, nullable=True),
        sa.Column("imported_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("imported_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "ais_data",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("client_id", postgresql.UUID(as_uuid=True), nullable=False, index=True),
        sa.Column("ay", sa.String(7), nullable=False, index=True),
        sa.Column("pan", sa.String(10), nullable=False),
        sa.Column("source", sa.String(50), nullable=False),
        sa.Column("raw_json", postgresql.JSONB, nullable=False),
        sa.Column("parsed_summary", postgresql.JSONB, nullable=True),
        sa.Column("imported_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("imported_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "tis_data",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("client_id", postgresql.UUID(as_uuid=True), nullable=False, index=True),
        sa.Column("ay", sa.String(7), nullable=False, index=True),
        sa.Column("pan", sa.String(10), nullable=False),
        sa.Column("raw_json", postgresql.JSONB, nullable=False),
        sa.Column("parsed_summary", postgresql.JSONB, nullable=True),
        sa.Column("imported_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("imported_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "loss_ledger",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("client_id", postgresql.UUID(as_uuid=True), nullable=False, index=True),
        sa.Column("pan", sa.String(10), nullable=False, index=True),
        sa.Column("head", sa.String(30), nullable=False),
        sa.Column("source_ay", sa.String(7), nullable=False),
        sa.Column("original_amount", sa.Integer, nullable=False, server_default="0"),
        sa.Column("set_off_against", sa.Integer, nullable=False, server_default="0"),
        sa.Column("cf_years_remaining", sa.Integer, nullable=False, server_default="8"),
        sa.Column("is_active", sa.Boolean, nullable=False, server_default=sa.true()),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "audit_trail",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("action", sa.String(50), nullable=False, index=True),
        sa.Column("entity", sa.String(50), nullable=False, index=True),
        sa.Column("entity_id", sa.String(100), nullable=True),
        sa.Column("before", postgresql.JSONB, nullable=True),
        sa.Column("after", postgresql.JSONB, nullable=True),
        sa.Column("ip_address", postgresql.INET, nullable=True),
        sa.Column("user_agent", sa.Text, nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()"), index=True),
    )

    op.create_table(
        "documents",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("client_id", postgresql.UUID(as_uuid=True), nullable=True, index=True),
        sa.Column("filing_id", postgresql.UUID(as_uuid=True), nullable=True, index=True),
        sa.Column("name", sa.String(255), nullable=False),
        sa.Column("doc_type", sa.String(50), nullable=False),
        sa.Column("storage_path", sa.String(500), nullable=False),
        sa.Column("size_bytes", sa.BigInteger, nullable=False, server_default="0"),
        sa.Column("sha256", sa.String(64), nullable=False),
        sa.Column("uploaded_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("uploaded_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "bulk_import_jobs",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("filename", sa.String(255), nullable=False),
        sa.Column("total_rows", sa.Integer, nullable=False, server_default="0"),
        sa.Column("success_count", sa.Integer, nullable=False, server_default="0"),
        sa.Column("failed_count", sa.Integer, nullable=False, server_default="0"),
        sa.Column("errors", postgresql.JSONB, nullable=True),
        sa.Column("status", sa.String(20), nullable=False, server_default="pending"),
        sa.Column("committed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )

    op.create_table(
        "backup_records",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("filename", sa.String(255), nullable=False, unique=True),
        sa.Column("storage_path", sa.String(500), nullable=False),
        sa.Column("size_bytes", sa.BigInteger, nullable=False, server_default="0"),
        sa.Column("sha256", sa.String(64), nullable=False),
        sa.Column("backup_type", sa.String(20), nullable=False, server_default="manual"),
        sa.Column("target", sa.String(50), nullable=False, server_default="local"),
        sa.Column("created_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )


def downgrade() -> None:
    for tbl in [
        "backup_records", "bulk_import_jobs", "documents", "audit_trail",
        "loss_ledger", "tis_data", "ais_data", "form_26as_data",
        "tds_deductors", "itr_form_data", "computed_return_snapshots",
        "itr_filings", "clients", "users",
    ]:
        op.drop_table(tbl)
