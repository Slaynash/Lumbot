package slaynash.lum.bot.discord.slashs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class Ban extends Slash {
    @Override
    protected CommandData globalSlashData() {
        return Commands.slash("ban", "Ban users")
            .addOption(OptionType.STRING, "userid", "Enter User ID, space delimited for multiple", false)
            .addOption(OptionType.INTEGER, "purge", "Days to purge message from User, max 14 days", false)
            .addOption(OptionType.STRING, "reason", "Enter Mention/Message for public beta", false)
            .addOption(OptionType.ATTACHMENT, "txt", "Upload a .txt from DumpID command to use, UserID at the start of newlines", false)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    public void slashRun(SlashCommandInteractionEvent event) {
        if (event.getChannelType() == ChannelType.PRIVATE) {
            event.reply("Bans do not work in DMs").queue();
            return;
        }
        InteractionHook interactionhook = event.deferReply().complete();
        String guildID = event.getGuild().getId();
        // String channelID = event.getChannel().asGuildMessageChannel().getId();
        List<OptionMapping> userID = event.getOptionsByName("userid");
        List<OptionMapping> purge = event.getOptionsByName("purge");
        List<OptionMapping> reason = event.getOptionsByName("reason");
        List<OptionMapping> txt = event.getOptionsByName("txt");
        List<String> ids = new ArrayList<>();
        List<Member> members = new ArrayList<>();

        //Check if reply, userid, or txt is not present
//        if (userID.isEmpty() && txt.isEmpty() && event.getMessage().getReferencedMessage() == null) {
//            interactionhook.editOriginal("Usage: reply to user or " + getName() + " <UserID> (reason)").queue();
//            return;
//        }
        if (!userID.isEmpty()) {
            ids.addAll(Arrays.asList(userID.get(0).getAsString().split(" ")));
        }
        event.getInteraction();
    }
}
