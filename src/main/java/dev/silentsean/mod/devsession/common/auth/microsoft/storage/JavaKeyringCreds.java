package dev.silentsean.mod.devsession.common.auth.microsoft.storage;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.KeyringStorageType;
import com.github.javakeyring.PasswordAccessException;
import org.apache.logging.log4j.Logger;

class JavaKeyringCreds implements NativeCreds {

    private static final String PROBE_KEY = "__probe__";

    private final Keyring keyring;

    private JavaKeyringCreds(Keyring keyring) {
        this.keyring = keyring;
    }

    static JavaKeyringCreds open(Logger logger) {
        Keyring keyring;
        try {
            keyring = Keyring.create();
        } catch (Throwable t) {
            throw new IllegalStateException("No keyring backend is available", t);
        }

        try {
            keyring.getPassword(KeyringTokenStorage.DOMAIN, PROBE_KEY);
            logger.warn("Found a leftover probe credential in " + describe(keyring) + ", ignoring");
        } catch (PasswordAccessException expected) {
        } catch (Throwable t) {
            closeQuietly(keyring);
            throw new IllegalStateException(describe(keyring) + " is not usable", t);
        }

        return new JavaKeyringCreds(keyring);
    }

    @Override
    public String read(String key) {
        try {
            return keyring.getPassword(KeyringTokenStorage.DOMAIN, key);
        } catch (PasswordAccessException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read '" + key + "' from " + describe(), e);
        }
    }

    @Override
    public void write(String key, String value) {
        try {
            keyring.setPassword(KeyringTokenStorage.DOMAIN, key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write '" + key + "' to " + describe(), e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            keyring.deletePassword(KeyringTokenStorage.DOMAIN, key);
        } catch (PasswordAccessException ignored) {
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete '" + key + "' from " + describe(), e);
        }
    }

    @Override
    public String describe() {
        return describe(keyring);
    }

    private static String describe(Keyring keyring) {
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

    private static void closeQuietly(Keyring keyring) {
        try {
            keyring.close();
        } catch (Exception ignored) {
        }
    }

}
