package slaynash.lum.bot.discord.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.GuildConfigurations;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;

public final class CrossServerUtils {

    public static String sanitizeInputString(String input) {
        if (input == null) input = "";

        input = input
        .replace("](", " ")
        .replaceAll("^(?!<)@", "@ ")
        .replace("*", "\\*")
        .replace("`", "\\`");

        input = Pattern.compile("(nigg(er|a)|porn|penis)", Pattern.CASE_INSENSITIVE).matcher(input).replaceAll(Matcher.quoteReplacement("{REDACTED}"));
        input = Pattern.compile("(https?://)(www.)?([a-z\\d]+\\.)+[a-z]{2,63}(/[a-zA-Z\\d#.]+/?)*", Pattern.CASE_INSENSITIVE).matcher(input).replaceAll(Matcher.quoteReplacement("{REDACTED}"));

        input = input.substring(0, Math.min(input.length(), 50)); // limit inputs to 50 chars

        return input;
    }

    public static boolean testSlurs(String word) {
        Pattern patternNig = Pattern.compile("(([nП𝝥𝗇пոŊႶՈΠ∏𝗻∩ᑎηꞂⁿ𝝶ƞ⋂րחᴨ𝐧ₙȠɳᥒŋɲﬨ𝚗𝞟𝙣ռ𝘯ת𝞰Ռ𝒏𝑛𐌿ղ𐍀Ⲡ𝖓𝔫იᥥꞑᥰῃ𐒐ņԉԥᑙṇᑏᑚṉ𝓷𝝒Ԉ𝟆ṅṋŉդ𝛈በתּ𝜼กກቢｎቡNɴ𝝢𝖭𝗡ΝƝ𝘕ꞐᶰᴺⲚ𝚴𝙽𝐍𝞜𐌽𝙉ⲛ𝜨𝛮𝑵𝑁ṆℕŃИ𝓝ℿŅṊÑŇṄＮñń𝓃𝕟𝒩𝔑𝕹ꓠ𐔓𐆎̊𝜂ᵰͷи𐑍ᴎ𝔶𝝿𝞹ℼ𝛑𝜋𝚷𝜫ΩΩ𝛀𝛺𝜴𝝮𝞨ᘯᘵ𐊶ꓵᑛ⒩🄝⨅][\\s.,;:+=|^*`´'\"?!<>@#$%&\\[\\]{}()\\\\/_~-]*)+([ÍĪіiÎİ𝗂ⅰ𝐢ǏᎥᵢꜟÌꜞīį𝖏ĬוֹɨῙ𝗶ⁱ𝔦ᴉ𝔧ῑỈ𝖎ﺃ𝒊𝚒𝙞ἰأἱίĨﺄὶῘĺ𝑖îῐỉ𝘪ĭíìǐί𝒾𝓲ⅈȊﺂıἸ讠༏ΐï┃IΙ▎𝐥𝐈Іɪ𝖨ⵊ𝚰▏l❙ⵏǀꕯ𝘐▕ߊꟾꞁ|𝜤𝙸ӀⅠⅼا𝑰ﺍ⎮𐌠𝗜𐌉Ⲓ׀╿ן𝝞ӏ╽𝍩𝙄⎜❘⎢│𐌹𝙡𝞘エ∣𝘭𝐼⌶𝛪ⲓ𐒃ו/𝗹৷౹꘡ェ𝕀𐤖⟧𝓘𝗅।丨།工ㅣ୲ŀ∤ľ忄⍳ｉ𝕚𝚤ɩι𝛊𝜄𝜾𝝸𝞲ꙇꭵ𑣃ⓛⒾ⍸ᵻᵼⅱⅲ1!⒤⒥⑴🄘][\\s.,;:+=|^*`´'\"?!<>@#$%&\\[\\]{}()\\\\/_~-]*)+([𝗀qɡgց𝗴𝐠𝘨𝒈ᶢᵍ𝑔𝙜ǥՑ𝚐გ𝖌𝔤𝓰ģġဌｇG𝖦𝘎ɢǤ𝗚ԌᏀႺᴳ𝙶𝙂Ꮆԍ𝐆𝑮Ɠ𝐺Ᏻ₲𝔾𝓖ʛĢĠĞℊ𝕘ᶃƍ𝒢𝔊𝕲ꓖɠ̔ǧğǦǵꮐᏻ69⒢🄖∂𝛛𝜕𝝏𝞉𝟃𝟔𝟞𝟨𝟲𝟼🯶ⳒбᏮ𑣕੧୨৭൭𝟗𝟡𝟫𝟵𝟿🯹ꝮⳊ𑣌𑢬𑣖१𑣤۹][\\s.,;:+=|^*`´'\"?!<>@#$%&\\[\\]{}()\\\\/_~-]*){2,}([е𝖾ₑe𝘦ᵉ𝐞𝗲ℯ𝙚𝚎ӘəҼɘ𝟈ęᥱ𝒆𝑒℮ǝә𝓮ᕪᕦ𝖊巳ల已୧ലēéΕEЕ𝖤ⴹ𝗘Ꭼⵟ𝘌ᴱᴇ𝝚ꗋƐ⋿ℇĘꜪЄɛ𝙴Ȩεᗴ𝙀𝚬𝐸𝐄ϵ𝞔ꜫ𝛦𝑬𝜠𝔼Ԑє𝝴𝞮𝞊Ǝ𝟄𝜀𝜺ℰ𝛆𝓔∈𝝐ԑ∃ⵇ𝛜ÉᏋĒ𝞢ĖÈ𝝽ᙓÊĔƩЀẸ𐒢ᄐᇀદㅌ트ėêèëæӕӔÆ⋴ꞓ𝜖ⲉꮛ𑣎𐐩€Ⲉ⍷ꞒͤＥ𝔈𝕰ꓰ𑢦𑢮𐊆ěĚɇɆҿꭼⴺꓱƏ𝈡𖼭𐐁ᶟᵋᴈзҙ∑⅀Σ𝚺𝛴𝜮𝝨ⵉʒꝫⳍӡჳ𝛏𝜉ξ𝝃𝞷3⒠🄔𝟑𝟛𝟥𝟯𝟹🯳ꞫȜ𝈆ꝪⳌЗӠ𖼻𑣊౯][\\s.,;:+=|^*`´'\"?!<>@#$%&\\[\\]{}()\\\\/_~-]*)+([┎┏𝝘𝗿r𝗋⸢г┍ᵣʳГɼΓ┌𝞒Ꞅᒥ𝐫𝚪ᴦ𝘳Ꮁᒋɾ꜒ᣘ𝚛𝙧𝜞⸀ґɽᣴⲄ𝛤ͬ𝒓𝑟ṛⲅꞃ𝖗٢𝓻۲ŗЃꞅ୮「ŕR𝗥𝖱ʀ𝙍Ɍ𝘙ᎡⱤ𝐑ƦᴿᏒ𝚁ᖇ𝑅℟𝑹ℛℝ𝓡℞🝈Ŕℜ𝓇𝔯𝕣ꭇꭈꮁ𝈖𝕽𐒴ꓣ𖼵ɍғᵲꭱꮢяᵳ𝛕𝜏𝝉𝞃𝞽𝚼𝛶𝜰𝞤ℾҒҐᒮᒔᒖᒌᒰᒦ⒭🄡Ⓡ®][\\s.,;:+=|^*`´'\"?!<>@#$%&\\[\\]{}()\\\\/_~-]*))");
        return patternNig.matcher(word).find();
    }

