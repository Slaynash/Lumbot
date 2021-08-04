package slaynash.lum.bot.discord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ChattyLum {

    private static class HelpedRecentlyData {
        public final long time;
        public final long userid;
        public final long channelid;

        public HelpedRecentlyData(long userid, long channelid) {
            time = Instant.now().getEpochSecond();
            this.userid = userid;
            this.channelid = channelid;
        }
    }

    private static final String[] alreadyHelpedSentences = new String[] {
        "<:Neko_devil:865328473974374420>",
        "I already answered you <:konataCry:553690186022649858>",
        "I've already given you a response! <:MeguminEyes:852057834686119946>",
        "There's already the answer up here!! <:cirHappy:829458722634858496>",
        "Why won't you read my answer <:angry:835647632843866122>"
    };

    private static final String[] alreadyHelpedSentencesRare = new String[] {
        "<https://lmgtfy.app/?q=How+do+I+read>",
        "https://cdn.discordapp.com/attachments/657545944136417280/836231859998031932/unknown.png",
        "I wish I wasn't doing this job sometimes <:02Dead:835648208272883712>",
        "Your literacy skills test appears to have failed you. <:ram_disgusting:828070759070695425>"
    };

    private static final String[] thankedSentences = new String[] {
        "<:cirHappy:829458722634858496>",
        "<:Neko_lick:865328473646825513>",
        "<:Neko_think:865328471058939947>",
        "Always happy to help!",
        "Anytime <:Neko_cat_kiss_heart:851934821080367134>",
        "Anytime <:Neko_love:865328470699147274>",
        "Glad I could help!",
        "Mhm of course!",
        "No problem!",
        "You're Welcome <:Neko_cat_kiss_heart:851934821080367134>"
    };

    private static final String[] thankedSentencesRare = new String[] {
        "https://tenor.com/view/barrack-obama-youre-welcome-welcome-gif-12542858",
        "Notices you senpai <:cirHappy:829458722634858496>"
    };

    private static final String[] helloLum = new String[] {
        "<:Neko_cat_owo:851938214105186304>",
        "<:Neko_cat_shrug:851938033724817428>",
        "<:Neko_cat_uwu:851938142810275852>",
        "<:Neko_cat_wave:851938087353188372>",
        "<:Neko_hi:865328473667797012>"
    };

    private static final String[] niceLum = new String[] {
        "<:anime_okay:828069077875556382>",
        "<:Neko_angel:865328472539791370>",
        "<:Neko_blush:865328474326695946>",
        "<:Neko_cash:865328474130350111>",
        "<:Neko_cat_donut:851936309718024203>",
        "<:Neko_cat_okay:851938634327916566>",
        "<:Neko_cat_pizza:851935753826205736>",
        "<:Neko_cat_royal:851935888178544660>",
        "<:Neko_cat_woah:851935805874110504>",
        "<:Neko_dab:865328473719439381>",
        "<:Neko_lick:865328473646825513>",
        "<:Neko_love:865328470699147274>",
        "<:Neko_shy:865328471294083152>",
        "<:Neko_smug:865328473751814174>",
        "<:Neko_snuggle:865328474095878158>",
        "<:Neko_TeHe:865328470685909033>",
        "<:Neko_yay:865328469458288661>",
        "<:SagiriHeadEmpty:828070253337509918>",
        "<:Sagiri_Shy:828073525214183437>",
        "<a:HeartCat:828087151232286749>",
        "<a:Neko_cat_HeadPat:851934772959510578>"
    };

    private static final String[] badLum = new String[] {
        "<:baka:828070018935685130>",
        "<:Neko_ban:865328473789693952>",
        "<:Neko_cat_drool_stupid:851936505516785715>",
        "<:Neko_cat_fear:851936400819486720>",
        "<:Neko_cat_prison:851936449548255264>",
        "<:Neko_cool:865328469734719539>",
        "<:Neko_everythings_fine:865328471143088158>",
        "<:Neko_hug_me:865328470367141909>",
        "<:Neko_ohno:865328474192216064>",
        "<:Neko_pout:865328471102324778>",
        "<:Neko_rear:865327612771565619>",
        "<:Neko_reee:865328473659539467>",
        "<:Neko_sad:865328470652485633>",
        "<:Neko_sip:865328472560500786>",
        "<:Neko_sweat:865328470702817306>",
        "<:Neko_what:865328474238353418>",
        "<:Neko_wondering:865328471492001833>"
    };

    private static final String[] gunLum = new String[] {
        "<:Neko_ban:865328473789693952>",
        "<:Neko_cat_Gun:851934721914175498>",
        "<:Neko_cop:865328472540971058>",
        "<:Neko_dead:865328473760595999>",
        "<:Neko_knife:865328473858113536>",
        "<:Neko_L:865328469557379116>",
        "<:Neko_LOL:865328469335867392>",
        "<:Neko_P:865328473786941461>",
        "<:PaimonShock:828067937646018561>",
        "<:Sayori_yes:828068735369478188>",
        "<:WhatAreYouDoingMan:828068981117943848>",
        "https://tenor.com/view/breathing-is-fun-stare-dead-inside-anime-kaguya-gif-19901746",
        "https://tenor.com/view/comic-girls-dying-suffering-crying-sob-gif-15759497"
    };


    private static final Random random = new Random();

    private static final int helpDuration = 6 * 60; //in seconds
    private static final List<HelpedRecentlyData> helpedRecently = new ArrayList<>();


    public static boolean handle(String message, MessageReceivedEvent event) {
        boolean hasLum = message.matches(".*\\blum\\b.*");
        if (
            handleThanks(message, event) ||
            handleHelp(message, event))
            return true;

        if (!hasLum)
            return false;

        if (message.matches(".*\\b(good|nice|love(ly)?|cool|cuti?e(st)?|adorable|helped|thank(s)*|(head)?p([ea])t)\\b.*")) {
            System.out.println("Nice Lum was detected");
            event.getChannel().sendMessage(niceLum[random.nextInt(niceLum.length)]).queue();
            return true;
        }

        if (message.matches(".*\\b(off|fuck(ing)?|stfu|kill|gun)\\b.*")) {
            System.out.println("F off Lum was detected");
            event.getChannel().sendMessage(gunLum[random.nextInt(gunLum.length)]).queue();
            return true;
        }

        if (message.matches(".*\\b(bad|shu(t|sh)|smh|hush|stupid)\\b.*")) {
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
