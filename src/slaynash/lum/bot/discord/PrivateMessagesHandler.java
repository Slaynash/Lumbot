package slaynash.lum.bot.discord;

import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageSticker;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;

public class PrivateMessagesHandler {
    public static final String LOG_IDENTIFIER = "PrivateMessagesHandler";

    public static void handle(MessageReceivedEvent event) {

        if (event.getAuthor().getIdLong() != JDAManager.getJDA().getSelfUser().getIdLong()) {
            System.out.println(String.format("[DM] %s%s%s: %s",
                    event.getAuthor().getAsTag(),
                    event.getMessage().isEdited() ? " *edited*" : "",
                    event.getMessage().getType().isSystem() ? " *system*" : "",
                    event.getMessage().getContentRaw().replace("\n", "\n\t\t")));
            List<Attachment> attachments = event.getMessage().getAttachments();
            if (attachments.size() > 0) {
                System.out.println(attachments.size() + " Files");
                for (Attachment a : attachments)
                    System.out.println(" - " + a.getUrl());
            }
            if (ScamShield.checkForFishingPrivate(event)) {
                System.out.println("I was DM'd a Scam");
                return;
            }

            Guild mainguild = JDAManager.getJDA().getGuildById(633588473433030666L);
            User author = event.getAuthor();
            String channelName = ("dm-" + author.getName() + "-" + author.getDiscriminator() + "-" + author.getIdLong()).replaceAll("[!ǃ@#$%^`~&*()+=,./<>?;:'\"\\[\\]\\\\|{}]", "").replace("--", "-").replace(" ", "-").toLowerCase();
            TextChannel guildchannel = mainguild.getTextChannels().stream().filter(c -> c.getName().endsWith(author.getId())).findFirst().orElse(null);
            String message = author.getAsTag() + ":\n" + event.getMessage().getContentRaw();
            for (Attachment attachment : event.getMessage().getAttachments()) {
                message = message.concat("\n").concat(attachment.getUrl());
            }
            for (MessageSticker sticker : event.getMessage().getStickers()) {
                message = message.concat("\n").concat(sticker.getIconUrl());
            }
            if (message.length() > MessageEmbed.TEXT_MAX_LENGTH) {
                message = message.substring(0, MessageEmbed.TEXT_MAX_LENGTH);
            }
            if (guildchannel == null) {
                System.out.println("Creating DM Channel " + channelName);
                StringBuilder sb = new StringBuilder();
                event.getPrivateChannel().getHistoryBefore(event.getMessage(), 100).complete().getRetrievedHistory().forEach(m -> {
                    sb.append(m.getTimeCreated()).append(" ").append(m.getAuthor().getAsTag()).append(": ").append(m.getContentRaw()).append(" ");
                    m.getAttachments().forEach(a -> sb.append(a.getUrl()).append(" "));
                    m.getStickers().forEach(s -> sb.append(s.getIconUrl()).append(" "));
                    sb.append("\n");
                });
                final String finalMessage = message;
                if (sb.toString().isBlank())
                    mainguild.createTextChannel(channelName, mainguild.getCategoryById(924780998124798022L)).flatMap(tc -> tc.sendMessage(author.getAsMention() + "\n\n" + "Mutuals:\n" + author.getMutualGuilds()).flatMap(ababa -> tc.sendMessage(finalMessage))).queue();
                else
                    mainguild.createTextChannel(channelName, mainguild.getCategoryById(924780998124798022L)).flatMap(tc -> tc.sendMessage(author.getAsMention() + "\n\n" + "Mutuals:\n" + author.getMutualGuilds()).addFile(sb.toString().getBytes(), author.getName() + ".txt").flatMap(ababa -> tc.sendMessage(finalMessage))).queue();
                event.getMessage().addReaction(":Neko_cat_wave:851938087353188372").queue();
            }
            else {
                guildchannel.sendMessage(message).queue();
                event.getMessage().addReaction(":Neko_cat_okay:851938634327916566").queue();
            }
        }
        // CommandManager.runAsClient(event);
    }

    @SuppressWarnings("EmptyMethod")
    public static void handle(MessageUpdateEvent ignoredEvent) {
        //handle(new MessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));
    }

}
