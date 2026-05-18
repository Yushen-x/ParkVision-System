package com.parkvision.cps.repository;

import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
import com.parkvision.cps.domain.device.CameraDevice;
import com.parkvision.cps.domain.device.ChargingStation;
import com.parkvision.cps.domain.device.DeviceEvent;
import com.parkvision.cps.domain.device.GateDevice;
import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.ParkingSlot;

import java.util.List;
import java.util.Optional;

public interface ParkVisionRepository {
    List<ParkingSlot> findSlots();

    Optional<ParkingSlot> findSlotById(String slotId);

    Optional<ParkingSlot> findFirstAvailableSlot();

    Optional<ParkingSlot> findFirstDeepOccupiedSlot();

    ParkingSlot saveSlot(ParkingSlot slot);

    List<ParkingOrder> findOrders();

    Optional<ParkingOrder> findOrderByNo(String orderNo);

    ParkingOrder saveOrder(ParkingOrder order);

    List<AlertEvent> findAlerts();

    AlertEvent saveAlert(AlertEvent alert);

    List<PricingRule> findPricingRules();

    List<AccessListItem> findAccessList();

    List<SystemNodeStatus> findSystemNodes();

    SystemNodeStatus saveSystemNode(SystemNodeStatus node);

    List<AgvUnit> findAgvUnits();

    Optional<AgvUnit> findAgvById(String agvId);

    AgvUnit saveAgvUnit(AgvUnit agv);

    List<DispatchTask> findDispatchQueue();

    DispatchTask enqueueDispatchTask(DispatchTask task);

    List<CameraDevice> findCameraDevices();

    Optional<CameraDevice> findCameraDeviceById(String cameraId);

    CameraDevice saveCameraDevice(CameraDevice camera);

    List<GateDevice> findGateDevices();

    Optional<GateDevice> findGateDeviceById(String gateId);

    GateDevice saveGateDevice(GateDevice gate);

    List<ChargingStation> findChargingStations();

    Optional<ChargingStation> findChargingStationById(String chargerId);

    ChargingStation saveChargingStation(ChargingStation station);

    List<DeviceEvent> findDeviceEvents();

    DeviceEvent saveDeviceEvent(DeviceEvent event);
}
