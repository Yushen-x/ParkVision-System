package com.parkvision.cps.service;

import com.parkvision.cps.repository.ParkVisionRepository;
import com.parkvision.cps.dto.parking.ParkingSlotResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingService {
    private final ParkVisionRepository repository;

    public ParkingService(ParkVisionRepository repository) {
        this.repository = repository;
    }

    public List<ParkingSlotResponse> slots() {
        return repository.findSlots().stream()
                .map(ParkingSlotResponse::from)
                .toList();
    }
}
