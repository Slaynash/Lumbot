package slaynash.lum.bot.discord.logscanner;

import com.google.gson.annotations.SerializedName;

public class VRCModVersionDetails {
	public int _version;
	@SerializedName("ApprovalStatus")
	public int approvalstatus;
	public String reason;
	public String name;
	public String modversion;
	public String vrchatversion;
	public String loaderversion;
	public String modtype;
	public String author;
	public String description;
	public String downloadlink;
	public String sourcelink;
	public String discord;
	public String hash;
	public String changelog;
	public String updatedate;
}
