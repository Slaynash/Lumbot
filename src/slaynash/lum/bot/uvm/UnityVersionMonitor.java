package slaynash.lum.bot.uvm;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.gson.reflect.TypeToken;
import mono.cecil.AssemblyDefinition;
import mono.cecil.FieldDefinition;
import mono.cecil.MethodDefinition;
import mono.cecil.ModuleDefinition;
import mono.cecil.ParameterDefinition;
import mono.cecil.ReaderParameters;
import mono.cecil.ReadingMode;
import mono.cecil.TypeDefinition;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.NotImplementedException;
import slaynash.lum.bot.ConfigManager;
import slaynash.lum.bot.discord.JDAManager;
import slaynash.lum.bot.utils.ExceptionUtils;
import slaynash.lum.bot.utils.Utils;

public class UnityVersionMonitor {
    public static final String LOG_IDENTIFIER = "UnityVersionMonitor";

    private static final List<UnityICall> icalls = new ArrayList<>();

    private static final List<MonoStructInfo> monoStructs = new ArrayList<>() {
        {
            add(new MonoStructInfo("UnityEngine.Internal_DrawTextureArguments", "UnityEngine.CoreModule"));
            add(new MonoStructInfo("UnityEngine.Rendering.VertexAttribute", "UnityEngine.CoreModule",
                new MonoStructInfo("UnityEngine.Mesh/InternalShaderChannel", "UnityEngine.CoreModule")));
            add(new MonoStructInfo("UnityEngine.UIVertex", "UnityEngine.TextRenderingModule"));
        }
    };

    private static boolean initialisingUnityVersions = false;
    private static boolean isRunningCheck = false;

    private static final List<Thread> runningThreads = new ArrayList<>();
    private static Thread mainThread = null;

    public static void start() {
        if (!ConfigManager.mainBot)
            return;

        loadInstalledVersionCache();
        loadMonoStructCache();
        loadIcalls();

        Thread thread = new Thread(() -> {

            boolean firstRun = true;

            while (true) {

                if (firstRun)
                    firstRun = false;
                else
                    try {
                        Thread.sleep(60 * 60 * 1000);
                    }
                    catch (InterruptedException e) {
                        ExceptionUtils.reportException("UnityVersionMonitor was interrupted", e);
                        return;
                    }

                try {
                    runOnce();
                }
                catch (InterruptedException e) {
                    ExceptionUtils.reportException("UnityVersionMonitor was interrupted", e);
                    return;
                }
                catch (Exception e) {
                    ExceptionUtils.reportException("Unhandled exception in UnityVersionMonitor", e);
                    isRunningCheck = false;
                }
            }

        }, "UnityVersionMonitor");
        thread.setDaemon(true);
        runningThreads.add(thread);
        mainThread = thread;
        thread.start();
    }

