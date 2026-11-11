package dev.shared.milaaan.npc_rewards;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads the NPC table bundled in the jar (no internet access) and generates
 * the Boss (x4) and Uber (x8) variants of every base alien.
 */
public final class AlienTable {
    private static final String RESOURCE = "/dev/shared/milaaan/npc_rewards/npcs.json";
    private static final int BOSS_FACTOR = 4;
    private static final int UBER_FACTOR = 8;

    private AlienTable() {
    }

    /** Raw JSON content. */
    private static class File {
        List<Base> npcs = new ArrayList<>();
        List<String> uberMaps = new ArrayList<>();
    }

    /** One base alien as written in the JSON file. */
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

    public static List<AlienEntry> load() {
        try (InputStream in = AlienTable.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                System.out.println("NPC Rewards: missing " + RESOURCE);
                return Collections.emptyList();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                File file = new Gson().fromJson(reader, File.class);
                return file == null ? Collections.<AlienEntry>emptyList() : build(file);
            }
        } catch (IOException | JsonParseException e) {
            System.out.println("NPC Rewards: could not read the NPC table: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static List<AlienEntry> build(File file) {
        List<AlienEntry> entries = new ArrayList<>();
        for (Base base : file.npcs) {
            entries.add(variant(base, AlienEntry.Kind.NORMAL, "", 1, base.maps));
            if (!isEmpty(base.bossMaps)) entries.add(variant(base, AlienEntry.Kind.BOSS, "Boss ", BOSS_FACTOR, base.bossMaps));
            if (!base.noUber) entries.add(variant(base, AlienEntry.Kind.UBER, "Uber ", UBER_FACTOR, file.uberMaps));
        }
        return entries;
    }

    private static AlienEntry variant(Base b, AlienEntry.Kind kind, String prefix, int f, List<String> maps) {
        return new AlienEntry(prefix + b.name, kind, String.join(", ", maps), b.hp * f, b.shield * f,
                b.credits * f, b.uridium * f, b.experience * f, b.honor * f);
    }

    private static boolean isEmpty(List<String> list) {
        return list == null || list.isEmpty();
    }
}
