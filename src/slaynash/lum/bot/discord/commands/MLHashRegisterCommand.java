package slaynash.lum.bot.discord.commands;

import java.util.List;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;

public class MLHashRegisterCommand extends Command {
	
	@Override
	protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
		String hash = paramString.split(" ", 2)[1];
		System.out.println("[MLHashRegisterCommand] hash: " + paramString);
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
		
		
		try {
			Integer.parseInt(hash);
		}
		catch (Exception e) {
			paramMessageReceivedEvent.getChannel().sendMessage("Usage: l!registermlhash <ml hash>").queue();
			return;
		}
		
		CommandManager.melonLoaderHashes.add(hash);
		CommandManager.saveMLHashes();
		paramMessageReceivedEvent.getChannel().sendMessage("Added hash " + hash).queue();
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
