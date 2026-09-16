package dev.silentsean.mod.devsession.common.auth;

import dev.silentsean.mod.devsession.common.config.Account;

public interface IAuthProvider {

    SessionData login(Account account);

}
