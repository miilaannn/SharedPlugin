package dev.shared.milaaan.task.profit_stats.data;

import java.util.ArrayList;
import java.util.List;

/** Reference values for one NPC type, from the DarkOrbit wikis. */
public class WikiNpc {
    public String name;
    public int hp;
    public int shield;
    public double credits;
    public double uridium;
    public double experience;
    public double honor;
    /** Concrete map names, e.g. "1-3", "2-3", "3-3". */
    public List<String> maps = new ArrayList<>();

    public int totalHp() {
        return hp + shield;
    }

    public Rewards rewards() {
        return new Rewards(credits, uridium, experience, honor);
    }
}
