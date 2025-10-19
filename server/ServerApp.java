package server;

import common.CryptoUtil;
import common.DBUtil;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import java.awt.geom.RoundRectangle2D;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.awt.GraphicsEnvironment;

public class ServerApp extends JFrame {
    private JTextArea logArea;
    private JButton startBtn, chooseFileBtn;
    private JLabel fileLabel;
    private JLabel statusLabel;
    private JLabel dbStatusLabel;
    private JProgressBar progressBar;
    private JLabel clientCountLabel;
    private File selectedFile;
    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicInteger activeClients = new AtomicInteger(0);
    private Config config;

    // port and password are loaded from Config at runtime
    private static final int FILE_TIMEOUT_MINUTES = 2;

    public ServerApp() {
        // Try to set Nimbus L&F for a modern look if available
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            // ignore and fall back to default
        }

    setTitle("Secure File Server");
    setSize(720, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
    // Apply Apple-like minimal palette
    Color bg = Color.white;
    Color panelBg = new Color(250, 250, 252);
    Color accent = new Color(10, 132, 255);
    Color text = new Color(28, 28, 30);

    getContentPane().setBackground(bg);

    logArea = new JTextArea();
    logArea.setEditable(false);
    logArea.setLineWrap(true);
    logArea.setWrapStyleWord(true);
    logArea.setBorder(new EmptyBorder(12, 12, 12, 12));
    logArea.setBackground(panelBg);
    logArea.setForeground(text);

    startBtn = new JButton("Start Server");
    chooseFileBtn = new JButton("Choose File");
    JButton settingsBtn = new JButton("Settings");
    fileLabel = new JLabel("No file selected");
    statusLabel = new JLabel("Stopped");
    clientCountLabel = new JLabel("Active clients: 0");
    dbStatusLabel = new JLabel("DB: unknown");
    progressBar = new JProgressBar();
    progressBar.setStringPainted(true);

    // Fonts and padding - prefer San Francisco if available
    Font base = pickFont();
    fileLabel.setFont(base.deriveFont(Font.BOLD, 14f));
    statusLabel.setFont(base.deriveFont(Font.PLAIN, 13f));
    logArea.setFont(base.deriveFont(Font.PLAIN, 13f));

    JPanel topPanel = new JPanel(new BorderLayout(12, 12));
    topPanel.setBackground(bg);
    JPanel controls = new JPanel();
    controls.setBackground(bg);
    controls.add(chooseFileBtn);
    controls.add(startBtn);
    controls.add(settingsBtn);

    JPanel info = new JPanel(new GridLayout(3, 1));
    info.add(fileLabel);
    info.add(statusLabel);
    info.add(dbStatusLabel);
        topPanel.add(controls, BorderLayout.WEST);
        topPanel.add(info, BorderLayout.CENTER);
        topPanel.add(clientCountLabel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        JScrollPane centerScroll = new JScrollPane(logArea);
        centerScroll.setBorder(BorderFactory.createEmptyBorder());
        centerScroll.setBackground(panelBg);
        add(centerScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBorder(new EmptyBorder(10, 12, 12, 12));
        bottom.setBackground(bg);
        bottom.add(progressBar, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Style buttons to be flat, rounded and accent-colored
        styleButton(startBtn, accent, base);
        styleButton(chooseFileBtn, new Color(44, 44, 46), base);
        styleButton(settingsBtn, new Color(88, 86, 214), base);

        // Load config
        config = Config.load();
        // apply configured port/password if needed (password used when encrypting/decrypting)
        // If DB logging disabled, override DBUtil behavior by toggling a flag (DBUtil still attempts to log)

        settingsBtn.addActionListener(e -> {
            SettingsDialog d = new SettingsDialog(this, config);
            d.setVisible(true);
        });

        fileLabel.setForeground(text);
        statusLabel.setForeground(text);
    dbStatusLabel.setForeground(text);
        clientCountLabel.setForeground(text);

        chooseFileBtn.addActionListener(e -> selectFile());
        startBtn.addActionListener(e -> startServer());

        // Check DB connection at startup (non-blocking)
        new Thread(() -> {
            String res = common.DBUtil.testConnection();
            SwingUtilities.invokeLater(() -> {
                if (res == null) {
                    dbStatusLabel.setText("DB: connected");
                    dbStatusLabel.setForeground(new Color(0, 128, 0));
                } else {
                    dbStatusLabel.setText("DB: error");
                    dbStatusLabel.setForeground(Color.RED);
                    log("DB test failed: " + res);
                }
            });
        }).start();
    }

    // Pick preferred system font (San Francisco) with fallbacks
    private Font pickFont() {
        String[] prefs = {"SF Pro Text", "San Francisco", "Segoe UI", "Helvetica Neue", "Arial", "SansSerif"};
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = ge.getAvailableFontFamilyNames();
        for (String p : prefs) {
            for (String a : available) {
                if (a.equalsIgnoreCase(p)) {
                    return new Font(a, Font.PLAIN, 13);
                }
            }
        }
        return new Font("SansSerif", Font.PLAIN, 13);
    }

    private void styleButton(JButton b, Color bg, Font base) {
        b.setBackground(bg);
        b.setForeground(Color.white);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(new RoundedBorder(10));
        b.setFont(base.deriveFont(Font.BOLD, 13f));
        b.setPreferredSize(new Dimension(140, 36));
    }

    // Simple rounded border
    private static class RoundedBorder implements Border {
        private final int radius;
        RoundedBorder(int r) { radius = r; }
        public Insets getBorderInsets(Component c) { return new Insets(radius, radius, radius, radius); }
        public boolean isBorderOpaque() { return false; }
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0,0,0,20));
            g2.fillRoundRect(x, y, width-1, height-1, radius, radius);
            g2.dispose();
        }
    }

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            fileLabel.setText("Selected: " + selectedFile.getName());
            log("File selected: " + selectedFile.getName());
        }
    }

    private void startServer() {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Starting..."));
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(config.port);
            log("Server started on port " + config.port);
            // Also print to stdout so terminal/startup scripts can detect server status
            System.out.println("Server started on port " + config.port);
            SwingUtilities.invokeLater(() -> statusLabel.setText("Listening on port " + config.port));

                while (true) {
                    Socket socket = serverSocket.accept();
                    log("Client connected: " + socket.getInetAddress());
                    System.out.println("Client connected: " + socket.getInetAddress());
                    executor.execute(() -> handleClient(socket));
                }
            } catch (IOException ex) {
                log("Server error: " + ex.getMessage());
                ex.printStackTrace(System.err);
                SwingUtilities.invokeLater(() -> statusLabel.setText("Error"));
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            activeClients.incrementAndGet();
            SwingUtilities.invokeLater(() -> clientCountLabel.setText("Active clients: " + activeClients.get()));

            String requestedFile = in.readUTF();

            if (selectedFile == null || !selectedFile.exists() || !selectedFile.getName().equals(requestedFile)) {
                out.writeUTF("File not found");
                return;
            }

            byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
            // show progress based on bytes sent
            SwingUtilities.invokeLater(() -> {
                progressBar.setMaximum(fileBytes.length);
                progressBar.setValue(0);
                progressBar.setString("Encrypting...");
            });

            byte[] encrypted = CryptoUtil.encrypt(fileBytes, config.password);

            out.writeUTF("OK");
            out.writeUTF(selectedFile.getName());
            out.writeInt(encrypted.length);

            // send in chunks and update progress
            int chunk = 8192;
            int sent = 0;
            ByteArrayInputStream bin = new ByteArrayInputStream(encrypted);
            byte[] buffer = new byte[chunk];
            int r;
            while ((r = bin.read(buffer)) != -1) {
                out.write(buffer, 0, r);
                sent += r;
                final int fSent = sent;
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(fSent);
                    progressBar.setString(String.format("Sending %d / %d bytes", fSent, encrypted.length));
                });
            }

            log("File sent: " + selectedFile.getName());
            System.out.println("File sent: " + selectedFile.getName() + " to " + socket.getInetAddress());
            SwingUtilities.invokeLater(() -> progressBar.setString("Idle"));

            // JDBC logging (only if enabled in config)
            if (config == null || config.dbLogging) {
                try {
                    DBUtil.logDownload(socket.getInetAddress().getHostAddress(), selectedFile.getName());
                } catch (Exception dbEx) {
                    log("DB logging failed: " + dbEx.getMessage());
                }
            }

            scheduler.schedule(() -> {
                if (selectedFile.delete()) {
                    log("File deleted after timeout: " + selectedFile.getName());
                    SwingUtilities.invokeLater(() -> fileLabel.setText("No file selected"));
                }
            }, FILE_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            log("Client error: " + e.getMessage());
        } finally {
            activeClients.decrementAndGet();
            SwingUtilities.invokeLater(() -> clientCountLabel.setText("Active clients: " + activeClients.get()));
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerApp app = new ServerApp();
            app.setVisible(true);

            // If a file path is provided as the first argument, select it and auto-start the server.
                if (args != null && args.length > 0) {
                    File f = new File(args[0]);
                    if (f.exists() && f.isFile()) {
                        app.selectedFile = f; // main is inside the same class so this is allowed
                        app.log("File selected from args: " + f.getAbsolutePath());
                        app.startServer();
                    } else {
                        app.log("File from args not found: " + f.getAbsolutePath());
                    }
                }
            });
        }
    }
