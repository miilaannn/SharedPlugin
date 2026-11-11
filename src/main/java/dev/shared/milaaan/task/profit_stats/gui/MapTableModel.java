package dev.shared.milaaan.task.profit_stats.gui;

import dev.shared.milaaan.task.profit_stats.data.MapRecord;
import dev.shared.milaaan.task.profit_stats.data.Rewards;

/** "Maps (measured)" tab: what was really earned per hour on each map, all sources included. */
final class MapTableModel extends ReadOnlyTableModel<MapRecord> {
    static final int COL_URI_H = 5;

    MapTableModel(Texts texts) {
        super(texts,
                new String[]{"map", "hours", "kills", "kills_h", "credits_h", "uri_h", "xp_h", "honor_h"},
                new Class<?>[]{String.class, Double.class, Integer.class, Double.class,
                        Double.class, Double.class, Double.class, Double.class});
    }

    @Override
    public Object getValueAt(int rowIndex, int column) {
        MapRecord m = this.rows.get(rowIndex);
        Rewards perHour = m.perHour();
        double hours = m.seconds / 3600;
        switch (column) {
            case 0: return m.name;
            case 1: return hours;
            case 2: return m.kills;
            case 3: return hours > 0 ? m.kills / hours : 0.0;
            case 4: return perHour.credits;
            case 5: return perHour.uridium;
            case 6: return perHour.experience;
            case 7: return perHour.honor;
            default: return null;
        }
    }
}
