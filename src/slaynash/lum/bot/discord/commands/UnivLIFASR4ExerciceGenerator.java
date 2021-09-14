package slaynash.lum.bot.discord.commands;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.time.Duration;
import java.util.Base64;
import java.util.Random;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.utils.ExceptionUtils;

public class UnivLIFASR4ExerciceGenerator extends Command {

    Random rand = new Random();

    private static final HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        if (!checkPerms(paramMessageReceivedEvent))
            return;
        // ₁₂₃₄₅₆₇₈₉

        long uuid = rand.nextLong();

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://liris.cnrs.fr/vincent.nivoliers/suzette.php?exo=binconv&query=create&ticket=" + uuid))
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
            paramMessageReceivedEvent.getChannel().sendMessage("Une erreur s'est produite pendant la récupération de l'exercice").queue();
            return;
        }

        System.out.println("cnrs returned: " + data);

        String imageDataStr = data.split("\"data\": \"", 2)[1].split("\"}", 2)[0];
        byte[] imageData = Base64.getDecoder().decode(imageDataStr);

        paramMessageReceivedEvent.getChannel().sendFile(imageData, "exercice_" + uuid + ".png").queue();
    }

    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals(getName());
    }

    @Override
    public String getName() {
        return "l!exolifasr4";
    }

    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return checkPerms(event);
    }

    @Override
    public String getHelpDescription() {
        return "Send a LIFASR4 (UCBL University Class) exercice";
    }



    private boolean checkPerms(MessageReceivedEvent event) {
        return event.getGuild().getIdLong() == 624635229222600717L || event.getAuthor().getIdLong() == 145556654241349632L;
    }
}
