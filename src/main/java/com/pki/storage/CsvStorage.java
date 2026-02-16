package com.pki.storage;

import com.pki.model.TimeEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CSV-backed storage with file locking to prevent concurrent overwrites.
 * <p>
 * Every read/write acquires a {@link FileLock} on the data file so that
 * multiple users sharing the same file (e.g. on a network drive) cannot
 * corrupt each other's data.
 */
public class CsvStorage {

    private static final String HEADER = "date;collaborator;sector;task;hours;description";

    private final Path dataFile;
    private final Path backupDir;

    public CsvStorage(Path dataFile) {
        this.dataFile = dataFile;
        this.backupDir = dataFile.getParent().resolve(".hours-backups");
    }

    public Path getDataFile() {
        return dataFile;
    }

    // ---- Read ----

    /**
     * Read all entries from the CSV file. Returns an empty list if the file
     * does not exist yet.
     */
    public synchronized List<TimeEntry> readAll() throws IOException {
        if (!Files.exists(dataFile)) {
            return new ArrayList<>();
        }
        try (FileChannel ch = FileChannel.open(dataFile, StandardOpenOption.READ);
             FileLock ignored = ch.lock(0, Long.MAX_VALUE, true)) { // shared lock
            String content = readChannel(ch);
            return parseEntries(content);
        }
    }

    // ---- Write ----

    /**
     * Add a single entry. Acquires an exclusive lock, reads the current file,
     * appends the new entry, writes everything back.
     */
    public synchronized void addEntry(TimeEntry entry) throws IOException {
        ensureFileExists();
        try (FileChannel ch = FileChannel.open(dataFile,
                StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock ignored = ch.lock()) { // exclusive lock
            String content = readChannel(ch);
            List<TimeEntry> entries = parseEntries(content);
            entries.add(entry);
            writeChannel(ch, entries);
        }
    }

    /**
     * Remove entries that match the given entry exactly.
     * Returns the number of entries removed.
     */
    public synchronized int removeEntry(TimeEntry entry) throws IOException {
        if (!Files.exists(dataFile)) return 0;
        try (FileChannel ch = FileChannel.open(dataFile,
                StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock ignored = ch.lock()) {
            String content = readChannel(ch);
            List<TimeEntry> entries = parseEntries(content);
            int before = entries.size();
            entries.removeIf(e -> e.equals(entry));
            int removed = before - entries.size();
            if (removed > 0) {
                writeChannel(ch, entries);
            }
            return removed;
        }
    }

    /**
     * Replace the first occurrence of {@code oldEntry} with {@code newEntry}.
     * Returns {@code true} if the replacement was made.
     */
    public synchronized boolean replaceEntry(TimeEntry oldEntry, TimeEntry newEntry) throws IOException {
        if (!Files.exists(dataFile)) return false;
        try (FileChannel ch = FileChannel.open(dataFile,
                StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock ignored = ch.lock()) {
            String content = readChannel(ch);
            List<TimeEntry> entries = parseEntries(content);
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).equals(oldEntry)) {
                    entries.set(i, newEntry);
                    writeChannel(ch, entries);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Replace all entries atomically. Used for bulk operations.
     */
    public synchronized void writeAll(List<TimeEntry> entries) throws IOException {
        ensureFileExists();
        try (FileChannel ch = FileChannel.open(dataFile,
                StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock ignored = ch.lock()) {
            createBackup();
            writeChannel(ch, entries);
        }
    }

    // ---- Internal helpers ----

    private void ensureFileExists() throws IOException {
        if (!Files.exists(dataFile)) {
            Files.createDirectories(dataFile.getParent());
            Files.writeString(dataFile, HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }

    public void createBackup() {
        try {
            if (Files.exists(dataFile) && Files.size(dataFile) > 0) {
                Files.createDirectories(backupDir);
                String timestamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                Path backup = backupDir.resolve("hours_backup_" + timestamp + ".csv");
                Files.copy(dataFile, backup, StandardCopyOption.REPLACE_EXISTING);
                cleanOldBackups();
            }
        } catch (IOException e) {
            System.err.println("Warning: could not create backup: " + e.getMessage());
        }
    }

    private void cleanOldBackups() throws IOException {
        // Keep only the 10 most recent backups
        List<Path> backups;
        try (var stream = Files.list(backupDir)) {
            backups = stream
                    .filter(p -> p.getFileName().toString().startsWith("hours_backup_"))
                    .sorted(Collections.reverseOrder())
                    .toList();
        }
        for (int i = 10; i < backups.size(); i++) {
            Files.deleteIfExists(backups.get(i));
        }
    }

    private String readChannel(FileChannel ch) throws IOException {
        ch.position(0);
        ByteBuffer buf = ByteBuffer.allocate((int) ch.size());
        ch.read(buf);
        buf.flip();
        return StandardCharsets.UTF_8.decode(buf).toString();
    }

    private void writeChannel(FileChannel ch, List<TimeEntry> entries) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append(System.lineSeparator());
        for (TimeEntry e : entries) {
            sb.append(e.toCsvLine()).append(System.lineSeparator());
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        ch.position(0);
        ch.write(ByteBuffer.wrap(bytes));
        ch.truncate(bytes.length);
        ch.force(true); // flush to disk
    }

    private List<TimeEntry> parseEntries(String content) {
        List<TimeEntry> entries = new ArrayList<>();
        if (content == null || content.isBlank()) return entries;
        String[] lines = content.split("\\R");
        for (int i = 1; i < lines.length; i++) { // skip header
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            try {
                entries.add(TimeEntry.fromCsvLine(line));
            } catch (Exception e) {
                System.err.println("Warning: skipping malformed line " + (i + 1) + ": " + line);
            }
        }
        return entries;
    }
}
