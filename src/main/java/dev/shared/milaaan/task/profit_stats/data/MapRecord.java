package dev.shared.milaaan.task.profit_stats.data;

import java.util.HashMap;
import java.util.Map;

/** Real gains measured while the bot was running on one map. */
public class MapRecord {
    public String name;
    public double seconds;
    public int kills;
    public Rewards earned = new Rewards();
    /** Number of distinct NPCs of each type (by name) seen on this map. Gives the real NPC mix. */
    public Map<String, Integer> sightings = new HashMap<>();

    public int totalSightings() {
        int total = 0;
        for (int count : sightings.values()) total += count;
        return total;
    }

    public MapRecord() {
    }

    public MapRecord(String name) {
        this.name = name;
    }

    public Rewards perHour() {
        if (seconds < 1) return new Rewards();
        return earned.scaled(3600 / seconds);
    }
}
