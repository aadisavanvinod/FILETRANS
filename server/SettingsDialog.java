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
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        buttons.add(save); buttons.add(cancel);
        add(buttons, BorderLayout.SOUTH);

        save.addActionListener(e -> {
            try {
                cfg.port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) { cfg.port = 5000; }
            cfg.password = passField.getText();
            cfg.dbLogging = dbBox.isSelected();
            cfg.dbUrl = urlField.getText();
            cfg.dbUser = userField.getText();
            try { cfg.save(); } catch (Exception ex) { /* ignore */ }
            setVisible(false);
        });

        cancel.addActionListener(e -> setVisible(false));
    }
}
