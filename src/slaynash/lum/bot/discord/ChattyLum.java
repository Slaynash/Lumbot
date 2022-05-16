package slaynash.lum.bot.discord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.commands.LumGoneCommand;
import slaynash.lum.bot.discord.utils.CrossServerUtils;

public class ChattyLum {
    public static final String LOG_IDENTIFIER = "ChattyLum";
    private static final Random random = new Random();
    private static final int helpDuration = 6 * 60; //in seconds
    private static final List<HelpedRecentlyData> helpedRecently = new ArrayList<>();

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

    private static final ArrayList<String> alreadyHelpedSentences = new ArrayList<>(Arrays.asList(
        "<:Neko_devil:865328473974374420>",
        "I already answered you <:konataCry:553690186022649858>",
        "I've already given you a response! <:MeguminEyes:852057834686119946>",
        "There's already the answer up here!! <:cirHappy:829458722634858496>",
        "Why won't you read my answer <:angry:835647632843866122>",
        "https://tenor.com/view/exultant-what-anime-gif-16881880",
        "https://tenor.com/view/obama-what-dafuq-gif-7447350",
        "https://tenor.com/view/cute-pouting-pout-anime-girl-gif-14739721"
    ));

    private static final ArrayList<String> alreadyHelpedSentencesRare = new ArrayList<>(Arrays.asList(
        "<https://letmegooglethat.com/?q=How+do+I+read>",
        "https://cdn.discordapp.com/attachments/657545944136417280/836231859998031932/unknown.png",
        "I wish I wasn't doing this job sometimes <:02Dead:835648208272883712>",
        "Your literacy skills test appears to have failed you. <:ram_disgusting:828070759070695425>"
    ));

    private static final ArrayList<String> thankedSentences = new ArrayList<>(Arrays.asList(
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
    ));

    private static final ArrayList<String> thankedSentencesRare = new ArrayList<>(Arrays.asList(
        "https://tenor.com/view/barrack-obama-youre-welcome-welcome-gif-12542858",
        "https://tenor.com/view/obama-pew-deal-with-it-finger-pistol-gif-18056062",
        "https://tenor.com/view/your-welcome-gif-23991412",
        "https://tenor.com/view/sparrow-welcome-pirate-pirates-of-the-caribbean-film-series-gif-17171224",
        "https://tenor.com/view/dance-party-dance-off-dance-moves-anime-dress-gif-13775278",
        "Notices you senpai <:cirHappy:829458722634858496>"
    ));

    private static final ArrayList<String> helloLum = new ArrayList<>(Arrays.asList(
        "<:necoNya:893316276850671657>",
        "<:Neko_cat:865328470569385995>",
        "<:Neko_cat_owo:851938214105186304>",
        "<:Neko_cat_shrug:851938033724817428>",
        "<:Neko_cat_uwu:851938142810275852>",
        "<:Neko_cat_wave:851938087353188372>",
        "<:Neko_gems:865409576353660938>",
        "<:Neko_hi:865328473667797012>",
        "<:Neko_music:865328471018307584>",
        "<:Neko_pounce:865328474142277662>"
    ));

    private static final ArrayList<String> niceLum = new ArrayList<>(Arrays.asList(
        "<:Neko_angel:865328472539791370>",
        "<:Neko_blush:865328474326695946>",
        "<:Neko_cash:865328474130350111>",
        "<:Neko_cat_donut:851936309718024203>",
        "<:Neko_cat_okay:851938634327916566>",
        "<:Neko_cat_pizza:851935753826205736>",
        "<:Neko_cat_royal:851935888178544660>",
        "<:Neko_cat_woah:851935805874110504>",
        "<:Neko_dab:865328473719439381>",
        "<:Neko_excited:865328473782091796>",
        "<:neko_hug:895078856313143366>",
        "<:Neko_Hype:865328473895469116>",
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
        "<a:Neko_cat_HappyCry:862453203441680404>",
        "<a:Neko_cat_HeadPat:851934772959510578>",
        "<a:Neko_pet:883168394625560587>"
    ));

