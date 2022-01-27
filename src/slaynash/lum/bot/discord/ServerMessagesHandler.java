package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.coder4.emoji.EmojiUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Message.MessageFlag;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;
import slaynash.lum.bot.discord.melonscanner.MelonScannerApisManager;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class ServerMessagesHandler {
    public static final String LOG_IDENTIFIER = "ServerMessagesHandler";

    private static String fileExt;

    public static void mainHandle(MessageReceivedEvent event) {
        try {
            if (event.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong() &&
                    event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */ &&
                    event.getChannel().getName().toLowerCase().startsWith("dm-") &&
                    !event.getMessage().getContentRaw().startsWith(".")) {
                String[] userID = event.getChannel().getName().split("-");
                User user = JDAManager.getJDA().getUserById(userID[userID.length - 1]);
                if (user == null) {
                    Utils.replyEmbed("Can not find user, maybe there are no mutual servers.", Color.red, event);
                    return;
                }
                user.openPrivateChannel().queue(channel -> channel.sendMessage(event.getMessage()).queue(null, e -> Utils.sendEmbed("Failed to send message to target user: " + e.getMessage(), Color.red, event)),
                        error -> Utils.sendEmbed("Failed to open DM with target user: " + error.getMessage(), Color.red, event));
                return;
            }

            handleAP(event);
            if (event.getMessage().getType().isSystem() || event.getAuthor().getDiscriminator().equals("0000")) return; //prevents Webhooks and deleted accounts
            if (event.getAuthor().isBot()) {
                if (event.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong()) {
                    handleReplies(event);
                    TicketTool.tickettool(event);
                }
                return;
            }
            if (ABCpolice.abcPolice(event))
                return;
            CommandManager.runAsServer(event);
            long guildID = event.getGuild().getIdLong();
            String guildIDstr = event.getGuild().getId();
            boolean[] defaultConfig = new boolean[GuildConfigurations.ConfigurationMap.values().length];
            defaultConfig[GuildConfigurations.ConfigurationMap.LOGSCAN.ordinal()] = true;
            boolean[] guildConfig;
            guildConfig = GuildConfigurations.configurations.get(guildID) == null ? defaultConfig : GuildConfigurations.configurations.get(guildID);
            String message = Junidecode.unidecode(event.getMessage().getContentStripped()).toLowerCase();
            String memberMention = event.getMessage().getMember() == null ? "" : event.getMessage().getMember().getAsMention();
            Message replied = event.getMessage().getReferencedMessage();
            List<Attachment> attachments = event.getMessage().getAttachments();
            if (message.startsWith("l!ping"))
                message = message.substring(6).trim();

            System.out.println(String.format("[%s][%s] %s%s%s: %s%s",
                    event.getGuild().getName(),
                    event.getTextChannel().getName(),
                    event.getAuthor().getAsTag(),
                    event.getMessage().isEdited() ? " *edited*" : "",
                    event.getMessage().getType().isSystem() ? " *system*" : "",
                    event.getMessage().getContentRaw().replace("\n", "\n\t\t"),
                    attachments.isEmpty() ? "" : " *has attachments* " + attachments.get(0).getUrl()));

            if (!event.getTextChannel().canTalk())
                return;

            if (!event.getMessage().isEdited()) { //log handler
                if (guildConfig[GuildConfigurations.ConfigurationMap.GENERALLOGREMOVER.ordinal()] && (event.getChannel().getName().toLowerCase().contains("general") || event.getMessage().getCategory() != null && event.getMessage().getCategory().getIdLong() == 705284406561996811L/*emm high-tech*/) && attachments.size() > 0 && MelonScanner.isValidFileFormat(attachments.get(0)) && !CrossServerUtils.checkIfStaff(event)) {
                    String mess = memberMention + " ";
                    switch (guildIDstr) {
                        case "600298024425619456": //emmVRC
                            mess = mess + "Please reupload this log to <#600661924010786816> instead.";
                            break;
                        case "439093693769711616": //VRCMG
                            mess = mess + "Please reupload this log to <#801792974542471168> instead.";
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
                        case "758553724226109480": //1330 Studios
                            mess = mess + "Please reupload this log to <#832441046750330920> instead.";
                            break;
                        default:
                            mess = mess + "Please reupload this log to help and support or log scanning channel instead.";
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

            if (replied != null && replied.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong())
                MelonScanner.translateLog(event);

            if (guildConfig[GuildConfigurations.ConfigurationMap.SCAMSHIELD.ordinal()])
                new Thread(() -> ScamShield.checkForFishing(event)).start();

            if (guildConfig[GuildConfigurations.ConfigurationMap.DLLREMOVER.ordinal()] && !event.getMessage().isEdited() && !checkDllPostPermission(event) && event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                event.getMessage().delete().queue();
                event.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed(memberMention + " tried to post a " + fileExt + " file which is not allowed." + (fileExt.equals("dll") ? "\nPlease only download mods from trusted sources." : ""), Color.YELLOW)).queue();
                return;
            }

            if (message.startsWith("."))
                return;

            if (event.getAuthor().getIdLong() == 381571564098813964L) // Miku Hatsune#6969
                event.getMessage().addReaction(":baka:828070018935685130").queue(); // was requested https://discord.com/channels/600298024425619456/600299027476643860/855140894171856936
            if (event.getAuthor().getIdLong() == 574676945183440918L) // Yuri Mamoru#6818
                event.getMessage().addReaction("🍪").queue(); // was requested https://discord.com/channels/439093693769711616/657545944136417280/927001826497269760

            if (handleReplies(event, message))
                return;

            if (guildConfig[GuildConfigurations.ConfigurationMap.PARTIALLOGREMOVER.ordinal()] && (message.contains("[error]") || message.contains("developer:") || message.contains("[internal failure]") || message.contains("system.io.error") || message.contains("melonloader.installer.program") || message.contains("system.typeloadexception: could not resolve type with token") || message.matches("\\[[0-9.:]+] -{30}"))) {
                System.out.println("Partial Log was printed");

                boolean postedInWhitelistedServer = false;
                for (long whitelistedGuildId : GuildConfigurations.whitelistedRolesServers.keySet()) {
                    if (whitelistedGuildId == guildID) {
                        postedInWhitelistedServer = true;
                        break;
                    }
                }
                if (postedInWhitelistedServer && !CrossServerUtils.checkIfStaff(event)) {
                    if (message.contains("failed to create logs folder")) {
                        event.getChannel().sendMessage(memberMention + " Please make sure your MelonLoader folder is clear of special characters like `'` or Chinese characters").queue();
                    }
                    else {
                        event.getChannel().sendMessage(memberMention + " Please upload your `MelonLoader/Latest.log` instead of printing parts of it.\nIf you are unsure how to locate your Latest.log file, use the `!log` command in this channel.").queue();
                        event.getMessage().delete().queue();
                    }
                    return;
                }
            }

            if (guildConfig[GuildConfigurations.ConfigurationMap.MLREPLIES.ordinal()]) {
                long category = event.getMessage().getCategory() == null ? 0L : event.getMessage().getCategory().getIdLong();

                if (guildID == 663449315876012052L /* MelonLoader */) {
                    if (message.contains("melonclient") || message.contains("melon client") || message.contains("tlauncher")) {
                        event.getMessage().reply("This discord is about MelonLoader, a mod loader for Unity games. If you are looking for a Client, you are in the wrong Discord.").queue();
                        return;
                    }
                    if (message.startsWith("!phas") || message.matches(".*\\b(phas(mo(phobia)?)?)\\b.*") && message.matches(".*\\b(start|open|work|launch|mod|play|cheat|hack|use it|crash(e)?)(s)?\\b.*") && !CrossServerUtils.checkIfStaff(event)) {
                        event.getMessage().reply("We do not support the use of MelonLoader on Phasmophobia, nor does Phasmophobia support MelonLoader.\nPlease remove everything that isn't in the following image:").addFile(new File("images/Phasmo_folder.png")).queue();
                        return;
                    }
                }

                if (!(message.contains("765785673088499752") || message.contains("network-support")) && (guildID == 600298024425619456L/*emmVRC*/ || guildID == 439093693769711616L/*VRCMG*/ || guildID == 663449315876012052L/*MelonLoader*/) && category != 765058331345420298L/*Tickets*/ && category != 801137026450718770L/*Mod Tickets*/ && category != 600914209303298058L/*Staff*/ && message.matches(".*\\b(forg([oe])t|reset|change|lost|t remember)\\b.*") && message.matches(".*\\b(pins?|password)\\b.*")) {
                    System.out.println("Forgot pin asked");
                    if (guildID == 600298024425619456L/*emmVRC*/) {
                        if (message.contains("remod"))
                            Utils.replyEmbed("You can reset your [remod pin here](https://requi.dev/remod/pin.php)", null, event);
                        else if (!event.getMember().hasAccess(event.getJDA().getTextChannelById(658177110514597888L)))
                            Utils.replyEmbed(CrossServerUtils.sanitizeInputString(event.getMember().getEffectiveName()) + ", please create a new ticket in <#765785673088499752>. Thank you!", null, event);
                    }
                    else
                        if (message.contains("remod"))
                            Utils.replyEmbed("You can reset your [remod pin here](https://requi.dev/remod/pin.php)", null, event);
                        else
                            Utils.replyEmbed("Please join the [emmVRC Network Discord](https://discord.gg/emmvrc). From there, create a new ticket in #network-support. A Staff Member will be with you when available to assist.", null, event);
                    return;
                }

                if (message.startsWith("!vrcuk") || message.startsWith("!cuck")) {
                    System.out.println("VRChatUtilityKit print");
                    event.getChannel().sendMessage("Please download https://api.vrcmg.com/v0/mods/231/VRChatUtilityKit.dll and put it in your Mods folder.").queue();
                    return;
                }

                if (message.startsWith("!loggif")) {
                    System.out.println("log GIF printed");
                    event.getChannel().sendMessage("https://media.discordapp.net/attachments/600661924010786816/807099652611899402/Ke4nJYRBK5.gif").queue();
                    event.getChannel().sendMessage("https://media.discordapp.net/attachments/600661924010786816/807070239644450836/4qJBVv68ZN.gif").queue();
                    event.getChannel().sendMessage("https://media.discordapp.net/attachments/440088207799877634/807857275498790912/S6yvAxnMkE.gif").queue();
                    return;
                }

                if (message.startsWith("!log")) {
                    System.out.println("logs printed");
                    String sendMessage = "";
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
                            temp = "into <#801792974542471168>";
                        else if (guildID == 663449315876012052L /*MelonLoader*/)
                            temp = "into <#733305093264375849>";
                        else
                            temp = "into help-and-support";
                    }
                    else
                        temp = "here";
                    sendMessage = sendMessage + "How to find your Log file:\n\n- go to your game's root folder. It's the folder that contains your `Mods` folder\n- open the `MelonLoader` folder\n- find the file called `Latest` or `Latest.log`\n- drag and drop that file " + temp;
                    event.getChannel().sendMessage(sendMessage).allowedMentions(Collections.emptyList()).queue();
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

                if (message.startsWith("!tags")) {
                    System.out.println("VRChat Tags printed");
                    event.getChannel().sendMessage("https://i.imgur.com/dBoxDVG.png\nhttps://vrchatapi.github.io/tutorials/tags/").queue();
                    return;
                }

                if (message.startsWith("!vrccrash")) {
                    System.out.println("VRChat Crash");
                    event.getChannel().sendMessage("https://help.vrchat.com/hc/en-us/articles/1500002247921-I-crashed-can-t-launch-VRChat-other-issues").queue();
                    return;
                }
            }

            if (guildConfig[GuildConfigurations.ConfigurationMap.DADJOKES.ordinal()] && LumJokes.sendJoke(event)) {
                return;
            }

            if (guildConfig[GuildConfigurations.ConfigurationMap.LUMREPLIES.ordinal()] && ChattyLum.handle(message, event))
                return;
        }
        catch (Exception e) {
            ExceptionUtils.reportException("An error has occurred processing message:", e);
        }
    }

    public static void handle(MessageUpdateEvent event) {
        handle(new MessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));
    }

    public static void handle(MessageReceivedEvent event) {
        long inputTime = System.nanoTime();
        mainHandle(event);

        if (event.getMessage().getContentStripped().toLowerCase().startsWith("l!ping")) {
            long processing = System.nanoTime() - inputTime;
            long gatewayPing = event.getJDA().getGatewayPing();
            event.getChannel().sendMessage("Pong: Ping from Discord " + gatewayPing + "millisecond" + (gatewayPing > 1 ? "s" : "") + ".\nIt took " + processing + " nanosecond" + (processing > 1 ? "s" : "") + " to parse the command.").queue();
        }
    }

    /**
     * Check if the message is posted in a guild using a whitelist and if it contains a DLL.
     * @param event MessageReceivedEvent
     * @return true if the message is posted in a guild using a whitelist, contains a DLL attachment, and isn't posted by a whitelisted user
     */
    private static boolean checkDllPostPermission(MessageReceivedEvent event) {
        if (CrossServerUtils.checkIfStaff(event))
            return true;

        boolean allowed = true;
        for (Attachment attachment : event.getMessage().getAttachments()) {
            fileExt = attachment.getFileExtension();
            if (fileExt == null) fileExt = "";
            fileExt = fileExt.toLowerCase();

            if (fileExt.equals("dll")) {
                if (!checkHash(attachment)) {
                    allowed = false;
                    break;
                }
            }
            else if (fileExt.equals("exe") || fileExt.equals("zip") || fileExt.equals("7z") || fileExt.equals("rar") ||
                fileExt.equals("unitypackage") || fileExt.equals("vrca") || fileExt.equals("fbx")) {
                allowed = false;
                break;
            }
        }
        return allowed;
    }

    public static boolean checkHash(Attachment attachment) {
        try {
            InputStream is = attachment.retrieveInputStream().get();
            byte[] data = is.readAllBytes(); //Blocks so maybe limit large downloads
            is.close();
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            String hash = MelonScannerApisManager.bytesToHex(digester.digest(data));
            System.out.println("Attached DLL has the hash of: " + hash);
            return MelonScannerApisManager.getMods("VRChat").stream().anyMatch(m -> hash.equalsIgnoreCase(m.versions[0].hash)); //TODO loop through all Unity games with hashes
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed attachment hash check", e);
        }
        return false;
    }

    private static void handleAP(MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong() || event.getMessage().getContentRaw().startsWith("l!"))
            return;
        try {
            if (event.getTextChannel().isNews() && CommandManager.apChannels.contains(event.getChannel().getIdLong()) && event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE, Permission.VIEW_CHANNEL) && !event.getMessage().getFlags().contains(MessageFlag.CROSSPOSTED)) {
                event.getMessage().crosspost().queue();
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to handle Auto Publish", e);
        }
    }

    public static boolean handleReplies(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw().toLowerCase();
        return handleReplies(event, content);
    }

    public static boolean handleReplies(MessageReceivedEvent event, String content) {
        try {
            if (content == null || content.isBlank())
                return false;
            content = content.toLowerCase();
            if (event.getMessage().isEdited())
                return false;
            if (event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong())
                return true;
            if (content.startsWith("l!replies"))
                return true;
            Map<String, String> regexReplies = CommandManager.guildRegexReplies.get(event.getGuild().getIdLong());
            Map<String, String> replies = CommandManager.guildReplies.get(event.getGuild().getIdLong());

            if (regexReplies != null) {
                for (Entry<String, String> reply : regexReplies.entrySet()) {
                    String value = reply.getValue().replace("%u", event.getAuthor().getName());
                    if (content.matches("(?s)".concat(reply.getKey()))) {
                        if (EmojiUtils.isOneEmoji(value))
                            event.getMessage().addReaction(value).queue();
                        else if ((value.startsWith("<:") || value.startsWith("<a:")) && value.endsWith(">") && value.split(":").length == 3)
                            event.getMessage().addReaction(value.substring(1, value.length() - 1)).queue(); //This could error if unknown or too many reactions on message
                        else
                            event.getTextChannel().sendMessage(value).allowedMentions(Arrays.asList(MentionType.USER, MentionType.ROLE)).queue();
                        return true;
                    }
                }
            }
            if (replies != null) {
                for (Entry<String, String> reply : replies.entrySet()) {
                    String value = reply.getValue().replace("%u", event.getAuthor().getName());
                    if (content.contains(reply.getKey())) {
                        if (EmojiUtils.isOneEmoji(value))
                            event.getMessage().addReaction(value).queue();
                        else if ((value.startsWith("<:") || value.startsWith("<a:")) && value.endsWith(">") && value.split(":").length == 3)
                            event.getMessage().addReaction(value.substring(1, value.length() - 1)).queue();
                        else
                            event.getTextChannel().sendMessage(value).allowedMentions(Arrays.asList(MentionType.USER, MentionType.ROLE)).queue();
                        return true;
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
