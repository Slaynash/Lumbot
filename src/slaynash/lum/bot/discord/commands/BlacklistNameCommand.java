package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class BlacklistNameCommand extends Command {
    
    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (paramMessageReceivedEvent.getGuild().getIdLong() != 439093693769711616L && paramMessageReceivedEvent.getGuild().getIdLong() != 663449315876012052L && paramMessageReceivedEvent.getGuild().getIdLong() != 600298024425619456L) {
            paramMessageReceivedEvent.getChannel().sendMessage("Error: This command can't be used on this server").queue();
            return;
        }
        
        if(!paramMessageReceivedEvent.getMember().hasPermission(Permission.MANAGE_ROLES) && !paramMessageReceivedEvent.getMember().getId().equals("145556654241349632")) {
            paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the Manage Role permission").queue();
            return;
        }
        String[] params = paramMessageReceivedEvent.getMessage().getContentRaw().split(" ", 2);
        if(params.length < 2 || params[1].trim().equals("")) {
            paramMessageReceivedEvent.getChannel().sendMessage("Usage: l!blacklist <name part>").queue();
            return;
        }
        
        String nameTrimed = params[1].trim();
        
        if (CommandManager.blacklistedNames.contains(nameTrimed)) {
            CommandManager.blacklistedNames.remove(nameTrimed);
            paramMessageReceivedEvent.getChannel().sendMessage("Removed `" + nameTrimed + "` from the blacklist").queue();
        }
        else {
            CommandManager.blacklistedNames.add(nameTrimed);
            paramMessageReceivedEvent.getChannel().sendMessage("Added `" + nameTrimed + "` to the blacklist").queue();
        }
        CommandManager.saveNameBlacklist();
    }
    
    @Override
    protected boolean matchPattern(String pattern) {
        return pattern.split(" ", 2)[0].equals("l!blacklist");
    }
    
    @Override
    public String getHelpName() {
        return "l!blacklist";
    }
    
    @Override
    public String getHelpDescription() {
        return "Blacklist an username";
    }
    
}
