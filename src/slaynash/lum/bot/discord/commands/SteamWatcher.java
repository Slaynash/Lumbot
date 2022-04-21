package slaynash.lum.bot.discord.commands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.ServerChannel;
import slaynash.lum.bot.steam.Steam;
import slaynash.lum.bot.utils.ExceptionUtils;

public class SteamWatcher extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;
        String[] parts = paramString.split(" ");
        String guildID = event.getGuild().getId();
        String channelID = event.getTextChannel().getId();
        if (!event.getTextChannel().canTalk())
            return;
        if (parts.length == 1) {
            List<ServerChannel> channels = new ArrayList<>();
            try {
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `SteamWatch` WHERE `ServerID` = '" + guildID + "'");
                while (rs.next())
                    channels.add(new ServerChannel(rs.getString("GameID"), rs.getString("ChannelID")));
                DBConnectionManagerLum.closeRequest(rs);
            } catch (SQLException e) {
                ExceptionUtils.reportException("Failed to list server's steam watch", e, event.getTextChannel());
            }

            if (channels.size() != 0) {
                StringBuilder sb = new StringBuilder("Current Steam games being watched:\n(Channel Name) -> (Game ID)\n");
                for (ServerChannel sc : channels)
                    sb.append(event.getJDA().getTextChannelById(sc.channelId).getName()).append(" -> ").append(sc.serverID).append("\n"); //maybe look into sorting by channels
                event.getMessage().reply(sb.toString()).queue();
            }
            else
                event.getMessage().reply("No games being watched. Usage: " + getName() + " <GameID>").queue();
            return;
        }
        if (!parts[1].matches("^\\d+$")) {
            event.getMessage().reply("Invalid GameID, please check your game ID and try again. For example, use `438100` for VRChat.").queue();
            return;
        }
        Integer gameID = Integer.parseInt(parts[1]);
        int found = 0;
        try {
            found = DBConnectionManagerLum.sendUpdate("DELETE FROM `SteamWatch` WHERE `GameID` = '" + gameID + "' AND `ServerID` = '" + guildID + "' AND `ChannelID` = '" + channelID + "'");
        } catch (SQLException e) {
            ExceptionUtils.reportException("Failed to remove steam watch", e, event.getTextChannel());
        }

        if (found == 0) {
            try {
                DBConnectionManagerLum.sendUpdate("INSERT INTO `SteamWatch` (`GameID`, `ServerID`, `ChannelID`, `TS`) VALUES ('" + gameID + "', '" + guildID + "', ' " + channelID + "', CURRENT_TIMESTAMP);");
                event.getMessage().reply("Added gameID " + gameID + " to Steam Watch").queue();
            } catch (SQLException e) {
                ExceptionUtils.reportException("Failed to add steam watch", e, event.getTextChannel());
            }
            new Steam().intDetails(gameID);
        }
        else {
            event.getMessage().reply("Removed gameID " + gameID + " from Steam Watch").queue();
        }
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals(getName());
    }

    @Override
    public String getName() {
        return "l!steamwatch";
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getMember().hasPermission(Permission.ADMINISTRATOR);
    }

    @Override
    public String getHelpDescription() {
        return "Watch Steam depo for game changes";
    }
}
