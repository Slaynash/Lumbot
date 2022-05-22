package slaynash.lum.bot.steam;

public class SteamChannel {

    public final String gameID;
    public final String guildID;
    public final String channelId;
    public final String publicMessage;
    public final String betaMessage;
    public final String otherMessage;

    public SteamChannel(String gameID, String guildID, String channelId, String publicMessage, String betaMessage, String otherMessage) {
        this.gameID = gameID;
        this.guildID = guildID;
        this.channelId = channelId;
        this.publicMessage = publicMessage;
        this.betaMessage = betaMessage;
        this.otherMessage = otherMessage;
    }

}
