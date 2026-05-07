package com.parkvision.cps.domain.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ParkingOrder {
    private final String orderNo;
    private final String plateNo;
    private final String slotId;
    private final LocalDateTime entryTime;
    private OrderStatus status;
    private BigDecimal amount;

    public ParkingOrder(String orderNo, String plateNo, String slotId, LocalDateTime entryTime, OrderStatus status, BigDecimal amount) {
        this.orderNo = orderNo;
        this.plateNo = plateNo;
        this.slotId = slotId;
        this.entryTime = entryTime;
        this.status = status;
        this.amount = amount;
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
}
