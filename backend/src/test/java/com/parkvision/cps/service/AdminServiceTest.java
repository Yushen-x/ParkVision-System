package com.parkvision.cps.service;

import com.parkvision.cps.dto.admin.AdminOverviewMetrics;
import com.parkvision.cps.repository.FallbackParkVisionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminServiceTest {

    @Test
    void overviewMetricsReflectPersistedBillingAndProfiles() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        AdminService adminService = new AdminService(repository);

        AdminOverviewMetrics overview = adminService.overviewMetrics();

        assertThat(overview.customerCount()).isEqualTo(repository.findCustomerAccounts().size());
        assertThat(overview.vehicleCount()).isEqualTo(repository.findVehicleProfiles().size());
        assertThat(overview.paymentCount()).isEqualTo(repository.findPaymentTransactions().size());
        assertThat(overview.collectedRevenue()).isPositive();
        assertThat(overview.activeOrders()).isPositive();
    }

    @Test
    void orderRowsCanBeFilteredByStatusAndKeyword() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        AdminService adminService = new AdminService(repository);

        assertThat(adminService.orderRows("FINISHED", null, null, null))
                .allMatch(row -> "Closed".equals(row.status()));
        assertThat(adminService.orderRows(null, "SH-D5218", null, null))
                .allMatch(row -> row.plateNo().contains("SH-D5218"));
    }

    @Test
    void paymentsCanBeFilteredByMethodAndKeyword() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        AdminService adminService = new AdminService(repository);

        assertThat(adminService.payments("SUCCESS", "AUTO_SETTLEMENT", null, null, null)).isNotEmpty();
        assertThat(adminService.payments(null, null, "PV20260506004", null, null))
                .allMatch(payment -> payment.orderNo().contains("PV20260506004"));
    }

    @Test
    void ordersAndPaymentsCanBeFilteredByDateRange() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        AdminService adminService = new AdminService(repository);

        assertThat(adminService.orderRows(null, null, "2099-01-01", "2099-01-31")).isEmpty();
        assertThat(adminService.payments(null, null, null, "2099-01-01", "2099-01-31")).isEmpty();
        assertThat(adminService.orderRows(null, null, "2026-01-01", "2026-12-31")).isNotEmpty();
        assertThat(adminService.payments(null, null, null, "2026-01-01", "2026-12-31")).isNotEmpty();
    }

    @Test
    void alertsCanBeFilteredByLevelStatusAndKeyword() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        AdminService adminService = new AdminService(repository);

        assertThat(adminService.alerts("高", "急停中", null))
                .allMatch(alert -> "高".equals(alert.level()) && "急停中".equals(alert.status()));
        assertThat(adminService.alerts(null, null, "AGV-04"))
                .allMatch(alert -> alert.content().contains("AGV-04"));
    }

    @Test
    void orderDetailAggregatesPaymentBillingAndCustomerContext() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        AdminService adminService = new AdminService(repository);

        var detail = adminService.orderDetail("PV20260506004");

        assertThat(detail.orderNo()).isEqualTo("PV20260506004");
        assertThat(detail.customer()).isNotNull();
        assertThat(detail.vehicle()).isNotNull();
        assertThat(detail.payment()).isNotNull();
        assertThat(detail.billingComponents()).isNotEmpty();
        assertThat(detail.paymentStatus()).isEqualTo("PAID");
    }

    @Test
    void customerDetailAggregatesVehiclesOrdersAndPayments() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        AdminService adminService = new AdminService(repository);

        var detail = adminService.customerDetail("CUS0001");

        assertThat(detail.ownerId()).isEqualTo("CUS0001");
        assertThat(detail.totalVehicles()).isPositive();
        assertThat(detail.vehicles()).isNotEmpty();
        assertThat(detail.recentOrders()).isNotEmpty();
        assertThat(detail.totalPaid()).isNotNegative();
    }

    @Test
    void alertDetailAggregatesDeviceEventsAndOrderHints() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        AdminService adminService = new AdminService(repository);

        var detail = adminService.alertDetail("AL20260518001");

        assertThat(detail.alertNo()).isEqualTo("AL20260518001");
        assertThat(detail.recommendedAction()).isNotBlank();
        assertThat(detail.escalationState()).isNotBlank();
        assertThat(detail.deviceEvents()).isNotEmpty();
    }
}
