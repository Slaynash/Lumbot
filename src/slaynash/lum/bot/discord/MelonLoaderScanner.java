package slaynash.lum.bot.discord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.vrcmg.ModDetails;
import slaynash.lum.bot.discord.vrcmg.ModVersionDetails;

public class MelonLoaderScanner {
	
	public static String latestMLVersionRelease = "0.2.7.2";
	public static String latestMLVersionBeta = "0.3.0";
	
	private static List<MelonLoaderError> knownErrors = new ArrayList<MelonLoaderError>() {{
		add(new MelonLoaderError(
				".*System\\.IO\\.FileNotFoundException\\: .* 'System\\.IO\\.Compression.*", 
				"Your are actually missing the required .NET Framework for MelonLoader.\nPlease make sure to install it using the following link: <https://dotnet.microsoft.com/download/dotnet-framework/net48>"));
		add(new MelonLoaderError(
				"System.UnauthorizedAccessException:.*",
				"The access to a file has been denied. Please make sure the game is closed when installing MelonLoader, or try restarting your computer. If this doesn't works, try running the MelonLoader Installer with administrator privileges"));
		
		add(new MelonLoaderError(
				"\\[[0-9.:]+\\]    at MelonLoader\\.AssemblyGenerator\\.LocalConfig\\.Save\\(String path\\)",
				"The access to a file has been denied. Please try starting the game with administrator privileges, or try restarting your computer (failed to save AssemblyGenerator/config.cfg)"));
		add(new MelonLoaderError(
				"\\[[0-9.:]+\\]    at MelonLoader\\.AssemblyGenerator\\.Main\\.SetupDirectory\\(String path\\)",
				"The access to a file has been denied. Please try starting the game with administrator privileges, or try restarting your computer (failed to setup directories)"));
		add(new MelonLoaderError(
				"\\[[0-9.:]+\\] Il2CppDumper.exe does not exist!",
				"MelonLoader assembly generation failed. Please delete the `MelonLoader` folder and `version.dll` file from your game folder, and install MelonLoader again (failed to download Il2CppDumper)"));
		
		add(new MelonLoaderError(
				"\\[[0-9.:]+\\] \\[emmVRCLoader\\] You have emmVRC's Stealth Mode enabled..*",
				"You have emmVRC's Stealth Mode enabled. To access the functions menu, press the \"Report World\" button. Most visual functions of emmVRC have been disabled."));
		
		/*
		add(new MelonLoaderError(
				".*Harmony\\.HarmonyInstance\\..*",
				"You seems to have a 0Harmony.dll file in your `Mods` or `Plugins` folder. This breaks mods and plugins, since Harmony is embed into MelonLoader"));
		*/
	}};
	
	private static Gson gson = new Gson();
	private static List<ModDetails> mods = new ArrayList<ModDetails>();
	
	private final static HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
	
	private static Map<String, String> modNameMatcher = new HashMap<String, String>() {{
		// MelonInfo name -> Submitted name
	    put("Advanced Safety", "AdvancedSafety");
	    put("MControl", "MControl (Music Playback Controls)");
	    put("Player Volume Control", "PlayerVolumeControl");
	    put("UI Expansion Kit", "UIExpansionKit");
	    put("NearClipPlaneAdj", "NearClippingPlaneAdjuster.dll");
	    put("Particle and DynBone limiter settings UI", "ParticleAndBoneLimiterSettings");
	    put("MuteBlinkBeGone", "Mute Blink Be Gone");
	    put("DiscordRichPresence-ML", "VRCDiscordRichPresence-ML");
	    put("Core Limiter", "CoreLimiter");
	    put("MultiplayerDynamicBones", "Multiplayer Dynamic Bones");
	    put("Game Priority Changer", "GamePriority");
	    put("Runtime Graphics Settings", "RuntimeGraphicsSettings");
	    put("Advanced Invites", "AdvancedInvites");
	    put("No Steam. At all.", "NoSteamAtAll");
	    put("Rank Volume Control", "RankVolumeControl");
	    put("VRC Video Library", "VRCVideoLibrary");
	    put("Input System", "InputSystem");
	    put("TogglePostProcessing", "Toggle Post Processing");
	    put("ToggleMicIcon", "Toggle Mic Icon");
	    put("ThumbParams", "VRCThumbParams");
	    
	    // backward compatibility
	    put("BTKSANameplateFix", "BTKSANameplateMod");
	}};
	
