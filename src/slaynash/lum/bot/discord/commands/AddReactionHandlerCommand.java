package slaynash.lum.bot.discord.commands;

import java.awt.Color;

import com.coder4.emoji.EmojiUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.ReactionListener;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.discord.utils.MessageFinder;
import slaynash.lum.bot.utils.Utils;

public class AddReactionHandlerCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (!paramMessageReceivedEvent.getMember().hasPermission(Permission.MANAGE_ROLES) && !CrossServerUtils.isLumDev(paramMessageReceivedEvent.getMember())) {
            paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the Manage Role permission").queue();
            return;
        }
        String[] params = paramMessageReceivedEvent.getMessage().getContentRaw().replace("  ", " ").split(" ");
        if (params.length != 3 && params.length != 4) {
            System.out.println("Bad usage");
            paramMessageReceivedEvent.getChannel().sendMessage("Usage: " + getName() + " <messageid> <reaction> [roleid]").queue();
            return;
        }
        else if (!params[1].matches("^\\d+$")) {
            System.out.println("Bad MessageID permas");
            paramMessageReceivedEvent.getChannel().sendMessage("messageID invalid Usage: " + getName() + " <messageid> <reaction> [roleid]").queue();
            return;
        }
        if (!params[2].matches("^<a?:\\w+:\\d+>$") && !EmojiUtils.containsEmoji(params[2])) {
            paramMessageReceivedEvent.getChannel().sendMessage("Bad emoji Usage: " + getName() + " <messageid> <reaction> [roleid]").queue();
            return;
        }

        new MessageFinder().findMessageAsync(paramMessageReceivedEvent.getGuild(), params[1], success -> {
            if (success == null) {
                paramMessageReceivedEvent.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Error: Message not found", Color.RED)).queue();
                System.out.println("Failed to find message for Add Reaction");
                return;
            }

            ReactionListener react = null;
            for (ReactionListener rl : CommandManager.reactionListeners) {
                if (rl.messageId().equals(params[1]) && rl.emoteId().equals(params[2].matches("^<a?:\\w+:\\d+>$") ? params[2].split(":")[2].split(">", 2)[0] : params[2])) {
                    react = rl;
                    break;
                }
            }
            if (react != null) {
                System.out.println("Removing existing reaction");
                CommandManager.reactionListeners.remove(react);
                /*
                if(params[2].matches("^<:.*:[0-9]+>$")) {
                    String emoteId = params[2].split(":")[2].split(">", 2)[0];
                    //success.getReactions().removeIf(mr -> mr.getReactionEmote().getId().equals(emoteId) && mr.get);
                    //success.rea(paramMessageReceivedEvent.getGuild().getEmojiById(emoteId));
                }
                else
                    success.addReaction(params[2]);
                */

                paramMessageReceivedEvent.getChannel().sendMessage("Successfully removed reaction listener from the target message").queue();
                CommandManager.saveReactions();
            }
            else {
                if (params.length != 4 || !params[3].matches("^\\d+$")) {
                    paramMessageReceivedEvent.getChannel().sendMessage("RoleID Error Usage: " + getName() + " <messageid> <reaction> <roleid>").queue();
                    return;
                }

                Role role = paramMessageReceivedEvent.getGuild().getRoleById(params[3]);
                if (role == null) {
                    Utils.sendEmbed("Error: Role not found", Color.RED, paramMessageReceivedEvent);
                    System.out.println("Role not found");
                    return;
                }
                react = new ReactionListener(success.getId(), params[2].matches("^<a?:\\w+:\\d+>$") ? params[2].split(":")[2].split(">", 2)[0] : params[2], params[3]);

                if (params[2].matches("^<a?:\\w+:\\d+>$")) {
                    String emoteId = params[2].split(":")[2].split(">", 2)[0];
                    Emoji emote = paramMessageReceivedEvent.getGuild().getEmojiById(emoteId);
                    if (emote == null) {
                        paramMessageReceivedEvent.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Error: Emote not found on current server", Color.RED)).queue();
                        System.out.println("Emote not found on current server");
                        return;
                    }
                    success.addReaction(paramMessageReceivedEvent.getGuild().getEmojiById(emoteId)).queue();
                }
                else
                    success.addReaction(Emoji.fromUnicode(params[2])).queue();

                CommandManager.reactionListeners.add(react);
                if (paramMessageReceivedEvent.getChannelType() == ChannelType.TEXT && paramMessageReceivedEvent.getGuild().getSelfMember().hasPermission(paramMessageReceivedEvent.getChannel().asTextChannel(), Permission.MESSAGE_SEND))
                    paramMessageReceivedEvent.getChannel().sendMessage("Successfully added reaction listener to the target message").queue();
                System.out.println("Successfully added reaction listener to the target message");
            }
            CommandManager.saveReactions();
        }, error -> paramMessageReceivedEvent.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Error while looking for message: " + error.getMessage(), Color.RED)).queue());
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
        return "l!reaction";
    }

    @Override
    public String getHelpDescription() {
        return "Toggle role assignation on react";
    }
}
