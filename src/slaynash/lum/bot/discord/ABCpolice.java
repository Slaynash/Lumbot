package slaynash.lum.bot.discord;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.gcardone.junidecode.Junidecode;

public class ABCpolice {
    public static final String LOG_IDENTIFIER = "ABCpolice";
    public static boolean abcPolice(MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != 815364277123940423L)
            return false;
        if (event.getAuthor().isBot() || event.getMessage().isEdited())
            return true;
        String message = event.getMessage().getContentStripped().trim();
        if (event.getMessage().getStickers().size() > 0)
            message = event.getMessage().getStickers().get(0).getName() +  " " + message;
        System.out.println("abc " + event.getMember().getEffectiveName() + ": " + message);
        List<Message> history = new ArrayList<>(event.getChannel().asGuildMessageChannel().getHistoryBefore(event.getMessage(), 20).complete().getRetrievedHistory());
        boolean brokenChain = !history.isEmpty() && history.get(0).getAuthor().equals(event.getJDA().getSelfUser()) && history.get(0).getContentStripped().contains("tart back to");
        Optional<Message> find = history.stream().filter(h -> h.getContentStripped().contains("tart back to")).findFirst();
        find.ifPresent(value -> history.subList(history.indexOf(value), history.size()).clear());
        history.removeIf(m -> m.getAuthor().isBot());
        if (history.isEmpty() && !brokenChain) //new channel or wipe or bot spam
            return true;
        Message previousAuthorMessage = history.stream().filter(f -> f.getAuthor().equals(event.getAuthor())).findFirst().orElse(null);
        char currentLetter = convertChar(message);
        char previousLetter = !history.isEmpty() ? convertChar(history.get(0).getContentStripped()) : 0;
        boolean timing = previousAuthorMessage != null && previousAuthorMessage.getTimeCreated().isAfter(OffsetDateTime.now().minusHours(30));  // if the previous message was sent less than 30 hours ago

        if (brokenChain || previousLetter == 0 || previousLetter == 'z')
            previousLetter = 'a' - 1;
        System.out.println("abc previousLetter:" + previousLetter + " currentLetter:" + currentLetter + " brokenChain:" + brokenChain);

        if (message.isEmpty()) {
            System.out.println("abc empty message");
            event.getChannel().sendMessage(event.getMember().getEffectiveName() + " sent an empty message, Stickers are not allowed. Start back to `A`").queue();
            return true;
        }
        else if ((int) currentLetter != (int) previousLetter + 1) {
            System.out.println("abc does not match");
            event.getMessage().addReaction(Emoji.fromCustom("bonk", 907068295868477551L, false)).queue();
            event.getChannel().sendMessage(event.getMember().getEffectiveName() + " just broke the chain, it should have been `" + Character.toUpperCase((char) (previousLetter + 1)) + "`  <:Neko_sad:865328470652485633> Start back to `A`").queue();
            return true;
        }
        else if (!brokenChain && message.length() == 1) {
            System.out.println("abc hey that is cheating");
            event.getMessage().addReaction(Emoji.fromCustom("baka", 828070018935685130L, false)).queue();
            event.getChannel().sendMessage("Hey that is cheating <:Neko_pout:865328471102324778> Time to start back to `A`").queue();
            return true;
        }
        else if (!brokenChain && timing && currentLetter != 'a' && history.size() > 1 && (history.get(0).getAuthor().equals(event.getAuthor()) || history.get(1).getAuthor().equals(event.getAuthor()))) {
            System.out.println("abc spacing not meet");
            event.getChannel().sendMessage("User spacing rule was not met <:Neko_sad:865328470652485633> Someone else, start back to `A`").queue();
            return true;
        }
        else if (currentLetter == 'z') {
            System.out.println("abc looped around");
            event.getChannel().sendMessage("おめでとう。あなたはめちゃくちゃせずにそれを出来た！<:Neko_dab:865328473719439381>").queue();
            return true;
        }

        //valid ABC
        return true;
    }

    private static char convertChar(String letter) {
        if (letter.isEmpty())
            return '\0';
        letter = Junidecode.unidecode(letter.toLowerCase());
        if (letter.charAt(0) == 55356) { //Unicode indicator or something like that
            if (letter.charAt(1) >= 56806 && letter.charAt(1) <= 56831) { //convert regional_indicator to lowercase letters
                return (char) (letter.charAt(1) - 56709);
            }
        }
        return letter.replace(":", "").charAt(0);
    }
}