    private static final ArrayList<String> badLum = new ArrayList<>(Arrays.asList(
        "<:baka:828070018935685130>",
        "<:Neko_ban:865328473789693952>",
        "<:Neko_cat_drool_stupid:851936505516785715>",
        "<:Neko_cat_fear:851936400819486720>",
        "<:Neko_cat_prison:851936449548255264>",
        "<:Neko_cool:865328469734719539>",
        "<:Neko_everythings_fine:865328471143088158>",
        "<:Neko_evil:865328473701744650>",
        "<:Neko_F:865328473895731231>",
        "<:Neko_hug_me:865328470367141909>",
        "<:Neko_ohno:865328474192216064>",
        "<:Neko_popcorn:865328471965696011>",
        "<:Neko_pout:865328471102324778>",
        "<:Neko_rear:865327612771565619>",
        "<:Neko_reee:865328473659539467>",
        "<:Neko_sad:865328470652485633>",
        "<:Neko_sip:865328472560500786>",
        "<:Neko_sweat:865328470702817306>",
        "<:Neko_what:865328474238353418>",
        "<:Neko_wondering:865328471492001833>",
        "https://c.tenor.com/k5tS3My-PKcAAAAC/steve-harvey-speechless.gif",
        "https://tenor.com/view/anime-blob-pout-mean-upset-gif-22834109"
    ));

    private static final ArrayList<String> gunLum = new ArrayList<>(Arrays.asList(
        "<:Neko_ban:865328473789693952>",
        "<:Neko_cat_Gun:851934721914175498>",
        "<:Neko_cop:865328472540971058>",
        "<:Neko_dead:865328473760595999>",
        "<:Neko_Gun_Big:883168394759766046>",
        "<:Neko_knife:865328473858113536>",
        "<:Neko_L:865328469557379116>",
        "<:Neko_LOL:865328469335867392>",
        "<:Neko_P:865328473786941461>",
        "<:PaimonShock:918946477663981568>",
        "<:Sayori_yes:828068735369478188>",
        "<:WhatAreYouDoingMan:828068981117943848>",
        "https://tenor.com/view/breathing-is-fun-stare-dead-inside-anime-kaguya-gif-19901746",
        "https://tenor.com/view/comic-girls-dying-suffering-crying-sob-gif-15759497"
    ));

    private static final ArrayList<String> wah = new ArrayList<>(Arrays.asList(
        "https://tenor.com/view/suisei-hololive-hoshimachi-hoshimachisuisei-wah-gif-21759617",
        "https://tenor.com/view/raiden-shogun-gif-23244583"
    ));

    private static final ArrayList<String> what = new ArrayList<>(Arrays.asList(
        "https://cdn.discordapp.com/attachments/680928395399266314/832204354584182834/videoplayback_2.mp4",
        "https://tenor.com/view/indian-dramatic-whut-soapopera-gif-8233002",
        "https://tenor.com/view/brule-what-ay-what-gif-14969459",
        "https://tenor.com/view/what-cat-what-cat-computer-gif-18924522",
        "https://tenor.com/view/cat-wtf-shiny-what-shinywhat-gif-24910441",
        "https://tenor.com/view/wait-what-gif-21319330",
        "https://tenor.com/view/what-repeat-confused-gif-17603995",
        "https://tenor.com/view/nick-young-question-marks-what-excuse-me-huh-gif-4486363",
        "https://tenor.com/view/huh-rabbit-cute-gif-15676652",
        "https://tenor.com/view/obama-wtf-why-president-wut-gif-12221156",
        "https://tenor.com/view/what-confused-persian-room-cat-guardian-gif-11044457",
        "https://tenor.com/view/anime-what-gif-24642265"
    ));

    static { //on class load, nothing is removing these when the season is over other then a reboot
        if (LocalDate.now().getMonthValue() == 10) { //halloween
            helloLum.add("<:Neko_mummy:865328473761775627>");
            helloLum.add("<:Neko_pumpkin:865328473962053683>");
            niceLum.add("<:Neko_present:865328471232348190>");
            niceLum.add("<:Neko_pumpkinlove:865328473441435649>");
            badLum.add("<:Neko_clown:865328473732415508>");
            badLum.add("<:Neko_devil:865328473974374420>");
        }
        else if (LocalDate.now().getMonthValue() == 12) { //christmas
            helloLum.add("<:Neko_padoru:865328474150797342>");
            helloLum.add("<:Neko_snow:865328474196541440>");
            niceLum.add("<:Neko_padoru:865328474150797342>");
            niceLum.add("<:Neko_snow:865328474196541440>");
        }
    }

