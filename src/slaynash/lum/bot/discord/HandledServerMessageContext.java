package slaynash.lum.bot.discord;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HandledServerMessageContext {
    
    public final MessageReceivedEvent messageReceivedEvent;
    public final boolean isSuspicious;
    public final LocalDateTime creationTime;

    public HandledServerMessageContext(MessageReceivedEvent messageReceivedEvent, boolean isSuspicious) {
        this.messageReceivedEvent = messageReceivedEvent;
        this.isSuspicious = isSuspicious;
        this.creationTime = LocalDateTime.now(ZoneOffset.UTC);
    }

}
