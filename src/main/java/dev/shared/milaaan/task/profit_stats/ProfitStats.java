package dev.shared.milaaan.task.profit_stats;

import dev.shared.milaaan.config.ProfitStatsConfig;
import dev.shared.milaaan.task.profit_stats.data.Rewards;
import dev.shared.milaaan.task.profit_stats.data.StatsStore;
import dev.shared.milaaan.task.profit_stats.data.WikiData;
import dev.shared.milaaan.task.profit_stats.gui.ProfitWindow;
import dev.shared.milaaan.task.profit_stats.gui.Texts;
import dev.shared.milaaan.task.profit_stats.tracking.KillTracker;
import dev.shared.milaaan.task.profit_stats.tracking.RewardTracker;
import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.events.EventHandler;
import eu.darkbot.api.events.Listener;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.ExtraMenus;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.game.entities.Box;
import eu.darkbot.api.game.entities.Npc;
import eu.darkbot.api.game.other.EntityInfo;
import eu.darkbot.api.game.other.GameMap;
import eu.darkbot.api.game.other.Health;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.EntitiesAPI;
import eu.darkbot.api.managers.ExtensionsAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.I18nAPI;
import eu.darkbot.api.managers.StatsAPI;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Feature(name = "Profit Stats",
        description = "Learns what every NPC gives and how fast you kill it, and ranks maps by gains per hour with your current equipment. Open it from the extra menu.")
public class ProfitStats implements Task, Listener, ExtraMenus, Configurable<ProfitStatsConfig> {
    /** Bonus boxes collected closer than this to the hero are considered ours. */
    private static final double BOX_COLLECT_DISTANCE = 300;
    /** A gap between ticks longer than this (pause, freeze) is not counted as map time. */
    private static final double MAX_TICK_GAP_SECONDS = 5;
    private static final long SAVE_INTERVAL_MS = 60_000;
    private static final long SCAN_INTERVAL_MS = 1_000;
    private static final int MAX_REMEMBERED_NPC_IDS = 5_000;

    private final HeroAPI hero;
    private final StatsAPI stats;
    private final BotAPI bot;
    private final EntitiesAPI entities;
    private final Texts texts;
    private final WikiData wiki = WikiData.load();

    private final StatsStore store = new StatsStore();
    private final KillTracker kills = new KillTracker();
    private final RewardTracker rewards = new RewardTracker(store);
    /** NPC instances already counted on the current map. */
    private final Set<Integer> seenNpcIds = new HashSet<>();

    private ProfitStatsConfig config = new ProfitStatsConfig();
    private Rewards lastTotals;
    private long lastTick;
    private long lastSave = System.currentTimeMillis();
    private long lastScan;
    private int lastMapId = Integer.MIN_VALUE;

    public ProfitStats(PluginAPI api) {
        this.hero = api.requireAPI(HeroAPI.class);
        this.stats = api.requireAPI(StatsAPI.class);
        this.bot = api.requireAPI(BotAPI.class);
        this.entities = api.requireAPI(EntitiesAPI.class);
        this.texts = new Texts(api.requireAPI(I18nAPI.class), api.requireAPI(ExtensionsAPI.class), ProfitStats.class);
        this.store.load();
    }

