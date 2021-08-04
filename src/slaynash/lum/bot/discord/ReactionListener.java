package slaynash.lum.bot.discord;

public class ReactionListener {

    public final String messageId;
    public final String emoteId;
    public final String roleId;

    public ReactionListener(String messageId, String emoteId, String roleId) {
        this.messageId = messageId;
        this.emoteId = emoteId;
        this.roleId = roleId;
    }

}