    public static boolean handle(String message, MessageReceivedEvent event) {
        //message is lowercased and unidecode
        if (
            handleThanks(message, event) ||
            handleHelp(message, event))
            return true;

        if (message.contains("padoru")) {
            event.getMessage().reply("https://cdn.discordapp.com/attachments/657545944136417280/920891104281374780/Gura_Padoru.mp4").queue();
            return true;
        }
        if (message.equals("nya")) {
            event.getMessage().reply("https://cdn.discordapp.com/attachments/657545944136417280/920902875364864090/nya.mp4").queue();
            return true;
        }
        if (message.replace(" ", "").contains("welcometovrchat")) {
            event.getMessage().reply("https://cdn.discordapp.com/attachments/757187539638550610/932623632746836018/WELCOME_TO_VRCHAT_-_TFMJonny_Bo_Burnham_Parody.mp4").queue();
            return true;
        }
        if (message.matches("^wah\\b.*")) {
            event.getChannel().sendMessage(wah.get(random.nextInt(wah.size()))).queue();
            return true;
        }
        if (message.replace(" ", "").contains("ohno") && message.contains("anyway")) {
            event.getChannel().sendMessage("https://tenor.com/view/oh-no-oh-no-anyway-gif-18887547").queue();
            return true;
        }

        boolean hasLum = message.matches(".*\\blum\\b.*");
        boolean refLum = event.getMessage().getReferencedMessage() != null && event.getMessage().getReferencedMessage().getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong();
        if (!(hasLum || refLum))
            return false;

        if (message.contains("obese")) {
            event.getMessage().reply("<:Neko_sad:865328470652485633>").queue();
            return true;
        }

        if (CrossServerUtils.isLumDev(event.getMember()) && message.matches(".*\\bdab\\b.*")) {
            event.getChannel().sendMessage("<:Neko_dab:865328473719439381>").queue();
            return true;
        }
        if (message.contains("dab dab")) {
            event.getChannel().sendMessage("<:Neko_dab:865328473719439381>").queue();
            return true;
        }

        if (message.matches(".*\\bbye\\b.*")) {
            if (!(new LumGoneCommand().includeInHelp(event)))
                event.getChannel().sendMessage("<:Neko_sleep:865328470425862175>").queue();
            return true;
        }

        if (message.matches(".*\\bboop\\b.*")) {
            if (!(new LumGoneCommand().includeInHelp(event)))
                event.getChannel().sendMessage("<a:CuteFoxBoop:942576777509888072>").queue();
            return true;
        }

        if (message.matches(".*\\b(g(oo|u)d|best|nice|great(|est)|(be)?love(d|ly)?|nerd|s(hm|)exy|hugs?|beautiful|cool|cuti?e(st)?|adorable|amaz(e|ing)|helped|thanks*|p([ea])ts*|dab)\\b.*")) {
            System.out.println("Nice Lum was detected");
            event.getChannel().sendMessage(niceLum.get(random.nextInt(niceLum.size()))).queue();
            return true;
        }

        if (message.matches(".*\\b(off|fuck(ing)?|stfu|kill|murder|gun|knife|horny)\\b.*")) {
            System.out.println("F off Lum was detected");
            event.getChannel().sendMessage(gunLum.get(random.nextInt(gunLum.size()))).queue();
            return true;
        }

        if (message.matches(".*\\b(bad|shu(t|sh)|smh|hush|stupid|dumb?|baka|gay)\\b.*")) {
            System.out.println("Bad Lum was detected");
            event.getChannel().sendMessage(badLum.get(random.nextInt(badLum.size()))).queue();
            return true;
        }

        if (message.matches(".*\\b(hello|hi|hey)\\b.*")) {
            System.out.println("Hello Lum was detected");
            event.getChannel().sendMessage(helloLum.get(random.nextInt(helloLum.size()))).queue();
            return true;
        }

        if (message.matches(".*\\b(credit|stole|steal(ing)?)\\b.*")) {
            System.out.println("Lum stole Credit");
            event.getChannel().sendMessage("<:Hehe:792738744057724949>").queue();
            return true;
        }

        if (message.matches(".*\\b(poor)\\b.*")) {
            System.out.println("Poor Lum");
            event.getChannel().sendMessage("<:Nyan_sigh:740365203694420068>").queue();
            return true;
        }
        if (message.matches(".*\\b(berry)\\b.*")) {
            System.out.println("Lum Berry");
            event.getChannel().sendMessage("https://cdn.discordapp.com/attachments/915063138779553872/943708494245290004/unknown.png").queue();
            return true;
        }
        if (message.matches(".*\\b(world)\\b.*")) {
            System.out.println("Lum Rules the world");
            event.getChannel().sendMessage("<:Neko_Rule_the_World:949548832302194718>").queue();
            return true;
        }
        if (message.matches(".*\\b(cookie)\\b.*")) {
            System.out.println("Lum Cookie");
            event.getMessage().addReaction("🍪").queue();
            return true;
        }
        if (message.matches(".*\\b(handcuf).*")) {
            System.out.println("Lum handcuff");
            event.getChannel().sendMessage("<:Neko_cop:865328472540971058>").queue();
            return true;
        }
        if (message.matches(".*install vrc(|hat) sdk.*")) {
            System.out.println("Lum VRCSDK");
            event.getChannel().sendMessage("https://cdn.discordapp.com/attachments/773300021117321248/972746728136671232/How_to_install_VRC_SDK.mp4").queue();
            return true;
        }
        if (message.matches(".*\\b(looking for a girl).*")) {
            System.out.println("looking for a girl");
            event.getChannel().sendMessage("https://cdn.discordapp.com/attachments/773300021117321248/975632183949688942/looking_for_a_girl.mp4").queue();
            return true;
        }

        //must be last
        if (message.matches(".*\\b(what)\\b.*")) {
            System.out.println("Lum What?");
            event.getChannel().sendMessage(what.get(random.nextInt(what.size()))).queue();
            return true;
        }

        return false;
    }

