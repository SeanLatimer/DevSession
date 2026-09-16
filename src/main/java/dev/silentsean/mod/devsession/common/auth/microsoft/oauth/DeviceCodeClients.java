package dev.silentsean.mod.devsession.common.auth.microsoft.oauth;

public enum DeviceCodeClients {

    MULTIMC("multimc", "499546d9-bbfe-4b9b-a086-eb3d75afb78f"),
    DEVLOGIN("devlogin", "170105bd-9573-4222-b09c-6f24c3b77cd8"),
    PRISM("prism", "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb");

    private final String name;
    private final String clientId;

    DeviceCodeClients(String name, String clientId) {
        this.name = name;
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public String getClientId() {
        return clientId;
    }

    public static DeviceCodeClients of(String name) {
        for (DeviceCodeClients client : values()) {
            if (client.name.equalsIgnoreCase(name)) {
                return client;
            }
        }
        throw new IllegalArgumentException("Unknown device-code provider '" + name + "', valid options are: multimc, devlogin, prism");
    }

}
