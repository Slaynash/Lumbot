package slaynash.lum.bot.log;

public class LogSystem {

    public static void init() {
        System.setOut(new LumLogOutput(System.out, "OUT"));
        System.setErr(new LumLogOutput(System.err, "ERROR"));
    }

}
