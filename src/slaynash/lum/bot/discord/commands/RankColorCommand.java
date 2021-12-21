package slaynash.lum.bot.discord.commands;

import java.awt.Color;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.restaction.RoleAction;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.utils.Utils;

public class RankColorCommand extends Command {

    String arg = "";

    @Override
    protected void onServer(String command, MessageReceivedEvent event) {
        try {
            if (command.split(" ").length == 1 || (arg = command.split(" ", 2)[1]).equals("help") || !arg.startsWith("#")) {
                event.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Usage: " + getName() + " <hexcolor>\nExemple (pure green): " + getName() + " #00ff00", Color.BLUE)).queue();
            }
            else if (arg.length() != 7) {
                event.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Bad hex color !\nUsage: " + getName() + " <hexcolor>\nExemple (pure green): " + getName() + " #00ff00", Color.RED)).queue();
            }
            else {
                for (char c:arg.substring(1).toCharArray()) {
                    if (!(('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F'))) {
                        event.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Bad hex color !\nUsage: " + getName() + " <hexcolor>\nExemple (pure green): " + getName() + " #00ff00", Color.RED)).queue();
                        return;
                    }
                }
                for (Role r:event.getMember().getRoles()) {
                    Color color = r.getColor();
                    if (r.getColor() != null && r.getName() != null && r.getName().startsWith("#") && r.getName().length() == 7 && r.getName().toLowerCase().equals(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()))) {
                        event.getGuild().removeRoleFromMember(event.getMember(), r).queue();
                    }
                }
                if (event.getGuild().getRolesByName(arg, true).size() == 0) {
                    List<Role> cr = event.getGuild().getRolesByName("_COLOR_DEFAULT_", true);
                    if (cr.size() == 0) {
                        event.getChannel().sendMessage("Please add a default role named `_COLOR_DEFAULT_` to enable color roles").queue();
                    }
                    else {
                        System.out.println("_COLOR_DEFAULT_ position: " + cr.get(0).getPosition() + " | " + cr.get(0).getPositionRaw());
                        try {
                            RoleAction r = event.getGuild().createCopyOfRole(cr.get(0)).setName(arg).setColor(CommandManager.hex2Rgb(arg));
                            r.queue(
                                role -> event.getGuild().modifyRolePositions().selectPosition(0).moveTo(cr.get(0).getPosition() - 1).queue(// insecure
                                    success -> event.getGuild().addRoleToMember(event.getMember(), role).queue(
                                        success2 -> { },
                                        error -> event.getChannel().sendMessage("I don't have enough permission to this role to you").queue()
                                    ),
                                    failure -> event.getChannel().sendMessage("Unable to move the role `" + arg + "` to the position of `_COLOR_DEFAULT_`").queue()
                                ),
                                role -> event.getChannel().sendMessage("Unable to create the role :(").queue()
                            );
                        }
                        catch (InsufficientPermissionException e) {
                            event.getChannel().sendMessage("I don't have the permission to create a role ! :(").queue();
                        }
                    }
                }
                else {
                    event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRolesByName(arg, true).get(0)).queue();
                }
            }
        }
        catch (Exception e) {
            event.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("An error has occurred:\n" + e + "\n at " + e.getStackTrace()[0], Color.RED)).queue();
        }
    }

    @Override
    protected void onClient(String command, MessageReceivedEvent event) {
        event.getChannel().sendMessage("This is not a server, there is no colors here :/").queue();
    }

    @Override
    protected boolean matchPattern(String pattern) {
        return pattern.split(" ", 2)[0].equals(getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES);
    }

    @Override
    public String getHelpDescription() {
        return "Set rank color. Example (pure green): " + this.getName() + " #00ff00";
    }

    @Override
    public String getName() {
        return "l!rankcolor";
    }
}
