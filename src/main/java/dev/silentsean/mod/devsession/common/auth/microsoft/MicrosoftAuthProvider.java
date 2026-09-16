package dev.silentsean.mod.devsession.common.auth.microsoft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.silentsean.mod.devsession.common.DevSession;
import dev.silentsean.mod.devsession.common.auth.IAuthProvider;
import dev.silentsean.mod.devsession.common.auth.SessionData;
import dev.silentsean.mod.devsession.common.auth.microsoft.oauth.CodeOAuthProvider;
import dev.silentsean.mod.devsession.common.auth.microsoft.oauth.DeviceCodeClients;
import dev.silentsean.mod.devsession.common.auth.microsoft.oauth.DeviceCodeOAuthProvider;
import dev.silentsean.mod.devsession.common.auth.microsoft.oauth.OAuthProvider;
import dev.silentsean.mod.devsession.common.auth.microsoft.storage.FileTokenStorage;
import dev.silentsean.mod.devsession.common.auth.microsoft.storage.TokenStorage;
import dev.silentsean.mod.devsession.common.auth.microsoft.storage.TokenStorages;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.OAuthToken;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.Token;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.TokenKey;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.XBLToken;
import dev.silentsean.mod.devsession.common.config.Account;
import dev.silentsean.mod.devsession.common.config.DevSessionConfig;
import dev.silentsean.mod.devsession.common.util.Util;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * References: <ul>
 * <li><a href="https://wiki.vg/Microsoft_Authentication_Scheme">wiki.vg</a></li>
 * <li><a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow">Microsoft docs</a></li>
 * <li><a href="https://datatracker.ietf.org/doc/html/rfc7636">RFC 7636: Proof Key for Code Exchange</a></li>
 * <li><a href="https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code">Microsoft device code flow</a></li>
 * </ul>
 *
 * @author DJtheRedstoner
 */
public class MicrosoftAuthProvider implements IAuthProvider {

    private static final String SCOPES = "XboxLive.signin XboxLive.offline_access";
    private static final String XBL_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MINECRAFT_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    private static final Logger logger = LogManager.getLogger("DevSession/Microsoft");

    private final OAuthProvider oAuthProvider;
    private final FileTokenStorage fileStorage;
    private final TokenStorage secretStorage;
    private final boolean forceTokenRefresh;
    private final int profileCacheMinutes;

    private final Map<TokenKey<?>, Supplier<? extends Token>> tokenRegistry = new LinkedHashMap<>();
    private final Map<TokenKey<?>, Token> tokenStore = new LinkedHashMap<>();

    private JsonObject profileCache;
    private boolean freshInteractiveAuth;

    public MicrosoftAuthProvider(DevSession DevSession) {
        DevSessionConfig config = DevSession.getConfig();
        this.oAuthProvider = createOAuthProvider(config);
        this.forceTokenRefresh = config.getForceTokenRefresh();
        this.profileCacheMinutes = config.getProfileCacheMinutes();
        this.fileStorage = new FileTokenStorage(config.getConfigDir());
        this.secretStorage = TokenStorages.select(config.getTokenStorage(), logger, this.fileStorage);
        initTokenRegistry();
    }

    private static OAuthProvider createOAuthProvider(DevSessionConfig config) {
        String clientIdOverride = config.getClientId();
        String grantFlow = config.getGrantFlow();

        switch (grantFlow) {
            case "browser":
                return new CodeOAuthProvider(logger, SCOPES, clientIdOverride != null ? clientIdOverride : Constants.CLIENT_ID);
            case "device-code": {
                DeviceCodeClients client = DeviceCodeClients.of(config.getDeviceCodeProvider());
                String clientId = clientIdOverride != null ? clientIdOverride : client.getClientId();
                logger.info("Using the " + client.getName() + " client for device code sign-in");
                return new DeviceCodeOAuthProvider(logger, SCOPES, clientId);
            }
            default:
                throw new RuntimeException("Unknown microsoft.grantFlow '" + grantFlow + "', valid options are: browser, device-code");
        }
    }

    protected void initTokenRegistry() {
        register(TokenKey.OAUTH_TOKEN, this::getOAuthToken);
        register(TokenKey.XBL_TOKEN, this::getXBLToken);
        register(TokenKey.XSTS_TOKEN, this::getXSTSToken);
        register(TokenKey.SESSION_TOKEN, this::getMcSession);
    }

