package slaynash.lum.bot.discord;

import java.time.Instant;

public class HelpedRecentlyData {
    public long time;
    public long userid;
    public long channelid;

    public HelpedRecentlyData(long userid, long channelid) {
        time = Instant.now().getEpochSecond();
        this.userid = userid;
        this.channelid = channelid;
    }
}
