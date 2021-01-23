/*
 * Decompiled with CFR 0_132.
 */
package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import com.coder4.emoji.EmojiUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.restaction.RoleAction;

public class CommandManager {
    private static List<Command> commands = new ArrayList<Command>();
    private static boolean init = false;
    
	public static List<ReactionListener> reactionListeners = new ArrayList<>();
	public static Map<Long, String> logChannels = new HashMap<>();
	public static Map<Long, VerifyPair> verifyChannels = new HashMap<>();
	public static List<String> blacklistedNames = new ArrayList<String>();
	
	public static List<String> melonLoaderHashes = new ArrayList<>();
	public static Map<Long, String> mlReportChannels = new HashMap<>();

    protected static void registerCommand(Command command) {
        HelpCommand.registerCommand(command);
        List<Command> list = commands;
        synchronized (list) {
            commands.add(command);
        }
    }

    protected static void runAsClient(MessageReceivedEvent event) {
        String command = event.getMessage().getContentRaw();
        List<Command> list = commands;
        synchronized (list) {
            for (Command rcmd : commands) {
                if (!rcmd.matchPattern(command)) continue;
                rcmd.onClient(command, event);
            }
        }
    }

    protected static void runAsServer(MessageReceivedEvent event) {
        String command = event.getMessage().getContentRaw();
        List<Command> list = commands;
        synchronized (list) {
            for (Command rcmd : commands) {
                if (!rcmd.matchPattern(command)) continue;
                rcmd.onServer(command, event);
            }
        }
    }

    protected static void init() {
        if (init)
            return;
        
        init = true;
        CommandManager.registerCommand(new HelpCommand());
        CommandManager.registerStatus();
        //CommandManager.registerRun();
        CommandManager.registerRankColor();
        CommandManager.registerGetRoleId();
        CommandManager.registerAddReactionHandler();
        CommandManager.registerBlacklistName();
        CommandManager.registerSetLogChannelHandler();
        CommandManager.registerVerifyChannelHandler();
        CommandManager.registerCommandLaunch();
        CommandManager.registerVerifyCommand();
        CommandManager.registerRubybotOverDynobot();

        CommandManager.registerMLHashRegister();
        CommandManager.registerSetMLReportChannel();
        
        //CommandManager.registerModNotWorkingRegex();
    }




