package com.parkvision.cps.domain.device;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChargingStation(
        String chargerId,
        String protocol,
        String endpoint,
        String connectorStatus,
        BigDecimal powerKw,
        BigDecimal sessionKwh,
        String vehiclePlate,
        String authStatus,
        LocalDateTime lastSeen,
        String detail
) {
}
