package dev.silentsean.mod.devsession.common.config;

import dev.silentsean.mod.devsession.common.auth.IAuthProviderFactory;

public enum AccountType {

    MICROSOFT(IAuthProviderFactory.MICROSOFT);

    private final IAuthProviderFactory authProvider;

    AccountType(IAuthProviderFactory authProvider) {
        this.authProvider = authProvider;
    }

    public IAuthProviderFactory getAuthProviderFactory() {
        return authProvider;
    }

    public static AccountType of(String name) {
        for (AccountType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No such account type '" + name + "'");
    }
}
