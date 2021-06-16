package slaynash.lum.bot.discord;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.melonscanner.LogCounter;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;

public class ServerMessagesHandler {
    private static final String[] alreadyHelpedSentences = new String[] {
        "I already answered you <:konataCry:553690186022649858>",
        "Why won't you read my answer <:angry:835647632843866122>",
        "There's already the answer up here!! <:cirHappy:829458722634858496>",
        "I've already given you a response! <:MeguminEyes:852057834686119946>"
    };

    private static final String[] alreadyHelpedSentencesRare = new String[] {
        "I wish I wasn't doing this job sometimes <:02Dead:835648208272883712>",
        "https://cdn.discordapp.com/attachments/657545944136417280/836231859998031932/unknown.png",
        "Your literacy skills test appears to have failed you. <:ram_disgusting:828070759070695425>",
        "<https://lmgtfy.app/?q=How+do+I+read>"
    };
    
    private static final String[] thankedSentences = new String[] {
        "You're Welcome <:Neko_cat_kiss_heart:851934821080367134>",
        "<:cirHappy:829458722634858496>",
        "Anytime <:Neko_cat_kiss_heart:851934821080367134>",
        "Always happy to help!",
        "Mhm of course!",
        "No problem!",
        "Glad I could help!"
    };

    private static final String[] thankedSentencesRare = new String[] {
        "Notices you senpai <:cirHappy:829458722634858496>",
        "https://tenor.com/view/barrack-obama-youre-welcome-welcome-gif-12542858"
    };

    private static final String[] helloLum = new String[] {
        "<:Neko_cat_owo:851938214105186304>",
        "<:Neko_cat_shrug:851938033724817428>",
        "<:Neko_cat_uwu:851938142810275852>",
        "<:Neko_cat_uwu:851938142810275852>",
        "<:Neko_cat_wave:851938087353188372>"
    };

    private static final String[] niceLum = new String[] {
        "<:Neko_cat_donut:851936309718024203>",
        "<:Neko_cat_okay:851938634327916566>",
        "<:Neko_cat_pizza:851935753826205736>",
        "<:Neko_cat_royal:851935888178544660>",
        "<:Neko_cat_woah:851935805874110504>",
        "<a:Neko_cat_HeadPat:851934772959510578>",
        "<a:HeartCat:828087151232286749>"
    };

    private static final String[] badLum = new String[] {
        "<:Neko_cat_drool_stupid:851936505516785715>",
        "<:Neko_cat_fear:851936400819486720>",
        "<:Neko_cat_prison:851936449548255264>"
    };

    private static final String[] gunLum = new String[] {
        "<:Neko_cat_Gun:851934721914175498>"
    };

    private static final int helpDuration = 6 * 60; //in seconds

    private static Random random = new Random();
    private static String fileExt;
    
    private static List<HelpedRecentlyData> helpedRecently = new ArrayList<>();

    private static final Queue<HandledServerMessageContext> handledMessages = new LinkedList<>();

    public static void handle(MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;
        CommandManager.runAsServer(event);
        Long GuildID = event.getGuild().getIdLong();
        Boolean guildConfig[];
        guildConfig = GuildConfigurations.configurations.get(GuildID) == null ? new Boolean[] {false,false,false,false,false} : GuildConfigurations.configurations.get(GuildID);
        String message = event.getMessage().getContentRaw().toLowerCase();

        System.out.printf("[%s] [%s][%s] %s: %s\n",
                TimeManager.getTimeForLog(),
                event.getGuild().getName(),
                event.getTextChannel().getName(),
                event.getAuthor().getName(),
                event.getMessage().getContentRaw() );

        if (guildConfig[GuildConfigurations.ConfigurationMap.SPAMSHIELD.ordinal()] && checkForFishing(event))
            return;

        if (guildConfig[GuildConfigurations.ConfigurationMap.DLLREMOVER.ordinal()] && !checkDllPostPermission(event)) {
            event.getMessage().delete().queue();
            event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("<@!" + event.getMessage().getMember().getId() + "> tried to post a " + fileExt + " file which is not allowed." + (fileExt.equals("dll") ? "\nPlease only download mods from trusted sources." : ""), Color.YELLOW)).queue();
            return;
        }

        if (event.getGuild().getIdLong() == 663449315876012052L /* MelonLoader */) {
            String messageLowercase = event.getMessage().getContentRaw().toLowerCase();
            if (messageLowercase.contains("melonclient") || messageLowercase.contains("melon client") || messageLowercase.contains("tlauncher"))
                event.getMessage().reply("This discord is about MelonLoader, a mod loader for Unity games. If you are looking for a Client, you are in the wrong Discord.").queue();
        }
        
