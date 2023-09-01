package slaynash.lum.bot.uvm;

import java.util.Comparator;

public class UnityVersion {
    public String version;
    public String fullVersion;
    public String downloadUrl;
    public String downloadUrlIl2CppWin;

    public UnityVersion(String version, String fullVersion, String downloadUrl, String downloadUrlIl2CppWin) {
        this.version = version;
        this.fullVersion = fullVersion;
        this.downloadUrl = downloadUrl;
        this.downloadUrlIl2CppWin = downloadUrlIl2CppWin;
    }

    public static int compare(String left, String right) {
        int[] leftparts = getNumbers(left);
        int[] rightparts = getNumbers(right);

        long leftsum = leftparts[0] * 10000L + leftparts[1] * 100L + leftparts[2];
        long rightsum = rightparts[0] * 10000L + rightparts[1] * 100L + rightparts[2];

        return Long.compare(leftsum, rightsum);
    }

    private static int[] getNumbers(String s) {
        String[] numbersS = s.split("\\.");
        int[] numbers = new int[numbersS.length];
        for (int i = 0; i < numbersS.length; ++i)
            numbers[i] = Integer.parseInt(numbersS[i]);

        return numbers;
    }

    /*
    public static boolean isOverOrEqual(String currentversion, String validversion)
    {
        String[] versionparts = currentversion.split("\\.");

        String[] validversionparts = validversion.split("\\.");

        if (
            Integer.parseInt(versionparts[0]) >= Integer.parseInt(validversionparts[0]) &&
            Integer.parseInt(versionparts[1]) >= Integer.parseInt(validversionparts[1]) &&
            Integer.parseInt(versionparts[2]) >= Integer.parseInt(validversionparts[2]))
            return true;

        return false;
    }
    */

    public static boolean isOverOrEqual(String currentversion, String[] validversions) {
        if (validversions == null || validversions.length == 0)
            return true;

        String[] versionparts = currentversion.split("\\.");

        for (String validversion : validversions) {

            String[] validversionparts = validversion.split("\\.");

            if (
                Integer.parseInt(versionparts[0]) >= Integer.parseInt(validversionparts[0]) &&
                Integer.parseInt(versionparts[1]) >= Integer.parseInt(validversionparts[1]) &&
                Integer.parseInt(versionparts[2]) >= Integer.parseInt(validversionparts[2]))
                return true;

        }

        return false;
    }


    public static class Comparator implements java.util.Comparator<String> {
        @Override
        public int compare(String left, String right) {
            return UnityVersion.compare(left, right);
        }
    }

}
