package com.parkvision.cps.config;

import com.parkvision.cps.domain.admin.AccessListItem;
import com.parkvision.cps.domain.admin.PricingRule;
import com.parkvision.cps.repository.FallbackParkVisionRepository;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "parkvision.persistence", name = "mode", havingValue = "jdbc", matchIfMissing = true)
public class DatabaseSeeder implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final ParkVisionRepository repository;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate, ParkVisionRepository repository) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer slotCount = jdbcTemplate.queryForObject("select count(*) from parking_slot", Integer.class);
        FallbackParkVisionRepository seed = new FallbackParkVisionRepository();

        boolean freshDatabase = slotCount == null || slotCount == 0;
        if (freshDatabase) {
            seed.findSlots().forEach(repository::saveSlot);
        }

        seed.findOrders().forEach(order -> insertIfMissing(
                "select count(*) from parking_order where order_no = ?",
                () -> repository.saveOrder(order),
                order.getOrderNo()
        ));
        seed.findAlerts().forEach(alert -> insertIfMissing(
                "select count(*) from alert_event where alert_no = ?",
                () -> repository.saveAlert(alert),
                alert.alertNo()
        ));
        seed.findSystemNodes().forEach(node -> insertIfMissing(
                "select count(*) from system_node_status where node_name = ?",
                () -> repository.saveSystemNode(node),
                node.name()
        ));
        seed.findAgvUnits().forEach(agv -> insertIfMissing(
                "select count(*) from agv_unit where agv_id = ?",
                () -> repository.saveAgvUnit(agv),
                agv.getId()
        ));
        seed.findDispatchQueue().forEach(task -> insertIfMissing(
                "select count(*) from dispatch_task where plate_no = ? and task_type = ? and tag_name = ? and wait_time = ? and vip = ?",
                () -> repository.enqueueDispatchTask(task),
                task.getPlateNo(),
                task.getType(),
                task.getTag(),
                task.getWait(),
                task.isVip()
        ));
        seed.findCameraDevices().forEach(camera -> insertIfMissing(
                "select count(*) from vision_camera where camera_id = ?",
                () -> repository.saveCameraDevice(camera),
                camera.cameraId()
        ));
        seed.findGateDevices().forEach(gate -> insertIfMissing(
                "select count(*) from gate_device where gate_id = ?",
                () -> repository.saveGateDevice(gate),
                gate.gateId()
        ));
        seed.findChargingStations().forEach(station -> insertIfMissing(
                "select count(*) from charging_station where charger_id = ?",
                () -> repository.saveChargingStation(station),
                station.chargerId()
        ));
        seed.findDeviceEvents().forEach(event -> insertIfMissing(
                "select count(*) from device_event where event_id = ?",
                () -> repository.saveDeviceEvent(event),
                event.eventId()
        ));
        seed.findPricingRules().forEach(rule -> insertIfMissing(
                "select count(*) from pricing_rule where rule_name = ?",
                () -> insertPricingRule(rule),
                rule.name()
        ));
        seed.findAccessList().forEach(item -> insertIfMissing(
                "select count(*) from access_list_item where plate_no = ?",
                () -> insertAccessListItem(item),
                item.plateNo()
        ));
    }

    private void insertPricingRule(PricingRule rule) {
        jdbcTemplate.update(
                "insert into pricing_rule (rule_name, time_range, method, extra_policy, status) values (?, ?, ?, ?, ?)",
                rule.name(),
                rule.timeRange(),
                rule.method(),
                rule.extraPolicy(),
                rule.status()
        );
    }

    private void insertAccessListItem(AccessListItem item) {
        jdbcTemplate.update(
                "insert into access_list_item (plate_no, list_type, user_type, valid_until, remark) values (?, ?, ?, ?, ?)",
                item.plateNo(),
                item.listType(),
                item.userType(),
                item.validUntil(),
                item.remark()
        );
    }

    private void insertIfMissing(String existsSql, Runnable action, Object... args) {
        Integer count = jdbcTemplate.queryForObject(existsSql, Integer.class, args);
        if (count == null || count == 0) {
            action.run();
        }
    }
}
