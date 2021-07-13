package slaynash.lum.bot.discord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ChattyLum {

    private static class HelpedRecentlyData {
        public long time;
        public long userid;
        public long channelid;

        public HelpedRecentlyData(long userid, long channelid) {
            time = Instant.now().getEpochSecond();
            this.userid = userid;
            this.channelid = channelid;
        }
    }

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
        "<:Neko_cat_Gun:851934721914175498>",
        "https://tenor.com/view/comic-girls-dying-suffering-crying-sob-gif-15759497",
        "https://tenor.com/view/breathing-is-fun-stare-dead-inside-anime-kaguya-gif-19901746"
    };


    private static Random random = new Random();

    private static final int helpDuration = 6 * 60; //in seconds
    private static List<HelpedRecentlyData> helpedRecently = new ArrayList<>();


    public static boolean handle(String message, MessageReceivedEvent event) {
        boolean hasLum = message.matches(".*\\blum\\b.*");
        if (
            handleThanks(message, event) ||
            handleHelp(message, event))
            return true;

        if (!hasLum)
            return false;

        if (message.matches(".*\\b(good|nice|love(ly)?|cool|cuti?e(st)?|adorable|helped|thank(s)*|(head)?p(e|a)t)\\b.*")) {
            System.out.println("Nice Lum was detected");
            event.getChannel().sendMessage(niceLum[random.nextInt(niceLum.length)]).queue();
            return true;
        }

        if (message.matches(".*\\b(off|fuck|stfu|kill|gun)\\b.*")) {
            System.out.println("F off Lum was detected");
            event.getChannel().sendMessage(gunLum[random.nextInt(gunLum.length)]).queue();
            return true;
        }

        if (message.matches(".*\\b(bad|shu(t|sh)|smh|hush)\\b.*")) {
            System.out.println("Bad Lum was detected");
            event.getChannel().sendMessage(badLum[random.nextInt(badLum.length)]).queue();
            return true;
        }

        if (message.matches(".*\\b(hello|hi)\\b.*")) {
            System.out.println("Hello Lum was detected");
            event.getChannel().sendMessage(helloLum[random.nextInt(helloLum.length)]).queue();
            return true;
        }

        if (message.matches(".*\\b(credit|stole|steal(ing)?)\\b.*")) {
            System.out.println("Lum stole Credit");
            event.getChannel().sendMessage("<:Hehe:792738744057724949>").queue();
            return true;
        }

        return false;
    }

    private static boolean handleThanks(String message, MessageReceivedEvent event) {
        if (message.contains("thank") || message.contains("thx") || message.contains("neat") || message.contains("cool") || message.contains("nice") ||
            message.contains("helpful") || message.contains("epic") || message.contains("worked") || message.contains("tysm") || message.equals("ty") ||
            message.contains(" ty ") || message.contains("fixed") || message.matches("(^|.*\\s)rad(.*)") || message.contains("that bot") ||
            message.contains("this bot") || message.contains("awesome") || message.contains(" wow ")
        ) {
            if (wasHelpedRecently(event) && event.getMessage().getReferencedMessage() == null && (event.getMessage().getMentionedUsers().size() == 0 || event.getMessage().getMentionedUsers().get(0).getName() == "Lum")) {
                System.out.println("Thanks was detected");
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
                return true;
            }
        }

        return false;
    }

    private static boolean handleHelp(String message, MessageReceivedEvent event) {
        if (message.contains("help") && !message.contains("helping") || message.contains("fix") || message.contains("what do "/*i do*/) || message.contains("what should "/*i do*/)) {
            if (wasHelpedRecently(event) && event.getMessage().getReferencedMessage() == null && (event.getMessage().getMentionedUsers().size() == 0 || event.getMessage().getMentionedUsers().get(0).getName() == "Lum")) {
                System.out.println("Help was detected");
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
                return true;
            }
        }

        return false;
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
}
