package slaynash.lum.bot.discord;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.DBConnectionManagerLum;
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
                if (channelName.contains("reset"))
                    event.getTextChannel().sendMessage(DBConnectionManagerLum.getString("strings", "string", "value", "emmTTmessage").replace("$randomString$", randomString(8))).queue();
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
                Role emmadmin = event.getJDA().getGuildById(600298024425619456L).getRoleById(748392927365169233L);
                Role emmnetwork = event.getJDA().getGuildById(600298024425619456L).getRoleById(801670419723452487L);
                history.removeIf(m -> !m.getAuthor().equals(m.getJDA().getSelfUser()) && !m.getMember().getRoles().contains(emmadmin) && !m.getMember().getRoles().contains(emmnetwork));
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
                List<Field> embedFields = event.getMessage().getEmbeds().get(0).getFields();
                boolean codeFound = checkForCode(embedFields, code);
                String id = embedFields.get(0).getValue().replace("`", ""); //assume that ID is always in the first field
                System.out.println("Code: " + code + " ID:" + id);

                if (event.getGuild().getIdLong() == 600298024425619456L /* emmVRC */ && codeFound) {
                    if (channelName.contains("reset")) {
                        event.getTextChannel().sendMessage("e.pin reset " + id).queue();
                    }
                    else if (channelName.contains("wipe")) {
                        event.getTextChannel().sendMessage("Thank you for verifying your account!\nPlease confirm that you want all of your emmVRC favorites removed from your account.\nA staff member will help you further once they see your confirmation.").queue();
                    }
                    else if (channelName.contains("deletion")) {
                        event.getTextChannel().sendMessage("Thank you for verifying your account!\nPlease confirm that you want all data about your emmVRC account deleted.\nA staff member will help you further once they see your confirmation.").queue();
                    }
                }
                else if (event.getGuild().getIdLong() == 716536783621587004L /* TW */ && codeFound) {
                    event.getTextChannel().sendMessage("tw!deluser " + id).queue();
                }
            }, "Ticket");
            thread.start();
        }
    }

    private static final String AB = "23456789abcdefghijkmnopqrstuvwxyz";
    private static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(random.nextInt(AB.length())));
        String sbr = sb.toString();
        if (sbr.matches(".*(joe|red|sjw|[s5]hit|fuck|slut|clit|xxx).*"))
            return randomString(len); //just generate a new one until we get a good one
        return sbr;
    }

    private static boolean checkForCode(List<Field> embedFields, String code) {
        for (Field field : embedFields) {
            for (String line : field.getValue().split("\n")) {
                line = line.replaceAll("[^a-zA-Z0-9]", " ").replace("  ", " ").strip();
                for (String word : line.split(" ")) {
                    if (Utils.editDistance(word, code) <= 2) { //allow swapping two chars
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
