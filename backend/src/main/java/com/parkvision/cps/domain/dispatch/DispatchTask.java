package com.parkvision.cps.domain.dispatch;

public class DispatchTask {
    private final String plateNo;
    private final String type;
    private final String tag;
    private final String wait;
    private final boolean vip;

    public DispatchTask(String plateNo, String type, String tag, String wait, boolean vip) {
        this.plateNo = plateNo;
        this.type = type;
        this.tag = tag;
        this.wait = wait;
        this.vip = vip;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public String getType() {
        return type;
    }

    public String getTag() {
        return tag;
    }

    public String getWait() {
        return wait;
    }

    public boolean isVip() {
        return vip;
    }
}
