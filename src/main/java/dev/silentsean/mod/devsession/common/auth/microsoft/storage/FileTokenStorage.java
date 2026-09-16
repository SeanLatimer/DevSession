package dev.silentsean.mod.devsession.common.auth.microsoft.storage;

import com.google.gson.JsonObject;
import dev.silentsean.mod.devsession.common.auth.microsoft.token.OAuthToken;
import dev.silentsean.mod.devsession.common.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class FileTokenStorage implements TokenStorage {

    private static final Logger logger = LogManager.getLogger("DevSession/FileStorage");
    private static final int FILE_VERSION = 1;

    private final File accountsJson;
    private JsonObject root;

    public FileTokenStorage(File configDir) {
        this.accountsJson = new File(configDir, "microsoft_accounts.json");
    }

    @Override
    public OAuthToken loadOAuthToken(String account) {
        JsonObject entry = readAccountEntry(account);
        if (entry == null || !entry.has("oauth")) return null;
        try {
            return Util.gson.fromJson(entry.get("oauth"), OAuthToken.class);
        } catch (Exception e) {
            logger.error("Failed to parse the OAuth token for account '" + account + "' in microsoft_accounts.json", e);
            return null;
        }
    }

    @Override
    public void storeOAuthToken(String account, OAuthToken token) {
        JsonObject entry = readAccountEntry(account);
        if (entry == null) entry = new JsonObject();
        entry.add("oauth", Util.gson.toJsonTree(token));
        writeAccountEntry(account, entry);
    }

    @Override
    public void deleteOAuthToken(String account) {
        JsonObject entry = readAccountEntry(account);
        if (entry == null || !entry.has("oauth")) return;
        entry.remove("oauth");
        writeAccountEntry(account, entry);
    }

    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    public String describe() {
        return "microsoft_accounts.json";
    }

    public JsonObject readAccountEntry(String account) {
        JsonObject root = readRoot();
        if (root == null || !root.has(account)) return null;

        try {
            return root.get(account).getAsJsonObject();
        } catch (Exception e) {
            logger.error("Entry for account '" + account + "' in microsoft_accounts.json is malformed", e);
            return null;
        }
    }

    public void writeAccountEntry(String account, JsonObject entry) {
        if (readRoot() == null) root = new JsonObject();
        root.addProperty("version", FILE_VERSION);

        if (entry == null) {
            root.remove(account);
        } else {
            root.add(account, entry);
        }

        try {
            FileUtils.writeStringToFile(accountsJson, Util.gson.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Failed to write microsoft_accounts.json", e);
        }
    }

    private JsonObject readRoot() {
        if (root != null) return root;

        if (!accountsJson.exists()) return null;

        try {
            String json = FileUtils.readFileToString(accountsJson, StandardCharsets.UTF_8);
            JsonObject parsed = Util.parser.parse(json).getAsJsonObject();

            if (!parsed.has("version") || parsed.get("version").getAsInt() != FILE_VERSION) {
                logger.info("microsoft_accounts.json has unknown version. Ignoring.");
                return null;
            }

            root = parsed;
            return root;
        } catch (Exception e) {
            logger.error("Failed to parse microsoft_accounts.json", e);
            return null;
        }
    }

}
