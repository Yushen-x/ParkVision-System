package com.parkvision.cps.domain.dispatch;

public class AgvUnit {
    private final String id;
    private int x;
    private int y;
    private boolean loaded;
    private String task;
    private int batteryPct;
    private String mode;
    private double velocityMps;
    private String lastCommand;

    public AgvUnit(String id, int x, int y, boolean loaded, String task) {
        this(id, x, y, loaded, task, 100, "IDLE", 0.0, "hold");
    }

    public AgvUnit(String id, int x, int y, boolean loaded, String task, int batteryPct, String mode, double velocityMps, String lastCommand) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.loaded = loaded;
        this.task = task;
        this.batteryPct = batteryPct;
        this.mode = mode;
        this.velocityMps = velocityMps;
        this.lastCommand = lastCommand;
    }

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public int getBatteryPct() {
        return batteryPct;
    }

    public void setBatteryPct(int batteryPct) {
        this.batteryPct = batteryPct;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public double getVelocityMps() {
        return velocityMps;
    }

    public void setVelocityMps(double velocityMps) {
        this.velocityMps = velocityMps;
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }
}
