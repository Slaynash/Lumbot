package slaynash.lum.bot.discord.commands;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.discord.MelonLoaderScanner;
import slaynash.lum.bot.discord.logscanner.ModDetails;

public class MLBrokenModsCommand extends Command {
	
	@Override
	protected void onServer(String paramString, MessageReceivedEvent event) {
		if (!checkPerms(event))
			return;
		
		String[] parts = paramString.split(" ", 3);
		
		if (parts.length == 2 && parts[1].equals("list")) {
			
			String message = "**Broken mods:**\n";

			List<String> brokenMods = null;
			
			synchronized (CommandManager.brokenVrchatMods) {
				brokenMods = new ArrayList<>(CommandManager.brokenVrchatMods);
			}
			
			brokenMods.sort( Comparator.comparing( String::toString ));
			
			for (String s : brokenMods)
				message += s + "\n";
			
			
			
			List<ModDetails> knownMods = null;
			
			synchronized (MelonLoaderScanner.mods) {
				knownMods = MelonLoaderScanner.mods.get("VRChat");
				if (knownMods != null)
					knownMods = new ArrayList<>(knownMods);
			}
			
			if (knownMods != null) {
				knownMods.sort( Comparator.comparing( ModDetails::getName ));
					
				message += "\n**Non-broken mods:**\n";
				
				for (ModDetails md : knownMods) {
					String modname = md.name;
					
					boolean found = false;
					for (String s : brokenMods) {
						if (s.equals(modname)) {
							found = true;
							break;
						}
					}
					
					if (!found)
						message += modname + "\n";
				}
			}
			
			
			if (message.length() >= 2000) {
				String[] lines = message.split("\n");
				String toSend = "";
				int i = 0;
				while (i < lines.length) {
					if ((toSend + lines[i] + 1).length() > 2000) {
						event.getChannel().sendMessage(toSend).queue();
						toSend = lines[i];
					}
					else
						toSend += "\n" + lines[i];
					
					++i;
				}
				if (toSend.length() > 0)
					event.getChannel().sendMessage(toSend).queue();
			}
			else
				event.getChannel().sendMessage(message).queue();
		}
		else if (parts.length == 2 && parts[1].equals("addall")) {
			List<ModDetails> knownMods = null;
			
			synchronized (MelonLoaderScanner.mods) {
				knownMods = MelonLoaderScanner.mods.get("VRChat");
				
				if (knownMods == null) {
					event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Failed to get list of mods from API", Color.RED)).queue();
					return;
				}
				
				knownMods = new ArrayList<>(knownMods);
			}
			
			synchronized (CommandManager.brokenVrchatMods) {
				CommandManager.brokenVrchatMods.clear();
				CommandManager.brokenVrchatMods.addAll( knownMods.stream().map(ModDetails::getName).collect(Collectors.toList()) );
				CommandManager.saveBrokenVRChatMods();
			}
			event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Tagged all mods as broken", Color.GREEN)).queue();
			
		}
		else if (parts.length == 3 && parts[1].equals("add")) {
			if (CommandManager.brokenVrchatMods.contains(parts[2])) {
				event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("This mod is already marked as broken", Color.RED)).queue();
				return;
			}
			
			List<ModDetails> knownMods = null;
			
			synchronized (MelonLoaderScanner.mods) {
				knownMods = MelonLoaderScanner.mods.get("VRChat");
				
				if (knownMods == null) {
					event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Failed to get list of mods from API", Color.RED)).queue();
					return;
				}
				
				knownMods = new ArrayList<>(knownMods);
			}
				
			boolean found = false;
			for (ModDetails md : knownMods) {
				if (md.name.equals(parts[2])) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Unable to tag unknown mod as broken", Color.RED)).queue();
				return;
			}
			
			synchronized (CommandManager.brokenVrchatMods) {
				CommandManager.brokenVrchatMods.add(parts[2]);
				CommandManager.saveBrokenVRChatMods();
			}
			event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Tagged mod \"" + parts[2] + "\" as broken", Color.GREEN)).queue();
		}
		else if (parts.length == 3 && parts[1].equals("remove")) {
			if (!CommandManager.brokenVrchatMods.contains(parts[2])) {
				event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("This mod is not marked as broken", Color.RED)).queue();
				return;
			}
			
			synchronized (CommandManager.brokenVrchatMods) {
				CommandManager.brokenVrchatMods.remove(parts[2]);
				CommandManager.saveBrokenVRChatMods();
			}
			event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Untagged mod \"" + parts[2] + "\" from broken", Color.GREEN)).queue();
		}
		else {
			event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("**Usage**:\nl!vrcbrokenmod [add|remove|list|addall] <mod>", Color.RED)).queue();
		}
	}
	
	@Override
	protected boolean matchPattern(String paramString) {
		return paramString.split(" ", 2)[0].equals("l!vrcbrokenmod");
	}
	
	
	private boolean checkPerms(MessageReceivedEvent event) {
		//if (event.getMember().getIdLong() == 145556654241349632L) // Slaynash
		//	return true;
		
		Member member = event.getMember();
		
		if (event.getGuild().getIdLong() != 439093693769711616L) {// VRChat Modding Group
			System.out.println("[vrcbrokenmod] Command not run on the VRCMG");
			member = event.getJDA().getGuildById(439093693769711616L).getMember(event.getAuthor());
			System.out.println("[vrcbrokenmod] VRCMG member is " + member);
			
			if (member == null)
				return false;
		}
		
		List<Role> roles = member.getRoles();
		
		boolean hasPermissions = false;
		for (Role role : roles) {
			long roleId = role.getIdLong();
			if (roleId == 631581319670923274L /* Staff */ || roleId == 662720231591903243L /* Helper */ || roleId == 806278813335814165L /* Lum mods permission */) {
				hasPermissions = true;
				break;
			}
		}
		
		return hasPermissions;
	}
	
	@Override
	public boolean includeInHelp(MessageReceivedEvent event) {
		return checkPerms(event);
	}
	
	@Override
	public String getHelpDescription() {
		return "Manage mods marked as broken for the Log Scanner";
	}
	
	@Override
	public String getHelpName() {
		return "l!vrcbrokenmod";
	}
	
	
}
