package com.parkvision.cps.dto.owner;

import jakarta.validation.constraints.NotBlank;

public record ReservationRequest(
        @NotBlank String plateNo,
        String phone,
        String energyType
) {
}