    @Override
    public void setConfig(ConfigSetting<ProfitStatsConfig> config) {
        this.config = config.getValue();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onTickTask() {
        long now = System.currentTimeMillis();
        double dt = this.lastTick == 0 ? 0 : Math.min((now - this.lastTick) / 1000.0, MAX_TICK_GAP_SECONDS);
        this.lastTick = now;
        Rewards gains = this.readGains();

        String mapName = this.currentMapName();
        if (!this.isActive() || mapName == null) return;

        this.kills.tick(this.hero, now);
        this.store.addMapTime(mapName, dt, gains, false);
        this.rewards.onGains(gains, now);
        if (now - this.lastScan >= SCAN_INTERVAL_MS) {
            this.lastScan = now;
            this.scanNpcs(mapName);
        }
        if (now - this.lastSave > SAVE_INTERVAL_MS) {
            this.lastSave = now;
            this.store.saveIfDirty();
        }
    }

    private boolean isActive() {
        return this.config != null && this.config.enabled && this.bot.isRunning();
    }

    /** Current map name; resets per-map tracking when the map changes. Null when unknown. */
    private String currentMapName() {
        GameMap map = this.hero.getMap();
        if (map == null || map.getName() == null || map.getName().isEmpty()) return null;
        if (map.getId() != this.lastMapId) {
            this.lastMapId = map.getId();
            this.kills.reset();
            this.rewards.cancel();
            this.seenNpcIds.clear();
        }
        return map.getName();
    }

    /** Counts every NPC instance appearing on the map: gives the real NPC mix of each map and their HP. */
    private void scanNpcs(String mapName) {
        for (Npc npc : this.entities.getNpcs()) {
            if (!npc.isValid() || !this.seenNpcIds.add(npc.getId())) continue;
            Health health = npc.getHealth();
            int maxTotal = health.getMaxHp() + health.getMaxShield();
            boolean shouldKill = npc.getInfo() == null || npc.getInfo().getShouldKill();
            this.store.recordSighting(KillTracker.npcName(npc), mapName, maxTotal, shouldKill);
        }
        if (this.seenNpcIds.size() > MAX_REMEMBERED_NPC_IDS) this.seenNpcIds.clear();
    }

    /** Positive change of credits / uridium / XP / honor since last tick. Spending is ignored. */
    private Rewards readGains() {
        Rewards totals = new Rewards(this.stats.getTotalCredits(), this.stats.getTotalUridium(),
                this.stats.getTotalExperience(), this.stats.getTotalHonor());
        Rewards previous = this.lastTotals;
        this.lastTotals = totals;
        // Stats are 0 while (re)connecting: the first real value would look like a huge gain.
        if (previous == null || previous.credits <= 0 || previous.experience <= 0) return new Rewards();
        return new Rewards(
                Math.max(0, totals.credits - previous.credits),
                Math.max(0, totals.uridium - previous.uridium),
                Math.max(0, totals.experience - previous.experience),
                Math.max(0, totals.honor - previous.honor));
    }

    // ------------------------------------------------------------------ events

    @EventHandler
    public void onRunningToggle(BotAPI.RunningToggleEvent event) {
        this.lastTick = 0; // paused time is not map time
        this.kills.reset();
        this.rewards.cancel();
        this.store.saveIfDirty();
    }

    @EventHandler
    public void onEntityRemoved(EntitiesAPI.EntityRemoveEvent event) {
        if (this.config == null || !this.config.enabled) return;
        long now = System.currentTimeMillis();
        if (event.getEntity() instanceof Box) {
            if (event.getEntity().distanceTo(this.hero) < BOX_COLLECT_DISTANCE) this.rewards.onOtherGainSource();
            return;
        }
        KillTracker.Kill kill = this.kills.onEntityRemoved(event.getEntity(), now);
        if (kill != null) this.recordKill(kill, now);
    }

    private void recordKill(KillTracker.Kill kill, long now) {
        GameMap map = this.hero.getMap();
        String mapName = map == null ? null : map.getName();
        String setup = this.currentSetupKey();
        this.store.recordKill(kill.name, mapName, kill.maxTotalHp, setup, kill.ttkSeconds);
        if (kill.dps > 0) this.store.recordDps(setup, kill.dps);
        if (mapName != null) this.store.addMapTime(mapName, 0, null, true);
        this.rewards.onKill(kill.name, now, this.config.rewardWindowMs);
    }

    // ------------------------------------------------------------------ data for the window

    /** Ship type + hero configuration: what "your current equipment" means for TTK/DPS. */
    public String currentSetupKey() {
        String ship = this.hero.getShipType();
        if (ship == null || ship.isEmpty()) ship = "?";
        int cfg = this.hero.getConfiguration() == HeroAPI.Configuration.SECOND ? 2 : 1;
        return ship + " / config " + cfg;
    }

    /** DPS used for estimations: manual override or median measured with the current setup. */
    public double currentDps() {
        if (this.config.manualDps > 0) return this.config.manualDps;
        return this.store.medianDps(this.currentSetupKey());
    }

    public int currentDpsSamples() {
        return this.store.dpsSampleCount(this.currentSetupKey());
    }

    public boolean isManualDps() {
        return this.config.manualDps > 0;
    }

    /** 1 = MMO, 2 = EIC, 3 = VRU, 0 = unknown. */
    public int currentCompany() {
        EntityInfo info = this.hero.getEntityInfo();
        if (info == null || info.getFaction() == null) return 0;
        switch (info.getFaction()) {
            case MMO: return 1;
            case EIC: return 2;
            case VRU: return 3;
            default: return 0;
        }
    }

    public WikiData getWiki() {
        return this.wiki;
    }

    public ProfitStatsConfig getConfig() {
        return this.config;
    }

    public StatsStore getStore() {
        return this.store;
    }

    public Texts getTexts() {
        return this.texts;
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Collection<JComponent> getExtraMenuItems(PluginAPI api) {
        return Arrays.asList(
                this.createSeparator("Profit Stats"),
                this.create(this.texts.get("open"), e -> SwingUtilities.invokeLater(() -> ProfitWindow.open(this))));
    }
}
