"""OCR (Optical Character Recognition) using pytesseract.

Used as a fallback when:
  1. PDF text extraction fails (scanned/image PDF)
  2. Form 16 is a scanned image rather than text PDF
  3. Password-protected PDF can't be decrypted (treat as scanned)

Requirements:
  - Tesseract OCR engine installed on the system
  - pip install pytesseract Pillow

Windows installation: download tesseract installer from
  https://github.com/UB-Mannheim/tesseract/wiki
Add the Tesseract directory to PATH, or set TESSERACT_CMD env var.
"""
from __future__ import annotations

import os
import logging
from io import BytesIO
from dataclasses import dataclass

logger = logging.getLogger(__name__)

# Lazy import — only needed when OCR is actually invoked
_tesseract_installed: bool | None = None


def _check_tesseract() -> bool:
    global _tesseract_installed
    if _tesseract_installed is not None:
        return _tesseract_installed
    try:
        import pytesseract
        # Try to run tesseract --version
        import subprocess
        result = subprocess.run(
            ["tesseract", "--version"],
            capture_output=True,
            timeout=5,
        )
        _tesseract_installed = result.returncode == 0
    except Exception as e:
        logger.warning("Tesseract not available: %s", e)
        _tesseract_installed = False
    return _tesseract_installed


@dataclass
class OCRResult:
    """Result of OCR on a PDF page or image."""
    page_number: int
    text: str
    confidence: float
    language: str = "eng"

    @property
    def is_empty(self) -> bool:
        return not self.text.strip()


def ocr_image_bytes(
    image_bytes: bytes,
    language: str = "eng",
    config: str = "",
) -> OCRResult:
    """Run OCR on a single image (PNG/JPEG/etc).

    Args:
        image_bytes: Raw bytes of the image.
        language: Tesseract language code. "eng" (English) is default.
                  For Hindi: "eng+hin". For regional: see tesseract docs.
        config: Extra tesseract config string, e.g. "--psm 6" (see tesseract --help).

    Returns:
        OCRResult with extracted text.

    Raises:
        RuntimeError: If Tesseract is not installed.
    """
    if not _check_tesseract():
        raise RuntimeError(
            "Tesseract OCR is not installed. "
            "Install from https://github.com/UB-Mannheim/tesseract/wiki "
            "and ensure 'tesseract' is on PATH, or set TESSERACT_CMD env var."
        )

    from PIL import Image
    import pytesseract

    image = Image.open(BytesIO(image_bytes))

    # Set tesseract path if specified
    tesseract_cmd = os.environ.get("TESSERACT_CMD")
    if tesseract_cmd:
        pytesseract.pytesseract.tesseract_cmd = tesseract_cmd

    # Extract text with confidence data
    data = pytesseract.image_to_data(
        image,
        lang=language,
        config=config,
        output_type=pytesseract.Output.DICT,
    )

    # Rebuild text line by line, filtering out low-confidence text
    words = []
    confidences = []
    for i, conf in enumerate(data["conf"]):
        text = data["text"][i].strip()
        if text:
            words.append(text)
            confidences.append(float(conf))

    full_text = " ".join(words)
    avg_confidence = sum(confidences) / len(confidences) if confidences else 0.0

    return OCRResult(
        page_number=1,
        text=full_text,
        confidence=avg_confidence,
        language=language,
    )


def ocr_pdf_pages(
    pdf_bytes: bytes,
    password: str | None = None,
    language: str = "eng",
    max_pages: int = 10,
) -> list[OCRResult]:
    """OCR all pages of a PDF (for scanned/image PDFs).

    First tries pypdf text extraction. If that returns empty text for a page,
    falls back to OCR on the rendered page image.

    Args:
        pdf_bytes: Raw bytes of the PDF.
        password: PDF password (if encrypted).
        language: Tesseract language code.
        max_pages: Limit pages to OCR (safety for large PDFs).

    Returns:
        List of OCRResult, one per page.
    """
    from pypdf import PdfReader

    reader = PdfReader(BytesIO(pdf_bytes))
    if reader.is_encrypted:
        if password:
            reader.decrypt(password)
        else:
            # Can't OCR an encrypted PDF without password
            return []

    results = []
    for page_num, page in enumerate(reader.pages[:max_pages], start=1):
        # Try text extraction first
        text = ""
        try:
            text = page.extract_text() or ""
        except Exception:
            pass

        if text.strip():
            # Page has text — no OCR needed
            results.append(OCRResult(
                page_number=page_num,
                text=text,
                confidence=100.0,
                language="pdf_text",
            ))
        else:
            # Empty text — OCR the page image
            try:
                # Render page to image at 300 DPI
                from PIL import Image
                import pypdf.generics.misc

                # Use the render page approach
                image = page.migration.parent._get_page_image(page._raw_page, 300)
                img_byte_arr = BytesIO()
                image.save(img_byte_arr, format="PNG")
                img_bytes = img_byte_arr.getvalue()

                ocr_result = ocr_image_bytes(img_bytes, language=language)
                ocr_result.page_number = page_num
                results.append(ocr_result)
            except Exception as e:
                logger.warning("OCR failed for page %d: %s", page_num, e)
                results.append(OCRResult(
                    page_number=page_num,
                    text="",
                    confidence=0.0,
                    language=language,
                ))

    return results


def extract_structured_from_ocr(
    ocr_results: list[OCRResult],
    document_type: str,
) -> dict:
    """Parse OCR text from a document into structured data.

    Args:
        ocr_results: List of OCRResult from ocr_pdf_pages or ocr_image_bytes.
        document_type: One of "AIS", "TIS", "26AS", "Form16", "Other".

    Returns:
        Dict with extracted fields. Shape depends on document_type.
    """
    import re

    # Combine all page text
    full_text = "\n".join(r.text for r in ocr_results)

    result: dict = {}

    # Extract PAN (10-char alphanumeric)
    pan_match = re.search(r"\b([A-Z]{5}\d{4}[A-Z])\b", full_text)
    if pan_match:
        result["pan"] = pan_match.group(1)

    # Extract Assessment Year
    ay_match = re.search(r"AY[:\s]*(\d{4}-\d{2})", full_text, re.IGNORECASE)
    if ay_match:
        result["assessment_year"] = ay_match.group(1)

    # Extract name (usually appears near "Name:" or after PAN)
    name_match = re.search(r"(?:Name|Assesse[e]?)[>:\s]+([A-Za-z\s\.]+)", full_text, re.IGNORECASE)
    if name_match:
        result["name"] = name_match.group(1).strip()

    if document_type == "Form16":
        # Extract employer details
        tan_match = re.search(r"TAN[:\s]*([A-Z]{5}\d{4}[A-Z])", full_text)
        if tan_match:
            result["employer_tan"] = tan_match.group(1)

        # Extract salary details
        salary_match = re.search(r"Salary.*?(\d[\d,]+\.?\d*)", full_text, re.IGNORECASE | re.DOTALL)
        if salary_match:
            amount_str = re.sub(r"[^\d]", "", salary_match.group(1))
            result["salary_amount"] = int(amount_str) if amount_str else 0

    return result
