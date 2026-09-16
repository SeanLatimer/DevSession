package dev.silentsean.mod.devsession.common.auth.microsoft.storage;

import org.apache.logging.log4j.Logger;

public final class TokenStorages {

    private TokenStorages() {
    }

    public static TokenStorage select(String mode, Logger logger, FileTokenStorage fallback) {
        switch (mode) {
            case "file":
                logger.info("Storing authentication credentials on disk (tokenStorage = file)");
                return fallback;
            case "keyring": {
                KeyringTokenStorage keyring = KeyringTokenStorage.create(logger);
                logger.info("Storing authentication credentials in " + keyring.describe());
                return keyring;
            }
            default: {
                try {
                    KeyringTokenStorage keyring = KeyringTokenStorage.create(logger);
                    logger.info("Storing authentication credentials in " + keyring.describe());
                    return keyring;
                } catch (Throwable t) {
                    logger.warn("WARNING: No supported system credential store is available. "
                        + "Authentication credentials will be stored on disk instead of in an OS-protected credential store.");
                    logger.warn("Reason: " + t.getMessage());
                    return fallback;
                }
            }
        }
    }

}
