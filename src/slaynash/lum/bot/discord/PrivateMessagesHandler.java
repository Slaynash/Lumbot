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
            String channelName = ("dm-" + author.getName() + "-" + author.getDiscriminator() + "-" + author.getIdLong()).replaceAll("[!@#$%^`~&*()+=,./<>?;:'\"\\[\\]\\\\|{}]", "").replace("--", "-").replace(" ", "-").toLowerCase();
            TextChannel guildchannel = mainguild.getTextChannelsByName(channelName, true).stream().findFirst().orElse(null);
            String message = event.getMessage().getContentRaw();
            for (Attachment attachment : event.getMessage().getAttachments()) {
                message = message.concat("\n").concat(attachment.getUrl());
            }
            Profile profile = author.retrieveProfile().complete();
            MessageEmbed eb = new EmbedBuilder().setAuthor(author.getAsTag(), null, author.getAvatarUrl()).setDescription(author.getAsMention() + "\n\n" + message.trim()).setColor(profile.getAccentColor()).setImage(profile.getBannerUrl()).build();
            if (guildchannel == null) {
                StringBuilder sb = new StringBuilder();
                event.getPrivateChannel().getHistoryBefore(event.getMessage(), 100).complete().getRetrievedHistory().forEach(m -> {
                    sb.append(m.getTimeCreated()).append(" ").append(m.getAuthor().getAsTag()).append(": ").append(m.getContentRaw()).append(" ");
                    if (m.getAttachments().size() > 0) {
                        m.getAttachments().forEach(a -> sb.append(a.getUrl()).append(" "));
                    }
                    sb.append("\n");
                });
                if (sb.toString().isBlank())
                    mainguild.createTextChannel(channelName, mainguild.getCategoryById(924780998124798022L)).flatMap(tc -> tc.sendMessage("Mutuals:\n" + author.getMutualGuilds()).flatMap(ababa -> tc.sendMessageEmbeds(eb))).queue();
                else
                    mainguild.createTextChannel(channelName, mainguild.getCategoryById(924780998124798022L)).flatMap(tc -> tc.sendMessage("Mutuals:\n" + author.getMutualGuilds()).addFile(sb.toString().getBytes(), author.getName() + ".txt").flatMap(ababa -> tc.sendMessageEmbeds(eb))).queue();
                event.getMessage().addReaction(":Neko_cat_wave:851938087353188372").queue();
            }
            else {
                guildchannel.sendMessageEmbeds(eb).queue();
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
