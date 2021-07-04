package slaynash.lum.bot.api;

import java.util.Map;

public class WebRequest {

    public String path;
    public RequestMethod method = RequestMethod.NONE;
    public Map<String, String> parameters;
    public Map<String, String> headers;
    public byte[] content = new byte[0];
    public String clientIpAddress;

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
