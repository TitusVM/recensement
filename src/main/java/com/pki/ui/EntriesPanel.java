package com.pki.ui;

import com.pki.config.AppConfig;
import com.pki.model.TimeEntry;
import com.pki.storage.CsvStorage;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel for viewing and managing all time entries with filters.
 */
public class EntriesPanel extends JPanel {

    private final CsvStorage storage;
    private final AppConfig config;

    private final DatePickerField fromDateField;
    private final DatePickerField toDateField;
    private final JComboBox<String> collaboratorCombo;
    private final JComboBox<String> sectorFilterCombo;
    private final EntryTableModel tableModel;
    private final JTable table;
    private final JLabel totalLabel;
    private final JLabel countLabel;

    public EntriesPanel(CsvStorage storage, AppConfig config) {
        this.storage = storage;
        this.config = config;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---- Filter bar ----
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filters"));

        LocalDate now = LocalDate.now();
        fromDateField = new DatePickerField(now.withDayOfMonth(1));
        toDateField = new DatePickerField(now);
        collaboratorCombo = new JComboBox<>(new String[]{"All"});
        sectorFilterCombo = new JComboBox<>(new String[]{"All"});

        filterPanel.add(new JLabel("From:"));
        filterPanel.add(fromDateField);
        filterPanel.add(new JLabel("To:"));
        filterPanel.add(toDateField);
        filterPanel.add(new JLabel("Collaborator:"));
        filterPanel.add(collaboratorCombo);
        filterPanel.add(new JLabel("Sector:"));
        filterPanel.add(sectorFilterCombo);

        JButton filterButton = new JButton("Apply Filter");
        filterButton.setFont(filterButton.getFont().deriveFont(Font.BOLD));
        filterPanel.add(filterButton);

        add(filterPanel, BorderLayout.NORTH);

        // ---- Table ----
        tableModel = new EntryTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(4).setMaxWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(5).setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Entries"));
        add(scrollPane, BorderLayout.CENTER);

        // ---- Bottom bar ----
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton deleteButton = new JButton("Delete Selected");
        totalLabel = new JLabel("Total: 0.0h");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD));
        countLabel = new JLabel("(0 entries)");
        bottomPanel.add(deleteButton);
        bottomPanel.add(Box.createHorizontalStrut(20));
        bottomPanel.add(totalLabel);
        bottomPanel.add(countLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // ---- Wiring ----
        filterButton.addActionListener(e -> refreshTable());
        deleteButton.addActionListener(e -> deleteSelectedEntries());
    }

    /**
     * Reload the table with all entries matching the current filters.
     * Also refreshes the collaborator and sector filter dropdowns.
     */
    public void refreshTable() {
        try {
            List<TimeEntry> all = storage.readAll();

            // Update collaborator dropdown
            java.util.Set<String> collaborators = all.stream()
                    .map(TimeEntry::getCollaborator)
                    .collect(Collectors.toCollection(java.util.TreeSet::new));
            String prevCollab = (String) collaboratorCombo.getSelectedItem();
            collaboratorCombo.removeAllItems();
            collaboratorCombo.addItem("All");
            collaborators.forEach(collaboratorCombo::addItem);
            if (prevCollab != null) collaboratorCombo.setSelectedItem(prevCollab);

            // Update sector dropdown
            java.util.Set<String> sectors = all.stream()
                    .map(TimeEntry::getSector)
                    .collect(Collectors.toCollection(java.util.TreeSet::new));
            String prevSector = (String) sectorFilterCombo.getSelectedItem();
            sectorFilterCombo.removeAllItems();
            sectorFilterCombo.addItem("All");
            sectors.forEach(sectorFilterCombo::addItem);
            if (prevSector != null) sectorFilterCombo.setSelectedItem(prevSector);

            // Apply filters
            LocalDate from = fromDateField.getDate();
            LocalDate to = toDateField.getDate();
            String collabFilter = (String) collaboratorCombo.getSelectedItem();
            String sectorFilter = (String) sectorFilterCombo.getSelectedItem();

            List<TimeEntry> filtered = all.stream()
                    .filter(e -> !e.getDate().isBefore(from) && !e.getDate().isAfter(to))
                    .filter(e -> "All".equals(collabFilter)
                            || e.getCollaborator().equalsIgnoreCase(collabFilter))
                    .filter(e -> "All".equals(sectorFilter)
                            || e.getSector().equalsIgnoreCase(sectorFilter))
                    .sorted()
                    .toList();

            tableModel.setEntries(filtered);
            totalLabel.setText(String.format("Total: %.2fh", tableModel.getTotalHours()));
            countLabel.setText("(" + filtered.size() + " entries)");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load entries:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedEntries() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select entries to delete.",
                    "No selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + selectedRows.length + " selected entry/entries?",
                "Confirm deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Convert view rows to model rows (in case table is sorted)
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    int modelRow = table.convertRowIndexToModel(selectedRows[i]);
                    TimeEntry entry = tableModel.getEntryAt(modelRow);
                    storage.removeEntry(entry);
                }
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not delete entries:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
