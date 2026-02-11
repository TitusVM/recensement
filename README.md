# Hours Tracker

A simple, reliable Java desktop application for tracking how team members spend their time across sectors and tasks. Designed to replace fragile Excel macros with a clean UI and safe CSV storage.

> **This tool is NOT for verifying work time.** Its sole purpose is to help teams visualize work distribution — because pie charts are more convincing than words.

---

## Quick Start

### Prerequisites
- **Java 17+** (JDK for building, JRE for running)
- **Maven 3.6+** (for building only)

### Build
```bash
mvn clean package
```

### Run
```bash
java -jar target/recensement-1.0-SNAPSHOT.jar
```

On first launch, the application creates two files in the working directory:
- `sectors.csv` — sector/task configuration (edit to customize)
- `hours.csv` — the shared data file (all entries go here)

---

## How It Works

### 1. Log Hours (Tab 1)
1. Enter your **name** (remembered automatically for next time).
2. Pick the **date** (defaults to today, format: `yyyy-MM-dd`).
3. Select the **Sector** and **Task** from the dropdowns.
4. Enter **hours** (0.25 increments, e.g., 0.5, 1, 2.5).
5. Click **Add Entry**. The entry is saved to `hours.csv` immediately.
6. Repeat for each task you worked on — your name and date stay filled in.

### 2. All Entries (Tab 2)
- View, filter, and delete entries.
- Filter by date range, collaborator, and sector.
- Columns are sortable (click headers).

### 3. Reports (Tab 3)
- Set a date range and optional collaborator filter.
- Click **Generate Report** to see:
  - **Pie chart: Hours by Sector** — how time is distributed across sectors.
  - **Pie chart: Hours by Task** — detailed breakdown by sector > task.
  - **Summary** — total hours, working days, number of collaborators.

---

## Customizing Sectors and Tasks

Edit `sectors.csv` in the data directory. Format:

```csv
# Lines starting with # are comments
Sector;Task
Development;Backend
Development;Frontend
Testing;Unit Tests
Administration;Meetings
```

Add or remove lines as needed. Comma-separated lines are also accepted for backward compatibility. Changes take effect on next application start.

---

## Shared / Team Setup

For a team sharing the same data file:

1. Place the built JAR and `sectors.csv` in a **shared network folder**.
2. Each team member runs:
   ```bash
   java -jar \\server\shared\hours-tracker\recensement-1.0-SNAPSHOT.jar
   ```
   Or set the data directory explicitly:
   ```bash
   java -Ddata.dir="\\server\shared\hours-data" -jar recensement-1.0-SNAPSHOT.jar
   ```
   Or via environment variable:
   ```bash
   set HOURS_TRACKER_DIR=\\server\shared\hours-data
   java -jar recensement-1.0-SNAPSHOT.jar
   ```
3. All team members read/write to the same `hours.csv`.
4. **File locking** prevents concurrent overwrites — the app acquires an exclusive lock on the CSV file during every write operation.

---

## Data Safety

- **File locking**: Every read uses a shared lock, every write uses an exclusive lock (`java.nio.channels.FileLock`). Two users cannot corrupt the file simultaneously.
- **Automatic backups**: A timestamped backup of `hours.csv` is created every time the application starts, and before bulk operations (like multi-delete). Backups are stored in `.hours-backups/`; the 10 most recent are kept.
- **Human-readable CSV**: The data file is plain CSV (semicolon-delimited for European locale compatibility) — you can open it in any spreadsheet application or text editor if needed.
- **Input sanitization**: Semicolons are automatically stripped from user input to prevent delimiter conflicts.

---

## CSV Format

`hours.csv`:
```csv
date;collaborator;sector;task;hours;description
2026-02-11;Alice;Development;Backend;4.0;API refactoring
2026-02-11;Alice;Administration;Meetings;2.0;
2026-02-11;Bob;Testing;Unit Tests;6.5;Coverage improvements
```

---

## Configuration Summary

| Setting | Method | Default |
|---|---|---|
| Data directory | `-Ddata.dir=...` or `HOURS_TRACKER_DIR` env var | Current working directory |
| Collaborator name | Entered in UI, saved to `~/.hours-tracker.prefs` | System username (`user.name`) |

> **Note:** The collaborator name is stored per-user in the home directory (`C:\Users\<username>\.hours-tracker.prefs` on Windows, `~/.hours-tracker.prefs` on Linux/macOS). It is saved automatically whenever you add an entry, so you only need to type your name once. This file is **not** in the shared data directory — each team member has their own.
| Sectors & tasks | `sectors.csv` in data directory | Auto-generated defaults |

---

## Architecture

```
com.pki
├── Main.java                 Entry point
├── config/
│   └── AppConfig.java        Sector config + user preferences
├── model/
│   └── TimeEntry.java        Data model (date, collaborator, sector, task, hours)
├── storage/
│   └── CsvStorage.java       CSV read/write with file locking
└── ui/
    ├── MainFrame.java         Main window (tabbed)
    ├── LogHoursPanel.java     Hour entry form + daily table
    ├── EntriesPanel.java      All entries with filters
    ├── ReportPanel.java       Pie charts + summary
    ├── PieChartComponent.java Custom pie chart (pure Java2D)
    └── EntryTableModel.java   Swing table model
```

**Zero external dependencies** — only the Java 17 standard library (Swing, NIO, java.time).