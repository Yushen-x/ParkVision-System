package com.parkvision.cps.dto.owner;

import com.parkvision.cps.domain.billing.PaymentTransaction;

import java.math.BigDecimal;
import java.util.List;

public record OwnerWallet(
        String ownerId,
        String memberLevel,
        String accountStatus,
        BigDecimal balance,
        BigDecimal discountRate,
        List<PaymentTransaction> transactions
) {
}
