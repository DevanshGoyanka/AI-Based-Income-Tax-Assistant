"""Common enums."""
from enum import Enum


class TaxRegime(str, Enum):
    OLD = "old"
    NEW = "new"


class ITRFormType(str, Enum):
    ITR1 = "ITR-1"
    ITR2 = "ITR-2"
    ITR3 = "ITR-3"
    ITR4 = "ITR-4"
    ITR5 = "ITR-5"
    ITR6 = "ITR-6"
    ITR7 = "ITR-7"
    ITRU = "ITR-U"


class AgeCategory(str, Enum):
    BELOW_60 = "below_60"
    SENIOR_60_TO_80 = "senior_60_to_80"
    SUPER_SENIOR_ABOVE_80 = "super_senior_above_80"


class ResidentialStatus(str, Enum):
    ROR = "ROR"
    RNOR = "RNOR"
    NRI = "NRI"


class Gender(str, Enum):
    M = "M"
    F = "F"
    O = "O"


class RuleLifecycle(str, Enum):
    SUPPORTED = "SUPPORTED"
    DRAFT = "DRAFT"
    FROZEN = "FROZEN"
    DEPRECATED = "DEPRECATED"


class IncomeHead(str, Enum):
    SALARY = "salary"
    HOUSE_PROPERTY = "house_property"
    BUSINESS = "business"
    CAPITAL_GAINS = "capital_gains"
    OTHER_SOURCES = "other_sources"
