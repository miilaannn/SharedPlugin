package dev.shared.milaaan.task.profit_stats.rank;

import dev.shared.milaaan.task.profit_stats.data.MapRecord;
import dev.shared.milaaan.task.profit_stats.data.NpcNames;
import dev.shared.milaaan.task.profit_stats.data.NpcRecord;
import dev.shared.milaaan.task.profit_stats.data.Rewards;
import dev.shared.milaaan.task.profit_stats.data.WikiData;
import dev.shared.milaaan.task.profit_stats.data.WikiNpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Combines measured data and the wiki table into per-NPC values and an estimated per-map ranking
 * for the current equipment (DPS). Pure logic, no game access.
 */
public class Calculator {
    /** Below this many clean reward samples, wiki rewards are used instead of measured ones. */
    public static final int MIN_REWARD_SAMPLES = 3;
    /** Below this many NPC sightings on a map, the wiki NPC list is used for the map mix. */
    public static final int MIN_SIGHTINGS = 15;

    private final WikiData wiki;
    private final Map<String, NpcRecord> records = new HashMap<>();
    private final String setupKey;
    private final double dps;

    public Calculator(WikiData wiki, List<NpcRecord> npcRecords, String setupKey, double dps) {
        this.wiki = wiki;
        this.setupKey = setupKey;
        this.dps = dps;
        for (NpcRecord r : npcRecords) this.records.put(NpcNames.key(r.name), r);
    }

    // ------------------------------------------------------------------ NPC values

    public NpcValue value(String name) {
        NpcRecord record = this.records.get(NpcNames.key(name));
        WikiNpc ref = this.wiki.find(name);

        NpcValue v = new NpcValue();
        v.name = record != null ? record.name : (ref != null ? ref.name : name);
        this.fillHp(v, record, ref);
        this.fillRewards(v, record, ref);
        this.fillTtk(v, record);
        this.fillInfo(v, record, ref);
        return v;
    }

    private void fillHp(NpcValue v, NpcRecord record, WikiNpc ref) {
        if (record != null && record.maxTotalHp > 0) {
            v.totalHp = record.maxTotalHp;
            v.hpSource = NpcValue.Source.MEASURED;
        } else if (ref != null) {
            v.totalHp = ref.totalHp();
            v.hpSource = NpcValue.Source.WIKI;
        }
    }

    private void fillRewards(NpcValue v, NpcRecord record, WikiNpc ref) {
        boolean measured = record != null && !record.rewardSamples.isEmpty();
        if (measured && (record.rewardSamples.size() >= MIN_REWARD_SAMPLES || ref == null)) {
            v.perKill = record.medianRewards();
            v.rewardSource = NpcValue.Source.MEASURED;
        } else if (ref != null) {
            v.perKill = ref.rewards();
            v.rewardSource = NpcValue.Source.WIKI;
        }
    }

    private void fillTtk(NpcValue v, NpcRecord record) {
        if (record != null && record.ttkSampleCount(this.setupKey) > 0) {
            v.ttk = record.medianTtk(this.setupKey);
            v.ttkSource = NpcValue.Source.MEASURED;
        } else if (this.dps > 0 && v.totalHp > 0) {
            v.ttk = v.totalHp / this.dps;
            v.ttkSource = NpcValue.Source.ESTIMATED;
        }
    }

    private void fillInfo(NpcValue v, NpcRecord record, WikiNpc ref) {
        if (record != null) {
            v.kills = record.kills;
            if (record.shouldKill != null) v.killable = record.shouldKill;
            v.maps = record.maps;
        }
        if (v.maps.isEmpty() && ref != null) v.maps = new LinkedHashSet<>(ref.maps);
    }

    /** Every NPC known from the wiki or met in game. */
    public List<NpcValue> allNpcs() {
        Map<String, String> names = new LinkedHashMap<>();
        for (WikiNpc ref : this.wiki.all()) names.put(NpcNames.key(ref.name), ref.name);
        for (NpcRecord r : this.records.values()) names.put(NpcNames.key(r.name), r.name);
        List<NpcValue> values = new ArrayList<>(names.size());
        for (String name : names.values()) values.add(this.value(name));
        return values;
    }

