package dev.silentsean.mod.devsession.common.auth.microsoft.storage;

import dev.silentsean.mod.devsession.common.util.Util;
import org.apache.logging.log4j.Logger;

public interface NativeCreds {

    String read(String key);

    void write(String key, String value);

    void delete(String key);

    String describe();

    static NativeCreds open(Logger logger) {
        if (Util.isWindows()) {
            return WindowsCredStore.open();
        }
        return JavaKeyringCreds.open(logger);
    }

}
