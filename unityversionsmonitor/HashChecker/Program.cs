using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using SlaynashUtils;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;

// Slaynash: This code was originally a prototype and never has been cleaned up,
// it shouldn't even be in the prod Lum but it works so far and I wanted to include
// the sources of it into Lum's repo (for version control), so here they are.

// Note: this isn't part of the CI pipeline.

namespace HashChecker
{
    class Program
    {
        public class HashInfo
        {
            public class Hash
            {
                public string[] archs;
                public string[] unityminversions;
                public string hash;

                public Hash (string[] archs, string[] unityminversions, string hash)
                {
                    this.archs = archs;
                    this.unityminversions = unityminversions;
                    this.hash = hash;
                }
            }

            public string hashname;
            public Hash[] hashes;

            public HashInfo (string hashname, Hash[] hashes)
            {
                this.hashname = hashname;
                this.hashes = hashes;
            }
        }
        public class Arch
        {
            public class Path
            {
                public string[] unityminversions;
                public string path;

                public Path(string[] unityminversions, string path)
                {
                    this.unityminversions = unityminversions;
                    this.path = path;
                }
            }

            public string name;
            public Path[] paths;

            public Arch(string name, Path[] paths)
            {
                this.name = name;
                this.paths = paths;
            }
        }

        public static readonly HashInfo[] hashinfos = new []
        {
            new HashInfo("SetupPixelCorrectCoordinates", new[]
            {
                new HashInfo.Hash(
                    new [] { "x64" },
                    new string[0],
                    "48 89 5c 24 08 57 48 81 ec a0 00 00 00 8b d9 e8 ?? ?? ?? ?? 48 8b f8 e8"
                ),

                new HashInfo.Hash(
                    new [] { "x86" },
                    new [] { "2019.1.0" },
                    "55 8b ec 83 ec 60 53 56 57 e8 ?? ?? ?? ?? 8b d8 e8 ?? ?? ?? ?? ff 75 08 8d 4d f0"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new [] { "2017.3.0", "2018.1.0" },
                    "55 8b ec 83 ec 60 53 56 57 e8 ?? ?? ?? ?? ff 75 08 8b d8 8d 45 f0 50 e8"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new string[0],
                    "55 8b ec 83 ec 60 56 e8 ?? ?? ?? ?? 8b f0 8b 45 08 50 8d 4d f0 51 e8"
                )
            }),
            new HashInfo("PresentFrame", new[]
            {
                /* This technically resolve a call to PresentFrame, but there is a lot of matches sometimes
                new HashInfo.Hash(
                    new [] { "x64" },
                    new string[0],
                    "b1 01 ff d0 e8"
                ),
                
                new HashInfo.Hash(
                    new [] { "x86" },
                    new string[0],
                    "6a 01 ff d0 83 c4 04 e8"
                ),
                */
                new HashInfo.Hash(
                    new [] { "x64" },
                    new [] { "2018.4.18", "2019.3.0", "2020.1.0" },
                    "40 53 48 83 ec 20 e8 ?? ?? ?? ?? 48 8b c8 48 8b d8 e8 ?? ?? ?? ?? e8 ?? ?? ?? ?? 48 85 c0 74"
                ),
                new HashInfo.Hash(
                    new [] { "x64" },
                    new [] { "2018.3.0", "2019.1.0" },
                    "48 83 ec 28 e8 ?? ?? ?? ?? 48 85 c0 74 15 e8 ?? ?? ?? ?? 48 8b c8 48 8b 10 ff 92 ?? ?? 00 00 84 c0" // We can't use this one too early, else we match multiple functions
                ),
                new HashInfo.Hash(
                    new [] { "x64" },
                    new string[0],
                    "48 83 ec 28 e8 ?? ?? ?? ?? 48 85 c0 74 15 e8 ?? ?? ?? ?? 48 8b c8 48 8b 10 ff 92 e0 00 00 00 84 c0"
                ),

                new HashInfo.Hash(
                    new [] { "x86" },
                    new string[] { "2018.4.18", "2019.3.0", "2020.1.0", "2021.1.0" },
                    "55 8b ec 51 56 e8 ?? ?? ?? ?? 8b f0 8b ce e8 ?? ?? ?? ?? e8 ?? ?? ?? ?? 85 c0 74 ?? e8"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new [] { "2018.4.9", "2019.1.0" },
                    "55 8b ec 51 e8 ?? ?? ?? ?? 85 c0 74 15 e8 ?? ?? ?? ?? 8b c8 8b 10 8b 82 ?? 00 00 00 ff d0"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new [] { "2018.1.0" },
                    "55 8b ec 51 e8 ?? ?? ?? ?? 85 c0 74 12 e8 ?? ?? ?? ?? 8b c8 8b 10 8b 42 ?? ff d0 84 c0 75"
                ),
                // 2017.3.0
                new HashInfo.Hash(
                    new [] { "x86" },
                    new string[0],
                    "e8 ?? ?? ?? ?? 85 c0 74 12 e8 ?? ?? ?? ?? 8b ?? 8b ?? 8b 42 70 ff d0 84 c0 75"
                )
            }),
            new HashInfo("D3D11:WaitForLastPresentationAndGetTimestamp", new[]
            {
                new HashInfo.Hash(
                    new [] { "x64" },
                    new string [] { "2022.1.0" },
                    "48 89 5c 24 10 56 48 81 ec b0 00 00 00 0f 29 b4 24 a0 00 00 00 48 8b f1"
                ),
                new HashInfo.Hash(
                    new [] { "x64" },
                    new string [] { "2020.2.7", "2020.3.0", "2021.1.0" },
                    "48 89 5c 24 10 56 48 81 ec 90 00 00 00 0f 29 b4 24 80 00 00 00 48 8b f1"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new string [] { "2022.1.0" },
                    "55 8b ec 83 ec 58 53 56 8b d9 57 89 5d fc e8 ?? ?? ?? ?? 6a 02 8b c8"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new string [] { "2020.3.9", "2021.1.5", "2021.2.0" },
                    "55 8b ec 83 ec 48 53 56 8b d9 57 89 5d fc e8 ?? ?? ?? ?? 6a 02 8b c8"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new string [] { "2020.2.7", "2020.3.0", "2021.1.0" },
                    "55 8b ec 83 ec 40 53 56 8b d9 57 89 5d fc e8 ?? ?? ?? ?? 6a 02 8b c8"
                )
            }),
            new HashInfo("D3D12:WaitForLastPresentationAndGetTimestamp", new[]
            {
                new HashInfo.Hash(
                    new [] { "x64" },
                    new string [] { "2022.1.0" },
                    "48 89 5c 24 08 57 48 81 ec b0 00 00 00 0f 29 b4 24 a0 00 00 00 48 8b d9"
                ),
                new HashInfo.Hash(
                    new [] { "x64" },
                    new string [] { "2020.2.7", "2020.3.0", "2021.1.0" },
                    "48 89 5c 24 08 57 48 81 ec 90 00 00 00 0f 29 b4 24 80 00 00 00 48 8b d9"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new string [] { "2022.1.0" },
                    "55 8b ec 83 ec 58 56 57 8b f9 89 7d f8 e8 ?? ?? ?? ?? 6a 02 8b c8"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new string [] { "2021.2.0" },
                    "55 8b ec 83 ec 48 56 57 8b f9 89 7d f8 e8 ?? ?? ?? ?? 6a 02 8b c8"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new string [] { "2020.3.9", "2021.1.5" },
                    "55 8b ec 83 ec 48 56 57 8b f9 89 7d f0 e8 ?? ?? ?? ?? 6a 02 8b c8"
                ),
                new HashInfo.Hash(
                    new [] { "x86" },
                    new string [] { "2020.2.7", "2020.3.0", "2021.1.0" },
                    "55 8b ec 83 ec 40 53 56 57 8b f9 89 7d f4 e8 ?? ?? ?? ?? 6a 02 8b c8"
                )
            })
        };

