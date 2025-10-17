package server;

import common.CryptoUtil;
import common.DBUtil;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.*;

// ...existing code...
public class ServerApp extends JFrame {
    private JTextArea logArea;
    private JButton startBtn, chooseFileBtn;
    private File selectedFile;
    private ServerSocket serverSocket;
    private ExecutorService executor = Executors.newFixedThreadPool(5);
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final int PORT = 5000;
    private static final String PASSWORD = "SuperSecret123"; 
    private static final int FILE_TIMEOUT_MINUTES = 2;

    public ServerApp() {
        setTitle("Secure File Server");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);

        startBtn = new JButton("Start Server");
        chooseFileBtn = new JButton("Choose File");

        JPanel topPanel = new JPanel();
        topPanel.add(chooseFileBtn);
        topPanel.add(startBtn);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        chooseFileBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = chooser.getSelectedFile();
                log("File selected: " + selectedFile.getName());
            }
        });

        startBtn.addActionListener(e -> startServer());
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                log("Server started on port " + PORT);

                while (true) {
                    Socket socket = serverSocket.accept();
                    log("Client connected: " + socket.getInetAddress());
                    executor.execute(() -> handleClient(socket));
                }
            } catch (IOException ex) {
                log("Server error: " + ex.getMessage());
            }
        }).start();
    }

private void handleClient(Socket socket) {
    try (DataInputStream in = new DataInputStream(socket.getInputStream());
         DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

        String requestedFile = in.readUTF();

        if (selectedFile == null || !selectedFile.exists() || 
            !selectedFile.getName().equals(requestedFile)) {
            out.writeUTF("File not found");
            return;
        }

        byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
        byte[] encrypted = CryptoUtil.encrypt(fileBytes, PASSWORD);

        out.writeUTF("OK");
        out.writeUTF(selectedFile.getName());
        out.writeInt(encrypted.length);
        out.write(encrypted);

        log("File sent: " + selectedFile.getName());

        // JDBC logging
        DBUtil.logDownload(socket.getInetAddress().getHostAddress(), selectedFile.getName());
        //DBUtil.logDownload(socket.getInetAddress().getHostAddress(), selectedFile.getName());

        scheduler.schedule(() -> {
            if (selectedFile.delete()) {
                log("File deleted after timeout: " + selectedFile.getName());
            }
        }, FILE_TIMEOUT_MINUTES, TimeUnit.MINUTES);

    } catch (Exception e) {
        log("Client error: " + e.getMessage());
    }
}


    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerApp().setVisible(true));
    }
}
