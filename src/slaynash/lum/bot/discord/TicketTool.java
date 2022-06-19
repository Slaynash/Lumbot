package slaynash.lum.bot.discord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.Main;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class TicketTool {
    public static final String LOG_IDENTIFIER = "TicketTool";
    private static final Random random = new Random();

    public static void tickettool(MessageReceivedEvent event) {
        long category = event.getMessage().getCategory() == null ? 0L : event.getMessage().getCategory().getIdLong();
        String channelName = event.getTextChannel().getName();
        if (category != 765058331345420298L /*emmVRC Tickets*/ && category != 899140251241570344L /*emmVRC Tickets Claimed*/ && category != 952713158533971968L /*TW*/ || event.getChannel().getIdLong() == 801679570863783937L/*testing*/)
            return;
        if ((event.getAuthor().getIdLong() == 722196398635745312L /*tickettool*/ || event.getAuthor().getIdLong() == 557628352828014614L /*free tickettool*/) && event.getMessage().getContentDisplay().startsWith("Welcome")) {
            if (event.getGuild().getIdLong() == 600298024425619456L /* emmVRC */) {
                //The code needs to be the first ` in message
                if (channelName.contains("reset")){
                    event.getTextChannel().sendMessage(DBConnectionManagerLum.getString("strings", "string", "value", "emmTTmessage").replace("$randomString$", randomString(8))).queue();
                    try {
                        DBConnectionManagerLum.sendUpdate("INSERT INTO `TicketTool`(`ChannelName`, `ChannelID`, `UserID`, `Created`) VALUES (?,?,?,?)", event.getTextChannel().getName(), event.getTextChannel().getIdLong(), event.getMessage().getMentionedUsers().get(0).getIdLong(), System.currentTimeMillis());
                    } catch (SQLException e) {
                        ExceptionUtils.reportException("Failed to create TT autoclose", e);
                    }
                }
                else if (channelName.contains("wipe"))
                    event.getTextChannel().sendMessage(DBConnectionManagerLum.getString("strings", "string", "value", "emmTTmessage").replace("$randomString$", randomString(8))).queue();
                else if (channelName.contains("deletion"))
                    event.getTextChannel().sendMessage(DBConnectionManagerLum.getString("strings", "string", "value", "emmTTmessage").replace("$randomString$", randomString(8))).queue();
                else if (channelName.contains("export"))
                    event.getTextChannel().sendMessage(DBConnectionManagerLum.getString("strings", "string", "value", "emmTTexport").replace("$randomString$", randomString(8))).queue();
            }
            else if (event.getGuild().getIdLong() == 716536783621587004L /* TW */) {
                //The code needs to be the first ` in message
                event.getTextChannel().sendMessage(DBConnectionManagerLum.getString("strings", "string", "value", "twTTmessage").replace("$randomString$", randomString(8))).queue();
            }
        }
        else if ((event.getAuthor().getIdLong() == 886944444107063347L /*Rubybot*/ || event.getAuthor().getIdLong() == 150562159196241920L /*Karren-sama*/) && event.getMessage().getEmbeds().size() > 0) {
            Thread thread = new Thread(() -> {
                System.out.println("Receved embed from Rubybot");
                List<Message> history = new ArrayList<>(event.getTextChannel().getHistoryFromBeginning(100).complete().getRetrievedHistory());
                // Role emmadmin = event.getJDA().getGuildById(600298024425619456L).getRoleById(748392927365169233L);
                // Role emmnetwork = event.getJDA().getGuildById(600298024425619456L).getRoleById(801670419723452487L);
                // history.removeIf(m -> !m.getAuthor().equals(m.getJDA().getSelfUser()) && !m.getMember().getRoles().contains(emmadmin) && !m.getMember().getRoles().contains(emmnetwork)); had a member is null error
                history.removeIf(m -> !m.getContentRaw().toLowerCase().contains("status"));
                if (history.size() == 0) {
                    System.out.println("[ERROR] Can not find my messages");
                    return;
                }
                String[] split = history.get(history.size() - 1).getContentRaw().split("`");
                if (split.length < 2) {
                    System.out.println("[ERROR] Can not find my pin in ticket");
                    return;
                }
                String code = split[1].toLowerCase();
                String codeNotFound = "`" + code + "` was not found. If this is your account, please make sure to save it in your bio or status.";
                List<Field> embedFields = event.getMessage().getEmbeds().get(0).getFields();
                Field idField = null;
                for (Field field : embedFields) {
                    if (field.getName().equalsIgnoreCase("ID") || field.getName().equalsIgnoreCase("UserID")) {
                        idField = field;
                        break;
                    }
                }
                if (idField == null) {
                    System.out.println("[ERROR] Can not find ID field");
                    return;
                }
                boolean codeFound = checkForCode(embedFields, code);
                String id = idField.getValue().replace("`", "");
                System.out.println("Code: " + code + " ID:" + id + " CodeFound: " + codeFound);

                if (event.getGuild().getIdLong() == 600298024425619456L /* emmVRC */) {
                    if (channelName.contains("reset")) {
                        if (codeFound)
                            event.getTextChannel().sendMessage("e.pin reset " + id).queue();
                        else
                            event.getTextChannel().sendMessage(codeNotFound).queue();
                    }
                    else if (channelName.contains("wipe")) {
                        if (codeFound)
                            event.getTextChannel().sendMessage(DBConnectionManagerLum.getString("strings", "string", "value", "emmTTwipecomplete")).queue();
                        else
                            event.getTextChannel().sendMessage(codeNotFound).queue();
                    }
                    else if (channelName.contains("deletion")) {
                        if (codeFound)
                            event.getTextChannel().sendMessage(DBConnectionManagerLum.getString("strings", "string", "value", "emmTTdeletecomplete")).queue();
                        else
                            event.getTextChannel().sendMessage(codeNotFound).queue();
                    }
                    else if (channelName.contains("export")) {
                        if (codeFound)
                            event.getTextChannel().sendMessage(DBConnectionManagerLum.getString("strings", "string", "value", "emmTTexportcomplete")).queue();
                        else
                            event.getTextChannel().sendMessage(codeNotFound).queue();
                    }
                }
                else if (event.getGuild().getIdLong() == 716536783621587004L /* TW */ && codeFound) {
                    if (codeFound)
                        event.getTextChannel().sendMessage("tw!deluser " + id).queue();
                    else
                        event.getTextChannel().sendMessage(codeNotFound).queue();
                }
                if(codeFound) {
                    try {
                        DBConnectionManagerLum.sendUpdate("UPDATE `TicketTool` SET `Completed`=? WHERE `ChannelID`=?", System.currentTimeMillis(), event.getTextChannel().getIdLong());
                    } catch (SQLException e) {
                        ExceptionUtils.reportException("Failed to handle TT completed TS", e);
                    }
                }
            }, "Ticket");
            thread.start();
        }
    }

    public static void start() {
        if (JDAManager.getJDA().getSelfUser().getIdLong() != 275759980752273418L)
            return;
        Thread thread = new Thread(() -> {
            while (!Main.isShuttingDown) {
                try {
                    ResultSet rs = DBConnectionManagerLum.sendRequest("CALL `TicketsToClose`(?)", System.currentTimeMillis());
                    while (rs.next()) {
                        long ukey = rs.getLong("ukey");
                        long channelID = rs.getLong("ChannelID");
                        // long userID = rs.getLong("UserID");
                        // long created = rs.getLong("Created");
                        TextChannel channel = JDAManager.getJDA().getTextChannelById(channelID);
                        if (channel == null) {
                            DBConnectionManagerLum.sendUpdate("DELETE FROM `TicketTool` WHERE `ukey`=?", ukey); // Was deleted by not Lum
                            continue;
                        }
                        if (channel.getParent() != null && channel.getParent().getIdLong() == 765058331345420298L) {
                            channel.sendMessage("$close").queue();
                            DBConnectionManagerLum.sendUpdate("DELETE FROM `TicketTool` WHERE `ukey`=?", ukey);
                        }
                    }
                    Thread.sleep(5 * 1000); // sleep for 5 seconds
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("Failed to handle TT autoclose", e);
                }
            }
        }, "TTThread");
        thread.setDaemon(false);
        thread.start();
    }

    private static final String AB = "23456789abcdefghijkmnopqrstuvwxyz";
    private static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(random.nextInt(AB.length())));
        String sbr = sb.toString();
        String sbrt = sbr.replace("4", "a").replace("8", "b").replace("3", "e").replace("9", "q")
                .replace("2", "r").replace("5", "s").replace("7", "t").replace("vv", "w");
        if (blacklistTerms.stream().anyMatch(sbrt::contains) || blacklistTerms.stream().anyMatch(sbr.replaceAll("\\d", "")::contains))
            return randomString(len); //just generate a new one until we get a good one
        return sbr;
    }

    private static boolean checkForCode(List<Field> embedFields, String code) {
        for (Field field : embedFields) {
            for (String line : field.getValue().split("\n")) {
                line = line.replaceAll("[^a-zA-Z\\d]", " ").replace("  ", " ").strip();
                for (String word : line.split(" ")) {
                    if (Utils.editDistance(word, code) <= 2) { //allow swapping two chars
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //known words that VRChat censors plus a few more I added.
    static final List<String> blacklistTerms = Arrays.asList("ass", "biatch", "bimbos", "bitch", "boner", "buceta", "bugger",
            "busty", "cawk", "chinga", "chink", "choade", "cnut", "cock", "cocks", "cuck", "cum", "cumdump", "cummer",
            "cumming", "cumshot", "cunt", "cuntbag", "cunts", "cutrope", "dick", "dirsa", "doggin", "dogging", "doosh",
            "duche", "dyke", "erotic", "fag", "fagging", "faggitt", "faggot", "faggs", "fagot", "fagots", "fags", "fap",
            "fapfap", "fck", "fcuk", "fuck", "gaysex", "hairpie", "horny", "hotsex", "jackoff", "jerkoff", "jism",
            "jizm", "jizz", "joder", "joe", "kkk", "knobead", "knobed", "knobend", "kummer", "kumming", "kwif",
            "mangina", "mgtow", "mierda", "misandr", "misogyn", "mofo", "nazi", "nigga", "nigger", "nobhead", "nutsack",
            "orgasim", "orgasm", "pecker", "penis", "phuck", "phuk", "phuq", "porno", "prick", "pricks", "pusse",
            "pussi", "pussy", "queaf", "rapist", "red", "retard", "rimjaw", "rimjob", "scroat", "scrote", "scrotum",
            "sex", "shit", "sjw", "skank", "smegma", "snatch", "teets", "tit", "titfuck", "tittie", "titty", "titwank",
            "tupper", "twat", "twunt", "twunter", "vagina", "viagra", "wank", "wanker", "whore", "woose", "xrated",
            "xxx");
}