        public static readonly Arch[] archs = new[]
        {
            new Arch("mono x86 nondev", new [] {
                new Arch.Path(new [] { "2021.2.0", "2022.1.0" }, "win32_player_nondevelopment_mono/UnityPlayer.dll"),
                new Arch.Path(new [] { "2017.2.0", "2018.1.0" }, "win32_nondevelopment_mono/UnityPlayer.dll"),
                new Arch.Path(new string[0], "win32_nondevelopment_mono/player_win.exe")
            }),
            new Arch("mono x64 nondev", new [] {
                new Arch.Path(new [] { "2021.2.0", "2022.1.0" }, "win64_player_nondevelopment_mono/UnityPlayer.dll"),
                new Arch.Path(new [] { "2017.2.0", "2018.1.0" }, "win64_nondevelopment_mono/UnityPlayer.dll"),
                new Arch.Path(new string[0], "win64_nondevelopment_mono/player_win.exe")
            }),

            new Arch("il2cpp x86 nondev", new [] {
                new Arch.Path(new [] { "2021.2.0", "2022.1.0" }, "win32_player_nondevelopment_il2cpp/UnityPlayer.dll"),
                new Arch.Path(new [] { "2017.2.0", "2018.1.0" }, "win32_nondevelopment_il2cpp/UnityPlayer.dll"),
                new Arch.Path(new string[0], "win32_nondevelopment_il2cpp/player_win.exe")
            }),
            new Arch("il2cpp x64 nondev", new [] {
                new Arch.Path(new [] { "2021.2.0", "2022.1.0" }, "win64_player_nondevelopment_il2cpp/UnityPlayer.dll"),
                new Arch.Path(new [] { "2017.2.0", "2018.1.0" }, "win64_nondevelopment_il2cpp/UnityPlayer.dll"),
                new Arch.Path(new string[0], "win64_nondevelopment_il2cpp/player_win.exe")
            }),

            //("il2cpp x64 dev", "win64_development_il2cpp/UnityPlayer.dll"),
            //("il2cpp x86 dev", "win32_development_il2cpp/UnityPlayer.dll"),
            //("mono x64 dev", "win64_development_mono/UnityPlayer.dll"),
            //("mono x86 dev", "win32_development_mono/UnityPlayer.dll"),
        };

