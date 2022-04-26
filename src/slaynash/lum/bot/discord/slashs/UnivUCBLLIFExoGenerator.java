package slaynash.lum.bot.discord.slashs;

import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Random;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class UnivUCBLLIFExoGenerator {

    Random rand = new Random();

    InteractionHook interactionhook;

    private static final HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    public void onCommand(SlashCommandEvent event) {
        // ₁₂₃₄₅₆₇₈₉

        event.deferReply().queue(success -> interactionhook = success);

        try {
            String exo = event.getOptions().get(0).getAsString();
            String ticket = event.getOptions().size() > 1 ? event.getOptions().get(1).getAsString() : null;
            String subcommandname = event.getSubcommandName();
            System.out.println(event.getUser().getAsTag() + "exo: " + exo + " subcommandname: " + subcommandname + " ticket: " + ticket);
            Pattern p = Pattern.compile("([^\\w\\-!.~'\\(\\)*])");
            Matcher m = p.matcher(exo);
            if (m.find()) {
                event.reply("Invalid character in exo: " + m.group(0)).setEphemeral(true).queue();
                return;
            }
            if (ticket != null) {
                Matcher m2 = p.matcher(ticket);
                if (m2.find()) {
                    event.reply("Invalid character in ticket: " + m2.group(0)).setEphemeral(true).queue();
                    return;
                }
            }

            if (!subcommandname.equals("create") && !subcommandname.equals("solve")) {
                if (interactionhook != null)
                    interactionhook.sendMessage("Nom de sous-commande invalide: " + event.getSubcommandName()).queue();
                else
                    event.getChannel().sendMessage("Nom de sous-commande invalide: " + event.getSubcommandName()).queue();
                return;
            }
            else if (ticket != null && ticket.contains(" ")) {
                if (interactionhook != null)
                    interactionhook.sendMessage("Le ticket ne peut pas avoir d'espace").queue();
                else
                    event.getChannel().sendMessage("Le ticket ne peut pas avoir d'espace").queue();
                return;
            }

            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://liris.cnrs.fr/vincent.nivoliers/suzette.php?exo=" + exo + "&query=" + subcommandname + (ticket != null ? ("&ticket=" + ticket) : "")))
                .setHeader("User-Agent", "LUM Bot")
                .timeout(Duration.ofSeconds(30))
                .build();

            String data;

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                data = response.body();
            }
            catch (IOException | InterruptedException e) {
                ExceptionUtils.reportException("Request failed for from liris.cnrs.fr", e);
                event.getChannel().sendMessage("Une erreur s'est produite pendant la récupération de l'exercice").queue();
                return;
            }

            System.out.println("cnrs returned: " + data);

            String ticketReturned = data.split("\"ticket\": \"", 2)[1].split("\",", 2)[0];
            String imageDataStr = data.split("\"data\": \"", 2)[1].split("\"}", 2)[0];
            byte[] imageData = Base64.getDecoder().decode(imageDataStr);

            if (interactionhook != null)
                interactionhook.sendMessage("Ticket: " + ticketReturned).addFile(imageData, "exercice_" + ticketReturned + ".png").queue();
            else
                event.getChannel().sendMessage("Ticket: " + ticketReturned).addFile(imageData, "exercice_" + ticketReturned + ".png").queue();
        }
        catch (Exception e) {
            ExceptionUtils.reportException("UCBLLIF command failed", e);
            if (interactionhook != null)
                interactionhook.sendMessageEmbeds(Utils.wrapMessageInEmbed("La commande a échouée. Cette erreur a été rapporté aux développeurs.", Color.RED)).queue();
        }
    }
}
