package slaynash.lum.bot.discord.slashs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.steam.Steam;
import slaynash.lum.bot.steam.SteamChannel;
import slaynash.lum.bot.utils.ExceptionUtils;

public class SteamWatcher extends Slash {
    @Override
    protected CommandData globalSlashData() {
        return new CommandData("steam", "Steam Watcher")
            .addOption(OptionType.STRING, "gameid", "Enter Game ID, blank for list", false)
            .addOption(OptionType.STRING, "public", "Enter Mention/Message for public changes", false)
            .addOption(OptionType.STRING, "beta", "Enter Mention/Message for public beta", false)
            .addOption(OptionType.STRING, "other", "Enter Mention/Message for non-public changes", false)
            .setDefaultEnabled(false);
    }

    @Override
    public void slashRun(SlashCommandEvent event) {
        if (event.getChannelType() == ChannelType.PRIVATE) {
            event.reply("Steam Watch currently does not work in DMs").queue();
            return;
        }
        InteractionHook interactionhook = event.deferReply().complete();
        String guildID = event.getGuild().getId();
        String channelID = event.getTextChannel().getId();
        List<OptionMapping> gameID = event.getOptionsByName("gameid");
        List<OptionMapping> publicMess = event.getOptionsByName("public");
        List<OptionMapping> betaMess = event.getOptionsByName("beta");
        List<OptionMapping> otherMess = event.getOptionsByName("other");
        List<SteamChannel> channels = new ArrayList<>();
        if (gameID.isEmpty()) {
            try {
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `SteamWatch` WHERE `ServerID` = '" + guildID + "'");
                while (rs.next())
                    channels.add(new SteamChannel(rs.getString("GameID"), guildID, rs.getString("ChannelID"), rs.getString("publicMention"), rs.getString("betaMention"), rs.getString("otherMention")));
                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to list server's steam watch", e, event.getTextChannel());
            }
            if (channels.isEmpty()) {
                interactionhook.sendMessage("No steam watch channels set up").queue();
            }
            else {
                StringBuilder sb = new StringBuilder("Current Steam games being watched:\n(Channel Name) -> (Game)\n");
                for (SteamChannel sc : channels) {
                    sb.append(event.getJDA().getTextChannelById(sc.channelId).getName()).append(" -> ").append(new Steam().getGameName(Integer.parseInt(sc.gameID))).append(" (").append(sc.gameID).append(")"); //maybe look into sorting by channels
                    if (sc.publicMessage != null) sb.append(" (Public: ").append(sc.publicMessage).append(")");
                    if (sc.betaMessage != null) sb.append(" (Beta: ").append(sc.betaMessage).append(")");
                    if (sc.otherMessage != null) sb.append(" (Other: ").append(sc.otherMessage).append(")");
                    sb.append("\n");
                }
                interactionhook.sendMessage(sb.toString()).queue();
            }
            return;
        }
        String gameIDstr = gameID.get(0).getAsString();
        if (gameIDstr == null || !gameIDstr.matches("^\\d+$")) {
            interactionhook.sendMessage("Invalid GameID, please check your game ID and try again. For example, use `438100` for VRChat.").setEphemeral(true).queue();
            return;
        }
        Integer gameIDint = Integer.parseInt(gameIDstr);
        int found = 0;
        try {
            found = DBConnectionManagerLum.sendUpdate("DELETE FROM `SteamWatch` WHERE `GameID` = ? AND `ServerID` = ? AND `ChannelID` = ?", gameIDstr, guildID, channelID);
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("Failed to remove steam watch", e, event.getTextChannel());
        }

        String publicString;
        String betaString;
        String otherString;

        if (publicMess.isEmpty())
            publicString = null;
        else
            publicString = "'" + publicMess.get(0).getAsString() + "'";
        if (betaMess.isEmpty())
            betaString = null;
        else
            betaString = "'" + betaMess.get(0).getAsString() + "'";
        if (otherMess.isEmpty())
            otherString = null;
        else
            otherString = "'" + otherMess.get(0).getAsString() + "'";

        if (found == 0) {
            try {
                DBConnectionManagerLum.sendUpdate("INSERT INTO `SteamWatch` (`GameID`, `ServerID`, `ChannelID`, `publicMention`, `betaMention`, `otherMention`) VALUES (?,?,?,?,?,?)", gameIDstr, guildID, channelID, publicString, betaString, otherString);
                interactionhook.sendMessage("Added " + new Steam().getGameName(gameIDint) + " to Steam Watch").queue();
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to add steam watch", e, event.getTextChannel());
            }
            new Steam().intDetails(gameIDint);
        }
        else {
            interactionhook.sendMessage("Removed " + new Steam().getGameName(gameIDint) + " from Steam Watch").queue();
        }
    }
}