    /**
     * Check if sender is part of Guild Staff/Trusted.
     * @param event MessageReceivedEvent
     * @return true if sender really was Guild Staff/Trusted
     */
    public static boolean checkIfStaff(MessageReceivedEvent event) {
        for (Entry<Long, long[]> whitelistedRolesServer : GuildConfigurations.whitelistedRolesServers.entrySet()) {
            Guild targetGuild;
            Member serverMember;
            if ((targetGuild = event.getJDA().getGuildById(whitelistedRolesServer.getKey())) != null &&
                (serverMember = targetGuild.getMember(event.getAuthor())) != null)
            {
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
        if (event.getMember() == null) //https://discord.com/channels/633588473433030666/851519891965345845/883320272982278174
            return false;
        return event.getMember().hasPermission(Permission.ADMINISTRATOR) || event.getMember().hasPermission(Permission.MESSAGE_MANAGE) || event.getMember().hasPermission(Permission.BAN_MEMBERS) || event.getMember().hasPermission(Permission.KICK_MEMBERS);
    }

    public static boolean isLumDev(Member member) {
        return isLumDev(member.getId());
    }

    public static boolean isLumDev(String userId) {
        return
            "145556654241349632".equals(userId) || // Slaynash
            "240701606977470464".equals(userId); // Rakosi
    }

    private static int lastGuildCount;
    public static void loadGuildCount() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("storage/guildcount.txt"));
            lastGuildCount = Integer.parseInt(reader.readLine());
            reader.close();
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to load Guild Count", e);
        }
    }
    private static void saveGuildCount(int count) {
        lastGuildCount = count;
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("storage/guildcount.txt"))) {
            writer.write(String.valueOf(count));
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to save Guild Count", e);
        }
    }
    public static void checkGuildCount(GuildJoinEvent event) {
        int guildSize = JDAManager.getJDA().getGuilds().size();
        System.out.println("Joined " + event.getGuild().getName() + ", connected to " + guildSize + " guilds");
        if (guildSize % 25 == 0 && guildSize != lastGuildCount) {
            event.getJDA().getTextChannelById(876466104036393060L).sendMessage("I joined my " + guildSize + "th guild <:Neko_cat_woah:851935805874110504>").queue();
            saveGuildCount(guildSize);
        }
    }
}
