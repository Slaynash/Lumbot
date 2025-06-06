package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.DBConnectionManagerLum;
import slaynash.lum.bot.discord.commands.AddMissingRoles;
import slaynash.lum.bot.discord.commands.AddReactionHandlerCommand;
import slaynash.lum.bot.discord.commands.AutoPublish;
import slaynash.lum.bot.discord.commands.Ban;
import slaynash.lum.bot.discord.commands.CommandLaunchCommand;
import slaynash.lum.bot.discord.commands.DumpID;
import slaynash.lum.bot.discord.commands.HelpCommand;
import slaynash.lum.bot.discord.commands.Kick;
import slaynash.lum.bot.discord.commands.LockDown;
import slaynash.lum.bot.discord.commands.LumGoneCommand;
import slaynash.lum.bot.discord.commands.Purge;
import slaynash.lum.bot.discord.commands.RankColorCommand;
import slaynash.lum.bot.discord.commands.RubybotOverDynobotCommand;
import slaynash.lum.bot.discord.commands.SetScreeningRoleHandlerCommand;
import slaynash.lum.bot.discord.commands.TestVRCObfmap;
import slaynash.lum.bot.discord.commands.UVMCommand;
import slaynash.lum.bot.discord.commands.Unban;
import slaynash.lum.bot.discord.commands.VerifyChannelHandlerCommand;
import slaynash.lum.bot.discord.commands.VerifyCommandCommand;
import slaynash.lum.bot.utils.ExceptionUtils;

public class CommandManager {
    public static final String LOG_IDENTIFIER = "CommandManager";

    private static final List<Command> commands = new ArrayList<>();
    private static boolean init = false;

    public static final List<ReactionListener> reactionListeners = new ArrayList<>();
    public static final List<Long> apChannels = new ArrayList<>();
    public static final Map<Long, VerifyPair> verifyChannels = new HashMap<>();
    public static final Map<Long, Long> autoScreeningRoles = new HashMap<>();

    protected static void registerCommand(Command command) {
        synchronized (commands) {
            commands.add(command);
        }
    }

    protected static void runAsClient(MessageReceivedEvent event) {
        String command = event.getMessage().getContentRaw();
        synchronized (commands) {
            for (Command rcmd : commands) {
                if (!rcmd.matchPattern(command.toLowerCase())) continue;
                if (!rcmd.allowBots() && event.getAuthor().isBot()) continue;
                try {
                    rcmd.onClient(command, event);
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("Failed to run command " + command, e, event.getChannel());
                }
            }
        }
    }

