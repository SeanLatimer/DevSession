package dev.silentsean.mod.devsession;

import dev.silentsean.mod.devsession.common.DevSession;

public class DevSessionBootstrap {

    private static final DevSession DevSession = new DevSession();

    public static String[] processArguments(String[] args) {
        return DevSession.processArguments(args);
    }

}
