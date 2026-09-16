package dev.silentsean.mod.devsession.common.auth.microsoft.oauth;

import com.google.gson.JsonObject;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.OAuthToken;
import dev.silentsean.mod.devsession.common.util.Util;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;

public class DeviceCodeOAuthProvider extends OAuthProvider {

    private static final String DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String DEVICE_CODE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";

    private final String clientId;
    private long pollIntervalMillis = 5000;

    public DeviceCodeOAuthProvider(Logger logger, String scopes, String clientId) {
        super(logger, scopes);
        this.clientId = clientId;
    }

    @Override
    public OAuthToken getOAuthToken() {
        JsonObject deviceCode = Util.client.urlEncodedJsonPost(DEVICE_CODE_URL, Util.stringMap(
            "client_id", clientId,
            "scope", scopes
        ));

        String userCode = deviceCode.get("user_code").getAsString();
        String verificationUri = deviceCode.get("verification_uri").getAsString();
        if (deviceCode.has("message")) {
            logger.info(deviceCode.get("message").getAsString());
        }
        logger.info("Open " + verificationUri + " in a browser and enter the code: " + userCode);

        if (deviceCode.has("interval")) {
            pollIntervalMillis = deviceCode.get("interval").getAsInt() * 1000L;
        }
        long expiresInSeconds = deviceCode.has("expires_in") ? deviceCode.get("expires_in").getAsInt() : 900;
        long deadline = Util.secondsSinceEpoch() + expiresInSeconds;
        String code = deviceCode.get("device_code").getAsString();

        while (Util.secondsSinceEpoch() < deadline) {
            sleep(pollIntervalMillis);

            OAuthToken token = pollForToken(code);
            if (token != null) {
                logger.info("Device code validated!");
                return token;
            }
        }

        throw new RuntimeException("The device code expired before sign-in was completed, please try again");
    }

    private OAuthToken pollForToken(String code) {
        JsonObject res = Util.client.urlEncodedJsonPost(TOKEN_URL, Util.stringMap(
            "client_id", clientId,
            "device_code", code,
            "grant_type", DEVICE_CODE_GRANT_TYPE
        ), Set.of(200, 400));

        if (res.has("error")) {
            String error = res.get("error").getAsString();
            if (error.equals("authorization_pending")) return null;
            if (error.equals("slow_down")) {
                pollIntervalMillis += 5000;
                return null;
            }
            throw new RuntimeException("Device code sign-in failed: " + error
                + (res.has("error_description") ? " (" + res.get("error_description").getAsString() + ")" : ""));
        }

        return OAuthToken.fromJson(res);
    }

    @Override
    public OAuthToken refreshToken(OAuthToken token) {
        try {
            return getAuthorizationToken(Util.stringMap(
                "grant_type", "refresh_token",
                "refresh_token", token.getRefreshToken()
            ));
        } catch (Exception e) {
            logger.error("Error refreshing OAuth token, trying to get new token!", e);
            return getOAuthToken();
        }
    }

    private OAuthToken getAuthorizationToken(Map<String, String> extraParams) {
        Map<String, String> params = Util.stringMap(
            "client_id", clientId,
            "scope", scopes
        );
        params.putAll(extraParams);

        JsonObject res = Util.client.urlEncodedJsonPost(TOKEN_URL, params);

        return OAuthToken.fromJson(res);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for the device code sign-in", e);
        }
    }

}
