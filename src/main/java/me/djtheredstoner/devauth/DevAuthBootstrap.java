package me.djtheredstoner.devauth;

import me.djtheredstoner.devauth.common.DevAuth;

public class DevAuthBootstrap {

    private static final DevAuth devAuth = new DevAuth();

    public static String[] processArguments(String[] args) {
        return devAuth.processArguments(args);
    }

}
