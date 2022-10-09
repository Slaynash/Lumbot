package slaynash.lum.bot.discord.commands;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Blacklist extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        String[] parts = paramString.split(" ", 2);

        if (parts.length == 1) {
            event.getMessage().reply("Please add username").queue();
            return;
        }

        String username = Junidecode.unidecode(parts[1]).toLowerCase();

        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `blacklistusername` WHERE `username` = ? LIMIT 1", username);
            if (rs.next()) {
                event.getMessage().reply("Username already blacklisted").queue();
                DBConnectionManagerLum.closeRequest(rs);
                return;
            }
            DBConnectionManagerLum.closeRequest(rs);
            int update = DBConnectionManagerLum.sendUpdate("INSERT INTO `blacklistusername`(`username`) VALUES (?)", username);
            if (update == 0)
                event.getMessage().reply("Failed to blacklist username").queue();
            else {
                event.getMessage().reply("Successfully blacklisted username " + username).queue();
                if (event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
                    List<Member> members = new ArrayList<>();
                    event.getGuild().loadMembers(m -> {
                        if (m.getNickname() != null && Junidecode.unidecode(m.getNickname()).toLowerCase().equals(username)) {
                            members.add(m);
                            m.kick().reason("Lum: Remove existing Scammers").queue();
                            return;
                        }
                        if (Junidecode.unidecode(m.getUser().getName()).toLowerCase().equals(username)) {
                            members.add(m);
                            m.kick().reason("Lum: Remove existing Scammers").queue();
                        }
                    });
                    if (members.size() > 0) {
                        event.getMessage().reply("Kicked " + members.size() + " members with username " + username).queue();
                        String report = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
                        if (report == null) return;
                        TextChannel reportchannel = event.getGuild().getTextChannelById(report);
                        if (reportchannel == null) return;
                        for (Member m : members) {
                            reportchannel.sendMessage("Kicked " + m.getUser().getAsTag() + " by setting blacklist").setAllowedMentions(Collections.emptyList()).queue();
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to add blacklist", e, event.getChannel().asGuildMessageChannel());
        }
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals(getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return CrossServerUtils.isLumDev(event.getMember());
    }

    @Override
    public String getHelpDescription() {
        return "Add a username to the blacklist";
    }

    @Override
    public String getName() {
        return "l!blacklist";
    }

}