	public static void Init() {
		
		Thread t = new Thread(() -> {
			System.out.println("MelonLoaderScannerThread start");
			while (true) {
				HttpRequest request = HttpRequest.newBuilder()
	                .GET()
	                .uri(URI.create("http://client.ruby-core.com/api/mods.json"))
	                .setHeader("User-Agent", "LUM Bot")
	                .build();
				
				try {
					HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
					
					synchronized (mods) {
						mods = gson.fromJson(response.body(), new TypeToken<ArrayList<ModDetails>>() {}.getType());
						
						/*
						for (int i = 0; i < mods.size(); ++i) {
							ModDetails mod = mods.get(i);
							for (int j = 0; j < mod.versions.length; ++j) {
								ModVersionDetails modversion = mod.versions[j];
								if (modversion.name.equals("emmVRC") && modversion.modversion.equals("Loader 1.0.0"))
									modversion.modversion = "Loader 1.1.0";
							}
						}
						*/
						/*
						System.out.println("Mods: ");
						for (ModDetails mod : mods) {
							List<String> versions = new ArrayList<String>();
							for (ModVersionDetails version : mod.versions)
								versions.add(version.name + " " + version.modversion);
							System.out.println(" - [" + String.join(", ", versions) + "]");
						}
						*/
					}
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}, "MelonLoaderScannerThread");
		t.setDaemon(true);
		t.start();
	}

	public static void scanLogs(MessageReceivedEvent event) {
		List<Attachment> attachments = event.getMessage().getAttachments();
		
		List<MelonLoaderError> errors = new ArrayList<MelonLoaderError>();
		String mlVersion = null;
		boolean hasErrors = false;
		String game = null;

		boolean preListingMods = false;
		boolean listingMods = false;
		Map<String, String> loadedMods = new HashMap<String, String>();

		List<String> duplicatedMods = new ArrayList<String>();
		List<String> unverifiedMods = new ArrayList<String>();
		List<String> universalMods = new ArrayList<String>();
		List<String> incompatibleMods = new ArrayList<String>();
		List<MelonInvalidMod> invalidMods = new ArrayList<MelonInvalidMod>();
		Map<String, String> modAuthors = new HashMap<String, String>();
		
		List<String> modsThrowingErrors = new ArrayList<String>();
		
		String emmVRCVersion = null;
		String emmVRCVRChatBuild = null;
		
		for (int i = 0; i < attachments.size(); ++i) {
			Attachment attachment = attachments.get(i);
			
			if (attachment.getFileExtension().toLowerCase().equals("log") || attachment.getFileExtension().toLowerCase().equals("txt")) {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(attachment.retrieveInputStream().get()))) {
					
					System.out.println("Reading file " + attachment.getFileName());
					String lastModName = null;
					String line;
					while ((line = br.readLine()) != null) {
						
						// Mod listing
						
						if (preListingMods || listingMods) {
							if (line.matches("\\[[0-9.:]+\\] ------------------------------")) {}
							else if (preListingMods && (line.matches("\\[[0-9.:]+\\] No Plugins Loaded!") || line.matches("\\[[0-9.:]+\\] No Mods Loaded!"))) {
								preListingMods = false;
								listingMods = true;
								
								continue;
							}
							else if (line.matches("\\[[0-9.:]+\\] .* by .*")) {
								preListingMods = false;
								listingMods = true;
								
								//System.out.println("line: " + line);
								String[] modAndAuthor = line.split(" ", 2)[1].split("by", 2);
								String[] nameAndVersion = modAndAuthor[0].trim().split(" v");
								if (loadedMods.containsKey(nameAndVersion[0]) && !duplicatedMods.contains(nameAndVersion[0]))
									duplicatedMods.add(nameAndVersion[0].trim());
								loadedMods.put(nameAndVersion[0].trim(), nameAndVersion.length > 1 ? nameAndVersion[1].trim() : "");
								if (modAndAuthor.length > 1)
									modAuthors.put(nameAndVersion[0].trim(), modAndAuthor[1].split("\\(")[0].trim());
								lastModName = nameAndVersion[0].trim();
								
								continue;
							}
							else if (line.matches("\\[[0-9.:]+\\] Game Compatibility: .*")) {
								String compatibility = line.split(" ", 4)[3];
								if (compatibility.equals("Universal"))
									universalMods.add(lastModName);
								else if (compatibility.equals("Compatible")) {}
								else
									incompatibleMods.add(lastModName);
								
								continue;
							}
							else if (listingMods) {
								listingMods = false;
							}
							
						}
						
						if (line.matches("\\[[0-9.:]+\\] Using v0\\..*")) {
							mlVersion = line.split("v")[1].split(" ")[0].trim();
							preListingMods = true;
						}
						else if (line.matches("\\[[0-9.:]+\\] MelonLoader v0\\..*")) {
							mlVersion = line.split("v")[1].split(" ")[0].trim();
							preListingMods = true;
						}
						else if (mlVersion == null && line.matches("\\[[0-9.:]+\\] Name: .*")) {
							game = line.split(":", 4)[3].trim();
						}
						else if (mlVersion == null && line.matches("\\[[0-9.:]+\\] \\[emmVRCLoader\\] VRChat build is: .*")) {
							emmVRCVRChatBuild = line.split(":", 4)[3].trim();
						}
						else if (mlVersion == null && line.matches("\\[[0-9.:]+\\] \\[emmVRCLoader\\] You are running version .*")) {
							emmVRCVersion = line.split("version", 2)[1].trim();
						}
						else {
							for (MelonLoaderError knownError : knownErrors) {
								if (line.matches(knownError.regex)) {
									if (!errors.contains(knownError))
										errors.add(knownError);
									//System.out.println("ERROR LINE 1: " + line);
									hasErrors = true;
								}
								else if (line.matches("\\[[0-9.:]+\\] \\[.*\\] \\[Error\\].*")) {
									String mod = line.split("\\]", 3)[1].split("\\[")[1];
									if (!modsThrowingErrors.contains(mod))
										modsThrowingErrors.add(mod);
									hasErrors = true;
								}
								else if (line.matches("\\[[0-9.:]+\\].*\\[Error\\].*")) {
									hasErrors = true;
								}
							}
						}
					}
					
				} catch (InterruptedException | ExecutionException | IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		boolean isMLOutdated = mlVersion != null && !(mlVersion.equals(latestMLVersionRelease) || mlVersion.equals(latestMLVersionBeta));
		
		if (game != null && game.equals("VRChat")) {
			for (Entry<String, String> entry : loadedMods.entrySet()) {
				String modName = entry.getKey();
				String modVersion = entry.getValue();
				
				if (modVersion.startsWith("v"))
					modVersion = modVersion.substring(1);
				if (modVersion.split("\\.").length == 2)
					modVersion += ".0";
				
				/*
				if (modName.equals("emmVRCLoader")) {
					modName = "emmVRC";
					modVersion = "Loader " + modVersion;
					modAuthors.put("emmVRC", modAuthors.get("emmVRCLoader"));
				}
				*/
				
				if (modName.equals("MultiplayerDynamicBones") && modVersion.startsWith("release ")) {
					modVersion = modVersion.substring("release ".length());
				}
				
				String matchedModName = modNameMatcher.get(entry.getKey().trim());
				//System.out.println("matchedModName: " + matchedModName);
				if (matchedModName != null) {
					modAuthors.put(matchedModName, modAuthors.get(modName));
					//modName = matchedModName;
				}
				
				String latestModVersion = null;
				for (ModDetails modDetail : mods) {
					boolean isCurrentMod = false;
					for (ModVersionDetails modVersionDetail : modDetail.versions) {
						//System.out.println("\"" + modVersionDetail.name + "\" vs \"" + matchedModName + "\":" + modVersionDetail.name.equals(matchedModName));
						if (isCurrentMod || modVersionDetail.name.equals(modName) || (matchedModName != null && modVersionDetail.name.equals(matchedModName))) {
							latestModVersion = modVersionDetail.modversion;
							if (latestModVersion.startsWith("v"))
								latestModVersion = latestModVersion.substring(1);
							if (latestModVersion.split("\\.").length == 2)
								latestModVersion += ".0";
							isCurrentMod = true;
						}
					}
				}
				
				if (latestModVersion == null) {
					unverifiedMods.add(modName);
				}
				else if (!modVersion.equals(latestModVersion)) {
					invalidMods.add(new MelonInvalidMod(modName, modVersion, latestModVersion));
				}
				else if (modName.equals("emmVRC")) {
					if (emmVRCVersion == null) {
						
					}
					else {
						
					}
				}
			}
		}
		/*
		if (game.equals("Phasmophobia")) {
			event.getChannel().sendMessage("**MelonLoader log autocheck:** This game isn't supported by the autochecker").queue();
			
			if (event.getGuild().getIdLong() == 663449315876012052L) { // MelonLoader
				event.getGuild().addRoleToMember(
					event.getAuthor().getIdLong(),
					event.getGuild().getRoleById(665739123323043851L)); // @Voided
			
				event.getGuild().getTextChannelById(712504192974979133L) // #bot-moderation-logs
					.sendMessage("Voided user <@" + event.getAuthor().getId() + ">: Phasmophobia logs detected by autochecker").queue();
			}
				
		}
		else
		*/
		if (errors.size() > 0 || isMLOutdated || duplicatedMods.size() != 0 || unverifiedMods.size() != 0 || invalidMods.size() != 0 || incompatibleMods.size() != 0 || (mlVersion != null && loadedMods.size() == 0)) {
			String message = "**MelonLoader log autocheck:** The autocheck reported the following problems <@" + event.getAuthor().getId() + ">:";
			
			if (isMLOutdated)
				message += "\n - The installed MelonLoader is outdated. Installed: **v" + sanitizeInputString(mlVersion) + "**. Latest: **v" + latestMLVersionRelease + "**";
			
			if ((mlVersion != null && loadedMods.size() == 0))
				message += "\n - You have no mods installed in your Mods and Plugins folder";
			
			if (duplicatedMods.size() > 0) {
				String error = "\n - The following mods are installed multiple times in your Mods and/or Plugins folder:";
				for (String s : duplicatedMods)
					error += "\n   \\> " + sanitizeInputString(s);
				message += error;
			}
			
			if (incompatibleMods.size() > 0) {
				String error = "\n - You are using the following incompatible mods:";
				for (String s : incompatibleMods)
					error += "\n   \\> " + sanitizeInputString(s);
				message += error;
			}
			
			if (unverifiedMods.size() > 0) {
				String error = "\n - You are using the following unverified/unknown mods:";
				for (String s : unverifiedMods)
					error += "\n   \\> " + sanitizeInputString(s) + (modAuthors.containsKey(s) ? (" **by** " + sanitizeInputString(modAuthors.get(s))) : "");
				message += error;
			}
			
			if (invalidMods.size() > 0) {
				String error = "\n - You are using the following outdated mods:";
				for (MelonInvalidMod m : invalidMods)
					error += "\n   \\> " + m.name + " - installed: `" + sanitizeInputString(m.currentVersion) + "`, latest: `" + m.latestVersion + "`";
				message += error;
			}
			
			
			for (int i = 0; i < errors.size(); ++i)
				message += "\n - " + sanitizeInputString(errors.get(i).error);
			
			
			if (modsThrowingErrors.size() > 0) {
				String error = "\n - The following mods are throwing errors:";
				for (String s : modsThrowingErrors)
					error += "\n   \\> " + sanitizeInputString(s);
				message += error;
			}
			
			event.getChannel().sendMessage(message).queue();
		}
		else if (mlVersion != null) {
			if (hasErrors) {
				event.getChannel().sendMessage("**MelonLoader log autocheck:** The autocheck found some unknown problems in your logs. Please wait for a moderator or an helper to manually check the file").queue();
			}
			else
				event.getChannel().sendMessage("**MelonLoader log autocheck:** The autocheck completed without finding any problem. Please wait for a moderator or an helper to manually check the file").queue();
		}
	}
	
	private static String sanitizeInputString(String input) {
		return input
				.replace("@", "@ ")
				.replace("*", "\\*")
				.replace("`", "\\`")
				.replace("nigger", "[CENSORED]")
				.replace("nigga", "[CENSORED]");
	}
	
	private static class MelonLoaderError {
		String regex;
		String error;
		
		public MelonLoaderError(String regex, String error) {
			this.regex = regex;
			this.error = error;
		}
	}
	
	public static class MelonInvalidMod {
		String name;
		String currentVersion;
		String latestVersion;
		
		public MelonInvalidMod(String name, String currentVersion, String latestVersion) {
			this.name = name;
			this.currentVersion = currentVersion;
			this.latestVersion = latestVersion;
		}
	}
}
