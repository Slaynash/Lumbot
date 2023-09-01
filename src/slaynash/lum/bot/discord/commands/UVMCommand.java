package slaynash.lum.bot.discord.commands;

import java.util.HashMap;
import java.util.function.Function;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.uvm.UnityVersionMonitor;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.utils.CrossServerUtils;

public class UVMCommand extends Command {

    private HashMap<String, Function<String[], Boolean>> subcommands = new HashMap<>() {{
        put("checkicalls", (args) -> { UnityVersionMonitor.runFullICallCheck(); return true; });
        // put("checkhashes", (args) -> { UnityVersionMonitor.runFullHashCheck(); return true; });
        // put("fulldownload", (args) -> { UnityVersionMonitor.runFullDownloadCheck(); return true; });
        put("checkintegrity", (args) -> { UnityVersionMonitor.runFullIntegrityCheck(); return true; });
        put("kill", (args) -> { UnityVersionMonitor.killThreads(); return true; });
        put("restart", (args) -> { UnityVersionMonitor.startMainThread(); return true; });
        put("setenabled", (args) -> {
            if (args.length != 1) return false;
            try {
                UnityVersionMonitor.setEnabled(Boolean.parseBoolean(args[0]));
            } catch (Exception e) {
                return false;
            }
            return true;
        });
    }};

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        String[] parts = paramString.split(" ", 3);
        String subcommandName;
        Function<String[], Boolean> subcommandRunnable;

        if (parts.length != 2 || (subcommandRunnable = subcommands.get((subcommandName = parts[1]))) == null) {
            event.getMessage().reply("Usage: " + getName() + " <subcommand>\nsubcommands: " + String.join(", ", subcommands.keySet())).queue();
            return;
        }

        String[] args = (parts.length == 3 && parts[2].length() > 0) ? parts[2].split(" ") : new String[0];

        UnityVersionMonitor.startThread(() -> {
            event.getMessage().reply("Starting checks \"" + subcommandName + "\"").queue();
            subcommandRunnable.apply(args);
        }, subcommandName);
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
        return "Trigger Unity Version Monitor commands/checks";
    }

    @Override
    public String getName() {
        return "l!uvm";
    }
}
