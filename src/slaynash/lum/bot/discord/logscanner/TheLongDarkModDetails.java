package slaynash.lum.bot.discord.logscanner;

import com.google.gson.annotations.SerializedName;

public class TheLongDarkModDetails {
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
    public TheLongDarkDownload[] download;
}
