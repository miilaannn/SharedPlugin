package dev.shared.milaaan.task.profit_stats.tracking;

import dev.shared.milaaan.task.profit_stats.data.Rewards;
import dev.shared.milaaan.task.profit_stats.data.StatsStore;

/**
 * Attributes gains received just after a kill to that NPC.
 * Samples are discarded when another source may have mixed in (bonus box, overlapping kills).
 */
public class RewardTracker {
    /** Two kills closer than this cannot be told apart. */
    private static final long OVERLAP_MS = 1_000;

    private final StatsStore store;
    private Pending pending;

    private static class Pending {
        String npc;
        long start;
        long deadline;
        Rewards gains = new Rewards();
        boolean contaminated;
    }

    public RewardTracker(StatsStore store) {
        this.store = store;
    }

    public void onKill(String npc, long now, long windowMs) {
        boolean overlap = false;
        if (pending != null) {
            overlap = now - pending.start < OVERLAP_MS;
            if (overlap) pending.contaminated = true;
            finish();
        }
        pending = new Pending();
        pending.npc = npc;
        pending.start = now;
        pending.deadline = now + windowMs;
        pending.contaminated = overlap;
    }

    /** A bonus box (or similar) was collected near the hero: gains are no longer NPC-only. */
    public void onOtherGainSource() {
        if (pending != null) pending.contaminated = true;
    }

    public void onGains(Rewards gains, long now) {
        if (pending == null) return;
        pending.gains.add(gains);
        if (now >= pending.deadline) finish();
    }

    public void cancel() {
        pending = null;
    }

    private void finish() {
        if (!pending.contaminated) store.recordReward(pending.npc, pending.gains);
        pending = null;
    }
}
