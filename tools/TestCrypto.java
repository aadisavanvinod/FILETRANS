package tools;

import common.CryptoUtil;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TestCrypto {
    public static void main(String[] args) throws Exception {
        String password = "test-password-123";
        String plain = "The quick brown fox jumps over the lazy dog";

        byte[] enc = CryptoUtil.encrypt(plain.getBytes(StandardCharsets.UTF_8), password);
        byte[] dec = CryptoUtil.decrypt(enc, password);
        String recovered = new String(dec, StandardCharsets.UTF_8);

        System.out.println("Plain: " + plain);
        System.out.println("Recovered: " + recovered);
        if (!plain.equals(recovered)) {
            System.err.println("ERROR: recovered text does not match");
            System.exit(2);
        }
        System.out.println("OK: encrypt/decrypt round-trip successful");
    }
}
