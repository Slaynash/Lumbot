package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerShortUrls;

public class Main extends ListenerAdapter {
    public static JDA jda;

    public static void main(String[] args) throws LoginException, IllegalArgumentException, InterruptedException, RateLimitedException {
        TrustManager[] trustAllCertificates = new TrustManager[]{new X509TrustManager(){
            @Override public X509Certificate[] getAcceptedIssuers() { return null; }
            @Override public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            @Override public void checkServerTrusted(X509Certificate[] certs, String authType) { }
        }};
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCertificates, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        catch (GeneralSecurityException e) {
            throw new ExceptionInInitializerError(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            JDA jda = JDAManager.getJDA();
            if (jda != null && jda.getSelfUser().getIdLong() == 275759980752273418L) // Lum (blue)
            jda
                .getGuildById(633588473433030666L)
                .getTextChannelById(808076226064941086L)
                .sendMessage(JDAManager.wrapMessageInEmbed("Lum is shutting down", Color.orange))
                .complete();
        }));

        ConfigManager.init();

        DBConnectionManagerShortUrls.init();
        
        loadLogchannelList();
        loadVerifychannelList();
        loadReactionsList();
        loadNameBlacklist();
        loadMelonLoaderVersions();
        loadMLHashes();
        loadMLVRCHash();
        loadMLReportChannels();
        //loadBrokenVRCMods();
        loadVRCBuild();
        MelonLoaderScanner.Init();
        
        CommandManager.init();
        JDAManager.init(ConfigManager.discordToken);
        
        JDAManager.getJDA().getPresence().setActivity(Activity.watching("melons getting loaded"));

        if (JDAManager.getJDA().getSelfUser().getIdLong() == 275759980752273418L) // Lum (blue)
            JDAManager.getJDA()
                .getGuildById(633588473433030666L)
                .getTextChannelById(808076226064941086L)
                .sendMessage(JDAManager.wrapMessageInEmbed("Lum restarted successfully !", Color.green))
                .queue();

        VRCApiVersionScanner.init();
        
