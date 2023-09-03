package slaynash.lum.bot.uvm;

import java.io.IOException;
import java.io.RandomAccessFile;

public class VFTableOffsetFinder implements AutoCloseable {

    private static record SectionHeader(int virtualSize, int virtualAddress, int rawSize, int rawAddress) { }

    private String pdbPath;
    private boolean is64bit;
    private SectionHeader[] sections;
    private RandomAccessFile dllReader;
    private long imageBase;

    public VFTableOffsetFinder(String dllPath, String pdbPath, boolean is64bit) throws IOException, InterruptedException {
        this.pdbPath = pdbPath;
        this.is64bit = is64bit;
        this.sections = pdbUtilGetHeadersOffset(pdbPath);
        this.dllReader = new RandomAccessFile(dllPath, "r");
        this.imageBase = is64bit ? 0x180000000L : 0x10000000L;
    }

    public void close() throws Exception {
        dllReader.close();
    }

    
    public int getVFTableOffset(String vftableSymbol, String vfSymbol) throws IOException, InterruptedException {
        long[] funcOffsetPDB = pdbUtilGetSymbolOffset(pdbPath, vfSymbol);
        long[] vftableOffsetPDB = pdbUtilGetSymbolOffset(pdbPath, vftableSymbol);

        long section1PDBToVirtual = sections[0].virtualAddress + imageBase;
        long section2PDBToRaw = sections[1].rawAddress;
        long section2RawEnd = sections[1].rawAddress + sections[1].rawSize;

        long vfRVA = funcOffsetPDB[1] + section1PDBToVirtual;
        long vftableRaw = vftableOffsetPDB[1] + section2PDBToRaw;

        int ret = -1;
        dllReader.seek(vftableRaw);
        if (is64bit) {
            for (long i = vftableRaw; i < section2RawEnd; i += 8) {
                long targetRVA =
                    ((long)dllReader.read() <<  0) |
                    ((long)dllReader.read() <<  8) |
                    ((long)dllReader.read() << 16) |
                    ((long)dllReader.read() << 24) |
                    ((long)dllReader.read() << 32) |
                    ((long)dllReader.read() << 40) |
                    ((long)dllReader.read() << 48) |
                    ((long)dllReader.read() << 56);

                if (targetRVA == vfRVA) {
                    ret = (int)(i - vftableRaw);
                    break;
                }
            }
        }
        else {
            for (long i = vftableRaw; i < section2RawEnd; i += 4) {
                int targetRVA =
                    (dllReader.read() <<  0) |
                    (dllReader.read() <<  8) |
                    (dllReader.read() << 16) |
                    (dllReader.read() << 24);

                if (targetRVA == vfRVA) {
                    ret = (int)(i - vftableRaw);
                    break;
                }
            }
        }

        return ret;
    }

    private static SectionHeader[] pdbUtilGetHeadersOffset(String pdbPath) throws IOException, InterruptedException {
        String output = "";

        ProcessBuilder pb = new ProcessBuilder(
            "llvm-pdbutil",
            "dump",
            "-section-headers",
            pdbPath
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        if (p.waitFor() != 0)
            throw new IOException("Failed to read pdb sections (error code " + p.exitValue() + ", file \"" + pdbPath + "\")");
        
        output = new String(p.getInputStream().readAllBytes()).trim();

        if (output.isEmpty()) {
            throw new IOException("PDBUtil return empty output");
        }

        String[] sectionsRaw = output.split("SECTION HEADER #");
        SectionHeader[] ret = new SectionHeader[sectionsRaw.length - 1];
        for (int i = 1; i < sectionsRaw.length; i++) {
            String sectionRaw = sectionsRaw[i];

            int virtualSize = -1;
            int virtualAddress = -1;
            int rawSize = -1;
            int rawAddress = -1;

            String[] lines = sectionRaw.split("\n");
            for (String line : lines) {
                String[] parts = line.trim().split(" ", 2);
                if (parts.length != 2)
                    continue;
                
                switch (parts[1]) {
                    case "virtual size":             { virtualSize    = Integer.parseInt(parts[0], 16); break; }
                    case "virtual address":          { virtualAddress = Integer.parseInt(parts[0], 16); break; }
                    case "size of raw data":         { rawSize        = Integer.parseInt(parts[0], 16); break; }
                    case "file pointer to raw data": { rawAddress     = Integer.parseInt(parts[0], 16); break; }
                }
            }

            ret[i - 1] = new SectionHeader(virtualSize, virtualAddress, rawSize, rawAddress);
        }

        return ret;
    }

    private static long[] pdbUtilGetSymbolOffset(String file, String symbol) throws IOException, InterruptedException {
        String command = "llvm-pdbutil dump -publics " + file + " | grep " + symbol + " -A 1";
        String output = "";

        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        if (p.waitFor() != 0) {
            // System.out.println("Failed command: " + command);
            throw new IOException("Failed to find symbol (error code " + p.exitValue() + ")");
        }
        
        output = new String(p.getInputStream().readAllBytes()).trim();

        if (output.isEmpty()) {
            throw new IOException("Symbol not found");
        }

        String[] parts = output.split("addr = ");
        String[] addressParts = parts[1].split(":");
        long[] ret = new long[2];
        ret[0] = Long.parseLong(addressParts[0]);
        ret[1] = Long.parseLong(addressParts[1]);

        return ret;
    }

}