    private static void runOnce() throws InterruptedException {

        List<UnityVersion> remoteVersions = UnityDownloader.fetchUnityVersions();
        if (remoteVersions == null)
            return;

        UnityDownloader.filterNewVersionsAndLog(remoteVersions);
        if (remoteVersions.size() == 0)
            return;
        initialisingUnityVersions = remoteVersions.size() >= 10;

        if (isRunningCheck) {
            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessage("Waiting for running check to finish").queue();
            try {
                waitForEndOfRunningCheck();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
        
        isRunningCheck = true;

        loadIcalls();

        for (UnityVersion newVersion : remoteVersions) {
            UnityDownloader.downloadUnity(newVersion);

            // run tools sanity checks

            try {
                runHashChecker(newVersion.version);
            }
            catch (InterruptedException e) {
                ExceptionUtils.reportException("HashChecker run was aborted before start " + newVersion.version, e);
            }
            catch (Exception e) {
                ExceptionUtils.reportException("Failed to run HashChecker for Unity " + newVersion.version, e);
            }
            if (!initialisingUnityVersions) {
                runICallChecker(newVersion.version, null);
                runMonoStructChecker(newVersion.version);
            }
            // VFTables Checker
        }

        if (new File(UnityUtils.downloadPath).list() == null) {
            ExceptionUtils.reportException("Unity download path is missing");
            return;
        }

        List<String> allUnityVersions = new ArrayList<>();
        for (String version : new File(UnityUtils.downloadPath).list())
            if (!version.endsWith("_tmp"))
                allUnityVersions.add(version);

        allUnityVersions.sort(new UnityVersion.Comparator());


        // ICall init check
        if (initialisingUnityVersions) {

            StringBuilder sb = new StringBuilder();
            for (String version : allUnityVersions) {
                runICallChecker(version, sb);
                sb.append(version).append("\n---------------------------------------------------------------------------------------\n");
            }

            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("Icall checks results:", Color.gray)
            ).addFiles(FileUpload.fromData(sb.toString().getBytes(), "icall_init_report.txt")).queue();
        }

        // MonoStruct init check
        boolean originalInitialisingUnityVersions = initialisingUnityVersions;
        if (monoStructs.size() > 0 && monoStructs.get(0).rows.size() == 0)
            initialisingUnityVersions = true;

        if (initialisingUnityVersions) {
            for (MonoStructInfo msi : monoStructs)
                msi.rows.clear();

            for (String version : allUnityVersions)
                runMonoStructChecker(version);

            for (MonoStructInfo msi : monoStructs) {
                StringBuilder results = new StringBuilder();
                for (MonoStructRow msr : msi.rows)
                    results.append("\n\n`").append(String.join("`, `", msr.unityVersions)).append("`\n```\n").append(String.join("\n", msr.fields)).append("```");
                JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                    Utils.wrapMessageInEmbed("MonoStruct checks results for " + msi.name + ":" + results, Color.gray)
                ).queue();
            }
        }
        initialisingUnityVersions = originalInitialisingUnityVersions;

        // VFTable init check
        // TODO VFTable init check

        isRunningCheck = false;
    }

    private static void waitForEndOfRunningCheck() throws InterruptedException {
        while (isRunningCheck) {
            Thread.sleep(100);
        }
    }





    

    public static void loadInstalledVersionCache() {
        UnityDownloader.loadInstalledVersionCache();
    }

    public static void loadMonoStructCache() {
        try {
            System.out.println("Loading MonoStructs cache");
            Map<String, List<MonoStructRow>> monoStructsSave = UnityUtils.gson.fromJson(Files.readString(Paths.get("unityversionsmonitor/monoStructCache.json")), new TypeToken<HashMap<String, List<MonoStructRow>>>(){}.getType());
            for (MonoStructInfo msi : monoStructs) {
                List<MonoStructRow> msr = monoStructsSave.get(msi.name);
                if (msr != null)
                    msi.rows.addAll(msr);
            }
            System.out.println("Done loading MonoStructs cache");
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to load MonoStructs cache", e);
        }
    }

    public static void loadIcalls() {
        try {
            System.out.println("Loading Icalls");
            List<UnityICall> storedIcalls = UnityUtils.gson.fromJson(Files.readString(Paths.get("unityversionsmonitor/icalls.jsonc")), new TypeToken<ArrayList<UnityICall>>(){}.getType());
            icalls.clear();
            icalls.addAll(storedIcalls);
            System.out.println("Done loading Icalls");
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to load Icalls", e);
        }
    }

    public static void saveMonoStructCache() {
        try {
            Map<String, List<MonoStructRow>> monoStructsSave = new HashMap<>();
            for (MonoStructInfo msi : monoStructs)
                monoStructsSave.put(msi.name, msi.rows);
            Files.write(Paths.get("unityversionsmonitor/monoStructCache.json"), UnityUtils.gson.toJson(monoStructsSave).getBytes());
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to save MonoStructs cache", e);
        }
    }

    

