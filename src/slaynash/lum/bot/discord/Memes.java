package slaynash.lum.bot.discord;

import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.AttachmentOption;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Memes {
    private static float removalPercent = 0.2f;
    private static int minVotes = 5;
    private static String upArrow = ":UNOUpVote:936073450815111208";
    private static String downArrow = ":UNODownVote:936073450676703282";
    private static long memeChannelID = 1002766110350917682L;
    private static long memeReportChannelID = 1007540894506946581L;

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

        if (reactionVotes < removalPercent) {
            StringBuilder sb = new StringBuilder();
            sb.append(message.getAuthor().getName()).append(" ").append(message.getAuthor().getId()).append("\n");
            sb.append(message.getContentRaw() + "\n");
            if (upReactions.size() > 0)
                sb.append("\nup voted:\n" + upReactions + "\n");
            sb.append("\ndown voted:\n" + downReactions + "\n");
            MessageAction ma = message.getGuild().getTextChannelById(memeReportChannelID).sendMessage(sb.toString());
            for (Attachment attach : message.getAttachments()) {
                try {
                    ma.addFile(attach.retrieveInputStream().get(), attach.getFileName(), AttachmentOption.SPOILER);
                } catch (Exception e) {
                    ExceptionUtils.reportException("Failed reattaching meme", e);
                }
            }
            ma.queue();
            message.delete().reason("Meme was voted out").queue();
        }
    }

    private static float getPercentage(int up, int down) {
        return (float) up / (up + down);
    }
}
