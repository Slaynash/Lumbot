package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.commands.AddReactionHandlerCommand;
import slaynash.lum.bot.discord.commands.CommandLaunchCommand;
import slaynash.lum.bot.discord.commands.HelpCommand;
import slaynash.lum.bot.discord.commands.LockDown;
import slaynash.lum.bot.discord.commands.MLBrokenModsCommand;
import slaynash.lum.bot.discord.commands.MLHashRegisterCommand;
import slaynash.lum.bot.discord.commands.MLSetMinForVRC;
import slaynash.lum.bot.discord.commands.Purge;
import slaynash.lum.bot.discord.commands.RankColorCommand;
import slaynash.lum.bot.discord.commands.RubybotOverDynobotCommand;
import slaynash.lum.bot.discord.commands.SetEmmHash;
import slaynash.lum.bot.discord.commands.SetLogChannelHandlerCommand;
import slaynash.lum.bot.discord.commands.SetMLReportChannelCommand;
import slaynash.lum.bot.discord.commands.SetScreeningRoleHandlerCommand;
import slaynash.lum.bot.discord.commands.SetVRCBuild;
import slaynash.lum.bot.discord.commands.ThawServer;
import slaynash.lum.bot.discord.commands.VerifyChannelHandlerCommand;
import slaynash.lum.bot.discord.commands.VerifyCommandCommand;
import slaynash.lum.bot.discord.melonscanner.MLHashPair;

public class CommandManager {
    private static List<Command> commands = new ArrayList<Command>();
    private static boolean init = false;
    
    public static List<ReactionListener> reactionListeners = new ArrayList<>();
    public static Map<Long, String> logChannels = new HashMap<>();
    public static Map<Long, VerifyPair> verifyChannels = new HashMap<>();
    public static Map<Long, Long> autoScreeningRoles = new HashMap<>();
    
    public static List<MLHashPair> melonLoaderHashes = new ArrayList<>();
    public static List<MLHashPair> melonLoaderAlphaHashes = new ArrayList<>();
    public static Map<Long, String> mlReportChannels = new HashMap<>();
    public static List<String> brokenVrchatMods = new ArrayList<>();

    public static String melonLoaderVRCHash = "25881";
    public static String melonLoaderVRCMinDate = "feb. 6, 2021 at 10.01pm CET";
    
    public static String vrchatBuild = "1";
    public static String emmVRCHash  = "1";

    protected static void registerCommand(Command command) {
        List<Command> list = commands;
        synchronized (list) {
            commands.add(command);
        }
    }

    protected static void runAsClient(MessageReceivedEvent event) {
        String command = event.getMessage().getContentRaw();
        List<Command> list = commands;
        synchronized (list) {
            for (Command rcmd : commands) {
                if (!rcmd.matchPattern(command)) continue;
                rcmd.onClient(command, event);
            }
        }
    }

    protected static void runAsServer(MessageReceivedEvent event) {
        String command = event.getMessage().getContentRaw();
        List<Command> list = commands;
        synchronized (list) {
            for (Command rcmd : commands) {
                if (!rcmd.matchPattern(command)) continue;
                rcmd.onServer(command, event);
            }
        }
    }

    public static void init() {
        if (init)
            return;
        
        init = true;
        CommandManager.registerCommand(new HelpCommand());
        CommandManager.registerCommand(new RankColorCommand());

        CommandManager.registerCommand(new AddReactionHandlerCommand());
        CommandManager.registerCommand(new SetLogChannelHandlerCommand());
        CommandManager.registerCommand(new VerifyChannelHandlerCommand());
        CommandManager.registerCommand(new SetScreeningRoleHandlerCommand());

        CommandManager.registerCommand(new CommandLaunchCommand());

        CommandManager.registerCommand(new VerifyCommandCommand());

        CommandManager.registerCommand(new RubybotOverDynobotCommand());

        CommandManager.registerCommand(new MLHashRegisterCommand());
        CommandManager.registerCommand(new SetMLReportChannelCommand());
        CommandManager.registerCommand(new MLSetMinForVRC());
        
        CommandManager.registerCommand(new MLBrokenModsCommand());
        
        CommandManager.registerCommand(new SetVRCBuild());
        CommandManager.registerCommand(new SetEmmHash());

        CommandManager.registerCommand(new LockDown());
        CommandManager.registerCommand(new ThawServer());
        CommandManager.registerCommand(new Purge());
    }


    public static void saveReactions() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("rolereactions.txt")))
        {
            for(ReactionListener rl : reactionListeners) {
                writer.write(rl.messageId + " " + rl.emoteId + " " + rl.roleId + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveScreenings() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("rolescreening.txt")))
        {
            for(Entry<Long, Long> pair : autoScreeningRoles.entrySet()) {
                writer.write(pair.getKey() + " " + pair.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void saveLogChannels() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("logchannels.txt")))
        {
            for(Entry<Long, String> logchannel : logChannels.entrySet()) {
                writer.write(logchannel.getKey() + " " + logchannel.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void saveMLReportChannels() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("mlreportchannels.txt")))
        {
            for(Entry<Long, String> logchannel : mlReportChannels.entrySet()) {
                writer.write(logchannel.getKey() + " " + logchannel.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void saveMLHashes() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("mlhashes.txt")))
        {
            for(MLHashPair s : melonLoaderHashes)
                writer.write("r " + s.x86 + " " + s.x64 + "\n");
            
            for(MLHashPair s : melonLoaderAlphaHashes)
                writer.write("a " + s.x86 + " " + s.x64 + "\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveMLVRCHash() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("mlvrchash.txt")))
        {
            writer.write(melonLoaderVRCHash + "\n" + melonLoaderVRCMinDate);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void saveVerifyChannels() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("verifychannels.txt")))
        {
            for(Entry<Long, VerifyPair> verifychannel : verifyChannels.entrySet()) {
                writer.write(verifychannel.getKey() + " " + verifychannel.getValue().channelId + " " + verifychannel.getValue().roleId + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveBrokenVRChatMods() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("brokenvrcmods.txt"))) {
            for(String s : brokenVrchatMods)
                writer.write(s + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveGuildConfigs() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("guildconfigurations.txt"))) {
            for (Entry<Long, boolean[]> saveconfigurations : GuildConfigurations.configurations.entrySet()) {
                writer.write(saveconfigurations.getKey().toString());
                for (Boolean bool : saveconfigurations.getValue()) {
                    writer.write(" " + bool.toString());
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Command> getCommands() {
        return commands;
    }

    public static Color hex2Rgb(String colorStr) {
        return new Color(Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
    }
}

