package slaynash.lum.bot.discord.logscanner;

public class ModDetails {
	public String name;
	public ModVersion[] versions;

	public ModDetails(String name, ModVersion[] versions) {
		this.name = name;
		this.versions = versions;
	}
	
	public ModDetails(String name, String version) {
		this.name = name;
		this.versions = new ModVersion[] {new ModVersion(version, null)};
	}
}
