package tools;

import common.CryptoUtil;
import java.io.*;
import java.net.Socket;

public class TestTransferClient {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: TestTransferClient <server> <port> <password> <filename>");
            System.exit(2);
        }
        String server = args[0];
        int port = Integer.parseInt(args[1]);
        String password = args[2];
        String filename = args[3];

        try (Socket s = new Socket(server, port)) {
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            out.writeUTF(filename);
            out.writeUTF(password);
            String resp = in.readUTF();
            if (!"OK".equals(resp)) {
                System.err.println("Server: " + resp);
                return;
            }
            String fname = in.readUTF();
            long len = in.readLong();
            System.out.println("Receiving " + fname + ", encrypted length=" + len);
            File outF = new File("downloaded_" + fname);
            try (OutputStream fos = new FileOutputStream(outF)) {
                CryptoUtil.decryptStream(in, fos, password, len);
            }
            System.out.println("Saved to " + outF.getAbsolutePath());
        }
    }
}
