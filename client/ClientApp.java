package client;

import common.CryptoUtil;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;

public class ClientApp extends JFrame {
    private JTextField serverIpField, fileField;
    private JTextArea logArea;
    private JButton downloadBtn;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private static final int PORT = 5000;
    private static final String PASSWORD = "SuperSecret123";

    public ClientApp() {
        // Nimbus look and feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        setTitle("Secure File Client");
        setSize(520, 360);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new GridLayout(2, 2, 6, 6));
        top.setBorder(new EmptyBorder(8, 8, 0, 8));
        top.add(new JLabel("Server IP:"));
        serverIpField = new JTextField("127.0.0.1");
        top.add(serverIpField);
        top.add(new JLabel("File Name:"));
        fileField = new JTextField("test.txt");
        top.add(fileField);

        downloadBtn = new JButton("Download File");
        statusLabel = new JLabel("Idle");
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.add(downloadBtn, BorderLayout.NORTH);
        center.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setBorder(new EmptyBorder(0, 8, 8, 8));
        bottom.add(statusLabel, BorderLayout.WEST);
        bottom.add(progressBar, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        downloadBtn.addActionListener(e -> downloadFile());
    }

    private void downloadFile() {
        new Thread(() -> {
            String server = serverIpField.getText().trim();
            String filename = fileField.getText().trim();
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Connecting...");
                progressBar.setValue(0);
                logArea.append("Connecting to " + server + "...\n");
            });

            try (Socket socket = new Socket(server, PORT);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                out.writeUTF(filename);

                String response = in.readUTF();
                if (!response.equals("OK")) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("Server: " + response + "\n");
                        statusLabel.setText("Error");
                    });
                    return;
                }

                String fileName = in.readUTF();
                int length = in.readInt();
                byte[] encrypted = new byte[length];
                in.readFully(encrypted);

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Decrypting...");
                    progressBar.setMaximum(encrypted.length);
                    progressBar.setValue(0);
                });

                byte[] decrypted = CryptoUtil.decrypt(encrypted, PASSWORD);

                File outFile = new File("downloaded_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(decrypted);
                }

                SwingUtilities.invokeLater(() -> {
                    logArea.append("File received: " + outFile.getName() + "\n");
                    statusLabel.setText("Done");
                    progressBar.setValue(progressBar.getMaximum());
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append("Error: " + ex.getMessage() + "\n");
                    statusLabel.setText("Error");
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientApp().setVisible(true));
    }
}
