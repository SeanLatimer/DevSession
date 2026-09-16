package dev.silentsean.mod.devsession.common.auth.microsoft.storage;

import dev.silentsean.mod.devsession.common.auth.microsoft.token.Token;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.TokenKey;

public interface TokenStorage {

    Token loadToken(String account, TokenKey<?> key);

    void storeToken(String account, TokenKey<?> key, Token token);

    void deleteToken(String account, TokenKey<?> key);

    boolean isNative();

    String describe();

}
