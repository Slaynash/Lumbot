package slaynash.lum.bot.discord.commands;

import java.util.List;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.MLHashPair;

public class MLHashRegisterCommand extends Command {
    
    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (paramMessageReceivedEvent.getGuild().getIdLong() != 663449315876012052L) // MelonLoader
            return;
        
        List<Role> memberRoles = paramMessageReceivedEvent.getMember().getRoles();
        boolean isLavaGang = false;
        for (Role memberRole : memberRoles) {
            if (memberRole.getIdLong() == 663450403194798140L) { // Lava Gang
                isLavaGang = true;
                break;
            }
        }
        
        if (!isLavaGang)
            return;
        
        String[] split = paramString.split(" ", 3);
        if (split.length != 3) {
            paramMessageReceivedEvent.getChannel().sendMessage("Usage: l!registermlhash <release|alpha> <ml hash x86> <ml hash x64>").queue();
            return;
        }

        String branch = split[1];
        String hash86 = split[2];
        String hash64 = split[2];
        System.out.println("[MLHashRegisterCommand] branch: " + branch + ", hash: " + paramString);

        if (!branch.equals("alpha") && !branch.equals("release")) {
            paramMessageReceivedEvent.getChannel().sendMessage("Usage: l!registermlhash <release|alpha> <ml hash x86> <ml hash x64>").queue();
            return;
        }
        
        try {
            Integer.parseInt(hash86);
            Integer.parseInt(hash64);
        }
        catch (Exception e) {
            paramMessageReceivedEvent.getChannel().sendMessage("Usage: l!registermlhash <release|alpha> <ml hash x86> <ml hash x64>").queue();
            return;
        }
        
        if (branch.equals("alpha"))
            CommandManager.melonLoaderAlphaHashes.add(new MLHashPair(hash86, hash64));
        else
            CommandManager.melonLoaderHashes.add(new MLHashPair(hash86, hash64));
        
        CommandManager.saveMLHashes();
        paramMessageReceivedEvent.getChannel().sendMessage("Added hashes " + hash86 + " (x86) and " + hash64 + " (x64) to branch " + branch).queue();
    }
    
    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals("l!registermlhash");
    }
    
    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        if (event.getGuild().getIdLong() != 663449315876012052L) // MelonLoader
            return false;
        
        List<Role> memberRoles = event.getMember().getRoles();
        for (Role memberRole : memberRoles) {
            if (memberRole.getIdLong() == 663450403194798140L) { // Lava Gang
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public String getHelpDescription() {
        return "Whitelist a MelonLoader hash code";
    }
    
    @Override
    public String getHelpName() {
        return "l!registermlhash";
    }
    
}
