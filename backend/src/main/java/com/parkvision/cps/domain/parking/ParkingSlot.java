package com.parkvision.cps.domain.parking;

public class ParkingSlot {
    private final String id;
    private final String layer;
    private SlotStatus status;

    public ParkingSlot(String id, String layer, SlotStatus status) {
        this.id = id;
        this.layer = layer;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getLayer() {
        return layer;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }
}
