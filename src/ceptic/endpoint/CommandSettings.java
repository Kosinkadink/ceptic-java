package ceptic.endpoint;

public class CommandSettings {

    public final int bodyMax;
    public final long timeMax;

    public CommandSettings(int bodyMax, long timeMax) {
        this.bodyMax = bodyMax;
        this.timeMax = timeMax;
    }

    public static CommandSettings createWithBodyMax(int bodyMax) {
        return new CommandSettings(bodyMax, 0);
    }

}