        if (guildConfig[GuildConfigurations.ConfigurationMap.PARTIALLOGREMOVER.ordinal()] && (message.contains("[error]") || message.contains("developer:") || message.contains("[internal failure]"))) {
            System.out.println("Partial Log was printed");
            
            boolean postedInWhitelistedServer = false;
            long guildId = event.getGuild().getIdLong();
            for (long whitelistedGuildId : GuildConfigurations.whitelistedRolesServers.keySet()) {
                if (whitelistedGuildId == guildId) {
                    postedInWhitelistedServer = true;
                    break;
                }
            }
            if (postedInWhitelistedServer && !checkIfStaff(event)) {
                event.getChannel().sendMessage("<@!" + event.getMessage().getMember().getId() + "> Please upload your `MelonLoader/Latest.log` instead of printing parts of it.\nIf you are unsure how to locate your Latest.log file, use the `!log` command in this channel.").queue();
                event.getMessage().delete().queue();
            }
        }

        if(guildConfig[GuildConfigurations.ConfigurationMap.LUMREPLIES.ordinal()]){
            if (message.contains("thank") || message.contains("thx") || message.contains("neat") || message.contains("cool") || message.contains("nice") ||
                message.contains("helpful") || message.contains("epic") || message.contains("worked") || message.contains("tysm") || message.equals("ty") ||
                message.contains(" ty ") || message.contains("fixed") || message.matches("(^|.*\\s)rad(.*)") || message.contains("that bot") ||
                message.contains("this bot") || message.contains("awesome") || message.contains(" wow ")) {
                System.out.println("Thanks was detected");
                if (wasHelpedRecently(event) && (event.getMessage().getReferencedMessage()==null || event.getMessage().getReferencedMessage().getAuthor().getIdLong() == 275759980752273418L/*LUM*/)) {
                    String sentence;
                    boolean rare = random.nextInt(100) == 69;
                    if (rare)
                        sentence = "You're Welcome, but thank <@145556654241349632> and <@240701606977470464> instead for making me. <a:HoloPet:829485119664160828>";
                    else {
                        rare = random.nextInt(10) == 9;
                        sentence = rare
                        ? thankedSentencesRare[random.nextInt(thankedSentencesRare.length)]
                        : thankedSentences    [random.nextInt(thankedSentences.length)];
                    }
                event.getChannel().sendMessage(sentence).queue();
                return;
                }
            }
        
            else if (message.contains("help") && !message.contains("helping") || message.contains("fix") || message.contains("what do "/*i do*/) || message.contains("what should "/*i do*/)) {
                System.out.println("Help was detected");
                if (wasHelpedRecently(event) && (event.getMessage().getReferencedMessage()==null || event.getMessage().getReferencedMessage().getAuthor().getIdLong() == 275759980752273418L/*LUM*/)) {
                    String sentence;
                    boolean rare = random.nextInt(1000) == 420;
                    if (rare)
                        sentence = "Shut the fuck up, I literally answered your dumb ass!";
                    else {
                        rare = random.nextInt(10) == 9;
                        sentence = rare
                        ? alreadyHelpedSentencesRare[random.nextInt(alreadyHelpedSentencesRare.length)]
                        : alreadyHelpedSentences    [random.nextInt(alreadyHelpedSentences.length)];
                    }
                    event.getChannel().sendMessage(sentence).queue();
                    return;
                }
            }
        }

        if(guildConfig[GuildConfigurations.ConfigurationMap.LUMREPLIES.ordinal()]){
            if (message.matches("(.*\\b(good|nice|love|cool|cutie|helped|thank)\\b.*) (.*\\blum\\b.*)|(.*\\blum\\b.*) (.*\\b(good|nice|love|cool|cutie|helped|thank)\\b.*)")) {
                System.out.println("Nice Lum was detected");
                event.getChannel().sendMessage(niceLum[random.nextInt(niceLum.length)]).queue();
            }

            else if (message.contains(/*f*/" off lum") || message.contains(/*f*/" you lum") || message.contains("stfu lum")) {
                System.out.println("F off Lum was detected");
                event.getChannel().sendMessage(gunLum[random.nextInt(gunLum.length)]).queue();
            }

            else if (message.contains("bad lum") || message.contains("lum shush") || message.contains(/*shut*/" up lum") || message.contains(/*shush*/" it lum")) {
                System.out.println("Bad Lum was detected");
                event.getChannel().sendMessage(badLum[random.nextInt(badLum.length)]).queue();
            }

            else if (message.contains("hello lum") || message.contains("hi lum")) {
                System.out.println("Hello Lum was detected");
                event.getChannel().sendMessage(helloLum[random.nextInt(helloLum.length)]).queue();
            }

            else if ((message.contains("credit") || message.contains("stole")) && message.contains("lum")) {
                System.out.println("Lum stole Credit");
                event.getChannel().sendMessage("<:Hehe:792738744057724949>").queue();
            }
        }
        
