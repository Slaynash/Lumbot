package slaynash.lum.bot.discord.commands;

import java.util.List;
import java.util.Objects;

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
        if (parts.length == 1) {
            event.getMessage().reply("Usage: " + getName() + " <GameID>").queue();
            return;
        }
        Integer gameID = Integer.parseInt(parts[1]);
        ServerChannel sc = null;
        List<ServerChannel> rc = Steam.reportChannels.get(gameID);
        for (ServerChannel serverChannel : rc) {
            if (Objects.equals(serverChannel.serverID, event.getGuild().getId()) && Objects.equals(serverChannel.channelId, event.getTextChannel().getId())) {
                sc = serverChannel;
                break;
            }
        }
        if (sc == null) {
            rc.add(new ServerChannel(event.getGuild().getId(), event.getTextChannel().getId()));
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
