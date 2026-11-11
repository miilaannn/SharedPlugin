package dev.shared.milaaan.npc_rewards;

import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.extensions.PluginInfo;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.I18nAPI;

/** Translated texts of the window ("milaaan.npc_rewards.gui.*" keys of the plugin language files). */
public final class Texts {
    private static final String PREFIX = "milaaan.npc_rewards.gui.";

    private final I18nAPI i18n;
    private final ExtensionsAPI extensions;
    private final Class<?> feature;
    private PluginInfo plugin;

    public Texts(I18nAPI i18n, ExtensionsAPI extensions, Class<?> feature) {
        this.i18n = i18n;
        this.extensions = extensions;
        this.feature = feature;
    }

    public String get(String key) {
        return this.i18n.getOrDefault(this.plugin(), PREFIX + key, key);
    }

    private PluginInfo plugin() {
        if (this.plugin == null) {
            FeatureInfo<?> info = this.extensions.getFeatureInfo(this.feature);
            if (info != null) this.plugin = info.getPluginInfo();
        }
        return this.plugin;
    }
}
