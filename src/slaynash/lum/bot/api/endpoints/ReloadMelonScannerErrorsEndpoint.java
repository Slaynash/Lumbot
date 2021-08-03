package slaynash.lum.bot.api.endpoints;

import slaynash.lum.bot.api.Endpoint;
import slaynash.lum.bot.api.WebRequest;
import slaynash.lum.bot.api.WebResponse;
import slaynash.lum.bot.discord.melonscanner.MelonLoaderError;

public class ReloadMelonScannerErrorsEndpoint extends Endpoint {

    @Override
    public WebResponse handle(WebRequest request) {
        System.out.println("Reloading Melon Scanner Errors");

        WebResponse r = new WebResponse();
        r.addHeader("Content-Type", "application/json");

        if (MelonLoaderError.reload()) {
            r.setData("{\"result\":\"OK\"}");
        }
        else {
            r.returnCode = 500;
            r.setData("{\"result\":\"Failed\"}");
        }

        return r;
    }

}
