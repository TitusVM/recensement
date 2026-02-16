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
 * Panel for generating reports with pie charts.
 * <p>
 * Left column: Hours by Sector (date range + collaborator filter).
 * Right column: Hours by Task within a sector (date range + collaborator + sector filter).
 * Each chart has its own Refresh button.
 */
public class ReportPanel extends JPanel {

    private final CsvStorage storage;
    private final AppConfig config;

    // Left chart filters
    private final DatePickerField sectorFromDate;
    private final DatePickerField sectorToDate;
    private final JComboBox<String> sectorCollabCombo;
    private final PieChartComponent sectorChart;

    // Right chart filters
    private final DatePickerField taskFromDate;
    private final DatePickerField taskToDate;
    private final JComboBox<String> taskCollabCombo;
    private final JComboBox<String> taskSectorCombo;
    private final PieChartComponent taskChart;

    private final JLabel summaryLabel;

    public ReportPanel(CsvStorage storage, AppConfig config) {
        this.storage = storage;
        this.config = config;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        LocalDate now = LocalDate.now();

        // =============== LEFT COLUMN: Hours by Sector ===============
        JPanel leftPanel = new JPanel(new BorderLayout(4, 4));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Hours by Sector"));

        JPanel leftFilters = new JPanel();
        leftFilters.setLayout(new BoxLayout(leftFilters, BoxLayout.Y_AXIS));

        // Date row
        JPanel leftDateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        sectorFromDate = new DatePickerField(now.withDayOfMonth(1));
        sectorToDate = new DatePickerField(now);
        leftDateRow.add(new JLabel("From:"));
        leftDateRow.add(sectorFromDate);
        leftDateRow.add(new JLabel("To:"));
        leftDateRow.add(sectorToDate);

        // Collaborator + Refresh row
        JPanel leftCollabRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        sectorCollabCombo = new JComboBox<>(new String[]{"All"});
        JButton refreshSectorBtn = new JButton("Refresh");
        refreshSectorBtn.setFont(refreshSectorBtn.getFont().deriveFont(Font.BOLD));
        leftCollabRow.add(new JLabel("Collaborator:"));
        leftCollabRow.add(sectorCollabCombo);
        leftCollabRow.add(refreshSectorBtn);

        leftFilters.add(leftDateRow);
        leftFilters.add(leftCollabRow);
        leftPanel.add(leftFilters, BorderLayout.NORTH);

        sectorChart = new PieChartComponent();
        leftPanel.add(sectorChart, BorderLayout.CENTER);

        // =============== RIGHT COLUMN: Hours by Task ===============
        JPanel rightPanel = new JPanel(new BorderLayout(4, 4));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Hours by Task"));

        JPanel rightFilters = new JPanel();
        rightFilters.setLayout(new BoxLayout(rightFilters, BoxLayout.Y_AXIS));

        // Date row
        JPanel rightDateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        taskFromDate = new DatePickerField(now.withDayOfMonth(1));
        taskToDate = new DatePickerField(now);
        rightDateRow.add(new JLabel("From:"));
        rightDateRow.add(taskFromDate);
        rightDateRow.add(new JLabel("To:"));
        rightDateRow.add(taskToDate);

        // Collaborator + Sector + Refresh row
        JPanel rightFilterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        taskCollabCombo = new JComboBox<>(new String[]{"All"});
        taskSectorCombo = new JComboBox<>(new String[]{"All"});
        JButton refreshTaskBtn = new JButton("Refresh");
        refreshTaskBtn.setFont(refreshTaskBtn.getFont().deriveFont(Font.BOLD));
        rightFilterRow.add(new JLabel("Collaborator:"));
        rightFilterRow.add(taskCollabCombo);
        rightFilterRow.add(new JLabel("Sector:"));
        rightFilterRow.add(taskSectorCombo);
        rightFilterRow.add(refreshTaskBtn);

        rightFilters.add(rightDateRow);
        rightFilters.add(rightFilterRow);
        rightPanel.add(rightFilters, BorderLayout.NORTH);

        taskChart = new PieChartComponent();
        rightPanel.add(taskChart, BorderLayout.CENTER);

        // =============== Layout ===============
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        chartsPanel.add(leftPanel);
        chartsPanel.add(rightPanel);
        add(chartsPanel, BorderLayout.CENTER);

        // ---- Summary ----
        summaryLabel = new JLabel("Click 'Refresh' on either chart to generate a report.");
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.ITALIC, 11f));
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));
        add(summaryLabel, BorderLayout.SOUTH);

        // ---- Wiring ----
        refreshSectorBtn.addActionListener(e -> generateSectorChart());
        refreshTaskBtn.addActionListener(e -> generateTaskChart());
    }

    /**
     * Reload the collaborator and sector dropdowns from current data.
     */
    public void refreshData() {
        try {
            List<TimeEntry> all = storage.readAll();

            // Collaborators
            Set<String> collaborators = all.stream()
                    .map(TimeEntry::getCollaborator)
                    .collect(Collectors.toCollection(TreeSet::new));
            refreshCombo(sectorCollabCombo, collaborators, "All");
            refreshCombo(taskCollabCombo, collaborators, "All");

            // Sectors
            Set<String> sectors = all.stream()
                    .map(TimeEntry::getSector)
                    .collect(Collectors.toCollection(TreeSet::new));
            refreshCombo(taskSectorCombo, sectors, "All");
        } catch (Exception ignored) {
        }
    }

    private void refreshCombo(JComboBox<String> combo, Set<String> values, String defaultItem) {
        String prev = (String) combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(defaultItem);
        values.forEach(combo::addItem);
        if (prev != null) combo.setSelectedItem(prev);
    }

    // ---- Left chart: Hours by Sector ----

    private void generateSectorChart() {
        try {
            List<TimeEntry> filtered = loadFiltered(
                    sectorFromDate.getDate(), sectorToDate.getDate(),
                    (String) sectorCollabCombo.getSelectedItem(), null);

            if (filtered.isEmpty()) {
                sectorChart.setData(Collections.emptyMap());
                updateSummary(filtered, sectorFromDate.getDate(), sectorToDate.getDate());
                return;
            }

            Map<String, Double> bySector = new LinkedHashMap<>();
            for (TimeEntry e : filtered) {
                bySector.merge(e.getSector(), e.getHours(), Double::sum);
            }
            sectorChart.setData(bySector);
            updateSummary(filtered, sectorFromDate.getDate(), sectorToDate.getDate());
        } catch (Exception ex) {
            showError("Could not generate sector chart", ex);
        }
    }

    // ---- Right chart: Hours by Task ----

    private void generateTaskChart() {
        try {
            String sectorFilter = (String) taskSectorCombo.getSelectedItem();
            List<TimeEntry> filtered = loadFiltered(
                    taskFromDate.getDate(), taskToDate.getDate(),
                    (String) taskCollabCombo.getSelectedItem(),
                    "All".equals(sectorFilter) ? null : sectorFilter);

            if (filtered.isEmpty()) {
                taskChart.setData(Collections.emptyMap());
                summaryLabel.setText("No entries found for the selected task filters.");
                return;
            }

            Map<String, Double> byTask = new LinkedHashMap<>();
            for (TimeEntry e : filtered) {
                byTask.merge(e.getTask(), e.getHours(), Double::sum);
            }
            taskChart.setData(byTask);
        } catch (Exception ex) {
            showError("Could not generate task chart", ex);
        }
    }

    // ---- Helpers ----

    private List<TimeEntry> loadFiltered(LocalDate from, LocalDate to,
                                         String collabFilter, String sectorFilter) throws Exception {
        List<TimeEntry> all = storage.readAll();
        return all.stream()
                .filter(e -> !e.getDate().isBefore(from) && !e.getDate().isAfter(to))
                .filter(e -> collabFilter == null || "All".equals(collabFilter)
                        || e.getCollaborator().equalsIgnoreCase(collabFilter))
                .filter(e -> sectorFilter == null
                        || e.getSector().equalsIgnoreCase(sectorFilter))
                .toList();
    }

    private void updateSummary(List<TimeEntry> entries, LocalDate from, LocalDate to) {
        if (entries.isEmpty()) {
            summaryLabel.setText("No entries found for the selected filters.");
            return;
        }
        double totalHours = entries.stream().mapToDouble(TimeEntry::getHours).sum();
        long uniqueDays = entries.stream().map(TimeEntry::getDate).distinct().count();
        long uniqueCollaborators = entries.stream()
                .map(e -> e.getCollaborator().toLowerCase()).distinct().count();
        summaryLabel.setText(String.format(
                "Period: %s to %s  |  Entries: %d  |  Total hours: %.1f  |  "
                        + "Working days: %d  |  Collaborators: %d",
                from.format(com.pki.model.TimeEntry.DATE_FMT),
                to.format(com.pki.model.TimeEntry.DATE_FMT),
                entries.size(), totalHours, uniqueDays, uniqueCollaborators));
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(this,
                message + ":\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
