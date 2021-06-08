package slaynash.lum.bot.discord.melonscanner.apis;

import com.google.gson.annotations.SerializedName;

public class VRCModDetails {
    public int _id;
    public String mention;
    public long messageid;
    public int versionofmsg;
    public String uploaddate;
    public String[] aliases;
    public VRCModVersionDetails[] versions;

    public static class VRCModVersionDetails {
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
}
