package slaynash.lum.bot.steam;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import in.dragonbra.javasteam.types.KeyValue;

public class SteamAppDetails {

    String appId;
    SteamAppDetailsCommon common;
    SteamAppDepots depots;

    private SteamAppDetails() {}

    public SteamAppDetails(KeyValue keyValues) {
        KeyValue appinfo = keyValues.get("appinfo");
        appId = appinfo.get("appid").asString();
        
        KeyValue common = appinfo.get("common");
        if (common != KeyValue.INVALID)
            this.common = new SteamAppDetailsCommon(common);
        
        KeyValue depots = appinfo.get("depots");
        if (depots != KeyValue.INVALID)
            this.depots = new SteamAppDepots(depots);
    }

    public static SteamAppDetails compare(SteamAppDetails oldDetails, SteamAppDetails newDetails) {
        SteamAppDetails ret = new SteamAppDetails();
        boolean changed = false;

        changed |= (ret.depots = SteamAppDepots.compare(oldDetails.depots, newDetails.depots)) != null;

        return changed ? ret : null;
    }


    public static class SteamAppDetailsCommon {
        public String name;
        public String type;
        public String releasestate;

        public SteamAppDetailsCommon(KeyValue keyValues) {
            name = keyValues.get("name").asString();
            type = keyValues.get("type").asString();
            releasestate = keyValues.get("releasestate").asString();
        }
    }

    public static class SteamAppDepots {
        // baselanguages
        public Map<String, SteamAppBranch> branches = new HashMap<>();

        private SteamAppDepots() {}

        public SteamAppDepots(KeyValue keyValues) {
            KeyValue branches = keyValues.get("common");
            for (KeyValue branch : branches.getChildren())
                this.branches.put(branch.getName(), new SteamAppBranch(branch));
        }

        public static SteamAppDepots compare(SteamAppDepots oldDepot, SteamAppDepots newDepot) {
            SteamAppDepots ret = new SteamAppDepots();
            boolean changed = false;

            Map<String, SteamAppBranch> changeBranches = new HashMap<>();
            for (Entry<String, SteamAppBranch> newBranch : newDepot.branches.entrySet()) {
                SteamAppBranch oldBranch = oldDepot.branches.get(newBranch.getKey());
                if (oldBranch == null)
                    changeBranches.put(newBranch.getKey(), newBranch.getValue());
                else {
                    SteamAppBranch compareResult = SteamAppBranch.compare(oldBranch, newBranch.getValue());
                    if (compareResult != null)
                        changeBranches.put(newBranch.getKey(), compareResult);
                }
            }
            for (Entry<String, SteamAppBranch> oldBranch : oldDepot.branches.entrySet()) {
                if (!newDepot.branches.containsKey(oldBranch.getKey()))
                    changeBranches.put(oldBranch.getKey(), oldBranch.getValue());
            }
            changed |= (ret.branches = changeBranches.size() > 0 ? changeBranches : null) != null;

            return changed ? ret : null;
        }
    }

    public static class SteamAppBranch {
        public long buildid;
        public String description;
        public long timeupdated;
        public Boolean pwdrequired;

        private SteamAppBranch() {}

        public SteamAppBranch(KeyValue keyValues) {
            buildid = keyValues.get("name").asLong();
            KeyValue description = keyValues.get("description");
            this.description = description != KeyValue.INVALID ? description.asString() : null;
            timeupdated = keyValues.get("timeupdated").asLong();
            KeyValue pwdrequired = keyValues.get("pwdrequired");
            this.pwdrequired = pwdrequired != KeyValue.INVALID ? pwdrequired.asBoolean() : null;
        }

        public static SteamAppBranch compare(SteamAppBranch oldBranch, SteamAppBranch newBranch) {
            SteamAppBranch ret = new SteamAppBranch();
            boolean changed = false;

            changed |= (ret.buildid = (oldBranch.buildid != newBranch.buildid ? newBranch.buildid : -1)) != -1;
            changed |= (ret.description = (oldBranch.description != newBranch.description ? (newBranch.description != null ? newBranch.description : oldBranch.description) : null)) != null;
            changed |= (ret.timeupdated = (oldBranch.timeupdated != newBranch.timeupdated ? newBranch.timeupdated : -1)) != -1;
            changed |= (ret.pwdrequired = (oldBranch.pwdrequired != newBranch.pwdrequired ? (newBranch.pwdrequired != null ? newBranch.pwdrequired : oldBranch.pwdrequired) : null)) != null;

            return changed ? ret : null;
        }
    }
}
