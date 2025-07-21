package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class MessageLogger {

    public static void logMessage(MessageReceivedEvent event) {
        String messageId = event.getMessage().getId();
        String messageContent = event.getMessage().getContentRaw();
        String author = event.getAuthor().getId();
        String channel = null;
        String guild = null;
        OffsetDateTime timestamp = event.getMessage().getTimeCreated();
        Gson gson = new Gson();
        String attachmentJson = gson.toJson(event.getMessage().getAttachments().stream().map(Attachment::getUrl).toList());
        if (event.isFromGuild()) {
            channel = event.getChannel().getId();
            guild = event.getGuild().getId();
        }

        // Log the message details in SQL
        try {
            String sql = "INSERT INTO Messages (message_id, content, author_id, channel_id, guild_id, timestamp, attachments) VALUES (?, ?, ?, ?, ?, ?, ?)";
            DBConnectionManagerLum.sendUpdate(sql, messageId, messageContent, author, channel, guild, timestamp, attachmentJson);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to log message", e);
        }
    }

    public static void updateMessage(MessageUpdateEvent event) {
        String messageContent = event.getMessage().getContentRaw();
        String messageOldContent = null;
        long messageId = event.getMessage().getIdLong();
        long author = event.getAuthor().getIdLong();
        Long channel = null;
        Long guild = null;
        OffsetDateTime timestamp = event.getMessage().getTimeCreated();
        OffsetDateTime updateTS = event.getMessage().getTimeEdited();
        Gson gson = new Gson();
        String attachmentJson = gson.toJson(event.getMessage().getAttachments().stream().map(Attachment::getUrl).toList());
        if (event.isFromGuild()) {
            channel = event.getChannel().getIdLong();
            guild = event.getGuild().getIdLong();
        }

        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM Messages WHERE message_id = ?", messageId);
            if (rs.next()) {
                messageOldContent = rs.getString("content");
            }
            else {
                // Add the message to the database if it doesn't exist
                try {
                    String sql = "INSERT INTO Messages (message_id, content, author_id, channel_id, guild_id, timestamp, attachments) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    DBConnectionManagerLum.sendUpdate(sql, messageId, messageContent, author, channel, guild, timestamp, attachmentJson);
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("Failed to log message", e);
                }
            }
            DBConnectionManagerLum.closeRequest(rs);

            MessageChannelUnion report = CommandManager.getModReportChannels(guild, "message");
            if (report != null) {
                User user = event.getJDA().getUserById(author);
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Message Updated");
                embed.setColor(Color.decode("#337fd5"));
                embed.setAuthor(user.getEffectiveName(), null, user.getEffectiveAvatarUrl());
                embed.addField("User", user.getAsMention() + " | " + user.getEffectiveName(), false);
                if (messageOldContent == null) {
                    embed.addField("Content", "Message content was not cached.", false);
                }
                else if (messageOldContent.equals(messageContent)) {
                    // Likely embed updating, no need to report
                    return;
                }
                else {
                    String diff = getDiff(messageOldContent, messageContent);
                    if (diff.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
                        diff = diff.substring(0, MessageEmbed.DESCRIPTION_MAX_LENGTH - 3) + "...";
                    }
                    embed.setDescription(diff);
                }
                embed.addField("Date Posted", "<t:" + timestamp.toEpochSecond() + ":f>", false);
                embed.addField("Message Link", String.format("https://discord.com/channels/%d/%d/%d", guild, channel, messageId), false);
                embed.setTimestamp(Instant.now());
                if (event.getGuild().getSelfMember().hasPermission((GuildChannel) report, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                    report.sendMessageEmbeds(embed.build()).queue();
                }
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to check if message exists", e);
        }
        finally {
            System.out.println("Updating message: " + messageId + " | " + messageContent);
            // update the message log in SQL
            try {
                String sql = "UPDATE Messages SET content = ?, updateTS = ? WHERE message_id = ?";
                DBConnectionManagerLum.sendUpdate(sql, messageContent, updateTS, messageId);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed to update message", e);
            }
        }
    }

    public static void deletedMessage(MessageDeleteEvent event) {
        String messageId = event.getMessageId();

        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM Messages WHERE message_id = ?", messageId);
            if (rs.next()) {
                String guild = rs.getString("guild_id");
                if (guild != null) {
                    // Send update to Report channels
                    MessageChannelUnion report = CommandManager.getModReportChannels(Long.parseLong(guild), "message");
                    if (report != null) {
                        User user = event.getJDA().getUserById(rs.getString("author_id"));
                        if (user == null) {
                            user = event.getJDA().retrieveUserById(rs.getString("author_id")).complete();
                        }
                        if (user.isBot() || user.isSystem()) return; // Don't log bot or system messages
                        String channelId = rs.getString("channel_id");
                        String content = rs.getString("content");
                        String attachments = rs.getString("attachments");
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setTitle("Message Deleted");
                        embed.setColor(Color.decode("#ff470f"));
                        embed.setAuthor(user.getEffectiveName(), null, user.getEffectiveAvatarUrl());
                        embed.addField("User", user.getAsMention() + " | " + user.getEffectiveName(), false);
                        if (content == null) {
                            embed.addField("Content", "Message content was not cached.", false);
                        }
                        else if (content.isEmpty() && (attachments == null || attachments.length() <= 2)) {
                            embed.addField("Content", "Message content is Empty.", false);
                        }
                        else if (!content.isEmpty()) {
                            if (content.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
                                content = content.substring(0, MessageEmbed.DESCRIPTION_MAX_LENGTH - 3) + "...";
                            }
                            embed.setDescription(content);
                        }
                        embed.addField("Date Posted", "<t:" + rs.getTimestamp("timestamp").toInstant().getEpochSecond() + ":f>", false);
                        embed.addField("Message Link", String.format("https://discord.com/channels/%s/%s/%s", guild, channelId, messageId), false);
                        embed.setTimestamp(Instant.now());

                        if (attachments != null && attachments.length() > 2) {
                            long maxFileSize = event.getGuild().getBoostTier().getMaxFileSize();
                            List<FileUpload> mediaFiles = new java.util.ArrayList<>();
                            String[] attachmentList = new Gson().fromJson(attachments, String[].class);
                            for (String attachmentUrl : attachmentList) {
                                FileUpload media = fetchMedia(attachmentUrl, maxFileSize);
                                if (media != null) { // cdn is only available for a few seconds after deleted message
                                    mediaFiles.add(media);
                                }
                            }
                            // TODO: may look nicer to send the embed and files separately
                            report.sendMessageEmbeds(embed.build()).setFiles(mediaFiles).queue();
                        }
                        else {
                            report.sendMessageEmbeds(embed.build()).queue();
                        }
                    }
                }
                else {
                    System.out.println("Guild ID is null for message: " + messageId);
                }
            }
            else {
                // TODO: Maybe send log message for unknown message deletion
                System.out.println("Message not found in database: " + messageId);
            }
            DBConnectionManagerLum.closeRequest(rs);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to send message delete report", e);
        }
        finally {
            // delete the message log in SQL
            try {
                DBConnectionManagerLum.sendUpdate("DELETE FROM Messages WHERE message_id = ?", messageId);
            }
            catch (SQLException e) {
                ExceptionUtils.reportException("Failed to delete message", e);
            }
        }
    }

    private static String getDiff(String oldContent, String newContent) {
        DiffRowGenerator generator = DiffRowGenerator.create()
                .showInlineDiffs(true)
                .inlineDiffByWord(true)
                .mergeOriginalRevised(true)
                .oldTag(f -> "~~")
                .newTag(f -> "**")
                .build();
        List<DiffRow> rows = generator.generateDiffRows(
                        Arrays.asList(oldContent.split("\n")),
                        Arrays.asList(newContent.split("\n")));

        //combine all rows into one string
        StringBuilder sb = new StringBuilder();
        for (DiffRow row : rows) sb.append(row.getOldLine()).append("\n");

        return sb.toString();
    }

    private static FileUpload fetchMedia(String url, long maxSize) {
        // TODO: Check if the URL is a currently valid Discord attachment URL
        try {
            // Refresh the URL to get the latest version of the attachment
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString("{\"attachment_urls\":[\"" + url + "\"]}"))
                    .uri(URI.create("https://discord.com/api/v10/attachments/refresh-urls"))
                    .setHeader("Authorization", "Bot " + ConfigManager.discordToken)
                    .setHeader("Content-Type", "application/json")
                    .header("User-Agent", "LUM Bot " + ConfigManager.commitHash)
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<byte[]> response = Utils.downloadRequest(request, "MessageLogger Media Refresh");
            String refreshedURL = JsonParser.parseString(new String(response.body()))
                    .getAsJsonObject()
                    .getAsJsonArray("refreshed_urls")
                    .get(0)
                    .getAsJsonObject()
                    .get("refreshed")
                    .getAsString();

            // TODO: Check if the refreshed URL is a valid Discord attachment URL

            //download the file from the refreshed URL
            HttpRequest fileRequest = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(refreshedURL))
                    .setHeader("User-Agent", "LUM Bot " + ConfigManager.commitHash)
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<InputStream> fileResponse = Utils.downloadRequestIS(fileRequest, "MessageLogger Media Download");

            byte[] bytes = fileResponse.body().readAllBytes();

            if (bytes.length > maxSize) {
                System.err.println("File size exceeds the maximum allowed size of " + maxSize + " bytes. File size: " + bytes.length + " bytes");
                return null; // Return null if the file is too big
            }

            String fileName = refreshedURL.substring(refreshedURL.lastIndexOf('/') + 1);
            // also remove the query parameters if they exist
            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf('?'));
            }

            return FileUpload.fromData(bytes, fileName).asSpoiler();
        }
        catch (Exception e) {
            System.err.println("Failed to fetch media from URL: " + url);
            ExceptionUtils.reportException("Failed to fetch media for Log", e);
            return null;
        }
    }
}
