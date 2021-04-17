package slaynash.lum.bot.discord.logscanner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionUtils {

    private static Pattern versionPattern = Pattern.compile("\\d+");

    private VersionUtils() {}

    // left more recent: 1
    // identicals: 0
    // right more recent: -1
    public static int CompareVersion(VersionData left, VersionData right) {
        if (left.getIsValidSemver() != right.getIsValidSemver())
            return left.getIsValidSemver() ? 1 : -1;

        int compareLength = left.getLength() > right.getLength() ? left.getLength() : right.getLength();
        for (int i = 0; i < compareLength; ++i) {
            int leftNumber = left.getIndex(i);
            int rightNumber = right.getIndex(i);

            if (leftNumber > rightNumber)
                return 1;
            if (leftNumber < rightNumber)
                return -1;
        }

        return 0;
    }

    // left more recent: 1
    // identicals: 0
    // right more recent: -1
    public static int CompareVersion(String left, String right) {
        return CompareVersion(GetVersion(left), GetVersion(right));
    }

    public static VersionData GetVersion(String versionString) {
        versionString = versionString != null ? versionString.trim() : null;

        if (versionString == null || versionString.equals(""))
            return new VersionData();

        Matcher matcher = versionPattern.matcher(versionString);
        boolean isValidSemver = versionString.matches("^v?[0-9][\\d.-_]*[^\\s]*$");
        //System.out.println("SEMVER \"" + versionString + "\": " + isValidSemver);

        return new VersionData(versionString, matcher, isValidSemver);
    }


    public static class VersionData
    {
        private String raw;
        private List<Integer> numbers;

        private boolean isValidSemver;

        public VersionData() {
            isValidSemver = false;
            raw = "";
            numbers = new ArrayList<Integer>(0);
        }

        public VersionData(String raw, Matcher collection, boolean validSemver) {
            isValidSemver = validSemver;
            this.raw = raw;
            numbers = new ArrayList<Integer>();

            while (collection.find()) {
                int parsedNumber = Integer.parseInt(collection.group());
                numbers.add(parsedNumber > 0 ? parsedNumber : 0);
            }
        }

        public int getIndex(int index) {
            return numbers.size() > index ? numbers.get(index) : 0;
        }

        public int getLength() {
            return numbers.size();
        }

        public boolean getIsValidSemver() {
            return isValidSemver;
        }

        public String getRaw() {
            return new String(raw);
        }
    }
}
