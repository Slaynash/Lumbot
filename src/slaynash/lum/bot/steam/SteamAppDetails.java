package slaynash.lum.bot.steam;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import in.dragonbra.javasteam.types.KeyValue;

public class SteamAppDetails {

    String appId;
    SteamAppDetailsCommon common;
    SteamAppDepots depots;

    private SteamAppDetails() {}

    public SteamAppDetails(KeyValue keyValues) {
        appId = keyValues.get("appid").asString();
        System.out.println("appid: " + appId);

        KeyValue common = keyValues.get("common");
        if (common != KeyValue.INVALID)
            this.common = new SteamAppDetailsCommon(common);
        else
            System.out.println("key \"common\" not found in " + keyValues.getName());

        KeyValue depots = keyValues.get("depots");
        if (depots != KeyValue.INVALID)
            this.depots = new SteamAppDepots(depots);
        else
            System.out.println("key \"depots\" not found in " + keyValues.getName());
    }

    public static SteamAppDetails compare(SteamAppDetails oldDetails, SteamAppDetails newDetails) {
        SteamAppDetails ret = new SteamAppDetails();
        boolean changed;

        changed = (ret.depots = SteamAppDepots.compare(oldDetails.depots, newDetails.depots)) != null;

        return changed ? ret : null;
    }


    public static class SteamAppDetailsCommon {
        public final String name;
        public final String type;
        public final String releasestate;

        public SteamAppDetailsCommon(KeyValue keyValues) {
            name = keyValues.get("name").asString();
            type = keyValues.get("type").asString();
            releasestate = keyValues.get("releasestate").asString();
        }
    }

    public static class SteamAppDepots {
        // baselanguages
        public Map<Integer, SteamAppDepot> elements = new HashMap<>();
        public Map<String, SteamAppBranch> branches = new HashMap<>();

        private SteamAppDepots() {}

        public SteamAppDepots(KeyValue keyValues) {
            for (KeyValue element : keyValues.getChildren()) {
                try {
                    Integer key = Integer.parseInt(element.getName());
                    elements.put(key, new SteamAppDepot(element));
                }
                catch (Exception ignored) { }
            }
            KeyValue branches = keyValues.get("branches");
            for (KeyValue branch : branches.getChildren())
                this.branches.put(branch.getName(), new SteamAppBranch(branch));
        }

        public static SteamAppDepots compare(SteamAppDepots oldDepots, SteamAppDepots newDepots) {
            SteamAppDepots ret = new SteamAppDepots();
            boolean changed;

            Map<String, SteamAppBranch> changeBranches = new HashMap<>();
            for (Entry<String, SteamAppBranch> newBranchEntries : newDepots.branches.entrySet()) {
                String branchKey = newBranchEntries.getKey();
                SteamAppBranch newBranch = newBranchEntries.getValue();
                SteamAppBranch oldBranch = oldDepots.branches.get(newBranchEntries.getKey());

                if (oldBranch == null)
                    changeBranches.put(branchKey, newBranch);
                else {
                    SteamAppBranch compareResult = SteamAppBranch.compare(oldBranch, newBranch);
                    if (compareResult != null)
                        changeBranches.put(branchKey, compareResult);
                }
            }
            for (Entry<String, SteamAppBranch> oldBranch : oldDepots.branches.entrySet()) {
                if (!newDepots.branches.containsKey(oldBranch.getKey()))
                    changeBranches.put(oldBranch.getKey(), oldBranch.getValue());
            }
            changed = (ret.branches = changeBranches.size() > 0 ? changeBranches : null) != null;

            Map<Integer, SteamAppDepot> changeElements = new HashMap<>();
            for (Entry<Integer, SteamAppDepot> newElementEntries : newDepots.elements.entrySet()) {
                Integer elementKey = newElementEntries.getKey();
                SteamAppDepot newElement = newElementEntries.getValue();
                SteamAppDepot oldElement = oldDepots.elements.get(newElementEntries.getKey());

                if (oldElement == null)
                    changeElements.put(elementKey, newElement);
                else {
                    SteamAppDepot compareResult = SteamAppDepot.compare(oldElement, newElement);
                    if (compareResult != null)
                        changeElements.put(elementKey, compareResult);
                }
            }
            for (Entry<Integer, SteamAppDepot> oldElements : oldDepots.elements.entrySet()) {
                if (!newDepots.elements.containsKey(oldElements.getKey()))
                    changeElements.put(oldElements.getKey(), oldElements.getValue());
            }
            changed |= (ret.elements = changeElements.size() > 0 ? changeElements : null) != null;

            return changed ? ret : null;
        }
    }