    private static boolean handleThanks(String message, MessageReceivedEvent event) {
        if (message.matches(".*\\b(th(ank|x)|neat|cool|nice|(?<!(not|n'?t|get) )help|epic|work(s|ed)|ty(|sm)|fixed|rad.*|th(at|is) bot|awesome|wow)\\b.*")) {
            if (event.getMessage().getReferencedMessage() == null && (event.getMessage().getMentionedUsers().size() == 0 || event.getMessage().getMentionedUsers().get(0).getName().equals("Lum")) && wasHelpedRecently(event) || message.matches(".*\\blum\\b.*")) {
                System.out.println("Thanks was detected");
                String sentence;
                boolean rare = random.nextInt(100) == 69;
                if (rare)
                    sentence = "You're Welcome, but thank <@145556654241349632> and <@240701606977470464> instead for making me. <a:HoloPet:829485119664160828>";
                else {
                    rare = random.nextInt(10) == 9;
                    sentence = rare
                        ? thankedSentencesRare.get(random.nextInt(thankedSentencesRare.size()))
                        : thankedSentences.    get(random.nextInt(thankedSentences.size()));
                }
                event.getChannel().sendMessage(sentence).allowedMentions(Collections.emptyList()).queue();
                return true;
            }
        }

        return false;
    }

    private static boolean handleHelp(String message, MessageReceivedEvent event) {
        if (message.matches(".*\\b(help|fix|what (do|should))\\b.*")) {
            if (wasHelpedRecently(event) && event.getMessage().getReferencedMessage() == null && (event.getMessage().getMentionedUsers().size() == 0 || event.getMessage().getMentionedUsers().get(0).getName().equals("Lum"))) {
                System.out.println("Help was detected");
                String sentence;
                boolean rare = random.nextInt(1000) == 420;
                if (rare)
                    sentence = "Shut the fuck up, I literally answered your dumb ass!";
                else {
                    rare = random.nextInt(10) == 9;
                    sentence = rare
                        ? alreadyHelpedSentencesRare.get(random.nextInt(alreadyHelpedSentencesRare.size()))
                        : alreadyHelpedSentences.    get(random.nextInt(alreadyHelpedSentences.size()));
                }
                event.getChannel().sendMessage(sentence).queue();
                return true;
            }
        }

        return false;
    }

    public static void addNewHelpedRecently(MessageReceivedEvent event) {
        helpedRecently.removeIf(h -> h.userid == event.getMember().getIdLong()); //remove all past log from user
        helpedRecently.removeIf(h -> h.time + helpDuration < Instant.now().getEpochSecond());
        helpedRecently.add(new HelpedRecentlyData(event.getMember().getIdLong(), event.getChannel().getIdLong()));
        // System.out.println("Helped recently added");
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
