package slaynash.lum.bot.discord;

import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.User.Profile;
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
            String channelName = ("dm-" + author.getAsTag() + "-" + author.getIdLong()).replaceAll("[!@#$%^&*()+=/]", "").toLowerCase();
            TextChannel guildchannel = mainguild.getTextChannelsByName(channelName, true).stream().findFirst().orElse(null);
            String message = event.getMessage().getContentRaw();
            for (Attachment attachment : event.getMessage().getAttachments()) {
                message = message.concat("\n").concat(attachment.getUrl());
            }
            Profile profile = author.retrieveProfile().complete();
            MessageEmbed eb = new EmbedBuilder().setAuthor(author.getAsTag(), null, author.getAvatarUrl()).setDescription(author.getAsMention() + "\n\n" + message.trim()).setColor(profile.getAccentColor()).setImage(profile.getBannerUrl()).build();
            if (guildchannel == null) {
                mainguild.createTextChannel(channelName, mainguild.getCategoryById(924780998124798022L)).flatMap(tc -> tc.sendMessageEmbeds(eb)).queue();
            }
            else {
                guildchannel.sendMessageEmbeds(eb).queue();
            }
        }
        // CommandManager.runAsClient(event);
    }

    @SuppressWarnings("EmptyMethod")
    public static void handle(MessageUpdateEvent event) {
        //handle(new MessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));
    }
}
