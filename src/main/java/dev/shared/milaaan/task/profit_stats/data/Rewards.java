package dev.shared.milaaan.task.profit_stats.data;

/** Credits, uridium, experience and honor amounts. Mutable, not thread-safe. */
public class Rewards {
    public double credits;
    public double uridium;
    public double experience;
    public double honor;

    public Rewards() {
    }

    public Rewards(double credits, double uridium, double experience, double honor) {
        this.credits = credits;
        this.uridium = uridium;
        this.experience = experience;
        this.honor = honor;
    }

    public Rewards copy() {
        return new Rewards(credits, uridium, experience, honor);
    }

    public void add(Rewards other) {
        credits += other.credits;
        uridium += other.uridium;
        experience += other.experience;
        honor += other.honor;
    }

    public Rewards scaled(double factor) {
        return new Rewards(credits * factor, uridium * factor, experience * factor, honor * factor);
    }

    public boolean isEmpty() {
        return credits == 0 && uridium == 0 && experience == 0 && honor == 0;
    }
}
