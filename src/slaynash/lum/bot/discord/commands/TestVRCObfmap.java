package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.VRChatVersionComparer;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.utils.CrossServerUtils;

public class TestVRCObfmap extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!includeInHelp(event))
            return;

        String[] parts = paramString.split(" ");

        if (parts.length != 4) {
            event.getMessage().reply("usage: " + ConfigManager.discordPrefix + getName() + " <manifestid> <branch> <map url>").queue();
            return;
        }

        Thread t = new Thread(() -> {
            VRChatVersionComparer.obfMapUrl = parts[3];
            VRChatVersionComparer.run(parts[1], parts[2], event);
        }, "TestVRCObfMap");
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals(ConfigManager.discordPrefix + getName());
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return CrossServerUtils.isLumDev(event.getMember());
    }

    @Override
    public String getHelpDescription() {
        return "Tests the VRChat obfucation map";
    }

    @Override
    public String getName() {
        return "testvrcobfmap";
    }

}