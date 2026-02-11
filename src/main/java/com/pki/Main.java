package com.pki;

import com.pki.config.AppConfig;
import com.pki.storage.CsvStorage;
import com.pki.ui.MainFrame;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry point for the Hours Tracker application.
 * <p>
 * The data directory can be configured via:
 * <ul>
 *   <li>Environment variable {@code HOURS_TRACKER_DIR}</li>
 *   <li>System property {@code -Ddata.dir=...}</li>
 *   <li>Default: current working directory</li>
 * </ul>
 */
public class Main {
    public static void main(String[] args) {
        // Determine data directory
        String dirProp = System.getProperty("data.dir",
                System.getenv().getOrDefault("HOURS_TRACKER_DIR", "."));
        Path dataDir = Paths.get(dirProp).toAbsolutePath().normalize();

        try {
            // Use the system look and feel for a native appearance
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            try {
                AppConfig config = new AppConfig(dataDir);
                config.load();

                CsvStorage storage = new CsvStorage(dataDir.resolve("hours.csv"));
                storage.createBackup();

                MainFrame frame = new MainFrame(storage, config);
                frame.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Failed to start Hours Tracker:\n" + ex.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
                System.exit(1);
            }
        });
    }
}