    protected <T extends Token> void register(TokenKey<T> tokenKey, Supplier<T> supplier) {
        tokenRegistry.put(tokenKey, supplier);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Token> T get(TokenKey<T> tokenKey) {
        Supplier<T> supplier = (Supplier<T>) tokenRegistry.get(tokenKey);
        T token = (T) tokenStore.get(tokenKey);
        if (token == null || token.isExpired() || forceTokenRefresh) {
            logger.info("Fetching token " + tokenKey.getName());
            if (token instanceof OAuthToken) {
                try {
                    OAuthToken oAuthToken = (OAuthToken) token;
                    logger.info("Attempting to refresh OAuth token");
                    token = (T) oAuthProvider.refreshToken(oAuthToken);
                } catch (Exception e) {
                    logger.error("Failed to refresh OAuth token", e);
                    token = supplier.get();
                }
            } else {
                token = supplier.get();
            }

            OffsetDateTime expiry = OffsetDateTime.ofInstant(Instant.ofEpochSecond(token.getExpiry()), ZoneOffset.UTC);
            Instant now = Instant.now();
            Duration duration = Duration.between(now, expiry);
            String formattedExpiry = DateTimeFormatter.ISO_DATE_TIME.format(expiry);
            String formattedDuration = DurationFormatUtils.formatDuration(duration.toMillis(), "dd:HH:mm:ss");
            logger.info("Fetched token " + tokenKey.getName() + " (Expiry: " + formattedExpiry + " or in " + formattedDuration + ")");
            tokenStore.put(tokenKey, token);
        }
        return token;
    }

    protected OAuthToken getOAuthToken() {
        OAuthToken token = oAuthProvider.getOAuthToken();
        freshInteractiveAuth = true;
        return token;
    }

    protected XBLToken getXBLToken() {
        OAuthToken oAuthToken = get(TokenKey.OAUTH_TOKEN);

        JsonObject object = new JsonObject();

        JsonObject properties = new JsonObject();
        object.add("Properties", properties);
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + oAuthToken.getToken());

        //noinspection HttpUrlsUsage
        object.addProperty("RelyingParty", "http://auth.xboxlive.com");
        object.addProperty("TokenType", "JWT");

        JsonObject res = Util.client.jsonPost(XBL_URL, object);

        return XBLToken.fromJson(res, true);
    }

    protected XBLToken getXSTSToken() {
        XBLToken xblToken = get(TokenKey.XBL_TOKEN);

        JsonObject object = new JsonObject();

        JsonObject properties = new JsonObject();
        object.add("Properties", properties);
        properties.addProperty("SandboxId", "RETAIL");
        JsonArray userTokens = new JsonArray();
        userTokens.add(new JsonPrimitive(xblToken.getToken()));
        properties.add("UserTokens", userTokens);

        object.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        object.addProperty("TokenType", "JWT");

        JsonObject res = Util.client.jsonPost(XSTS_URL, object);

        return XBLToken.fromJson(res, true);
    }

    protected Token getMcSession() {
        XBLToken xstsToken = get(TokenKey.XSTS_TOKEN);

        JsonObject object = new JsonObject();
        object.addProperty("identityToken", "XBL3.0 x=" + xstsToken.getUserHash() + ";" + xstsToken.getToken());

        JsonObject res = Util.client.jsonPost(MINECRAFT_URL, object);

        return Token.fromJson(res);
    }

    @Override
    public SessionData login(Account account) {
        String name = account.getName();
        freshInteractiveAuth = false;

        readStoredTokens(name);
        try {
            SessionData data = getMinecraftProfile();
            persistTokens(name, true);
            return data;
        } catch (Exception e) {
            persistTokens(name, false);
            throw new RuntimeException("Failed to login", e);
        }
    }

