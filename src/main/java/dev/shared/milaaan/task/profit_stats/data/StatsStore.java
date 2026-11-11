package dev.shared.milaaan.task.profit_stats.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent, thread-safe storage. The bot thread writes, the Swing window reads snapshots.
 * Saved as JSON in "profit_stats/data.json" inside the DarkBot folder.
 */
public class StatsStore {
    private static final Path FILE = Paths.get("profit_stats", "data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Data data = new Data();
    private boolean dirty;

    /** Serialized content. */
    private static class Data {
        Map<String, NpcRecord> npcs = new HashMap<>();
        Map<String, MapRecord> maps = new HashMap<>();
        Map<String, List<Double>> dpsBySetup = new HashMap<>();
    }

    public synchronized void load() {
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) {
                if (loaded.npcs == null) loaded.npcs = new HashMap<>();
                if (loaded.maps == null) loaded.maps = new HashMap<>();
                if (loaded.dpsBySetup == null) loaded.dpsBySetup = new HashMap<>();
                for (MapRecord m : loaded.maps.values()) if (m.sightings == null) m.sightings = new HashMap<>();
                data = loaded;
            }
        } catch (IOException | JsonParseException e) {
            System.out.println("[ProfitStats] Could not read " + FILE + ": " + e.getMessage());
        }
    }

    public synchronized void saveIfDirty() {
        if (!dirty) return;
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling("data.json.tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
        } catch (IOException e) {
            System.out.println("[ProfitStats] Could not save " + FILE + ": " + e.getMessage());
        }
    }

    public synchronized void reset() {
        data = new Data();
        dirty = true;
    }

    // ---- Writes (bot thread) ----

    public synchronized void addMapTime(String map, double seconds, Rewards gains, boolean kill) {
        MapRecord record = data.maps.computeIfAbsent(map, MapRecord::new);
        record.seconds += seconds;
        if (gains != null) record.earned.add(gains);
        if (kill) record.kills++;
        dirty = true;
    }

    public synchronized void recordKill(String npc, String map, int maxTotalHp, String setupKey, double ttkSeconds) {
        NpcRecord record = data.npcs.computeIfAbsent(npc, NpcRecord::new);
        record.kills++;
        if (maxTotalHp > 0) record.maxTotalHp = maxTotalHp;
        if (map != null) record.maps.add(map);
        if (ttkSeconds > 0) {
            Samples.push(record.ttkBySetup.computeIfAbsent(setupKey, k -> new ArrayList<>()), ttkSeconds);
        }
        dirty = true;
    }

    /** An NPC appeared on the map (counted once per NPC instance). */
    public synchronized void recordSighting(String npc, String map, int maxTotalHp, boolean shouldKill) {
        NpcRecord record = data.npcs.computeIfAbsent(npc, NpcRecord::new);
        if (maxTotalHp > 0) record.maxTotalHp = maxTotalHp;
        record.shouldKill = shouldKill;
        record.maps.add(map);
        MapRecord mapRecord = data.maps.computeIfAbsent(map, MapRecord::new);
        mapRecord.sightings.merge(npc, 1, Integer::sum);
        dirty = true;
    }

    public synchronized void recordReward(String npc, Rewards rewards) {
        NpcRecord record = data.npcs.computeIfAbsent(npc, NpcRecord::new);
        Samples.push(record.rewardSamples, rewards.copy());
        dirty = true;
    }

    public synchronized void recordDps(String setupKey, double dps) {
        Samples.push(data.dpsBySetup.computeIfAbsent(setupKey, k -> new ArrayList<>()), dps);
        dirty = true;
    }

    // ---- Reads (any thread) ----

    public synchronized double medianDps(String setupKey) {
        return Samples.median(data.dpsBySetup.get(setupKey));
    }

    public synchronized int dpsSampleCount(String setupKey) {
        List<Double> list = data.dpsBySetup.get(setupKey);
        return list == null ? 0 : list.size();
    }

    /** Deep copy so the GUI can read without holding the lock. */
    public synchronized List<NpcRecord> snapshotNpcs() {
        return GSON.fromJson(GSON.toJson(new ArrayList<>(data.npcs.values())),
                new com.google.gson.reflect.TypeToken<List<NpcRecord>>() {}.getType());
    }

    public synchronized List<MapRecord> snapshotMaps() {
        return GSON.fromJson(GSON.toJson(new ArrayList<>(data.maps.values())),
                new com.google.gson.reflect.TypeToken<List<MapRecord>>() {}.getType());
    }
}
