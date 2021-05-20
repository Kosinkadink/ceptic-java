package ceptic.server;

class SettingBoundedResult {

    private final String error;
    private final int value;

    public SettingBoundedResult(String error, int value) {
        this.error = error;
        this.value = value;
    }

    public boolean hasError() {
        return !error.isEmpty();
    }

    public String getError() {
        return error;
    }

    public int getValue() {
        return value;
    }
}
