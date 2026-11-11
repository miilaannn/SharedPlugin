package dev.shared.milaaan.task.profit_stats.tracking;

import eu.darkbot.api.game.entities.Entity;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.other.Health;
import eu.darkbot.api.managers.HeroAPI;

/**
 * Follows the NPC the hero is attacking and detects when it dies.
 * Measures time-to-kill and the damage per second really dealt by the current setup.
 */
public class KillTracker {
    /** NPC is considered killed (not just gone) if it disappears below this HP ratio. */
    private static final double DEAD_HP_RATIO = 0.35;
    /** Removal must happen shortly after we last damaged it. */
    private static final long KILL_GRACE_MS = 4_000;
    /** Forget a target we stopped hitting for this long. */
    private static final long FORGET_MS = 10_000;
    /** Only fights started on an (almost) full HP NPC give a valid TTK. */
    private static final double FULL_FIGHT_RATIO = 0.9;
    private static final double MIN_TTK_SECONDS = 0.5;

    private Tracked tracked;

    /** Result of a confirmed kill. ttkSeconds / dps are 0 when the fight was not measured from the start. */
    public static class Kill {
        public final String name;
        public final int maxTotalHp;
        public final double ttkSeconds;
        public final double dps;

        Kill(String name, int maxTotalHp, double ttkSeconds, double dps) {
            this.name = name;
            this.maxTotalHp = maxTotalHp;
            this.ttkSeconds = ttkSeconds;
            this.dps = dps;
        }
    }

    private static class Tracked {
        int id;
        String name;
        int maxTotal;
        int startTotal;
        int lastTotal;
        double lastHpRatio;
        long firstDamage;
        long lastDamage;
        boolean fullFight;
    }

    public void reset() {
        tracked = null;
    }

    public void tick(HeroAPI hero, long now) {
        Npc target = hero.getTargetAs(Npc.class);
        if (target == null || !target.isValid() || !hero.isAttacking(target)) {
            if (tracked != null && now - Math.max(tracked.lastDamage, tracked.firstDamage) > FORGET_MS) {
                tracked = null;
            }
            return;
        }

        Health health = target.getHealth();
        int total = health.getHp() + health.getShield();
        int maxTotal = health.getMaxHp() + health.getMaxShield();

        if (tracked == null || tracked.id != target.getId()) {
            tracked = new Tracked();
            tracked.id = target.getId();
            tracked.name = npcName(target);
            tracked.maxTotal = maxTotal;
            tracked.startTotal = total;
            tracked.lastTotal = total;
            tracked.lastHpRatio = health.hpPercent();
            tracked.fullFight = maxTotal > 0 && total >= maxTotal * FULL_FIGHT_RATIO;
            tracked.lastDamage = now;
            return;
        }

        if (total < tracked.lastTotal) {
            if (tracked.firstDamage == 0) tracked.firstDamage = now;
            tracked.lastDamage = now;
        }
        tracked.lastTotal = total;
        tracked.lastHpRatio = health.hpPercent();
        if (maxTotal > 0) tracked.maxTotal = maxTotal;
    }

    /** Call when an entity disappears. Returns the kill if it was our dying target, otherwise null. */
    public Kill onEntityRemoved(Entity entity, long now) {
        if (!(entity instanceof Npc) || tracked == null || entity.getId() != tracked.id) return null;

        Tracked t = tracked;
        tracked = null;
        boolean dying = t.lastHpRatio <= DEAD_HP_RATIO;
        boolean recentlyHit = t.firstDamage != 0 && now - t.lastDamage <= KILL_GRACE_MS;
        if (!dying || !recentlyHit) return null;

        double ttk = (now - t.firstDamage) / 1000.0;
        if (!t.fullFight || ttk < MIN_TTK_SECONDS) return new Kill(t.name, t.maxTotal, 0, 0);
        return new Kill(t.name, t.maxTotal, ttk, t.startTotal / ttk);
    }

    public static String npcName(Npc npc) {
        String name = npc.getInfo() != null ? npc.getInfo().getName() : null;
        if (name == null || name.isEmpty()) name = npc.getEntityInfo().getUsername();
        return name == null || name.isEmpty() ? "NPC #" + npc.getNpcId() : name;
    }
}