    private static void runHashChecker(String unityVersion) throws IOException, InterruptedException {
        Map<String, Map<String, Integer>> results = null;

        System.out.println("Running command: \"sh\" \"-c\" \"mono unityversionsmonitor/HashChecker.exe --uv=" + unityVersion + " --nhro\"");
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", "mono unityversionsmonitor/HashChecker.exe --uv=" + unityVersion + " --nhro");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("[HashChecker] " + line);
                if (line.startsWith("RESULT_")) {
                    String[] resultParts = line.substring("RESULT_".length()).split(" ", 2);
                    results = UnityUtils.gson.fromJson(resultParts[1], new TypeToken<HashMap<String, HashMap<String, Integer>>>(){}.getType());
                }
            }
        }

        p.waitFor();

        if (results == null) {
            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("HashChecker has reported no result for Unity " + unityVersion, Color.red)
            ).queue();
            return;
        }

        StringBuilder reports = new StringBuilder();
        for (Entry<String, Map<String, Integer>> arch : results.entrySet()) {
            for (Entry<String, Integer> hash : arch.getValue().entrySet()) {
                if (hash.getValue() > 1)
                    reports.append(arch.getKey()).append(" - ").append(hash.getKey()).append(": ").append(hash.getValue()).append(" results\n");
                else if (hash.getValue() == 0)
                    reports.append(arch.getKey()).append(" - ").append(hash.getKey()).append(": Hash not valid\n");
                else if (hash.getValue() == -1)
                    reports.append(arch.getKey()).append(" - ").append(hash.getKey()).append(": No hash for this version\n");
                else if (hash.getValue() == -2)
                    reports.append(arch.getKey()).append(" - ").append(hash.getKey()).append(": File not found\n");
            }
        }

        if (reports.length() > 0) {
            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("Failed to validate all hashes for Unity " + unityVersion + ":\n\n" + reports, Color.red)
            ).queue();
        }
        else {
            if (!initialisingUnityVersions) {
                JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                    Utils.wrapMessageInEmbed("Hash check succeeded for Unity " + unityVersion, Color.green)
                ).queue();
            }
        }
    }

    public static void runICallChecker(String unityVersion, StringBuilder stringBuilder) {

        System.out.println("[" + unityVersion + "] Running icall check for Unity " + unityVersion);

        StringBuilder reportNoValidVersion = new StringBuilder();

        Map<String, List<UnityICall>> assemblies = new HashMap<>();
        for (UnityICall icall : icalls) {
            if (!UnityVersion.isOverOrEqual(unityVersion, icall.unityVersions)) {
                boolean found = false;
                for (UnityICall oldICallEntry : icall.oldICalls) {
                    if (UnityVersion.isOverOrEqual(unityVersion, oldICallEntry.unityVersions)) {
                        System.out.println("[" + unityVersion + "] Icall " + icall.icall + " => " + oldICallEntry.icall);
                        icall = oldICallEntry;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.out.println("[" + unityVersion + "] ICall has no valid version: " + icall.icall);
                    reportNoValidVersion.append(icall.icall);
                    continue;
                }
            }

            List<UnityICall> icallsForAssembly = assemblies.computeIfAbsent(icall.assemblyName, k -> new ArrayList<>());
            icallsForAssembly.add(icall);
        }

        StringBuilder reportNoType = new StringBuilder();
        StringBuilder reportNoMethod = new StringBuilder();
        StringBuilder reportMismatchingParams = new StringBuilder();

        for (Entry<String, List<UnityICall>> assemblyEntry : assemblies.entrySet()) {
            String assemblyName = assemblyEntry.getKey();
            AssemblyDefinition ad = AssemblyDefinition.readAssembly(UnityUtils.downloadPath + "/" + unityVersion + "/" + UnityUtils.getMonoManagedSubpath(unityVersion) + "/" + assemblyName + ".dll", new ReaderParameters(ReadingMode.Deferred, new CecilAssemblyResolverProvider.AssemblyResolver()));
            ModuleDefinition mainModule = ad.getMainModule();

            for (UnityICall icall : assemblyEntry.getValue()) {
                String[] icallParts = icall.icall.split("::", 2);
                TypeDefinition typeDefinition = mainModule.getType(icallParts[0]);
                if (typeDefinition == null) {
                    reportNoType.append("\n").append(icall.icall);
                    continue;
                }

                boolean found = false;
                boolean foundAndMatches = false;
                StringBuilder similarMethods = new StringBuilder();
                for (MethodDefinition md : typeDefinition.getMethods()) {
                    if (md.getName().equals(icallParts[1])) {
                        found = true;
                        boolean valid = true;
                        List<ParameterDefinition> parameterDefs = md.getParameters();

                        String returnTypeTranslated = md.getReturnType().getFullName();
                        if (md.getReturnType().isArray())
                            returnTypeTranslated += "[]";

                        List<String> parameterDefsTranslated = parameterDefs.stream()
                            .map(pd -> {
                                String fullname = pd.getParameterType().getFullName();
                                if (fullname.endsWith("&"))
                                    fullname = "ref " + fullname.substring(0, fullname.length() - 1);
                                if (pd.getParameterType().isArray())
                                    fullname += "[]";

                                return fullname;
                            })
                            .collect(Collectors.toList());
                        if (!md.isStatic())
                            parameterDefsTranslated.add(0, icallParts[0]);

                        if (!returnTypeTranslated.equals(icall.returnType) || parameterDefsTranslated.size() != icall.parameters.length) {
                            valid = false;
                        }
                        else {
                            for (int i = 0; i < icall.parameters.length; ++i) {
                                if (!parameterDefsTranslated.get(i).equals(icall.parameters[i])) {
                                    valid = false;
                                    break;
                                }
                            }
                        }

                        if (valid) {
                            foundAndMatches = true;
                            break;
                        }
                        else
                            similarMethods.append("\n`").append(returnTypeTranslated).append(" <- ").append(String.join(", ", parameterDefsTranslated)).append("`");
                        // ELSE it's valid
                    }
                }

                if (!found) {
                    reportNoMethod.append("\n").append(icall.icall);
                    System.out.println("[" + unityVersion + "] ICall method not found: " + icall.icall);
                }
                else if (!foundAndMatches) {
                    reportMismatchingParams.append("\n\n").append(icall.icall);
                    reportMismatchingParams.append("\nExpected:\n`").append(icall.returnType).append(" <- ").append(String.join(", ", icall.parameters)).append("`");
                    reportMismatchingParams.append("\nFound:").append(similarMethods);
                    System.out.println("[" + unityVersion + "] ICall parameters mismatches for " + icall.icall);
                    System.out.println("[" + unityVersion + "] Expected: " + icall.returnType + " <- " + String.join(", ", icall.parameters));
                    System.out.println("[" + unityVersion + "] Found: " + similarMethods);
                }
            }

            ad.dispose();
        }
        // 3. Send result

        if (!initialisingUnityVersions) {
            boolean hasError = false;
            if (reportNoValidVersion.length() > 0) {
                hasError = true;
                if (stringBuilder != null)
                    stringBuilder.append("**The following icalls have no definition for Unity ").append(unityVersion).append(":**").append(reportNoMethod);
                else
                    JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                        Utils.wrapMessageInEmbed("**The following icalls have no definition for Unity " + unityVersion + ":**" + reportNoMethod, Color.red)
                    ).queue();
            }
            if (reportNoType.length() > 0) {
                hasError = true;
                if (stringBuilder != null)
                    stringBuilder.append("**Failed to find the following icall managed types for Unity ").append(unityVersion).append(":**").append(reportNoType);
                else
                    JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                        Utils.wrapMessageInEmbed("**Failed to find the following icall managed types for Unity " + unityVersion + ":**" + reportNoType, Color.red)
                    ).queue();
            }
            if (reportNoMethod.length() > 0) {
                hasError = true;
                if (stringBuilder != null)
                    stringBuilder.append("**Failed to find the following icall managed methods for Unity ").append(unityVersion).append(":**").append(reportNoMethod);
                else
                    JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                        Utils.wrapMessageInEmbed("**Failed to find the following icall managed methods for Unity " + unityVersion + ":**" + reportNoMethod, Color.red)
                    ).queue();
            }
            if (reportMismatchingParams.length() > 0) {
                hasError = true;
                if (stringBuilder != null)
                    stringBuilder.append("**The following icall methods mismatch for Unity ").append(unityVersion).append(":**").append(reportMismatchingParams);
                else
                    JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                    Utils.wrapMessageInEmbed("**The following icall methods mismatch for Unity " + unityVersion + ":**" + reportMismatchingParams, Color.red)
                ).queue();
            }

            if (!hasError)
                if (stringBuilder != null)
                    stringBuilder.append("ICall check succeeded for Unity ").append(unityVersion);
                else
                    JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                        Utils.wrapMessageInEmbed("ICall check succeeded for Unity " + unityVersion, Color.green)
                    ).queue();
        }
    }

    public static void runMonoStructChecker(String unityVersion) {

        int successfullChecks = 0;

        for (MonoStructInfo msi : monoStructs) {

            // 1. Fetch struct

            AssemblyDefinition ad = AssemblyDefinition.readAssembly(UnityUtils.downloadPath + "/" + unityVersion + "/" + UnityUtils.getMonoManagedSubpath(unityVersion) + "/" + msi.assembly + ".dll", new ReaderParameters(ReadingMode.Deferred, new CecilAssemblyResolverProvider.AssemblyResolver()));
            ModuleDefinition mainModule = ad.getMainModule();

            TypeDefinition typeDefinition = mainModule.getType(msi.name);
            if (typeDefinition == null) {
                for (MonoStructInfo alt : msi.altStructs) {
                    typeDefinition = mainModule.getType(alt.name);
                    if (typeDefinition != null)
                        break;
                }
                if (typeDefinition == null) {
                    System.out.println("Failed to validate the following MonoStruct for Unity " + unityVersion + ": " + msi.name);
                    continue;
                }
            }

            List<String> fields = new ArrayList<>();

            if (typeDefinition.isEnum()) {
                int fieldOffset = 0;
                for (FieldDefinition fieldDef : typeDefinition.getFields()) {
                    if (fieldDef.getName().equals("value__"))
                        continue;

                    int fieldConstant = (int) fieldDef.getConstant();
                    if (fieldConstant != fieldOffset)
                        fields.add(fieldDef.getName() + " = " + (fieldOffset = fieldConstant));
                    else
                        fields.add(fieldDef.getName());

                    fieldOffset++;
                }
            }
            else
                for (FieldDefinition fieldDef : typeDefinition.getFields())
                    if (!fieldDef.isStatic())
                        fields.add(fieldDef.getFieldType().getFullName() + " " + fieldDef.getName());

            ad.dispose();


            // 2. Compare

            // Foreach MSI, GET the 1st one WHERE compareUnityVersions(unityVersion, msi.version) > 0 OR NULL
            MonoStructRow msrTarget = null;
            int msrTargetIndex = 0;
            for (int iMSR = 0; iMSR < msi.rows.size(); ++iMSR) {
                MonoStructRow msr = msi.rows.get(iMSR);
                for (String uv : msr.unityVersions) {
                    if (UnityVersion.compare(unityVersion, uv) > 0) {
                        msrTarget = msr;
                        msrTargetIndex = iMSR;
                        break;
                    }
                }
                if (msrTarget != null)
                    break;
            }
            // If found, check if struct matches -> fieldsMatch = true
            boolean fieldsMatch = msrTarget != null && monoStructContainsFields(msrTarget, fields);
            // If 'fieldsMatch'
            if (fieldsMatch) {
                // If for all versions of row, !isUnityVersionOverOrEqual(unityVersion, matchedVersion) THEN add version string Else OK
                boolean isVersionValid = UnityVersion.isOverOrEqual(unityVersion, msrTarget.unityVersions.toArray(new String[0]));

                if (isVersionValid)
                    ++successfullChecks;
                else {
                    String oldUnityVersions = String.join("`, `", msrTarget.unityVersions);
                    msrTarget.unityVersions.add(unityVersion);
                    if (!initialisingUnityVersions)
                        JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                            Utils.wrapMessageInEmbed("New Minimal Unity versions for MonoStruct " + msi.name + ":\n**OLD:** `" + oldUnityVersions + "`\n**NEW:** `" + String.join("`, `", msrTarget.unityVersions) + "`", Color.red)
                        ).queue();
                }
            }
            // Else
            else if (msrTarget != null) {
                if (msrTargetIndex > 0)
                    msrTarget = msi.rows.get(--msrTargetIndex);
                if (msrTarget.fields.size() == fields.size()) {
                    fieldsMatch = true;
                    for (int iField = 0; iField < msrTarget.fields.size(); ++iField) {
                        if (!msrTarget.fields.get(iField).equals(fields.get(iField))) {
                            fieldsMatch = false;
                            break;
                        }
                    }
                }

                if (fieldsMatch) {
                    String oldUnityVersions = String.join("`, `", msrTarget.unityVersions);
                    msrTarget.unityVersions.add(unityVersion);
                    if (!initialisingUnityVersions)
                        JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                            Utils.wrapMessageInEmbed("New Minimal Unity versions for MonoStruct " + msi.name + ":\n**OLD:** `" + oldUnityVersions + "`\n**NEW:** `" + String.join("`, `", msrTarget.unityVersions) + "`", Color.red)
                        ).queue();
                }
                else {
                    msi.rows.add(msrTargetIndex, new MonoStructRow(unityVersion, fields));

                    StringBuilder report = new StringBuilder(msi.name + "\n```\n");
                    for (String field : fields)
                        report.append(field).append("\n");
                    report.append("```");
                    if (!initialisingUnityVersions)
                        JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                            Utils.wrapMessageInEmbed("New MonoStructs " + msi.name + " for Unity " + unityVersion + ":\n\n" + report, Color.red)
                        ).queue();
                }
            }
            else {
                // Add new row on the beginning
                msi.rows.add(0, new MonoStructRow(unityVersion, fields));

                StringBuilder report = new StringBuilder(msi.name + "\n```\n");
                for (String field : fields)
                    report.append(field).append("\n");
                report.append("```");
                if (!initialisingUnityVersions)
                    JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                        Utils.wrapMessageInEmbed("New MonoStructs " + msi.name + " for Unity " + unityVersion + ":\n\n" + report, Color.red)
                    ).queue();
            }
        }

        if (!initialisingUnityVersions && successfullChecks == monoStructs.size()) {
            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("MonoStruct checks succeeded for Unity " + unityVersion, Color.green)
            ).queue();
        }

        saveMonoStructCache();
    }

    

    private static boolean monoStructContainsFields(MonoStructRow msi, List<String> fields) {
        if (msi.fields.size() == fields.size()) {
            for (int i = 0; i < fields.size(); ++i)
                if (!msi.fields.get(i).equals(fields.get(i)))
                    return false;

            return true;
        }
        return false;
    }

    

    private static class UnityICall {
        public final String icall;
        public final byte[] icallUtf8;
        public final String[] unityVersions;
        public final String assemblyName;
        public final String returnType;
        public final String[] parameters;
        public final List<UnityICall> oldICalls = new ArrayList<>(0);

        public UnityICall(String icall, String[] unityVersions, String assemblyName, String returnType, String[] parameters, UnityICall... oldICalls) {
            this.icall = icall;
            this.icallUtf8 = icall.getBytes(StandardCharsets.UTF_8);
            this.unityVersions = unityVersions;
            this.assemblyName = assemblyName;
            this.returnType = returnType;
            this.parameters = parameters;
            if (oldICalls != null)
                Collections.addAll(this.oldICalls, oldICalls);
        }
    }

    private static class MonoStructInfo {
        public final String name;
        public final String assembly;
        public final List<MonoStructRow> rows = new ArrayList<>();
        public final MonoStructInfo[] altStructs;

        public MonoStructInfo(String fullname, String assembly, MonoStructInfo... altStructs) {
            this.name = fullname;
            this.assembly = assembly;
            this.altStructs = altStructs != null ? altStructs : new MonoStructInfo[0];
        }
    }

    private static class MonoStructRow {
        public final List<String> unityVersions;
        public final List<String> fields;

        public MonoStructRow(String unityVersion, List<String> fields) {
            unityVersions = new ArrayList<>(1);
            unityVersions.add(unityVersion);
            this.fields = fields;
        }
    }




    public static void runFullICallCheck() {
        if (isRunningCheck) {
            while (isRunningCheck)
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessage("Waiting for running check to finish").queue();
        }
        isRunningCheck = true;
        loadIcalls();

        try {
            List<String> allUnityVersions = new ArrayList<>();
            for (String version : new File(UnityUtils.downloadPath).list())
                if (!version.endsWith("_tmp"))
                    allUnityVersions.add(version);

            allUnityVersions.sort(new UnityVersion.Comparator());

            StringBuilder sb = new StringBuilder();
            for (String version : allUnityVersions) {
                sb.append("\n").append(version).append(" ---------------------------------------------------------------------------------------\n");
                runICallChecker(version, sb);
            }

            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("Icall checks results:", Color.gray)
            ).addFiles(FileUpload.fromData(sb.toString().getBytes(), "icall_init_report.txt")).queue();
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Unhandled exception in UnityVersionMonitor", e);
        }

        isRunningCheck = false;
    }

    // TODO
    // public static void runFullDownloadCheck() {
    //     if (isRunningCheck) {
    //         while (isRunningCheck)
    //             try {
    //                 Thread.sleep(100);
    //             }
    //             catch (InterruptedException e) {
    //                 e.printStackTrace();
    //             }
    //         JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessage("Waiting for running check to finish").queue();
    //     }
    //     isRunningCheck = true;

    //     // Cleanup download folder
    //     try {
    //         Files.walk(Path.of(UnityUtils.downloadPath))
    //             .sorted(Comparator.reverseOrder())
    //             .map(Path::toFile)
    //             .forEach(File::delete);
    //     }
    //     catch (IOException e) {
    //         ExceptionUtils.reportException("Failed to cleanup download folder", e);
    //         isRunningCheck = false;
    //         return;
    //     }

    //     List<UnityVersion> remoteVersions = UnityDownloader.fetchUnityVersions();
    //     if (remoteVersions == null) {
    //         JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessage("Failed to fetch unity versions").queue();
    //         isRunningCheck = false;
    //         return;
    //     }
    //     for (UnityVersion newVersion : remoteVersions)
    //         UnityDownloader.downloadUnity(newVersion);

    //     isRunningCheck = false;
    // }

    private static class HashCheckerArch {
        private static class Path {
            public String[] unityVersions;
            public String path;

            public Path(String[] unityVersions, String path) {
                this.unityVersions = unityVersions;
                this.path = path;
            }
        }

        public String name;
        public HashCheckerArch.Path[] paths;

        private HashCheckerArch(String name, HashCheckerArch.Path[] paths) {
            this.name = name;
            this.paths = paths;
        }
    }

    private static HashCheckerArch[] hashArchs = new HashCheckerArch[]
    {
        new UnityVersionMonitor.HashCheckerArch("mono x86 nondev", new UnityVersionMonitor.HashCheckerArch.Path[] {
            new UnityVersionMonitor.HashCheckerArch.Path(new String[] { "2021.2.0", "2022.1.0" }, "win32_player_nondevelopment_mono/UnityPlayer.dll"),
            new UnityVersionMonitor.HashCheckerArch.Path(new String[] { "2017.2.0", "2018.1.0" }, "win32_nondevelopment_mono/UnityPlayer.dll"),
            new UnityVersionMonitor.HashCheckerArch.Path(new String[0], "win32_nondevelopment_mono/player_win.exe")
        }),
        new HashCheckerArch("mono x64 nondev", new HashCheckerArch.Path[] {
            new HashCheckerArch.Path(new String[] { "2021.2.0", "2022.1.0" }, "win64_player_nondevelopment_mono/UnityPlayer.dll"),
            new HashCheckerArch.Path(new String[] { "2017.2.0", "2018.1.0" }, "win64_nondevelopment_mono/UnityPlayer.dll"),
            new HashCheckerArch.Path(new String[0], "win64_nondevelopment_mono/player_win.exe")
        }),

        new HashCheckerArch("il2cpp x86 nondev", new HashCheckerArch.Path[] {
            new HashCheckerArch.Path(new String[] { "2021.2.0", "2022.1.0" }, "win32_player_nondevelopment_il2cpp/UnityPlayer.dll"),
            new HashCheckerArch.Path(new String[] { "2017.2.0", "2018.1.0" }, "win32_nondevelopment_il2cpp/UnityPlayer.dll"),
            // new HashCheckerArch.Path(new String[0], "win32_nondevelopment_il2cpp/player_win.exe")
        }),
        new HashCheckerArch("il2cpp x64 nondev", new HashCheckerArch.Path[] {
            new HashCheckerArch.Path(new String[] { "2021.2.0", "2022.1.0" }, "win64_player_nondevelopment_il2cpp/UnityPlayer.dll"),
            new HashCheckerArch.Path(new String[] { "2017.2.0", "2018.1.0" }, "win64_nondevelopment_il2cpp/UnityPlayer.dll"),
            // new HashCheckerArch.Path(new String[0], "win64_nondevelopment_il2cpp/player_win.exe")
        }),

        //("il2cpp x64 dev", "win64_development_il2cpp/UnityPlayer.dll"),
        //("il2cpp x86 dev", "win32_development_il2cpp/UnityPlayer.dll"),
        //("mono x64 dev", "win64_development_mono/UnityPlayer.dll"),
        //("mono x86 dev", "win32_development_mono/UnityPlayer.dll"),
    };

    public static void runFullIntegrityCheck() {
        if (isRunningCheck) {
            while (isRunningCheck)
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessage("Waiting for running check to finish").queue();
        }
        isRunningCheck = true;
        loadIcalls();

        try {
            StringBuilder reportBuilder = new StringBuilder();

            List<String> versions = UnityDownloader.fetchUnityVersions().stream()
                .map(uv -> uv.version)
                .distinct()
                .sorted(new UnityVersion.Comparator())
                .toList();

            for (String version : versions) {
                if (!new File(UnityUtils.downloadPath + "/" + version).exists()) {
                    reportBuilder.append(version + ": Missing files" + "\n");
                    continue;
                }

                for (HashCheckerArch arch : hashArchs) {
                    String path = null;
                    for (HashCheckerArch.Path archPath : arch.paths) {
                        if (UnityVersion.isOverOrEqual(version, archPath.unityVersions)) {
                            path = archPath.path;
                            break;
                        }
                    }

                    if (path == null) { // Unsupported, likely il2cpp <2018
                        // reportBuilder.append(version + ": No path for arch " + arch.name + "\n");
                        continue;
                    }

                    if (!new File(UnityUtils.downloadPath + "/" + version + "/" + path).exists()) {
                        reportBuilder.append(version + ": Missing file " + path + "\n");
                        continue;
                    }
                }
            }

            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                Utils.wrapMessageInEmbed("Integrity result:", Color.gray)
            ).addFiles(FileUpload.fromData(reportBuilder.toString().getBytes(), "integrity_report.txt")).queue();
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Unhandled exception in UnityVersionMonitor", e);
        }

        isRunningCheck = false;
    }

    public static void redownloadVersion(String version) {
        if (isRunningCheck) {
            while (isRunningCheck)
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessage("Waiting for running check to finish").queue();
        }
        isRunningCheck = true;

        try {
            List<UnityVersion> versions = UnityDownloader.fetchUnityVersions();
            UnityVersion targetVersion = null;
            for (UnityVersion uv : versions) {
                if (uv.version.equals(version)) {
                    targetVersion = uv;
                    break;
                }
            }

            if (targetVersion == null) {
                JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                    Utils.wrapMessageInEmbed("Failed to find Unity version " + version, Color.red)
                ).queue();
                isRunningCheck = false;
                return;
            }

            UnityDownloader.downloadUnity(targetVersion);

            JDAManager.getJDA().getTextChannelById(876466104036393060L /* #lum-status */).sendMessageEmbeds(
                    Utils.wrapMessageInEmbed("Done downloading unity version " + version, Color.red)
                ).queue();
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Unhandled exception in UnityVersionMonitor", e);
        }

        isRunningCheck = false;
    }

    public static void startThread(Runnable runnable, String subcommandName) {
        Thread t = new Thread(runnable, "UnityVersionMonitor command " + subcommandName);
        t.setDaemon(true);
        runningThreads.add(t);
        t.start();
    }

    public static void killThreads() {
        for (Thread thread : runningThreads)
            thread.interrupt();

        runningThreads.clear();
        mainThread = null;
    }

    public static void startMainThread() {
        if (mainThread != null && mainThread.isAlive())
            return;

        start();
    }

    public static void setEnabled(boolean enabled) {
        throw new NotImplementedException();
    }

}
