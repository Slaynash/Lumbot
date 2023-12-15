package slaynash.lum.bot.api.endpoints;

import net.dv8tion.jda.api.JDA.Status;
import slaynash.lum.bot.api.Endpoint;
import slaynash.lum.bot.api.WebRequest;
import slaynash.lum.bot.api.WebResponse;
import slaynash.lum.bot.discord.JDAManager;

public class PingEndpoint extends Endpoint {

    @Override
    public WebResponse handle(WebRequest request) {
        WebResponse r = new WebResponse();
        r.addHeader("Content-Type", "text/plain");
        if (JDAManager.getJDA() == null) {
            r.returnCode = 569;
            r.setData("Lum is Booting");
        }
        if (JDAManager.getJDA().getStatus() == Status.CONNECTED) {
            r.setData("pong " + JDAManager.getJDA().getGatewayPing());
        }
        else {
            r.returnCode = 503;
            r.setData("Lum is not running " + JDAManager.getJDA().getStatus());
        }
        return r;
    }
}
