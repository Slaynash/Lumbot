package slaynash.lum.bot.api;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import slaynash.lum.bot.api.endpoints.ReloadMelonscannererrorsEndpoint;
import slaynash.lum.bot.api.endpoints.ReloadTranslationsEndpoint;
import slaynash.lum.bot.utils.ExceptionUtils;

public class API {

    public static Map<String, Endpoint> endpoints = new HashMap<>();

    public static Gson gson;

    private static ServerSocket socket;
    private static int totalConnectionCount;


    public static void start() throws IOException {

        gson = new GsonBuilder()
            //.serializeNulls()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            /*
            .registerTypeAdapter(WorldInstanceInfos.class, new JsonSerializer<WorldInstanceInfos>() {
                @Override
                public JsonElement serialize(WorldInstanceInfos src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonArray serialized = new JsonArray();

                    serialized.add(src.id);
                    serialized.add(src.users);

                    return serialized;
                }
            })
            */
            .create();

        endpoints.put("/api/1/internal/reloadtranslations", new ReloadTranslationsEndpoint());
        endpoints.put("/api/1/internal/reloadmelonscannererrors", new ReloadMelonscannererrorsEndpoint());

        socket = new ServerSocket(28644);

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
