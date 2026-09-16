package me.djtheredstoner.devauth.common.util.request;

import com.google.gson.JsonObject;
import me.djtheredstoner.devauth.util.request.jdk.JDKClient;

import java.util.Map;

public interface Client {

    static Client getInstance() {
        return new JDKClient();
    }

    JsonObject jsonPost(String url, JsonObject body);

    JsonObject urlEncodedJsonPost(String url, Map<String, String> body);

    JsonObject authorizedJsonGet(String url, String authorization);

}
