package dev.shared.milaaan.npc_rewards;

/** Reference stats of one NPC type, from the bundled wiki table. */
public class AlienEntry {
    /** Normal, Boss (x4) or Uber (x8). */
    public enum Kind { NORMAL, BOSS, UBER }

    private static final double PER_HP = 1_000;

    final String name;
    final Kind kind;
    /** Display string, e.g. "X-3, X-4" (X = your company: 1 MMO, 2 EIC, 3 VRU). */
    final String maps;
    final int hp;
    final int shield;
    final double credits;
    final double uridium;
    final double experience;
    final double honor;

    AlienEntry(String name, Kind kind, String maps, int hp, int shield,
               double credits, double uridium, double experience, double honor) {
        this.name = name;
        this.kind = kind;
        this.maps = maps;
        this.hp = hp;
        this.shield = shield;
        this.credits = credits;
        this.uridium = uridium;
        this.experience = experience;
        this.honor = honor;
    }

    int totalHp() {
        return this.hp + this.shield;
    }

    /** Reward per 1,000 HP + shield to destroy: higher = more profitable for the same damage. */
    double perThousandHp(double reward) {
        return this.totalHp() <= 0 ? 0 : reward * PER_HP / this.totalHp();
    }
}
