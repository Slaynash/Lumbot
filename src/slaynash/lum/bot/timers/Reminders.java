package slaynash.lum.bot.timers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.Main;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Reminders extends TimerTask {
    private record Reminder(long id, long userId, long serverId, long channelId, String message, int color, Instant due) { }

    private static final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
    private static final Set<Long> completed = ConcurrentHashMap.newKeySet();

    public void run() {
        System.out.println("Running Reminders task...");
        // Delete only here, before reading due reminders, so a stale query cannot requeue a completed reminder.
        for (long id : completed)
            deleteCompleted(id);

        List<Reminder> reminders = new ArrayList<>();
        ResultSet rs = null;
        try {
            rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `Reminders` WHERE `TSend` < CURRENT_TIMESTAMP");
            while (rs.next()) {
                try {
                    reminders.add(new Reminder(rs.getLong("ID"), rs.getLong("UserID"), rs.getLong("ServerID"),
                        rs.getLong("ChannelID"), rs.getString("Message"), rs.getInt("Color"), rs.getTimestamp("TSend").toInstant()));
                }
                catch (SQLException | RuntimeException e) {
                    ExceptionUtils.reportException("Failed to read reminder row", e);
                }
            }
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Failed to get reminders", e);
        }
        finally {
            DBConnectionManagerLum.closeRequest(rs);
        }

        for (Reminder reminder : reminders) {
            if (!inFlight.add(reminder.id()))
                continue;
            try {
                User user = JDAManager.getJDA().getUserById(reminder.userId());
                if (user == null) {
                    JDAManager.getJDA().retrieveUserById(reminder.userId()).queue(
                        retrieved -> sendReminder(reminder, retrieved),
                        error -> deliveryFailed(reminder.id(), error));
                }
                else {
                    sendReminder(reminder, user);
                }
            }
            catch (RuntimeException e) {
                deliveryFailed(reminder.id(), e);
            }
        }
    }

    private static void sendReminder(Reminder reminder, User user) {
        boolean directMessage = false;
        try {
            String message = reminder.message();
            if (message != null)
                message = message.substring(0, Math.min(message.length(), MessageEmbed.DESCRIPTION_MAX_LENGTH));
            MessageEmbed embed = new EmbedBuilder().setColor(reminder.color()).setTitle("Reminder").setDescription(message).build();
            if (message == null)
                message = user.getAsMention();

            RestAction<Message> action;
            if (reminder.serverId() == 0) {
                directMessage = true;
                action = user.openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(embed));
            }
            else {
                Guild guild = JDAManager.getJDA().getGuildById(reminder.serverId());
                if (guild == null) {
                    directMessage = true;
                    action = user.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessageEmbeds(embed));
                }
                else {
                    GuildChannel guildChannel = guild.getGuildChannelById(reminder.channelId());
                    MessageChannel channel = (MessageChannel) guildChannel;
                    if (channel == null || !channel.canTalk()) {
                        directMessage = true;
                        action = user.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessageEmbeds(embed));
                    }
                    else if (guild.getSelfMember().hasPermission(guildChannel, Permission.MESSAGE_EMBED_LINKS)) {
                        action = channel.sendMessageEmbeds(embed).addContent(user.getAsMention());
                    }
                    else {
                        action = channel.sendMessage(message).addContent("\n" + user.getAsMention());
                    }
                }
            }

            System.out.println("Reminder " + reminder.id() + " queued; due=" + reminder.due() + ", queued=" + Instant.now());
            boolean isDirectMessage = directMessage;
            action.queue(sent -> {
                completed.add(reminder.id());
                System.out.println("Reminder " + reminder.id() + " delivered; due=" + reminder.due() + ", delivered=" + Instant.now() + ", messageId=" + sent.getId());
            }, error -> {
                if (isDirectMessage)
                    directMessageFailed(reminder.id(), error);
                else
                    deliveryFailed(reminder.id(), error);
            });
        }
        catch (RuntimeException e) {
            if (directMessage)
                directMessageFailed(reminder.id(), e);
            else
                deliveryFailed(reminder.id(), e);
        }
    }

    private static void directMessageFailed(long id, Throwable error) {
        completed.add(id);
        ExceptionUtils.reportException("Failed to DM reminder " + id + "; will delete it", error);
    }

    private static void deliveryFailed(long id, Throwable error) {
        inFlight.remove(id);
        ExceptionUtils.reportException("Failed to deliver reminder " + id + "; will retry", error);
    }

    private static void deleteCompleted(long id) {
        synchronized (completed) {
            if (!completed.contains(id))
                return;
            try {
                DBConnectionManagerLum.sendUpdate("DELETE FROM `Reminders` WHERE `ID` = ?", id);
                completed.remove(id);
                inFlight.remove(id);
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to delete completed reminder " + id + "; will retry deletion", e);
            }
        }
    }

    public static void start() {
        System.out.println("Starting Reminders task...");
        Main.SCHEDULER.scheduleAtFixedRate(
            new Reminders(),
            0,
            5,
            TimeUnit.SECONDS
        );
    }
}
