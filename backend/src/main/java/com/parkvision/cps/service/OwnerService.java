package com.parkvision.cps.service;

import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.domain.auth.AppUser;
import com.parkvision.cps.domain.billing.PaymentTransaction;
import com.parkvision.cps.domain.customer.CustomerAccount;
import com.parkvision.cps.domain.customer.VehicleProfile;
import com.parkvision.cps.domain.order.OrderStatus;
import com.parkvision.cps.domain.order.ParkingOrder;
import com.parkvision.cps.dto.owner.OrderBill;
import com.parkvision.cps.dto.owner.OwnerProfile;
import com.parkvision.cps.dto.owner.OwnerWallet;
import com.parkvision.cps.repository.AuthUserRepository;
import com.parkvision.cps.repository.ParkVisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Owner-scoped read/write access. Every operation is bound to the signed-in
 * owner: they can only see and act on their own vehicles and orders.
 */
@Service
public class OwnerService {
    private static final BigDecimal MAX_RECHARGE = new BigDecimal("100000");

    private final AuthUserRepository authUserRepository;
    private final ParkVisionRepository repository;
    private final OrderService orderService;
    private final BillingService billingService;

    public OwnerService(AuthUserRepository authUserRepository, ParkVisionRepository repository,
                        OrderService orderService, BillingService billingService) {
        this.authUserRepository = authUserRepository;
        this.repository = repository;
        this.orderService = orderService;
        this.billingService = billingService;
    }

    public OwnerProfile profile(String username) {
        AppUser user = requireUser(username);
        String ownerId = user.ownerId();
        CustomerAccount account = ownerId == null ? null : repository.findCustomerAccounts().stream()
                .filter(item -> item.ownerId().equalsIgnoreCase(ownerId))
                .findFirst()
                .orElse(null);
        List<VehicleProfile> vehicles = vehiclesFor(ownerId);
        Set<String> plates = plateSet(vehicles);
        int activeOrders = (int) repository.findOrders().stream()
                .filter(order -> plates.contains(order.getPlateNo()) && order.getStatus() != OrderStatus.FINISHED)
                .count();

        return new OwnerProfile(
                user.username(),
                user.displayName(),
                ownerId,
                account == null ? user.displayName() : account.ownerName(),
                account == null ? "—" : account.phoneMasked(),
                account == null ? "STANDARD" : account.memberLevel(),
                account == null ? "ACTIVE" : account.accountStatus(),
                account == null ? BigDecimal.ZERO : account.balance(),
                vehicles.size(),
                activeOrders,
                vehicles.stream().map(VehicleProfile::plateNo).toList()
        );
    }

    public List<VehicleProfile> vehicles(String username) {
        return vehiclesFor(requireUser(username).ownerId());
    }

    public List<ParkingOrder> orders(String username) {
        Set<String> plates = plateSet(vehiclesFor(requireUser(username).ownerId()));
        return repository.findOrders().stream()
                .filter(order -> plates.contains(order.getPlateNo()))
                .sorted(Comparator.comparing(ParkingOrder::getEntryTime).reversed())
                .toList();
    }

    public ParkingOrder changeOwnOrder(String username, String orderNo, OrderStatus status) {
        requireOwnedOrder(username, orderNo);
        return orderService.changeStatus(orderNo, status);
    }

    public ParkingOrder entry(String username, String plateNo) {
        String plate = plateNo == null ? "" : plateNo.trim().toUpperCase();
        Set<String> plates = plateSet(vehiclesFor(requireUser(username).ownerId()));
        if (!plates.contains(plate)) {
            throw new BusinessException("PLATE_NOT_OWNED", "该车牌不属于当前账号");
        }
        return orderService.entryForPlate(plate);
    }

    public OwnerWallet wallet(String username) {
        AppUser user = requireUser(username);
        CustomerAccount account = requireAccount(user);
        Set<String> scope = new HashSet<>(plateSet(vehiclesFor(account.ownerId())));
        scope.add(account.ownerId());
        List<PaymentTransaction> transactions = repository.findPaymentTransactions().stream()
                .filter(tx -> scope.contains(tx.plateNo()))
                .sorted(Comparator.comparing(PaymentTransaction::paidAt).reversed())
                .limit(30)
                .toList();
        return new OwnerWallet(
                account.ownerId(),
                account.memberLevel(),
                account.accountStatus(),
                account.balance() == null ? BigDecimal.ZERO : account.balance(),
                billingService.memberDiscountRate(account),
                transactions
        );
    }

    @Transactional
    public OwnerWallet recharge(String username, BigDecimal amount) {
        AppUser user = requireUser(username);
        CustomerAccount account = requireAccount(user);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "充值金额必须大于 0");
        }
        if (amount.compareTo(MAX_RECHARGE) > 0) {
            throw new BusinessException("AMOUNT_TOO_LARGE", "单次充值不能超过 " + MAX_RECHARGE + " 元");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal balance = account.balance() == null ? BigDecimal.ZERO : account.balance();
        BigDecimal newBalance = balance.add(normalized).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();
        repository.saveCustomerAccount(new CustomerAccount(
                account.ownerId(), account.ownerName(), account.phoneMasked(), account.memberLevel(),
                account.accountStatus(), newBalance, account.createdAt()
        ));
        repository.savePaymentTransaction(new PaymentTransaction(
                "RCG-" + System.currentTimeMillis(),
                "RECHARGE",
                account.ownerId(),
                normalized,
                "RECHARGE",
                "SUCCESS",
                now
        ));
        return wallet(username);
    }

    public OrderBill bill(String username, String orderNo) {
        ParkingOrder order = requireOwnedOrder(username, orderNo);
        PaymentTransaction payment = repository.findPaymentByOrderNo(orderNo).orElse(null);
        return new OrderBill(
                order.getOrderNo(),
                order.getPlateNo(),
                order.getAmount(),
                order.getDiscountAmount(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                repository.findBillingComponentsByOrderNo(orderNo),
                payment
        );
    }

    private CustomerAccount requireAccount(AppUser user) {
        String ownerId = user.ownerId();
        if (ownerId == null || ownerId.isBlank()) {
            throw new BusinessException("NO_ACCOUNT", "当前账号未关联客户档案");
        }
        return repository.findCustomerAccounts().stream()
                .filter(item -> item.ownerId().equalsIgnoreCase(ownerId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("NO_ACCOUNT", "未找到客户账户: " + ownerId));
    }

    private ParkingOrder requireOwnedOrder(String username, String orderNo) {
        Set<String> plates = plateSet(vehiclesFor(requireUser(username).ownerId()));
        ParkingOrder order = repository.findOrderByNo(orderNo)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在: " + orderNo));
        if (!plates.contains(order.getPlateNo())) {
            throw new BusinessException("ORDER_NOT_OWNED", "无法操作他人的订单");
        }
        return order;
    }

    private List<VehicleProfile> vehiclesFor(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            return List.of();
        }
        return repository.findVehicleProfiles().stream()
                .filter(vehicle -> ownerId.equalsIgnoreCase(vehicle.ownerId()))
                .toList();
    }

    private Set<String> plateSet(List<VehicleProfile> vehicles) {
        return vehicles.stream().map(VehicleProfile::plateNo).collect(Collectors.toSet());
    }

    private AppUser requireUser(String username) {
        return authUserRepository.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new BusinessException("AUTH_REQUIRED", "请先登录"));
    }
}
