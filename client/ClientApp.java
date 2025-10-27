package client;

import common.CryptoUtil;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class ClientApp extends JFrame {
    private final JTextField serverIpField, fileField, portField;
    private final JPasswordField passwordField;
    private final JTextArea logArea;
    private final JButton downloadBtn;
    private final JLabel statusLabel;
    private static final String CONFIG_FILE = "client.properties";
    private static final int DEFAULT_PORT = 5000; // Default port number
    private final JProgressBar progressBar; // Progress bar for status updates

    public ClientApp() {
        // Nimbus look and feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {}

        setTitle("Secure File Client");
        setSize(520, 360);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // Top panel for server IP
        JPanel ipPanel = new JPanel(new BorderLayout(6, 6));
        ipPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
        JLabel ipLabel = new JLabel("Server IP:", SwingConstants.RIGHT);
        ipLabel.setFont(ipLabel.getFont().deriveFont(Font.BOLD, 14f));
        serverIpField = new JTextField("127.0.0.1", 18);
        serverIpField.setFont(serverIpField.getFont().deriveFont(Font.PLAIN, 14f));
        serverIpField.setToolTipText("Enter the IP address of the file server (e.g., 192.168.1.100)");
        ipPanel.add(ipLabel, BorderLayout.WEST);
        ipPanel.add(serverIpField, BorderLayout.CENTER);

        // Connection panel (Port)
        JPanel connPanel = new JPanel(new GridLayout(1, 2, 6, 6));
        connPanel.setBorder(new EmptyBorder(4, 10, 0, 10));
        connPanel.add(new JLabel("Port:", SwingConstants.RIGHT));
        portField = new JTextField(String.valueOf(DEFAULT_PORT), 8);
        portField.setToolTipText("Server port number (default: 5000)");
        connPanel.add(portField);

        // File panel
        JPanel filePanel = new JPanel(new GridLayout(1, 2, 6, 6));
        filePanel.setBorder(new EmptyBorder(4, 10, 0, 10));
        filePanel.add(new JLabel("File Name:", SwingConstants.RIGHT));
        fileField = new JTextField("test.txt", 14);
        fileField.setToolTipText("Name of the file to download");
        filePanel.add(fileField);

        // Password panel
        JPanel pwPanel = new JPanel(new BorderLayout(6,6));
        pwPanel.setBorder(new EmptyBorder(4,10,0,10));
        pwPanel.add(new JLabel("Password:", SwingConstants.RIGHT), BorderLayout.WEST);
        passwordField = new JPasswordField(14);
        passwordField.setToolTipText("Password for file decryption");
        pwPanel.add(passwordField, BorderLayout.CENTER);

        // Buttons
        downloadBtn = new JButton("Download File");
        JButton saveBtn = new JButton("Save Settings");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        buttonPanel.add(downloadBtn);
        buttonPanel.add(saveBtn);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.add(buttonPanel, BorderLayout.NORTH);
        center.add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Idle");
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setBorder(new EmptyBorder(0, 10, 10, 10));
        bottom.add(statusLabel, BorderLayout.WEST);
        bottom.add(progressBar, BorderLayout.CENTER);

        // Stack all panels vertically
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(ipPanel);
        northPanel.add(connPanel);
        northPanel.add(filePanel);
        northPanel.add(pwPanel);

        add(northPanel, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        downloadBtn.addActionListener(e -> connectToServer());
        saveBtn.addActionListener(e -> saveSettings());

    // load persisted settings (if any)
    loadSettings();

    // center and show window
    setLocationRelativeTo(null);
    setVisible(true);
    }

    private void connectToServer() {
        String serverIp = serverIpField.getText();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            logArea.append("Invalid port number. Using default: " + DEFAULT_PORT + "\n");
            port = DEFAULT_PORT;
        }
        try (Socket socket = new Socket(serverIp, port)) {
            logArea.append("Connected to server: " + serverIp + "\n");
            socket.getOutputStream().write(0); // Simulate usage
            downloadFile();
        } catch (IOException e) {
            logArea.append("Connection failed: " + e.getMessage() + "\n");
        }
    }

    private void downloadFile() {
        new Thread(() -> {
                String server = serverIpField.getText().trim();
                String filename = fileField.getText().trim();
                int port = DEFAULT_PORT;
                try {
                    port = Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException ex) {
                    // keep default
                }
                String password = new String(passwordField.getPassword());
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Connecting...");
                progressBar.setValue(0);
                logArea.append("Connecting to " + server + "...\n");
            });

            try (Socket socket = new Socket(server, port);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                out.writeUTF(filename);
                out.writeUTF(password);

                String response = in.readUTF();
                if (!response.equals("OK")) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("Server: " + response + "\n");
                        statusLabel.setText("Error");
                    });
                    return;
                }

                String fileName = in.readUTF();
                long length = in.readLong();

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Receiving & decrypting...");
                    progressBar.setIndeterminate(true);
                });

                File outFile = new File("downloaded_" + fileName);
                try (java.io.OutputStream fos = new java.io.FileOutputStream(outFile)) {
                    // if length == -1, server is streaming; otherwise decrypt known-length stream
                    CryptoUtil.decryptStream(in, fos, password, length);
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("Decryption error: " + e.getMessage() + "\n");
                        statusLabel.setText("Error");
                    });
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    logArea.append("File received: " + outFile.getName() + "\n");
                    statusLabel.setText("Done");
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(progressBar.getMaximum());
                });

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append("Error: " + ex.getMessage() + "\n");
                    statusLabel.setText("Error");
                });
            }
        }).start();
    }

    private void loadSettings() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.load(fis);
            serverIpField.setText(props.getProperty("serverIp", "127.0.0.1"));
            portField.setText(props.getProperty("port", String.valueOf(DEFAULT_PORT)));
            fileField.setText(props.getProperty("fileName", "test.txt"));
            logArea.append("Settings loaded successfully.\n");
        } catch (IOException e) {
            logArea.append("Failed to load settings: " + e.getMessage() + "\n");
        }
    }

    private void saveSettings() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("serverIp", serverIpField.getText());
            props.setProperty("port", portField.getText());
            props.setProperty("fileName", fileField.getText());
            props.store(fos, "Client Settings");
            logArea.append("Settings saved successfully.\n");
        } catch (IOException e) {
            logArea.append("Failed to save settings: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientApp::new);
    }
}
