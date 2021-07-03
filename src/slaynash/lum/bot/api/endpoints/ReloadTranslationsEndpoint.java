package slaynash.lum.bot.api.endpoints;

import slaynash.lum.bot.Localization;
import slaynash.lum.bot.api.Endpoint;
import slaynash.lum.bot.api.WebRequest;
import slaynash.lum.bot.api.WebResponse;

public class ReloadTranslationsEndpoint extends Endpoint {

    @Override
    public WebResponse handle(WebRequest request) {
        System.out.println("Reloading translations");
        
        WebResponse r = new WebResponse();
		r.AddHeader("Content-Type", "application/json");

        if (Localization.reload()) {
            r.setData("{\"result\":\"OK\"}");
        }
        else {
            r.returnCode = 500;
		    r.setData("{\"result\":\"Failed\"}");
        }
        
		return r;
    }
    


}
