package slaynash.lum.bot.discord.commands;

import java.awt.Color;

import com.coder4.emoji.EmojiUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.ReactionListener;
import slaynash.lum.bot.discord.utils.MessageFinder;

public class AddReactionHandlerCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (!paramMessageReceivedEvent.getMember().hasPermission(Permission.MANAGE_ROLES) && !paramMessageReceivedEvent.getMember().getId().equals("145556654241349632")) {
            paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the Manage Role permission").queue();
            return;
        }
        String[] params = paramMessageReceivedEvent.getMessage().getContentRaw().split(" ");
        if (params.length != 4 || !params[1].matches("^[0-9]+$") || (!params[2].matches("^<a?:[A-Za-z0-9]+:[0-9]+>$") && !EmojiUtils.containsEmoji(params[2]))) {
            paramMessageReceivedEvent.getChannel().sendMessage("Usage: l!reaction <messageid> <reaction> [roleid]").queue();
            return;
        }

        new MessageFinder().findMessageAsync(paramMessageReceivedEvent.getGuild(), params[1], success -> {
            if (success == null) {
                paramMessageReceivedEvent.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("Error: Message not found", Color.RED)).queue();
                return;
            }

            ReactionListener react = null;
            for (ReactionListener rl : CommandManager.reactionListeners) {
                if (rl.messageId.equals(params[1]) && rl.emoteId.equals(params[2])) {
                    react = rl;
                    break;
                }
            }
            if (react != null) {
                CommandManager.reactionListeners.remove(react);
                /*
                if(params[2].matches("^<:.*:[0-9]+>$")) {
                    String emoteId = params[2].split(":")[2].split(">", 2)[0];
                    //success.getReactions().removeIf(mr -> mr.getReactionEmote().getId().equals(emoteId) && mr.get);
                    //success.rea(paramMessageReceivedEvent.getGuild().getEmoteById(emoteId));
                }
                else
                    success.addReaction(params[2]);
                */

                paramMessageReceivedEvent.getChannel().sendMessage("Successfully removed reaction listener from the target message").queue();
                CommandManager.saveReactions();
            }
            else {
                if (!params[3].matches("^[0-9]+$")) {
                    paramMessageReceivedEvent.getChannel().sendMessage("Usage: l!reaction <messageid> <reaction> [roleid]").queue();
                    return;
                }

                Role role = paramMessageReceivedEvent.getGuild().getRoleById(params[3]);
                if (role == null) {
                    paramMessageReceivedEvent.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("Error: Role not found", Color.RED)).queue();
                    return;
                }
                react = new ReactionListener(success.getId(), params[2].matches("^<a?:[A-Za-z0-9]+:[0-9]+>$") ? params[2].split(":")[2].split(">", 2)[0] : params[2], params[3]);

                if (params[2].matches("^<a?:[A-Za-z0-9]+:[0-9]+>$")) {
                    String emoteId = params[2].split(":")[2].split(">", 2)[0];
                    Emote emote = paramMessageReceivedEvent.getGuild().getEmoteById(emoteId);
                    if (emote == null) {
                        paramMessageReceivedEvent.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("Error: Emote not found on current server", Color.RED)).queue();
                        return;
                    }
                    success.addReaction(paramMessageReceivedEvent.getGuild().getEmoteById(emoteId)).queue();
                }
                else
                    success.addReaction(params[2]).queue();

                CommandManager.reactionListeners.add(react);
                paramMessageReceivedEvent.getChannel().sendMessage("Successfully added reaction listener to the target message").queue();
            }
            CommandManager.saveReactions();
        }, error -> paramMessageReceivedEvent.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("Error while looking for message: " + error.getMessage(), Color.RED)).queue());
    }

    @Override
    protected boolean matchPattern(String pattern) {
        return pattern.split(" ", 2)[0].equals("l!reaction");
    }

    @Override
    public String getHelpName() {
        return "l!reaction";
    }

    @Override
    public String getHelpDescription() {
        return "Toggle role assignation on react";
    }
}
