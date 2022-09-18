package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.coder4.emoji.EmojiUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Message.MessageFlag;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.DBConnectionManagerLum;
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
            if (MessageProxy.fromDev(event))
                return;
            CommandManager.runAsServer(event);
            if (event.getChannel().getType() == ChannelType.NEWS) {
                handleAP(event);
                return;
            }
            if (event.getMessage().getType().isSystem() || event.getAuthor().getDiscriminator().equals("0000")) return; //prevents Webhooks and deleted accounts
            if (event.getAuthor().isBot()) {
                if (event.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong()) {
                    handleReplies(event);
                }
                return;
            }
            if (ABCpolice.abcPolice(event))
                return;
            if (Memes.memeRecieved(event))
                return;

            long guildID = event.getGuild().getIdLong();
            String guildIDstr = event.getGuild().getId();
            GuildConfiguration guildconfig = DBConnectionManagerLum.getGuildConfig(guildID);
            String message = Junidecode.unidecode(event.getMessage().getContentStripped()).toLowerCase();
            String memberMention = event.getMessage().getMember() == null ? "" : event.getMessage().getMember().getAsMention();
            Message replied = event.getMessage().getReferencedMessage();
            List<Attachment> attachments = event.getMessage().getAttachments();
            if (message.startsWith("l!ping"))
                message = message.substring(6).trim();

            System.out.println(String.format("[%s][%s] %s%s%s: %s%s",
                    event.getGuild().getName(),
                    event.getChannel().getName(),
                    event.getAuthor().getAsTag(),
                    event.getMessage().isEdited() ? " *edited*" : "",
                    event.getMessage().getType().isSystem() ? " *system*" : "",
                    event.getMessage().getContentRaw().replace("\n", "\n\t\t"),
                    attachments.isEmpty() ? "" : " *has attachments* " + attachments.get(0).getUrl()));

            if (!event.getChannel().canTalk())
                return;

            if (!event.getMessage().isEdited()) { //log handler
                if (guildconfig.MLGeneralRemover() && (event.getChannel().getName().toLowerCase().contains("general") || event.getMessage().getCategory() != null && event.getMessage().getCategory().getIdLong() == 705284406561996811L/*emm high-tech*/) && attachments.size() > 0 && MelonScanner.isValidFileFormat(attachments.get(0), false) && !CrossServerUtils.checkIfStaff(event)) {
                    String mess = switch (guildIDstr) {
                        case "600298024425619456" -> //emmVRC
                                memberMention + " Please reupload this log to <#600661924010786816> instead.";
                        case "439093693769711616" -> //VRCMG
                                memberMention + " Please reupload this log to <#801792974542471168> instead.";
                        case "663449315876012052" -> //MelonLoader
                                memberMention + " Please reupload this log to <#733305093264375849> instead.";
                        case "563139253542846474" -> //BoneWorks
                                memberMention + " Please reupload this log to <#675024565277032449> instead.";
                        case "322211727192358914" -> //TLDModding
                                memberMention + " Please reupload this log to <#827601339672035408> instead.";
                        case "758553724226109480" -> //1330 Studios
                                memberMention + " Please reupload this log to <#832441046750330920> instead.";
                        default -> memberMention + " Please reupload this log to help and support or log scanning channel instead.";
                    };
                    event.getChannel().sendMessage(mess).queue();
                    if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE))
                        event.getMessage().delete().queue();
                }
                else if (guildconfig.MLLogScan()) {
                    new Thread(() -> {
                        try {
                            MelonScanner.scanMessage(event);
                        }
                        catch (Exception e) {
                            ExceptionUtils.reportException("An error has occurred while reading logs:", e, event.getChannel().asTextChannel());
                        }
                    }).start();
                }
            }

            if (replied != null && replied.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong())
                MelonScanner.translateLog(event);

            if (guildconfig.ScamShield())
                new Thread(() -> ScamShield.checkForFishing(event)).start();

            if (guildconfig.DLLRemover() && !event.getMessage().isEdited() && !checkDllPostPermission(event) && event.getGuild().getSelfMember().hasPermission(event.getChannel().asTextChannel(), Permission.MESSAGE_MANAGE)) {
                event.getMessage().delete().queue();
                event.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed(memberMention + " tried to post a " + fileExt + " file which is not allowed." + (fileExt.equals("dll") ? "\nPlease only download mods from trusted sources." : ""), Color.YELLOW)).queue();
                return;
            }

            if (message.startsWith("."))
                return;

            // ignore any quotes
            StringBuilder sb = new StringBuilder();
            for (String line : message.split(System.getProperty("line.separator"))) {
                if (!line.startsWith("> "))
                    sb.append(line).append(System.getProperty("line.separator"));
            }
            message = sb.toString().trim();

            if (guildconfig.LumReplies()) {
                if (event.getAuthor().getIdLong() == 381571564098813964L) // Miku Hatsune#6969
                    event.getMessage().addReaction(Emoji.fromCustom("baka", 828070018935685130L, false)).queue(); // was requested https://discord.com/channels/600298024425619456/600299027476643860/855140894171856936
            }

            if (handleReplies(event, message))
                return;

            if (guildconfig.MLPartialRemover() && (message.contains("[error] ") || message.contains("developer:") || message.contains("[internal failure] ") || message.contains("system.io.error") || message.contains("melonloader.installer.program") || message.contains("system.typeloadexception: could not resolve type with token") || message.matches("\\[[\\d.:]+] -{30}"))) {
                System.out.println("Partial Log was printed");

                if (event.getChannel().getName().contains("develo"))
                    return;
                if (!CrossServerUtils.checkIfStaff(event)) {
                    if (message.contains("failed to create logs folder")) {
                        event.getChannel().sendMessage(memberMention + " Please make sure your MelonLoader folder is clear of special characters like `'` or Chinese characters").queue();
                    }
                    else {
                        event.getChannel().sendMessage(memberMention + " Please upload your `MelonLoader/Latest.log` instead of printing parts of it.\nIf you are unsure how to locate your Latest.log file, use the `!log` command in this channel.").queue();
                        if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE))
                            event.getMessage().delete().queue();
                    }
                    return;
                }
            }

            if (guildconfig.MLReplies()) {
                if (guildID == 663449315876012052L /* MelonLoader */) {
                    if (message.contains("melonclient") || message.contains("melon client") || message.contains("tlauncher")) {
                        event.getMessage().reply("This discord is about MelonLoader, a mod loader for Unity games. If you are looking for a Client, you are in the wrong Discord.").queue();
                        return;
                    }
                    if (message.startsWith("!phas") || message.matches(".*\\b(phas(mo(phobia)?)?)\\b.*") && message.matches(".*\\b(start|open|work|launch|mod|play|cheat|hack|use it|crash(e)?)(s)?\\b.*") && !CrossServerUtils.checkIfStaff(event)) {
                        event.getMessage().reply("We do not support the use of MelonLoader on Phasmophobia, nor does Phasmophobia support MelonLoader.\nPlease remove everything that isn't in the following image:").addFiles(FileUpload.fromData(new File("images/Phasmo_folder.png"), "Phasmo_folder.png")).queue();
                        return;
                    }
                }

                if (message.startsWith("!loggif")) {
                    System.out.println("log GIF printed");
                    event.getChannel().sendMessage("https://cdn.discordapp.com/attachments/1002485820478980157/1002508618500943882/browselocal.gif").queue();
                    event.getChannel().sendMessage("https://cdn.discordapp.com/attachments/1002485820478980157/1002509011075211334/getlog.gif").queue();
                    event.getChannel().sendMessage("The file will either be named Latest or Latest.log depending on your system settings").queue();
                    return;
                }

                if (message.startsWith("!log") || event.getMessage().getContentRaw().equals("<:logs:821719779756081182>")) {
                    if (message.startsWith("!loge")) {
                        System.out.println("loge");
                        event.getChannel().sendMessage("https://cdn.discordapp.com/attachments/733305093264375849/835411858533515334/LOGE.mp4").queue();
                        return;
                    }
                    System.out.println("logs printed");
                    String sendMessage = "";
                    if (replied != null && replied.getMember() != null) {
                        sendMessage = sendMessage + CrossServerUtils.sanitizeInputString(replied.getMember().getEffectiveName()) + "\n\n";
                    }
                    String temp;
                    if (guildconfig.MLGeneralRemover() && event.getChannel().getName().toLowerCase().contains("general")) {
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
                    event.getChannel().sendMessage(sendMessage).mention(Collections.emptyList()).queue();
                    return;
                }

                if (message.startsWith("!ovras")) {
                    System.out.println("OVR:AS printed");
                    event.getChannel().sendMessage("Download it from Steam here: <https://store.steampowered.com/app/1009850/OVR_Advanced_Settings/>\nVideo guide on setting up playspacemover: https://youtu.be/E4ZByfPWTuM").queue();
                    return;
                }

                if (message.startsWith("!vrcx")) {
                    System.out.println("VRCX printed");
                    event.getChannel().sendMessage("VRCX is not a mod and you can find it here: <https://github.com/Natsumi-sama/VRCX#how-to-install-vrcx>").queue();
                    return;
                }

                if (message.startsWith("!proxy")) {
                    System.out.println("Proxy printed");
                    event.getChannel().sendMessage("In Windows, click the Start menu and type in \"Proxy\" and click the result \"Change Proxy\". Disable all 3 toggles in the image below:").addFiles(FileUpload.fromData(new File("images/proxy.png"), "proxy.png")).queue();
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

                if (message.startsWith("!dmca")) {
                    System.out.println("DMCA");
                    event.getChannel().sendMessage("https://cdn.discordapp.com/attachments/773300021117321248/943737387450777640/DMCA.mp4").queue();
                    if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE))
                        event.getMessage().delete().queue();
                    return;
                }
            }

            if (guildconfig.DadJokes() && LumJokes.sendJoke(event)) {
                return;
            }

            if (guildconfig.LumReplies() && ChattyLum.handle(message, event))
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
            double processing = (System.nanoTime() - inputTime) / 1000000f;
            long gatewayPing = event.getJDA().getGatewayPing();
            event.getChannel().sendMessage("Pong: Ping from Discord " + gatewayPing + " millisecond" + (gatewayPing > 1 ? "s" : "") + ".\nIt took " + java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(processing) + " millisecond" + (processing > 1 ? "s" : "") + " to parse the command.").queue();
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
            InputStream is = attachment.getProxy().download().get();
            byte[] data = is.readAllBytes(); //Blocks so maybe limit large downloads
            is.close();
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            String hash = MelonScannerApisManager.bytesToHex(digester.digest(data));
            System.out.println("Attached DLL has the hash of: " + hash);
            return MelonScannerApisManager.getMods("ChilloutVR").stream().anyMatch(m -> hash.equalsIgnoreCase(m.versions[0].hash)); //TODO loop through all Unity games with hashes
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
            if (event.getChannel().getType() == ChannelType.NEWS && CommandManager.apChannels.contains(event.getChannel().getIdLong()) && event.getGuild().getSelfMember().hasPermission(event.getChannel().asTextChannel(), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_MANAGE, Permission.VIEW_CHANNEL) && !event.getMessage().getFlags().contains(MessageFlag.CROSSPOSTED) && !event.getMessage().getFlags().contains(MessageFlag.IS_CROSSPOST)) {
                System.out.println("Crossposting in " + event.getGuild().getName() + ", " + event.getChannel().asTextChannel().getName());
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
            if (event.getMember().equals(event.getGuild().getSelfMember()))
                return true;
            String guildID = event.getGuild().getId();
            try {
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `Replies` WHERE `guildID` = '" + guildID + "'");

                while (rs.next()) {
                    int ukey = rs.getInt("ukey");
                    String regex = rs.getString("regex");
                    String contains = rs.getString("contains");
                    String equals = rs.getString("equals");
                    long user = rs.getLong("user");
                    long channel = rs.getLong("channel");
                    long ignorerole = rs.getLong("ignorerole");
                    String message = rs.getString("message");
                    boolean delete = rs.getBoolean("bdelete");
                    boolean kick = rs.getBoolean("bkick");
                    boolean ban = rs.getBoolean("bban");
                    boolean edit = rs.getBoolean("bedit");
                    if (!edit && event.getMessage().isEdited()) {
                        continue;
                    }
                    if (regex != null && !regex.isBlank()) {
                        if (!content.matches("(?s)".concat(regex))) {
                            continue;
                        }
                    }
                    if (contains != null && !contains.isBlank()) {
                        if (!content.contains(contains)) {
                            continue;
                        }
                    }
                    if (equals != null && !equals.isBlank()) {
                        if (!content.equals(equals)) {
                            continue;
                        }
                    }
                    if (user > 69420) {
                        if (event.getAuthor().getIdLong() != user) {
                            continue;
                        }
                    }
                    if (channel > 69420) {
                        System.out.println("Channel: " + channel + " " + event.getChannel().getIdLong());
                        if (event.getChannel().getIdLong() != channel && (event.getChannel().asTextChannel() == null || event.getChannel().asTextChannel().getParentCategory() == null || event.getChannel().asTextChannel().getParentCategory().getIdLong() != channel)) {
                            continue;
                        }
                    }
                    if (ignorerole > 69420) {
                        if (event.getMember().getRoles().stream().anyMatch(r -> r.getIdLong() == ignorerole)) {
                            continue;
                        }
                    }
                    if (event.getAuthor().isBot() && !rs.getBoolean("bbot")) {
                        continue;
                    }

                    if (delete && event.getGuild().getSelfMember().hasPermission(event.getChannel().asTextChannel(), Permission.MESSAGE_MANAGE)) {
                        event.getMessage().delete().queue();
                    }
                    if (kick && event.getGuild().getSelfMember().hasPermission(event.getChannel().asTextChannel(), Permission.KICK_MEMBERS)) {
                        event.getMember().kick().queue();
                    }
                    if (ban && event.getGuild().getSelfMember().hasPermission(event.getChannel().asTextChannel(), Permission.BAN_MEMBERS)) {
                        event.getMember().ban(0, TimeUnit.DAYS).queue();
                    }
                    if (EmojiUtils.isOneEmoji(message))
                        event.getMessage().addReaction(Emoji.fromUnicode(message)).queue();
                    else if (message.matches("^<a?:\\w+:\\d+>$")) {
                        System.out.println("Emoji: " + message);
                        RichCustomEmoji emote = event.getJDA().getEmojiById(message.replace(">", "").split(":")[2]);
                        try {
                            if (emote.canInteract(emote.getGuild().getSelfMember()))
                                event.getMessage().addReaction(emote).queue(); //This could error if too many reactions on message
                            else
                                event.getChannel().asTextChannel().sendMessage("Lum can not use emote in reply " + ukey).queue();
                        }
                        catch (Exception e) {
                            event.getChannel().asTextChannel().sendMessage("Lum can not use that emote from reply " + ukey + " as I need to be in that emote's server.").queue();
                        }
                    }
                    else if (!message.isBlank()) {
                        event.getChannel().asTextChannel().sendMessage(message).queue();
                    }
                    DBConnectionManagerLum.closeRequest(rs);
                    return true;
                }

                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to check replies", e, event.getChannel().asTextChannel());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
