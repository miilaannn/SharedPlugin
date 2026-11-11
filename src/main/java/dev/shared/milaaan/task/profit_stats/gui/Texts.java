package dev.shared.milaaan.task.profit_stats.gui;

import eu.darkbot.api.extensions.FeatureInfo;
import eu.darkbot.api.extensions.PluginInfo;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.I18nAPI;

/** Translated texts of the ProfitStats window ("milaaan.profit_stats.gui.*" keys of the plugin language files). */
public final class Texts {
    private static final String PREFIX = "milaaan.profit_stats.gui.";

    private final I18nAPI i18n;
    private final ExtensionsAPI extensions;
    private final Class<?> feature;
    private PluginInfo plugin;

    public Texts(I18nAPI i18n, ExtensionsAPI extensions, Class<?> feature) {
        this.i18n = i18n;
        this.extensions = extensions;
        this.feature = feature;
    }

    /** Arguments use the MessageFormat syntax: {0}, {1}… */
    public String get(String key, Object... arguments) {
        return i18n.getOrDefault(plugin(), PREFIX + key, key, arguments);
    }

    private PluginInfo plugin() {
        if (plugin == null) {
            FeatureInfo<?> info = extensions.getFeatureInfo(feature);
            if (info != null) plugin = info.getPluginInfo();
        }
        return plugin;
    }
}
