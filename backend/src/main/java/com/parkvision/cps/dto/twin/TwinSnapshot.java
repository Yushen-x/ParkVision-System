package com.parkvision.cps.dto.twin;

import com.parkvision.cps.domain.dashboard.DashboardSummary;
import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.dto.parking.ParkingSlotResponse;

import java.util.List;

/**
 * Authoritative real-time state pushed over the digital-twin WebSocket channel.
 * Fields mirror the existing REST DTOs so the frontend can apply the payload
 * with the same logic it uses for the polled endpoints.
 */
public record TwinSnapshot(
        DashboardSummary summary,
        List<ParkingSlotResponse> slots,
        List<AgvUnit> agvs,
        List<DispatchTask> queue,
        boolean emergency,
        long ts
) {
}
