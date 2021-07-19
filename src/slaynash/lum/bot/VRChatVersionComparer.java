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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import mono.cecil.AssemblyDefinition;
import mono.cecil.FieldDefinition;
import mono.cecil.ModuleDefinition;
import mono.cecil.PropertyDefinition;
import mono.cecil.ReaderParameters;
import mono.cecil.ReadingMode;
import mono.cecil.TypeDefinition;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.utils.ExceptionUtils;

public class VRChatVersionComparer {

    public static String obfMapUrl;

    public static synchronized void run(String manifestId, String branch, MessageReceivedEvent event) {
        String unityVersion = null;

        File steamdlfolder = new File("vrcdecomp/VRChat");
        if (steamdlfolder.exists()) {
            try {
                Files.walk(steamdlfolder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            catch (IOException e) {
                ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to delete old vrchat download folder", e);
                return;
            }
        }

        if (!new File("vrcdecomp/versions/" + branch + "_" + manifestId).exists()) {
            System.out.println("Downloading VRChat from Steam");
            if (event != null)
                event.getChannel().sendMessage("Downloading VRChat from Steam...").queue();
            try {
                Process p = Runtime.getRuntime().exec("dotnet vrcdecomp/depotdownloader/DepotDownloader.dll -app 438100 -depot 438101 -manifest " + manifestId + " -username hugoflores69 -remember-password -dir vrcdecomp/VRChat");
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
            if (event != null)
                event.getChannel().sendMessage("Running Cpp2IL...").queue();
            try {
                ProcessBuilder pb = new ProcessBuilder("sh", "-c", "./Cpp2IL-2021.1.2-Linux --game-path VRChat --exe-name VRChat --skip-analysis --skip-metadata-txts --disable-registration-prompts");
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

            try {
                new File("vrcdecomp/versions/" + branch + "_" + manifestId + "/cpp2il_out").mkdirs();
                Files.move(new File("vrcdecomp/cpp2il_out").toPath(), new File("vrcdecomp/versions/" + branch + "_" + manifestId + "/cpp2il_out").toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to move cpp2il_out", e);
                return;
            }

            try {
                Files.move(new File("vrcdecomp/VRChat/GameAssembly.dll").toPath(), new File("vrcdecomp/versions/" + branch + "_" + manifestId + "/GameAssembly.dll").toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e) {
                ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to move GameAssembly.dll", e);
                return;
            }

            try {
                Files.walk(steamdlfolder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            catch (IOException e) {
                ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to delete vrchat download folder", e);
                return;
            }
        }
        else {
            try {
                unityVersion = Files.readAllLines(new File("vrcdecomp/versions/" + branch + "_" + manifestId + "/unityversion.txt").toPath()).get(0);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to read unityversion.txt", e);
                return;
            }
        }

        if (!new File("unitydeps_" + unityVersion).exists()) {
            System.out.println("Downloading Unity dependencies");
            if (event != null)
                event.getChannel().sendMessage("Downloading and extracting Unity dependencies...").queue();
            try (
                BufferedInputStream in = new BufferedInputStream(new URL("https://github.com/LavaGang/Unity-Runtime-Libraries/raw/master/" + unityVersion + ".zip").openStream());
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
                Process p = Runtime.getRuntime().exec("unzip -o vrcdecomp/unitydeps.zip -d vrcdecomp/unitydeps_" + unityVersion);
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

            try {
                new File("vrcdecomp/unitydeps.zip").delete();
            }
            catch (Exception e) {
                ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to delete unitydeps.zip", e);
                return;
            }
        }


        System.out.println("Downloading the latest deobfuscation map");
        if (event != null)
            event.getChannel().sendMessage("Downloading and extracting the deobfuscation mapping...").queue();

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
        if (event != null)
            event.getChannel().sendMessage("Running Il2CppAssemblyUnhollower...").queue();
        try {
            Process p = Runtime.getRuntime().exec("mono vrcdecomp/unhollower/AssemblyUnhollower.exe " +
                "--input=vrcdecomp/versions/" + branch + "_" + manifestId + "/cpp2il_out " +
                "--output=vrcdecomp/versions/" + branch + "_" + manifestId + "/unhollower_out " +
                "--mscorlib=vrcdecomp/mscorlib.dll " +
                "--unity=vrcdecomp/unitydeps_" + unityVersion + " " +
                "--gameassembly=vrcdecomp/versions/" + branch + "_" + manifestId + "/GameAssembly.dll " +
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
        if (event != null)
            event.getChannel().sendMessage("Checking assembly").queue();

        AssemblyDefinition ad = AssemblyDefinition.readAssembly("vrcdecomp/versions/" + branch + "_" + manifestId + "/unhollower_out/Assembly-CSharp.dll", new ReaderParameters(ReadingMode.Deferred, new CecilAssemblyResolverProvider.AssemblyResolver()));
        ModuleDefinition mainModule = ad.getMainModule();

        List<String> missingTypes = new ArrayList<>();

        Map<String, String> obf2deobf = new HashMap<>();
        Map<String, TypeDefinition> assemblyTypes = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(mapData)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(";");
                obf2deobf.put(parts[0], parts[1]);
            }

            for (TypeDefinition typedef : mainModule.getAllTypes()) {
                assemblyTypes.put(typedef.getFullName().replace('/', '.'), typedef);
            }

            // for (TypeDefinition typedef : mainModule.getAllTypes())
            //     System.out.println(" > " + typedef.getFullName());

            for (Entry<String, String> entry : obf2deobf.entrySet()) {
                String obfname = entry.getKey();
                String deobfname = entry.getValue();
                if (obfname.startsWith("."))
                    obfname = obfname.substring(1);

                String fullname = "";

                String[] parts = obfname.split("::");
                boolean isFieldOrProperty = parts.length > 1;

                String[] nameparts = parts[0].split("\\.");
                int partsToProcess = isFieldOrProperty ? nameparts.length : nameparts.length - 1;
                for (int i = 0; i < partsToProcess; ++i) {
                    String obfParentName = "";
                    for (int j = 0; j <= i; ++j)
                        obfParentName += "." + nameparts[j];

                    String deobfParentName;
                    if ((deobfParentName = obf2deobf.get(obfParentName)) != null)
                        fullname += deobfParentName;
                    else
                        fullname += nameparts[i];

                    fullname += ".";
                }

                if (isFieldOrProperty)
                    fullname = fullname.substring(0, fullname.length() - 1);
                else
                    fullname += deobfname;

                if (isFieldOrProperty) {
                    // System.out.println(obfname + ":");
                    // System.out.println(" > fullname: " + fullname);

                    TypeDefinition typedef;
                    if ((typedef = assemblyTypes.get(fullname)) == null) {
                        missingTypes.add(fullname + "::" + deobfname);
                        continue;
                    }

                    boolean found = false;
                    System.out.println(" > enum: " + typedef.isEnum());
                    if (typedef.isEnum()) {
                        for (FieldDefinition fielddef : typedef.getFields()) {
                            // System.out.println(" >>> " + fielddef.getName());
                            if (fielddef.getName().equals(deobfname)) {
                                found = true;
                                break;
                            }
                        }
                        if (found)
                            continue;
                    }

                    for (PropertyDefinition propdef : typedef.getProperties()) {
                        // System.out.println(" >>> " + propdef.getName());
                        if (propdef.getName().equals(deobfname)) {
                            found = true;
                            break;
                        }
                    }
                    if (found)
                        continue;

                    missingTypes.add(fullname + "::" + deobfname);
                }
                else {
                    if (!assemblyTypes.containsKey(fullname))
                        missingTypes.add(fullname);
                }

            }
        }
        catch (Exception e) {
            ExceptionUtils.reportException("VRChat deobf map check failed", "Failed to read assembly", e);
            return;
        }

        ad.dispose();



        StringBuilder sb = new StringBuilder();
        System.out.println("Missing types:");
        for (String missingType : missingTypes) {
            System.out.println(" - " + missingType);
            sb.append(missingType + "\n");
        }

        System.out.println();
        System.out.println("Done.");

        if (event != null) {
            event.getChannel()
                .sendMessage("The map contains " + missingTypes.size() + " mismatching elements:")
                .addFile(sb.toString().getBytes(), "missingelements.txt")
                .queue();
        }
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
