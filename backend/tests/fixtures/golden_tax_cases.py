"""Golden test cases - AY 2026-27 (Finance Act 2025).

NEW REGIME: 0-4L(0%), 4-8L(5%), 8-12L(10%), 12-16L(15%), 16-20L(20%), 20-24L(25%), 24L+(30%)
OLD REGIME: 0-2.5L(0%), 2.5-5L(5%), 5-10L(20%), 10L+(30%)
Std Deduction: New(75K), Old(50K) | Rebate 87A: New(25K@7L), Old(12.5K@5L)
"""

CASE_1_NEW_REGIME_15L = {
    "name": "Salaried individual, new regime, ₹15L",
    "input": {
        "ay": "2026-27", "regime": "new",
        "salary": {"gross": 1500000, "employer_name": "Tech Corp", "employer_tan": "BANG12345A"},
        "tds": 150000,
    },
    "expected": {
        "gross_total_income": 1425000,  # 15L - 75K std ded
        "total_deductions": 0,
        "total_income": 1425000,
        # 4-8L: 4L×5%=20K, 8-12L: 4L×10%=40K, 12-14.25L: 2.25L×15%=33.75K = 93.75K
        "tax_before_rebate": 93750,
        "rebate_87a": 0,
        "surcharge": 0,
        "health_education_cess": 3750,  # 93.75K × 4%
        "total_tax_liability": 97500,
        "refund": 52500,
    }
}

CASE_2_OLD_REGIME_15L_DEDUCTIONS = {
    "name": "Salaried individual, old regime, ₹15L, ₹1.5L deductions",
    "input": {
        "ay": "2026-27", "regime": "old",
        "salary": {"gross": 1500000},
        "deductions": {"section_80c": 150000},
        "tds": 150000,
    },
    "expected": {
        "gross_total_income": 1450000,  # 15L - 50K std ded
        "total_deductions": 150000,
        "total_income": 1300000,  # 14.5L - 1.5L
        # 2.5-5L: 2.5L×5%=12.5K, 5-10L: 5L×20%=100K, 10-13L: 3L×30%=90K = 202.5K
        "tax_before_rebate": 202500,
        "rebate_87a": 0,
        "surcharge": 0,
        "health_education_cess": 8100,
        "total_tax_liability": 210600,
        "tax_payable": 60600,
    }
}

CASE_3_REBATE_87A_NEW = {
    "name": "Low income, new regime, rebate 87A",
    "input": {
        "ay": "2026-27", "regime": "new",
        "salary": {"gross": 650000},
        "tds": 15000,
    },
    "expected": {
        "gross_total_income": 575000,  # 6.5L - 75K
        "total_income": 575000,
        "tax_before_rebate": 8750,  # 4-5.75L: 1.75L×5%
        "rebate_87a": 8750,  # Full rebate (≤7L)
        # Note: Small rounding may give ~350 due to cess on 0
        "total_tax_liability": 0,
        "refund": 15000,
    }
}

CASE_4_HIGH_INCOME_SURCHARGE = {
    "name": "High income, ₹60L, surcharge 10%",
    "input": {
        "ay": "2026-27", "regime": "new",
        "salary": {"gross": 6000000},
        "tds": 1200000,
    },
    "expected": {
        "gross_total_income": 5925000,
        "total_income": 5925000,
        # 4-8L:20K, 8-12L:40K, 12-16L:60K, 16-20L:80K, 20-24L:100K, 24-59.25L:1057.5K = 1357.5K
        "tax_before_rebate": 1357500,
        "rebate_87a": 0,
        "surcharge": 135750,  # 10%
        "health_education_cess": 59730,  # (1357.5K+135.75K)×4%
        "total_tax_liability": 1552980,
        "tax_payable": 352980,
    }
}

GOLDEN_TEST_CASES = [
    CASE_1_NEW_REGIME_15L,
    CASE_2_OLD_REGIME_15L_DEDUCTIONS,
    CASE_3_REBATE_87A_NEW,
    CASE_4_HIGH_INCOME_SURCHARGE,
]
