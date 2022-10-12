package slaynash.lum.bot.steam;

public record SteamChannel(String gameID, String guildID, String channelId, String publicMessage, String betaMessage, String otherMessage) {
}
