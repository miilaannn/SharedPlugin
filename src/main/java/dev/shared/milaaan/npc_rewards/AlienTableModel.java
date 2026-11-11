package dev.shared.milaaan.npc_rewards;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.Locale;

/** Read-only table: one row per NPC, rewards and rewards per 1,000 HP + shield. */
final class AlienTableModel extends AbstractTableModel {
    static final int FIRST_RATIO_COLUMN = 10;
    static final int COL_URI_PER_HP = 11;

    private static final String[] COLUMN_KEYS = {
            "npc", "type", "maps", "hp", "shield", "total_hp",
            "credits", "uri", "xp", "honor",
            "credits_hp", "uri_hp", "xp_hp", "honor_hp"};
    private static final Class<?>[] TYPES = {
            String.class, String.class, String.class, Integer.class, Integer.class, Integer.class,
            Double.class, Double.class, Double.class, Double.class,
            Double.class, Double.class, Double.class, Double.class};

    private final List<AlienEntry> rows;
    private final String[] columns = new String[COLUMN_KEYS.length];
    private final Texts texts;

    AlienTableModel(List<AlienEntry> rows, Texts texts) {
        this.rows = rows;
        this.texts = texts;
        for (int i = 0; i < COLUMN_KEYS.length; i++) this.columns[i] = texts.get("col." + COLUMN_KEYS[i]);
    }

    @Override
    public int getRowCount() {
        return this.rows.size();
    }

    @Override
    public int getColumnCount() {
        return this.columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return this.columns[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return TYPES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int column) {
        AlienEntry a = this.rows.get(rowIndex);
        if (column >= FIRST_RATIO_COLUMN) return this.ratio(a, column);
        switch (column) {
            case 0: return a.name;
            case 1: return this.texts.get("type." + (a.category == null ? "event" : a.category.toLowerCase(Locale.ROOT)));
            case 2: return a.maps;
            case 3: return a.hp > 0 ? a.hp : null;
            case 4: return a.shield;
            case 5: return a.hp > 0 ? a.totalHp() : null;
            case 6: return a.credits;
            case 7: return a.uridium;
            case 8: return a.experience;
            case 9: return a.honor;
            default: return null;
        }
    }

    private Object ratio(AlienEntry a, int column) {
        if (column == FIRST_RATIO_COLUMN) return a.perThousandHp(a.credits);
        if (column == COL_URI_PER_HP) return a.perThousandHp(a.uridium);
        if (column == FIRST_RATIO_COLUMN + 2) return a.perThousandHp(a.experience);
        return a.perThousandHp(a.honor);
    }
}
