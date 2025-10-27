package tools;

import server.SettingsDialog;
import server.Config;
import javax.swing.*;
import java.awt.*;

public class TestSettingsDialog {
    public static void main(String[] args) throws Exception {
        // Load current config
        Config cfg = Config.load();
        System.out.println("Before password: " + cfg.password);

        // Create owner frame (not shown)
        JFrame owner = new JFrame();

        // Construct dialog (builds UI)
        SettingsDialog dlg = new SettingsDialog(owner, cfg);

        // Find buttons by searching component tree
        JButton genBtn = findButton(dlg.getContentPane(), "Generate");
        JButton saveBtn = findButton(dlg.getContentPane(), "Save");

        if (genBtn == null || saveBtn == null) {
            System.err.println("Could not find Generate/Save buttons in SettingsDialog");
            System.exit(2);
        }

        // Click Generate and then Save on the EDT
        SwingUtilities.invokeAndWait(() -> {
            genBtn.doClick();
            // small delay to allow listener to update field
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            saveBtn.doClick();
        });

        // Reload config from disk to ensure save persisted
        Config cfg2 = Config.load();
        System.out.println("After generated password: " + cfg2.password);
    }

    private static JButton findButton(Container c, String text) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JButton) {
                JButton b = (JButton) comp;
                if (text.equals(b.getText())) return b;
            }
            if (comp instanceof Container) {
                JButton r = findButton((Container) comp, text);
                if (r != null) return r;
            }
        }
        return null;
    }
}
