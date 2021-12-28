package slaynash.lum.bot.discord;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.utils.Utils;

public class TicketTool {
    public static final String LOG_IDENTIFIER = "TicketTool";
    private static final Random random = new Random();

    public static void tickettool(MessageReceivedEvent event) {
        long category = event.getMessage().getCategory() == null ? 0L : event.getMessage().getCategory().getIdLong();
        String channelName = event.getTextChannel().getName();
        String pString = "To confirm your identity, please add this passcode to your VRChat Status or Bio: `" + randomString(8) + "`\nAfter you added it, please send either`/vrcuser [username or UserID]`or`r.vrcuser [username or UserID]`into this channel, for example`r.vrcuser tupper`\n\nTo edit your Bio navigate to the Social menu, select yourself, then choose \"Edit Bio\".\nYou can also sign in to <https://www.vrchat.com/home> and add it to your Bio there.";
        if (category != 765058331345420298L /*emmVRC Tickets*/ && category != 899140251241570344L /*emmVRC Tickets Claimed*/ || event.getChannel().getIdLong() == 801679570863783937L/*testing*/)
            return;
        if (event.getAuthor().getIdLong() == 722196398635745312L /*tickettool*/ && event.getMessage().getContentDisplay().startsWith("Welcome")) {
            if (channelName.contains("reset"))
                event.getTextChannel().sendMessage(pString).queue();
            else if (channelName.contains("wipe"))
                event.getTextChannel().sendMessage(pString).queue();
            else if (channelName.contains("deletion"))
                event.getTextChannel().sendMessage(pString).queue();
            else if (channelName.contains("export"))
                event.getTextChannel().sendMessage("Avatar Favorite Exporting is also available via emmVRC > Settings > small Export button in the upper right corner\nIt would be exported to `VRChat\\UserData\\emmVRC\\ExportedList.json`\nIf you are unable to use the automatic export, please let say so otherwise have a wonderful day and you can close this ticket.").queue();
        }
        else if (event.getAuthor().getIdLong() == 886944444107063347L /*Rubybot*/ && event.getMessage().getEmbeds().size() > 0) {
            Thread thread = new Thread(() -> {
                System.out.println("Receved embed from Rubybot");
                List<Message> history = new ArrayList<>(event.getTextChannel().getHistoryFromBeginning(100).complete().getRetrievedHistory());
                history.removeIf(m -> !m.getAuthor().equals(m.getJDA().getSelfUser()));
                if (history.size() == 0) {
                    System.out.println("Can not find my messages");
                    return;
                }
                String[] split = history.get(history.size() - 1).getContentRaw().split("`");
                if (split.length < 2) {
                    System.out.println("Can not find my pin in ticket");
                    return;
                }
                String code = split[1].toLowerCase();
                List<Field> embed = event.getMessage().getEmbeds().get(0).getFields();
                boolean codeFound = checkForCode(embed, code);
                String id = embed.get(0).getValue(); //assume that ID is always in the first field

                if (channelName.contains("reset") && codeFound) {
                    event.getTextChannel().sendMessage("e.pin reset " + id).queue();
                }
                else if (channelName.contains("wipe") && codeFound) {
                    event.getTextChannel().sendMessage("Thank you for verifying your account!\nPlease confirm that you want all of your emmVRC favorites removed from your account.\nA staff member will help you further once they see your confirmation.").queue();
                }
                else if (channelName.contains("deletion") && codeFound) {
                    event.getTextChannel().sendMessage("Thank you for verifying your account!\nPlease confirm that you want all data about your emmVRC account deleted.\nA staff member will help you further once they see your confirmation.").queue();
                }

                System.out.println("Code: " + code + " ID:" + id);
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
        if (sbr.matches(".*(joe|red|sjw|[s5]hit|fuck|slut|clit).*"))
            return randomString(len); //just generate a new one until we get a good one
        return sbr;
    }

    private static boolean checkForCode(List<Field> embed, String code) {
        for (Field field : embed) {
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
