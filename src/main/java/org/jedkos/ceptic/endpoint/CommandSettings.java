package org.jedkos.ceptic.endpoint;

public class CommandSettings {

    public final int bodyMax;
    public final long timeMax;

    public CommandSettings(int bodyMax, long timeMax) {
        this.bodyMax = bodyMax;
        this.timeMax = timeMax;
    }

    public CommandSettings copy() {
        return new CommandSettings(bodyMax, timeMax);
    }

    public static CommandSettings combine(CommandSettings base, CommandSettings updates) {
        int bodyMax = base.bodyMax;
        long timeMax = base.timeMax;
        if (updates.bodyMax >= 0)
            bodyMax = updates.bodyMax;
        if (updates.timeMax >= 0)
            timeMax = updates.timeMax;
        return new CommandSettings(bodyMax, timeMax);
    }

    public static CommandSettings createWithBodyMax(int bodyMax) {
        return new CommandSettings(bodyMax, -1);
    }

}