    private static void registerSetLogChannelHandler() {
    	CommandManager.registerCommand(new Command() {
			
			@Override
			protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
				if(!paramMessageReceivedEvent.getMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
					paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the View Audit Logs permission").queue();
					return;
				}
				logChannels.put(paramMessageReceivedEvent.getGuild().getIdLong(), paramMessageReceivedEvent.getChannel().getId());
				paramMessageReceivedEvent.getChannel().sendMessage("Successfully set log channel").queue();
				saveLogChannels();
			}
			
			@Override
			protected void onLUM(String paramString) {
			}
			
			@Override
			protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
			}
			
			@Override
			protected boolean matchPattern(String paramString) {
				return paramString.startsWith("l!setlogchannel");
			}
			
			@Override
			protected String getHelpPath() {
				return "";
			}
			
			@Override
			protected String getHelpName() {
				return "l!setlogchannel";
			}
			
			@Override
			protected String getHelpDescription() {
				return "Set the log channel to the current one.";
			}
		});
	}
    
    private static void registerMLHashRegister() {
    	CommandManager.registerCommand(new Command() {
			
			@Override
			protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
				String hash = paramString.split(" ", 2)[1];
				System.out.println("hash: " + paramString);
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
				
				melonLoaderHashes.add(hash);
				saveMLHashes();
				paramMessageReceivedEvent.getChannel().sendMessage("Added hash " + hash).queue();
			}
			
			@Override
			protected void onLUM(String paramString) {
			}
			
			@Override
			protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
			}
			
			@Override
			protected boolean matchPattern(String paramString) {
				return paramString.startsWith("l!registermlhash");
			}
			
			@Override
			protected String getHelpPath() {
				return null;
			}
			
			@Override
			protected String getHelpName() {
				return null;
			}
			
			@Override
			protected String getHelpDescription() {
				return null;
			}
		});
	}
    
    private static void registerSetMLReportChannel() {
    	CommandManager.registerCommand(new Command() {
			
			@Override
			protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
				if(!paramMessageReceivedEvent.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the Administrator permission").queue();
					return;
				}
				
				if (paramMessageReceivedEvent.getChannel().getId().equals( mlReportChannels.get(paramMessageReceivedEvent.getGuild().getIdLong()) )) {
					mlReportChannels.remove(paramMessageReceivedEvent.getGuild().getIdLong());
					saveMLReportChannels();
					paramMessageReceivedEvent.getChannel().sendMessage("Successfully unset ML report channel").queue();
				}
				else {
					mlReportChannels.put(paramMessageReceivedEvent.getGuild().getIdLong(), paramMessageReceivedEvent.getChannel().getId());
					saveMLReportChannels();
					paramMessageReceivedEvent.getChannel().sendMessage("Successfully set ML report channel").queue();
				}
			}
			
			@Override
			protected void onLUM(String paramString) {
			}
			
			@Override
			protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
			}
			
			@Override
			protected boolean matchPattern(String paramString) {
				return paramString.startsWith("l!setmlreportchannel");
			}
			
			@Override
			protected String getHelpPath() {
				return null;
			}
			
			@Override
			protected String getHelpName() {
				return null;
			}
			
			@Override
			protected String getHelpDescription() {
				return null;
			}
		});
	}
    
    private static void registerRubybotOverDynobot() {
    	CommandManager.registerCommand(new Command() {
			
			@Override
			protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
				paramMessageReceivedEvent.getChannel().sendMessage("<:SmugSip:743484784415866950>").queue();
			}
			
			@Override
			protected void onLUM(String paramString) {
			}
			
			@Override
			protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
			}
			
			@Override
			protected boolean matchPattern(String paramString) {
				return paramString.trim().toLowerCase().replace(" ", "").equals("rubybot>dynobot");
			}
			
			@Override
			protected String getHelpPath() {
				return "";
			}
			
			@Override
			protected String getHelpName() {
				return "";
			}
			
			@Override
			protected String getHelpDescription() {
				return "";
			}
		});
	}
    
    private static void registerVerifyChannelHandler() {
    	CommandManager.registerCommand(new Command() {
			
			@Override
			protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
				if(!paramMessageReceivedEvent.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the Administrator permission").queue();
					return;
				}
				
				if(verifyChannels.containsKey(paramMessageReceivedEvent.getGuild().getIdLong())) {
					verifyChannels.remove(paramMessageReceivedEvent.getGuild().getIdLong());
					paramMessageReceivedEvent.getChannel().sendMessage("Successfully removed verify handler").queue();
				}
				else {
					String[] parts = paramString.split(" ", 3);
					if(parts.length != 2)
					{
						paramMessageReceivedEvent.getChannel().sendMessage("Usage: l!setverifychannel `roleid`").queue();
					}
					try {
						Long.parseLong(parts[1]);
					}
					catch(Exception e)
					{
						paramMessageReceivedEvent.getChannel().sendMessage("Error: Invalid role id. usage: l!setverifychannel `roleid`").queue();
						return;
					}
					
					verifyChannels.put(paramMessageReceivedEvent.getGuild().getIdLong(), new VerifyPair(paramMessageReceivedEvent.getChannel().getId(), parts[1]));
					paramMessageReceivedEvent.getChannel().sendMessage("Successfully set verify handler").queue();
				}
				saveVerifyChannels();
			}
			
			@Override
			protected void onLUM(String paramString) {
			}
			
			@Override
			protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
			}
			
			@Override
			protected boolean matchPattern(String paramString) {
				return paramString.startsWith("l!setverifychannel");
			}
			
			@Override
			protected String getHelpPath() {
				return "";
			}
			
			@Override
			protected String getHelpName() {
				return "l!setverifychannel";
			}
			
			@Override
			protected String getHelpDescription() {
				return "Set the verify channel to the current one.";
			}
		});
	}
    
    private static void registerVerifyCommand() {
    	CommandManager.registerCommand(new Command() {
			
			@Override
			protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
				VerifyPair pair = verifyChannels.get(paramMessageReceivedEvent.getGuild().getIdLong());
				if(pair != null)
				{
					//System.out.println("Server has registered verify command with channel " + pair.channelId + " and role " + pair.roleId);
					if(paramMessageReceivedEvent.getChannel().getId().equals(pair.channelId))
					{
						paramMessageReceivedEvent.getGuild().addRoleToMember(paramMessageReceivedEvent.getAuthor().getIdLong(), paramMessageReceivedEvent.getGuild().getRoleById(pair.roleId)).queue();
						paramMessageReceivedEvent.getChannel().sendMessage("You are now verified!").queue();
					}
				}
			}
			
			@Override
			protected void onLUM(String paramString) {
			}
			
			@Override
			protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
			}
			
			@Override
			protected boolean matchPattern(String paramString) {
				return paramString.equals("!verify");
			}
			
			@Override
			protected String getHelpPath() {
				return "";
			}
			
			@Override
			protected String getHelpName() {
				return "!verify";
			}
			
			@Override
			protected String getHelpDescription() {
				return "";
			}
		});
	}
    /*
    private static void registerModNotWorkingRegex() {
    	CommandManager.registerCommand(new Command() {
			
			@Override
			protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
				if(paramMessageReceivedEvent.getGuild().getIdLong() == 439093693769711616L || paramMessageReceivedEvent.getGuild().getIdLong() == 663449315876012052L && paramMessageReceivedEvent.getChannel().getIdLong() == 695075243961745487L)
					paramMessageReceivedEvent.getChannel().sendMessage("Hey\nThe VRChat mods are currently broken since VRChat v2020.1.1 (Udon & Unity 2018 update).\nWhen using the VRChat Mod Manager, you get an error: this is due to the mod loader not being compatible anymore.\nWe are currently working on an universal mod loader for every Unity games, called MelonLoader.\nMelonLoader isn't available yet, but you will (only) get your mods back when it will be!").queue();
			}
			
			@Override
			protected void onLUM(String paramString) {
			}
			
			@Override
			protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
			}
			
			@Override
			protected boolean matchPattern(String paramString) {
				return !paramString.toLowerCase().contains("bone") && paramString.toLowerCase().matches(".*mod.*work.*");
			}
			
			@Override
			protected String getHelpPath() {
				return "";
			}
			
			@Override
			protected String getHelpName() {
				return "l!setlogchannel";
			}
			
			@Override
			protected String getHelpDescription() {
				return "Set the log channel to the current one.";
			}
		});
	}
    */
    

	private static void saveReactions() {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("rolereactions.txt")))
		{
			for(ReactionListener rl : reactionListeners) {
				writer.write(rl.messageId + " " + rl.emoteId + " " + rl.roleId + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void saveLogChannels() {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("logchannels.txt")))
		{
			for(Entry<Long, String> logchannel : logChannels.entrySet()) {
				writer.write(logchannel.getKey() + " " + logchannel.getValue() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void saveMLReportChannels() {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("mlreportchannels.txt")))
		{
			for(Entry<Long, String> logchannel : mlReportChannels.entrySet()) {
				writer.write(logchannel.getKey() + " " + logchannel.getValue() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void saveMLHashes() {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("mlhashes.txt")))
		{
			for(String s : melonLoaderHashes) {
				writer.write(s + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void saveVerifyChannels() {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("verifychannels.txt")))
		{
			for(Entry<Long, VerifyPair> verifychannel : verifyChannels.entrySet()) {
				writer.write(verifychannel.getKey() + " " + verifychannel.getValue().channelId + " " + verifychannel.getValue().roleId + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void saveNameBlacklist() {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("nameblacklist.txt")))
		{
			for(String s : blacklistedNames) {
				writer.write(s + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    private static void registerAddReactionHandler() {
		CommandManager.registerCommand(new Command() {
			
			@Override
			protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
				if(!paramMessageReceivedEvent.getMember().hasPermission(Permission.MANAGE_ROLES) && !paramMessageReceivedEvent.getMember().getId().equals("145556654241349632")) {
					paramMessageReceivedEvent.getChannel().sendMessage("Error: You need to have the Manage Role permission").queue();
					return;
				}
				String[] params = paramMessageReceivedEvent.getMessage().getContentRaw().split(" ");
				if(params.length != 4 || !params[1].matches("^[0-9]+$") || (!params[2].matches("^<:.*:[0-9]+>$") && !EmojiUtils.isOneEmoji(params[2]))) {
					paramMessageReceivedEvent.getChannel().sendMessage("Usage: l!reaction <messageid> <reaction> [roleid]").queue();
					return;
				}
				
				new MessageFinder().findMessageAsync(paramMessageReceivedEvent.getGuild(), params[1], success -> {
					if(success == null) {
						paramMessageReceivedEvent.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Error: Message not found", Color.RED)).queue();
						return;
					}
					
					ReactionListener react = null;
					for(ReactionListener rl : reactionListeners) {
						if(rl.messageId.equals(params[1]) && rl.emoteId.equals(params[2])) {
							react = rl;
							break;
						}
					}
					if(react != null) {
						reactionListeners.remove(react);
						/*
						if(params[2].matches("^<:.*:[0-9]+>$")) {
							String emoteId = params[2].split(":")[2].split(">", 2)[0];
							//success.getReactions().removeIf(mr -> mr.getReactionEmote().getId().equals(emoteId) && mr.get);
							//success.rea(paramMessageReceivedEvent.getGuild().getEmoteById(emoteId));
						}
						else
							success.addReaction(params[2]);
						*/
						
						paramMessageReceivedEvent.getChannel().sendMessage("Successfully removed reaction listener from the target message").queue();
						saveReactions();
					}
					else {
						if(!params[3].matches("^[0-9]+$")) {
							paramMessageReceivedEvent.getChannel().sendMessage("Usage: l!reaction <messageid> <reaction> [roleid]").queue();
							return;
						}
						
						Role role = paramMessageReceivedEvent.getGuild().getRoleById(params[3]);
						if(role == null) {
							paramMessageReceivedEvent.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Error: Role not found", Color.RED)).queue();
							return;
						}
						react = new ReactionListener(success.getId(), params[2].matches("^<:.*:[0-9]+>$") ? params[2].split(":")[2].split(">", 2)[0] : params[2], params[3]);
						
						
						if(params[2].matches("^<:.*:[0-9]+>$")) {
							String emoteId = params[2].split(":")[2].split(">", 2)[0];
							Emote emote = paramMessageReceivedEvent.getGuild().getEmoteById(emoteId);
							if(emote == null) {
								paramMessageReceivedEvent.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Error: Emote not found on current server", Color.RED)).queue();
								return;
							}
							success.addReaction(paramMessageReceivedEvent.getGuild().getEmoteById(emoteId)).queue();
						}
						else
							success.addReaction(params[2]).queue();
						
						
						reactionListeners.add(react);
						paramMessageReceivedEvent.getChannel().sendMessage("Successfully added reaction listener to the target message").queue();
					}
					saveReactions();
				}, error -> paramMessageReceivedEvent.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Error while looking for message: " + error.getMessage(), Color.RED)).queue());
			}
			
			@Override
			protected void onLUM(String paramString) {
			}
			
			@Override
			protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
			}
			
			@Override
			protected boolean matchPattern(String pattern) {
				return pattern.startsWith("l!reaction ");
			}
			
			@Override
			protected String getHelpPath() {
				return "";
			}
			
			@Override
			protected String getHelpName() {
				return "l!reaction";
			}
			
			@Override
			protected String getHelpDescription() {
				return "Toggle role assignation on react";
			}
		});
	}
    
    private static void registerBlacklistName() {
		CommandManager.registerCommand(new Command() {
			
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
				
				if (blacklistedNames.contains(nameTrimed)) {
					blacklistedNames.remove(nameTrimed);
					paramMessageReceivedEvent.getChannel().sendMessage("Removed `" + nameTrimed + "` from the blacklist").queue();
				}
				else {
					blacklistedNames.add(nameTrimed);
					paramMessageReceivedEvent.getChannel().sendMessage("Added `" + nameTrimed + "` to the blacklist").queue();
				}
				saveNameBlacklist();
			}
			
			@Override
			protected void onLUM(String paramString) {
			}
			
			@Override
			protected void onClient(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
			}
			
			@Override
			protected boolean matchPattern(String pattern) {
				return pattern.startsWith("l!blacklist ");
			}
			
			@Override
			protected String getHelpPath() {
				return "";
			}
			
			@Override
			protected String getHelpName() {
				return "l!blacklist";
			}
			
			@Override
			protected String getHelpDescription() {
				return "Blacklist an username";
			}
		});
	}

	private static void registerCommandLaunch() {
        CommandManager.registerCommand(new Command(){

            @Override
            protected void onServer(String command, MessageReceivedEvent event) {
                System.out.println("loading lua file commands/" + event.getMessage().getContentRaw().split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z0-9.-]", "_"));
                Globals m_globals = LuaPackages.createCommandGlobals(event);
                try {
                    File rom = new File("commands/" + command.substring(2).split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z0-9.-]", "_"));
                    m_globals.get("dofile").call(LuaValue.valueOf(rom.toString()));
                }
                catch (LuaError e) {
                    event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Une erreur est survenue dans le programme \"" + command.substring(2).split(" ")[0] + "\"\n" + e.getMessage(), Color.RED)).queue();
                    e.printStackTrace();
                }
            }

            @Override
            protected void onLUM(String command) {
            }

            @Override
            protected void onClient(String command, MessageReceivedEvent event) {
            }

            @Override
            protected boolean matchPattern(String pattern) {
                if (pattern.startsWith("l!") && new File("commands/" + pattern.substring(2).split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z0-9.-]", "_")).exists() && !pattern.substring(2).split(" ")[0].replaceAll("\n", "").replaceAll("[^a-zA-Z0-9.-]", "_").equals("")) {
                    return true;
                }
                return false;
            }

            @Override
            protected String getHelpPath() {
                return null;
            }

            @Override
            protected String getHelpDescription() {
                return null;
            }

            @Override
            protected String getHelpName() {
                return null;
            }
        });
    }
	
	/*
    private static void registerRun() {
        CommandManager.registerCommand(new Command(){

            @Override
            protected void onServer(String command, MessageReceivedEvent event) {
            	
            	if(!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            		event.getChannel().sendMessage("Error: You need to have the Moderator permission").queue();
					return;
				}
            	
                String[] parts = event.getMessage().getContentRaw().split("```");
                if (parts.length < 2) {
                    event.getChannel().sendMessage("Invalid syntax!\nExemple of correct syntax:\nl!run\n```lua\nsendln(\"Hello World!\")\n```").queue();
                } else if (!parts[1].toLowerCase().startsWith("lua")) {
                    event.getChannel().sendMessage("The code must be tagged as lua to be run! ").queue();
                } else {
                    String code = parts[1].substring(3).trim();
                    Globals m_globals = LuaPackages.createRunGlobals(event);
                    try {
                        LuaValue lv = m_globals.load(code);
                        lv.call();
                    }
                    catch (Exception e) {
                        event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Impossible de lancer le code !\n" + e.getMessage(), Color.RED)).queue();
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected void onLUM(String command) {
            }

            @Override
            protected void onClient(String command, MessageReceivedEvent event) {
            }

            @Override
            protected boolean matchPattern(String pattern) {
                return pattern.startsWith("l!run");
            }

            @Override
            protected String getHelpPath() {
                return "";
            }

            @Override
            protected String getHelpDescription() {
                return "Run a lua command";
            }

            @Override
            protected String getHelpName() {
                return "l!run";
            }
        });
    }
    */
    
    private static void registerGetRoleId() {
        CommandManager.registerCommand(new Command(){

            @Override
            protected void onServer(String command, MessageReceivedEvent event) {
                String[] parts = event.getMessage().getContentRaw().split(" ", 2);
                List<Role> roles;
                if (parts.length < 2) {
                    event.getChannel().sendMessage("Usage: l!roleid <role name>").queue();
                }
                else if((roles = event.getGuild().getRolesByName(parts[1], true)).size() == 0) {
                    event.getChannel().sendMessage("Role not found").queue();
                }
                else {
                	String out = "Ids for roles matching `" + parts[1] + "`:";
                	for(Role r : roles)
                		out += " " + r.getId();
                	event.getChannel().sendMessage(out).queue();
                }
                
            }

            @Override
            protected void onLUM(String command) {
            }

            @Override
            protected void onClient(String command, MessageReceivedEvent event) {
            }

            @Override
            protected boolean matchPattern(String pattern) {
                return pattern.startsWith("l!roleid");
            }

            @Override
            protected String getHelpPath() {
                return "";
            }

            @Override
            protected String getHelpDescription() {
                return "Get role id";
            }

            @Override
            protected String getHelpName() {
                return "l!roleid <role name>";
            }
        });
    }

    private static void registerRankColor() {
        CommandManager.registerCommand(new Command(){
            String arg = "";

            @Override
            protected void onServer(String command, MessageReceivedEvent event) {
            	try {
					if(command.split(" ").length == 1 || (arg = command.split(" ", 2)[1]).equals("help") || !arg.startsWith("#")){
						event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Usage: "+getHelpName()+" <hexcolor>\nExemple (pure green): "+getHelpName()+" #00ff00", Color.BLUE)).queue();
					}else if(arg.length() != 7){
						event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Bad hex color !\nUsage: "+getHelpName()+" <hexcolor>\nExemple (pure green): "+getHelpName()+" #00ff00", Color.RED)).queue();
					}
					else {
						for(char c:arg.substring(1).toCharArray()) {
							if(!(('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F'))) {
								event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Bad hex color !\nUsage: "+getHelpName()+" <hexcolor>\nExemple (pure green): "+getHelpName()+" #00ff00", Color.RED)).queue();
								return;
							}
						}
						for(Role r:event.getMember().getRoles()) {
							Color color = r.getColor();
							if(r.getColor() != null && r.getName() != null && r.getName().startsWith("#") && r.getName().length() == 7 && r.getName().toLowerCase().equals(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()))) {
								event.getGuild().removeRoleFromMember(event.getMember(), r).complete();
							}
						}
						if(event.getGuild().getRolesByName(arg, true).size() == 0) {
							List<Role> cr = event.getGuild().getRolesByName("_COLOR_DEFAULT_", true);
							if(cr.size() == 0) {
								event.getChannel().sendMessage("Please add a default role named `_COLOR_DEFAULT_` to enable color roles").queue();
							}
							else {
								System.out.println("_COLOR_DEFAULT_ position: "+cr.get(0).getPosition()+" | "+cr.get(0).getPositionRaw());
								try {
									RoleAction r = event.getGuild().createCopyOfRole(cr.get(0)).setName(arg).setColor(hex2Rgb(arg));
									r.queue(
											role -> {
												event.getGuild().modifyRolePositions().selectPosition(0).moveTo(cr.get(0).getPosition()-1).queue(// insecure
														success -> {
															event.getGuild().addRoleToMember(event.getMember(), role).queue(
																	success2 -> {},
																	error -> {
																		event.getChannel().sendMessage("I don't have enough permission to this role to you").queue();
																	}
															);
														},
														failure -> {
															event.getChannel().sendMessage("Unable to move the role `"+arg+"` to the position of `_COLOR_DEFAULT_`").queue();
														}
												);
											},
											role -> {
												event.getChannel().sendMessage("Unable to create the role :(").queue();
											}
									);
								}
								catch(InsufficientPermissionException e) {
									event.getChannel().sendMessage("I don't have the permission to create a role ! :(").queue();
								}
							}
						}
						else {
							event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRolesByName(arg, true).get(0)).queue();
						}
					}
				}
				catch(Exception e) {
					event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("An error has occured:\n"+e.toString()+"\n at "+e.getStackTrace()[0], Color.RED)).queue();
				}
            }

            @Override
            protected void onLUM(String command) {
            }

            @Override
            protected void onClient(String command, MessageReceivedEvent event) {
                event.getChannel().sendMessage("This is not a server, there is no colors here :/").queue();
            }

            @Override
            protected boolean matchPattern(String pattern) {
                return pattern.startsWith("l!rankcolor");
            }

            @Override
            protected String getHelpPath() {
                return "";
            }

            @Override
            protected String getHelpDescription() {
                return "Set rank color. exemple (pure green): " + this.getHelpName() + " #00ff00";
            }

            @Override
            protected String getHelpName() {
                return "l!rankcolor";
            }
        });
    }

    private static void registerStatus() {
        CommandManager.registerCommand(new Command(){

            @Override
            protected void onServer(String command, MessageReceivedEvent event) {
                if (command.split(" ").length == 1 || command.split(" ")[1].equals("help")) {
                    event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Deconnecte du systeme LUM", Color.BLUE)).queue();
                }
            }

            @Override
            protected void onLUM(String command) {
            }

            @Override
            protected void onClient(String command, MessageReceivedEvent event) {
                if (command.split(" ").length == 1 || command.split(" ")[1].equals("help")) {
                    event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("Deconnecte du systeme LUM", Color.BLUE)).queue();
                }
            }

            @Override
            protected boolean matchPattern(String pattern) {
                return pattern.startsWith("l!status");
            }

            @Override
            protected String getHelpPath() {
                return "";
            }

            @Override
            protected String getHelpDescription() {
                return "Print Survival-Machines servers status";
            }

            @Override
            protected String getHelpName() {
                return "l!status";
            }
        });
    }

    public static List<Command> getCommands() {
        return commands;
    }

    public static Color hex2Rgb(String colorStr) {
        return new Color(Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
    }
    
    

    
    
    private static class MessageFinder {
    	private boolean waiting = false;
    	private boolean found = false;
		private Message message = null;
		private Throwable error = null;
		
		private void findMessageAsync(Guild guild, String messageId, Consumer<Message> success, Consumer<Throwable> failure) {
        	new Thread(() -> {
    			
        		findMessage(guild, messageId);
        		if(error != null)
        			failure.accept(error);
        		else
        			success.accept(message);
    			
    		}, "MessageFindThread " + guild.getId()).start();
		}
    	
    	private void findMessage(Guild guild, String messageId) {
        	
        	for(TextChannel tc : guild.getTextChannels()) {
				System.out.println("tc: " + tc);
				waiting = true;
				try {
					tc.retrieveMessageById(messageId).queue(success -> {
						message = success;
						found = true;
						waiting = false;
					}, failure -> {
						//System.err.println("F: " + failure.getMessage());
						if(!failure.getMessage().startsWith("10008")) {
							error = failure;
							found = true;
						}
						waiting = false;
					});
				}
				catch (Exception e)
				{
					waiting = false;
					System.out.println("Failed to read channel " + tc + ": " + e.getMessage());
				}
				
				while(waiting) {
					try{Thread.sleep(1);}catch(Exception e){}
				}
				if(found)
					break;
			}
        }
    }

}

