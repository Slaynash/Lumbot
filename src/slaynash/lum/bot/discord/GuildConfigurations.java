package slaynash.lum.bot.discord;

import java.util.HashMap;

public class GuildConfigurations {
    public static HashMap<Long, boolean[]> configurations;

    public static final HashMap<Long, long[]> whitelistedRolesServers = new HashMap<>() {{
                put(439093693769711616L /* VRCMG */, new long[] {
                    631581319670923274L /* Staff */,
                    662720231591903243L /* Helper */,
                    825266051277258754L /* cutie */,
                    585594394153844865L /* Modder */ });
                put(600298024425619456L /* emmVRC */, new long[] {
                    748392927365169233L /* Admin */,
                    653722281864069133L /* Helper */ });
                put(663449315876012052L /* MelonLoader */, new long[] {
                    663450403194798140L /* Lava Gang */,
                    663450567015792670L /* Administrator */,
                    663450611253248002L /* Moderator */,
                    663450655775522843L /* Modder */ });
                put(673663870136746046L /* Modders & Chill */, new long[] {
                    673725166626406428L /* Modders */,
                    673726384450961410L /* Moderators */ });
                put(633588473433030666L /* Slaynash's Workbench */, new long[] {
                    633590573412122634L /* Friends */});
                put(835185040752246835L /* The Long Development */, new long[] {
                    837912560497721344L /* Team Member */,
                    836863571811106846L /* Fellow Modder */});
                put(322211727192358914L /* The Long Dark Modding */, new long[] {
                    370425060844109835L /* Modders */});
                put(748692902137430018L /* Beat Saber Legacy Group */, new long[] {
                    748701248701857972L /* Staff */,
                    750814355985006633L /* Modders */,
                    810258575620309033L /* Helper */});
        }};

    public static final HashMap<Long, Long> lockDownRoles = new HashMap<>() {{
            put(439093693769711616L /* VRCMG */,        548534015108448256L /* Member */);
            put(600298024425619456L /* emmVRC */,       600304115972702209L /* Members */);
            put(663449315876012052L /* MelonLoader */,  663462327022256150L /* Member */);
            put(716536783621587004L /* Totally Wholesome */,716541223145308170L /* Member */);
        }};

    public static final HashMap<Long, Long> noMicChannels = new HashMap<>() {{
            put(439093693769711616L /* VRCMG */,        673955689298788376L /* no-mic */);
            put(600298024425619456L /* emmVRC */,       682379676106096678L /* no-mic */);
            put(663449315876012052L /* MelonLoader */,  701494833495408640L /* voice-chat */);
            put(716536783621587004L /* Totally Wholesome */,725076119623303380L /* mute-voice */);
        }};

    public enum ConfigurationMap {
        SCAMSHIELD(),
        DLLREMOVER(),
        LOGREACTION(),
        LUMREPLIES(),
        PARTIALLOGREMOVER(),
        GENERALLOGREMOVER(),
        DADJOKES();
    }
}
