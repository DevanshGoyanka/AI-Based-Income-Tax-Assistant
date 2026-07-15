"""AIS import use case - hexagonal architecture."""
from typing import BinaryIO
from uuid import UUID

from app.core.domain.canonical_ais import CanonicalAIS
from app.core.parsers.ais_json_parser import AISJSONParser
from app.core.parsers.ais_pdf_parser import AISPDFParser


class ImportAISUseCase:
    """Import AIS from JSON or PDF files."""
    
    def __init__(self):
        self.json_parser = AISJSONParser()
        self.pdf_parser = AISPDFParser()
    
    async def execute(
        self,
        file: BinaryIO,
        file_type: str,
        password: str,
        pan: str = None
    ) -> CanonicalAIS:
        """Import AIS from file.
        
        Args:
            file: Binary file stream
            file_type: "json" or "pdf"
            password: For JSON: DOB in DDMMYYYY. For PDF: not used (PAN+DOB generated)
            pan: PAN number (required for PDF)
            
        Returns:
            Canonical AIS data
        """
        if file_type == "json":
            return self.json_parser.parse(file, password)
        elif file_type == "pdf":
            if not pan:
                raise ValueError("PAN is required for PDF import")
            return self.pdf_parser.parse(file, pan, password)
        else:
            raise ValueError(f"Unsupported file type: {file_type}")
