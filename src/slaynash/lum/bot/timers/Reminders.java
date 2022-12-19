package slaynash.lum.bot.timers;

import java.sql.ResultSet;
import java.util.TimerTask;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Reminders extends TimerTask {
    public void run() {
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `Reminders` WHERE `TSend` < CURRENT_TIMESTAMP");
            while (rs.next()) {
                EmbedBuilder embedBuilder = new EmbedBuilder().setColor(rs.getInt("Color"));
                embedBuilder.setTitle("Reminder");
                embedBuilder.setDescription(rs.getString("Message"));

                long userID = rs.getLong("UserID");
                User user = JDAManager.getJDA().getUserById(userID);
                if (user == null) {
                    user = JDAManager.getJDA().retrieveUserById(userID).complete();
                }
                if (rs.getLong("ServerID") == 0) {
                    user.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(embedBuilder.build()).queue());
                }
                else {
                    JDAManager.getJDA().getGuildById(rs.getLong("ServerID")).getTextChannelById(rs.getLong("ChannelID"))
                            .sendMessageEmbeds(embedBuilder.build()).addContent(user.getAsMention()).queue();
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
