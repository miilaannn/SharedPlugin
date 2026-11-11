package dev.shared.milaaan.task.profit_stats.gui;

import dev.shared.milaaan.task.profit_stats.data.Rewards;
import dev.shared.milaaan.task.profit_stats.rank.NpcValue;

import java.util.List;
import java.util.Locale;

/** "NPC" tab: per-kill values (measured or wiki) and theoretical gains per hour for every NPC. */
final class NpcTableModel extends ReadOnlyTableModel<NpcValue> {
    static final int COL_URI_H = 11;

    private final Texts texts;
    private double overhead;

    NpcTableModel(Texts texts) {
        super(texts,
                new String[]{"npc", "kills", "hp", "ttk", "ttk_source",
                        "credits_kill", "uri_kill", "xp_kill", "honor_kill", "reward_source",
                        "credits_h", "uri_h", "xp_h", "honor_h", "kill_ticked", "maps"},
                new Class<?>[]{String.class, Integer.class, Integer.class, Double.class, String.class,
                        Double.class, Double.class, Double.class, Double.class, String.class,
                        Double.class, Double.class, Double.class, Double.class, String.class, String.class});
        this.texts = texts;
    }

    void update(List<NpcValue> values, double overheadSeconds) {
        this.overhead = overheadSeconds;
        setRows(values);
    }

    @Override
    public Object getValueAt(int rowIndex, int column) {
        NpcValue v = this.rows.get(rowIndex);
        if (column >= 10 && column <= 13) return perHour(v, column);
        switch (column) {
            case 0: return v.name;
            case 1: return v.kills;
            case 2: return v.totalHp;
            case 3: return v.ttk;
            case 4: return this.source(v.ttkSource);
            case 5: return v.perKill.credits;
            case 6: return v.perKill.uridium;
            case 7: return v.perKill.experience;
            case 8: return v.perKill.honor;
            case 9: return this.source(v.rewardSource);
            case 14: return this.texts.get(v.killable ? "yes" : "no");
            case 15: return String.join(", ", v.maps);
            default: return null;
        }
    }

    private Object perHour(NpcValue v, int column) {
        Rewards perHour = v.perHour(this.overhead);
        if (column == 10) return perHour.credits;
        if (column == 11) return perHour.uridium;
        if (column == 12) return perHour.experience;
        return perHour.honor;
    }

    private String source(NpcValue.Source source) {
        return this.texts.get("source." + source.name().toLowerCase(Locale.ROOT));
    }
}
