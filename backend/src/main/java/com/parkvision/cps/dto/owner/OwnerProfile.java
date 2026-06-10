package com.parkvision.cps.dto.owner;

import java.math.BigDecimal;
import java.util.List;

/**
 * The signed-in owner's own profile: login identity plus the linked customer
 * account, bound plates and a count of currently active orders.
 */
public record OwnerProfile(
        String username,
        String displayName,
        String ownerId,
        String ownerName,
        String phoneMasked,
        String memberLevel,
        String accountStatus,
        BigDecimal balance,
        int vehicleCount,
        int activeOrders,
        List<String> plates
) {
}
