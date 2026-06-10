package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.billing.OrderBillingComponent;
import com.parkvision.cps.domain.billing.PaymentTransaction;
import com.parkvision.cps.domain.customer.CustomerAccount;
import com.parkvision.cps.domain.customer.VehicleProfile;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.dto.pricing.PricingComponent;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Turns a finished stay into money: it quotes the fee from the active pricing
 * rule, applies the member discount, debits the owner's wallet (when the plate
 * maps to a registered account) and persists the payment + billing breakdown.
 */
@Service
public class BillingService {
    private final ParkVisionRepository repository;
    private final PricingService pricingService;

    public BillingService(ParkVisionRepository repository, PricingService pricingService) {
        this.repository = repository;
        this.pricingService = pricingService;
    }

    /**
     * Settle and mutate the order in place (exit time, amount, discount, payment
     * status). Throws {@code INSUFFICIENT_BALANCE} when a registered owner cannot
     * cover the net fee, which gates retrieval/payment until they recharge.
     */
    public void settle(ParkingOrder order) {
        LocalDateTime settledAt = LocalDateTime.now();
        int durationMinutes = (int) Math.max(30, Duration.between(order.getEntryTime(), settledAt).toMinutes());
        PricingService.Quote quote = pricingService.quote(order, durationMinutes);
        BigDecimal gross = quote.gross();

        CustomerAccount account = resolveAccount(order.getPlateNo()).orElse(null);
        BigDecimal discountRate = memberDiscountRate(account);
        BigDecimal net = gross.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = gross.subtract(net).setScale(2, RoundingMode.HALF_UP);

        String method;
        if (account != null) {
            BigDecimal balance = account.balance() == null ? BigDecimal.ZERO : account.balance();
            if (balance.compareTo(net) < 0) {
                throw new BusinessException("INSUFFICIENT_BALANCE",
                        "钱包余额不足（需 " + net + " 元，当前 " + balance.setScale(2, RoundingMode.HALF_UP) + " 元），请先充值");
            }
            BigDecimal newBalance = balance.subtract(net).setScale(2, RoundingMode.HALF_UP);
            repository.saveCustomerAccount(withBalance(account, newBalance));
            method = "WALLET";
        } else {
            method = "CASH";
        }

        order.setExitTime(settledAt);
        order.setPaidAt(settledAt);
        order.setPaymentStatus("PAID");
        order.setPaymentMethod(method);
        order.setDurationMinutes(durationMinutes);
        order.setDiscountAmount(discountAmount);
        order.setAmount(net);

        repository.savePaymentTransaction(new PaymentTransaction(
                "PAY-" + order.getOrderNo(),
                order.getOrderNo(),
                order.getPlateNo(),
                net,
                method,
                "SUCCESS",
                settledAt
        ));

        int idx = 0;
        for (PricingComponent component : quote.components()) {
            if (component.amount() == null || component.amount().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            repository.saveBillingComponent(new OrderBillingComponent(
                    "BILL-" + order.getOrderNo() + "-" + idx++,
                    order.getOrderNo(),
                    component.accent().toUpperCase(),
                    component.label() + " · " + component.formula(),
                    component.amount(),
                    settledAt
            ));
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            repository.saveBillingComponent(new OrderBillingComponent(
                    "BILL-" + order.getOrderNo() + "-DISCOUNT",
                    order.getOrderNo(),
                    "MEMBER_DISCOUNT",
                    "会员折扣 · " + (account == null ? "" : account.memberLevel()),
                    discountAmount.negate(),
                    settledAt
            ));
        }
    }

    public Optional<CustomerAccount> resolveAccount(String plate) {
        Optional<String> ownerId = repository.findVehicleProfiles().stream()
                .filter(profile -> profile.plateNo().equalsIgnoreCase(plate))
                .map(VehicleProfile::ownerId)
                .findFirst();
        return ownerId.flatMap(id -> repository.findCustomerAccounts().stream()
                .filter(account -> account.ownerId().equals(id))
                .findFirst());
    }

    public BigDecimal memberDiscountRate(CustomerAccount account) {
        if (account == null || account.memberLevel() == null) {
            return BigDecimal.ONE;
        }
        String level = account.memberLevel().toUpperCase();
        if (level.contains("VIP") || level.contains("钻")) {
            return new BigDecimal("0.85");
        }
        if (level.contains("GOLD") || level.contains("金")) {
            return new BigDecimal("0.90");
        }
        if (level.contains("SILVER") || level.contains("银")) {
            return new BigDecimal("0.95");
        }
        return BigDecimal.ONE;
    }

    private CustomerAccount withBalance(CustomerAccount account, BigDecimal balance) {
        return new CustomerAccount(
                account.ownerId(),
                account.ownerName(),
                account.phoneMasked(),
                account.memberLevel(),
                account.accountStatus(),
                balance,
                account.createdAt()
        );
    }
}
