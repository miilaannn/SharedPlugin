package dev.shared.milaaan.task.profit_stats.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Everything learned about one NPC type (grouped by name). */
public class NpcRecord {
    public String name;
    public int kills;
    /** Max HP + max shield, last seen. */
    public int maxTotalHp;
    public List<Rewards> rewardSamples = new ArrayList<>();
    /** Time-to-kill samples in seconds, per setup key (ship + configuration). */
    public Map<String, List<Double>> ttkBySetup = new HashMap<>();
    public Set<String> maps = new LinkedHashSet<>();
    /** "Kill" checkbox of the DarkBot NPC list, last seen. null = never seen in game. */
    public Boolean shouldKill;

    public NpcRecord() {
    }

    public NpcRecord(String name) {
        this.name = name;
    }

    public Rewards medianRewards() {
        return Samples.medianRewards(rewardSamples);
    }

    public double medianTtk(String setupKey) {
        return Samples.median(ttkBySetup.get(setupKey));
    }

    public int ttkSampleCount(String setupKey) {
        List<Double> list = ttkBySetup.get(setupKey);
        return list == null ? 0 : list.size();
    }
}
