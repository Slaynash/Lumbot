package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildAvailableEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Main extends ListenerAdapter {
    public static JDA jda;

	private static int lastAMLUsesCount;

    public static void main(String[] args) throws LoginException, IllegalArgumentException, InterruptedException, RateLimitedException {
        System.setProperty("jsse.enableSNIExtension", "false");
        TrustManager[] trustAllCertificates = new TrustManager[]{new X509TrustManager(){
            @Override public X509Certificate[] getAcceptedIssuers() { return null; }
            @Override public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            @Override public void checkServerTrusted(X509Certificate[] certs, String authType) { }
        }};
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCertificates, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        catch (GeneralSecurityException e) {
            throw new ExceptionInInitializerError(e);
        }
        if (args.length < 1) {
            System.err.println("Please specify token in passed arguments");
            return;
        }
        
        loadLogchannelList();
        loadVerifychannelList();
        loadReactionsList();
        loadNameBlacklist();
        loadMelonLoaderVersions();
        MelonLoaderScanner.Init();
        
        CommandManager.init();
        //new slaynash.lum.bot.discord.Console();
        JDAManager.init(args[0]);
        
        /*
        JDAManager.getJDA().getGuildById("365589812616364036").retrieveInvites().queue((list) -> {
			for(Invite inv : list) {
				if(inv.getCode().equals("V35fm3u")) {
					lastAMLUsesCount = inv.getUses();
					//System.out.println("lastAMLUsesCount: " + lastAMLUsesCount);
					break;
				}
			}
		});
        */
        
        ServerManager.Start();
        System.out.println("LUM Started!");
    }

    private static void loadReactionsList() {
    	BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader("rolereactions.txt"));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(" ", 4);
				if(parts.length == 3 && parts[0].matches("^\\d+$") && parts[2].matches("^\\d+$")) {
					CommandManager.reactionListeners.add(new ReactionListener(parts[0], parts[1], parts[2]));
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    private static void loadNameBlacklist() {
    	BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader("nameblacklist.txt"));
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.trim().equals(""))
				CommandManager.blacklistedNames.add(line.trim());
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    private static void loadLogchannelList() {
    	BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader("logchannels.txt"));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(" ", 4);
				if(parts.length == 2 && parts[0].matches("^\\d+$") && parts[1].matches("^\\d+$")) {
					CommandManager.logChannels.put(Long.parseLong(parts[0]), parts[1]);
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    private static void loadVerifychannelList() {
    	BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader("verifychannels.txt"));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(" ", 5);
				if(parts.length == 3 && parts[0].matches("^\\d+$") && parts[1].matches("^\\d+$") && parts[2].matches("^\\d+$")) {
					CommandManager.verifyChannels.put(Long.parseLong(parts[0]), new VerifyPair(parts[1], parts[2]));
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    private static void loadMelonLoaderVersions() {
    	BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader("melonloaderversions.txt"));
			MelonLoaderScanner.latestMLVersionRelease = reader.readLine().trim();
			MelonLoaderScanner.latestMLVersionBeta = reader.readLine().trim();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            PrivateMessagesHandler.handle(event);
        } else {
            ServerMessagesHandler.handle(event);
        }
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
    	//System.out.println("[" + event.getGuild().getName() + "] [#" + event.getChannel().getName() + "] " + event.getUser().getName() + " reacted with " + event.getReactionEmote().getName() + "(isEmote: " + event.getReactionEmote().isEmote() + ")");
    	for(ReactionListener rl : CommandManager.reactionListeners) {
    		if(event.getMessageId().equals(rl.messageId) && (event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getId().equals(rl.emoteId) : event.getReactionEmote().getName().equals(rl.emoteId))) {
    			Role role = event.getGuild().getRoleById(rl.roleId);
    			if(role != null) {
	    			event.getGuild().addRoleToMember(event.getMember(), role).queue();
	    			WriteLogMessage(event.getGuild(), "Added role `" + role.getName() + "` to <@" + event.getUser().getId() + ">");
    			}
                return;
    		}
    	}
    	/*
        if(true)
        	return;
        if (event.getMessageId().equals("446648726648520704") && event.getReactionEmote().getName().equals("\u2705")) { // MetrixVR
            //System.out.println("Adding Member role to " + event.getMember().getUser().getId());
        	for(Role role : event.getMember().getRoles())
        		if(role.getId().equals("446708059247607809")) {
                    event.getGuild().getController().removeSingleRoleFromMember(event.getMember(), event.getGuild().getRoleById("446708059247607809")).queue();
                    return;
        		}
            event.getGuild().getController().addSingleRoleToMember(event.getMember(), event.getGuild().getRoleById("446708059247607809")).queue();
            //event.getReaction().removeReaction().queue();
            return;
        }//548534015108448256
        if (event.getMessageId().equals("548538000552886273") && event.getReactionEmote().getName().equals("\u2705")) { // VRCTools
            //System.out.println("Adding Member role to " + event.getMember().getUser().getId());
        	for(Role role : event.getMember().getRoles())
        		if(role.getId().equals("548534015108448256")) {
                    event.getGuild().getController().removeSingleRoleFromMember(event.getMember(), event.getGuild().getRoleById("548534015108448256")).queue();
                    return;
        		}
            event.getGuild().getController().addSingleRoleToMember(event.getMember(), event.getGuild().getRoleById("548534015108448256")).queue();
            //event.getReaction().removeReaction().queue();
            return;
        }
        */
        /*
        for(RoleAutoAssignDesc v : autoAssignList) {
        	if( v.messageId.equals(event.getMessageId()) && v.reactionName.equals(event.getReactionEmote().getName()) )
        		event.getGuild().getController().addSingleRoleToMember( event.getMember(), event.getGuild().getRoleById(v.roleId) ).queue();
        }
        */
    }
    
    private void WriteLogMessage(Guild guild, String message) {
    	String channelId = null;
    	if((channelId = CommandManager.logChannels.get(guild.getIdLong())) != null) {
    		for(TextChannel c : guild.getTextChannels()) {
    			if(c.getId().equals(channelId)) {
    	        	((TextChannel)c).sendMessage(JDAManager.wrapMessageInEmbed(message, Color.gray)).queue();
    				break;
    			}
    		}
    	}
	}

	@Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
    	for(ReactionListener rl : CommandManager.reactionListeners) {
    		if(event.getMessageId().equals(rl.messageId) && (event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getId().equals(rl.emoteId) : event.getReactionEmote().getName().equals(rl.emoteId))) {
    			Role role = event.getGuild().getRoleById(rl.roleId);
    			if(role != null) {
	    			event.getGuild().removeRoleFromMember(event.getUserId(), role).queue();
	    			WriteLogMessage(event.getGuild(), "Removed role `" + role.getName() + "` from <@" + event.getUserId() + ">");
    			}
                return;
    		}
    	}
    }
	
	@Override
	public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
		/*
		event.getChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
			WriteLogMessage(event.getGuild(), "**Message sent by <@" + message.getAuthor().getId() + "> deleted in <#" + message.getChannel().getId() + ">**\n" + message);
		});
		*/
	}
	
	@Override
	public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
		//WriteLogMessage(event.getGuild(), "**Message sent by <@" + event.getAuthor().getId() + "> edited in <#" + event.getChannel().getId() + ">**\n" + event.getMessage());
	}
    
    /*
    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        super.onGuildMessageReactionRemove(event);
        if (event.getMessageId().equals("446648726648520704") && event.getReactionEmote().getName().equals("\u2705")) {
            event.getGuild().getController().removeSingleRoleFromMember(event.getMember(), event.getGuild().getRoleById("446708059247607809")).queue();
            return;
        }
        if (event.getMessageId().equals("538451377353916426") && event.getReactionEmote().getName().equals("\u2705")) {
            event.getGuild().getController().removeSingleRoleFromMember(event.getMember(), event.getGuild().getRoleById("538202432581271563")).queue();
            return;
        }
        if (event.getMessageId().equals("548538000552886273") && event.getReactionEmote().getName().equals("\u2705")) {
            //System.out.println("Adding Member role to " + event.getMember().getUser().getId());
            event.getGuild().getController().removeSingleRoleFromMember(event.getMember(), event.getGuild().getRoleById("548534015108448256")).queue();
            return;
        }
    }
    */
	
	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		
		if(event.getGuild().getIdLong() == 439093693769711616L || event.getGuild().getIdLong() == 663449315876012052L || event.getGuild().getIdLong() == 600298024425619456L) {
			String name = event.getUser().getName();
			for (String s : CommandManager.blacklistedNames) {
				if (name.matches(".*\\Q" + s + "\\E.*")) {
					event.getGuild().ban(event.getMember(), 0, "Blacklisted username").queue();
					event.getGuild().getDefaultChannel().sendMessage("Automatically banned blacklisted username " + event.getMember().getNickname());
					return;
				}
			}
		}
		
		if(event.getUser().isBot())
			return;
		
		if(event.getGuild().getId().equals("446646432339066912")) {
			event.getUser().openPrivateChannel().queue((channel) -> {
				channel.sendMessage("Welcome to the MetrixVR discord !\nPlease make sure to read the <#446646432339066916> channel and react with :white_check_mark: to see every channels.\nIf you need some help, you can ask in the #help-requests channel.\n\nTo play the game, you will need a game account. You can create one at <https://metrixvr.net/register>.\nYou can ask for a key in the #🔑key-requests🔑 channel. Every keys will be sent at the same time as the next update.\nPlease read the latest #announcements for more infos!\n\nHave a good day\n - Slaynash").queue();
			});
		}
		if(event.getGuild().getId().equals("439093693769711616")) {
			event.getUser().openPrivateChannel().queue((channel) -> {
				channel.sendMessage("Welcome to the VRChat Modding Group discord !\nPlease make sure to read the <#721966647228891227> channel and react with :white_check_mark: to see every channels.\nIf you need some help, you can ask in the #help-requests channel.\n\nHave a good day\n - Slaynash").queue();
			});
		}
		if(event.getGuild().getId().equals("398382180608507904")) {
			event.getUser().openPrivateChannel().queue((channel) -> {
				channel.sendMessage("Bienvenue sur le serveur **VRChat Communauté Francophone** ! Nous te prions tout d'abord de lire le règlement dans le salon <#399022887631323148> afin de connaître le bon fonctionnement du serveur et pour ne pas t'y perdre. (N'oublie pas de choisir un rôle :wink:)\nSur ce, amuse toi bien et bon jeu !").queue();
			});
		}
		/*
		if(event.getGuild().getId().equals("365589812616364036")) {
			event.getGuild().retrieveInvites().queue((list) -> {
				for(Invite inv : list) {
					//System.out.println("Invite code: " + inv.getCode() + ", inv.getUses(): " + inv.getUses() + ", lastAMLUsesCount: " + lastAMLUsesCount);
					if(inv.getCode().equals("V35fm3u") && inv.getUses() > lastAMLUsesCount) {
						lastAMLUsesCount = inv.getUses();
						Role role = event.getGuild().getRoleById(646656920945557525L);
						event.getGuild().addRoleToMember(event.getMember(), role).queue();
						break;
					}
				}
			});
		}
		*/
	}
	
	@Override
	public void onGuildAvailable(GuildAvailableEvent event) {
		System.out.println("onGuildAvailable " + event.getGuild().getId());
		/*
		if(event.getGuild().getId().equals("365589812616364036")) {
			event.getGuild().retrieveInvites().queue((list) -> {
				for(Invite inv : list) {
					if(inv.getCode().equals("V35fm3u")) {
						lastAMLUsesCount = inv.getUses();
						break;
					}
				}
			});
		}
		*/
	}

}