        new Thread(() -> {
            try {
                MelonScanner.scanMessage(event);
            }
            catch(Exception e) {
                e.printStackTrace();
                
                String error = "**An error has occured while reading logs:**\n" + ExceptionUtils.getStackTrace(e);
                
                if (error.length() > 1000) {
                    String[] lines = error.split("\n");
                    String toSend = "";
                    int i = 0;
                    
                    while (i < lines.length) {
                        if ((toSend + lines[i] + 1).length() > 1000) {
                            toSend += "...";
                            break;
                        }
                        
                        toSend += "\n" + lines[i];
                    }
                    
                    event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed(toSend, Color.RED)).queue();
                }
                else
                    event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed(error, Color.RED)).queue();
            }
        }).start();
    }

    /**
     * Check if the message is posted in a guild using a whitelist and if it contains a DLL
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

                if(checkIfStaff(event))
                    return true;

                return false; // The sender isn't allowed to send a DLL file
            }
        }
        return true; // No attachement, or no DLL
    }
    
    public static void addNewHelpedRecently(MessageReceivedEvent event) {
        for (int i = helpedRecently.size() - 1; i >= 0; --i)
            if (helpedRecently.get(i).time + helpDuration < Instant.now().getEpochSecond())
                helpedRecently.remove(i);
        
        helpedRecently.add(new HelpedRecentlyData(event.getMember().getIdLong(), event.getChannel().getIdLong()));
        System.out.println("Helped recently added");
    }
    
    public static boolean wasHelpedRecently(MessageReceivedEvent event) {
        for (int i = 0; i < helpedRecently.size(); ++i) {
            HelpedRecentlyData hrd = helpedRecently.get(i);
            if (hrd.channelid == event.getChannel().getIdLong() && hrd.userid == event.getMember().getIdLong() && hrd.time + helpDuration > Instant.now().getEpochSecond()) {
                helpedRecently.remove(i); // trigger only one message per log
                return true;
            }
        }
        return false;
    }
    /**
     * Check if sender is part of Guild Staff/Trusted
     * @param event
     * @return true if sender really was Guild Staff/Trusted
     */
    public static boolean checkIfStaff(MessageReceivedEvent event){
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

    private static boolean checkForFishing(MessageReceivedEvent event) {

        if (!GuildConfigurations.whitelistedRolesServers.containsKey(event.getGuild().getIdLong()))
            return false;

        if(checkIfStaff(event))
            return false;

        if (ArrayUtils.contains(GuildConfigurations.whitelistedRolesServers.get(event.getGuild().getIdLong()), event.getAuthor().getIdLong()))
            return false;

        // I found a simple referral and you can loot skins there\nhttp://csgocyber.ru/simlpebonus\nIf it's not difficult you can then throw me a trade and I'll give you the money
        //@everyone Hello I am leaving CS:GO and giving away my skins to people who send trade offers. For first people I will give away my 3 knifes. Don't be greedy and take few skins :  https://streancommunuty.ru/tradoffer/new/?partner=1284276379&token=iMDdLkoe
        String message = event.getMessage().getContentRaw().toLowerCase();
        int suspiciousValue = 0;
        suspiciousValue += message.contains("http") ? 1 : 0;
        suspiciousValue += message.contains("@everyone") ? 1 : 0;
        suspiciousValue += message.contains("money") ? 1 : 0;
        suspiciousValue += message.contains("loot") ? 1 : 0;
        suspiciousValue += message.contains("cs:go") || message.contains("csgo") ? 2 : 0;
        suspiciousValue += message.contains("trade") ? 2 : 0;
        suspiciousValue += message.contains("skins") ? 2 : 0;

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        while (handledMessages.peek() != null && handledMessages.peek().creationTime.until(now, ChronoUnit.SECONDS) > 60)
            handledMessages.remove();

        if (suspiciousValue < 2)
            suspiciousValue = 0;
        if (suspiciousValue > 3 && suspiciousValue < 7)
            suspiciousValue = 3;
        handledMessages.add(new HandledServerMessageContext(event, suspiciousValue)); // should avoid false-positives, force 2 messages

        List<HandledServerMessageContext> sameauthormessages = handledMessages.stream()
            .filter(m -> m.messageReceivedEvent.getMember().getIdLong() == event.getMember().getIdLong())
            .collect(Collectors.toList());

        int suspiciousCount = (int)sameauthormessages.stream().map(m -> m.suspiciousValue).reduce(0, Integer::sum);

        if (suspiciousCount > 4) {
            String usernameWithTag = event.getAuthor().getAsTag();
            String userId = event.getAuthor().getId();

            LogCounter.AddSSCounter(userId, message);

            event.getMember().ban(1, "Banned by Lum's Scam Shield").complete();

            EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor("Ban Report", null, "https://cdn.discordapp.com/avatars/275759980752273418/05d2f38ca37928426f7c49b191b8b552.webp")
                .setDescription("User **" + usernameWithTag + "** (*" + userId + "*) was Banned by the Scam Shield")
                .setTimestamp(Instant.now());
            
            String reportChannel = CommandManager.mlReportChannels.get(event.getGuild().getIdLong());
            if (reportChannel != null)
                event.getGuild().getTextChannelById(reportChannel).sendMessage(embedBuilder.build()).queue();
            else
                event.getChannel().sendMessage(embedBuilder.build()).queue();

            return true;
        }

        return false;
    }
}
