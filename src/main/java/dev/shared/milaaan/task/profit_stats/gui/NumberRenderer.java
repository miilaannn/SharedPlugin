package dev.shared.milaaan.task.profit_stats.gui;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.Locale;

/** Right-aligned numbers with thousands separators; 0 is shown as "-". */
final class NumberRenderer extends DefaultTableCellRenderer {
    NumberRenderer() {
        setHorizontalAlignment(SwingConstants.RIGHT);
    }

    static String format(double value) {
        if (value == 0) return "-";
        if (Math.abs(value) < 10) return String.format(Locale.getDefault(), "%.1f", value);
        return String.format(Locale.getDefault(), "%,.0f", value);
    }

    @Override
    protected void setValue(Object value) {
        setText(value instanceof Number ? format(((Number) value).doubleValue()) : String.valueOf(value));
    }
}
