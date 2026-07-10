"""Cryptographic utilities for ITD document decryption."""
import base64
import hashlib
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.backends import default_backend


def decode_aadhaar(value: str) -> str:
    """Decode base64-encoded Aadhaar number from prefill JSON."""
    if not value:
        return ""
    try:
        decoded = base64.b64decode(value).decode("utf-8")
        return decoded if decoded.isdigit() and len(decoded) == 12 else value
    except Exception:
        return value


def decrypt_ais_json(encrypted_text: str, password: str) -> dict:
    """Decrypt AIS JSON using PBKDF2 + AES-256-CBC.
    
    Format: {64-hex-chars}{base64-ciphertext}
    Algorithm: PBKDF2-SHA256(100k iterations, salt from first 32 hex) -> AES-256-CBC
    IV: first 16 bytes of decrypted ciphertext
    """
    import json
    
    if len(encrypted_text) < 64:
        raise ValueError("Encrypted text too short (need at least 64 chars for hash)")
    
    hash_hex = encrypted_text[:64]
    ciphertext_b64 = encrypted_text[64:]
    
    salt = bytes.fromhex(hash_hex[:64])[:32]
    
    ciphertext = base64.b64decode(ciphertext_b64)
    if len(ciphertext) < 16:
        raise ValueError("Ciphertext too short (need at least 16 bytes for IV)")
    
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        iterations=100000,
        backend=default_backend()
    )
    key = kdf.derive(password.encode("utf-8"))
    
    iv = ciphertext[:16]
    actual_ciphertext = ciphertext[16:]
    
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
    decryptor = cipher.decryptor()
    plaintext_padded = decryptor.update(actual_ciphertext) + decryptor.finalize()
    
    # PKCS7 unpad
    padding_len = plaintext_padded[-1]
    plaintext = plaintext_padded[:-padding_len]
    
    return json.loads(plaintext.decode("utf-8"))


def decrypt_pdf(pdf_bytes: bytes, password: str) -> str:
    """Decrypt password-protected PDF and extract text.
    
    Returns the extracted text content from the PDF.
    """
    from pypdf import PdfReader
    from io import BytesIO
    
    reader = PdfReader(BytesIO(pdf_bytes))
    if reader.is_encrypted:
        if not reader.decrypt(password):
            raise ValueError("Invalid PDF password")
    
    # Extract text from all pages
    text_parts = []
    for page in reader.pages:
        text = page.extract_text()
        if text:
            text_parts.append(text)
    
    return "\n".join(text_parts)


def extract_zip_txt(zip_bytes: bytes, password: str) -> str:
    """Extract TXT file from password-protected ZIP."""
    import zipfile
    from io import BytesIO
    
    with zipfile.ZipFile(BytesIO(zip_bytes)) as zf:
        txt_files = [n for n in zf.namelist() if n.endswith('.txt')]
        if not txt_files:
            raise ValueError("No .txt file found in ZIP")
        
        txt_file = txt_files[0]
        try:
            content = zf.read(txt_file, pwd=password.encode('utf-8'))
            return content.decode('utf-8')
        except RuntimeError as e:
            if 'password' in str(e).lower():
                raise ValueError("Invalid ZIP password")
            raise
