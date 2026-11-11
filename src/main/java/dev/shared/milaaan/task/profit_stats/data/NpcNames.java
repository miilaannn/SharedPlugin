package dev.shared.milaaan.task.profit_stats.data;

/**
 * In-game names are decorated: "-=[ Mordon ]=-", "..::{ Boss Mordon }::..", "«¤( UberMordon )»¤".
 * The key keeps letters and digits only. It stays case-sensitive because "Streuner" and "StreuneR" are different NPCs.
 */
public final class NpcNames {
    private NpcNames() {
    }

    public static String key(String name) {
        if (name == null) return "";
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) sb.append(c);
        }
        return sb.toString();
    }
}
