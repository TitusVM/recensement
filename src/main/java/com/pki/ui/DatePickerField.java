package com.pki.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * A date picker field: text field (editable) + button that opens a calendar popup.
 * Provides {@link #getDate()} and {@link #setDate(LocalDate)} for typed access.
 * Fires an {@link ActionEvent} when the date changes (via picker or Enter key).
 */
public class DatePickerField extends JPanel {

    private final JTextField textField;
    private final JButton prevDayBtn;
    private final JButton nextDayBtn;
    private final JButton calendarBtn;
    private JPopupMenu popup;
    private LocalDate selectedDate;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public DatePickerField(LocalDate initialDate) {
        super(new BorderLayout(0, 0));
        this.selectedDate = initialDate != null ? initialDate : LocalDate.now();

        // ◀ button (previous day)
        prevDayBtn = new JButton("\u25C0");
        prevDayBtn.setMargin(new Insets(1, 3, 1, 3));
        prevDayBtn.setFocusable(false);
        prevDayBtn.setToolTipText("Previous day");

        textField = new JTextField(selectedDate.format(FMT), 10);
        textField.setToolTipText("dd.MM.yyyy — use \u25C0 \u25B6 or arrow keys to step by day");

        // ▶ button (next day)
        nextDayBtn = new JButton("\u25B6");
        nextDayBtn.setMargin(new Insets(1, 3, 1, 3));
        nextDayBtn.setFocusable(false);
        nextDayBtn.setToolTipText("Next day");

        // ▼ calendar button
        calendarBtn = new JButton("\u25BC");
        calendarBtn.setMargin(new Insets(1, 4, 1, 4));
        calendarBtn.setFocusable(false);
        calendarBtn.setToolTipText("Open calendar");

        // Layout: [◀] [text field] [▶] [▼]
        JPanel rightButtons = new JPanel(new GridLayout(1, 2, 0, 0));
        rightButtons.add(nextDayBtn);
        rightButtons.add(calendarBtn);

        add(prevDayBtn, BorderLayout.WEST);
        add(textField, BorderLayout.CENTER);
        add(rightButtons, BorderLayout.EAST);

        // Arrow buttons
        prevDayBtn.addActionListener(e -> stepDay(-1));
        nextDayBtn.addActionListener(e -> stepDay(1));

        // Open popup on calendar button click
        calendarBtn.addActionListener(e -> togglePopup());

        // Parse date on Enter / focus lost
        textField.addActionListener(e -> parseTextField());
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                parseTextField();
            }
        });

        // Keyboard arrow keys: LEFT = previous day, RIGHT = next day
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT && e.isAltDown()) {
                    stepDay(-1);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && e.isAltDown()) {
                    stepDay(1);
                    e.consume();
                }
            }
        });
    }

    public DatePickerField() {
        this(LocalDate.now());
    }

    // ---- Public API ----

    public LocalDate getDate() {
        parseTextField();
        return selectedDate;
    }

    public void setDate(LocalDate date) {
        this.selectedDate = date != null ? date : LocalDate.now();
        textField.setText(selectedDate.format(FMT));
    }

    /** Get the raw text (for backward compat with code that read getText()). */
    public String getText() {
        return textField.getText().trim();
    }

    /** Register a listener called when the date changes. */
    public void addDateChangeListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        prevDayBtn.setEnabled(enabled);
        nextDayBtn.setEnabled(enabled);
        calendarBtn.setEnabled(enabled);
    }

    // ---- Internal ----

    private void stepDay(int days) {
        parseTextField(); // sync first
        selectedDate = selectedDate.plusDays(days);
        textField.setText(selectedDate.format(FMT));
        fireDateChanged();
    }

    private void parseTextField() {
        try {
            LocalDate parsed = LocalDate.parse(textField.getText().trim(), FMT);
            if (!parsed.equals(selectedDate)) {
                selectedDate = parsed;
                fireDateChanged();
            }
        } catch (Exception ignored) {
            // Restore last valid date
            textField.setText(selectedDate.format(FMT));
        }
    }

    private void fireDateChanged() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dateChanged");
        for (ActionListener l : listenerList.getListeners(ActionListener.class)) {
            l.actionPerformed(event);
        }
    }

    private void togglePopup() {
        if (popup != null && popup.isVisible()) {
            popup.setVisible(false);
            return;
        }
        parseTextField(); // sync before opening
        showCalendarPopup(selectedDate);
    }

    private void showCalendarPopup(LocalDate displayMonth) {
        if (popup != null) popup.setVisible(false);
        popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(buildCalendarPanel(YearMonth.from(displayMonth)));
        popup.show(this, 0, getHeight());
    }

    private JPanel buildCalendarPanel(YearMonth yearMonth) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.setBackground(Color.WHITE);

        // ---- Navigation header ----
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JButton prevBtn = flatButton("\u25C0"); // ◀
        JButton nextBtn = flatButton("\u25B6"); // ▶
        JLabel monthLabel = new JLabel(
                yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                        + " " + yearMonth.getYear(),
                SwingConstants.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 12f));

        prevBtn.addActionListener(e -> showCalendarPopup(yearMonth.minusMonths(1).atDay(1)));
        nextBtn.addActionListener(e -> showCalendarPopup(yearMonth.plusMonths(1).atDay(1)));

        header.add(prevBtn, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextBtn, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // ---- Day grid ----
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setOpaque(false);

        // Day-of-week headers
        DayOfWeek[] dow = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
        for (DayOfWeek d : dow) {
            JLabel lbl = new JLabel(d.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
            lbl.setForeground(Color.GRAY);
            grid.add(lbl);
        }

        // Leading blanks
        LocalDate first = yearMonth.atDay(1);
        int startDow = first.getDayOfWeek().getValue(); // Mon=1
        for (int i = 1; i < startDow; i++) {
            grid.add(new JLabel(""));
        }

        // Day buttons
        LocalDate today = LocalDate.now();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            JButton dayBtn = new JButton(String.valueOf(day));
            dayBtn.setMargin(new Insets(2, 2, 2, 2));
            dayBtn.setFont(dayBtn.getFont().deriveFont(Font.PLAIN, 11f));
            dayBtn.setFocusable(false);
            dayBtn.setContentAreaFilled(false);
            dayBtn.setBorderPainted(true);
            dayBtn.setOpaque(true);

            if (date.equals(selectedDate)) {
                dayBtn.setBackground(new Color(66, 133, 244));
                dayBtn.setForeground(Color.WHITE);
                dayBtn.setContentAreaFilled(true);
            } else if (date.equals(today)) {
                dayBtn.setBorder(BorderFactory.createLineBorder(new Color(66, 133, 244), 1));
                dayBtn.setBackground(Color.WHITE);
            } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                dayBtn.setForeground(new Color(180, 180, 180));
                dayBtn.setBackground(Color.WHITE);
            } else {
                dayBtn.setBackground(Color.WHITE);
            }

            dayBtn.addActionListener(e -> {
                selectedDate = date;
                textField.setText(date.format(FMT));
                popup.setVisible(false);
                fireDateChanged();
            });

            grid.add(dayBtn);
        }

        panel.add(grid, BorderLayout.CENTER);

        // ---- Today button ----
        JButton todayBtn = new JButton("Today");
        todayBtn.setFont(todayBtn.getFont().deriveFont(Font.PLAIN, 10f));
        todayBtn.setFocusable(false);
        todayBtn.addActionListener(e -> {
            selectedDate = today;
            textField.setText(today.format(FMT));
            popup.setVisible(false);
            fireDateChanged();
        });
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        bottomPanel.setOpaque(false);
        bottomPanel.add(todayBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static JButton flatButton(String text) {
        JButton btn = new JButton(text);
        btn.setMargin(new Insets(1, 6, 1, 6));
        btn.setFocusable(false);
        btn.setContentAreaFilled(false);
        return btn;
    }
}
