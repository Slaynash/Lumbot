package slaynash.lum.bot.discord;

import java.awt.Color;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class MessageProxy {

    public static void fromDM(MessageReceivedEvent event) {
        Guild mainGuild = event.getJDA().getGuildById(JDAManager.mainGuildID);

        User author = event.getAuthor();
        String channelName = Junidecode.unidecode("dm-" + author.getName() + "-" + author.getDiscriminator() + "-" + author.getId()).toLowerCase()
                .replaceAll("[^a-z\\d\\-_]", "").replace(" ", "-").replace("--", "-");
        TextChannel guildchannel = mainGuild.getTextChannels().stream().filter(c -> c.getName().contains(author.getId())).findFirst().orElse(null);

        String message = author.getAsTag() + ":\n" + event.getMessage().getContentRaw();

        for (CustomEmoji emoji : event.getMessage().getMentions().getCustomEmojis()) {
            message = message.concat("\n").concat(emoji.getImageUrl());
        }
        for (StickerItem sticker : event.getMessage().getStickers()) {
            message = message.concat("\n").concat(sticker.getIconUrl());
        }
        if (message.length() > MessageEmbed.TEXT_MAX_LENGTH) {
            message = message.substring(0, MessageEmbed.TEXT_MAX_LENGTH);
        }
        if (guildchannel == null) {
            System.out.println("Creating DM Channel " + channelName);
            System.out.println("Number of Channels: " + mainGuild.getTextChannels().size());
            StringBuilder sb = new StringBuilder();
            event.getChannel().getHistoryBefore(event.getMessage(), 100).complete().getRetrievedHistory()
                    .forEach(m -> {
                        sb.append(m.getTimeCreated()).append(" ").append(m.getAuthor().getAsTag()).append(": ")
                                .append(m.getContentRaw()).append(" ");
                        m.getAttachments().forEach(a -> sb.append(a.getUrl()).append(" "));
                        m.getStickers().forEach(s -> sb.append(s.getIconUrl()).append(" "));
                        sb.append("\n");
                    });
            guildchannel = mainGuild.createTextChannel(channelName, mainGuild.getCategoryById(924780998124798022L)).complete();

            if (sb.toString().isBlank())
                guildchannel.sendMessage(author.getAsMention() + "\n\n" + "Mutuals:\n" + author.getMutualGuilds()).queue();
            else
                guildchannel.sendMessage(author.getAsMention() + "\n\n" + "Mutuals:\n" + author.getMutualGuilds()).addFiles(FileUpload.fromData(sb.toString().getBytes(), author.getName() + ".txt")).queue();

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
    }

    public static boolean fromDev(MessageReceivedEvent event) throws InterruptedException, ExecutionException {
        if (event.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong() &&
                event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */ &&
                event.getChannel().getName().toLowerCase().startsWith("dm-") &&
                !event.getMessage().getContentRaw().startsWith(".") &&
                !event.getMessage().getContentRaw().startsWith("l!")) {
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

            String message = author.getAsTag() + ":\n" + event.getMessage().getContentRaw();
            for (CustomEmoji emoji : event.getMessage().getMentions().getCustomEmojis()) {
                message = message.concat("\n").concat(emoji.getImageUrl());
            }
            for (StickerItem sticker : event.getMessage().getStickers()) {
                message = message.concat("\n").concat(sticker.getIconUrl());
            }
            if (message.length() > MessageEmbed.TEXT_MAX_LENGTH) {
                message = message.substring(0, MessageEmbed.TEXT_MAX_LENGTH);
            }

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

    private static void saveIDs(long ogID, long devID) {
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
            User author = event.getUser();
            TextChannel guildchannel = mainGuild.getTextChannels().stream().filter(c -> c.getName().contains(author.getId())).findFirst().orElse(null);
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
}
