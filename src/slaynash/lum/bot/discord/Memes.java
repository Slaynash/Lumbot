package slaynash.lum.bot.discord;

import java.sql.ResultSet;
import java.util.List;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.utils.ExceptionUtils;

public class Memes {
    private static final float removalPercent = 0.2f;
    private static final int minVotes = 5;
    private static final CustomEmoji upArrow = Emoji.fromCustom("UNOUpVote", 936073450815111208L, false);
    private static final CustomEmoji downArrow = Emoji.fromCustom("UNODownVote", 936073450676703282L, false);

    public static boolean memeRecieved(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EXT_EMOJI)) {
            if (event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_SEND))
                event.getChannel().sendMessage("I need the `MESSAGE_ADD_REACTION` and `MESSAGE_EXT_EMOJI` permissions to moderate memes").queue();
            return false;
        }

        if (event.getAuthor().isBot()) return false;
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `ReportChannel` FROM `Memes` WHERE MemeChannel = ?", channel.getIdLong());
            if (rs.next()) {
                event.getMessage().addReaction(upArrow).queue(c -> event.getMessage().addReaction(downArrow).queue());
                DBConnectionManagerLum.closeRequest(rs);
                return true;
            }
            DBConnectionManagerLum.closeRequest(rs);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to check if channel is meme channel", e);
        }
        return false;
    }

    public static void memeReaction(GenericMessageReactionEvent event) {
        if (event.getUser().isBot()) return;
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) return;
        long memeReportChannelID;
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `ReportChannel` FROM `Memes` WHERE MemeChannel = ?", event.getChannel().getIdLong());
            if (rs.next()) {
                memeReportChannelID = rs.getLong("ReportChannel");
            }
            else return;
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to check if channel is a meme channel", e);
            return;
        }
        Message message = event.retrieveMessage().complete();
        if (message == null) return;
        List<User> upReactions = message.retrieveReactionUsers(upArrow).complete();
        List<User> downReactions = message.retrieveReactionUsers(downArrow).complete();
        upReactions.remove(event.getJDA().getSelfUser());
        downReactions.remove(event.getJDA().getSelfUser());
        // ignore og author
        upReactions.remove(message.getAuthor());
        downReactions.remove(message.getAuthor());

        if (upReactions.size() + downReactions.size() < minVotes) return;

        float reactionVotes = getPercentage(upReactions.size(), downReactions.size());

        if (reactionVotes < removalPercent) {
            if (memeReportChannelID != 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(message.getAuthor().getName()).append(" ").append(message.getAuthor().getId()).append("\n");
                sb.append(message.getContentRaw()).append("\n");
                if (!upReactions.isEmpty())
                    sb.append("\nup voted:\n").append(upReactions).append("\n");
                sb.append("\ndown voted:\n").append(downReactions).append("\n");
                MessageCreateAction ma = message.getGuild().getTextChannelById(memeReportChannelID).sendMessage(sb.toString());
                for (Attachment attach : message.getAttachments()) {
                    try {
                        ma.addFiles(FileUpload.fromData(attach.getProxy().download().get(), attach.getFileName()).asSpoiler());
                    }
                    catch (Exception e) {
                        ExceptionUtils.reportException("Failed reattaching meme", e);
                    }
                }
                ma.queue();
            }
            message.delete().reason("Meme was voted out").queue();
        }
    }

    private static float getPercentage(int up, int down) {
        return (float) up / (up + down);
    }
}