    protected static void runAsServer(MessageReceivedEvent event) {
        if (event.getChannelType().equals(ChannelType.TEXT) && !event.getGuild().getSelfMember().hasPermission(event.getChannel().asGuildMessageChannel(), Permission.MESSAGE_SEND)) //every command sends a message so lets require send message perms
            return;
        String command = event.getMessage().getContentRaw();
        if (command.startsWith(ConfigManager.discordPrefix + "ping")) command = command.substring((ConfigManager.discordPrefix + "ping").length()).trim();
        synchronized (commands) {
            for (Command rcmd : commands) {
                if (!rcmd.matchPattern(command.toLowerCase())) continue;
                if (!rcmd.allowBots() && event.getAuthor().isBot()) continue;
                try {
                    rcmd.onServer(command, event);
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("Failed to run command " + command, e, event.getChannel());
                }
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
        CommandManager.registerCommand(new VerifyChannelHandlerCommand());
        CommandManager.registerCommand(new SetScreeningRoleHandlerCommand());

        CommandManager.registerCommand(new CommandLaunchCommand());

        CommandManager.registerCommand(new VerifyCommandCommand());

        CommandManager.registerCommand(new RubybotOverDynobotCommand());

        CommandManager.registerCommand(new LockDown());
        CommandManager.registerCommand(new Purge());
        CommandManager.registerCommand(new DumpID());
        CommandManager.registerCommand(new Ban());
        CommandManager.registerCommand(new Unban());
        CommandManager.registerCommand(new Kick());
        CommandManager.registerCommand(new AutoPublish());

        CommandManager.registerCommand(new UVMCommand());
        CommandManager.registerCommand(new AddMissingRoles());

        CommandManager.registerCommand(new LumGoneCommand());
        CommandManager.registerCommand(new TestVRCObfmap());
    }


    public static void saveReactions() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("storage/rolereactions.txt"))) {
            for (ReactionListener rl : reactionListeners) {
                writer.write(rl.messageId() + " " + rl.emoteId() + " " + rl.roleId() + "\n");
            }
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed save ReactionRoles", e);
        }
    }

    public static void saveScreenings() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("storage/rolescreening.txt"))) {
            for (Entry<Long, Long> pair : autoScreeningRoles.entrySet()) {
                writer.write(pair.getKey() + " " + pair.getValue() + "\n");
            }
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to save ScreeningRoles", e);
        }
    }

    public static MessageChannelUnion getModReportChannels(GenericMessageEvent event, String type) {
        if (event.getChannelType() == ChannelType.PRIVATE) return event.getChannel();
        return getModReportChannels(event.getGuild().getIdLong(), type);
    }
    public static MessageChannelUnion getModReportChannels(Guild guild, String type) {
        return getModReportChannels(guild.getIdLong(), type);
    }
    public static MessageChannelUnion getModReportChannels(GenericCommandInteractionEvent event, String type) {
        if (event.getChannelType() == ChannelType.PRIVATE) return (MessageChannelUnion) event.getChannel();
        return getModReportChannels(event.getGuild().getIdLong(), type);
    }
    public static MessageChannelUnion getModReportChannels(Long guildID, String type) {
        MessageChannelUnion channel = null;
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT `" + type + "` FROM `LogChannel` WHERE GuildID = ?", guildID);
            if (rs.next()) {
                channel = (MessageChannelUnion) JDAManager.getJDA().getGuildChannelById(rs.getLong(1));
            }
            rs.close();
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to get Mod Report Channels", e);
        }
        return channel;
    }

    public enum SetModReportChannelsResult { insert, delete, error }
    public static SetModReportChannelsResult setModReportChannels(Long guildId, Long channelId) {
        try {
            ResultSet rs = DBConnectionManagerLum.sendRequest("SELECT * FROM `LogChannel` WHERE Channel = ?", channelId);
            if (rs.next()) {
                DBConnectionManagerLum.sendUpdate("DELETE FROM `LogChannel` WHERE Channel = ?", channelId);
                return SetModReportChannelsResult.delete;
            }
            else {
                DBConnectionManagerLum.sendUpdate("INSERT INTO `LogChannel` (`GuildID`, `Channel`) VALUES (?, ?)", guildId, channelId);
                return SetModReportChannelsResult.insert;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to save Mod Report Channels", e);
        }
        return SetModReportChannelsResult.error;
    }

    public static void saveVerifyChannels() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("storage/verifychannels.txt"))) {
            for (Entry<Long, VerifyPair> verifychannel : verifyChannels.entrySet()) {
                writer.write(verifychannel.getKey() + " " + verifychannel.getValue().channelId() + " " + verifychannel.getValue().roleId() + "\n");
            }
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to save Verify Channels", e);
        }
    }

    public static void saveAPChannels() {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("storage/autopublishchannels.txt"))) {
            for (Long channel : apChannels) {
                writer.write(channel + "\n");
            }
        }
        catch (IOException e) {
            ExceptionUtils.reportException("Failed to save MelonLoader Report Channels", e);
        }
    }

    public static List<Command> getCommands() {
        return commands;
    }

    public static Color hex2Rgb(String colorStr) {
        if (colorStr.startsWith("#")) colorStr = colorStr.substring(1);
        if (colorStr.length() != 6) throw new IllegalArgumentException("Invalid color length");
        return new Color(Integer.valueOf(colorStr.substring(0, 2), 16), Integer.valueOf(colorStr.substring(2, 4), 16), Integer.valueOf(colorStr.substring(4, 6), 16));
    }
}
