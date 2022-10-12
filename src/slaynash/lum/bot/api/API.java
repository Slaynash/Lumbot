package slaynash.lum.bot.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import slaynash.lum.bot.Main;
import slaynash.lum.bot.api.endpoints.PingEndpoint;
import slaynash.lum.bot.api.endpoints.ReloadMelonScannerErrorsEndpoint;
import slaynash.lum.bot.api.endpoints.ReloadTranslationsEndpoint;
import slaynash.lum.bot.utils.ExceptionUtils;

public class API {
    public static final String LOG_IDENTIFIER = "API";

    public static final Map<String, Endpoint> endpoints = new HashMap<>();

    public static Gson gson;

    private static ServerSocket socket = null;
    private static int totalConnectionCount;


    public static void start() {

        gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();

        try {
            endpoints.put("/api/1/internal/reloadtranslations", new ReloadTranslationsEndpoint());
            endpoints.put("/api/1/internal/reloadmelonscannererrors", new ReloadMelonScannerErrorsEndpoint());
            endpoints.put("/api/1/ping", new PingEndpoint());

            while (socket == null) {
                try {
                    socket = new ServerSocket();
                }
                catch (Exception e) {
                    System.err.println("Error while creating socket: " + e.getMessage());
                    Thread.sleep(60 * 1000);
                }
            }
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(28644));
        }
        catch (Exception e) {
            ExceptionUtils.reportException("Failed to start up API", e);
            return;
        }

        Thread thread = new Thread(() -> {
            while (!Main.isShuttingDown) {
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
            try {
                System.out.print("Closing API socket...");
                socket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }, "APIThread");
        thread.setDaemon(false);
        thread.start();
    }

}
