package com.pki.ui;

import com.pki.config.AppConfig;
import com.pki.model.TimeEntry;
import com.pki.storage.CsvStorage;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Panel for logging hours: form + table showing today's entries.
 */
public class LogHoursPanel extends JPanel {

    private final CsvStorage storage;
    private final AppConfig config;

    private final JTextField collaboratorField;
    private final DatePickerField dateField;
    private final JComboBox<String> sectorCombo;
    private final JComboBox<String> taskCombo;
    private final JSpinner hoursSpinner;
    private final JTextField descriptionField;
    private final EntryTableModel tableModel;
    private final JTable table;
    private final JLabel totalLabel;

    public LogHoursPanel(CsvStorage storage, AppConfig config) {
        this.storage = storage;
        this.config = config;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---- Form ----
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("New Entry"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Collaborator
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Collaborator:"), gbc);
        collaboratorField = new JTextField(config.getLastCollaborator(), 20);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(collaboratorField, gbc);
        row++;

        // Date
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Date:"), gbc);
        dateField = new DatePickerField(LocalDate.now());
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(dateField, gbc);
        row++;

        // Sector + New Sector button
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Sector:"), gbc);
        sectorCombo = new JComboBox<>(config.getSectorNames().toArray(new String[0]));
        JButton newSectorButton = new JButton("New Sector");
        newSectorButton.setMargin(new Insets(2, 6, 2, 6));
        JPanel sectorPanel = new JPanel(new BorderLayout(4, 0));
        sectorPanel.add(sectorCombo, BorderLayout.CENTER);
        sectorPanel.add(newSectorButton, BorderLayout.EAST);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(sectorPanel, gbc);
        row++;

        // Task + New Task button
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Task:"), gbc);
        taskCombo = new JComboBox<>();
        JButton newTaskButton = new JButton("New Task");
        newTaskButton.setMargin(new Insets(2, 6, 2, 6));
        JPanel taskPanel = new JPanel(new BorderLayout(4, 0));
        taskPanel.add(taskCombo, BorderLayout.CENTER);
        taskPanel.add(newTaskButton, BorderLayout.EAST);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(taskPanel, gbc);
        row++;

        // Hours + Description (same row)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        formPanel.add(new JLabel("Hours:"), gbc);
        hoursSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.25, 24.0, 0.25));
        hoursSpinner.setEditor(new JSpinner.NumberEditor(hoursSpinner, "0.##"));
        descriptionField = new JTextField(20);
        JPanel hoursDescPanel = new JPanel(new BorderLayout(8, 0));
        hoursDescPanel.add(hoursSpinner, BorderLayout.WEST);
        JPanel descSubPanel = new JPanel(new BorderLayout(4, 0));
        descSubPanel.add(new JLabel("Description (optional):"), BorderLayout.WEST);
        descSubPanel.add(descriptionField, BorderLayout.CENTER);
        hoursDescPanel.add(descSubPanel, BorderLayout.CENTER);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(hoursDescPanel, gbc);
        row++;

