package slaynash.lum.bot.discord.utils;

import java.util.function.Consumer;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class MessageFinder {
    private boolean waiting = false;
    public boolean found = false;
    public Message message = null;
    public Throwable error = null;

    public void findMessageAsync(Guild guild, String messageId, Consumer<Message> success, Consumer<Throwable> failure) {
        new Thread(() -> {

            findMessage(guild, messageId);
            if (error != null)
                failure.accept(error);
            else
                success.accept(message);

        }, "MessageFindThread " + guild.getId()).start();
    }

    public void findMessage(Guild guild, String messageId) {

        for (TextChannel tc : guild.getTextChannels()) {
            if (!guild.getSelfMember().hasPermission(tc, Permission.VIEW_CHANNEL))
                continue;
            System.out.println("tc: " + tc);
            waiting = true;
            try {
                tc.retrieveMessageById(messageId).queue(success -> {
                    message = success;
                    found = true;
                    waiting = false;
                }, failure -> {
                        //System.err.println("F: " + failure.getMessage());
                        if (!failure.getMessage().startsWith("10008")) {
                            error = failure;
                            found = true;
                        }
                        waiting = false;
                    });
            }
            catch (Exception e) {
                waiting = false;
                System.out.println("Failed to read channel " + tc + ": " + e.getMessage());
            }

            while (waiting) {
                try {
                    Thread.sleep(1);
                }
                catch (Exception ignored) { }
            }
            if (found)
                break;
        }
    }
}
