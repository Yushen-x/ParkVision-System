package com.parkvision.cps.dto.admin;

public record AdminCustomerVehicleRow(
        String ownerId,
        String ownerName,
        String phoneMasked,
        String memberLevel,
        String accountStatus,
        String plateNo,
        String energyType,
        String membershipType,
        String accessType
) {
}
