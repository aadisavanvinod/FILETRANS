package common;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.IOException;

public class CryptoUtil {
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 16;
    private static final int KDF_ITERATIONS = 65536;
    private static final int KEY_BITS = 128; // AES-128

    private static SecretKeySpec deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, KDF_ITERATIONS, KEY_BITS);
        byte[] keyBytes = SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret((PBEKeySpec) spec).getEncoded();
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Encrypts data using password-derived AES-128-CBC. Output = salt||iv||ciphertext
     */
    public static byte[] encrypt(byte[] data, String password) throws Exception {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[SALT_LEN];
        rnd.nextBytes(salt);
        byte[] iv = new byte[IV_LEN];
        rnd.nextBytes(iv);

        SecretKeySpec key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] ct = cipher.doFinal(data);

        byte[] out = new byte[SALT_LEN + IV_LEN + ct.length];
        System.arraycopy(salt, 0, out, 0, SALT_LEN);
        System.arraycopy(iv, 0, out, SALT_LEN, IV_LEN);
        System.arraycopy(ct, 0, out, SALT_LEN + IV_LEN, ct.length);

        // try to clear derived key bytes
        try {
            byte[] kb = key.getEncoded();
            if (kb != null) Arrays.fill(kb, (byte) 0);
        } catch (Throwable ignored) {
        }

        return out;
    }

    /**
     * Decrypts data produced by encrypt(...). Expects salt||iv||ciphertext
     */
    public static byte[] decrypt(byte[] data, String password) throws Exception {
        if (data == null || data.length < SALT_LEN + IV_LEN) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }

        byte[] salt = Arrays.copyOfRange(data, 0, SALT_LEN);
        byte[] iv = Arrays.copyOfRange(data, SALT_LEN, SALT_LEN + IV_LEN);
        byte[] ct = Arrays.copyOfRange(data, SALT_LEN + IV_LEN, data.length);

        SecretKeySpec key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] plain = cipher.doFinal(ct);

        try {
            byte[] kb = key.getEncoded();
            if (kb != null) Arrays.fill(kb, (byte) 0);
        } catch (Throwable ignored) {
        }

        return plain;
    }

    /**
     * Stream-encrypts data from input and writes salt||iv||ciphertext to out.
     * This avoids allocating the entire file in memory.
     */
    public static void encryptStream(InputStream in, OutputStream out, String password) throws Exception {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[SALT_LEN];
        rnd.nextBytes(salt);
        byte[] iv = new byte[IV_LEN];
        rnd.nextBytes(iv);

        // write salt and iv first
        out.write(salt);
        out.write(iv);

        SecretKeySpec key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        try (CipherOutputStream cos = new CipherOutputStream(out, cipher)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                cos.write(buf, 0, r);
            }
            cos.flush();
        }

        try {
            byte[] kb = key.getEncoded();
            if (kb != null) Arrays.fill(kb, (byte) 0);
        } catch (Throwable ignored) {}
    }

    // LimitedInputStream reads up to a fixed number of bytes then returns EOF
    private static class LimitedInputStream extends InputStream {
        private final InputStream in;
        private long remaining;
        LimitedInputStream(InputStream in, long limit) { this.in = in; this.remaining = limit; }
        public int read() throws IOException { if (remaining == 0) return -1; int v = in.read(); if (v != -1) remaining--; return v; }
        public int read(byte[] b, int off, int len) throws IOException { if (remaining == 0) return -1; int toRead = (int)Math.min(len, remaining); int r = in.read(b, off, toRead); if (r > 0) remaining -= r; return r; }
        public void close() throws IOException { /* don't close underlying */ }
    }

    /**
     * Stream-decrypts data from input (expects salt||iv||ciphertext). If encryptedLength >=0,
     * it will only read that many bytes from the input (including salt+iv). If -1, reads until EOF.
     */
    public static void decryptStream(InputStream in, OutputStream out, String password, long encryptedLength) throws Exception {
        // read salt and iv
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];

        // total bytes to read for ciphertext may be unknown (-1)
        if (encryptedLength == -1) {
            // read salt and iv from stream
            readFully(in, salt);
            readFully(in, iv);
            SecretKeySpec key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            try (CipherInputStream cis = new CipherInputStream(in, cipher)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = cis.read(buf)) != -1) out.write(buf, 0, r);
                out.flush();
            }
            try { byte[] kb = key.getEncoded(); if (kb != null) Arrays.fill(kb, (byte)0); } catch (Throwable ignored) {}
        } else {
            // encryptedLength includes salt+iv+ct
            if (encryptedLength < SALT_LEN + IV_LEN) throw new IllegalArgumentException("Invalid encrypted length");
            readFully(in, salt);
            readFully(in, iv);
            long ctLen = encryptedLength - SALT_LEN - IV_LEN;
            SecretKeySpec key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            InputStream limited = new LimitedInputStream(in, ctLen);
            try (CipherInputStream cis = new CipherInputStream(limited, cipher)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = cis.read(buf)) != -1) out.write(buf, 0, r);
                out.flush();
            }
            try { byte[] kb = key.getEncoded(); if (kb != null) Arrays.fill(kb, (byte)0); } catch (Throwable ignored) {}
        }
    }

    private static void readFully(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int r = in.read(buf, off, buf.length - off);
            if (r == -1) throw new IOException("Unexpected EOF");
            off += r;
        }
    }
}