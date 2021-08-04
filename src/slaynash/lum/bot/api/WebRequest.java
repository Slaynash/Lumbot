package slaynash.lum.bot.api;

import java.util.Map;

public class WebRequest {

    public final String path;
    public RequestMethod method = RequestMethod.NONE;
    public final Map<String, String> parameters;
    public final Map<String, String> headers;
    public byte[] content = new byte[0];
    public final String clientIpAddress;

    public WebRequest(String path, String method, Map<String, String> parameters, Map<String, String> headers, String clientIpAddress) {
        if (method.equals("GET"))
            this.method = RequestMethod.GET;
        if (method.equals("HEAD"))
            this.method = RequestMethod.HEAD;
        if (method.equals("POST"))
            this.method = RequestMethod.POST;
        if (method.equals("PUT"))
            this.method = RequestMethod.PUT;

        this.path = path;
        this.parameters = parameters;
        this.headers = headers;
        this.clientIpAddress = clientIpAddress;
    }

}