        //private static readonly string targetDirectory = @"C:\Users\Hugo\Downloads\UDGB-master\UDGB-master\Output\Debug\tmp";
        private static readonly string targetDirectory = @"/mnt/hdd3t/unity_versions";

        internal enum HashResult
        {
            NF, // No File
            NH, // No Hash
            NV, // Not Valid,
            OK, // OK
            MF  // Multiple founds
        }

        internal class UnityVersionComparer : IComparer<string>
        {
            public int Compare(string left, string right)
            {
                int[] leftparts = left.Split(Path.DirectorySeparatorChar).Last().Split('.').Select(s => int.Parse(s)).ToArray();
                int[] rightparts = right.Split(Path.DirectorySeparatorChar).Last().Split('.').Select(s => int.Parse(s)).ToArray();
                long leftsum = leftparts[0] * 10000 + leftparts[1] * 100 + leftparts[2];
                long rightsum = rightparts[0] * 10000 + rightparts[1] * 100 + rightparts[2];

                if (leftsum > rightsum)
                    return 1;
                if (leftsum < rightsum)
                    return -1;
                return 0;
            }
        }


        private static string targetVersion;
        private static bool humanReadableOutput;

        static void Main(string[] args)
        {
            targetVersion = null;
            humanReadableOutput = true;
            foreach (string arg in args)
            {
                if (arg.StartsWith("--uv="))
                    targetVersion = arg.Substring("--uv=".Length);
                else if (arg.Equals("--nhro"))
                    humanReadableOutput = false;
            }

            //LoadConfig();

            if (humanReadableOutput)
            {
                Console.WriteLine("Checking the following signatures:");
                for (int iHashinfo = 0; iHashinfo < hashinfos.Length; ++iHashinfo)
                    Console.WriteLine("  " + hashinfos[iHashinfo].hashname);
                Console.WriteLine();

                Console.WriteLine("Labels:");
                PrintResult(HashResult.OK); Console.WriteLine("Signature is valid");
                PrintResult(HashResult.NF); Console.WriteLine("File Not Found");
                PrintResult(HashResult.NH); Console.WriteLine("No signature for the target architecture");
                PrintResult(HashResult.NV); Console.WriteLine("Signature not valid");
                PrintResult(HashResult.MF); Console.WriteLine("Signature found multiple times");
                Console.WriteLine();

                Console.Write("\t\t");
                for (int iArch = 0; iArch < archs.Length; ++iArch)
                {
                    if (iArch != 0)
                        Console.Write("|    ");
                    Console.Write(archs[iArch].name + "\t");
                }
                Console.WriteLine();
            }

            if (targetVersion == null)
            {
                List<string> directories = Directory.GetDirectories(targetDirectory).Where(d => !d.Contains("_tmp")).ToList();
                directories.Sort(new UnityVersionComparer());

                foreach (string rootpath in directories)
                {
                    string version = rootpath.Split(Path.DirectorySeparatorChar).Last();
                    ProcessUnityVersion(version, rootpath);
                }
            }
            else
            {
                string rootpath = Path.Combine(targetDirectory, targetVersion);
                if (!Directory.Exists(rootpath))
                    throw new FileNotFoundException("Version " + targetVersion + " doesn't exists");

                string version = targetVersion;
                ProcessUnityVersion(version, rootpath);
            }
        }

