package dev.shared.milaaan.task.profit_stats.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference NPC table bundled in the jar (no internet access at runtime).
 * Generates the Boss (x4) and Uber (x8) variants from the base aliens.
 */
public final class WikiData {
    private static final String RESOURCE = "/dev/shared/milaaan/profit_stats/npcs.json";
    private static final int BOSS_FACTOR = 4;
    private static final int UBER_FACTOR = 8;

    private final Map<String, WikiNpc> byKey = new LinkedHashMap<>();

    private static class File {
        List<Base> npcs = new ArrayList<>();
        List<String> uberMaps = new ArrayList<>();
    }

    private static class Base {
        String name;
        int hp;
        int shield;
        double credits;
        double uridium;
        double experience;
        double honor;
        List<String> maps = new ArrayList<>();
        List<String> bossMaps = new ArrayList<>();
        boolean noUber;
    }

    public static WikiData load() {
        WikiData data = new WikiData();
        try (InputStream in = WikiData.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                System.out.println("[ProfitStats] Missing " + RESOURCE);
                return data;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                File file = new Gson().fromJson(reader, File.class);
                if (file != null) data.fill(file);
            }
        } catch (IOException | JsonParseException e) {
            System.out.println("[ProfitStats] Could not read NPC table: " + e.getMessage());
        }
        return data;
    }

    private void fill(File file) {
        for (Base base : file.npcs) {
            add(base, "", 1, base.maps);
            add(base, "Boss ", BOSS_FACTOR, base.bossMaps);
            if (!base.noUber) add(base, "Uber ", UBER_FACTOR, file.uberMaps);
        }
    }

    private void add(Base base, String prefix, int factor, List<String> maps) {
        WikiNpc npc = new WikiNpc();
        npc.name = prefix + base.name;
        npc.hp = base.hp * factor;
        npc.shield = base.shield * factor;
        npc.credits = base.credits * factor;
        npc.uridium = base.uridium * factor;
        npc.experience = base.experience * factor;
        npc.honor = base.honor * factor;
        for (String map : maps == null ? Collections.<String>emptyList() : maps) npc.maps.addAll(expandMap(map));
        byKey.put(NpcNames.key(npc.name), npc);
    }

    /** "X-3" means the three company maps 1-3, 2-3 and 3-3. */
    static List<String> expandMap(String map) {
        List<String> result = new ArrayList<>(3);
        if (map.startsWith("X-") || map.startsWith("x-")) {
            String level = map.substring(2);
            for (int company = 1; company <= 3; company++) result.add(company + "-" + level);
        } else {
            result.add(map);
        }
        return result;
    }

    /** Finds the reference entry for an in-game NPC name such as "..::{ Boss Mordon }::..". */
    public WikiNpc find(String inGameName) {
        return byKey.get(NpcNames.key(inGameName));
    }

    public Collection<WikiNpc> all() {
        return Collections.unmodifiableCollection(byKey.values());
    }

    public List<WikiNpc> onMap(String mapName) {
        List<WikiNpc> result = new ArrayList<>();
        for (WikiNpc npc : byKey.values()) if (npc.maps.contains(mapName)) result.add(npc);
        return result;
    }

    public List<String> allMaps() {
        List<String> maps = new ArrayList<>();
        for (WikiNpc npc : byKey.values()) for (String m : npc.maps) if (!maps.contains(m)) maps.add(m);
        return maps;
    }
}