        System.out.println("LUM Started!");
    }

    private static void loadReactionsList() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("rolereactions.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 4);
                if(parts.length == 3 && parts[0].matches("^\\d+$") && parts[2].matches("^\\d+$")) {
                    CommandManager.reactionListeners.add(new ReactionListener(parts[0], parts[1], parts[2]));
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void loadMLHashes() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("mlhashes.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.equals("")) {
                    String[] parts = line.split(" ", 3);
                    if (parts[0].equals("r"))
                        CommandManager.melonLoaderHashes.add(new MLHashPair(parts[1], parts[2]));
                    else
                        CommandManager.melonLoaderAlphaHashes.add(new MLHashPair(parts[1], parts[2]));

                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadMLVRCHash() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("mlvrchash.txt"));
            CommandManager.melonLoaderVRCHash = reader.readLine();
            CommandManager.melonLoaderVRCMinDate = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
    private static void loadBrokenVRCMods() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("brokenvrcmods.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().equals(""))
                    CommandManager.brokenVrchatMods.add(line.trim());
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    */
    private static void loadVRCBuild() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("vrchatbuild.txt"));
            String line = reader.readLine();
            if (line != null)
                CommandManager.vrchatBuild = line.trim();
            else
                CommandManager.vrchatBuild = "1";
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void loadMLReportChannels() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("mlreportchannels.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 4);
                if(parts.length == 2 && parts[0].matches("^\\d+$") && parts[1].matches("^\\d+$")) {
                    CommandManager.mlReportChannels.put(Long.parseLong(parts[0]), parts[1]);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void loadNameBlacklist() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("nameblacklist.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().equals(""))
                    CommandManager.blacklistedNames.add(line.trim());
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void loadLogchannelList() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("logchannels.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 4);
                if(parts.length == 2 && parts[0].matches("^\\d+$") && parts[1].matches("^\\d+$")) {
                    CommandManager.logChannels.put(Long.parseLong(parts[0]), parts[1]);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void loadVerifychannelList() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("verifychannels.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 5);
                if(parts.length == 3 && parts[0].matches("^\\d+$") && parts[1].matches("^\\d+$") && parts[2].matches("^\\d+$")) {
                    CommandManager.verifyChannels.put(Long.parseLong(parts[0]), new VerifyPair(parts[1], parts[2]));
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void loadMelonLoaderVersions() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("melonloaderversions.txt"));
            MelonLoaderScanner.latestMLVersionRelease = reader.readLine().trim();
            MelonLoaderScanner.latestMLVersionBeta = reader.readLine().trim();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            PrivateMessagesHandler.handle(event);
        } else {
            ServerMessagesHandler.handle(event);
        }
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        //System.out.println("[" + event.getGuild().getName() + "] [#" + event.getChannel().getName() + "] " + event.getUser().getName() + " reacted with " + event.getReactionEmote().getName() + "(isEmote: " + event.getReactionEmote().isEmote() + ")");
        for(ReactionListener rl : CommandManager.reactionListeners) {
            if(event.getMessageId().equals(rl.messageId) && (event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getId().equals(rl.emoteId) : event.getReactionEmote().getName().equals(rl.emoteId))) {
                Role role = event.getGuild().getRoleById(rl.roleId);
                if(role != null) {
                    event.getGuild().addRoleToMember(event.getMember(), role).queue();
                    WriteLogMessage(event.getGuild(), "Added role `" + role.getName() + "` to <@" + event.getUser().getId() + ">");
                }
                return;
            }
        }
    }
    
    private void WriteLogMessage(Guild guild, String message) {
        String channelId = null;
        if((channelId = CommandManager.logChannels.get(guild.getIdLong())) != null) {
            for(TextChannel c : guild.getTextChannels()) {
                if(c.getId().equals(channelId)) {
                    ((TextChannel)c).sendMessage(JDAManager.wrapMessageInEmbed(message, Color.gray)).queue();
                    break;
                }
            }
        }
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        for(ReactionListener rl : CommandManager.reactionListeners) {
            if(event.getMessageId().equals(rl.messageId) && (event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getId().equals(rl.emoteId) : event.getReactionEmote().getName().equals(rl.emoteId))) {
                Role role = event.getGuild().getRoleById(rl.roleId);
                if(role != null) {
                    event.getGuild().removeRoleFromMember(event.getUserId(), role).queue();
                    WriteLogMessage(event.getGuild(), "Removed role `" + role.getName() + "` from <@" + event.getUserId() + ">");
                }
                return;
            }
        }
    }
    
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        
        if(event.getGuild().getIdLong() == 439093693769711616L || event.getGuild().getIdLong() == 663449315876012052L || event.getGuild().getIdLong() == 600298024425619456L) {
            String name = event.getUser().getName();
            for (String s : CommandManager.blacklistedNames) {
                if (name.matches(".*\\Q" + s + "\\E.*")) {
                    event.getGuild().ban(event.getMember(), 0, "Blacklisted username").queue();
                    event.getGuild().getDefaultChannel().sendMessage("Automatically banned blacklisted username " + event.getMember().getNickname());
                    return;
                }
            }
        }
        
        if(event.getUser().isBot())
            return;
        
        if(event.getGuild().getId().equals("446646432339066912")) {
            event.getUser().openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Welcome to the MetrixVR discord !\nPlease make sure to read the <#446646432339066916> channel and react with :white_check_mark: to see every channels.\nIf you need some help, you can ask in the #help-requests channel.\n\nTo play the game, you will need a game account. You can create one at <https://metrixvr.net/register>.\nYou can ask for a key in the #🔑key-requests🔑 channel. Every keys will be sent at the same time as the next update.\nPlease read the latest #announcements for more infos!\n\nHave a good day\n - Slaynash").queue();
            });
        }
        if(event.getGuild().getId().equals("439093693769711616")) {
            event.getUser().openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Welcome to the VRChat Modding Group discord !\nPlease make sure to read the <#721966647228891227> channel and react with :white_check_mark: to see every channels.\nIf you need some help, you can ask in the #help-requests channel.\n\nHave a good day\n - Slaynash").queue();
            });
        }
        if(event.getGuild().getId().equals("398382180608507904")) {
            event.getUser().openPrivateChannel().queue((channel) -> {
                channel.sendMessage("Bienvenue sur le serveur **VRChat Communauté Francophone** ! Nous te prions tout d'abord de lire le règlement dans le salon <#399022887631323148> afin de connaître le bon fonctionnement du serveur et pour ne pas t'y perdre. (N'oublie pas de choisir un rôle :wink:)\nSur ce, amuse toi bien et bon jeu !").queue();
            });
        }
    }

}
