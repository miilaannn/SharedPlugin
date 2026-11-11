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

/** Loads the NPC table bundled in the jar (no internet access at runtime). */
public final class AlienTable {
    private static final String RESOURCE = "/dev/shared/milaaan/npc_rewards/npcs.json";

    private AlienTable() {
    }

    /** Raw JSON content. */
    private static class File {
        List<AlienEntry> npcs = new ArrayList<>();
    }

    public static List<AlienEntry> load() {
        try (InputStream in = AlienTable.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                System.out.println("NPC Rewards: missing " + RESOURCE);
                return Collections.emptyList();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                File file = new Gson().fromJson(reader, File.class);
                return file == null || file.npcs == null ? Collections.<AlienEntry>emptyList() : file.npcs;
            }
        } catch (IOException | JsonParseException e) {
            System.out.println("NPC Rewards: could not read the NPC table: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
