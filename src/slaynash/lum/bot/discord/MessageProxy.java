package slaynash.lum.bot.discord;

import java.awt.Color;
import java.util.concurrent.ExecutionException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.gcardone.junidecode.Junidecode;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;
import slaynash.lum.bot.utils.Utils;

public class MessageProxy {

    public static void fromDM(MessageReceivedEvent event) {
        Guild mainGuild = event.getJDA().getGuildById(JDAManager.mainGuildID);

        User author = event.getAuthor();
        String channelName = Junidecode.unidecode("dm-" + author.getName() + "-" + author.getDiscriminator() + "-" + author.getId()).toLowerCase()
                .replaceAll("[^a-z\\d\\-_]", "").replace(" ", "-").replace("--", "-");
        TextChannel guildchannel = mainGuild.getTextChannels().stream().filter(c -> c.getName().contains(author.getId())).findFirst().orElse(null);

        String message = author.getAsTag() + ":\n" + event.getMessage().getContentRaw();
        for (Attachment attachment : event.getMessage().getAttachments()) {
            message = message.concat("\n").concat(attachment.getUrl());
            if (MelonScanner.isValidFileFormat(attachment, false)) {
                event.getMessage().reply(
                        "Sorry, I do not scan logs in DMs. Please use a server that has MelonLoader log scans enabled.")
                        .queue();
                if (guildchannel != null)
                    guildchannel.sendMessage(
                            "Sorry, I do not scan logs in DMs. Please use a server that has MelonLoader log scans enabled.")
                            .queue();
                return;
            }
        }
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
            final String finalMessage = message;
            if (sb.toString().isBlank())
                mainGuild.createTextChannel(channelName, mainGuild.getCategoryById(924780998124798022L))
                        .flatMap(tc -> tc
                                .sendMessage(author.getAsMention() + "\n\n" + "Mutuals:\n" + author.getMutualGuilds())
                                .flatMap(ababa -> tc.sendMessage(finalMessage)))
                        .queue();
            else
                mainGuild.createTextChannel(channelName, mainGuild.getCategoryById(924780998124798022L))
                        .flatMap(tc -> tc
                                .sendMessage(author.getAsMention() + "\n\n" + "Mutuals:\n" + author.getMutualGuilds())
                                .addFiles(FileUpload.fromData(sb.toString().getBytes(), author.getName() + ".txt"))
                                .flatMap(ababa -> tc.sendMessage(finalMessage))
                        )
                        .queue();
            event.getMessage().addReaction(Emoji.fromCustom("Neko_cat_wave", 851938087353188372L, false)).queue();
        }
        else {
            guildchannel.sendMessage(message).queue();
            event.getMessage().addReaction(Emoji.fromCustom("Neko_cat_okay", 851938634327916566L, false)).queue();
        }
    }

    public static boolean fromDev(MessageReceivedEvent event) throws InterruptedException, ExecutionException {
        if (event.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong() &&
                event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */ &&
                event.getChannel().getName().toLowerCase().startsWith("dm-") &&
                !event.getMessage().getContentRaw().startsWith(".")) {
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
            MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
            messageBuilder.setContent(message.getContentRaw());
            for (Attachment attachment : message.getAttachments()) {
                if (attachment.getSize() > 8000000) {
                    messageBuilder.setContent(messageBuilder.getContent().concat("\n").concat(attachment.getUrl()));
                }
                else {
                    messageBuilder.addFiles(FileUpload.fromData(attachment.getProxy().download().get(), attachment.getFileName()));
                }
            }
            for (StickerItem sticker : event.getMessage().getStickers()) {
                messageBuilder.setContent(messageBuilder.getContent() + "\n" + sticker.getIconUrl());
            }
            user.openPrivateChannel().queue(
                    channel -> channel.sendMessage(messageBuilder.build()).queue(null,
                            e -> Utils.sendEmbed("Failed to send message to target user: " + e.getMessage(), Color.red, event)),
                    error -> Utils.sendEmbed("Failed to open DM with target user: " + error.getMessage(), Color.red, event));

            return true;
        }
        return false;
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
        else if (event.getGuild().getIdLong() == 633588473433030666L /* Slaynash's Workbench */ && event.getChannel().asTextChannel().getParentCategory() != null && event.getChannel().asTextChannel().getParentCategory().getIdLong() == 924780998124798022L) {
            event.getUser().openPrivateChannel().queue(
                channel -> channel.sendTyping().queue(null,
                        e -> event.getChannel().asTextChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Can not send message to target user: " + e.getMessage(), Color.red)).queue()),
                error -> event.getChannel().asTextChannel().sendMessageEmbeds(Utils.wrapMessageInEmbed("Can not open DM with target user: " + error.getMessage(), Color.red)).queue());
        }

    }

}
