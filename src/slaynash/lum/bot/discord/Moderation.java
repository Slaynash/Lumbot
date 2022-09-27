package slaynash.lum.bot.discord;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;

public class Moderation {
    public static void voiceJoin(GuildVoiceJoinEvent event) {
        Long guildID = event.getGuild().getIdLong();
        if (GuildConfigurations.noMicChannels.containsKey(guildID) && event.getMember().hasPermission(Permission.MESSAGE_SEND)) {
            VoiceChannel vc = event.getGuild().getVoiceChannelById(GuildConfigurations.noMicChannels.get(guildID));
            if (vc != null) {
                vc.getManager().putMemberPermissionOverride(event.getMember().getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();
            }
        }
    }

    public static void voiceLeave(GuildVoiceLeaveEvent event) {
        Long guildID = event.getGuild().getIdLong();
        if (GuildConfigurations.noMicChannels.containsKey(guildID)) {
            VoiceChannel vc = event.getGuild().getVoiceChannelById(GuildConfigurations.noMicChannels.get(guildID));
            if (vc != null) {
                vc.getManager().removePermissionOverride(event.getMember().getIdLong()).queue();
            }
        }
    }

    public static void voiceStartup() {
        JDA jda = JDAManager.getJDA();
        for (Long chID : GuildConfigurations.noMicChannels.values()) {
            TextChannel channel = jda.getTextChannelById(chID);
            if (channel == null) {
                System.out.println("[ERROR] Mute Voice Channel " + chID + " is null");
                continue;
            }
            for (PermissionOverride override : channel.getMemberPermissionOverrides()) {
                if (override.isMemberOverride()) {
                    if (!override.getMember().getVoiceState().inAudioChannel()) {
                        try {
                            override.delete().queue();
                        }
                        catch (Exception e) {
                            System.out.println("Can't remove vc mute override from " + override.getMember().getEffectiveName() + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public static List<Long> getAdmins(Guild guild) {
        List<Long> adminList = new ArrayList<>();
        adminList.add(145556654241349632L/*Slay*/);
        adminList.add(240701606977470464L/*rakosi2*/);
        for (Member member : guild.getMembers()) {
            if (member.isOwner() || member.hasPermission(Permission.MANAGE_SERVER) || member.hasPermission(Permission.ADMINISTRATOR))
                adminList.add(member.getIdLong());
        }
        return adminList;
    }
}
