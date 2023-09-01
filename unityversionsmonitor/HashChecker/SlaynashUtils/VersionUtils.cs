using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;

namespace SlaynashUtils
{
    public static class VersionUtils
    {
        // left more recent: 1
        // identicals: 0
        // right more recent: -1
        public static int CompareVersion(VersionData left, VersionData right)
        {
            if (left.IsValidSemver != right.IsValidSemver)
                return left.IsValidSemver ? 1 : -1;

            int compareLength = left.Length > right.Length ? left.Length : right.Length;
            for (int i = 0; i < compareLength; ++i)
            {
                int leftNumber = left.GetIndex(i);
                int rightNumber = right.GetIndex(i);

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
        public static int CompareVersion(string left, string right)
        {
            return CompareVersion(GetVersion(left), GetVersion(right));
        }

        public static VersionData GetVersion(string versionString)
        {
            versionString = versionString.Trim();

            if (string.IsNullOrEmpty(versionString))
                return VersionData.ZERO;

            MatchCollection matches = Regex.Matches(versionString, "\\d+");
            bool isValidSemver = Regex.IsMatch(versionString, "^v?[0-9][\\d.-_]*[^\\s]*$");

            return new VersionData(matches, isValidSemver);
        }


        public class VersionData
        {
            public static readonly VersionData ZERO = new VersionData();

            public List<int> numbers;

            public bool IsValidSemver { get; }
            public int Length => numbers.Count;

            public VersionData()
            {
                IsValidSemver = false;
                numbers = new List<int>(0);
            }

            public VersionData(MatchCollection collection, bool validSemver)
            {
                IsValidSemver = validSemver;
                numbers = new List<int>(collection.Count);

                foreach (Match match in collection)
                {
                    int parsedNumber = int.Parse(match.Value);
                    numbers.Add(parsedNumber > 0 ? parsedNumber : 0);
                }
            }

            public int GetIndex(int index)
            {
                return numbers.Count > index ? numbers.ElementAt(index) : 0;
            }
        }
    }
}
