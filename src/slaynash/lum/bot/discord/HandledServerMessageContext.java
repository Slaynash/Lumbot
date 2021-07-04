package slaynash.lum.bot.discord;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HandledServerMessageContext {

    public final MessageReceivedEvent messageReceivedEvent;
    public final int suspiciousValue;
    public final long guildId;
    public final LocalDateTime creationTime;

    public HandledServerMessageContext(MessageReceivedEvent messageReceivedEvent, int suspiciousValue, long guildId) {
        this.messageReceivedEvent = messageReceivedEvent;
        this.suspiciousValue = suspiciousValue;
        this.guildId = guildId;
        this.creationTime = LocalDateTime.now(ZoneOffset.UTC);
    }
}