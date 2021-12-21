package slaynash.lum.bot.discord;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.gcardone.junidecode.Junidecode;

public class ABCpolice {
    public static boolean abcPolice(MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != 815364277123940423L)
            return false;
        if (event.getAuthor().isBot() || event.getMessage().isEdited())
            return true;
        String message = event.getMessage().getContentStripped().toLowerCase();
        System.out.println(event.getMember().getEffectiveName() + ": " + message);
        List<Message> history = new ArrayList<>(event.getTextChannel().getHistoryBefore(event.getMessage(), 20).complete().getRetrievedHistory());
        List<Message> chain = history;
        boolean brokenChain = history.size() > 0 && history.get(0).getAuthor().equals(event.getJDA().getSelfUser()) && history.get(0).getContentStripped().contains("tart back to");
        Optional<Message> find = chain.stream().filter(h -> h.getContentStripped().contains("tart back to")).findFirst();
        find.ifPresent(value -> chain.subList(chain.indexOf(value), chain.size()).clear());
        chain.removeIf(m -> m.getAuthor().isBot() || m.getContentStripped().isBlank());
        if (chain.size() == 0) //new channel or wipe or bot spam
            return true;
        char currentLetter = convertChar(message);
        char previousLetter = convertChar(chain.get(0).getContentStripped());
        Message previousMessage = history.stream().filter(f -> f.getAuthor().equals(event.getAuthor())).findFirst().orElse(null);
        boolean timing = previousMessage != null && previousMessage.getTimeCreated().isAfter(OffsetDateTime.now().minusHours(48));

        if (brokenChain || previousLetter == 'z')
            previousLetter = 'a' - 1;

        if (message.length() == 0) {
            System.out.println("abc empty message");
            event.getChannel().sendMessage(event.getMember().getEffectiveName() + " sent an empty message, Stickers are not allowed. Start back to `A`").queue();
            return true;
        }
        else if ((int) currentLetter != (int) previousLetter + 1) {
            System.out.println("abc does not match");
            event.getMessage().addReaction(":bonk:907068295868477551").queue();
            event.getChannel().sendMessage(event.getMember().getEffectiveName() + " just broke the chain <:Neko_sad:865328470652485633> Start back to `A`").queue();
            return true;
        }
        else if (!brokenChain && (message.length() == 1 || !Character.isLetterOrDigit(message.charAt(1))) && currentLetter != 'a' && currentLetter != 'i') {
            System.out.println("abc hey that is cheating");
            event.getMessage().addReaction(":baka:828070018935685130").queue();
            event.getChannel().sendMessage("Hey that is cheating <:Neko_pout:865328471102324778> Time to start back to `A`")/*.delay(Duration.ofSeconds(30)).flatMap(Message::delete)*/.queue();
            return true;
        }
        else if (!brokenChain && timing && chain.size() > 1 && (chain.get(0).getAuthor().equals(event.getAuthor()) || chain.get(1).getAuthor().equals(event.getAuthor()))) {
            System.out.println("abc spacing not meet");
            event.getChannel().sendMessage("User spacing rule was not met <:Neko_sad:865328470652485633> Someone else, start back to `A`").queue();
            return true;
        }

        //valid ABC
        return true;
    }

    private static char convertChar(String letter) {
        if (letter.length() == 0)
            return '\0';
        letter = Junidecode.unidecode(letter);
        if (letter.charAt(0) == 55356) { //Unicode indicator or something like that
            if (letter.charAt(1) >= 56806 && letter.charAt(1) <= 56831) { //convert regional_indicator to lowercase letters
                return (char) (letter.charAt(1) - 56709);
            }
        }
        return Character.toLowerCase(letter.charAt(0));
    }
}
