package slaynash.lum.bot.discord.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.ServerChannel;
import slaynash.lum.bot.steam.Steam;

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
            boolean found = false;
            StringBuilder sb = new StringBuilder("Current Steam games being watched:\n");
            for (Entry<Integer, List<ServerChannel>> gEntry : Steam.reportChannels.entrySet()) {
                for (ServerChannel sc : gEntry.getValue()) {
                    if (Objects.equals(sc.serverID, guildID)) {
                        sb.append(event.getJDA().getTextChannelById(sc.channelId).getName()).append(" -> ").append(gEntry.getKey()).append("\n"); //maybe look into sorting by channels
                        found = true;
                    }
                }
            }
            if (found)
                event.getMessage().reply(sb.toString()).queue();
            else
                event.getMessage().reply("Usage: " + getName() + " <GameID>").queue();
            return;
        }
        Integer gameID = Integer.parseInt(parts[1]);
        ServerChannel sc = null;
        List<ServerChannel> rc = Steam.reportChannels.getOrDefault(gameID, new ArrayList<>());
        if (rc.isEmpty())
            new Steam().getDetails(gameID);
        else
            for (ServerChannel serverChannel : rc) {
                if (Objects.equals(serverChannel.serverID, guildID) && Objects.equals(serverChannel.channelId, channelID)) {
                    sc = serverChannel;
                    break;
                }
            }
        if (sc == null) {
            rc.add(new ServerChannel(guildID, channelID));
            event.getMessage().reply("Added gameID " + gameID + " to Steam Watch").queue();
        }
        else {
            rc.remove(sc);
            event.getMessage().reply("Removed gameID " + gameID + " from Steam Watch").queue();
        }
        Steam.reportChannels.put(gameID, rc);
        CommandManager.saveSteamWatch();
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
