package dev.shared.milaaan.npc_rewards;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.ExtraMenus;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.I18nAPI;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Adds an "NPC Rewards" entry to the extra menu: a reference table of every NPC, where to find it and what it gives. */
@Feature(name = "NPC Rewards",
        description = "Reference table of every NPC: maps, HP, shield, rewards and rewards per 1,000 HP. Open it from the extra menu.",
        enabledByDefault = true)
public class NpcRewards implements ExtraMenus {
    private final Texts texts;
    private final List<AlienEntry> aliens = AlienTable.load();

    public NpcRewards(PluginAPI api) {
        this.texts = new Texts(api.requireAPI(I18nAPI.class), api.requireAPI(ExtensionsAPI.class), NpcRewards.class);
    }

    @Override
    public Collection<JComponent> getExtraMenuItems(PluginAPI api) {
        return Arrays.asList(
                this.createSeparator("NPC Rewards"),
                this.create(this.texts.get("open"),
                        e -> SwingUtilities.invokeLater(() -> NpcRewardsWindow.open(this.aliens, this.texts))));
    }
}
