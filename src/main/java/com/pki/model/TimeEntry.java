package com.pki.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * A single time-tracking entry: who worked on what, when, and for how long.
 */
public class TimeEntry implements Comparable<TimeEntry> {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LocalDate date;
    private final String collaborator;
    private final String sector;
    private final String task;
    private final double hours;
    private final String description;

    public TimeEntry(LocalDate date, String collaborator, String sector, String task, double hours) {
        this(date, collaborator, sector, task, hours, "");
    }

    public TimeEntry(LocalDate date, String collaborator, String sector, String task, double hours, String description) {
        this.date = Objects.requireNonNull(date);
        this.collaborator = sanitize(Objects.requireNonNull(collaborator));
        this.sector = sanitize(Objects.requireNonNull(sector));
        this.task = sanitize(Objects.requireNonNull(task));
        if (hours <= 0 || hours > 24) {
            throw new IllegalArgumentException("Hours must be between 0 (exclusive) and 24 (inclusive).");
        }
        this.hours = hours;
        this.description = description == null ? "" : sanitize(description);
    }

    // --- Getters ---

    public LocalDate getDate() { return date; }
    public String getCollaborator() { return collaborator; }
    public String getSector() { return sector; }
    public String getTask() { return task; }
    public double getHours() { return hours; }
    public String getDescription() { return description; }

    // --- CSV serialization ---

    public String toCsvLine() {
        return String.join(";",
                date.format(DATE_FMT),
                escapeCsv(collaborator),
                escapeCsv(sector),
                escapeCsv(task),
                String.valueOf(hours),
                escapeCsv(description));
    }

    public static TimeEntry fromCsvLine(String line) {
        String[] parts = parseCsvLine(line);
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid CSV line: " + line);
        }
        String desc = parts.length >= 6 ? parts[5].trim() : "";
        return new TimeEntry(
                LocalDate.parse(parts[0].trim(), DATE_FMT),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                Double.parseDouble(parts[4].trim()),
                desc);
    }

    // --- CSV helpers ---

    /** Strip the delimiter and trim whitespace from user input. */
    private static String sanitize(String value) {
        return value.replace(";", "").trim();
    }

    private static String escapeCsv(String value) {
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String[] parseCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ';') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    // --- Object methods ---

    @Override
    public int compareTo(TimeEntry other) {
        int cmp = this.date.compareTo(other.date);
        if (cmp != 0) return cmp;
        cmp = this.collaborator.compareToIgnoreCase(other.collaborator);
        if (cmp != 0) return cmp;
        cmp = this.sector.compareToIgnoreCase(other.sector);
        if (cmp != 0) return cmp;
        return this.task.compareToIgnoreCase(other.task);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeEntry that)) return false;
        return Double.compare(that.hours, hours) == 0
                && date.equals(that.date)
                && collaborator.equalsIgnoreCase(that.collaborator)
                && sector.equalsIgnoreCase(that.sector)
                && task.equalsIgnoreCase(that.task)
                && description.equalsIgnoreCase(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, collaborator.toLowerCase(), sector.toLowerCase(),
                task.toLowerCase(), hours, description.toLowerCase());
    }

    @Override
    public String toString() {
        String str = date + " | " + collaborator + " | " + sector + " > " + task + " | " + hours + "h";
        if (!description.isEmpty()) str += " | " + description;
        return str;
    }
}
