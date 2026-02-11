package com.pki.ui;

import com.pki.config.AppConfig;
import com.pki.model.TimeEntry;
import com.pki.storage.CsvStorage;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel for generating reports with pie charts: hours by sector and by task.
 */
public class ReportPanel extends JPanel {

    private final CsvStorage storage;
    private final AppConfig config;

    private final DatePickerField fromDateField;
    private final DatePickerField toDateField;
    private final JComboBox<String> collaboratorCombo;
    private final PieChartComponent sectorChart;
    private final PieChartComponent taskChart;
    private final JLabel summaryLabel;

    public ReportPanel(CsvStorage storage, AppConfig config) {
        this.storage = storage;
        this.config = config;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---- Filter bar ----
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Report Filters"));

        LocalDate now = LocalDate.now();
        fromDateField = new DatePickerField(now.withDayOfMonth(1));
        toDateField = new DatePickerField(now);
        collaboratorCombo = new JComboBox<>(new String[]{"All"});

        filterPanel.add(new JLabel("From:"));
        filterPanel.add(fromDateField);
        filterPanel.add(new JLabel("To:"));
        filterPanel.add(toDateField);
        filterPanel.add(new JLabel("Collaborator:"));
        filterPanel.add(collaboratorCombo);

        JButton generateButton = new JButton("Generate Report");
        generateButton.setFont(generateButton.getFont().deriveFont(Font.BOLD));
        filterPanel.add(generateButton);

        add(filterPanel, BorderLayout.NORTH);

        // ---- Charts ----
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        sectorChart = new PieChartComponent();
        sectorChart.setTitle("Hours by Sector");
        taskChart = new PieChartComponent();
        taskChart.setTitle("Hours by Task");
        chartsPanel.add(sectorChart);
        chartsPanel.add(taskChart);
        add(chartsPanel, BorderLayout.CENTER);

        // ---- Summary ----
        summaryLabel = new JLabel("Click 'Generate Report' to see charts.");
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.ITALIC, 12f));
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(8, 5, 5, 5));
        add(summaryLabel, BorderLayout.SOUTH);

        // ---- Wiring ----
        generateButton.addActionListener(e -> generateReport());
    }

    /**
     * Reload the collaborator dropdown and regenerate charts.
     */
    public void refreshData() {
        try {
            List<TimeEntry> all = storage.readAll();
            Set<String> collaborators = all.stream()
                    .map(TimeEntry::getCollaborator)
                    .collect(Collectors.toCollection(TreeSet::new));
            String prev = (String) collaboratorCombo.getSelectedItem();
            collaboratorCombo.removeAllItems();
            collaboratorCombo.addItem("All");
            collaborators.forEach(collaboratorCombo::addItem);
            if (prev != null) collaboratorCombo.setSelectedItem(prev);
        } catch (Exception ex) {
            // Silently ignore on refresh; errors will show when generating
        }
    }

    private void generateReport() {
        try {
            List<TimeEntry> all = storage.readAll();

            // Apply filters
            LocalDate from = fromDateField.getDate();
            LocalDate to = toDateField.getDate();
            String collabFilter = (String) collaboratorCombo.getSelectedItem();

            List<TimeEntry> filtered = all.stream()
                    .filter(e -> !e.getDate().isBefore(from) && !e.getDate().isAfter(to))
                    .filter(e -> "All".equals(collabFilter)
                            || e.getCollaborator().equalsIgnoreCase(collabFilter))
                    .toList();

            if (filtered.isEmpty()) {
                sectorChart.setData(Collections.emptyMap());
                taskChart.setData(Collections.emptyMap());
                summaryLabel.setText("No entries found for the selected filters.");
                return;
            }

            // Hours by Sector
            Map<String, Double> bySector = new LinkedHashMap<>();
            for (TimeEntry e : filtered) {
                bySector.merge(e.getSector(), e.getHours(), Double::sum);
            }
            sectorChart.setData(bySector);

            // Hours by Task (Sector > Task)
            Map<String, Double> byTask = new LinkedHashMap<>();
            for (TimeEntry e : filtered) {
                String key = e.getSector() + " > " + e.getTask();
                byTask.merge(key, e.getHours(), Double::sum);
            }
            taskChart.setData(byTask);

            // Summary
            double totalHours = filtered.stream().mapToDouble(TimeEntry::getHours).sum();
            long uniqueDays = filtered.stream().map(TimeEntry::getDate).distinct().count();
            long uniqueCollaborators = filtered.stream()
                    .map(e -> e.getCollaborator().toLowerCase())
                    .distinct().count();
            summaryLabel.setText(String.format(
                    "Period: %s to %s  |  Entries: %d  |  Total hours: %.1f  |  "
                            + "Working days: %d  |  Collaborators: %d",
                    from, to, filtered.size(), totalHours, uniqueDays, uniqueCollaborators));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not generate report:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
