package com.parkvision.cps.domain.customer;

import java.time.LocalDateTime;

public record VehicleProfile(
        String plateNo,
        String ownerId,
        String vehicleType,
        String energyType,
        String membershipType,
        String defaultAuthStatus,
        LocalDateTime createdAt
) {
}
