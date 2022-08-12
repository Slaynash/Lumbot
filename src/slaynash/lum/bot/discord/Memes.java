package slaynash.lum.bot.discord;

import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

public class Memes {
    private static float removalPercent = 0.3f;
    private static int minVotes = 5;
    private static String upArrow = ":UNOUpVote:936073450815111208";
    private static String downArrow = ":UNODownVote:936073450676703282";
    private static long memeChannelID = 1002766110350917682L;

    public static boolean memeRecieved(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        if (event.getAuthor().isBot()) return false;
        if (channel == null) return false;
        if (channel.getIdLong() != memeChannelID) return false;
        event.getMessage().addReaction(upArrow).queue(c -> event.getMessage().addReaction(downArrow).queue());
        return true;
    }

    public static void memeReaction(GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;
        if (event.getChannel().getIdLong() != memeChannelID) return;
        Message message = event.retrieveMessage().complete();
        if (message == null) return;
        List<User> upReactions = message.retrieveReactionUsers(upArrow).complete();
        List<User> downReactions = message.retrieveReactionUsers(downArrow).complete();
        upReactions.remove(event.getJDA().getSelfUser());
        downReactions.remove(event.getJDA().getSelfUser());

        if (upReactions.size() + downReactions.size() < minVotes) return;

        float reactionVotes = getPercentage(upReactions.size(), downReactions.size());

        if (reactionVotes < removalPercent)
            message.delete().reason("Meme was voted out").queue();
        //TODO add reporting
    }

    private static float getPercentage(int up, int down) {
        return (float) up / (up + down);
    }
}
