package slaynash.lum.bot.discord;

public class ReactionListener {
	
	public String messageId;
	public String emoteId;
	public String roleId;

	public ReactionListener(String messageId, String emoteId, String roleId) {
		this.messageId = messageId;
		this.emoteId = emoteId;
		this.roleId = roleId;
	}
	
}
