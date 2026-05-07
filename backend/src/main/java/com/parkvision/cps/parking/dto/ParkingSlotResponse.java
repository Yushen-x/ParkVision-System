package com.parkvision.cps.parking.dto;

import com.parkvision.cps.parking.ParkingSlot;
import com.parkvision.cps.parking.SlotStatus;

public record ParkingSlotResponse(
        String id,
        String layer,
        String status,
        boolean available,
        String renderColor
) {
    public static ParkingSlotResponse from(ParkingSlot slot) {
        return new ParkingSlotResponse(
                slot.getId(),
                slot.getLayer(),
                slot.getStatus().name().toLowerCase(),
                slot.getStatus() == SlotStatus.EMPTY,
                colorOf(slot.getStatus())
        );
    }

    private static String colorOf(SlotStatus status) {
        return switch (status) {
            case EMPTY -> "cyan";
            case OCCUPIED -> "blue";
            case CHARGING -> "green";
            case BUFFER -> "amber";
            case MAINTENANCE -> "red";
        };
    }
}
