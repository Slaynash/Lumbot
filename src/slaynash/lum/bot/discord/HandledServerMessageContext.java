package slaynash.lum.bot.discord;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.ScamShield.ScamResults;

public class HandledServerMessageContext {

    public final MessageReceivedEvent messageReceivedEvent;
    public final ScamResults suspiciousResults;
    public final long guildId;
    public final LocalDateTime creationTime;

    public HandledServerMessageContext(MessageReceivedEvent messageReceivedEvent, ScamResults suspiciousResults, long guildId) {
        this.messageReceivedEvent = messageReceivedEvent;
        this.suspiciousResults = suspiciousResults;
        this.guildId = guildId;
        this.creationTime = LocalDateTime.now(ZoneOffset.UTC);
    }
    public HandledServerMessageContext(MessageReceivedEvent messageReceivedEvent, long guildId) {
        this.messageReceivedEvent = messageReceivedEvent;
        this.guildId = guildId;
        this.suspiciousResults = null;
        this.creationTime = LocalDateTime.now(ZoneOffset.UTC);
    }
}
