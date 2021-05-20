package ceptic.common;

import java.util.concurrent.TimeUnit;

public class Timer {

    private long startTime = 0;
    private long endTime = 0;

    public Timer() {

    }

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void update() {
        start();
    }

    public void stop() {
        endTime = System.currentTimeMillis();
    }

    public long getTimeDiffMillis() {
        return endTime-startTime;
    }

    public long getTimeMillis() {
        return System.currentTimeMillis()-startTime;
    }

    public double getTimeSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(getTimeMillis());
    }

}