    private void readStoredTokens(String account) {
        JsonObject entry = fileStorage.readAccountEntry(account);
        boolean loadedStoredOAuth = false;

        if (entry != null) {
            for (TokenKey<?> tokenKey : tokenRegistry.keySet()) {
                if (tokenKey == TokenKey.OAUTH_TOKEN) continue;
                if (entry.has(tokenKey.getName())) {
                    Token token = Util.gson.fromJson(entry.get(tokenKey.getName()), tokenKey.getClazz());
                    tokenStore.put(tokenKey, token);
                }
            }

            if (entry.has("profileCache")) {
                profileCache = entry.getAsJsonObject("profileCache");
            }

            if (secretStorage.isNative() && entry.has("oauth")) {
                OAuthToken migrated = migrateOAuthToken(account, entry);
                if (migrated != null) {
                    tokenStore.put(TokenKey.OAUTH_TOKEN, migrated);
                    loadedStoredOAuth = true;
                }
            }
        }

        if (!loadedStoredOAuth && !forceTokenRefresh) {
            try {
                OAuthToken stored = secretStorage.loadOAuthToken(account);
                if (stored != null) {
                    tokenStore.put(TokenKey.OAUTH_TOKEN, stored);
                    loadedStoredOAuth = true;
                }
            } catch (Exception e) {
                logger.error("Failed to read the OAuth token from " + secretStorage.describe() + ", a new login will be required", e);
            }
        }

        if (forceTokenRefresh && loadedStoredOAuth) {
            logger.info("forceTokenRefresh is enabled, treating all stored tokens as expired");
        }
    }

    private OAuthToken migrateOAuthToken(String account, JsonObject entry) {
        try {
            OAuthToken legacy = Util.gson.fromJson(entry.get("oauth"), OAuthToken.class);
            secretStorage.storeOAuthToken(account, legacy);
            entry.remove("oauth");
            fileStorage.writeAccountEntry(account, entry);
            logger.info("Migrated the OAuth token for account '" + account + "' from microsoft_accounts.json into " + secretStorage.describe());
            return legacy;
        } catch (Exception e) {
            logger.error("Failed to migrate the OAuth token for account '" + account + "' into " + secretStorage.describe(), e);
            return null;
        }
    }

    private void persistTokens(String account, boolean success) {
        if (!success) {
            fileStorage.writeAccountEntry(account, null);
            return;
        }

        boolean oauthOnDisk = false;
        if (secretStorage.isNative()) {
            OAuthToken oAuthToken = (OAuthToken) tokenStore.get(TokenKey.OAUTH_TOKEN);
            try {
                secretStorage.storeOAuthToken(account, oAuthToken);
            } catch (Exception e) {
                logger.error("Failed to store the OAuth token in " + secretStorage.describe()
                    + ", it will be stored on disk for this session", e);
                oauthOnDisk = true;
            }
        }

        JsonObject entry = new JsonObject();
        for (Map.Entry<TokenKey<?>, Token> entryToken : tokenStore.entrySet()) {
            if (entryToken.getKey() == TokenKey.OAUTH_TOKEN && secretStorage.isNative() && !oauthOnDisk) continue;
            entry.add(entryToken.getKey().getName(), Util.gson.toJsonTree(entryToken.getValue()));
        }
        if (profileCache != null) {
            entry.add("profileCache", profileCache);
        }
        fileStorage.writeAccountEntry(account, entry);
    }

    protected SessionData getMinecraftProfile() {
        Token mcSession = get(TokenKey.SESSION_TOKEN);

        if (!freshInteractiveAuth && hasValidProfileCache()) {
            logger.info("Using cached profile information");
            return new SessionData(
                mcSession.getToken(),
                profileCache.get("uuid").getAsString(),
                profileCache.get("name").getAsString(),
                "msa",
                "{}"
            );
        }

        try {
            JsonObject profileObject = Util.client.authorizedJsonGet(
                MINECRAFT_PROFILE_URL,
                "Bearer " + mcSession.getToken()
            );

            String uuid = profileObject.get("id").getAsString();
            String name = profileObject.get("name").getAsString();

            profileCache = new JsonObject();
            profileCache.addProperty("uuid", uuid);
            profileCache.addProperty("name", name);
            profileCache.addProperty("cachedAt", Util.secondsSinceEpoch());

            return new SessionData(mcSession.getToken(), uuid, name, "msa", "{}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch minecraft profile, does the user own the game?", e);
        }
    }

    private boolean hasValidProfileCache() {
        if (profileCache == null || !profileCache.has("uuid") || !profileCache.has("name")) return false;
        long cachedAt = profileCache.has("cachedAt") ? profileCache.get("cachedAt").getAsLong() : 0;
        return Util.secondsSinceEpoch() - cachedAt < profileCacheMinutes * 60L;
    }

}
