package com.pki.ui;

import com.pki.config.AppConfig;
import com.pki.model.TimeEntry;
import com.pki.storage.CsvStorage;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");
        totalLabel = new JLabel("Total: 0.0h");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD));
        countLabel = new JLabel("(0 entries)");
        bottomPanel.add(editButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(Box.createHorizontalStrut(20));
        bottomPanel.add(totalLabel);
        bottomPanel.add(countLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // ---- Wiring ----
        filterButton.addActionListener(e -> refreshTable());
        editButton.addActionListener(e -> editSelectedEntry());
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

    private void editSelectedEntry() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length != 1) {
            JOptionPane.showMessageDialog(this, "Please select exactly one entry to edit.",
                    "Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRows[0]);
        TimeEntry original = tableModel.getEntryAt(modelRow);

        // Build edit dialog
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), "Edit Entry", true);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.setSize(480, 340);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints lbl = new GridBagConstraints();
        lbl.anchor = GridBagConstraints.EAST;
        lbl.insets = new Insets(4, 4, 4, 4);
        GridBagConstraints fld = new GridBagConstraints();
        fld.anchor = GridBagConstraints.WEST;
        fld.fill = GridBagConstraints.HORIZONTAL;
        fld.weightx = 1;
        fld.insets = new Insets(4, 4, 4, 4);

        int row = 0;

        // Date
        DatePickerField dateField = new DatePickerField(original.getDate());
        lbl.gridy = row; fld.gridy = row;
        form.add(new JLabel("Date:"), lbl);
        form.add(dateField, fld);
        row++;

        // Collaborator
        JTextField collabField = new JTextField(original.getCollaborator(), 20);
        lbl.gridy = row; fld.gridy = row;
        form.add(new JLabel("Collaborator:"), lbl);
        form.add(collabField, fld);
        row++;

        // Sector
        JComboBox<String> sectorCombo = new JComboBox<>();
        for (String s : config.getSectorNames()) sectorCombo.addItem(s);
        sectorCombo.setSelectedItem(original.getSector());
        if (sectorCombo.getSelectedIndex() == -1) {
            sectorCombo.addItem(original.getSector());
            sectorCombo.setSelectedItem(original.getSector());
        }
        sectorCombo.setEditable(true);
        lbl.gridy = row; fld.gridy = row;
        form.add(new JLabel("Sector:"), lbl);
        form.add(sectorCombo, fld);
        row++;

        // Task
        JComboBox<String> taskCombo = new JComboBox<>();
        taskCombo.setEditable(true);
        Runnable updateTasks = () -> {
            String sec = (String) sectorCombo.getSelectedItem();
            taskCombo.removeAllItems();
            if (sec != null) {
                for (String t : config.getTasksForSector(sec)) taskCombo.addItem(t);
            }
        };
        updateTasks.run();
        taskCombo.setSelectedItem(original.getTask());
        if (taskCombo.getSelectedIndex() == -1) {
            taskCombo.addItem(original.getTask());
            taskCombo.setSelectedItem(original.getTask());
        }
        sectorCombo.addActionListener(e -> {
            String prev = (String) taskCombo.getSelectedItem();
            updateTasks.run();
            taskCombo.setSelectedItem(prev);
        });
        lbl.gridy = row; fld.gridy = row;
        form.add(new JLabel("Task:"), lbl);
        form.add(taskCombo, fld);
        row++;

        // Hours
        SpinnerNumberModel hoursModel = new SpinnerNumberModel(
                original.getHours(), 0.25, 24.0, 0.25);
        JSpinner hoursSpinner = new JSpinner(hoursModel);
        lbl.gridy = row; fld.gridy = row;
        form.add(new JLabel("Hours:"), lbl);
        form.add(hoursSpinner, fld);
        row++;

        // Description
        JTextField descField = new JTextField(original.getDescription(), 20);
        lbl.gridy = row; fld.gridy = row;
        form.add(new JLabel("Description:"), lbl);
        form.add(descField, fld);

        dialog.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        saveBtn.setFont(saveBtn.getFont().deriveFont(Font.BOLD));
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                String collab = collabField.getText().trim();
                String sector = ((String) sectorCombo.getSelectedItem());
                String task = ((String) taskCombo.getSelectedItem());
                double hours = (Double) hoursSpinner.getValue();
                String desc = descField.getText().trim();
                LocalDate date = dateField.getDate();

                if (collab.isEmpty() || sector == null || sector.isBlank()
                        || task == null || task.isBlank()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Collaborator, Sector, and Task are required.",
                            "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                TimeEntry updated = new TimeEntry(date, collab, sector.trim(),
                        task.trim(), hours, desc);
                boolean replaced = storage.replaceEntry(original, updated);
                if (!replaced) {
                    JOptionPane.showMessageDialog(dialog,
                            "Could not find the original entry (it may have been modified or deleted).",
                            "Not found", JOptionPane.WARNING_MESSAGE);
                }
                dialog.dispose();
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Could not save entry:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.getRootPane().setDefaultButton(saveBtn);
        dialog.setVisible(true);
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
