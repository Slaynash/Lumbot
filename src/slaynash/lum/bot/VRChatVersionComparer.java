package slaynash.lum.bot;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import mono.cecil.AssemblyDefinition;
import mono.cecil.ModuleDefinition;
import mono.cecil.ReaderParameters;
import mono.cecil.ReadingMode;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.utils.ExceptionUtils;

public class VRChatVersionComparer {

    public static String obfMapUrl;

    public static void run(String manifestId, String branch, MessageReceivedEvent event) {
        String unityVersion = null;

        System.out.println("Downloading VRChat from Steam");
        try {
            Process p = Runtime.getRuntime().exec("dotnet vrcdecomp/depotdownloader/DepotDownloader.dll -app 438100 -depot 438101 -manifest " + manifestId + " -username hugoflores69 -remember-password -dir vrcdecomp/VRChat_" + branch);
            logAppOutput(p, "DepotDownloader");
            int returncode = p.waitFor();
            if (returncode != 0) {
                ExceptionUtils.reportException("VRChat deobf map check failed", "DepotDownloader returned " + returncode);
                return;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("VRChat deobf map check failed", "DepotDownloader failed to run", e);
            return;
        }

        System.out.println("Running Cpp2IL");
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "./Cpp2IL-2021.1.2-Linux --game-path VRChat_" + branch + " --exe-name VRChat --skip-analysis --skip-metadata-txts --disable-registration-prompts");
            pb.directory(new File("vrcdecomp"));
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = "";
                while ((line = br.readLine()) != null) {
                    System.out.println("[Cpp2IL] " + line);
                    if (unityVersion == null && line.contains("unity version to be "))
                        unityVersion = line.split("unity version to be ")[1];
                }
            }

            int returncode = p.waitFor();
            if (returncode != 0) {
                ExceptionUtils.reportException("VRChat deobf map check failed", "Cpp2IL returned " + returncode);
                return;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("VRChat deobf map check failed", "Cpp2IL failed to run", e);
            return;
        }

        if (unityVersion == null) {
            ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to get Unity version from Cpp2IL");
            return;
        }


        System.out.println("Downloading Unity dependencies");
        try (BufferedInputStream in = new BufferedInputStream(new URL("https://github.com/LavaGang/Unity-Runtime-Libraries/raw/master/" + unityVersion + ".zip").openStream());
            FileOutputStream fileOutputStream = new FileOutputStream("vrcdecomp/unitydeps.zip")
        ) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1)
                fileOutputStream.write(dataBuffer, 0, bytesRead);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to download dependencies for Unity " + unityVersion, e);
            return;
        }

        System.out.println("Extracting Unity dependencies");
        try {
            Process p = Runtime.getRuntime().exec("unzip -o vrcdecomp/unitydeps.zip -d vrcdecomp/unitydeps");
            logAppOutput(p, "unzip");
            int returncode = p.waitFor();
            if (returncode != 0) {
                ExceptionUtils.reportException("VRChat deobf map check failed", "unzip returned " + returncode);
                return;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("VRChat deobf map check failed", "unzip failed to run", e);
            return;
        }


        System.out.println("Downloading the latest obfuscation map");

        byte[] buffer = new byte[1024];

        ByteArrayOutputStream mapStream = new ByteArrayOutputStream();
        try (BufferedInputStream in = new BufferedInputStream(new URL(obfMapUrl).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream("vrcdecomp/deobfmap.csv.gz")
        ) {
            int bytesRead;
            while ((bytesRead = in.read(buffer, 0, 1024)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                mapStream.write(buffer, 0, bytesRead);
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to download current deobfuscation map", e);
            return;
        }

        System.out.println("Decompressing the obfuscation map");

        ByteArrayOutputStream decompressedStream = new ByteArrayOutputStream();
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(mapStream.toByteArray()))) {
            int len;
            while ((len = gis.read(buffer)) > 0)
                decompressedStream.write(buffer, 0, len);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to decompress current deobfuscation map", e);
            return;
        }

        byte[] mapData = decompressedStream.toByteArray();


        System.out.println("Running Unhollower");
        try {
            Process p = Runtime.getRuntime().exec("mono vrcdecomp/unhollower/AssemblyUnhollower.exe " +
                "--input=vrcdecomp/cpp2il_out " +
                "--output=vrcdecomp/unhollower_out " +
                "--mscorlib=vrcdecomp/mscorlib.dll " +
                "--unity=vrcdecomp/unitydeps " +
                "--gameassembly=vrcdecomp/VRChat_" + branch + "/GameAssembly.dll " +
                "--rename-map=vrcdecomp/deobfmap.csv.gz " +
                "--blacklist-assembly=Mono.Security --blacklist-assembly=Newtonsoft.Json --blacklist-assembly=Valve.Newtonsoft.Json");
            logAppOutput(p, "Il2CppAssemblyUnhollower");
            int returncode = p.waitFor();
            if (returncode != 0) {
                ExceptionUtils.reportException("VRChat deobf map check failed", "Il2CppAssemblyUnhollower returned " + returncode);
                return;
            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("VRChat deobf map check failed", "Il2CppAssemblyUnhollower failed to run", e);
            return;
        }


        System.out.println("Checking assembly");

        AssemblyDefinition ad = AssemblyDefinition.readAssembly("vrcdecomp/unhollower_out/Assembly-CSharp.dll", new ReaderParameters(ReadingMode.Deferred, new CecilAssemblyResolverProvider.AssemblyResolver()));
        ModuleDefinition mainModule = ad.getMainModule();

        List<String> missingTypes = new ArrayList<>();

        Map<String, String> obf2deobf = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(mapData)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(";");
                obf2deobf.put(parts[0], parts[1]);
            }

            // for (TypeDefinition typedef : mainModule.getAllTypes())
            //     System.out.println(" > " + typedef.getFullName());

            for (Entry<String, String> entry : obf2deobf.entrySet()) {
                String obfname = entry.getKey();
                String deobfname = entry.getValue();
                if (obfname.startsWith("."))
                    obfname = obfname.substring(1);

                String fullname = "";

                String[] nameparts = obfname.split("\\.");
                for (int i = 0; i < nameparts.length - 1; ++i) {
                    String obfParentName = "";
                    for (int j = 0; j <= i; ++j) {
                        obfParentName += "." + nameparts[j];
                    }

                    String deobfParentName;
                    if ((deobfParentName = obf2deobf.get(obfParentName)) != null)
                        fullname += deobfParentName;
                    else
                        fullname += nameparts[i];

                    fullname += ".";
                }

                fullname += deobfname;

                if (deobfname.contains("::")) {
                    // String[] memberParts = deobfname.split("::");
                    // String deobfClassname = obf2deobf.get(memberParts[0]);
                    // deobfname = memberParts[1];

                    // TypeDefinition typedef;
                    // if ((typedef = mainModule.getType(deobfClassname)) == null)
                    //     // missingTypes.add(deobfClassname);
                    //     continue;

                    // if (typedef.getFields())
                    //     // TODO
                }
                else {
                    if (mainModule.getType(fullname) == null)
                        missingTypes.add(fullname);
                }

            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to read assembly", e);
            return;
        }

        ad.dispose();


        System.out.println("Missing types:");
        for (String missingType : missingTypes)
            System.out.println(" - " + missingType);

        System.out.println();
        System.out.println("Done.");

    }

    private static void logAppOutput(Process process, String appname) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                System.out.println("[" + appname + "] " + line);
            }
        }
    }

}