    // ------------------------------------------------------------------ map ranking

    /** Estimated gains on one map with the current equipment. */
    public static class MapEstimate {
        public String map;
        /** True when the NPC mix was observed in game, false when it comes from the wiki list. */
        public boolean observedMix;
        public int sightings;
        public String npcs;
        public double killsPerHour;
        public Rewards perHour = new Rewards();
        /** Really measured on this map, null when not enough time spent there. */
        public Rewards measuredPerHour;
        public double hoursMeasured;
    }

    /**
     * @param company      1 (MMO), 2 (EIC), 3 (VRU) to keep only that company's X-maps; 0 = all maps
     * @param onlyKillable ignore NPCs not ticked as "kill" in the DarkBot NPC list
     */
    public List<MapEstimate> rankMaps(List<MapRecord> mapRecords, double overheadSeconds,
                                      boolean onlyKillable, int company, double minMeasuredSeconds) {
        Map<String, MapRecord> visited = new HashMap<>();
        for (MapRecord m : mapRecords) visited.put(m.name, m);

        List<MapEstimate> result = new ArrayList<>();
        for (String map : this.candidateMaps(mapRecords)) {
            if (!isCompanyMap(map, company)) continue;
            MapRecord record = visited.get(map);
            MapEstimate estimate = this.estimate(map, record, overheadSeconds, onlyKillable);
            if (estimate == null) continue;
            if (record != null && record.seconds > 0 && record.seconds >= minMeasuredSeconds) {
                estimate.measuredPerHour = record.perHour();
                estimate.hoursMeasured = record.seconds / 3600;
            }
            result.add(estimate);
        }
        return result;
    }

    private Set<String> candidateMaps(List<MapRecord> mapRecords) {
        Set<String> maps = new LinkedHashSet<>(this.wiki.allMaps());
        for (MapRecord m : mapRecords) if (m.totalSightings() > 0) maps.add(m.name);
        return maps;
    }

    private MapEstimate estimate(String map, MapRecord record, double overhead, boolean onlyKillable) {
        boolean observed = record != null && record.totalSightings() >= MIN_SIGHTINGS;
        Map<String, Integer> mix = observed ? record.sightings : this.wikiMix(map);

        double weights = 0;
        double seconds = 0;
        Rewards gains = new Rewards();
        List<String> used = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : mix.entrySet()) {
            NpcValue v = this.value(entry.getKey());
            if ((onlyKillable && !v.killable) || !v.hasTtk()) continue;
            double w = entry.getValue();
            weights += w;
            seconds += w * (v.ttk + overhead);
            gains.add(v.perKill.scaled(w));
            used.add(v.name);
        }
        if (weights <= 0 || seconds <= 0) return null;

        MapEstimate e = new MapEstimate();
        e.map = map;
        e.observedMix = observed;
        e.sightings = observed ? record.totalSightings() : 0;
        e.npcs = String.join(", ", used);
        e.killsPerHour = weights * 3600 / seconds;
        e.perHour = gains.scaled(3600 / seconds);
        return e;
    }

    /** Without observations every wiki NPC of the map counts the same. */
    private Map<String, Integer> wikiMix(String map) {
        Map<String, Integer> mix = new LinkedHashMap<>();
        for (WikiNpc ref : this.wiki.onMap(map)) mix.put(ref.name, 1);
        return mix;
    }

    /** Company X-maps are "1-n" (MMO), "2-n" (EIC), "3-n" (VRU); every other map (4-x, 5-x, gates…) is kept. */
    static boolean isCompanyMap(String map, int company) {
        if (company <= 0 || map.length() < 3 || map.charAt(1) != '-') return true;
        char first = map.charAt(0);
        if (first < '1' || first > '3') return true;
        return first - '0' == company;
    }
}
