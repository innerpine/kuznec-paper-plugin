package dev.mark.kuznec.config;

public enum FillerMenuType {
    MAIN("main"),
    UPGRADE("upgrade");

    private final String configKey;

    FillerMenuType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
