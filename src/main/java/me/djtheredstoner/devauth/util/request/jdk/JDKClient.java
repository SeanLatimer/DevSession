package me.djtheredstoner.devauth.util.request.jdk;

import com.google.gson.JsonObject;
import me.djtheredstoner.devauth.common.auth.microsoft.Constants;
import me.djtheredstoner.devauth.common.util.Util;
import me.djtheredstoner.devauth.common.util.request.Client;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JDKClient implements Client {

    private static final Logger logger = LogManager.getLogger("DevAuth/HTTP/JDK");

    public JDKClient() {
        logger.info("Using JDK client");
    }

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public JsonObject jsonPost(String url, JsonObject body) {
        try {
            String bodyString = Util.gson.toJson(body);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(bodyString))
                .header("User-Agent", Constants.USER_AGENT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();

            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(res);

            return Util.parser.parse(res.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Error POSTing url: " + url, e);
        }
    }

    @Override
    public JsonObject urlEncodedJsonPost(String url, Map<String, String> body) {
        try {
            StringBuilder bodyString = new StringBuilder();
            for (Map.Entry<String, String> entry : body.entrySet()) {
                bodyString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                bodyString.append('=');
                bodyString.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                bodyString.append('&');
            }
            bodyString.deleteCharAt(bodyString.length() - 1);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(bodyString.toString()))
                .header("User-Agent", Constants.USER_AGENT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .build();

            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(res);

            return Util.parser.parse(res.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Error POSTing url: " + url, e);
        }
    }

    @Override
    public JsonObject authorizedJsonGet(String url, String authorization) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", Constants.USER_AGENT)
                .header("Authorization", authorization)
                .build();

            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(res);

            return Util.parser.parse(res.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Error GETing url: " + url, e);
         }
    }

    private void checkStatus(HttpResponse<String> res) {
        if (res.statusCode() != 200) {
            throw new RuntimeException(
                "Received bad status " + res.statusCode() + " body: " + res.body()
            );
        }
    }
}
