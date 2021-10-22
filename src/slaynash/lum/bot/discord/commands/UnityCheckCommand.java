package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.UnityVersionMonitor;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.utils.CrossServerUtils;

public class UnityCheckCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        String[] parts = paramString.split(" ");

        if (parts.length != 2 || !(parts[1].equals("runicalls") || parts[1].equals("runhashes"))) {
            event.getMessage().reply("usage: " + getName() + " <runicalls>");
            return;
        }

        Thread t = new Thread(() -> {
            if (parts[1].equals("runicalls"))
                UnityVersionMonitor.runFullICallCheck();
        }, "UnityCheckCommand");
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals(getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return CrossServerUtils.isLumDev(event.getMember()) && event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */;
    }

    @Override
    public String getHelpDescription() {
        return "Run unity checks";
    }

    @Override
    public String getName() {
        return "l!unitycheck";
    }
}
