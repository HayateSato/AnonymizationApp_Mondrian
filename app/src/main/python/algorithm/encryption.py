from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
import base64

def generate_fernet(password):
    password = password.encode()
    salt = b'salt_'  # You can change this salt, but keep it constant
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        iterations=100000,
    )
    key = base64.urlsafe_b64encode(kdf.derive(password))
    fernet = Fernet(key)
    return fernet

def encrypt_value(value, fernet):
    return fernet.encrypt(str(value).encode()).decode()

