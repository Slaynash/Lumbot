package slaynash.lum.bot.api;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebResponse {

    public int returnCode;
    public String returnMessage;
    public Map<String, List<String>> headers = new HashMap<>();
    public byte[] data = new byte[0];

    public WebResponse() {
        returnCode = 200;
        returnMessage = "OK";
        //headers.put("Server", "Slaynash's Server");
        //headers.put("Content-Type", "text/html; charset=UTF-8");
        //headers.put("Date", ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
    }

    public void setData(String data) {
        System.out.println("SetData: " + data);
        setData(data.getBytes(StandardCharsets.UTF_8));
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void addHeader(String key, String value) {
        List<String> header = headers.get(key);
        if (header == null) {
            header = new ArrayList<String>();
            headers.put(key, header);
        }
        header.add(value);
    }

    public static WebResponse getBadRequestResponse() {
        WebResponse r = new WebResponse();
        r.returnCode = 400;
        r.setData("<html>\r\n" +
                "<head><title>400 Bad Request</title></head>\r\n" +
                "<body bgcolor=\"white\">\r\n" +
                "<center><h1>400 Bad Request</h1></center>\r\n" +
                "<hr><center>Slaynash's Server</center>\r\n" +
                "</body>\r\n" +
                "</html>");
        return r;
    }

    public static WebResponse getMissingCredentialsResponse() {
        WebResponse r = new WebResponse();
        r.returnCode = 401;
        r.addHeader("Content-Type", "application/json");
        r.setData("{\"error\":{\"message\":\"Missing Credentials\",\"status_code\":401}}");
        return r;
    }

    public static WebResponse getInvalidCredentialsResponse() {
        WebResponse r = new WebResponse();
        r.returnCode = 401;
        r.addHeader("Content-Type", "application/json");
        r.setData("{\"error\":{\"message\":\"Invalid Credentials\",\"status_code\":401}}");
        return r;
    }

    public static WebResponse getUnauthorizedResponse() {
        WebResponse r = new WebResponse();
        r.returnCode = 403;
        r.addHeader("Content-Type", "application/json");
        r.setData("{\"error\":\"Unauthorized\",\"status_code\":403}");
        return r;
    }

    public static WebResponse getNotFoundResponse() {
        WebResponse r = new WebResponse();
        r.returnCode = 404;
        r.addHeader("Content-Type", "application/json");
        r.setData("{\"error\":\"Endpoint Not Found\",\"status_code\":404}");
        return r;
    }

    public static WebResponse getInternalErrorResponse() {
        WebResponse r = new WebResponse();
        r.returnCode = 500;
        r.addHeader("Content-Type", "application/json");
        r.setData("{\"error\":\"Internal Server Error\",\"status_code\":500}");
        return r;
    }

    public static WebResponse getNotImplementedResponse() {
        WebResponse r = new WebResponse();
        r.returnCode = 501;
        r.addHeader("Content-Type", "application/json");
        r.setData("{\"error\":\"Method Not Implemented\",\"status_code\":501}");
        return r;
    }

    public static WebResponse getBadGatewayResponse() {
        WebResponse r = new WebResponse();
        r.returnCode = 502;
        r.addHeader("Content-Type", "application/json");
        r.setData("{\"error\":\"Bad Gateway\",\"status_code\":502}");
        return r;
    }

    public static WebResponse getUnsupportedResponse() {
        WebResponse r = new WebResponse();
        r.returnCode = 505;
        r.addHeader("Content-Type", "application/json");
        r.setData("{\"error\":\"HTTP Version Not Supported\",\"status_code\":505}");
        return r;
    }
}
