package com.pki.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Application configuration: sector/task definitions and user preferences.
 * <p>
 * Sectors are loaded from a {@code sectors.csv} file in the data directory.
 * If the file does not exist, a default configuration is created.
 * <p>
 * User preferences (collaborator name) are stored per-user in the home directory.
 */
public class AppConfig {

    private final Path dataDir;
    private final Path sectorsFile;
    private final Path prefsFile;

    private final LinkedHashMap<String, List<String>> sectors = new LinkedHashMap<>();
    private String lastCollaborator = System.getProperty("user.name", "");

    public AppConfig(Path dataDir) {
        this.dataDir = dataDir;
        this.sectorsFile = dataDir.resolve("sectors.csv");
        this.prefsFile = Paths.get(System.getProperty("user.home"), ".hours-tracker.prefs");
    }

    // ---- Load ----

    public void load() throws IOException {
        loadSectors();
        loadPreferences();
    }

    private void loadSectors() throws IOException {
        if (!Files.exists(sectorsFile)) {
            createDefaultSectors();
        }
        sectors.clear();
        List<String> lines = Files.readAllLines(sectorsFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split(";", 2);
            if (parts.length < 2) {
                // Fallback: also accept comma for backward compatibility
                parts = line.split(",", 2);
            }
            if (parts.length == 2) {
                String sector = parts[0].trim();
                String task = parts[1].trim();
                if (!sector.isEmpty() && !task.isEmpty()) {
                    sectors.computeIfAbsent(sector, k -> new ArrayList<>()).add(task);
                }
            }
        }
        if (sectors.isEmpty()) {
            createDefaultSectors();
            loadSectors(); // reload
        }
    }

    private void createDefaultSectors() throws IOException {
        Files.createDirectories(dataDir);
        String defaults = """
                # Hours Tracker - Sector and Task Configuration
                # Format: Sector,Task (one pair per line)
                # Lines starting with # are comments.
                # Edit this file to add, remove, or rename sectors and tasks.
                # Changes take effect when the application is restarted.
                #
                Development,Backend
                Development,Frontend
                Development,Code Review
                Development,Bug Fixing
                Development,Architecture / Design
                Testing,Unit Tests
                Testing,Integration Tests
                Testing,Manual Testing
                Administration,Meetings
                Administration,Documentation
                Administration,Planning
                Administration,Training
                Support,Customer Support
                Support,Internal Support
                Support,Incident Response
                """;
        Files.writeString(sectorsFile, defaults, StandardCharsets.UTF_8);
    }

    private void loadPreferences() {
        try {
            if (Files.exists(prefsFile)) {
                Properties props = new Properties();
                try (var reader = Files.newBufferedReader(prefsFile, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
                lastCollaborator = props.getProperty("collaborator", lastCollaborator);
            }
        } catch (IOException e) {
            System.err.println("Warning: could not load preferences: " + e.getMessage());
        }
    }

    // ---- Save ----

    public void savePreferences() {
        try {
            Properties props = new Properties();
            props.setProperty("collaborator", lastCollaborator);
            try (var writer = Files.newBufferedWriter(prefsFile, StandardCharsets.UTF_8)) {
                props.store(writer, "Hours Tracker User Preferences");
            }
        } catch (IOException e) {
            System.err.println("Warning: could not save preferences: " + e.getMessage());
        }
    }

    // ---- Accessors ----

    public LinkedHashMap<String, List<String>> getSectors() {
        return sectors;
    }

    public List<String> getSectorNames() {
        return new ArrayList<>(sectors.keySet());
    }

    /** Check if a sector already exists (case-insensitive). */
    public boolean sectorExists(String sector) {
        if (sector == null) return false;
        return sectors.keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase(sector.trim()));
    }

    public List<String> getTasksForSector(String sector) {
        return sectors.getOrDefault(sector, Collections.emptyList());
    }

    public String getLastCollaborator() {
        return lastCollaborator;
    }

    public void setLastCollaborator(String name) {
        this.lastCollaborator = name;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path getSectorsFile() {
        return sectorsFile;
    }

    /**
     * Add a new task to the given sector, updating both the in-memory map
     * and the sectors.csv file on disk.
     *
     * @return true if the task was added, false if it already exists.
     */
    public boolean addTask(String sector, String task) throws IOException {
        if (sector == null || task == null) return false;
        sector = sector.trim();
        task = task.trim();
        if (sector.isEmpty() || task.isEmpty()) return false;

        List<String> tasks = sectors.computeIfAbsent(sector, k -> new ArrayList<>());
        // Check for duplicate (case-insensitive)
        String finalTask = task;
        if (tasks.stream().anyMatch(t -> t.equalsIgnoreCase(finalTask))) {
            return false;
        }
        tasks.add(task);

        // Append to sectors.csv
        String line = sector + ";" + task + System.lineSeparator();
        Files.writeString(sectorsFile, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return true;
    }
}
