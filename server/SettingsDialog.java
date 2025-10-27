package server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class SettingsDialog extends JDialog {
    private Config cfg;

    public SettingsDialog(JFrame owner, Config cfg) {
        super(owner, "Server Settings", true);
        this.cfg = cfg;
        setSize(420, 260);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8,8));

        JPanel form = new JPanel(new GridLayout(5,2,8,8));
        form.setBorder(new EmptyBorder(12,12,12,12));

    JTextField portField = new JTextField(Integer.toString(cfg.port));
    JTextField passField = new JTextField(cfg.password);
        JCheckBox dbBox = new JCheckBox("Enable DB logging", cfg.dbLogging);
        JTextField urlField = new JTextField(cfg.dbUrl);
        JTextField userField = new JTextField(cfg.dbUser);

    form.add(new JLabel("Port:")); form.add(portField);
    form.add(new JLabel("Password:")); form.add(passField);
        form.add(new JLabel("DB URL:")); form.add(urlField);
        form.add(new JLabel("DB User:")); form.add(userField);
        form.add(dbBox); form.add(new JLabel());

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
    JButton gen = new JButton("Generate");
    JButton save = new JButton("Save");
    JButton cancel = new JButton("Cancel");
        buttons.add(save); buttons.add(cancel);
    buttons.add(gen);
        add(buttons, BorderLayout.SOUTH);

        save.addActionListener(e -> {
            try {
                cfg.port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) { cfg.port = 5000; }
            cfg.password = passField.getText();
            cfg.dbLogging = dbBox.isSelected();
            cfg.dbUrl = urlField.getText();
            cfg.dbUser = userField.getText();

            // Ensure server password is not the same as the DB password
            if (cfg.password != null && cfg.password.equals(cfg.dbPass)) {
                // generate a new secure password
                byte[] r = new byte[16];
                try {
                    java.security.SecureRandom.getInstanceStrong().nextBytes(r);
                } catch (Exception ex2) {
                    new java.security.SecureRandom().nextBytes(r);
                }
                StringBuilder sb = new StringBuilder();
                for (byte b : r) sb.append(String.format("%02x", b));
                cfg.password = sb.toString();
                passField.setText(cfg.password);
                JOptionPane.showMessageDialog(this, "The server password matched the DB password. A new password was generated to keep them separate.", "Password adjusted", JOptionPane.INFORMATION_MESSAGE);
            }

            try { cfg.save(); } catch (Exception ex) { /* ignore */ }
            setVisible(false);
        });

        cancel.addActionListener(e -> setVisible(false));

        gen.addActionListener(e -> {
            try {
                // generate a 16-byte secure random password and encode as hex
                byte[] r = new byte[16];
                java.security.SecureRandom.getInstanceStrong().nextBytes(r);
                StringBuilder sb = new StringBuilder();
                for (byte b : r) sb.append(String.format("%02x", b));
                String pw = sb.toString();
                passField.setText(pw);
            } catch (Exception ex) {
                // fallback
                byte[] r = new byte[16];
                new java.security.SecureRandom().nextBytes(r);
                StringBuilder sb = new StringBuilder();
                for (byte b : r) sb.append(String.format("%02x", b));
                passField.setText(sb.toString());
            }
        });
    }
}
