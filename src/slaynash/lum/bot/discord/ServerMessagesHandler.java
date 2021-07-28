package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.TimeManager;

public class ServerMessagesHandler {


    private static String fileExt;

    public static void handle(MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;
            long guildID = event.getGuild().getIdLong();
            String guildIDstr = event.getGuild().getId();
            boolean[] defaultConfig = new boolean[GuildConfigurations.ConfigurationMap.values().length];
            defaultConfig[GuildConfigurations.ConfigurationMap.LOGSCAN.ordinal()] = true;
            boolean[] guildConfig;
            guildConfig = GuildConfigurations.configurations.get(guildID) == null ? defaultConfig : GuildConfigurations.configurations.get(guildID);
            String message = event.getMessage().getContentStripped().toLowerCase();
            String memberMention = event.getMessage().getMember().getAsMention();
            List<Attachment> attachments = event.getMessage().getAttachments();

            System.out.printf("[%s] [%s][%s] %s%s%s: %s%s\n",
                    TimeManager.getTimeForLog(),
                    event.getGuild().getName(),
                    event.getTextChannel().getName(),
                    event.getAuthor().getAsTag(),
                    event.getMessage().isEdited() ? " *edited*" : "",
                    event.getMessage().getType().isSystem() ? " *system*" : "",
                    event.getMessage().getContentRaw().replace("\n", "\n\t\t"),
                    event.getMessage().getAttachments().isEmpty() ? "" : " *has attachments*");

            CommandManager.runAsServer(event);

            if (!event.getMessage().isEdited()) { //log handler
                if (guildConfig[GuildConfigurations.ConfigurationMap.GENERALLOGREMOVER.ordinal()] && (event.getChannel().getName().toLowerCase().contains("general") || (event.getMessage().getCategory() != null && event.getMessage().getCategory().getIdLong() == 705284406561996811L/*emm high-tech*/)) && attachments.size() > 0 && MelonScanner.isValidFileFormat(attachments.get(0)) && !checkIfStaff(event)) {
                    String mess = memberMention + " ";
                    switch (guildIDstr) {
                        case "600298024425619456": //emmVRC
                            mess = mess + "Please reupload this log to <#600661924010786816> instead.";
                            break;
                        case "439093693769711616": //VRCMG
                            mess = mess + "Please reupload this log to <#440088207799877634> instead.";
                            break;
                        case "663449315876012052": //MelonLoader
                            mess = mess + "Please reupload this log to <#733305093264375849> instead.";
                            break;
                        case "563139253542846474": //BoneWorks
                            mess = mess + "Please reupload this log to <#675024565277032449> instead.";
                            break;
                        case "322211727192358914": //TLDModding
                            mess = mess + "Please reupload this log to <#827601339672035408> instead.";
                            break;
                        default:
                            mess = mess + "Please reupload this log to #help-and-support or #log-scanner channel instead.";
                            break;
                    }
                    event.getChannel().sendMessage(mess).queue();
                    if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE))
                        event.getMessage().delete().queue();
                }
                else if (guildConfig[GuildConfigurations.ConfigurationMap.LOGSCAN.ordinal()]) {
                    new Thread(() -> {
                        try {
                            MelonScanner.scanMessage(event);
                        }
                        catch (Exception e) {
                            ExceptionUtils.reportException("An error has occurred while reading logs:", e, event.getTextChannel());
                        }
                    }).start();
                }
            }

            if (guildConfig[GuildConfigurations.ConfigurationMap.SCAMSHIELD.ordinal()] && ScamShield.checkForFishing(event))
                return;

            if (guildConfig[GuildConfigurations.ConfigurationMap.DLLREMOVER.ordinal()] && !event.getMessage().isEdited() && !checkDllPostPermission(event)) {
                event.getMessage().delete().queue();
                event.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed(memberMention + " tried to post a " + fileExt + " file which is not allowed." + (fileExt.equals("dll") ? "\nPlease only download mods from trusted sources." : ""), Color.YELLOW)).queue();
                return;
            }

            if (event.getAuthor().getIdLong() == 381571564098813964L) // Miku Hatsune#6969
                event.getMessage().addReaction(":baka:828070018935685130").queue(); // was requested

            if (guildConfig[GuildConfigurations.ConfigurationMap.PARTIALLOGREMOVER.ordinal()] && (message.contains("[error]") || message.contains("developer:") || message.contains("[internal failure]") || message.contains("System.IO.Error") || message.contains("MelonLoader.Installer.Program"))) {
                System.out.println("Partial Log was printed");

                boolean postedInWhitelistedServer = false;
                for (long whitelistedGuildId : GuildConfigurations.whitelistedRolesServers.keySet()) {
                    if (whitelistedGuildId == guildID) {
                        postedInWhitelistedServer = true;
                        break;
                    }
                }
                if (postedInWhitelistedServer && !checkIfStaff(event)) {
                    event.getChannel().sendMessage(memberMention + " Please upload your `MelonLoader/Latest.log` instead of printing parts of it.\nIf you are unsure how to locate your Latest.log file, use the `!log` command in this channel.").queue();
                    event.getMessage().delete().queue();
                    return;
                }
            }

            if (guildConfig[GuildConfigurations.ConfigurationMap.LUMREPLIES.ordinal()] && !message.startsWith(".") && ChattyLum.handle(message, event))
                return;

            if (guildConfig[GuildConfigurations.ConfigurationMap.MLREPLIES.ordinal()]) {
                Long category = event.getMessage().getCategory() == null ? 0L : event.getMessage().getCategory().getIdLong();

                if (guildID == 663449315876012052L /* MelonLoader */) {
                    if (message.contains("melonclient") || message.contains("melon client") || message.contains("tlauncher")) {
                        event.getMessage().reply("This discord is about MelonLoader, a mod loader for Unity games. If you are looking for a Client, you are in the wrong Discord.").queue();
                        return;
                    }
                    if (message.matches(".*\\b(phas(mo(phobia)?)?)\\b.*") && message.matches(".*\\b(start|open|work|launch|mod|play|cheat|hack|crash(e)?)(s)?\\b.*") && !checkIfStaff(event)) {
                        event.getMessage().reply("We do not support the use of MelonLoader on Phasmophobia, nor does Phasmophobia support MelonLoader.\nPlease follow the instructions in this image to remove MelonLoader:").addFile(new File("images/MLPhasmo.png")).queue();
                        return;
                    }
                }

                if ((guildID == 600298024425619456L/*emmVRC*/ || guildID == 439093693769711616L/*VRCMG*/ || guildID == 663449315876012052L/*MelonLoader*/) && category != 765058331345420298L/*Tickets*/ && category != 801137026450718770L/*Mod Tickets*/ && category != 600914209303298058L/*Staff*/ && message.matches("(.*\\b(forg(o|e)t|reset|lost|t remember).*) (.*\\b(pin|password)\\b.*)|(.*\\b(pin|password)\\b.*) (.*\\b(forg(o|e)t|reset|lost|t remember).*)")) {
                    System.out.println("Forgot pin asked");
                    if (guildID == 600298024425619456L/*emmVRC*/)
                        event.getMessage().replyEmbeds(JDAManager.wrapMessageInEmbed(CrossServerUtils.sanitizeInputString(event.getMember().getEffectiveName()) + ", please create a new ticket in <#765785673088499752>. Thank you!", null)).queue();
                    else
                        event.getMessage().replyEmbeds(JDAManager.wrapMessageInEmbed("Please join the [emmVRC Network Discord](https://discord.gg/emmvrc). From there, create a new ticket in #network-support. A Staff Member will be with you when available to assist.", null)).queue();
                    return;
                }
                if ((guildID == 600298024425619456L/*emmVRC*/ || guildID == 439093693769711616L/*VRCMG*/) && category != 765058331345420298L/*Tickets*/ && category != 801137026450718770L/*Mod Tickets*/ && category != 600914209303298058L/*Staff*/ && message.matches("(.*\\b(disable|off|out)\\b.*) (.*\\bstealth\\b.*)|(.*\\bstealth\\b.*) (.*\\b(disable|off|out)\\b.*)")) {
                    System.out.println("Stealth mode asked");
                    event.getMessage().reply("To disable Stealth Mode, click the Report World button in your quick menu. From there, you can access emmVRC Functions. You'll find the Stealth Mode toggle on the 4th page.").queue();
                    return;
                }

                if (message.startsWith("!vrcuk") || message.startsWith("!cuck")) {
                    System.out.println("VRChatUtilityKit print");
                    event.getChannel().sendMessage("Please download https://api.vrcmg.com/v0/mods/231/VRChatUtilityKit.dll and put it in your Mods folder.").queue();
                    return;
                }

                if (message.startsWith("!log")) {
                    System.out.println("logs printed");
                    String sendMessage = "";
                    Message replied = event.getMessage().getReferencedMessage();
                    if (replied != null && replied.getMember() != null) {
                        sendMessage = sendMessage + CrossServerUtils.sanitizeInputString(replied.getMember().getEffectiveName()) + "\n\n";
                    }
                    else if (replied != null /*and member is null*/) {
                        event.getMessage().reply("That user is no longer in this server.").queue();
                        return;
                    }
                    String temp;
                    if (guildConfig[GuildConfigurations.ConfigurationMap.GENERALLOGREMOVER.ordinal()] && event.getChannel().getName().toLowerCase().contains("general")) {
                        if (guildID == 600298024425619456L /*emmVRC*/)
                            temp = "into <#600661924010786816>";
                        else if (guildID == 439093693769711616L /*VRCMG*/)
                            temp = "into <#440088207799877634>";
                        else if (guildID == 663449315876012052L /*MelonLoader*/)
                            temp = "into <#733305093264375849>";
                        else
                            temp = "into help-and-support";
                    }
                    else
                        temp = "here";
                    sendMessage = sendMessage + "How to find your Log file:\n\n- go to your game's root folder. It's the folder that contains your `Mods` folder\n- open the `MelonLoader` folder\n- find the file called `Latest.log`\n- drag and drop that file " + temp + ".\n\nIf you see `MelonLoader.ModHandler.dll` instead of `Latest.log`, Please update MelonLoader to " + MelonScanner.latestMLVersionRelease;
                    event.getChannel().sendMessage(sendMessage).queue();
                    return;
                }

                if (message.startsWith("!uix")) {
                    System.out.println("UIX printed");
                    event.getChannel().sendMessage("Please download https://api.vrcmg.com/v0/mods/55/UIExpansionKit.dll and put it in your Mods folder.").queue();
                    return;
                }

                if (message.startsWith("!amapi") || message.startsWith("!vrcama")) {
                    System.out.println("actionmenuapi printed");
                    event.getChannel().sendMessage("Please download https://api.vrcmg.com/v0/mods/201/ActionMenuApi.dll and put it in your Mods folder.").queue();
                    return;
                }

                if (message.startsWith("!ovras")) {
                    System.out.println("OVR:AS printed");
                    event.getChannel().sendMessage("https://youtu.be/E4ZByfPWTuM").queue();
                    return;
                }

                if (message.startsWith("!vrcx")) {
                    System.out.println("VRCX printed");
                    event.getChannel().sendMessage("VRCX is not a mod and you can find it here: <https://github.com/Natsumi-sama/VRCX>").queue();
                    return;
                }

                if (message.startsWith("!proxy")) {
                    System.out.println("Proxy printed");
                    event.getChannel().sendMessage("In Windows, click the Start menu and type in \"Proxy\" and click the result \"Change Proxy\". Disable all 3 toggles in the image below:").addFile(new File("images/proxy.png")).queue();
                    return;
                }

                if (message.startsWith("!vrcmu")) {
                    System.out.println("VRCModUpdater printed");
                    event.getChannel().sendMessage("Please download the VRChat Mod Updater and move it into your Plugins folder: https://github.com/Slaynash/VRCModUpdater/releases/latest/download/VRCModUpdater.Loader.dll").queue();
                    return;
                }

                if (message.startsWith("!vrcma")) {
                    System.out.println("VRCMelonAssistant printed");
                    event.getChannel().sendMessage("Download the VRChat Mod Assistant and double click it to easily install mods: <https://github.com/knah/VRCMelonAssistant/releases/latest/download/VRCMelonAssistant.exe>").queue();
                    return;
                }

                if (message.startsWith("!vrcticket")) {
                    System.out.println("VRChat Ticket printed");
                    event.getChannel().sendMessage("Please open a new VRChat ticket as there is not much we can do about that <https://help.vrchat.com/hc/requests/new>").queue();
                    return;
                }

                if (message.startsWith("!vrcanny")) {
                    System.out.println("VRChat Canny printed");
                    event.getChannel().sendMessage("Please make your voice heard over at VRChat's Canny <https://vrchat.canny.io/feature-requests>").queue();
                    return;
                }
            }

            if (guildConfig[GuildConfigurations.ConfigurationMap.DADJOKES.ordinal()] && LumJokes.sendJoke(event)) {
                return;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("An error has occurred processing message:", e, event.getTextChannel());
        }
    }

    public static void handle(MessageUpdateEvent event) {
        handle(new MessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));
        return;
    }

    /**
     * Check if the message is posted in a guild using a whitelist and if it contains a DLL.
     * @param event
     * @return true if the message is posted in a guild using a whitelist, contains a DLL attachment, and isn't posted by a whitelisted user
     */
    private static boolean checkDllPostPermission(MessageReceivedEvent event) {
        long guildId = event.getGuild().getIdLong();
        boolean postedInWhitelistedServer = false;
        for (long whitelistedGuildId : GuildConfigurations.whitelistedRolesServers.keySet()) {
            if (whitelistedGuildId == guildId) {
                postedInWhitelistedServer = true;
                break;
            }
        }

        if (!postedInWhitelistedServer)
            return true; // Not a whitelisted server

        for (Attachment attachment : event.getMessage().getAttachments()) {
            fileExt = attachment.getFileExtension();
            if (fileExt == null) fileExt = "";
            fileExt = fileExt.toLowerCase();

            if (fileExt.equals("dll") || fileExt.equals("exe") || fileExt.equals("zip") || fileExt.equals("7z") ||
                fileExt.equals("rar") || fileExt.equals("unitypackage") || fileExt.equals("vrca") || fileExt.equals("fbx")) {

                if (checkIfStaff(event))
                    return true;

                return false; // The sender isn't allowed to send a DLL file
            }
        }
        return true; // No attachement, or no DLL
    }

    /**
     * Check if sender is part of Guild Staff/Trusted.
     * @param event
     * @return true if sender really was Guild Staff/Trusted
     */
    public static boolean checkIfStaff(MessageReceivedEvent event) {
        for (Entry<Long, long[]> whitelistedRolesServer : GuildConfigurations.whitelistedRolesServers.entrySet()) {
            Guild targetGuild;
            Member serverMember;
            if ((targetGuild = event.getJDA().getGuildById(whitelistedRolesServer.getKey())) != null &&
                (serverMember = targetGuild.getMember(event.getAuthor())) != null) {
                List<Role> roles = serverMember.getRoles();
                for (Role role : roles) {
                    long roleId = role.getIdLong();
                    for (long whitelistedRoleId : whitelistedRolesServer.getValue()) {
                        if (whitelistedRoleId == roleId) {
                            return true; // The sender is whitelisted
                        }
                    }
                }
            }
        }
        return false;
    }

}
