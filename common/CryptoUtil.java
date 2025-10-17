package common;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil {
    private static final String ALGORITHM = "AES";

    private static SecretKeySpec getKey(String password) throws Exception {
        byte[] key = password.getBytes("UTF-8");
        byte[] fixedKey = new byte[16]; // AES-128
        System.arraycopy(key, 0, fixedKey, 0, Math.min(key.length, fixedKey.length));
        return new SecretKeySpec(fixedKey, ALGORITHM);
    }

    public static byte[] encrypt(byte[] data, String password) throws Exception {
        SecretKeySpec secretKey = getKey(password);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, String password) throws Exception {
        SecretKeySpec secretKey = getKey(password);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }
}