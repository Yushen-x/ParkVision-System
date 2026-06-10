package com.parkvision.cps.service;

import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.parking.ParkingSlot;
import com.parkvision.cps.domain.parking.SlotStatus;
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

    public ParkingSlotResponse clearSlot(String slotId) {
        ParkingSlot slot = repository.findSlotById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + slotId));
        slot.setStatus(SlotStatus.EMPTY);
        repository.saveSlot(slot);
        repository.findOrders().stream()
                .filter(o -> slotId.equals(o.getSlotId()) && o.getStatus() != OrderStatus.FINISHED)
                .forEach(o -> {
                    o.setStatus(OrderStatus.FINISHED);
                    repository.saveOrder(o);
                });
        return ParkingSlotResponse.from(slot);
    }
}
