"""Show ALL extracted details from prefill_data table for any client."""
import asyncio
import json
import sys
from pathlib import Path

# Ensure backend root is on sys.path so `app.*` imports work
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import select
from app.infra.db.base import AsyncSessionLocal
from app.infra.db.models import Client, PrefillData


def fmt(value, default="N/A"):
    if value is None or value == "":
        return default
    if isinstance(value, (int, float)):
        return f"Rs.{value:,}"
    return str(value)


async def show_details(pan: str):
    async with AsyncSessionLocal() as db:
        result = await db.execute(
            select(Client).where(Client.pan == pan).order_by(Client.created_at.desc())
        )
        all_clients = result.scalars().all()
        if not all_clients:
            print(f'No client found with PAN {pan}')
            return
        # Prefer clients with real names (not "Test Client")
        client = next((c for c in all_clients if c.name and not c.name.startswith("Test ")), all_clients[0])

        result = await db.execute(select(PrefillData).where(PrefillData.client_id == client.id))
        prefill = result.scalar_one_or_none()
        if not prefill:
            print(f'No prefill data for client {pan}')
            return

        raw = prefill.raw_json if isinstance(prefill.raw_json, dict) else json.loads(prefill.raw_json)

        # 1. CLIENT
        print('=' * 80)
        print('1. CLIENT TABLE (clients)')
        print('=' * 80)
        print(f'   ID: {client.id}')
        print(f'   PAN: {client.pan}')
        print(f'   Name: {client.name}')
        print(f'   DOB: {client.dob}')
        print(f'   Mobile: {client.mobile}')
        print(f'   Email: {client.email}')
        print(f'   Created: {client.created_at}')
        print()

        # 2. PREFILL META
        print('=' * 80)
        print('2. PREFILL META (prefill_data)')
        print('=' * 80)
        print(f'   ID: {prefill.id}')
        print(f'   AY: {prefill.ay}')
        print(f'   Uploaded: {prefill.uploaded_at}')
        print()

        # 3. PERSONAL INFO
        print('=' * 80)
        print('3. PERSONAL INFO (raw_json.personalInfo)')
        print('=' * 80)
        pi = raw.get('personalInfo', {})
        addr = pi.get('address', {}) or {}
        name_parts = pi.get('assesseeName', {})
        # Handle both 'surName' and 'surNameOrOrgName' key formats
        last_name = name_parts.get('surName') or name_parts.get('surNameOrOrgName') or ''
        full_name = ' '.join(filter(None, [
            name_parts.get('firstName', ''),
            name_parts.get('middleName', ''),
            last_name
        ]))
        print(f'   PAN: {pi.get("pan")}')
        print(f'   First Name: {name_parts.get("firstName")}')
        print(f'   Middle Name: {name_parts.get("middleName")}')
        print(f'   Last Name: {last_name}')
        print(f'   Full Name: {full_name}')
        print(f'   DOB: {pi.get("dob")}')
        print(f'   Aadhaar: {pi.get("aadhaarCardNo")}')
        print(f'   Status: {pi.get("status")}')
        print(f'   Father Name: {pi.get("fatherName")}')
        print()
        print(f'   Address:')
        print(f'      Residence: {addr.get("residenceName")}')
        print(f'      Flat/House: {addr.get("residenceNo")}')
        print(f'      Road: {addr.get("roadOrStreet")}')
        print(f'      Locality: {addr.get("localityOrArea")}')
        print(f'      City: {addr.get("cityOrTownOrDistrict")}')
        print(f'      State Code: {addr.get("stateCode")}')
        print(f'      Pincode: {addr.get("pincode") or addr.get("pinCode") or addr.get("zipCode")}')
        print(f'      Country: {addr.get("countryCode")}')
        print(f'   Mobile: {addr.get("mobileNo")}')
        print(f'   Email: {addr.get("emailAddress")}')
        print()

        # 4. FILING STATUS
        print('=' * 80)
        print('4. FILING STATUS (raw_json.filingStatus)')
        print('=' * 80)
        fs = raw.get('filingStatus', {}) or {}
        for k, v in fs.items():
            print(f'   {k}: {v}')
        print()

        # 5. VERIFICATION
        print('=' * 80)
        print('5. VERIFICATION (raw_json.verification)')
        print('=' * 80)
        vrf = raw.get('verification', {}) or {}
        print(f'   Capacity: {vrf.get("capacity")}')
        decl = vrf.get('declaration', {}) or {}
        print(f'   Assessee Ver Name: {decl.get("assesseeVerName")}')
        print(f'   Assessee Ver PAN: {decl.get("assesseeVerPAN")}')
        print(f'   Father Name: {decl.get("fatherName")}')
        print()

        # 6. FORM 26AS - TDS ON OTHER THAN SALARY
        print('=' * 80)
        print('6. TDS ON OTHER THAN SALARY (form26as.tdsOnOthThanSals)')
        print('=' * 80)
        f26 = raw.get('form26as', {}) or {}
        tds_oth = f26.get('tdsOnOthThanSals', {}).get('tdSonOthThanSal', [])
        if tds_oth:
            for idx, tds in enumerate(tds_oth, 1):
                print(f'\n   Entry {idx}:')
                deductor = tds.get('employerOrDeductorOrCollectDetl', {}) or {}
                print(f'      Deductor TAN: {deductor.get("tan")}')
                print(f'      Deductor Name: {deductor.get("employerOrDeductorOrCollecterName")}')
                print(f'      Section Code: {tds.get("sectionCode")}')
                print(f'      Gross Amount: {fmt(tds.get("grossAmount"), 0)}')
                print(f'      Head of Income: {tds.get("headOfIncome")}')
                tax_dtls = tds.get('taxDeductCreditDtls', {}) or {}
                print(f'      TDS Deducted: {fmt(tax_dtls.get("taxDeductedOwnHands"), 0)}')
                print(f'      TDS Claimed: {fmt(tax_dtls.get("taxClaimedOwnHands"), 0)}')
        else:
            print('   No entries')
        print()

        # 7. FORM 26AS - TDS ON SALARY
        print('=' * 80)
        print('7. TDS ON SALARY (form26as.tdsOnSalaries)')
        print('=' * 80)
        tds_sal = f26.get('tdsOnSalaries', {}).get('tdsOnSalary', [])
        if tds_sal:
            for idx, sal in enumerate(tds_sal, 1):
                print(f'\n   Entry {idx}:')
                emp = sal.get('employerOrDeductorOrCollectDetl', {}) or {}
                print(f'      Employer TAN: {emp.get("tan")}')
                print(f'      Employer Name: {emp.get("employerOrDeductorOrCollecterName")}')
                print(f'      Total TDS Salary: {fmt(sal.get("totalTDSSal"), 0)}')
                print(f'      Income Charged: {fmt(sal.get("incChrgSal"), 0)}')
                tax_dtls = sal.get('taxDeductCreditDtls', {}) or {}
                print(f'      TDS Deducted: {fmt(tax_dtls.get("taxDeductedOwnHands"), 0)}')
                print(f'      TDS Claimed: {fmt(tax_dtls.get("taxClaimedOwnHands"), 0)}')
        else:
            print('   No entries')
        print()

        # 8. FORM 26AS - TAX PAYMENTS
        print('=' * 80)
        print('8. TAX PAYMENTS (form26as.taxPayments)')
        print('=' * 80)
        payments = f26.get('taxPayments', {}).get('taxPayment', [])
        if payments:
            for idx, pay in enumerate(payments, 1):
                print(f'\n   Payment {idx}:')
                bank = pay.get('nameOfBankAndBranch', {}) or {}
                print(f'      Date: {pay.get("dateDep")}')
                print(f'      BSR Code: {pay.get("bsrCode")}')
                print(f'      Bank: {bank.get("nameOfBank")}')
                print(f'      Branch: {bank.get("nameOfBranch")}')
                print(f'      Amount: {fmt(pay.get("amt"), 0)}')
                print(f'      Receipt: {pay.get("receiptNumber")}')
                print(f'      Challan Serial: {pay.get("srlNoOfChaln")}')
        else:
            print('   No entries')
        print()

        # 9. FORM 26AS - INTEREST/OTHER INCOME
        print('=' * 80)
        print('9. INTEREST/OTHER INCOME (form26as.incomeDeductionsOthersInc)')
        print('=' * 80)
        inc_list = f26.get('incomeDeductionsOthersInc', [])
        if inc_list:
            nature_map = {
                'SAV': 'Savings Account Interest',
                'IFD': 'Term/Fixed Deposit Interest',
                'DIV': 'Dividend Income',
                'TAX': 'Tax Refund',
            }
            for inc in inc_list:
                nature = inc.get('othSrcNatureDesc')
                amt = inc.get('othSrcOthAmount', 0)
                print(f'   {nature_map.get(nature, nature)}: {fmt(amt, 0)}')
        else:
            print('   No entries')
        print(f'   Term Deposit Interest (separate): {fmt(f26.get("intrstFrmTermDeposit"), 0)}')
        print()

        # 10. FORM 24Q - SALARY DETAILS
        print('=' * 80)
        print('10. FORM 24Q - SALARY DETAILS (form24q)')
        print('=' * 80)
        f24 = raw.get('form24q', {}) or {}
        salaries = f24.get('salaries', {}).get('salary', []) if f24 else []
        if salaries:
            for idx, s in enumerate(salaries, 1):
                print(f'\n   Employer {idx}:')
                print(f'      Name: {s.get("nameOfEmployer")}')
                print(f'      TAN: {s.get("tanOfEmployer")}')
                addr24 = s.get('addressDetail', {}) or {}
                print(f'      Address: {addr24.get("addDetail")}')
                print(f'      City: {addr24.get("cityOrTownOrDistrict")}')
                print(f'      State: {addr24.get("stateCode")}')
                print(f'      Pincode: {addr24.get("pinCode")}')
                sd = s.get('salarys', {}) or {}
                print(f'      Salary: {fmt(sd.get("salary"), 0)}')
                print(f'      Perquisites: {fmt(sd.get("valueOfPerquisites"), 0)}')
                print(f'      Profits in Lieu: {fmt(sd.get("profitsinLieuOfSalary"), 0)}')
        else:
            print('   No salary details')
        inc_ded = f24.get('incomeDeductions', {}) if f24 else {}
        if inc_ded:
            print(f'\n   Income Deductions:')
            print(f'      Salary: {fmt(inc_ded.get("salary"), 0)}')
            print(f'      Perquisites: {fmt(inc_ded.get("perquisitesValue"), 0)}')
            print(f'      Profits in Salary: {fmt(inc_ded.get("profitsInSalary"), 0)}')
            print(f'      Std Deduction (16ia): {fmt(inc_ded.get("deductionUs16Ia"), 0)}')
            print(f'      Prof Tax (16iii): {fmt(inc_ded.get("professionalTaxUs16Iii"), 0)}')
            print(f'      Entertainment (16ii): {fmt(inc_ded.get("entertainmentAlw16Ii"), 0)}')
        ch6a = f24.get('usrDeductUndChapVIAType', {}) if f24 else {}
        if ch6a:
            print(f'\n   Chapter VI-A Deductions:')
            for k, v in ch6a.items():
                if v:
                    print(f'      {k}: {fmt(v, 0)}')
        print(f'   Savings Bank Interest: {fmt(f24.get("intrstFrmSavingBank"), 0)}')
        print(f'   Total 10(13A) Exempt: {fmt(f24.get("TotalAllwncExemptUs10"), 0)}')
        print()

        # 11. INSIGHTS
        print('=' * 80)
        print('11. INSIGHTS (raw_json.insights)')
        print('=' * 80)
        ins = raw.get('insights', {}) or {}
        print(f'   Savings Interest: {fmt(ins.get("intrstFrmSavingBank"), 0)}')
        print(f'   Term Deposit Interest: {fmt(ins.get("intrstFrmTermDeposit"), 0)}')
        ins_salaries = ins.get('salaries', {}).get('salary', []) if ins else []
        if ins_salaries:
            print(f'\n   Employer Salary Records:')
            for idx, s in enumerate(ins_salaries, 1):
                print(f'      {idx}. {s.get("nameOfEmployer")} (TAN: {s.get("tanOfEmployer")})')
                sd = s.get('salarys', {}) or {}
                print(f'         Salary: {fmt(sd.get("salary"), 0)}')
                print(f'         Perquisites: {fmt(sd.get("valueOfPerquisites"), 0)}')
        cum_sal = ins.get('cumulativeSalary', {}) or {}
        if cum_sal:
            print(f'\n   Cumulative Salary:')
            print(f'      Salary: {fmt(cum_sal.get("salary"), 0)}')
            print(f'      Perquisites: {fmt(cum_sal.get("perquisitesValue"), 0)}')
            print(f'      Profits: {fmt(cum_sal.get("profitsInSalary"), 0)}')
        print()

        # 12. HOUSE PROPERTY
        print('=' * 80)
        print('12. HOUSE PROPERTY (lastFiledITR.scheduleHP)')
        print('=' * 80)
        lf = raw.get('lastFiledITR', {}) or {}
        sch_hp = lf.get('scheduleHP', {}) or {}
        hp_props = sch_hp.get('propertyDetails', [])
        if hp_props:
            for idx, p in enumerate(hp_props, 1):
                print(f'\n   Property {idx}:')
                for k, v in p.items():
                    if v:
                        print(f'      {k}: {v if not isinstance(v, (int, float)) else fmt(v, 0)}')
        else:
            print('   No house property')
        print()

        # 13. CAPITAL GAINS / TCS
        print('=' * 80)
        print('13. CAPITAL GAINS / TCS (lastFiledITR.scheduleTCS)')
        print('=' * 80)
        sch_tcs = lf.get('scheduleTCS', {}) or {}
        tcs_list = sch_tcs.get('tcs', [])
        if tcs_list:
            for idx, t in enumerate(tcs_list, 1):
                print(f'\n   TCS Entry {idx}:')
                for k, v in t.items():
                    if v:
                        print(f'      {k}: {v if not isinstance(v, (int, float)) else fmt(v, 0)}')
        else:
            print('   No TCS entries')
        print()

        # 14. OTHER SOURCE INCOME
        print('=' * 80)
        print('14. OTHER SOURCES INCOME')
        print('=' * 80)
        os_inc = raw.get('otherSourceIncome', {}) or {}
        if os_inc:
            print(json.dumps(os_inc, indent=4, default=str)[:1000])
        else:
            print('   No other source income')
        print()

        # 15. CARRY FORWARD LOSSES
        print('=' * 80)
        print('15. CARRY FORWARD LOSSES (scheduleCFL)')
        print('=' * 80)
        cfl = raw.get('scheduleCFL', {}) or {}
        cfl_list = cfl.get('CarryFwdLossDetail', [])
        if cfl_list:
            for idx, c in enumerate(cfl_list, 1):
                print(f'\n   AY {c.get("AssessmentYear")}:')
                print(f'      Filing Date: {c.get("DateOfFiling")}')
                print(f'      HP Loss CF: {fmt(c.get("HpLossCF"), 0)}')
                print(f'      STCG Loss CF: {fmt(c.get("StcgLossCF"), 0)}')
                print(f'      LTCG Loss CF: {fmt(c.get("LtcgLossCF"), 0)}')
                print(f'      Race Horse Loss: {fmt(c.get("OthSrcLossRaceHorseCF"), 0)}')
        else:
            print('   No carry forward losses')
        print()

        # 16. BANK ACCOUNTS
        print('=' * 80)
        print('16. BANK ACCOUNTS (bankAccountDtls)')
        print('=' * 80)
        bank_data = raw.get('bankAccountDtls', {})
        if isinstance(bank_data, list):
            accounts = []
            for item in bank_data:
                if isinstance(item, dict):
                    accounts.extend(item.get('addtnlBankDetails', []))
        else:
            accounts = bank_data.get('addtnlBankDetails', [])
        if accounts:
            for idx, acc in enumerate(accounts, 1):
                print(f'\n   Account {idx}:')
                print(f'      IFSC: {acc.get("ifsccode")}')
                print(f'      Account Number: {acc.get("bankAccountNo")}')
                print(f'      Bank Name: {acc.get("bankName")}')
                print(f'      Account Type: {acc.get("AccountType")}')
                print(f'      Use for Refund: {acc.get("useForRefund")}')
        else:
            print('   No bank accounts')
        print()

        # 17. CHAPTER VI-A DEDUCTIONS
        print('=' * 80)
        print('17. CHAPTER VI-A DEDUCTIONS')
        print('=' * 80)
        # Check insights.UsrDeductUndChapVIAType (e.g. Section80TTB with caps)
        ch6a_insights = ins.get('UsrDeductUndChapVIAType', {}) or {}
        # Check form24q.usrDeductUndChapVIAType (e.g. section80C lowercase)
        ch6a_24q = f24.get('usrDeductUndChapVIAType', {}) if f24 else {}
        merged = {}
        for src_dict in (ch6a_insights, ch6a_24q):
            for k, v in src_dict.items():
                if v:
                    merged[k] = v

        if merged:
            # Section mapping for readability
            section_names = {
                'Section80C': '80C', 'section80C': '80C',
                'Section80CCC': '80CCC', 'section80CCC': '80CCC',
                'Section80CCDEmployeeOrSE': '80CCD (Employee)', 'section80CCDEmployeeOrSE': '80CCD (Employee)',
                'Section80CCD1B': '80CCD1B', 'section80CCD1B': '80CCD1B',
                'Section80CCDEmployer': '80CCD (Employer)', 'section80CCDEmployer': '80CCD (Employer)',
                'Section80D': '80D', 'section80D': '80D',
                'Section80DD': '80DD', 'section80DD': '80DD',
                'Section80DDB': '80DDB', 'section80DDB': '80DDB',
                'Section80E': '80E', 'section80E': '80E',
                'Section80EE': '80EE', 'section80EE': '80EE',
                'Section80EEA': '80EEA', 'section80EEA': '80EEA',
                'Section80EEB': '80EEB', 'section80EEB': '80EEB',
                'Section80G': '80G', 'section80G': '80G',
                'Section80GG': '80GG', 'section80GG': '80GG',
                'Section80GGA': '80GGA', 'section80GGA': '80GGA',
                'Section80GGC': '80GGC', 'section80GGC': '80GGC',
                'Section80TTA': '80TTA', 'section80TTA': '80TTA',
                'Section80TTB': '80TTB', 'section80TTB': '80TTB',
                'Section80U': '80U', 'section80U': '80U',
            }
            for k, v in merged.items():
                display = section_names.get(k, k)
                print(f'   {display}: {fmt(v, 0)}')
        else:
            print('   No Chapter VI-A deductions')
        print()

        # 18. SCHEDULES
        print('=' * 80)
        print('18. SCHEDULES')
        print('=' * 80)
        for sched_name in ['Schedule80G', 'Schedule80RA', 'scheduleEI', 'ScheduleESOP', 'scheduleAL']:
            if sched_name in raw and raw[sched_name]:
                sval = json.dumps(raw[sched_name], default=str)
                print(f'   {sched_name}: {sval[:200]}...' if len(sval) > 200 else f'   {sched_name}: {sval}')
        print()

        # 19. TAX REGIME OPTION
        print('=' * 80)
        print('19. TAX REGIME OPTION (form10IF)')
        print('=' * 80)
        f10if = raw.get('form10IF', {}) or {}
        print(f'   New Tax Regime: {f10if.get("newTaxRegime")}')
        print()

        # 20. ALL TOP-LEVEL KEYS
        print('=' * 80)
        print('20. ALL TOP-LEVEL KEYS IN PREFILL JSON')
        print('=' * 80)
        for key in sorted(raw.keys()):
            val = raw[key]
            if val is None:
                status = "null"
            elif isinstance(val, dict):
                status = f"dict ({len(val)} keys)"
            elif isinstance(val, list):
                status = f"list ({len(val)} items)"
            elif isinstance(val, str):
                status = f"string ({len(val)} chars)"
            elif isinstance(val, (int, float)):
                status = f"number ({val})"
            elif isinstance(val, bool):
                status = f"bool ({val})"
            else:
                status = str(type(val).__name__)
            print(f'   {key:35s} {status}')
        print()

        print('=' * 80)
        print('END OF EXTRACTED DETAILS')
        print('=' * 80)


if __name__ == "__main__":
    pan = sys.argv[1].upper() if len(sys.argv) > 1 else "ACUPG3482G"
    asyncio.run(show_details(pan))
