package slaynash.lum.bot.timers;

import java.sql.ResultSet;
import java.util.TimerTask;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Reminders extends TimerTask {
    public void run() {
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `Reminders` WHERE `TSend` < CURRENT_TIMESTAMP");
            while (rs.next()) {
                String message = rs.getString("Message");
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
                if (serverID == 0) {
                    user.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(embedBuilder.build()).queue());
                }
                else if (!JDAManager.getJDA().getGuildById(serverID).getTextChannelById(channelID).canTalk()) {
                    // can't talk in channel, just delete the reminder. Not much I can do
                }
                else if (JDAManager.getJDA().getGuildById(serverID).getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS)) {
                    JDAManager.getJDA().getGuildById(serverID).getTextChannelById(channelID).sendMessage(message).addContent(user.getAsMention()).queue();
                }
                else {
                    JDAManager.getJDA().getGuildById(serverID).getTextChannelById(channelID).sendMessageEmbeds(embedBuilder.build()).addContent(user.getAsMention()).queue();
                }
                DBConnectionManagerLum.sendUpdate("DELETE FROM `Reminders` WHERE `ID` = " + rs.getLong("ID"));
            }
            DBConnectionManagerLum.closeRequest(rs);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to get reminders", e);
        }
    }
}
