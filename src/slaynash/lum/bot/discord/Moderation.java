package slaynash.lum.bot.discord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege.Type;

public class Moderation {
    public static void voiceJoin(GuildVoiceJoinEvent event) {
        Long guildID = (event.getGuild().getIdLong());
        if (GuildConfigurations.noMicChannels.containsKey(guildID)) {
            event.getGuild().getGuildChannelById(GuildConfigurations.noMicChannels.get(guildID)).getManager().putPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE), null).queue();
        }
    }

    public static void voiceLeave(GuildVoiceLeaveEvent event) {
        Long guildID = (event.getGuild().getIdLong());
        if (GuildConfigurations.noMicChannels.containsKey(guildID)) {
            event.getGuild().getGuildChannelById(GuildConfigurations.noMicChannels.get(guildID)).getManager().removePermissionOverride(event.getMember()).queue();
        }
    }

    public static List<Long> getAdmins(Guild guild) {
        List<Long> adminList = new ArrayList<>();
        adminList.add(145556654241349632L/*Slay*/);
        adminList.add(240701606977470464L/*rakosi2*/);
        for (Member member : guild.getMembers()) {
            if (member.hasPermission(Permission.ADMINISTRATOR))
                adminList.add(member.getIdLong());
        }
        return adminList;
    }

    public static Collection<? extends CommandPrivilege> getAdminsPrivileges(Guild guild) {
        List<Long> admins = getAdmins(guild);
        List<CommandPrivilege> adminPrivList = new ArrayList<>();
        for (Long id : admins) {
            adminPrivList.add(new CommandPrivilege(Type.USER, true, id));
        }
        return adminPrivList;
    }
}