    public static class SteamAppDepot {
        public String name;
        public Map<String, Long> manifests = new HashMap<>();

        private SteamAppDepot() {}

        public SteamAppDepot(KeyValue keyValues) {
            name = keyValues.get("name").asString();
            KeyValue manifestsKV = keyValues.get("manifests");
            if (manifestsKV != KeyValue.INVALID) {
                manifests = new HashMap<>();
                for (KeyValue kv : manifestsKV.getChildren())
                    manifests.put(kv.getName(), kv.asLong());
            }
        }

        public static SteamAppDepot compare(SteamAppDepot oldDepot, SteamAppDepot newDepot) {
            SteamAppDepot ret = new SteamAppDepot();
            boolean changed;

            changed = (ret.name = isStringEquals(oldDepot.name, newDepot.name) ? null : (newDepot.name != null ? newDepot.name : oldDepot.name)) != null;

            Map<String, Long> changeManifests = new HashMap<>();
            for (Entry<String, Long> newManifestEntries : newDepot.manifests.entrySet()) {
                String manifestKey = newManifestEntries.getKey();
                Long newManifest = newManifestEntries.getValue();
                Long oldManifest = oldDepot.manifests.get(manifestKey);

                if (oldManifest == null)
                    changeManifests.put(manifestKey, newManifest);
                else {
                    long compareResult = oldManifest.equals(newManifest) ? -1 : newManifest;
                    if (compareResult != -1)
                        changeManifests.put(manifestKey, compareResult);
                }
            }
            for (Entry<String, Long> oldManifestEntry : oldDepot.manifests.entrySet()) {
                String manifestKey = oldManifestEntry.getKey();
                Long newManifest = newDepot.manifests.get(manifestKey);
                Long oldManifest = oldManifestEntry.getValue();

                if (newManifest == null)
                    changeManifests.put(manifestKey, oldManifest);
            }
            changed |= (ret.manifests = changeManifests.size() > 0 ? changeManifests : null) != null;

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
            buildid = keyValues.get("buildid").asLong();
            KeyValue description = keyValues.get("description");
            this.description = description != KeyValue.INVALID ? description.asString() : null;
            timeupdated = keyValues.get("timeupdated").asLong();
            KeyValue pwdrequired = keyValues.get("pwdrequired");
            this.pwdrequired = pwdrequired != KeyValue.INVALID ? pwdrequired.asBoolean() : null;
        }

        public static SteamAppBranch compare(SteamAppBranch oldBranch, SteamAppBranch newBranch) {
            SteamAppBranch ret = new SteamAppBranch();
            boolean changed;

            changed = (ret.buildid = oldBranch.buildid != newBranch.buildid ? newBranch.buildid : -1) != -1;
            changed |= (ret.description = !isStringEquals(oldBranch.description, newBranch.description) ? (newBranch.description != null ? newBranch.description : oldBranch.description) : null) != null;
            changed |= (ret.timeupdated = oldBranch.timeupdated != newBranch.timeupdated ? newBranch.timeupdated : -1) != -1;
            changed |= (ret.pwdrequired = oldBranch.pwdrequired != newBranch.pwdrequired ? (newBranch.pwdrequired != null ? newBranch.pwdrequired : oldBranch.pwdrequired) : null) != null;

            return changed ? ret : null;
        }
    }

    private static boolean isStringEquals(String left, String right) {
        return Objects.equals(left, right);
    }
}
