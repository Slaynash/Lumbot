package slaynash.lum.bot.discord.commands;

import java.io.File;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.LuaPackages;
import slaynash.lum.bot.utils.ExceptionUtils;

public class CommandLaunchCommand extends Command {

    @Override
    protected void onServer(String command, MessageReceivedEvent event) {
        System.out.println("loading lua file commands/" + event.getMessage().getContentRaw().split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z\\d.-]", "_"));
        Globals m_globals = LuaPackages.createCommandGlobals(event);
        try {
            File rom = new File("commands/" + command.substring(2).split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z\\d.-]", "_") + ".lua");
            m_globals.get("dofile").call(LuaValue.valueOf(rom.toString()));
        }
        catch (LuaError e) {
            ExceptionUtils.reportException("An error has occurred: \"" + command.substring(2).split(" ")[0] + "\"", e);
        }
    }

    @Override
    protected boolean matchPattern(String pattern) {
        return pattern.startsWith(ConfigManager.discordPrefix) && new File("commands/" + pattern.substring(2).split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z\\d.-]", "_") + ".lua").exists() && !pattern.substring(2).split(" ")[0].isBlank();
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return false;
    }

}
