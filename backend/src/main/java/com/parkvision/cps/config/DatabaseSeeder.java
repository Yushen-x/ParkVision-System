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
        if (slotCount != null && slotCount > 0) {
            return;
        }

        FallbackParkVisionRepository seed = new FallbackParkVisionRepository();

        seed.findSlots().forEach(repository::saveSlot);
        seed.findOrders().forEach(repository::saveOrder);
        seed.findAlerts().forEach(repository::saveAlert);
        seed.findSystemNodes().forEach(repository::saveSystemNode);
        seed.findAgvUnits().forEach(repository::saveAgvUnit);
        seed.findDispatchQueue().forEach(repository::enqueueDispatchTask);
        seed.findCameraDevices().forEach(repository::saveCameraDevice);
        seed.findGateDevices().forEach(repository::saveGateDevice);
        seed.findChargingStations().forEach(repository::saveChargingStation);
        seed.findDeviceEvents().forEach(repository::saveDeviceEvent);
        seed.findPricingRules().forEach(this::insertPricingRule);
        seed.findAccessList().forEach(this::insertAccessListItem);
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
}
