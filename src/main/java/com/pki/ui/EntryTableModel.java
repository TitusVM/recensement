package com.pki.ui;

import com.pki.model.TimeEntry;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Reusable Swing table model for displaying {@link TimeEntry} rows.
 */
public class EntryTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Date", "Collaborator", "Sector", "Task", "Hours", "Description"};
    private final List<TimeEntry> entries = new ArrayList<>();

    public void setEntries(List<TimeEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        fireTableDataChanged();
    }

    public List<TimeEntry> getEntries() {
        return List.copyOf(entries);
    }

    public TimeEntry getEntryAt(int row) {
        return entries.get(row);
    }

    public void addEntry(TimeEntry entry) {
        entries.add(entry);
        fireTableRowsInserted(entries.size() - 1, entries.size() - 1);
    }

    public void removeEntryAt(int row) {
        entries.remove(row);
        fireTableRowsDeleted(row, row);
    }

    public double getTotalHours() {
        return entries.stream().mapToDouble(TimeEntry::getHours).sum();
    }

    // ---- AbstractTableModel ----

    @Override
    public int getRowCount() { return entries.size(); }

    @Override
    public int getColumnCount() { return COLUMNS.length; }

    @Override
    public String getColumnName(int col) { return COLUMNS[col]; }

    @Override
    public Class<?> getColumnClass(int col) {
        return col == 4 ? Double.class : String.class;
    }

    @Override
    public Object getValueAt(int row, int col) {
        TimeEntry e = entries.get(row);
        return switch (col) {
            case 0 -> e.getDate().format(com.pki.model.TimeEntry.DATE_FMT);
            case 1 -> e.getCollaborator();
            case 2 -> e.getSector();
            case 3 -> e.getTask();
            case 4 -> e.getHours();
            case 5 -> e.getDescription();
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) { return false; }
}
