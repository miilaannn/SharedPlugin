package dev.shared.milaaan.task.profit_stats.data;

import java.util.ArrayList;
import java.util.List;

/** Small helpers for capped sample lists and medians (robust to outliers such as bonus boxes). */
public final class Samples {
    public static final int MAX_SAMPLES = 40;

    private Samples() {
    }

    public static <T> void push(List<T> list, T value) {
        list.add(value);
        while (list.size() > MAX_SAMPLES) list.remove(0);
    }

    public static double median(List<Double> values) {
        if (values == null || values.isEmpty()) return 0;
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compare);
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(mid) : (sorted.get(mid - 1) + sorted.get(mid)) / 2;
    }

    /** Median of each field independently. */
    public static Rewards medianRewards(List<Rewards> rewards) {
        if (rewards == null || rewards.isEmpty()) return new Rewards();
        List<Double> c = new ArrayList<>(rewards.size());
        List<Double> u = new ArrayList<>(rewards.size());
        List<Double> e = new ArrayList<>(rewards.size());
        List<Double> h = new ArrayList<>(rewards.size());
        for (Rewards r : rewards) {
            c.add(r.credits);
            u.add(r.uridium);
            e.add(r.experience);
            h.add(r.honor);
        }
        return new Rewards(median(c), median(u), median(e), median(h));
    }
}
