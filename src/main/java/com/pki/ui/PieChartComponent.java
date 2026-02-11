package com.pki.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A lightweight Swing component that draws a pie chart with a legend.
 * No external dependencies required.
 */
public class PieChartComponent extends JComponent {

    private static final Color[] PALETTE = {
            new Color(66, 133, 244),    // Blue
            new Color(234, 67, 53),     // Red
            new Color(251, 188, 4),     // Yellow
            new Color(52, 168, 83),     // Green
            new Color(255, 109, 0),     // Orange
            new Color(156, 39, 176),    // Purple
            new Color(0, 188, 212),     // Cyan
            new Color(139, 195, 74),    // Light Green
            new Color(255, 87, 34),     // Deep Orange
            new Color(63, 81, 181),     // Indigo
            new Color(233, 30, 99),     // Pink
            new Color(121, 85, 72),     // Brown
    };

    private String title = "";
    private final LinkedHashMap<String, Double> data = new LinkedHashMap<>();
    private double total = 0;

    public void setTitle(String title) {
        this.title = title;
        repaint();
    }

    public void setData(Map<String, Double> newData) {
        data.clear();
        total = 0;
        // Sort by value descending for better visual
        newData.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> {
                    data.put(e.getKey(), e.getValue());
                    total += e.getValue();
                });
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 350);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(250, 200);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Background
        g2.setColor(getBackground());
        g2.fillRect(0, 0, w, h);

        // Title
        int titleHeight = 0;
        if (title != null && !title.isEmpty()) {
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            FontMetrics fm = g2.getFontMetrics();
            titleHeight = fm.getHeight() + 8;
            g2.setColor(getForeground());
            int titleX = (w - fm.stringWidth(title)) / 2;
            g2.drawString(title, titleX, fm.getAscent() + 4);
        }

        if (data.isEmpty() || total == 0) {
            g2.setFont(getFont().deriveFont(Font.ITALIC, 12f));
            g2.setColor(Color.GRAY);
            String msg = "No data to display";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            g2.dispose();
            return;
        }

        // Layout: pie on the left, legend on the right
        int legendWidth = Math.min(180, w / 3);
        int pieAreaW = w - legendWidth - 20;
        int pieAreaH = h - titleHeight - 10;
        int pieDiameter = Math.min(pieAreaW, pieAreaH) - 20;
        if (pieDiameter < 50) pieDiameter = 50;

        int pieX = (pieAreaW - pieDiameter) / 2 + 10;
        int pieY = titleHeight + (pieAreaH - pieDiameter) / 2;

        // Draw pie slices
        double startAngle = 0;
        int colorIdx = 0;
        java.util.List<Map.Entry<String, Double>> entries = new java.util.ArrayList<>(data.entrySet());

        for (Map.Entry<String, Double> entry : entries) {
            double extent = (entry.getValue() / total) * 360.0;
            Color sliceColor = PALETTE[colorIdx % PALETTE.length];
            g2.setColor(sliceColor);
            g2.fill(new Arc2D.Double(pieX, pieY, pieDiameter, pieDiameter,
                    startAngle, extent, Arc2D.PIE));

            // Slice border
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Arc2D.Double(pieX, pieY, pieDiameter, pieDiameter,
                    startAngle, extent, Arc2D.PIE));

            // Label on slice (only if slice is large enough)
            if (extent > 15) {
                double midAngle = Math.toRadians(startAngle + extent / 2);
                double labelRadius = pieDiameter * 0.35;
                int labelX = (int) (pieX + pieDiameter / 2.0 + labelRadius * Math.cos(midAngle));
                int labelY = (int) (pieY + pieDiameter / 2.0 - labelRadius * Math.sin(midAngle));

                String pct = String.format("%.0f%%", (entry.getValue() / total) * 100);
                g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(pct);

                // Text shadow for readability
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawString(pct, labelX - textW / 2 + 1, labelY + fm.getAscent() / 2 + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(pct, labelX - textW / 2, labelY + fm.getAscent() / 2);
            }

            startAngle += extent;
            colorIdx++;
        }

        // Draw legend
        int legendX = w - legendWidth;
        int legendY = titleHeight + 15;
        g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        FontMetrics fm = g2.getFontMetrics();
        int lineHeight = fm.getHeight() + 4;

        colorIdx = 0;
        for (Map.Entry<String, Double> entry : entries) {
            Color sliceColor = PALETTE[colorIdx % PALETTE.length];

            // Color box
            g2.setColor(sliceColor);
            g2.fillRoundRect(legendX, legendY, 12, 12, 3, 3);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(legendX, legendY, 12, 12, 3, 3);

            // Label
            g2.setColor(getForeground());
            String label = String.format("%s (%.1fh)", entry.getKey(), entry.getValue());
            // Truncate if needed
            String display = label;
            int maxLabelW = legendWidth - 20;
            if (fm.stringWidth(display) > maxLabelW) {
                while (fm.stringWidth(display + "...") > maxLabelW && display.length() > 3) {
                    display = display.substring(0, display.length() - 1);
                }
                display = display + "...";
            }
            g2.drawString(display, legendX + 18, legendY + 11);

            legendY += lineHeight;
            colorIdx++;

            if (legendY > h - 20) break; // don't overflow
        }

        // Total
        g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
        g2.setColor(getForeground());
        String totalStr = String.format("Total: %.1fh", total);
        g2.drawString(totalStr, legendX, legendY + lineHeight);

        g2.dispose();
    }
}
