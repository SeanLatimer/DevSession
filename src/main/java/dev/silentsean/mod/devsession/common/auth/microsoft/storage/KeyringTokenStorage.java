package dev.silentsean.mod.devsession.common.auth.microsoft.storage;

import com.google.gson.JsonObject;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.OAuthToken;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.Token;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.TokenKey;
import dev.silentsean.mod.devsession.common.util.Util;
import org.apache.logging.log4j.Logger;

public class KeyringTokenStorage implements TokenStorage {

    public static final String DOMAIN = "devsession";
    private static final int PAYLOAD_VERSION = 1;

    private final NativeCreds creds;
    private final Logger logger;

    private KeyringTokenStorage(NativeCreds creds, Logger logger) {
        this.creds = creds;
        this.logger = logger;
    }

    public static KeyringTokenStorage create(Logger logger) {
        return new KeyringTokenStorage(NativeCreds.open(logger), logger);
    }

    @Override
    public Token loadToken(String account, TokenKey<?> key) {
        String payload = creds.read(entryName(account, key));
        if (payload == null) return null;

        try {
            JsonObject parsed = Util.parser.parse(payload).getAsJsonObject();
            if (!parsed.has("version") || parsed.get("version").getAsInt() != PAYLOAD_VERSION) {
                logger.warn("Cached " + key.getName() + " token for account '" + account + "' in " + describe()
                    + " has an unknown version. Ignoring.");
                return null;
            }

            if (key == TokenKey.OAUTH_TOKEN) {
                if (!parsed.has("refreshToken")) {
                    logger.warn("Cached oauth token for account '" + account + "' in " + describe()
                        + " has no refresh token. Ignoring.");
                    return null;
                }
                return new OAuthToken("", parsed.get("refreshToken").getAsString(), 0);
            }

            return Util.gson.fromJson(parsed.getAsJsonObject("token"), key.getClazz());
        } catch (Exception e) {
            logger.warn("Failed to parse the cached " + key.getName() + " token for account '" + account + "' in "
                + describe() + ", ignoring", e);
            return null;
        }
    }

    @Override
    public void storeToken(String account, TokenKey<?> key, Token token) {
        JsonObject payload = new JsonObject();
        payload.addProperty("version", PAYLOAD_VERSION);

        if (key == TokenKey.OAUTH_TOKEN) {
            String refreshToken = ((OAuthToken) token).getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new IllegalStateException("The oauth token for account '" + account + "' has no refresh token to store");
            }
            payload.addProperty("refreshToken", refreshToken);
        } else {
            payload.add("token", Util.gson.toJsonTree(token));
        }

        creds.write(entryName(account, key), Util.compactGson.toJson(payload));
    }

    @Override
    public void deleteToken(String account, TokenKey<?> key) {
        creds.delete(entryName(account, key));
    }

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    public String describe() {
        return creds.describe();
    }

    private static String entryName(String account, TokenKey<?> key) {
        return account + ":" + key.getName();
    }

}
