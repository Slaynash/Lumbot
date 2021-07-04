package slaynash.lum.bot.discord.commands;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public class SetVRCBuild extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!checkPerms(event))
            return;

        String[] parts = paramString.split(" ", 2);

        try {
            Integer.parseInt(parts[1]);

            if (CommandManager.vrchatBuild.equals(parts[1])) {
                event.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("VRChat build is already set to " + parts[1], Color.ORANGE)).queue();
                return;
            }

            CommandManager.vrchatBuild = parts[1];

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("vrchatbuild.txt"))) {
                writer.write(parts[1]);
            }
            catch (IOException e) {
                ExceptionUtils.reportException("Failed to save VRChat Build", e);
            }

            event.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("VRChat build is now set to " + parts[1], Color.GREEN)).queue();

        }
        catch (Exception e) {
            event.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("**Usage**:\nl!vrcbuild <build>", Color.RED)).queue();
        }
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals("l!vrcbuild");
    }

    private boolean checkPerms(MessageReceivedEvent event) {
        //if (event.getMember().getIdLong() == 145556654241349632L) // Slaynash
        //    return true;

        Member member = event.getMember();

        if (event.getGuild().getIdLong() != 439093693769711616L) { // VRChat Modding Group
            System.out.println("[vrcbrokenmod] Command not run on the VRCMG");
            member = event.getJDA().getGuildById(439093693769711616L).getMember(event.getAuthor());
            System.out.println("[vrcbrokenmod] VRCMG member is " + member);

            if (member == null)
                return false;
        }

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
        return "Sets the latest VRChat build";
    }

    @Override
    public String getHelpName() {
        return "l!vrcbuild";
    }

}
