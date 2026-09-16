package dev.silentsean.mod.devsession.common.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import dev.silentsean.mod.devsession.common.Properties;
import dev.silentsean.mod.devsession.common.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class DevSessionConfig {

    private static final String[] TOKEN_STORAGE_MODES = {"auto", "keyring", "file"};
    private static final String[] TOKEN_CACHE_MODES = {"all", "refresh"};
    private static final String[] GRANT_FLOWS = {"browser", "device-code"};

    private final boolean defaultEnabled;
    private final String defaultAccount;
    private final Map<String, Account> accounts;
    private final File configDir;
    private final String tokenStorage;
    private final String tokenCache;
    private final boolean forceTokenRefresh;
    private final int profileCacheMinutes;
    private final String grantFlow;
    private final String deviceCodeProvider;
    private final String clientId;

    private DevSessionConfig(boolean defaultEnabled, String defaultAccount, Map<String, Account> accounts, File configDir,
                             String tokenStorage, String tokenCache, boolean forceTokenRefresh, int profileCacheMinutes,
                             String grantFlow, String deviceCodeProvider, String clientId) {
        this.defaultEnabled = defaultEnabled;
        this.defaultAccount = defaultAccount;
        this.accounts = accounts;
        this.configDir = configDir;
        this.tokenStorage = tokenStorage;
        this.tokenCache = tokenCache;
        this.forceTokenRefresh = forceTokenRefresh;
        this.profileCacheMinutes = profileCacheMinutes;
        this.grantFlow = grantFlow;
        this.deviceCodeProvider = deviceCodeProvider;
        this.clientId = clientId;
    }

    public static DevSessionConfig load() {
        String configDirPath = Properties.CONFIG_DIR.getValue();
        if (configDirPath == null) configDirPath = Util.getDefaultConfigDir().getAbsolutePath();

        File configDir = new File(configDirPath);

        if (configDir.exists() && !configDir.isDirectory()) throw new RuntimeException("Config directory is not a directory");

        File configFile = new File(configDir, "config.toml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();

            try (InputStream is = DevSessionConfig.class.getResourceAsStream("/assets/devsession/config.default.toml")) {
                if (is == null) {
                    throw new RuntimeException("Failed to locate default config file");
                }
                FileUtils.writeByteArrayToFile(configFile, IOUtils.toByteArray(is));
            } catch (Exception e) {
                throw new RuntimeException("Failed to write default config file", e);
            }
        }

        FileConfig config = FileConfig.of(configFile);
        config.load();

        boolean defaultEnabled = config.getOrElse("defaultEnabled", false);
        String defaultAccount = config.get("defaultAccount");

        Map<String, Account> accounts = new LinkedHashMap<>();

        Config accountsConfig = config.get("accounts");
        if (accountsConfig != null) {
            for (Config.Entry entry : accountsConfig.entrySet()) {
                Config value = entry.getValue();
                accounts.put(entry.getKey(), new Account(
                    entry.getKey(),
                    AccountType.of(value.get("type")),
                    value.get("username"),
                    value.get("password")
                ));
            }
        }

        String tokenStorage = resolve(config, Properties.TOKEN_STORAGE).toLowerCase(Locale.ROOT);
        if (!matches(tokenStorage, TOKEN_STORAGE_MODES)) {
            throw new RuntimeException("Invalid tokenStorage value '" + tokenStorage + "', valid options are: " + String.join(", ", TOKEN_STORAGE_MODES));
        }

        String tokenCache = resolve(config, Properties.TOKEN_CACHE).toLowerCase(Locale.ROOT);
        if (!matches(tokenCache, TOKEN_CACHE_MODES)) {
            throw new RuntimeException("Invalid tokenCache value '" + tokenCache + "', valid options are: " + String.join(", ", TOKEN_CACHE_MODES));
        }

        boolean forceTokenRefresh = Properties.parseBoolean(resolve(config, Properties.FORCE_TOKEN_REFRESH));

        int profileCacheMinutes;
        try {
            profileCacheMinutes = Integer.parseInt(resolve(config, Properties.PROFILE_CACHE_MINUTES));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid profileCacheMinutes value '" + resolve(config, Properties.PROFILE_CACHE_MINUTES) + "', it must be a whole number", e);
        }

        String grantFlow = resolve(config, Properties.GRANT_FLOW).toLowerCase(Locale.ROOT);
        if (!matches(grantFlow, GRANT_FLOWS)) {
            throw new RuntimeException("Invalid microsoft.grantFlow value '" + grantFlow + "', valid options are: " + String.join(", ", GRANT_FLOWS));
        }

        String deviceCodeProvider = resolve(config, Properties.DEVICE_CODE_PROVIDER).toLowerCase(Locale.ROOT);
        String clientId = resolveNullable(config, Properties.CLIENT_ID);

        return new DevSessionConfig(defaultEnabled, defaultAccount, accounts, configDir,
            tokenStorage, tokenCache, forceTokenRefresh, profileCacheMinutes, grantFlow, deviceCodeProvider, clientId);
    }

    private static String resolve(FileConfig config, Properties property) {
        String override = property.getOverridingValue();
        if (override != null) return override;
        Object value = config.get(property.getKey());
        return value != null ? value.toString() : property.getDefaultValue();
    }

    private static String resolveNullable(FileConfig config, Properties property) {
        String override = property.getOverridingValue();
        if (override != null) return override;
        Object value = config.get(property.getKey());
        return value != null ? value.toString() : null;
    }

    private static boolean matches(String value, String[] valid) {
        for (String candidate : valid) {
            if (candidate.equals(value)) return true;
        }
        return false;
    }

    public boolean getDefaultEnabled() {
        return defaultEnabled;
    }

    public String getDefaultAccount() {
        return defaultAccount;
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public File getConfigDir() {
        return configDir;
    }

    public String getTokenStorage() {
        return tokenStorage;
    }

    public String getTokenCache() {
        return tokenCache;
    }

    public boolean getForceTokenRefresh() {
        return forceTokenRefresh;
    }

    public int getProfileCacheMinutes() {
        return profileCacheMinutes;
    }

    public String getGrantFlow() {
        return grantFlow;
    }

    public String getDeviceCodeProvider() {
        return deviceCodeProvider;
    }

    public String getClientId() {
        return clientId;
    }
}
