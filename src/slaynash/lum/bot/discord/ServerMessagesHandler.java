package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.coder4.emoji.EmojiUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Message.MessageFlag;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.ConfigManager;
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
            if (event.getChannel().getName().contains("no-chat")) {
                if (event.getMessage().getType() == MessageType.THREAD_CREATED)
                    event.getMessage().delete().queue();
                else if (!event.getAuthor().isBot() && event.getMessage().getAttachments().isEmpty() && !event.getMessage().getContentRaw().contains("http")) {

                    event.getChannel().getHistoryBefore(event.getMessage(), 1).queue(history -> {
                        if (history.getRetrievedHistory().isEmpty() || history.getRetrievedHistory().get(0).getAuthor() != event.getAuthor()
                            || history.getRetrievedHistory().get(0).getAttachments().isEmpty() || history.getRetrievedHistory().get(0).getContentRaw().contains("http")
                            || event.getMessage().getTimeCreated().isAfter(history.getRetrievedHistory().get(0).getTimeCreated().plusMinutes(5)))
                        {
                            event.getMessage().reply("No chat in this channel, please upload a photo or create a thread to chat in.")
                                .delay(Duration.ofSeconds(6)).flatMap(Message::delete).flatMap(t -> event.getMessage().delete()).queue();
                        }
                    });
                    return;
                }
            }
            if (MessageProxy.fromDev(event))
                return;
            CommandManager.runAsServer(event);
            if (event.getChannel().getType() == ChannelType.NEWS) {
                handleAP(event);
                return;
            }
            if (event.getMessage().getType().isSystem() || event.isWebhookMessage()) return; //prevents Webhooks and deleted accounts
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
            if (message.startsWith(ConfigManager.discordPrefix + "ping"))
                message = message.substring(6).trim();

            System.out.println(String.format("[%s][%s][%s] %s%s%s: %s%s",
                    event.getGuild().getName(),
                    event.getChannel().getName(),
                    event.getChannelType(),
                    event.getAuthor().getEffectiveName(),
                    event.getMessage().isEdited() ? " *edited*" : "",
                    event.getMessage().getType().isSystem() ? " *system*" : "",
                    event.getMessage().getContentRaw().replace("\n", "\n\t\t"),
                    attachments.isEmpty() ? "" : " *has attachments* " + attachments.get(0).getUrl()));

            if (attachments.size() == 1) {  // temp debug for scam bot breaking bots
                System.out.println(event.getRawData().toString());
            }

            if (event.getChannel().getType() == ChannelType.TEXT && !event.getChannel().canTalk())
                return;

            if (!event.getMessage().isEdited()) { //log handler
                if (guildconfig.MLGeneralRemover() && event.getChannel().getName().toLowerCase().contains("general") && event.getChannelType() == ChannelType.TEXT && !CrossServerUtils.checkIfStaff(event) && !attachments.isEmpty() && MelonScanner.isValidFileFormat(attachments.get(0), false)) {
                    String mess = switch (guildIDstr) {
                        case "663449315876012052" -> //MelonLoader
                            memberMention + " Please reupload this log to <#733305093264375849> instead.";
                        case "563139253542846474" -> //BoneLab
                            memberMention + " Please create a thread in <#1019659373695475802> and reupload this log to there instead.";
                        case "322211727192358914" -> //TLDModding
                            memberMention + " Please reupload this log to <#827601339672035408> instead.";
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
                            ExceptionUtils.reportException("An error has occurred while reading logs:", e, event.getChannel());
                        }
                    }).start();
                }
            }

            if (replied != null && replied.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong())
                MelonScanner.translateLog(event);

            if (guildconfig.ScamShield())
                new Thread(() -> ScamShield.checkForFishing(event)).start();

            if (guildconfig.DLLRemover() && !event.getMessage().isEdited() && !checkDllPostPermission(event) && event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_MANAGE)) {
                event.getMessage().delete().queue();
                event.getChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed(memberMention + " tried to post a " + fileExt + " file which is not allowed." + (fileExt.equals("dll") ? "\nPlease only download mods from trusted sources." : ""), Color.YELLOW)).queue();
                return;
            }

            if (message.startsWith("."))
                return;

            if (guildconfig.LumReplies()) {
                if (event.getAuthor().getIdLong() == 381571564098813964L) // Miku Hatsune#6969
                    event.getMessage().addReaction(Emoji.fromCustom("baka", 828070018935685130L, false)).queue(); // was requested https://discord.com/channels/600298024425619456/600299027476643860/855140894171856936
            }

            if (handleReplies(event, message))
                return;

            if (guildconfig.MLPartialRemover() && (message.contains("[error] ") || message.contains("developer:") || message.contains("[internal failure] ") || message.contains("system.io.error") || message.contains("melonloader.installer.program") || message.contains("d:\\a\\melonloader") || message.contains("system.typeloadexception: could not resolve type with token") || message.matches("\\[[\\d.:]+] -{30}"))) {
                System.out.println("Partial Log was printed");

                if (event.getChannel().getName().contains("develo") || event.getChannel().getName().contains("modders"))
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

                if (message.startsWith("!loggif")) {
                    System.out.println("log GIF printed");
                    event.getChannel().sendMessage("https://cdn.discordapp.com/attachments/1002485820478980157/1002508618500943882/browselocal.gif").queue();
                    event.getChannel().sendMessage("https://cdn.discordapp.com/attachments/1002485820478980157/1002509011075211334/getlog.gif").queue();
                    event.getChannel().sendMessage("The file will either be named Latest or Latest.log depending on your system settings").queue();
                    return;
                }

                if (message.contains("!log") || event.getMessage().getContentRaw().contains("821719779756081182")) {
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
                        else if (guildID == 322211727192358914L /*TLDModding*/)
                            temp = "into <#468386891507695628>";
                        else if (guildID == 563139253542846474L /*BoneLab*/)
                            temp = "into <#1019659373695475802>";
                        else
                            temp = "into help-and-support";
                    }
                    else
                        temp = "here";
                    sendMessage = sendMessage + "How to find your Log file:\n\n- go to your game's root folder. It's the folder that contains your `Mods` folder\n- open the `MelonLoader` folder\n- find the file called `Latest` or `Latest.log`\n- drag and drop that file " + temp;
                    event.getChannel().sendMessage(sendMessage).setAllowedMentions(Collections.emptyList()).queue();
                    return;
                }

                if (message.startsWith("!ovras")) {
                    System.out.println("OVR:AS printed");
                    event.getChannel().sendMessage("Download it from Steam here: <https://store.steampowered.com/app/1009850/OVR_Advanced_Settings/>\nVideo guide on setting up playspacemover: https://youtu.be/E4ZByfPWTuM").queue();
                    return;
                }

                if (message.startsWith("!proxy")) {
                    System.out.println("Proxy printed");
                    event.getChannel().sendMessage("In Windows, click the Start menu and type in \"Proxy\" and click the result \"Change Proxy\". Disable all 3 toggles in the image below:").addFiles(FileUpload.fromData(new File("images/proxy.png"), "proxy.png")).queue();
                    return;
                }
            }

            if (guildconfig.DadJokes() && LumJokes.sendJoke(event)) {
                return;
            }

            if (guildconfig.LumReplies() && ChattyLum.handle(message, event))
                //noinspection UnnecessaryReturnStatement
                return;
        }
        catch (Exception e) {
            ExceptionUtils.reportException("An error has occurred processing message:", e);
        }
    }

    public static void handle(MessageReceivedEvent event) {
        long inputTime = System.nanoTime();
        mainHandle(event);

        if (event.getMessage().getContentStripped().toLowerCase().startsWith(ConfigManager.discordPrefix + "ping")) {
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

        // TODO check linked downloads
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
                fileExt.equals("unitypackage") || fileExt.equals("vrca") || fileExt.equals("fbx"))
            {
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
            String hash = Utils.bytesToHex(digester.digest(data));
            System.out.println("Attached DLL has the hash of: " + hash);
            return MelonScannerApisManager.getMods("ChilloutVR").stream().anyMatch(m -> hash.equalsIgnoreCase(m.versions[0].hash())); //TODO loop through all Unity games with hashes
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed attachment hash check", e);
        }
        return false;
    }

    private static void handleAP(MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong() || event.getMessage().getContentRaw().startsWith(ConfigManager.discordPrefix))
            return;
        try {
            if (event.getChannel().getType() == ChannelType.NEWS && CommandManager.apChannels.contains(event.getChannel().getIdLong()) && event.getGuild().getSelfMember().hasPermission(event.getChannel().asNewsChannel(), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_MANAGE, Permission.VIEW_CHANNEL) && !event.getMessage().getFlags().contains(MessageFlag.CROSSPOSTED) && !event.getMessage().getFlags().contains(MessageFlag.IS_CROSSPOST)) {
                System.out.println("Crossposting in " + event.getGuild().getName() + ", " + event.getChannel().getName());
                event.getMessage().crosspost().queue(null, e -> { });
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
            if (event.getChannelType() == ChannelType.PRIVATE) // Possible to get here from MelonScanner in DMs
                return MessageProxy.handleDMReplies(event, content);
            content = content.toLowerCase();
            if (event.getAuthor().equals(event.getJDA().getSelfUser()))
                return true;
            String guildID = event.isFromGuild() ? event.getGuild().getId() : "0";
            if (event.getChannelType() == ChannelType.TEXT && event.getChannel().asTextChannel().getParentCategoryIdLong() == 924780998124798022L) guildID = "0";
            try {
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `Replies` WHERE `guildID` = ?", guildID);

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
                    boolean report = rs.getBoolean("breport");
                    if (!edit && event.getMessage().isEdited()) {
                        continue;
                    }
                    if (regex != null && !regex.isBlank()) {
                        if (!content.matches("(?s)".concat(regex))) { //TODO prevent Catastrophic backtracking
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

                    if (delete && event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_MANAGE)) {
                        event.getMessage().delete().queue();
                    }
                    if (kick && event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.KICK_MEMBERS)) {
                        event.getMember().kick().queue();
                    }
                    if (ban && event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.BAN_MEMBERS)) {
                        event.getMember().ban(0, TimeUnit.DAYS).queue();
                    }
                    if (EmojiUtils.isOneEmoji(message))
                        event.getMessage().addReaction(Emoji.fromUnicode(message)).queue();
                    else if (message != null && message.matches("^<a?:\\w+:\\d+>$")) {
                        System.out.println("Emoji: " + message);
                        RichCustomEmoji emote = event.getJDA().getEmojiById(message.replace(">", "").split(":")[2]);
                        try {
                            if (emote.canInteract(emote.getGuild().getSelfMember()))
                                event.getMessage().addReaction(emote).queue(); //This could error if too many reactions on message
                            else
                                event.getChannel().asGuildMessageChannel().sendMessage("Lum can not use emote in reply " + ukey).queue();
                        }
                        catch (Exception e) {
                            event.getChannel().asGuildMessageChannel().sendMessage("Lum can not use that emote from reply " + ukey + " as I need to be in that emote's server.").queue();
                        }
                    }
                    else if (message != null && !message.isBlank()) {
                        if (guildID.equals("0")) {
                            String[] userID = event.getChannel().getName().split("-");
                            User privuser = JDAManager.getJDA().retrieveUserById(userID[userID.length - 1]).complete();
                            if (privuser == null) {
                                Utils.replyEmbed("Can not find user, maybe there are no mutual servers.", Color.red, event);
                            }
                            else {
                                privuser.openPrivateChannel().queue(
                                    privchannel -> privchannel.sendMessage(message).queue(s -> MessageProxy.saveIDs(s.getIdLong(), event.getMessageIdLong()),
                                            e -> Utils.sendEmbed("Failed to send message to target user: " + e.getMessage(), Color.red, event)),
                                    error -> Utils.sendEmbed("Failed to open DM with target user: " + error.getMessage(), Color.red, event));
                            }
                        }
                        else
                            event.getMessage().reply(message).setAllowedMentions(Arrays.asList(MentionType.USER, MentionType.ROLE)).queue();
                    }
                    if (report) {
                        MessageChannelUnion reportChannel = CommandManager.getModReportChannels(event, "reply");
                        if (reportChannel != null) {
                            if (!event.getGuild().getSelfMember().hasPermission(reportChannel.asGuildMessageChannel(), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)) {
                                event.getMessage().reply("I can not send reports to the report channel as I do not have permission to view or send messages in that channel.").queue();
                            }
                            else {
                                EmbedBuilder eb = new EmbedBuilder();
                                eb.setTitle("Reply Report");
                                eb.addField("User", event.getAuthor().getEffectiveName() + " (" + event.getAuthor().getId() + ")", false);
                                eb.addField("Channel", event.getChannel().getName() + " (" + event.getChannel().getId() + ")\n" + event.getMessage().getJumpUrl(), false);
                                eb.addField("Message", event.getMessage().getContentRaw(), false);
                                eb.addField("Reply", message, false);
                                eb.setFooter("Reply ID: " + ukey);
                                eb.setColor(Color.orange);
                                eb.setTimestamp(Instant.now());
                                Utils.sendEmbed(eb.build(), reportChannel);
                            }
                        }
                    }
                    DBConnectionManagerLum.closeRequest(rs);
                    return true;
                }

                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to check replies", e, event.getChannel());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
