package slaynash.lum.bot.discord.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.melonscanner.MelonApiMod;
import slaynash.lum.bot.discord.melonscanner.MelonScannerApisManager;

public class MLBrokenModsCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!checkPerms(event))
            return;
        String message = "**Broken mods:**\n";
        List<String> brokenMods = null;
        synchronized (CommandManager.brokenMods) {
            brokenMods = new ArrayList<>(CommandManager.brokenMods);
        }
        brokenMods.sort(Comparator.comparing(String::toString));
        for (String s : brokenMods)
            message += s + "\n";
        List<MelonApiMod> knownMods = MelonScannerApisManager.getMods("VRChat");
        if (knownMods != null) {
            knownMods.sort(Comparator.comparing(MelonApiMod::getName));
            message += "\n**Non-broken mods:**\n";
            for (MelonApiMod md : knownMods) {
                String modname = md.name;
                boolean found = false;
                for (String s : brokenMods) {
                    if (s.equals(modname)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    message += modname + "\n";
            }
        }
        if (message.length() >= 2000) {
            String[] lines = message.split("\n");
            String toSend = "";
            int i = 0;
            while (i < lines.length) {
                if ((toSend + lines[i] + 1).length() > 2000) {
                    event.getChannel().sendMessage(toSend).queue();
                    toSend = lines[i];
                }
                else
                    toSend += "\n" + lines[i];
                ++i;
            }
            if (toSend.length() > 0)
                event.getChannel().sendMessage(toSend).queue();
        }
        else
            event.getChannel().sendMessage(message).queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals("l!vrcbrokenmod");
    }

    private boolean checkPerms(MessageReceivedEvent event) {
        //if (event.getMember().getIdLong() == 145556654241349632L) // Slaynash
        //    return true;

        Member member = event.getMember();

        List<Role> roles = member.getRoles();

        boolean hasPermissions = false;
        for (Role role : roles) {
            long roleId = role.getIdLong();
            if (roleId == 631581319670923274L /* Staff */ || roleId == 662720231591903243L /* Helper */ || roleId == 806278813335814165L /* Lum mods permission */ || roleId == 825266051277258754L /* Hidden VRCMG Staff */) {
                hasPermissions = true;
                break;
            }
        }

        return hasPermissions;
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return checkPerms(event);
    }

    @Override
    public String getHelpDescription() {
        return "List mods marked as broken for the Log Scanner";
    }

    @Override
    public String getHelpName() {
        return "l!vrcbrokenmod";
    }

}
