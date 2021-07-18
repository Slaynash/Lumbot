package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.VRChatVersionComparer;
import slaynash.lum.bot.discord.Command;

public class TestVRCObfmap extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (event.getMember().getIdLong() != 145556654241349632L) // Slaynash
            return;

        String[] parts = paramString.split(" ");

        if (parts.length != 4) {
            event.getMessage().reply("usage: l!testvrcobfmap <manifestid> <branch> <map url>");
            return;
        }

        VRChatVersionComparer.obfMapUrl = parts[3];
        VRChatVersionComparer.run(parts[1], parts[2]);
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals("l!testvrcobfmap");
    }
    
}
