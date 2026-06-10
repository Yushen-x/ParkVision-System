package com.parkvision.cps.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * Create-or-update payload for a customer vehicle profile. When {@code ownerId}
 * is blank a new customer account is created; otherwise the existing account is
 * updated and the vehicle is bound to it.
 */
public record CustomerVehicleUpsertRequest(
        String ownerId,
        @NotBlank String ownerName,
        String phone,
        @NotBlank String plateNo,
        String vehicleType,
        String energyType,
        String membershipType,
        String memberLevel,
        String accountStatus,
        String accessType
) {
}
