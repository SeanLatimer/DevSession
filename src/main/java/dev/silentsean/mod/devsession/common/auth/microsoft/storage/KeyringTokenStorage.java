package dev.silentsean.mod.devsession.common.auth.microsoft.storage;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.KeyringStorageType;
import com.github.javakeyring.PasswordAccessException;
import com.google.gson.JsonObject;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.OAuthToken;
import dev.silentsean.mod.devsession.common.util.Util;
import org.apache.logging.log4j.Logger;

public class KeyringTokenStorage implements TokenStorage {

    public static final String DOMAIN = "devsession";
    private static final String PROBE_ACCOUNT = "__probe__";
    private static final int PAYLOAD_VERSION = 1;

    private final Keyring keyring;
    private final Logger logger;

    private KeyringTokenStorage(Keyring keyring, Logger logger) {
        this.keyring = keyring;
        this.logger = logger;
    }

    public static KeyringTokenStorage create(Logger logger) {
        Keyring keyring;
        try {
            keyring = Keyring.create();
        } catch (Throwable t) {
            throw new IllegalStateException("No keyring backend is available", t);
        }

        try {
            keyring.getPassword(DOMAIN, PROBE_ACCOUNT);
            logger.warn("Found a leftover probe credential in " + safeStorageType(keyring) + ", ignoring");
        } catch (PasswordAccessException expected) {
        } catch (Throwable t) {
            closeQuietly(keyring);
            throw new IllegalStateException(safeStorageType(keyring) + " is not usable", t);
        }

        return new KeyringTokenStorage(keyring, logger);
    }

    @Override
    public OAuthToken loadOAuthToken(String account) {
        String payload;
        try {
            payload = keyring.getPassword(DOMAIN, account);
        } catch (PasswordAccessException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read the credential for account '" + account + "' from " + describe(), e);
        }

        try {
            JsonObject parsed = Util.parser.parse(payload).getAsJsonObject();
            if (!parsed.has("version") || parsed.get("version").getAsInt() != PAYLOAD_VERSION
                || !parsed.has("refreshToken")) {
                logger.warn("Credential for account '" + account + "' in " + describe() + " has an unknown format. Ignoring.");
                return null;
            }
            return new OAuthToken("", parsed.get("refreshToken").getAsString(), 0);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse the credential for account '" + account + "' from " + describe(), e);
        }
    }

    @Override
    public void storeOAuthToken(String account, OAuthToken token) {
        String refreshToken = token.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalStateException("The OAuth token for account '" + account + "' has no refresh token to store");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("version", PAYLOAD_VERSION);
        payload.addProperty("refreshToken", refreshToken);

        try {
            keyring.setPassword(DOMAIN, account, Util.gson.toJson(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store the credential for account '" + account + "' in " + describe(), e);
        }
    }

    @Override
    public void deleteOAuthToken(String account) {
        try {
            keyring.deletePassword(DOMAIN, account);
        } catch (PasswordAccessException ignored) {
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete the credential for account '" + account + "' from " + describe(), e);
        }
    }

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    public String describe() {
        try {
            return describeType(keyring.getKeyringStorageType());
        } catch (Exception e) {
            return "the operating system credential store";
        }
    }

    private static String describeType(KeyringStorageType type) {
        switch (type) {
            case OSX_KEYCHAIN:
            case LEGACY_OSX_KEYCHAIN:
                return "the macOS Keychain";
            case GNOME_KEYRING:
                return "the GNOME Keyring";
            case KWALLET:
                return "KWallet";
            case WINDOWS_CREDENTIAL_STORE:
                return "Windows Credential Manager";
            default:
                return "the operating system credential store";
        }
    }

    private static String safeStorageType(Keyring keyring) {
        try {
            return describeType(keyring.getKeyringStorageType());
        } catch (Exception e) {
            return "operating system credential store";
        }
    }

    private static void closeQuietly(Keyring keyring) {
        try {
            keyring.close();
        } catch (Exception ignored) {
        }
    }

}
