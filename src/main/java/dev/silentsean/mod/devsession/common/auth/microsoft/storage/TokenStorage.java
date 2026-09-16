package dev.silentsean.mod.devsession.common.auth.microsoft.storage;

import dev.silentsean.mod.devsession.common.auth.microsoft.token.OAuthToken;

public interface TokenStorage {

    OAuthToken loadOAuthToken(String account);

    void storeOAuthToken(String account, OAuthToken token);

    void deleteOAuthToken(String account);

    boolean isNative();

    String describe();

}
