package com.parkvision.cps.parking;

import com.parkvision.cps.infrastructure.InMemoryDataStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ParkingService {
    private final InMemoryDataStore store;

    public ParkingService(InMemoryDataStore store) {
        this.store = store;
    }

    public List<Map<String, String>> slots() {
        return store.slots().stream()
                .map(slot -> Map.of(
                        "id", slot.getId(),
                        "layer", slot.getLayer(),
                        "status", slot.getStatus().name().toLowerCase()
                ))
                .toList();
    }
}
