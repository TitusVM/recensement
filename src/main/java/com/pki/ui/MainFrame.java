package com.pki.ui;

import com.pki.config.AppConfig;
import com.pki.storage.CsvStorage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application window with tabbed panels for logging, viewing, and reporting.
 */
public class MainFrame extends JFrame {

    private final LogHoursPanel logPanel;
    private final EntriesPanel entriesPanel;
    private final ReportPanel reportPanel;

    public MainFrame(CsvStorage storage, AppConfig config) {
        super("Hours Tracker");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 650));

        logPanel = new LogHoursPanel(storage, config);
        entriesPanel = new EntriesPanel(storage, config);
        reportPanel = new ReportPanel(storage, config);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Log Hours", logPanel);
        tabs.addTab("All Entries", entriesPanel);
        tabs.addTab("Reports", reportPanel);

        // Refresh data when switching tabs
        tabs.addChangeListener(e -> {
            int idx = tabs.getSelectedIndex();
            switch (idx) {
                case 0 -> logPanel.refreshTable();
                case 1 -> entriesPanel.refreshTable();
                case 2 -> reportPanel.refreshData();
            }
        });

        // ---- Status bar ----
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        JLabel dataFileLabel = new JLabel("Data: " + storage.getDataFile().toAbsolutePath());
        dataFileLabel.setFont(dataFileLabel.getFont().deriveFont(Font.PLAIN, 10f));
        dataFileLabel.setForeground(Color.GRAY);
        statusBar.add(dataFileLabel);

        JPanel content = new JPanel(new BorderLayout());
        content.add(tabs, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);
        setContentPane(content);

        // ---- Window close ----
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                config.savePreferences();
                dispose();
                System.exit(0);
            }
        });

        pack();
        setLocationRelativeTo(null); // center on screen
    }
}
