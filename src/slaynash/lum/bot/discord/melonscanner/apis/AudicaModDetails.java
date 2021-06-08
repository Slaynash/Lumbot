package slaynash.lum.bot.discord.melonscanner.apis;

import com.google.gson.annotations.SerializedName;

public class AudicaModDetails {
    @SerializedName("Name")
    public String name;
    
    @SerializedName("Author")
    public String author;
    
    @SerializedName("RepoName")
    public String repoName;
    
    @SerializedName("DisplayAuthor")
    public String displayAuthor;
    
    @SerializedName("Version")
    public String version;
    
    @SerializedName("Error")
    public boolean error;
    
    @SerializedName("Download")
    public AudicaDownload[] download;

    public static class AudicaDownload {
        public String name;
        public String browser_download_url;
    }
}
