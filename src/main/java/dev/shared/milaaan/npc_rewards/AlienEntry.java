package dev.shared.milaaan.npc_rewards;

/** Reference stats of one NPC, read from the bundled table. Unknown values are null (HP: 0). */
public class AlienEntry {
    private static final double PER_HP = 1_000;

    String name;
    /** classic, boss, uber, pirate, blacklight, special, gate or event. */
    String category;
    /** Where to find it, e.g. "X-3, X-4" (X = your company: 1 MMO, 2 EIC, 3 VRU). */
    String maps;
    int hp;
    int shield;
    Double credits;
    Double uridium;
    Double experience;
    Double honor;

    int totalHp() {
        return this.hp + this.shield;
    }

    /** Reward per 1,000 HP + shield to destroy (higher = more profitable), or null when unknown. */
    Double perThousandHp(Double reward) {
        if (reward == null || this.hp <= 0) return null;
        return reward * PER_HP / this.totalHp();
    }
}
