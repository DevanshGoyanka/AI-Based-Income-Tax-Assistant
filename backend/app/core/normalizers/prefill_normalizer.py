"""Prefill JSON to canonical normalizer."""
from decimal import Decimal
from typing import Dict, List

from app.core.domain.canonical_models import (
    CanonicalPersonalInfo,
    CanonicalBankAccount,
    CanonicalAdvanceTax,
    CanonicalSalary,
    CanonicalHouseProperty,
    CanonicalInterest,
    CanonicalDividend,
    CanonicalTDS,
    CanonicalIncome,
)


class PrefillNormalizer:
    """Normalize raw prefill JSON to canonical income models."""
    
    def normalize(self, raw_data: Dict) -> CanonicalIncome:
        """Transform raw prefill JSON to canonical income."""
        return CanonicalIncome(
            personal_info=self._extract_personal_info(raw_data),
            bank_accounts=self._extract_bank_accounts(raw_data),
            advance_tax=self._extract_advance_tax(raw_data),
            salaries=self._extract_salaries(raw_data),
            house_properties=self._extract_house_properties(raw_data),
            interest_income=self._extract_interest(raw_data),
            dividend_income=self._extract_dividends(raw_data),
            tds_entries=self._extract_tds(raw_data),
            other_income=self._extract_other_income(raw_data),
        )
    
    def _extract_salaries(self, data: Dict) -> List[CanonicalSalary]:
        """Extract salary income from insights, form26as, or form24q."""
        salaries = []
        
        # Priority 1: form24q (most detailed and present in our fixture)
        form24q = data.get("form24q", {})
        form24q_salaries = form24q.get("salaries", {}).get("salary", [])
        
        if form24q_salaries:
            for sal in form24q_salaries:
                # Handle nested employerOrDeductorOrCollectDetl structure
                employer_details = sal.get("employerOrDeductorOrCollectDetl", {})
                employer_name = employer_details.get("name", "")
                employer_tan = employer_details.get("tanOfEmployer", "")
                
                # Handle nested incomeDeductions structure
                inc_ded = sal.get("incomeDeductions", {})
                gross = self._to_decimal(inc_ded.get("salary", 0))
                exempt = self._to_decimal(inc_ded.get("allowances", 0))
                std_ded = self._to_decimal(inc_ded.get("deductionUs16Ia", 0))
                prof_tax = self._to_decimal(inc_ded.get("professionalTax", 0))
                
                # Get TDS from form26as
                tds = self._get_tds_for_employer_from_form26as(data, employer_tan)
                
                if employer_name:  # Only add if we have employer info
                    salaries.append(CanonicalSalary(
                        employer_name=employer_name,
                        employer_tan=employer_tan,
                        gross_salary=gross,
                        allowances_exempt=exempt,
                        professional_tax=prof_tax,
                        tds_deducted=tds,
                        standard_deduction=std_ded,
                    ))
            return salaries
        
        # Priority 2: insights.salaries.salary (aggregated data fallback)
        insights = data.get("insights", {})
        insight_salaries = insights.get("salaries", {}).get("salary", [])
        
        if insight_salaries:
            for sal in insight_salaries:
                employer_name = sal.get("nameOfEmployer", "")
                employer_tan = sal.get("tanOfEmployer", "")
                
                salarys_section = sal.get("salarys", {})
                gross = self._to_decimal(salarys_section.get("salary", 0))
                perquisites = self._to_decimal(salarys_section.get("valueOfPerquisites", 0))
                profits = self._to_decimal(salarys_section.get("profitsinLieuOfSalary", 0))
                
                # Get TDS from form26as for this employer
                tds = self._get_tds_for_employer_from_form26as(data, employer_tan)
                
                salaries.append(CanonicalSalary(
                    employer_name=employer_name,
                    employer_tan=employer_tan,
                    gross_salary=gross + perquisites + profits,
                    allowances_exempt=Decimal("0"),
                    professional_tax=Decimal("0"),
                    tds_deducted=tds,
                    standard_deduction=Decimal("0"),
                ))
            return salaries
        
        # Priority 2: form26as.tdsOnSalaries (fallback)
        form26as = data.get("form26as", {})
        form26as_salaries = form26as.get("tdsOnSalaries", {}).get("tdsOnSalary", [])
        
        if form26as_salaries:
            for sal_tds in form26as_salaries:
                employer_name = sal_tds.get("employerOrDeductorOrCollectDetl", {}).get(
                    "employerOrDeductorOrCollecterName", ""
                )
                employer_tan = sal_tds.get("employerOrDeductorOrCollectDetl", {}).get("tan", "")
                
                gross = self._to_decimal(sal_tds.get("incChrgSal", 0))
                tds = self._to_decimal(sal_tds.get("totalTDSSal", 0))
                
                salaries.append(CanonicalSalary(
                    employer_name=employer_name,
                    employer_tan=employer_tan,
                    gross_salary=gross,
                    allowances_exempt=Decimal("0"),
                    professional_tax=Decimal("0"),
                    tds_deducted=tds,
                    standard_deduction=Decimal("0"),
                ))
            return salaries
        
        # Priority 3: form24q (most detailed but not always present)
        form24q = data.get("form24q", {})
        form24q_salaries = form24q.get("salaries", {}).get("salary", [])
        
        if form24q_salaries:
            for sal in form24q_salaries:
                # Handle both nested employerOrDeductorOrCollectDetl and flat structure
                employer_details = sal.get("employerOrDeductorOrCollectDetl", {})
                employer_name = employer_details.get("name", sal.get("nameOfEmployer", ""))
                employer_tan = employer_details.get("tanOfEmployer", sal.get("tanOfEmployer", ""))
                
                # Handle both nested incomeDeductions and flat salary fields
                inc_ded = sal.get("incomeDeductions", sal)
                gross = self._to_decimal(inc_ded.get("salary", 0))
                exempt = self._to_decimal(inc_ded.get("allowances", 0))
                std_ded = self._to_decimal(inc_ded.get("deductionUs16Ia", 0))
                prof_tax = self._to_decimal(inc_ded.get("professionalTax", 0))
                
                # Calculate TDS from form26as for this employer
                tds = self._get_tds_for_employer_from_form26as(data, employer_tan)
                
                if employer_name:  # Only add if we have employer info
                    salaries.append(CanonicalSalary(
                        employer_name=employer_name,
                        employer_tan=employer_tan,
                        gross_salary=gross,
                        allowances_exempt=exempt,
                        professional_tax=prof_tax,
                        tds_deducted=tds,
                        standard_deduction=std_ded,
                    ))
        
        return salaries
    
    def _get_tds_for_employer_from_form26as(self, data: Dict, employer_tan: str) -> Decimal:
        """Get TDS amount for a specific employer TAN from form26as."""
        form26as = data.get("form26as", {})
        tds_salaries = form26as.get("tdsOnSalaries", {}).get("tdsOnSalary", [])
        
        for entry in tds_salaries:
            tan = entry.get("tanOfDeductor", "")
            if tan == employer_tan:
                return self._to_decimal(entry.get("totalTaxDeducted", 0))
        
        return Decimal("0")
    
    def _extract_house_properties(self, data: Dict) -> List[CanonicalHouseProperty]:
        """Extract house property details."""
        properties = []
        
        # From scheduleHP if present
        schedule_hp = data.get("scheduleHP", {})
        for prop in schedule_hp.get("houseProperties", {}).get("houseProperty", []):
            properties.append(CanonicalHouseProperty(
                address=prop.get("address", {}).get("addressDetail", ""),
                ownership_share=self._to_decimal(prop.get("ownedPercentage", 100)) / 100,
                annual_rent=self._to_decimal(prop.get("rentReceived", 0)),
                municipal_taxes=self._to_decimal(prop.get("taxPaidToLocalAuth", 0)),
                interest_paid=self._to_decimal(prop.get("intOnBorwCap", 0)),
                co_owners=[],
            ))
        
        return properties
    
    def _extract_interest(self, data: Dict) -> List[CanonicalInterest]:
        """Extract interest income from insights or scheduleOS."""
        interest_list = []
        
        # From insights (aggregated)
        insights = data.get("insights", {})
        savings_int = self._to_decimal(insights.get("intrstFrmSavingBank", 0))
        deposit_int = self._to_decimal(insights.get("intrstFrmTermDeposit", 0))
        
        if savings_int > 0:
            interest_list.append(CanonicalInterest(
                bank_name="Savings Bank",
                account_number="",
                interest_amount=savings_int,
            ))
        
        if deposit_int > 0:
            interest_list.append(CanonicalInterest(
                bank_name="Term Deposit",
                account_number="",
                interest_amount=deposit_int,
            ))
        
        return interest_list
    
    def _extract_dividends(self, data: Dict) -> List[CanonicalDividend]:
        """Extract dividend income."""
        dividends = []
        
        # From insights
        insights = data.get("insights", {})
        schedule_os = insights.get("scheduleOS", {})
        inc_other = schedule_os.get("incOthThanOwnRaceHorse", {})
        div_gross = self._to_decimal(inc_other.get("dividendGross", 0))
        
        if div_gross > 0:
            dividends.append(CanonicalDividend(
                company_name="Dividend Income",
                dividend_amount=div_gross,
            ))
        
        return dividends
    
    def _extract_tds(self, data: Dict) -> List[CanonicalTDS]:
        """Extract TDS entries from form26as."""
        tds_list = []
        
        form26as = data.get("form26as", {})
        
        # TDS on salary
        for sal_tds in form26as.get("tdsOnSalaries", {}).get("tdsOnSalary", []):
            tds_list.append(CanonicalTDS(
                deductor_name=sal_tds.get("deductorName", ""),
                deductor_tan=sal_tds.get("tanOfDeductor", ""),
                section_code="192",
                amount_paid=self._to_decimal(sal_tds.get("amountPaid", 0)),
                tax_deducted=self._to_decimal(sal_tds.get("totalTaxDeducted", 0)),
                tax_deposited=self._to_decimal(sal_tds.get("totalTaxDeposited", 0)),
                quarter="",
            ))
        
        # TDS on other than salary
        for other_tds in form26as.get("tdsOnOthThanSals", {}).get("tdSonOthThanSal", []):
            tds_list.append(CanonicalTDS(
                deductor_name=other_tds.get("deductorName", ""),
                deductor_tan=other_tds.get("tanOfDeductor", ""),
                section_code=other_tds.get("section", ""),
                amount_paid=self._to_decimal(other_tds.get("amountPaid", 0)),
                tax_deducted=self._to_decimal(other_tds.get("totalTaxDeducted", 0)),
                tax_deposited=self._to_decimal(other_tds.get("totalTaxDeposited", 0)),
                quarter=other_tds.get("quarter", ""),
            ))
        
        return tds_list
    
    def _extract_other_income(self, data: Dict) -> Decimal:
        """Extract other miscellaneous income."""
        insights = data.get("insights", {})
        schedule_os = insights.get("scheduleOS", {})
        inc_other = schedule_os.get("incOthThanOwnRaceHorse", {})
        
        # Sum up other income sources
        other_sources = self._to_decimal(inc_other.get("otherGross", 0))
        
        return other_sources
    
    def _extract_personal_info(self, data: Dict) -> CanonicalPersonalInfo:
        """Extract personal information."""
        personal = data.get("personalInfo", {})
        assessee_name = personal.get("assesseeName", {})
        address = personal.get("address", {})
        
        first_name = assessee_name.get("firstName", "")
        middle_name = assessee_name.get("middleName", "")
        surname = assessee_name.get("surNameOrOrgName", "")
        full_name = f"{first_name} {middle_name} {surname}".strip()
        
        addr_parts = [
            address.get("residenceName", ""),
            address.get("residenceNo", ""),
            address.get("roadOrStreet", ""),
            address.get("localityOrArea", ""),
            address.get("cityOrTownOrDistrict", ""),
        ]
        full_address = ", ".join(p for p in addr_parts if p)
        
        return CanonicalPersonalInfo(
            pan=personal.get("pan", ""),
            name=full_name,
            father_name=personal.get("fatherName", ""),
            dob=personal.get("dob", ""),
            aadhaar=personal.get("aadhaarCardNo", ""),
            email=address.get("emailAddress", ""),
            mobile=str(address.get("mobileNo", "")),
            address=full_address,
            pincode=str(address.get("pinCode", "")),
            state_code=address.get("stateCode", ""),
        )
    
    def _extract_bank_accounts(self, data: Dict) -> List[CanonicalBankAccount]:
        """Extract bank account details."""
        accounts = []
        bank_dtls = data.get("bankAccountDtls", [])
        
        for bank_group in bank_dtls:
            addtnl_banks = bank_group.get("addtnlBankDetails", [])
            for bank in addtnl_banks:
                accounts.append(CanonicalBankAccount(
                    bank_name=bank.get("bankName", ""),
                    account_number=bank.get("bankAccountNo", ""),
                    ifsc_code=bank.get("ifsccode", ""),
                    account_type=bank.get("AccountType", ""),
                    use_for_refund=bank.get("useForRefund", "false").lower() == "true",
                ))
        
        return accounts
    
    def _extract_advance_tax(self, data: Dict) -> List[CanonicalAdvanceTax]:
        """Extract advance tax payments."""
        payments = []
        form26as = data.get("form26as", {})
        tax_payments = form26as.get("taxPayments", {}).get("taxPayment", [])
        
        for payment in tax_payments:
            bank_branch = payment.get("nameOfBankAndBranch", {})
            
            payments.append(CanonicalAdvanceTax(
                bsr_code=payment.get("bsrCode", ""),
                challan_serial=str(payment.get("srlNoOfChaln", "")),
                date_paid=payment.get("dateDep", ""),
                amount=self._to_decimal(payment.get("amt", 0)),
                bank_name=bank_branch.get("nameOfBank", ""),
                branch_name=bank_branch.get("nameOfBranch", ""),
            ))
        
        return payments
    
    def _to_decimal(self, value) -> Decimal:
        """Convert value to Decimal."""
        if isinstance(value, (int, float)):
            return Decimal(str(value))
        if isinstance(value, str):
            try:
                return Decimal(value.replace(",", ""))
            except:
                return Decimal("0")
        return Decimal("0")
