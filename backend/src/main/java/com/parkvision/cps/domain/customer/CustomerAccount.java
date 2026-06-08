package com.parkvision.cps.domain.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerAccount(
        String ownerId,
        String ownerName,
        String phoneMasked,
        String memberLevel,
        String accountStatus,
        BigDecimal balance,
        LocalDateTime createdAt
) {
}
