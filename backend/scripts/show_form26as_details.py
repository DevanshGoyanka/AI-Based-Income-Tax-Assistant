"""Show extracted Form 26AS details from database for any client."""
import asyncio
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import select
from app.infra.db.base import AsyncSessionLocal
from app.infra.db.models import Client
from app.infra.db.models.form26as_data import Form26ASData


def fmt(value, default="N/A"):
    if value is None or value == "":
        return default
    if isinstance(value, (int, float)):
        return f"Rs.{value:,}"
    return str(value)


def show_one(f26_index, total, client, form26as):
    print('=' * 80)
    if total > 1:
        print(f'FORM 26AS RECORD {f26_index} of {total}')
    else:
        print('FORM 26AS DATA')
    print('=' * 80)
    print(f'Client: {client.name} ({client.pan})')
    print(f'Assessment Year: {form26as.ay}')
    print(f'Imported: {form26as.imported_at}')
    print()

    if not form26as.raw_json:
        print('   (No raw JSON stored)')
        return

    raw = form26as.raw_json if isinstance(form26as.raw_json, dict) else json.loads(form26as.raw_json)

    # Header
    print('-' * 80)
    print('HEADER')
    print('-' * 80)
    header = raw.get('header', {})
    for k, v in header.items():
        print(f'   {k}: {v}')
    print()

    # Part I - TDS Deductors
    print('-' * 80)
    print('PART-I: TDS DEDUCTORS')
    print('-' * 80)
    part1 = raw.get('part1_tds', [])
    if part1:
        for idx, ded in enumerate(part1, 1):
            print(f'\nDeductor {idx}:')
            print(f'   TAN: {ded.get("tan")}')
            print(f'   Name: {ded.get("name")}')
            print(f'   Total Amount: {fmt(ded.get("total_amount"), 0)}')
            print(f'   Total TDS: {fmt(ded.get("total_tds"), 0)}')
            print(f'   Total TDS Deposited: {fmt(ded.get("total_tds_deposited"), 0)}')

            transactions = ded.get('transactions', [])
            if transactions:
                print(f'\n   Transactions: {len(transactions)} entries')
                for t_idx, txn in enumerate(transactions, 1):
                    print(f'      {t_idx}. Date: {txn.get("date")}, Section: {txn.get("section")}, '
                          f'Amount: {fmt(txn.get("amount"), 0)}, TDS: {fmt(txn.get("tds"), 0)}')
    else:
        print('   No TDS deductors')
    print()

    # Part VI - TCS
    print('-' * 80)
    print('PART-VI: TCS (Tax Collected at Source)')
    print('-' * 80)
    part6 = raw.get('part6_tcs', [])
    if part6:
        for idx, tcs in enumerate(part6, 1):
            print(f'\nCollector {idx}:')
            print(f'   TAN: {tcs.get("tan")}')
            print(f'   Name: {tcs.get("name")}')
            print(f'   Amount: {fmt(tcs.get("amount"), 0)}')
            print(f'   TCS: {fmt(tcs.get("tcs"), 0)}')
    else:
        print('   No TCS entries')
    print()

    # Part VII - Refunds
    print('-' * 80)
    print('PART-VII: REFUNDS')
    print('-' * 80)
    part7 = raw.get('part7_refunds', [])
    if part7:
        for idx, ref in enumerate(part7, 1):
            print(f'\nRefund {idx}:')
            for k, v in ref.items():
                print(f'   {k}: {v}')
    else:
        print('   No refunds')
    print()


async def show_details(pan: str):
    async with AsyncSessionLocal() as db:
        result = await db.execute(
            select(Client).where(Client.pan == pan).order_by(Client.created_at.desc())
        )
        all_clients = result.scalars().all()
        if not all_clients:
            print(f'No client found with PAN {pan}')
            return
        client = next((c for c in all_clients if c.name and not c.name.startswith("Test ")), all_clients[0])

        result = await db.execute(
            select(Form26ASData)
            .where(Form26ASData.client_id == client.id)
            .order_by(Form26ASData.imported_at.desc())
        )
        form26as_list = result.scalars().all()

        if not form26as_list:
            print(f'No Form 26AS data found for client {pan}')
            return

        for i, form26as in enumerate(form26as_list, 1):
            show_one(i, len(form26as_list), client, form26as)

        print('=' * 80)
        print('END OF FORM 26AS DETAILS')
        print('=' * 80)


if __name__ == "__main__":
    pan = sys.argv[1].upper() if len(sys.argv) > 1 else "ACUPG3482G"
    asyncio.run(show_details(pan))
