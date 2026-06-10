package com.parkvision.cps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkvision.cps.dto.twin.TwinSnapshot;
import com.parkvision.cps.web.TwinStreamHub;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Periodically builds an authoritative twin snapshot from the live repository
 * state and pushes it to connected SSE clients. This replaces the previous
 * 5-second frontend polling for digital-twin / dashboard state, meeting the
 * sub-second realtime requirement (UC18). Snapshots are only built when at least
 * one client is connected, so there is no idle database churn.
 */
@Service
public class TwinBroadcastService {

    private final TwinStreamHub hub;
    private final DashboardService dashboardService;
    private final ParkingService parkingService;
    private final DispatchService dispatchService;
    private final DeviceService deviceService;
    private final ObjectMapper objectMapper;

    public TwinBroadcastService(
            TwinStreamHub hub,
            DashboardService dashboardService,
            ParkingService parkingService,
            DispatchService dispatchService,
            DeviceService deviceService,
            ObjectMapper objectMapper
    ) {
        this.hub = hub;
        this.dashboardService = dashboardService;
        this.parkingService = parkingService;
        this.dispatchService = dispatchService;
        this.deviceService = deviceService;
        this.objectMapper = objectMapper;
    }

    public TwinSnapshot buildSnapshot() {
        return new TwinSnapshot(
                dashboardService.summary(),
                parkingService.slots(),
                dispatchService.agvs(),
                dispatchService.queue(),
                deviceService.emergencyActive(),
                System.currentTimeMillis()
        );
    }

    public String currentSnapshotJson() {
        try {
            return objectMapper.writeValueAsString(buildSnapshot());
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    @Scheduled(fixedRateString = "${parkvision.twin.broadcast-interval-ms:1000}")
    public void broadcast() {
        if (!hub.hasClients()) {
            return;
        }
        String snapshot = currentSnapshotJson();
        if (snapshot != null) {
            hub.broadcast(snapshot);
        }
    }
}
