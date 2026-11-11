package dev.shared.milaaan.npc_rewards;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.Locale;

/** Right-aligned numbers: thousands separators, or 2 decimals for ratios. */
final class NumberRenderer extends DefaultTableCellRenderer {
    private final boolean decimals;

    NumberRenderer(boolean decimals) {
        this.decimals = decimals;
        setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    protected void setValue(Object value) {
        if (!(value instanceof Number)) {
            setText(value == null ? "?" : String.valueOf(value));
            return;
        }
        double v = ((Number) value).doubleValue();
        String pattern = this.decimals ? "%,.2f" : "%,.0f";
        setText(String.format(Locale.getDefault(), pattern, v));
    }
}
