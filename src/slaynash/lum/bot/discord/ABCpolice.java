package slaynash.lum.bot.discord;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ABCpolice {
    public static boolean abcPolice(MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != 815364277123940423L || event.getAuthor().isBot())
            return false;
        List<Message> history = new ArrayList<>(event.getTextChannel().getHistoryBefore(event.getMessage(), 10).complete().getRetrievedHistory());
        boolean brokenChain = history.size() > 0 && history.get(0).getAuthor().equals(event.getJDA().getSelfUser()) && history.get(0).getContentDisplay().contains("Start back to");
        history.removeIf(m -> m.getAuthor().isBot());
        if (history.size() == 0) //new channel or wipe or bot spam
            return true;
        char currentLetter = convertChar(event.getMessage().getContentDisplay());
        char previousLetter = convertChar(history.get(0).getContentDisplay());
        Message previousMessage = history.stream().filter(f -> f.getAuthor().equals(event.getAuthor())).findFirst().orElse(null);
        boolean timing = (previousMessage != null && previousMessage.getTimeCreated().isAfter(OffsetDateTime.now().minusHours(48)));

        if (brokenChain || previousLetter == 'z')
            previousLetter = 'a' - 1;

        if ((int) currentLetter != (int) (previousLetter) + 1) {
            System.out.println("abc does not match");
            event.getMessage().addReaction(":bonk:907068295868477551").queue();
            event.getChannel().sendMessage(event.getMember().getEffectiveName() + " just broke the chain <:Neko_sad:865328470652485633> Start back to `A`").queue();
            return true;
        }
        else if (event.getMessage().getContentDisplay().length() == 1) {
            System.out.println("abc hey that is cheating ");
            event.getMessage().addReaction(":baka:828070018935685130").queue();
            event.getChannel().sendMessage("Hey that is cheating <:Neko_pout:865328471102324778>")/*.delay(Duration.ofSeconds(30)).flatMap(Message::delete)*/.queue();
            return true;
        }
        else if (!brokenChain && timing && history.size() > 1 && (history.get(0).getAuthor().equals(event.getAuthor()) || history.get(1).getAuthor().equals(event.getAuthor()))) {
            System.out.println("abc spacing not meet");
            event.getChannel().sendMessage("User spacing rule was not meet <:Neko_sad:865328470652485633> Start back to `A`").queue();
            return true;
        }

        //valid ABC
        return true;
    }

    private static char convertChar(String letter) {
        if (letter.charAt(0) == 55356) { //Unicode indicator or something like that
            if (letter.charAt(1) >= 56806 && letter.charAt(1) <= 56831) { //convert regional_indicator to lowercase letters
                return (char) (letter.charAt(1) - 56709);
            }
        }
        return Character.toLowerCase(letter.charAt(0));
    }
}
