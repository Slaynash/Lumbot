package slaynash.lum.bot.timers;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Reminders extends TimerTask {
    public void run() {
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `Reminders` WHERE `TSend` < CURRENT_TIMESTAMP");
            while (rs.next()) {
                String message = rs.getString("Message");
                message = message.substring(0, Math.min(message.toString().length(), MessageEmbed.DESCRIPTION_MAX_LENGTH));
                EmbedBuilder embedBuilder = new EmbedBuilder().setColor(rs.getInt("Color"));
                embedBuilder.setTitle("Reminder");
                embedBuilder.setDescription(message);

                long userID = rs.getLong("UserID");
                User user = JDAManager.getJDA().getUserById(userID);
                long serverID = rs.getLong("ServerID");
                long channelID = rs.getLong("ChannelID");
                if (user == null) {
                    user = JDAManager.getJDA().retrieveUserById(userID).complete();
                }
                if (message == null) {
                    message = user.getAsMention();
                }
                if (serverID == 0) {
                    user.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(embedBuilder.build()).queue());
                }
                else {
                    Guild guild;
                    if ((guild = JDAManager.getJDA().getGuildById(serverID)) == null)
                        continue;
                    GuildChannel gchannel = guild.getGuildChannelById(channelID);
                    MessageChannel channel = (MessageChannel) gchannel;
                    if (!channel.canTalk()) {
                        user.openPrivateChannel().queue(pchannel -> pchannel.sendMessageEmbeds(embedBuilder.build()).queue());
                    }
                    else if (guild.getSelfMember().hasPermission(gchannel, Permission.MESSAGE_EMBED_LINKS)) {
                        channel.sendMessageEmbeds(embedBuilder.build()).addContent(user.getAsMention()).queue();
                    }
                    else {
                        channel.sendMessage(message).addContent("\n" + user.getAsMention()).queue();
                    }
                }
                DBConnectionManagerLum.sendUpdate("DELETE FROM `Reminders` WHERE `ID` = ?", rs.getLong("ID"));
            }
            DBConnectionManagerLum.closeRequest(rs);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to get reminders", e);
        }
    }

    public static void start() {
        Timer timer = new Timer();
        Calendar time = Calendar.getInstance();
        time.set(Calendar.MILLISECOND, 0);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MINUTE, time.get(Calendar.MINUTE) + 1);
        timer.schedule(
            new Reminders(),
            time.getTime(),
            1000 * 60
        );
    }
}
