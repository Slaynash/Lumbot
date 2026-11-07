package slaynash.lum.bot.discord.slashs;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.TimeFormat;

public class WhoisSlash extends Slash {
    private static final int DEFAULT_COLOR = 0x5865F2;
    private static final int MAX_ROLES_SHOWN = 30;

    @Override
    protected CommandData globalSlashData() {
        return Commands.slash("whois", "Show details about a user")
            .addOption(OptionType.USER, "user", "User to look up (defaults to you)", false)
            .setContexts(InteractionContextType.GUILD);
    }

    @Override
    protected void slashRun(SlashCommandInteractionEvent event) {
        var option = event.getOption("user");
        User user = option == null ? event.getUser() : option.getAsUser();
        Member member = option == null ? event.getMember() : option.getAsMember();

        event.deferReply().queue(hook -> user.retrieveProfile().queue(
            profile -> hook.editOriginalEmbeds(buildEmbed(user, member, profile)).queue(),
            error -> hook.editOriginalEmbeds(buildEmbed(user, member, null)).queue()));
    }

    private static net.dv8tion.jda.api.entities.MessageEmbed buildEmbed(User user, Member member, User.Profile profile) {
        EmbedBuilder eb = new EmbedBuilder();

        int color = DEFAULT_COLOR;
        if (profile != null && profile.getAccentColor() != null)
            color = profile.getAccentColorRaw();
        else if (member != null)
            color = member.getColorRaw();
        eb.setColor(color);

        String displayName = member != null ? member.getEffectiveName() : user.getEffectiveName();
        eb.setAuthor(displayName + (user.isBot() ? " [BOT]" : ""), null, user.getEffectiveAvatarUrl());
        eb.setTitle("User Information");
        eb.setThumbnail(member != null ? member.getEffectiveAvatarUrl() : user.getEffectiveAvatarUrl());
        if (profile != null && profile.getBannerUrl() != null)
            eb.setImage(profile.getBannerUrl());

        eb.setDescription(user.getAsMention() + (member == null ? "\n*Not a member of this server*" : ""));

        eb.addField("Username", "`" + user.getName() + "`", true);
        if (user.getGlobalName() != null && !user.getGlobalName().equals(user.getName()))
            eb.addField("Global Name", "`" + user.getGlobalName() + "`", true);

        eb.addField("Account Created", formatTime(user.getTimeCreated()), true);

        if (member != null) {
            if (member.getNickname() != null)
                eb.addField("Nickname", "`" + member.getNickname() + "`", true);
            eb.addField("Joined Server", formatTime(member.getTimeJoined()), true);
            if (member.getTimeBoosted() != null)
                eb.addField("Boosting Since", formatTime(member.getTimeBoosted()), true);
            if (member.isTimedOut())
                eb.addField("Timed Out Until", TimeFormat.DATE_TIME_SHORT.format(member.getTimeOutEnd()), true);

            if (member.isOwner())
                eb.addField("Server Owner", "Yes", true);
            if (member.isPending())
                eb.addField("Pending Screening", "Yes", true);

            List<Role> roles = member.getRoles();
            String roleText = roles.isEmpty() ? "None" : roles.stream().limit(MAX_ROLES_SHOWN).map(Role::getAsMention).collect(Collectors.joining(" "));
            if (roles.size() > MAX_ROLES_SHOWN)
                roleText += " *+" + (roles.size() - MAX_ROLES_SHOWN) + " more*";
            eb.addField("🎭 Roles [" + roles.size() + "]", roleText, false);

            var flags = member.getFlags();
            if (!flags.isEmpty())
                eb.addField("Server Flags", flags.stream().map(f -> "`" + f.name() + "`").collect(Collectors.joining(" ")), false);
        }
        if (member == null)
            eb.setFooter("Not in this server");
        eb.setTimestamp(OffsetDateTime.now());
        return eb.build();
    }

    private static String formatTime(OffsetDateTime time) {
        return TimeFormat.DATE_TIME_LONG.format(time) + "\n(" + TimeFormat.RELATIVE.format(time) + ")";
    }
}
