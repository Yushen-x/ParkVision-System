package com.parkvision.cps.domain.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ParkingOrder {
    private final String orderNo;
    private final String plateNo;
    private final String slotId;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private OrderStatus status;
    private BigDecimal amount;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private Integer durationMinutes;
    private BigDecimal discountAmount;

    public ParkingOrder(String orderNo, String plateNo, String slotId, LocalDateTime entryTime, OrderStatus status, BigDecimal amount) {
        this(
                orderNo,
                plateNo,
                slotId,
                entryTime,
                null,
                status,
                amount,
                status == OrderStatus.FINISHED ? "PAID" : "UNPAID",
                status == OrderStatus.FINISHED ? "AUTO_SETTLEMENT" : null,
                null,
                null,
                BigDecimal.ZERO
        );
    }

    public ParkingOrder(
            String orderNo,
            String plateNo,
            String slotId,
            LocalDateTime entryTime,
            LocalDateTime exitTime,
            OrderStatus status,
            BigDecimal amount,
            String paymentStatus,
            String paymentMethod,
            LocalDateTime paidAt,
            Integer durationMinutes,
            BigDecimal discountAmount
    ) {
        this.orderNo = orderNo;
        this.plateNo = plateNo;
        this.slotId = slotId;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.status = status;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.paymentStatus = paymentStatus == null ? "UNPAID" : paymentStatus;
        this.paymentMethod = paymentMethod;
        this.paidAt = paidAt;
        this.durationMinutes = durationMinutes;
        this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public String getSlotId() {
        return slotId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
}
