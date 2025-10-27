package tools;

import common.CryptoUtil;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class HeadlessServer {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: HeadlessServer <port> <password> <filePath>");
            System.exit(2);
        }
        int port = Integer.parseInt(args[0]);
        String password = args[1];
        File file = new File(args[2]);
        if (!file.exists()) { System.err.println("File not found: " + file); System.exit(2); }

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("HeadlessServer listening on " + port);
            while (true) {
                try (Socket s = ss.accept()) {
                    System.out.println("Client: " + s.getInetAddress());
                    DataInputStream in = new DataInputStream(s.getInputStream());
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    String req = in.readUTF();
                    String clientPw = in.readUTF();
                    System.out.println("Requested: " + req + " by " + s.getInetAddress());
                    if (!file.getName().equals(req)) {
                        out.writeUTF("File not found");
                        continue;
                    }
                    if (!password.equals(clientPw)) {
                        out.writeUTF("Auth Failed");
                        System.out.println("Auth failed for " + s.getInetAddress());
                        continue;
                    }
                    out.writeUTF("OK");
                    out.writeUTF(file.getName());
                    out.writeLong(-1L);
                    try (InputStream fin = new FileInputStream(file)) {
                        CryptoUtil.encryptStream(fin, out, password);
                    }
                    System.out.println("Sent file: " + file.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
