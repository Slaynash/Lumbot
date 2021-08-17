package slaynash.lum.bot.discord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege.Type;

public class Moderation {
    public static void voiceJoin(GuildVoiceJoinEvent event) {
        Long guildID = (event.getGuild().getIdLong());
        if (GuildConfigurations.noMicChannels.containsKey(guildID) && event.getMember().hasPermission(Permission.MESSAGE_WRITE)) {
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

    public static List<Long> getAdminRoles(Guild guild) {
        List<Long> adminList = new ArrayList<>();
        for (Role role : guild.getRoles()) {
            if (role.hasPermission(Permission.ADMINISTRATOR))
                adminList.add(role.getIdLong());
        }
        return adminList;
    }

    public static Collection<? extends CommandPrivilege> getAdminsPrivileges(Guild guild) {
        List<Long> admins = getAdminRoles(guild);
        List<CommandPrivilege> adminPrivList = new ArrayList<>();
        adminPrivList.add(new CommandPrivilege(Type.USER, true, guild.getOwnerIdLong()));
        for (Long id : admins) {
            adminPrivList.add(new CommandPrivilege(Type.ROLE, true, id));
        }
        adminPrivList.add(new CommandPrivilege(Type.USER, true, 145556654241349632L/*Slay*/));
        adminPrivList.add(new CommandPrivilege(Type.USER, true, 240701606977470464L/*rakosi2*/));

        return adminPrivList.subList(0, Math.min(10, adminPrivList.size()));
    }
}
