package slaynash.lum.bot.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class APIClient {
    public static final String LOG_IDENTIFIER = "APIClient";

    private final int id;
    public boolean valid = false;

    private final Socket socket;
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;

    public APIClient(Socket socket, int id) {
        this.id = id;
        this.socket = socket;

        try {
            inputStream = new BufferedInputStream(socket.getInputStream());
            outputStream = new BufferedOutputStream(socket.getOutputStream());

            System.out.println("[Connection " + id + " (" + socket.getPort() + ")] Connection started");

            Thread listenThread = new Thread(() -> {
                while (valid)
                    valid = listenForRequests();
                System.out.println("[Connection " + id + " (" + socket.getPort() + ")] Connection closed");
                try {
                    socket.close();
                }
                catch (Exception e2) {
                    System.err.println("[Connection " + id + " (" + socket.getPort() + ")] Error while closing socket: " + e2.getMessage());
                }
            });
            listenThread.setDaemon(true);
            listenThread.start();
        }
        catch (Exception e) {
            System.err.println("[Connection " + id + " (" + socket.getPort() + ")] Error while initializing user");
            e.printStackTrace();
            try {
                socket.close();
            }
            catch (Exception e2) {
                System.err.println("[Connection " + id + " (" + socket.getPort() + ")] Error while closing socket: " + e2.getMessage());
            }
            return;
        }

        valid = true;

    }

    private boolean listenForRequests() {
        try {
            String ln = readInLine();
            if (ln == null)
                return false;
            else {
                try {
                    System.out.println("[Connection " + id + " (" + socket.getPort() + ")] [Request] " + ln);

                    String[] parts = ln.split(" ", 3);
                    if (parts.length < 3 || !parts[2].startsWith("HTTP/")) {
                        System.out.println("[Connection " + id + " (" + socket.getPort() + ")] 400 Bad Request");
                        sendResponse(WebResponse.getBadRequestResponse(), true);
                        return false;
                    }
                    if (!parts[2].equals("HTTP/1.1")) {
                        System.err.println("[Connection " + id + " (" + socket.getPort() + ")] 505 HTTP Version Not Supported");
                        sendResponse(WebResponse.getUnsupportedResponse(), true);
                        return false;
                    }

                    Map<String, String> requestParameters = new HashMap<>();

                    String[] requestUrl = parts[1].split("\\?", 2);
                    if (requestUrl.length > 1) {
                        String[] requestParametersRaw = requestUrl[1].split("&");
                        for (String rpr : requestParametersRaw) {
                            String[] paramParts = rpr.split("=", 2);
                            requestParameters.put(URLDecoder.decode(paramParts[0], StandardCharsets.UTF_8), paramParts.length > 1 ? URLDecoder.decode(paramParts[1], StandardCharsets.UTF_8) : "");
                        }
                    }

                    requestUrl[0] = requestUrl[0].replaceFirst("/*$", "");

                    Map<String, String> headers = new HashMap<>();

                    while (!"".equals(ln = readInLine())) {
                        if (ln == null) return false;
                        String[] headerParts = ln.split(" ", 2);
                        headers.put(headerParts[0].substring(0, headerParts[0].length() - 1).toLowerCase(), headerParts[1]);
                    }

                    WebRequest request = new WebRequest(requestUrl[0] + (requestUrl.length > 1 ? ("?" + requestUrl[1]) : ""), parts[0], requestParameters, headers, socket.getInetAddress().getHostAddress());

                    if (request.method != RequestMethod.NONE) {
                        if (request.headers.containsKey("content-length")) {
                            int contentLength = Integer.parseInt(request.headers.get("content-length"));
                            byte[] content = new byte[contentLength];
                            int bytesRead = 0;
                            do {
                                bytesRead = inputStream.read(content, bytesRead, contentLength - bytesRead);
                            }
                            while (bytesRead < contentLength);
                            request.content = content;
                            //System.out.println(new String(content, StandardCharsets.UTF_8));
                        }
                        if (requestUrl[0].length() > 0) {
                            for (Entry<String, Endpoint> entry : API.endpoints.entrySet()) {
                                if (requestUrl[0].matches(entry.getKey())) {
                                    WebResponse response = entry.getValue().handle(request);
                                    System.out.println("[Connection " + id + " (" + socket.getPort() + ")] Response: " + new String(response.data, StandardCharsets.UTF_8));
                                    sendResponse(response, request.method != RequestMethod.HEAD);
                                    return true;
                                }
                            }
                        }
                        System.err.println("[Connection " + id + " (" + socket.getPort() + ")] " + request.path + " 404 Not found");
                        sendResponse(WebResponse.getNotFoundResponse(), request.method != RequestMethod.HEAD);
                    }
                    else {
                        System.err.println("[Connection " + id + " (" + socket.getPort() + ")] 501 Not Implemented");
                        sendResponse(WebResponse.getNotImplementedResponse(), true);
                        return false;
                    }
                }
                catch (Exception e) {
                    System.err.println("[Connection " + id + " (" + socket.getPort() + ")] 500 Internal Server Error");
                    e.printStackTrace();
                    sendResponse(WebResponse.getInternalErrorResponse(), true);
                    return false;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String readInLine() throws IOException {
        char c = (char) -1;
        StringBuilder s = new StringBuilder();
        do {
            c = (char) inputStream.read();
            //System.out.println("READ " + c + " (" + ((int)c) + ")");
            if (c == -1 || c == 65535)
                return null;
            if (c == '\r')
                continue;
            if (c == '\n')
                break;
            s.append(c);
        }
        while (c != -1);
        //System.out.println(s);
        return s.toString();
    }

    private void sendResponse(WebResponse response, boolean sendBody) throws IOException {
        outputStream.write(("HTTP/1.1 " + response.returnCode + " " + response.returnMessage + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (Entry<String, List<String>> entry : response.headers.entrySet()) {
            for (String value : entry.getValue())
                outputStream.write((entry.getKey() + ": " + value + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        outputStream.write(("Content-Length: " + response.data.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(response.data);
        outputStream.flush();
    }
}
