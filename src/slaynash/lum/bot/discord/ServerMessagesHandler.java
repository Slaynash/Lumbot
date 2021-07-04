package slaynash.lum.bot.discord;

import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.melonscanner.MelonScanner;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.TimeManager;

public class ServerMessagesHandler {


    private static String fileExt;

    private static final HttpRequest request = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create("https://icanhazdadjoke.com/"))
        .setHeader("User-Agent", "LUM Bot (https://discord.gg/akFkAG2)")
        .setHeader("Accept", "text/plain")
        .timeout(Duration.ofSeconds(20))
        .build();
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(Redirect.ALWAYS)
        .build();

    public static void handle(MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;
            CommandManager.runAsServer(event);
            long guildID = event.getGuild().getIdLong();
            String guildIDstr = event.getGuild().getId();
            boolean[] guildConfig;
            guildConfig = GuildConfigurations.configurations.get(guildID) == null ? new boolean[GuildConfigurations.ConfigurationMap.values().length] : GuildConfigurations.configurations.get(guildID);
            String message = event.getMessage().getContentStripped().toLowerCase();
            boolean hasLum = message.matches(".*\\blum\\b.*");
            List<Attachment> attachments = event.getMessage().getAttachments();

            System.out.printf("[%s] [%s][%s] %s: %s\n",
                    TimeManager.getTimeForLog(),
                    event.getGuild().getName(),
                    event.getTextChannel().getName(),
                    event.getAuthor().getName(),
                    event.getMessage().getContentRaw());

            if (guildConfig[GuildConfigurations.ConfigurationMap.GENERALLOGREMOVER.ordinal()] && (event.getChannel().getName().toLowerCase().contains("general") || (event.getMessage().getCategory() != null && event.getMessage().getCategory().getIdLong() == 705284406561996811L/*emm high-tech*/)) && attachments.size() > 0 && MelonScanner.isValidFileFormat(attachments.get(0)) && !checkIfStaff(event)) {
                String mess = "<@!" + event.getMessage().getMember().getId() + "> ";
                switch (guildIDstr) {
                    case "600298024425619456": //emmVRC
                        mess.concat("Please reupload this log to <#600661924010786816> instead.");
                        break;
                    case "439093693769711616": //VRCMG
                        mess.concat("Please reupload this log to <#440088207799877634> instead.");
                        break;
                    case "663449315876012052": //MelonLoader
                        mess.concat("Please reupload this log to <#733305093264375849> instead.");
                        break;
                    case "563139253542846474": //BoneWorks
                        mess.concat("Please reupload this log to <#675024565277032449> instead.");
                        break;
                    case "322211727192358914": //TLDModding
                        mess.concat("Please reupload this log to <#827601339672035408> instead.");
                        break;
                    default:
                        mess.concat("Please reupload this log to #help-and-support or #log-scanner channel instead.");
                        break;
                }
                event.getChannel().sendMessage(mess).queue();
                event.getMessage().delete().queue();
            }
            else {
                new Thread(() -> {
                    try {
                        MelonScanner.scanMessage(event);
                    }
                    catch (Exception e) {
                        ExceptionUtils.reportException("An error has occured while reading logs:", e, event.getTextChannel());
                    }
                }).start();
            }

            if (guildConfig[GuildConfigurations.ConfigurationMap.SCAMSHIELD.ordinal()] && ScamShield.checkForFishing(event))
                return;

            if (guildConfig[GuildConfigurations.ConfigurationMap.DLLREMOVER.ordinal()] && !checkDllPostPermission(event)) {
                event.getMessage().delete().queue();
                event.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("<@!" + event.getMessage().getMember().getId() + "> tried to post a " + fileExt + " file which is not allowed." + (fileExt.equals("dll") ? "\nPlease only download mods from trusted sources." : ""), Color.YELLOW)).queue();
                return;
            }

            if (guildID == 663449315876012052L /* MelonLoader */) {
                String messageLowercase = event.getMessage().getContentRaw().toLowerCase();
                if (messageLowercase.contains("melonclient") || messageLowercase.contains("melon client") || messageLowercase.contains("tlauncher"))
                    event.getMessage().reply("This discord is about MelonLoader, a mod loader for Unity games. If you are looking for a Client, you are in the wrong Discord.").queue();
            }

            if (event.getAuthor().getIdLong() == 381571564098813964L) // Miku Hatsune#6969
                event.getMessage().addReaction(":baka:828070018935685130").queue(); // was requested

            if (guildConfig[GuildConfigurations.ConfigurationMap.PARTIALLOGREMOVER.ordinal()] && (message.contains("[error]") || message.contains("developer:") || message.contains("[internal failure]"))) {
                System.out.println("Partial Log was printed");

                boolean postedInWhitelistedServer = false;
                for (long whitelistedGuildId : GuildConfigurations.whitelistedRolesServers.keySet()) {
                    if (whitelistedGuildId == guildID) {
                        postedInWhitelistedServer = true;
                        break;
                    }
                }
                if (postedInWhitelistedServer && !checkIfStaff(event)) {
                    event.getChannel().sendMessage("<@!" + event.getMessage().getMember().getId() + "> Please upload your `MelonLoader/Latest.log` instead of printing parts of it.\nIf you are unsure how to locate your Latest.log file, use the `!log` command in this channel.").queue();
                    event.getMessage().delete().queue();
                }
            }

            if (guildConfig[GuildConfigurations.ConfigurationMap.LUMREPLIES.ordinal()] && ChattyLum.handle(message, event))
                return;

            Long category = event.getMessage().getCategory() == null ? 0L : event.getMessage().getCategory().getIdLong();
            if (guildID == 600298024425619456L/*emmVRC*/ && category != 765058331345420298L/*Tickets*/ && category != 801137026450718770L/*Mod Tickets*/ && category != 600914209303298058L/*Staff*/ && message.matches("(.*\\b(forgot|forget|reset|lost).*) (.*\\b(pin|password)\\b.*)|(.*\\b(pin|password)\\b.*) (.*\\b(forgot|forget|reset|lost).*)")) {
                System.out.println("Forgot pin asked");
                event.getMessage().reply("Please create a new ticket in <#765785673088499752>. Thank you!").queue();
                return;
            }
            if (guildID == 600298024425619456L/*emmVRC*/ && category != 765058331345420298L/*Tickets*/ && category != 801137026450718770L/*Mod Tickets*/ && category != 600914209303298058L/*Staff*/ && message.matches("(.*\\b(disable|off|out)\\b.*) (.*\\bstealth\\b.*)|(.*\\bstealth\\b.*) (.*\\b(disable|off|out)\\b.*)")) {
                System.out.println("Stealth mode asked");
                event.getMessage().reply("To disable Stealth Mode, click the Report World button in your quick menu. From there, you can access emmVRC Functions. You'll find the Stealth Mode toggle on the 4th page.").queue();
                return;
            }

            if (message.startsWith("!vrcuk") || message.startsWith("!cuck")) {
                System.out.println("VRChatUtilityKit print");
                event.getChannel().sendMessage("Please download https://api.vrcmg.com/v0/mods/231/VRChatUtilityKit.dll and put it in your Mods folder.").queue();
                return;
            }

            if (message.startsWith("!log")) {
                System.out.println("logs printed");
                String sendMessage;
                if (guildID == 835185040752246835L || guildID == 322211727192358914L) /*TLD*/
                    sendMessage = "How to find your Log file\n\nFor **MelonLoader v0.3.0** and above:\n- go to your game's root folder. It's the folder that contains your `Mods` folder\n- open the `MelonLoader` folder\n- find the file called `Latest.log`\n- drag and drop that file into Discord\n" +
                        "\nFor **MelonLoader v0.2.7.4** and lower:\n- go to your game's root folder. It's the folder that contains your `Mods` folder\n- open the `Logs` folder\n- it will have a bunch of log files inside\n- drag and drop the newest one into Discord";
                else
                    sendMessage = "To find your Log file, navigate to your game's root directory. The path should be something like this:\n**Steam**: `C:\\Program Files (x86)\\Steam\\steamapps\\common\\(game)`\n**Oculus**: `C:\\Oculus Apps\\Software\\(game)-(game)`\n\nAlternatively, you could find it through the launcher you are using:\n**Steam**: `Steam Library > right-click (game) > Manage > Browse local files`\n**Oculus**: `Oculus Library > ••• > Details > Copy location to Clipboard`." +
                        " Open File Explorer and paste it into the directory bar (or manually navigate to it).\n\nFor MelonLoader v0.3.0 and above, navigate to the `MelonLoader` folder, then drag and drop `Latest.log` into Discord.\nFor MelonLoader v0.2.7.4 and lower, open the `Logs` folder, then drag and drop the latest MelonLoader log file into Discord.";
                event.getChannel().sendMessage(sendMessage).queue();
                return;
            }

            if (message.startsWith("!uix")) {
                System.out.println("UIX printed");
                event.getChannel().sendMessage("Please download https://api.vrcmg.com/v0/mods/55/UIExpansionKit.dll and put it in your Mods folder.").queue();
                return;
            }

            if (message.startsWith("!vrcx")) {
                System.out.println("VRCX printed");
                event.getChannel().sendMessage("VRCX is not a mod and you can find it here: <https://github.com/Natsumi-sama/VRCX>").queue();
                return;
            }

            if (hasLum && guildConfig[GuildConfigurations.ConfigurationMap.DADJOKES.ordinal()] && message.contains("joke")) {
                System.out.println("Requested a joke");
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                event.getChannel().sendMessage(response.body()).queue();
                return;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("An error has occured processing message:", e, event.getTextChannel());
        }
    }

    /**
     * Check if the message is posted in a guild using a whitelist and if it contains a DLL.
     * @param event
     * @return true if the message is posted in a guild using a whitelist, contains a DLL attachment, and isn't posted by a whitelisted user
     */
    private static boolean checkDllPostPermission(MessageReceivedEvent event) {
        long guildId = event.getGuild().getIdLong();
        boolean postedInWhitelistedServer = false;
        for (long whitelistedGuildId : GuildConfigurations.whitelistedRolesServers.keySet()) {
            if (whitelistedGuildId == guildId) {
                postedInWhitelistedServer = true;
                break;
            }
        }

        if (!postedInWhitelistedServer)
            return true; // Not a whitelisted server

        for (Attachment attachment : event.getMessage().getAttachments()) {
            fileExt = attachment.getFileExtension();
            if (fileExt == null) fileExt = "";
            fileExt = fileExt.toLowerCase();

            if (fileExt.equals("dll") || fileExt.equals("exe") || fileExt.equals("zip") || fileExt.equals("7z") ||
                fileExt.equals("rar") || fileExt.equals("unitypackage") || fileExt.equals("vrca") || fileExt.equals("fbx")) {

                if (checkIfStaff(event))
                    return true;

                return false; // The sender isn't allowed to send a DLL file
            }
        }
        return true; // No attachement, or no DLL
    }

    /**
     * Check if sender is part of Guild Staff/Trusted.
     * @param event
     * @return true if sender really was Guild Staff/Trusted
     */
    public static boolean checkIfStaff(MessageReceivedEvent event) {
        for (Entry<Long, long[]> whitelistedRolesServer : GuildConfigurations.whitelistedRolesServers.entrySet()) {
            Guild targetGuild;
            Member serverMember;
            if ((targetGuild = event.getJDA().getGuildById(whitelistedRolesServer.getKey())) != null &&
                (serverMember = targetGuild.getMember(event.getAuthor())) != null) {
                List<Role> roles = serverMember.getRoles();
                for (Role role : roles) {
                    long roleId = role.getIdLong();
                    for (long whitelistedRoleId : whitelistedRolesServer.getValue()) {
                        if (whitelistedRoleId == roleId) {
                            return true; // The sender is whitelisted
                        }
                    }
                }
            }
        }
        return false;
    }

}
