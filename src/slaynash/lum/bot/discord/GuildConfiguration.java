package slaynash.lum.bot.discord;

import java.sql.Timestamp;

public record GuildConfiguration(String uildID, Timestamp ts, boolean ScamShield,
                                 boolean ScamShieldBan, boolean ScamShieldCross, boolean ScamShieldDm, boolean MLLogScan, boolean MLLogReaction,
                                 boolean MLReplies, boolean MLPartialRemover, boolean MLGeneralRemover, boolean DLLRemover, boolean LumReplies, boolean DadJokes)
{
    public enum Setting {
        TS("ts"),
        SCAMSHIELD("ScamShield"),
        DLLREMOVER("DLLRemover"),
        LOGREACTION("MLLogReaction"),
        LUMREPLIES("LumReplies"),
        PARTIALLOGREMOVER("MLPartialRemover"),
        GENERALLOGREMOVER("MLGeneralRemover"),
        DADJOKES("DadJokes"),
        LOGSCAN("MLLogScan"),
        MLREPLIES("MLReplies"),
        SSBAN("ScamShieldBan"),
        SSCROSS("ScamShieldCross"),
        SSDM("ScamShieldDm");
        public final String string;
        Setting(String string) {
            this.string = string;
        }
    }
}
