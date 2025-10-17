package client;
import common.CryptoUtil;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class ClientApp extends JFrame {
    private JTextField serverIpField, fileField;
    private JTextArea logArea;
    private JButton downloadBtn;
    private static final int PORT = 5000;
    private static final String PASSWORD = "SuperSecret123"; 

    public ClientApp() {
        setTitle("Secure File Client");
        setSize(500, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(2, 2));
        topPanel.add(new JLabel("Server IP:"));
        serverIpField = new JTextField("127.0.0.1");
        topPanel.add(serverIpField);

        topPanel.add(new JLabel("File Name:"));
        fileField = new JTextField("test.txt");
        topPanel.add(fileField);

        downloadBtn = new JButton("Download File");

        logArea = new JTextArea();
        logArea.setEditable(false);

        add(topPanel, BorderLayout.NORTH);
        add(downloadBtn, BorderLayout.CENTER);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        downloadBtn.addActionListener(e -> downloadFile());
    }

    private void downloadFile() {
        new Thread(() -> {
            try (Socket socket = new Socket(serverIpField.getText().trim(), PORT);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                out.writeUTF(fileField.getText().trim());

                String response = in.readUTF();
                if (!response.equals("OK")) {
                    log("Server: " + response);
                    return;
                }

                String fileName = in.readUTF();
                int length = in.readInt();
                byte[] encrypted = new byte[length];
                in.readFully(encrypted);

                byte[] decrypted = CryptoUtil.decrypt(encrypted, PASSWORD);

                Files.write(Paths.get("downloaded_" + fileName), decrypted);
                log("File received: downloaded_" + fileName);

            } catch (Exception ex) {
                log("Error: " + ex.getMessage());
            }
        }).start();
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientApp().setVisible(true));
    }
}
