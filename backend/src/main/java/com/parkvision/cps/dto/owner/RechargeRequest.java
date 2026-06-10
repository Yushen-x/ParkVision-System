package com.parkvision.cps.dto.owner;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RechargeRequest(
        @NotNull BigDecimal amount
) {
}
