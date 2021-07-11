package slaynash.lum.bot.discord.commands;

import java.util.List;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.melonscanner.MLHashPair;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;

public class MLHashRegisterCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (!includeInHelp(paramMessageReceivedEvent))
            return;

        String[] split = paramString.split(" ");
        if (split.length != 5) {
            paramMessageReceivedEvent.getChannel().sendMessage(printUsage()).queue();
            return;
        }

        String branch = split[1].trim();
        String version = split[2].trim();
        String hash86 = split[3].trim();
        String hash64 = split[4].trim();
        System.out.println("[MLHashRegisterCommand] branch: " + branch + ", hash: " + paramString + " for ML version " + version);

        if (!branch.equals("alpha") && !branch.equals("release")) {
            paramMessageReceivedEvent.getChannel().sendMessage("Invalid branch " + printUsage()).queue();
            return;
        }

        if (!version.matches("^\\d+\\.\\d+\\.\\d+(\\.\\d+)?$")) {
            paramMessageReceivedEvent.getChannel().sendMessage("Invalid version " + printUsage()).queue();
            return;
        }

        if (!(hash64.matches("^[0-9]{5,}$") && hash86.matches("^[0-9]{5,}$"))) {
            paramMessageReceivedEvent.getChannel().sendMessage("Invalid hash " + printUsage()).queue();
            return;
        }

        if (branch.equals("alpha")) {
            MelonScanner.latestMLVersionBeta = version;
            CommandManager.melonLoaderAlphaHashes.add(new MLHashPair(hash86, hash64));
        }
        else {
            MelonScanner.latestMLVersionRelease = version;
            CommandManager.melonLoaderHashes.add(new MLHashPair(hash86, hash64));
        }

        CommandManager.saveMLHashes();
        CommandManager.saveMelonLoaderVersions();
        paramMessageReceivedEvent.getChannel().sendMessage("Added hashes " + hash86 + " (x86) and " + hash64 + " (x64) to " + version + " " + branch + " branch").queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals("l!registermlhash");
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        if (event.getGuild().getIdLong() != 663449315876012052L) // MelonLoader
            return false;

        List<Role> memberRoles = event.getMember().getRoles();
        for (Role memberRole : memberRoles) {
            if (memberRole.getIdLong() == 663450403194798140L) { // Lava Gang
                return true;
            }
        }

        return false;
    }

    @Override
    public String getHelpDescription() {
        return "Whitelist a MelonLoader hash code";
    }

    @Override
    public String getHelpName() {
        return "l!registermlhash";
    }

    public String printUsage() {
        return "Usage: l!registermlhash <release|alpha> <ml version> <ml hash x86> <ml hash x64>";
    }
}