        // Add button
        JButton addButton = new JButton("Add Entry");
        addButton.setFont(addButton.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(12, 8, 5, 8);
        formPanel.add(addButton, gbc);

        add(formPanel, BorderLayout.NORTH);

        // ---- Table ----
        tableModel = new EntryTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(4).setMaxWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        // Right-align hours column
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(5).setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Your entries for the selected date"));
        add(scrollPane, BorderLayout.CENTER);

        // ---- Bottom bar ----
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");
        totalLabel = new JLabel("Total: 0.0h");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD));
        bottomPanel.add(editButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(Box.createHorizontalStrut(20));
        bottomPanel.add(totalLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // ---- Wiring ----

        // Update tasks when sector changes
        sectorCombo.addActionListener(e -> updateTaskCombo());
        updateTaskCombo(); // initial populate

        // Add button
        addButton.addActionListener(e -> addEntry());

        // Edit button
        editButton.addActionListener(e -> editSelectedEntry());

        // Delete button
        deleteButton.addActionListener(e -> deleteSelectedEntry());

        // New Task button
        newTaskButton.addActionListener(e -> createNewTask());

        // New Sector button
        newSectorButton.addActionListener(e -> createNewSector());

        // Refresh table when date or collaborator changes
        dateField.addDateChangeListener(e -> refreshTable());
        collaboratorField.addActionListener(e -> {
            config.setLastCollaborator(collaboratorField.getText().trim());
            config.savePreferences();
            refreshTable();
        });

        // Initial refresh
        refreshTable();
    }

    private void updateTaskCombo() {
        String sector = (String) sectorCombo.getSelectedItem();
        taskCombo.removeAllItems();
        if (sector != null) {
            for (String task : config.getTasksForSector(sector)) {
                taskCombo.addItem(task);
            }
        }
    }

    private void createNewTask() {
        String sector = (String) sectorCombo.getSelectedItem();
        if (sector == null) {
            JOptionPane.showMessageDialog(this, "Please select a sector first.",
                    "No sector", JOptionPane.WARNING_MESSAGE);
            return;
        }
        promptAndAddTask(sector);
    }

    private void createNewSector() {
        String sectorName = JOptionPane.showInputDialog(this,
                "New sector name:",
                "New Sector", JOptionPane.PLAIN_MESSAGE);
        if (sectorName == null || sectorName.trim().isEmpty()) return;
        sectorName = sectorName.replace(";", "").trim(); // sanitize
        if (config.sectorExists(sectorName)) {
            JOptionPane.showMessageDialog(this,
                    "Sector \"" + sectorName + "\" already exists.",
                    "Duplicate sector", JOptionPane.INFORMATION_MESSAGE);
            sectorCombo.setSelectedItem(sectorName);
            return;
        }
        // Prompt for the first task in this new sector
        String taskName = JOptionPane.showInputDialog(this,
                "Every sector needs at least one task.\n"
                        + "Enter the first task for \"" + sectorName + "\":",
                "New Task for \"" + sectorName + "\"", JOptionPane.PLAIN_MESSAGE);
        if (taskName == null || taskName.trim().isEmpty()) return;
        taskName = taskName.replace(";", "").trim(); // sanitize
        try {
            config.addTask(sectorName, taskName);
            sectorCombo.addItem(sectorName);
            sectorCombo.setSelectedItem(sectorName);
            // updateTaskCombo is triggered by the selection change
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not create sector:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Prompt the user for a new task name in the given sector, add it, and select it.
     */
    private void promptAndAddTask(String sector) {
        String taskName = JOptionPane.showInputDialog(this,
                "New task name for sector \"" + sector + "\":",
                "New Task", JOptionPane.PLAIN_MESSAGE);
        if (taskName == null || taskName.trim().isEmpty()) return;
        taskName = taskName.replace(";", "").trim(); // sanitize
        try {
            if (config.addTask(sector, taskName)) {
                updateTaskCombo();
                taskCombo.setSelectedItem(taskName);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Task \"" + taskName + "\" already exists in sector \"" + sector + "\".",
                        "Duplicate task", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not create task:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addEntry() {
        String collaborator = collaboratorField.getText().trim();
        if (collaborator.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your name.",
                    "Missing collaborator", JOptionPane.WARNING_MESSAGE);
            collaboratorField.requestFocus();
            return;
        }

        LocalDate date = dateField.getDate();

        String sector = (String) sectorCombo.getSelectedItem();
        String task = (String) taskCombo.getSelectedItem();
        if (sector == null || task == null) {
            JOptionPane.showMessageDialog(this, "Please select a sector and task.",
                    "Missing selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double hours = (Double) hoursSpinner.getValue();

        try {
            String description = descriptionField.getText().trim();
            TimeEntry entry = new TimeEntry(date, collaborator, sector, task, hours, description);
            storage.addEntry(entry);
            config.setLastCollaborator(collaborator);
            config.savePreferences();
            hoursSpinner.setValue(1.0);
            descriptionField.setText("");
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save entry:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelectedEntry() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an entry to edit.",
                    "No selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        TimeEntry original = tableModel.getEntryAt(selectedRow);

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

        int r = 0;

        // Date
        DatePickerField editDateField = new DatePickerField(original.getDate());
        lbl.gridy = r; fld.gridy = r;
        form.add(new JLabel("Date:"), lbl);
        form.add(editDateField, fld);
        r++;

        // Collaborator
        JTextField editCollabField = new JTextField(original.getCollaborator(), 20);
        lbl.gridy = r; fld.gridy = r;
        form.add(new JLabel("Collaborator:"), lbl);
        form.add(editCollabField, fld);
        r++;

        // Sector
        JComboBox<String> editSectorCombo = new JComboBox<>();
        for (String s : config.getSectorNames()) editSectorCombo.addItem(s);
        editSectorCombo.setSelectedItem(original.getSector());
        if (editSectorCombo.getSelectedIndex() == -1) {
            editSectorCombo.addItem(original.getSector());
            editSectorCombo.setSelectedItem(original.getSector());
        }
        editSectorCombo.setEditable(true);
        lbl.gridy = r; fld.gridy = r;
        form.add(new JLabel("Sector:"), lbl);
        form.add(editSectorCombo, fld);
        r++;

        // Task
        JComboBox<String> editTaskCombo = new JComboBox<>();
        editTaskCombo.setEditable(true);
        Runnable populateTasks = () -> {
            String sec = (String) editSectorCombo.getSelectedItem();
            editTaskCombo.removeAllItems();
            if (sec != null) {
                for (String t : config.getTasksForSector(sec)) editTaskCombo.addItem(t);
            }
        };
        populateTasks.run();
        editTaskCombo.setSelectedItem(original.getTask());
        if (editTaskCombo.getSelectedIndex() == -1) {
            editTaskCombo.addItem(original.getTask());
            editTaskCombo.setSelectedItem(original.getTask());
        }
        editSectorCombo.addActionListener(ev -> {
            String prev = (String) editTaskCombo.getSelectedItem();
            populateTasks.run();
            editTaskCombo.setSelectedItem(prev);
        });
        lbl.gridy = r; fld.gridy = r;
        form.add(new JLabel("Task:"), lbl);
        form.add(editTaskCombo, fld);
        r++;

        // Hours
        SpinnerNumberModel hModel = new SpinnerNumberModel(
                original.getHours(), 0.25, 24.0, 0.25);
        JSpinner editHoursSpinner = new JSpinner(hModel);
        lbl.gridy = r; fld.gridy = r;
        form.add(new JLabel("Hours:"), lbl);
        form.add(editHoursSpinner, fld);
        r++;

        // Description
        JTextField editDescField = new JTextField(original.getDescription(), 20);
        lbl.gridy = r; fld.gridy = r;
        form.add(new JLabel("Description:"), lbl);
        form.add(editDescField, fld);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        saveBtn.setFont(saveBtn.getFont().deriveFont(Font.BOLD));
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        cancelBtn.addActionListener(ev -> dialog.dispose());
        saveBtn.addActionListener(ev -> {
            try {
                String collab = editCollabField.getText().trim();
                String sector = (String) editSectorCombo.getSelectedItem();
                String task = (String) editTaskCombo.getSelectedItem();
                double hours = (Double) editHoursSpinner.getValue();
                String desc = editDescField.getText().trim();
                LocalDate date = editDateField.getDate();

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

    private void deleteSelectedEntry() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an entry to delete.",
                    "No selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        TimeEntry entry = tableModel.getEntryAt(selectedRow);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this entry?\n" + entry,
                "Confirm deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                storage.removeEntry(entry);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not delete entry:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Reload the table with entries matching the current collaborator and date.
     */
    public void refreshTable() {
        try {
            String collaborator = collaboratorField.getText().trim();
            final LocalDate filterDate = dateField.getDate();
            List<TimeEntry> all = storage.readAll();
            List<TimeEntry> filtered = all.stream()
                    .filter(e -> e.getDate().equals(filterDate))
                    .filter(e -> collaborator.isEmpty()
                            || e.getCollaborator().equalsIgnoreCase(collaborator))
                    .sorted()
                    .toList();
            tableModel.setEntries(filtered);
            totalLabel.setText(String.format("Total: %.2fh", tableModel.getTotalHours()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load entries:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
