package my.SpringProject.SyncMenu;

import com.google.gson.Gson;
import com.intellij.openapi.components.Service;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public final class Connecter {

    private static final String SERVER_URL = "http://localhost:8080/sync";
    private static final String SERVER_WEBSOCKET_URL = "http://localhost:8080/sync/ws";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public void updatePathData(String key, String value) throws IOException{
        UpdateRequestJson requestJson = new UpdateRequestJson(key,value);
        String json = gson.toJson(requestJson);

        RequestBody requestBody = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(SERVER_URL + "/verifyPath")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
        }
    }
    public ConcurrentHashMap<String, String> getPaths() throws IOException {
        Request request = new Request.Builder()
                .url(SERVER_URL + "/paths")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            return new Gson().fromJson(response.body().string(), ConcurrentHashMap.class);
        }
    }


    public void updateData(String key, String value) throws IOException {
       UpdateRequestJson requestJson = new UpdateRequestJson(key,value);
       String json = gson.toJson(requestJson);

       RequestBody requestBody = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(SERVER_URL + "/update")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
        }

    }


    public ConcurrentHashMap<String, String> getData() throws IOException {
        Request request = new Request.Builder()
                .url(SERVER_URL + "/data")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            return new Gson().fromJson(response.body().string(), ConcurrentHashMap.class);
        }
    }
}