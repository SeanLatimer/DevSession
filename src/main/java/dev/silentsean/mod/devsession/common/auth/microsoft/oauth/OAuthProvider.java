package dev.silentsean.mod.devsession.common.auth.microsoft.oauth;

import dev.silentsean.mod.devsession.common.auth.microsoft.token.OAuthToken;
import org.apache.logging.log4j.Logger;

public abstract class OAuthProvider {

    protected final Logger logger;
    protected final String scopes;

    public OAuthProvider(Logger logger, String scopes) {
        this.logger = logger;
        this.scopes = scopes;
    }

    public abstract OAuthToken getOAuthToken();

    public abstract OAuthToken refreshToken(OAuthToken token);
}