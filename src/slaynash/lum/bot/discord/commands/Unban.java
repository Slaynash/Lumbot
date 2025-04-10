package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild.Ban;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.utils.Utils;

public class Unban extends Command {
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        User unbanUser;
        String[] parts = paramString.split(" ", 2);
        if (parts.length < 2) {
            event.getMessage().reply("Usage: " + ConfigManager.discordPrefix + getName() + " <UserID>").queue();
            return;
        }
        try {
            unbanUser = event.getJDA().getUserById(parts[1]);
        }
        catch (Exception e) {
            event.getMessage().reply("Invalid snowflake, User was not found!").queue();
            return;
        }
        if (unbanUser == null) {
            event.getMessage().reply("User was not found!").queue();
            return;
        }
        new Thread(() -> {
            Ban bannedMember = event.getGuild().retrieveBan(unbanUser).complete();
            if (bannedMember == null) {
                event.getMessage().reply("User was not banned!").queue();
                return;
            }
            event.getGuild().unban(unbanUser).reason("Unbanned by " + event.getMember().getEffectiveName()).queue();

            MessageChannelUnion reportChannel = CommandManager.getModReportChannels(event, "ban");
            if (reportChannel != null && !reportChannel.equals(event.getChannel()))
                Utils.sendMessage("User " + unbanUser.getAsMention() + "(" + unbanUser.getId() + ") has been unbanned by " + event.getMember().getEffectiveName() + "!", reportChannel);
            event.getChannel().sendMessage("User " + unbanUser.getAsMention() + "(" + unbanUser.getId() + ") has been unbanned!").queue();
        }).start();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith(ConfigManager.discordPrefix + getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS) && event.getMember().hasPermission(Permission.BAN_MEMBERS);
    }

    @Override
    public String getHelpDescription() {
        return "Unbans a member with UserID- Staff only";
    }

    @Override
    public String getName() {
        return "unban";
    }
}
