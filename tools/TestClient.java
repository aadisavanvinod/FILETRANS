package tools;

import common.CryptoUtil;
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class TestClient {
    public static void main(String[] args) throws Exception {
        String server = args.length > 0 ? args[0] : "127.0.0.1";
        String filename = args.length > 1 ? args[1] : "testfile.txt";

        try (Socket s = new Socket(server, 5000);
             DataOutputStream out = new DataOutputStream(s.getOutputStream());
             DataInputStream in = new DataInputStream(s.getInputStream())) {

            out.writeUTF(filename);

            String resp = in.readUTF();
            if (!"OK".equals(resp)) {
                System.out.println("Server response: " + resp);
                return;
            }

            String fn = in.readUTF();
            int len = in.readInt();
            byte[] enc = new byte[len];
            in.readFully(enc);

            byte[] dec = CryptoUtil.decrypt(enc, "SuperSecret123");
            Path outPath = Paths.get("test_downloaded_" + fn);
            Files.write(outPath, dec);
            System.out.println("Downloaded: " + outPath.toString());
        }
    }
}
