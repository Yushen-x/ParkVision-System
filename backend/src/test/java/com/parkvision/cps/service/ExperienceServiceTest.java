package com.parkvision.cps.service;

import com.parkvision.cps.dto.navigation.IndoorNavigationSnapshot;
import com.parkvision.cps.dto.pricing.PricingPreview;
import com.parkvision.cps.repository.FallbackParkVisionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExperienceServiceTest {

    @Test
    void pricingPreviewReflectsChargingAndDispatchState() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        PricingService pricingService = new PricingService(repository);

        PricingPreview preview = pricingService.preview("PV20260506002");

        assertThat(preview.orderNo()).isEqualTo("PV20260506002");
        assertThat(preview.components()).extracting(component -> component.label())
                .contains("新能源充电");
        assertThat(preview.totalAmount()).isGreaterThan(preview.baseAmount());
    }

    @Test
    void indoorRouteReturnsLiveHandoffSnapshot() {
        FallbackParkVisionRepository repository = new FallbackParkVisionRepository();
        DeviceService deviceService = new DeviceService(repository, true);
        NavigationService navigationService = new NavigationService(repository, deviceService);

        IndoorNavigationSnapshot snapshot = navigationService.currentRoute("PV20260506001");

        assertThat(snapshot.orderNo()).isEqualTo("PV20260506001");
        assertThat(snapshot.remainingMeters()).isPositive();
        assertThat(snapshot.handoffZone()).isEqualTo("A 区交接点");
    }
}
