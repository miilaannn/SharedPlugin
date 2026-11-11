package dev.shared.milaaan.task.profit_stats.gui;

import dev.shared.milaaan.task.profit_stats.rank.Calculator;

/** "Map ranking" tab: estimated gains per hour with the current equipment, next to the measured ones. */
final class RankingTableModel extends ReadOnlyTableModel<Calculator.MapEstimate> {
    static final int COL_URI_H = 3;

    private final Texts texts;

    RankingTableModel(Texts texts) {
        super(texts,
                new String[]{"map", "kills_h", "credits_h", "uri_h", "xp_h", "honor_h",
                        "real_uri_h", "real_hours", "mix", "npcs_used"},
                new Class<?>[]{String.class, Double.class, Double.class, Double.class, Double.class, Double.class,
                        Double.class, Double.class, String.class, String.class});
        this.texts = texts;
    }

    @Override
    public Object getValueAt(int rowIndex, int column) {
        Calculator.MapEstimate e = this.rows.get(rowIndex);
        switch (column) {
            case 0: return e.map;
            case 1: return e.killsPerHour;
            case 2: return e.perHour.credits;
            case 3: return e.perHour.uridium;
            case 4: return e.perHour.experience;
            case 5: return e.perHour.honor;
            case 6: return e.measuredPerHour == null ? 0.0 : e.measuredPerHour.uridium;
            case 7: return e.hoursMeasured;
            case 8: return e.observedMix ? this.texts.get("mix.observed", e.sightings) : this.texts.get("source.wiki");
            case 9: return e.npcs;
            default: return null;
        }
    }
}
