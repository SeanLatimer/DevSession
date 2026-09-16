package dev.silentsean.mod.devsession.common.util.request;

import com.google.gson.JsonObject;
import dev.silentsean.mod.devsession.util.request.jdk.JDKClient;

import java.util.Map;
import java.util.Set;

public interface Client {

    static Client getInstance() {
        return new JDKClient();
    }

    JsonObject jsonPost(String url, JsonObject body);

    default JsonObject urlEncodedJsonPost(String url, Map<String, String> body) {
        return urlEncodedJsonPost(url, body, Set.of(200));
    }

    JsonObject urlEncodedJsonPost(String url, Map<String, String> body, Set<Integer> acceptableStatuses);

    JsonObject authorizedJsonGet(String url, String authorization);

}
