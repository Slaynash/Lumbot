package slaynash.lum.bot.api;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import slaynash.lum.bot.api.endpoints.ReloadMelonScannerErrorsEndpoint;
import slaynash.lum.bot.api.endpoints.ReloadTranslationsEndpoint;
import slaynash.lum.bot.utils.ExceptionUtils;

public class API {
    public static final String LOG_IDENTIFIER = "API";

    public static final Map<String, Endpoint> endpoints = new HashMap<>();

    public static Gson gson;

    private static ServerSocket socket;
    private static int totalConnectionCount;


    public static void start() {

        gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();

        try {
            endpoints.put("/api/1/internal/reloadtranslations", new ReloadTranslationsEndpoint());
            endpoints.put("/api/1/internal/reloadmelonscannererrors", new ReloadMelonScannerErrorsEndpoint());

            socket = new ServerSocket(28644);
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to start up API", e);
        }

        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = socket.accept();
                    new APIClient(clientSocket, totalConnectionCount++);
                    // APIClient client = new APIClient(clientSocket, totalConnectionCount++);
                    // if (client.valid)
                    //     clients.add(client);
                }
                catch (IOException e) {
                    ExceptionUtils.reportException("Failed to handle API request", e);
                }
            }
        }, "APIThread");
        thread.setDaemon(true);
        thread.start();
    }

}
