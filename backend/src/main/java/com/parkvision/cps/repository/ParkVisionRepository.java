package com.parkvision.cps.repository;

import com.parkvision.cps.domain.admin.AlertEvent;
import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.domain.admin.SystemNodeStatus;
import com.parkvision.cps.domain.billing.OrderBillingComponent;
import com.parkvision.cps.domain.billing.PaymentTransaction;
import com.parkvision.cps.domain.customer.CustomerAccount;
import com.parkvision.cps.domain.customer.VehicleProfile;
import com.parkvision.cps.domain.device.CameraDevice;
import com.parkvision.cps.domain.device.ChargingStation;
import com.parkvision.cps.domain.device.DeviceEvent;
import com.parkvision.cps.domain.device.GateDevice;
import com.parkvision.cps.domain.dispatch.AgvUnit;
import com.parkvision.cps.domain.dispatch.DispatchTask;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.domain.parking.ParkingSlot;
import com.parkvision.cps.domain.reservation.Reservation;
import com.parkvision.cps.domain.vision.RecognitionEvent;

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

    List<Reservation> findReservations();

    Optional<Reservation> findReservationById(String id);

    Reservation saveReservation(Reservation reservation);

    List<CustomerAccount> findCustomerAccounts();

    CustomerAccount saveCustomerAccount(CustomerAccount account);

    List<VehicleProfile> findVehicleProfiles();

    VehicleProfile saveVehicleProfile(VehicleProfile vehicle);

    void deleteVehicleProfile(String plateNo);

    List<PaymentTransaction> findPaymentTransactions();

    Optional<PaymentTransaction> findPaymentByOrderNo(String orderNo);

    PaymentTransaction savePaymentTransaction(PaymentTransaction payment);

    List<OrderBillingComponent> findBillingComponentsByOrderNo(String orderNo);

    OrderBillingComponent saveBillingComponent(OrderBillingComponent component);

    List<AlertEvent> findAlerts();

    AlertEvent saveAlert(AlertEvent alert);

    List<PricingRule> findPricingRules();

    Optional<PricingRule> findPricingRuleById(String id);

    PricingRule savePricingRule(PricingRule rule);

    void deletePricingRule(String id);

    List<AccessListItem> findAccessList();

    Optional<AccessListItem> findAccessListItem(String plateNo);

    List<RecognitionEvent> findRecognitionEvents();

    RecognitionEvent saveRecognitionEvent(RecognitionEvent event);

    List<SystemNodeStatus> findSystemNodes();

    List<com.parkvision.cps.domain.admin.AuditLog> findAuditLogs(int limit);

    com.parkvision.cps.domain.admin.AuditLog saveAuditLog(com.parkvision.cps.domain.admin.AuditLog log);

    SystemNodeStatus saveSystemNode(SystemNodeStatus node);

    List<AgvUnit> findAgvUnits();

    Optional<AgvUnit> findAgvById(String agvId);

    AgvUnit saveAgvUnit(AgvUnit agv);

    List<DispatchTask> findDispatchQueue();

    DispatchTask enqueueDispatchTask(DispatchTask task);

    DispatchTask saveDispatchTask(DispatchTask task);

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
