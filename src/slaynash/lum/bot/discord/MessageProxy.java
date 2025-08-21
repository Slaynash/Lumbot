package slaynash.lum.bot.discord;

import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import com.coder4.emoji.EmojiUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.gcardone.junidecode.Junidecode;
import org.jetbrains.annotations.NotNull;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;
import slaynash.lum.bot.discord.utils.CrossServerUtils;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class MessageProxy {

    public static void fromDM(MessageReceivedEvent event) {
        Guild mainGuild = event.getJDA().getGuildById(JDAManager.mainGuildID);

        User author = event.getAuthor();
        String channelName = Junidecode.unidecode("dm-" + author.getEffectiveName() + "-" + author.getId()).toLowerCase()
                .replaceAll("[^a-z\\d\\-_]", "").replace(" ", "-").replace("--", "-");
        TextChannel guildchannel = mainGuild.getTextChannels().stream().filter(c -> c.getName().contains(author.getId())).findFirst().orElse(null);

        String message = getMessage(author, event.getMessage());
        if (guildchannel == null) {
            System.out.println("Creating DM Channel " + channelName);
            System.out.println("Number of Channels: " + mainGuild.getTextChannels().size());
            StringBuilder sb = new StringBuilder();
            event.getChannel().getHistoryBefore(event.getMessage(), 100).complete().getRetrievedHistory()
                    .forEach(m -> {
                        sb.append(m.getTimeCreated()).append(" ").append(m.getAuthor().getEffectiveName()).append(": ")
                                .append(m.getContentRaw()).append(" ");
                        m.getAttachments().forEach(a -> sb.append(a.getUrl()).append(" "));
                        m.getStickers().forEach(s -> sb.append(s.getIconUrl()).append(" "));
                        sb.append("\n");
                    });
            guildchannel = mainGuild.createTextChannel(channelName, mainGuild.getCategoryById(924780998124798022L)).complete();

            String mutuals = author.getAsMention() + "\n\n" + "Mutuals:\n" + CrossServerUtils.getMutualGuilds(author);
            if (mutuals.length() > Message.MAX_CONTENT_LENGTH) {
                mutuals = mutuals.substring(0, Message.MAX_CONTENT_LENGTH);
            }

            if (sb.toString().isBlank())
                guildchannel.sendMessage(mutuals).queue();
            else
                guildchannel.sendMessage(mutuals).addFiles(FileUpload.fromData(sb.toString().getBytes(), author.getName() + ".txt")).queue();

        }
        guildchannel.sendMessage(message).queue(s -> saveIDs(event.getMessageIdLong(), s.getIdLong()));

        for (Attachment attachment : event.getMessage().getAttachments()) {
            try {
                guildchannel.sendFiles(FileUpload.fromData(attachment.getProxy().download().get(), attachment.getFileName())).queue(s -> saveIDs(event.getMessageIdLong(), s.getIdLong()));
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed reattaching attachment", e);
            }
            if (MelonScanner.isValidFileFormat(attachment, false)) {
                MessageCreateData scan = MelonScanner.scanMessage(event, attachment);
                if (scan != null) {
                    guildchannel.sendMessage(scan).queue(s1 -> event.getChannel().sendMessage(scan).queue(s2 -> saveIDs(s2.getIdLong(), s1.getIdLong())));
                }
            }
        }
        handleDMReplies(event, event.getMessage().getContentRaw());
    }

    @NotNull
    private static String getMessage(User author, Message event) {
        String message = author.getEffectiveName() + ":\n" + event.getContentRaw();

        for (CustomEmoji emoji : event.getMentions().getCustomEmojis()) {
            if (event.getJDA().getEmojiById(emoji.getIdLong()) == null)
                message = message.concat("\n").concat(emoji.getImageUrl());
        }
        for (StickerItem sticker : event.getStickers()) {
            if (event.getJDA().getGuilds().stream().noneMatch(g -> g.getStickerById(sticker.getIdLong()) != null))
                message = message.concat("\n").concat(sticker.getIconUrl());
        }
        if (message.length() > Message.MAX_CONTENT_LENGTH) {
            message = message.substring(0, Message.MAX_CONTENT_LENGTH);
        }
        return message;
    }

    public static boolean fromDev(MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong() &&
                event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */ &&
                event.getChannel().getName().toLowerCase().startsWith("dm-") &&
                !event.getMessage().getContentRaw().startsWith(".") &&
                !event.getMessage().getContentRaw().startsWith(ConfigManager.discordPrefix))
        {
            String[] userID = event.getChannel().getName().split("-");
            User user = JDAManager.getJDA().retrieveUserById(userID[userID.length - 1]).complete();
            if (user == null) {
                Utils.replyEmbed("Can not find user, maybe there are no mutual servers.", Color.red, event);
                return true;
            }
            Message message = event.getMessage();
            try {
                message.getMentions().getCustomEmojis().forEach(emote -> {
                    RichCustomEmoji richemote = (RichCustomEmoji) emote; //this will throw an exception if the emote is not in a server with lum
                    richemote.getGuild().getName();
                    if (!richemote.canInteract(richemote.getGuild().getSelfMember())) { //Check if lum has perms to use the emote
                        message.reply("Lum can not use that emote.").queue();
                        // return true;
                    }
                });
            }
            catch (Exception e) {
                message.reply("Lum can not use that emote as I also need to be in that emote's server.").queue();
                return true;
            }
            user.openPrivateChannel().queue(
                    channel -> channel.sendMessage(prepareMessage(message)).queue(s -> saveIDs(s.getIdLong(), event.getMessageIdLong()),
                            e -> Utils.sendEmbed("Failed to send message to target user: " + e.getMessage(), Color.red, event)),
                    error -> Utils.sendEmbed("Failed to open DM with target user: " + error.getMessage(), Color.red, event));

            return true;
        }
        return false;
    }

    private static MessageCreateData prepareMessage(Message message) {
        MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
        messageBuilder.setContent(message.getContentRaw());
        for (Attachment attachment : message.getAttachments()) {
            if (attachment.getSize() > 8000000) {
                messageBuilder.setContent(messageBuilder.getContent().concat("\n").concat(attachment.getUrl()));
            }
            else {
                try {
                    messageBuilder.addFiles(FileUpload.fromData(attachment.getProxy().download().get(), attachment.getFileName()));
                }
                catch (InterruptedException | ExecutionException e) {
                    ExceptionUtils.reportException("failed to proxy attachment", e);
                }
            }
        }
        for (StickerItem sticker : message.getStickers()) {
            messageBuilder.setContent(messageBuilder.getContent() + "\n" + sticker.getIconUrl());
        }
        return messageBuilder.build();
    }

    public static void proxyTyping(UserTypingEvent event) {
        Guild mainGuild = event.getJDA().getGuildById(JDAManager.mainGuildID);
        if (mainGuild == null)
            return;
        TextChannel guildchannel = mainGuild.getTextChannels().stream().filter(c -> c.getName().endsWith(event.getUser().getId())).findFirst().orElse(null);
        if (guildchannel == null) {
            return;
        }
        if (event.isFromType(ChannelType.PRIVATE)) {
            if (guildchannel != null) {
                guildchannel.sendTyping().queue();
            }
        }
        else if (event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */ && ChannelType.TEXT.equals(event.getChannel().getType()) && event.getChannel().asTextChannel().getParentCategory() != null && event.getChannel().asTextChannel().getParentCategory().getIdLong() == 924780998124798022L) {
            event.getUser().openPrivateChannel().queue(
                channel -> channel.sendTyping().queue(null,
                        e -> event.getChannel().asGuildMessageChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Can not send message to target user: " + e.getMessage(), Color.red)).queue()),
                error -> event.getChannel().asGuildMessageChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Can not open DM with target user: " + error.getMessage(), Color.red)).queue());
        }
    }

    public static boolean edits(MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) return false;
        if (event.isFromType(ChannelType.PRIVATE)) {
            User author = event.getAuthor();
            Guild mainGuild = event.getJDA().getGuildById(JDAManager.mainGuildID);
            TextChannel guildchannel = mainGuild.getTextChannels().stream().filter(c -> c.getName().contains(author.getId())).findFirst().orElse(null);
            if (guildchannel == null) {
                return true;
            }

            String message = getMessage(author, event.getMessage());

            try {
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MessagePairs` WHERE `OGMessage` = ?", event.getMessageIdLong());
                while (rs.next()) {
                    guildchannel.editMessageById(rs.getLong("DevMessage"), message).queue();
                }
                DBConnectionManagerLum.closeRequest(rs);
                return true;
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("failed to update proxy message", e);
            }
        }
        else if (event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */ && event.getChannel().getName().toLowerCase().startsWith("dm-")) {
            // From Devs
            if (event.getMessage().getContentRaw().startsWith(".")) return true;
            String[] userID = event.getChannel().getName().split("-");
            User user = JDAManager.getJDA().retrieveUserById(userID[userID.length - 1]).complete();
            if (user == null) {
                event.getChannel().sendMessage("Could not find user's message").queue();
                return true;
            }
            try {
                PrivateChannel pmChannel = user.openPrivateChannel().complete();
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MessagePairs` WHERE `DevMessage` = ?", event.getMessageIdLong());
                while (rs.next()) {
                    pmChannel.editMessageById(rs.getLong("OGMessage"), prepareMessage(event.getMessage()).getContent()).queue();
                }
                DBConnectionManagerLum.closeRequest(rs);
                return true;
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("failed to update proxy message", e);
            }
        }
        return false;
    }

    public static void deletes(MessageDeleteEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            User author = event.getChannel().asPrivateChannel().getUser();
            Guild mainGuild = event.getJDA().getGuildById(JDAManager.mainGuildID);
            TextChannel guildchannel = mainGuild.getTextChannels().stream().filter(c -> c.getName().contains(author.getId())).findFirst().orElse(null);
            if (guildchannel == null) {
                return;
            }

            try {
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MessagePairs` WHERE `OGMessage` = ?", event.getMessageIdLong());
                while (rs.next()) {
                    guildchannel.retrieveMessageById(rs.getLong("DevMessage")).queue(message -> {
                        String mesString = "Deleted\n".concat(message.getContentRaw());
                        if (mesString.length() > MessageEmbed.TEXT_MAX_LENGTH) {
                            mesString = mesString.substring(0, MessageEmbed.TEXT_MAX_LENGTH);
                        }
                        message.editMessage(mesString).queue();
                    });
                }
                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("failed to remove proxy message", e);
            }
        }
        else if (event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */ && event.getChannel().getName().toLowerCase().startsWith("dm-")) {
            // From Devs
            String[] userID = event.getChannel().getName().split("-");
            User user = JDAManager.getJDA().retrieveUserById(userID[userID.length - 1]).complete();
            if (user == null) {
                event.getChannel().sendMessage("Could not find user's message").queue();
                return;
            }
            try {
                PrivateChannel pmChannel = user.openPrivateChannel().complete();
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MessagePairs` WHERE `DevMessage` = ?", event.getMessageIdLong());
                while (rs.next()) {
                    pmChannel.deleteMessageById(rs.getLong("OGMessage")).queue();
                }
                DBConnectionManagerLum.closeRequest(rs);
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("failed to remove proxy message", e);
            }
        }
    }

    public static void saveIDs(long ogID, long devID) {
        try {
            DBConnectionManagerLum.sendUpdate("INSERT INTO `MessagePairs` (`OGMessage`, `DevMessage`) VALUES (?, ?)", ogID, devID);
        }
        catch (SQLException e) {
            ExceptionUtils.reportException("save ID failed", e);
        }
    }

    public static boolean reactions(MessageReactionAddEvent event) {
        Guild mainGuild = event.getJDA().getGuildById(JDAManager.mainGuildID);
        if (event.isFromType(ChannelType.PRIVATE)) {
            TextChannel guildchannel = mainGuild.getTextChannels().stream().filter(c -> c.getName().contains(event.getMessageAuthorId())).findFirst().orElse(null);
            if (guildchannel == null) {
                return true;
            }

            try {
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MessagePairs` WHERE `OGMessage` = ?", event.getMessageIdLong());
                while (rs.next()) {
                    if (event.getEmoji().getType() == Emoji.Type.CUSTOM) {
                        long devMessage = rs.getLong("DevMessage");
                        guildchannel.addReactionById(devMessage, event.getEmoji()).queue(null, e -> guildchannel.retrieveMessageById(devMessage).queue(message -> message.reply(event.getEmoji().asCustom().getImageUrl()).queue()));
                    }
                    else {
                        guildchannel.addReactionById(rs.getLong("DevMessage"), event.getEmoji().asUnicode()).queue();
                    }
                }
                DBConnectionManagerLum.closeRequest(rs);
                return true;
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("failed to react to proxy message", e);
            }
        }
        else if (event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */ && event.getChannel().getName().toLowerCase().startsWith("dm-")) {
            // From Devs
            String[] userID = event.getChannel().getName().split("-");
            User user = JDAManager.getJDA().retrieveUserById(userID[userID.length - 1]).complete();
            if (user == null) {
                event.getChannel().sendMessage("Could not find user").queue();
                return true;
            }
            try {
                PrivateChannel pmChannel = user.openPrivateChannel().complete();
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MessagePairs` WHERE `DevMessage` = ?", event.getMessageIdLong());
                while (rs.next()) {
                    pmChannel.addReactionById(rs.getLong("OGMessage"), event.getEmoji()).queue(null, e -> event.getChannel().sendMessage("Unable to react with that emote").queue());
                }
                DBConnectionManagerLum.closeRequest(rs);
                return true;
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("failed to react to proxy message", e);
            }
        }
        return false;
    }

    public static boolean reactions(MessageReactionRemoveEvent event) {
        Guild mainGuild = event.getJDA().getGuildById(JDAManager.mainGuildID);
        if (event.isFromType(ChannelType.PRIVATE)) {
            User author = event.getUser();
            TextChannel guildchannel = mainGuild.getTextChannels().stream().filter(c -> c.getName().contains(author.getId())).findFirst().orElse(null);
            if (guildchannel == null) {
                return true;
            }

            try {
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MessagePairs` WHERE `OGMessage` = ?", event.getMessageIdLong());
                while (rs.next()) {
                    guildchannel.removeReactionById(rs.getLong("DevMessage"), event.getEmoji()).queue(null, e -> { });
                }
                DBConnectionManagerLum.closeRequest(rs);
                return true;
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("failed to remove reaction in proxy message", e);
            }
        }
        else if (event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */ && event.getChannel().getName().toLowerCase().startsWith("dm-")) {
            // From Devs
            String[] userID = event.getChannel().getName().split("-");
            User user = JDAManager.getJDA().retrieveUserById(userID[userID.length - 1]).complete();
            if (user == null) {
                event.getChannel().sendMessage("Could not find user").queue();
                return true;
            }
            try {
                PrivateChannel pmChannel = user.openPrivateChannel().complete();
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `MessagePairs` WHERE `DevMessage` = ?", event.getMessageIdLong());
                while (rs.next()) {
                    pmChannel.removeReactionById(rs.getLong("OGMessage"), event.getEmoji()).queue(null, e -> { });
                }
                DBConnectionManagerLum.closeRequest(rs);
                return true;
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("failed to remove reaction in proxy message", e);
            }
        }
        return false;
    }

    public static boolean handleDMReplies(MessageReceivedEvent event, String content) {
        try {
            if (content == null || content.isBlank())
                return false;
            content = content.toLowerCase();
            if (event.getAuthor().equals(event.getJDA().getSelfUser()))
                return false;
            try {
                ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `Replies` WHERE `guildID` = 0");

                boolean found = false;
                while (rs.next()) {
                    int ukey = rs.getInt("ukey");
                    String regex = rs.getString("regex");
                    String contains = rs.getString("contains");
                    String equals = rs.getString("equals");
                    long user = rs.getLong("user");
                    long channel = rs.getLong("channel");
                    long ignorerole = rs.getLong("ignorerole");
                    String message = rs.getString("message");
                    boolean edit = rs.getBoolean("bedit");
                    boolean report = rs.getBoolean("breport");
                    if (!edit && event.getMessage().isEdited()) {
                        continue;
                    }
                    if (regex != null && !regex.isBlank()) {
                        if (!content.matches("(?s)".concat(regex))) { //TODO prevent Catastrophic backtracking
                            continue;
                        }
                    }
                    if (contains != null && !contains.isBlank()) {
                        if (!content.contains(contains)) {
                            continue;
                        }
                    }
                    if (equals != null && !equals.isBlank()) {
                        if (!content.equals(equals)) {
                            continue;
                        }
                    }
                    if (user > 69420) {
                        if (event.getAuthor().getIdLong() != user) {
                            continue;
                        }
                    }
                    if (channel > 69420) {
                        System.out.println("Channel: " + channel + " " + event.getChannel().getIdLong());
                        if (event.getChannel().getIdLong() != channel && (event.getChannel().asTextChannel() == null || event.getChannel().asTextChannel().getParentCategory() == null || event.getChannel().asTextChannel().getParentCategory().getIdLong() != channel)) {
                            continue;
                        }
                    }
                    if (ignorerole > 69420) {
                        if (event.getMember().getRoles().stream().anyMatch(r -> r.getIdLong() == ignorerole)) {
                            continue;
                        }
                    }
                    if (event.getAuthor().isBot() && !rs.getBoolean("bbot")) {
                        continue;
                    }

                    if (EmojiUtils.isOneEmoji(message))
                        event.getMessage().addReaction(Emoji.fromUnicode(message)).queue();
                    else if (message != null && message.matches("^<a?:\\w+:\\d+>$")) {
                        System.out.println("Emoji: " + message);
                        RichCustomEmoji emote = event.getJDA().getEmojiById(message.replace(">", "").split(":")[2]);
                        try {
                            event.getMessage().addReaction(emote).queue(); //This could error if too many reactions on message
                        }
                        catch (Exception e) {
                            System.out.println("Failed to add reaction: " + e.getMessage());
                        }
                    }
                    else if (message != null && !message.isBlank()) {
                        event.getMessage().reply(message).queue();
                        Guild mainGuild = JDAManager.getJDA().getGuildById(JDAManager.mainGuildID);
                        if (mainGuild == null) {
                            System.out.println("Main guild not found");
                            return true;
                        }
                        TextChannel guildchannel = mainGuild.getTextChannels().stream().filter(c -> c.getName().contains(event.getAuthor().getId())).findFirst().orElse(null);
                        if (guildchannel == null) {
                            System.out.println("Creating DM Channel " + event.getAuthor().getId());
                            guildchannel = mainGuild.createTextChannel("dm-" + event.getAuthor().getEffectiveName() + "-" + event.getAuthor().getId(), mainGuild.getCategoryById(924780998124798022L)).complete();
                        }
                        guildchannel.sendMessage(message).setAllowedMentions(Arrays.asList(MentionType.USER, MentionType.ROLE)).queue();
                    }
                    found = true;
                    if (report) {
                        MessageChannelUnion reportChannel = CommandManager.getModReportChannels(event, "reply");
                        if (reportChannel != null) {
                            if (!event.getGuild().getSelfMember().hasPermission(reportChannel.asGuildMessageChannel(), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)) {
                                event.getMessage().reply("I can not send reports to the report channel as I do not have permission to view or send messages in that channel.").queue();
                            }
                            else {
                                EmbedBuilder eb = new EmbedBuilder();
                                eb.setTitle("Reply Report");
                                eb.addField("User", event.getAuthor().getEffectiveName() + " (" + event.getAuthor().getId() + ")", false);
                                eb.addField("Channel", event.getChannel().getName() + " (" + event.getChannel().getId() + ")\n" + event.getMessage().getJumpUrl(), false);
                                eb.addField("Message", event.getMessage().getContentRaw(), false);
                                eb.addField("Reply", message, false);
                                eb.setFooter("Reply ID: " + ukey);
                                eb.setColor(Color.orange);
                                eb.setTimestamp(Instant.now());
                                Utils.sendEmbed(eb.build(), reportChannel);
                            }
                        }
                    }
                }
                DBConnectionManagerLum.closeRequest(rs);
                return found;
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to check replies", e, event.getChannel());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
