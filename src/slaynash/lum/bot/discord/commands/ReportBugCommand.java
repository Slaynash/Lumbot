package slaynash.lum.bot.discord.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CrossServerUtils;

public class ReportBugCommand extends Command {

    @Override
    protected void onServer(String paramString, MessageReceivedEvent paramMessageReceivedEvent) {
        Member member = CrossServerUtils.resolveMember("663449315876012052" /* MelonLoader */, paramMessageReceivedEvent.getAuthor().getId());
    }

    @Override
    protected boolean matchPattern(String paramString) {
		return paramString.split(" ", 2)[0].equals("l!reportbug");
    }

    @Override
    public String getHelpName() {
        return "l!reportbug";
    }

    @Override
    public String getHelpDescription() {
        return "Report a bug";
    }

}
