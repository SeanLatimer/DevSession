package dev.silentsean.mod.devsession.common;

import java.util.Locale;

public enum Properties {

    ENABLED("enabled", "false"),
    CONFIG_DIR("configDir", null),
    ACCOUNT("account", null),
    TOKEN_STORAGE("tokenStorage", "auto"),
    TOKEN_CACHE("tokenCache", "all"),
    FORCE_TOKEN_REFRESH("forceTokenRefresh", "false"),
    PROFILE_CACHE_MINUTES("profileCacheMinutes", "360"),
    GRANT_FLOW("microsoft.grantFlow", "browser"),
    DEVICE_CODE_PROVIDER("microsoft.deviceCodeProvider", "multimc"),
    CLIENT_ID("microsoft.clientId", null);

    private final String key;
    private final String defaultValue;

    Properties(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getFullKey() {
        return "devsession." + key;
    }

    public String getEnvKey() {
        String snake = key.replace('.', '_').replaceAll("([a-z0-9])([A-Z])", "$1_$2");
        return "DEVSESSION_" + snake.toUpperCase(Locale.ROOT);
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getValue() {
        String override = getOverridingValue();
        if (override != null) return override;
        return defaultValue;
    }

    public String getOverridingValue() {
        String env = System.getenv(getEnvKey());
        if (env != null && !env.isEmpty()) return env;
        String property = System.getProperty(getFullKey());
        if (property != null && !property.isEmpty()) return property;
        return null;
    }

    public BooleanState getBooleanValue() {
        String override = getOverridingValue();
        if (override == null) return BooleanState.NOT_SET;
        return BooleanState.of(parseBoolean(override));
    }

    public static boolean parseBoolean(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("1")) return true;
        if (normalized.equals("0")) return false;
        return Boolean.parseBoolean(normalized);
    }

    public enum BooleanState {
        TRUE,
        FALSE,
        NOT_SET;

        public static BooleanState of(boolean state) {
            if (state) {
                return TRUE;
            } else {
                return FALSE;
            }
        }

        public boolean toBoolean() {
            if (this == NOT_SET) {
                throw new UnsupportedOperationException("NOT_SET cannot be converted to a boolean value");
            } else if (this == TRUE) {
                return true;
            } else {
                return false;
            }
        }
    }

}
