package com.parkvision.cps.dto.device;

import com.parkvision.cps.domain.device.CameraDevice;
import com.parkvision.cps.domain.device.ChargingStation;
import com.parkvision.cps.domain.device.DeviceEvent;
import com.parkvision.cps.domain.device.GateDevice;

import java.util.List;

public record DeviceOverview(
        List<CameraDevice> cameras,
        List<GateDevice> gates,
        List<ChargingStation> chargers,
        List<DeviceEvent> events
) {
}
