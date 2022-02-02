package slaynash.lum.bot.discord.commands;

import java.util.concurrent.atomic.AtomicInteger;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.utils.CrossServerUtils;

public class AddMissingRoles extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        addMissing(event);
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.startsWith(getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return CrossServerUtils.isLumDev(event.getMember());
    }

    @Override
    public String getHelpDescription() {
        return "Scans throgh all members for missing roles from screening acceptance";
    }

    @Override
    public String getName() {
        return "l!addmissing";
    }

    public void addMissing(MessageReceivedEvent event) {
        AtomicInteger runCount = new AtomicInteger(0);
        CommandManager.autoScreeningRoles.forEach((k, v) -> {
            Guild guild = JDAManager.getJDA().getGuildById(k);
            if (guild == null)
                return;
            Role role = guild.getRoleById(v);
            if (role == null)
                return;
            if (!guild.getSelfMember().canInteract(role)) {
                System.out.println("Lum can not modify role " + role.getName() + " in " + guild.getName() + " " + k);
                //TODO announce that Lum can not interact with role
                return;
            }
            guild.loadMembers(m -> {
                if (!m.isPending() && !m.getRoles().contains(role)) {
                    try {
                        guild.addRoleToMember(m, role).reason("User has agreed to Membership Screening requirements while Lum was rebooting").queue(null, z -> System.out.println("Failed to regive role to " + m.getUser().getAsTag() + " in " + guild.getName()));
                        System.out.println("Giving role " + role.getName() + " to " + m.getEffectiveName() + " in " + guild.getName());
                        runCount.getAndIncrement();
                    }
                    catch (Exception ignored) {
                    }
                }
            });
        });
        if (event != null) {
            event.getMessage().reply("Added roles to " + runCount.get() + " members").queue();
        }
    }
}
