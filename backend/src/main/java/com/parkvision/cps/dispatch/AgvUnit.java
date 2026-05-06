package com.parkvision.cps.dispatch;

public class AgvUnit {
    private final String id;
    private int x;
    private int y;
    private boolean loaded;
    private String task;

    public AgvUnit(String id, int x, int y, boolean loaded, String task) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.loaded = loaded;
        this.task = task;
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
}
