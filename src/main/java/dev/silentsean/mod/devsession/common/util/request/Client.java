package dev.silentsean.mod.devsession.common.util.request;

import com.google.gson.JsonObject;
import dev.silentsean.mod.devsession.util.request.jdk.JDKClient;

import java.util.Map;

public interface Client {

    static Client getInstance() {
        return new JDKClient();
    }

    JsonObject jsonPost(String url, JsonObject body);

    JsonObject urlEncodedJsonPost(String url, Map<String, String> body);

    JsonObject authorizedJsonGet(String url, String authorization);

}