        private static void ProcessUnityVersion(string version, string rootpath)
        {
            HashResult[] results = new HashResult[archs.Length * hashinfos.Length];

            for (int iArch = 0; iArch < archs.Length; ++iArch)
            {
                var arch = archs[iArch];

                string path = rootpath + "/" + arch.paths.First(v => IsUnityVersionOverOrEqual(version, v.unityminversions)).path;
                if (!File.Exists(path))
                {
                    //Console.WriteLine(path + " NOT FOUND");
                    continue;
                }

                for (int iHashinfo = 0; iHashinfo < hashinfos.Length; ++iHashinfo)
                {
                    var hashinfo = hashinfos[iHashinfo];

                    bool hasHash = false;
                    foreach (var hash in hashinfo.hashes)
                    {
                        if (!IsValidArch(arch.name, hash.archs))
                            continue;

                        if (!IsUnityVersionOverOrEqual(version, hash.unityminversions))
                            continue;

                        hasHash = true;

                        results[iHashinfo + iArch * hashinfos.Length] = HashResult.NV + Sigscan(path, hash.hash);
                        break;
                    }

                    if (!hasHash)
                        results[iHashinfo + iArch * hashinfos.Length] = HashResult.NH;
                }
            }

            if (humanReadableOutput)
            {
                Console.Write(version.PadRight(16));
                for (int iArch = 0; iArch < archs.Length; ++iArch)
                {
                    if (iArch > 0)
                        Console.Write("|\t");

                    for (int iHashinfo = 0; iHashinfo < hashinfos.Length; ++iHashinfo)
                        PrintResult(results[iHashinfo + iArch * hashinfos.Length]);

                }
                Console.WriteLine();
            }
            else
            {
                string res = "RESULT_" + version + " {";
                for (int iArch = 0; iArch < archs.Length; ++iArch)
                {
                    if (iArch > 0)
                        res += ",";
                    res += $"\"{archs[iArch].name}\":{{";
                    for (int iHashinfo = 0; iHashinfo < hashinfos.Length; ++iHashinfo)
                    {
                        if (iHashinfo > 0)
                            res += ",";
                        res += $"\"{hashinfos[iHashinfo].hashname}\":{(int)results[iHashinfo + iArch * hashinfos.Length] - 2}";
                    }
                    res += "}";
                }
                res += "}";

                Console.Write(res);
            }
        }

        private static bool IsUnityVersionOverOrEqual(string currentversion, string[] validversions)
        {
            if (validversions == null || validversions.Length == 0)
                return true;

            string[] versionparts = currentversion.Split('.');

            foreach (string validversion in validversions)
            {
                string[] validversionparts = validversion.Split('.');

                if (
                    int.Parse(versionparts[0]) >= int.Parse(validversionparts[0]) &&
                    int.Parse(versionparts[1]) >= int.Parse(validversionparts[1]) &&
                    int.Parse(versionparts[2]) >= int.Parse(validversionparts[2]))
                    return true;
            }

            return false;
        }

        private static bool IsValidArch(string currentArch, string[] targetArchs)
        {
            foreach (string targetArch in targetArchs)
            {
                string[] currentwords = currentArch.Split(' ');

                bool isValid = true;
                foreach (string targetword in targetArch.Split(' '))
                    if (!currentwords.Contains(targetword))
                    {
                        isValid = false;
                        break;
                    }

                if (isValid)
                    return true;
            }

            return false;
        }

        internal static void PrintResult(HashResult result)
        {
            ConsoleColor color = ConsoleColor.Gray;
            switch (result)
            {
                case HashResult.NF: color = ConsoleColor.Yellow; break;
                case HashResult.NH: color = ConsoleColor.Magenta; break;
                case HashResult.NV: color = ConsoleColor.Red; break;
                case HashResult.OK: color = ConsoleColor.Green; break;
                default: color = ConsoleColor.Cyan; break;
            }
            Console.ForegroundColor = color;
            Console.Write((result >= HashResult.MF ? ((int)result).ToString() : result.ToString()) + "\t");
            Console.ResetColor();
        }

        internal static int Sigscan(string filepath, string signature)
        {
            return Sigscan(File.ReadAllBytes(filepath), signature);
        }

        internal static int Sigscan(byte[] module, string signature)
        {
            int moduleSize = module.Length;

            string signatureSpaceless = signature.Replace(" ", "");
            int signatureLength = signatureSpaceless.Length / 2;
            byte[] signatureBytes = new byte[signatureLength];
            bool[] signatureNullBytes = new bool[signatureLength];
            for (int i = 0; i < signatureLength; ++i)
            {
                if (signatureSpaceless[i * 2] == '?')
                    signatureNullBytes[i] = true;
                else
                    signatureBytes[i] = (byte)((GetHexVal(signatureSpaceless[i * 2]) << 4) + (GetHexVal(signatureSpaceless[(i * 2) + 1])));
            }

            long index = 0;
            long maxIndex = index + moduleSize;
            int processed = 0;

            int foundtimes = 0;

            while (index < maxIndex)
            {
                if (signatureNullBytes[processed] || module[index] == signatureBytes[processed])
                {
                    ++processed;

                    if (processed == signatureLength)
                    {
                        ++foundtimes;
                        processed = 0;
                    }
                }
                else
                {
                    processed = 0;
                }

                ++index;
            }

            return foundtimes;
        }

        private static int GetHexVal(char hex)
        {
            int val = (int)hex;
            return val - (val < 58 ? 48 : (val < 97 ? 55 : 87));
        }
    }
}
