package dev.shared.milaaan.task.profit_stats.rank;

import dev.shared.milaaan.task.profit_stats.data.Rewards;

import java.util.Collections;
import java.util.Set;

/** Best known values for one NPC type: measured when available, otherwise from the wiki table. */
public class NpcValue {
    /** Where a value comes from. */
    public enum Source { MEASURED, ESTIMATED, WIKI, UNKNOWN }

    public String name;
    public int kills;
    public int totalHp;
    public Source hpSource = Source.UNKNOWN;
    public Rewards perKill = new Rewards();
    public Source rewardSource = Source.UNKNOWN;
    /** Seconds; 0 when unknown. */
    public double ttk;
    public Source ttkSource = Source.UNKNOWN;
    public boolean killable = true;
    public Set<String> maps = Collections.emptySet();

    public boolean hasTtk() {
        return ttk > 0;
    }

    /** Theoretical gains in 1 h when killing only this NPC. */
    public Rewards perHour(double overheadSeconds) {
        if (!hasTtk()) return new Rewards();
        return perKill.scaled(3600 / (ttk + overheadSeconds));
    }
}
