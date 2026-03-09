package dev.mark.kuznec.config;

public final class ConfigValidationException extends IllegalStateException {

    private final ConfigValidationResult result;

    public ConfigValidationException(ConfigValidationResult result) {
        super(result.formatErrors());
        this.result = result;
    }

    public ConfigValidationResult getResult() {
        return result;
    }
}
