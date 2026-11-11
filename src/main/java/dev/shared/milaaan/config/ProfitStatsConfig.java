package dev.shared.milaaan.config;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("milaaan.profit_stats")
public class ProfitStatsConfig {

    @Option("general.enabled")
    public boolean enabled = true;

    /** Time after a kill during which gains are attributed to the killed NPC. */
    @Option("milaaan.profit_stats.reward_window")
    @Number(min = 500, max = 10_000, step = 250)
    public int rewardWindowMs = 2_500;

    /** Average seconds lost between two kills (searching, travelling), used for per-hour estimations. */
    @Option("milaaan.profit_stats.overhead")
    @Number(min = 0, max = 120, step = 1)
    public int overheadSeconds = 6;

    /** Forces the DPS used for estimations. 0 = use the DPS measured with the current ship/config. */
    @Option("milaaan.profit_stats.manual_dps")
    @Number(min = 0, max = 1_000_000, step = 1_000)
    public int manualDps = 0;

    /** Maps with less time than this are hidden from the measured map table. */
    @Option("milaaan.profit_stats.min_map_minutes")
    @Number(min = 0, max = 240, step = 1)
    public int minMapMinutes = 5;

    /** Map ranking: only count NPCs ticked as "kill" in the DarkBot NPC list. */
    @Option("milaaan.profit_stats.only_killable")
    public boolean onlyKillable = true;

    /** Map ranking: only show your own company's X-maps (plus 4-x, 5-x…). */
    @Option("milaaan.profit_stats.own_company_only")
    public boolean ownCompanyOnly = true;
}
