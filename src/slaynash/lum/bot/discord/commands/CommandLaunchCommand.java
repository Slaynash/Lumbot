package slaynash.lum.bot.discord.commands;

import java.awt.Color;
import java.io.File;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.LuaPackages;

public class CommandLaunchCommand extends Command {
    
    @Override
    protected void onServer(String command, MessageReceivedEvent event) {
        System.out.println("loading lua file commands/" + event.getMessage().getContentRaw().split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z0-9.-]", "_"));
        Globals m_globals = LuaPackages.createCommandGlobals(event);
        try {
            File rom = new File("commands/" + command.substring(2).split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z0-9.-]", "_"));
            m_globals.get("dofile").call(LuaValue.valueOf(rom.toString()));
        }
        catch (LuaError e) {
            event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("An error has occured: \"" + command.substring(2).split(" ")[0] + "\"\n" + e.getMessage(), Color.RED)).queue();
            e.printStackTrace();
        }
    }

    @Override
    protected boolean matchPattern(String pattern) {
        if (pattern.startsWith("l!") && new File("commands/" + pattern.substring(2).split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z0-9.-]", "_")).exists() && !pattern.substring(2).split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z0-9.-]", "_").equals("")) {
            return true;
        }
        return false;
    }
    
    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return false;
    }
    
}
