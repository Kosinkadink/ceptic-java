package ceptic.common;

import java.util.concurrent.TimeUnit;

public class Stopwatch {

    private long startTime = 0;
    private long endTime = 0;

    public Stopwatch() {

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
