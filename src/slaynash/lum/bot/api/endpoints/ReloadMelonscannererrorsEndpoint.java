package slaynash.lum.bot.api.endpoints;

import slaynash.lum.bot.api.Endpoint;
import slaynash.lum.bot.api.WebRequest;
import slaynash.lum.bot.api.WebResponse;
import slaynash.lum.bot.discord.melonscanner.MelonLoaderError;

public class ReloadMelonscannererrorsEndpoint extends Endpoint {

    @Override
    public WebResponse handle(WebRequest request) {
        System.out.println("Reloading Melon Scanner Errors");
        
        WebResponse r = new WebResponse();
		r.AddHeader("Content-Type", "application/json");

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
