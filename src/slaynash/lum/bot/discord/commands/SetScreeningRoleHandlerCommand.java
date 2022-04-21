package slaynash.lum.bot.discord.commands;

import java.awt.Color;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.Utils;

public class SetScreeningRoleHandlerCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (!includeInHelp(paramMessageReceivedEvent)) {
            paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the Manage Role permission").queue();
            return;
        }
        String[] params = paramMessageReceivedEvent.getMessage().getContentRaw().split(" ");
        if (params.length > 2 || params.length == 2 && !params[1].matches("^\\d+$")) {
            paramMessageReceivedEvent.getChannel().sendMessage("Usage: " + getName() + " [roleid]").queue();
            return;
        }

        if (params.length == 2) {
            Role role = paramMessageReceivedEvent.getGuild().getRoleById(params[1]);
            if (role == null) {
                paramMessageReceivedEvent.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Error: Role not found", Color.RED)).queue();
                return;
            }

            CommandManager.autoScreeningRoles.put(paramMessageReceivedEvent.getGuild().getIdLong(), role.getIdLong());
            CommandManager.saveScreenings();
            paramMessageReceivedEvent.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Successfully set screening role", Color.GREEN)).queue();
        }
        else {
            CommandManager.autoScreeningRoles.remove(paramMessageReceivedEvent.getGuild().getIdLong());
            CommandManager.saveScreenings();
            paramMessageReceivedEvent.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Successfully removed screening role", Color.GREEN)).queue();
        }

    }

    @Override
    protected boolean matchPattern(String pattern) {
        return pattern.split(" ", 2)[0].equals(getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getMember().hasPermission(Permission.MANAGE_ROLES) || CrossServerUtils.isLumDev(event.getMember());
    }

    @Override
    public String getName() {
        return "l!setscreeningrole";
    }

    @Override
    public String getHelpDescription() {
        return "Set target role assignation on membership screening accept, or disable if empty";
    }
}
