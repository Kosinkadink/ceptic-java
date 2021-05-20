package ceptic.commands;

public class CommandSettings {

    protected final int bodyMax;

    protected CommandSettings(int bodyMax) {
        this.bodyMax = bodyMax;
    }

    public static CommandSettings create(int bodyMax) {
        return new CommandSettings(bodyMax);
    }

    public static CommandSettings createNoBodyAllowed() {
        return new CommandSettings(0);
    }

}
