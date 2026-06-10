package com.parkvision.cps.domain.dispatch;

import java.time.LocalDateTime;

/**
 * A dispatch task with a real lifecycle: QUEUED → IN_PROGRESS → DONE.
 * Tasks are enqueued by business actions (entry / retrieval / VIP / pre-dispatch),
 * consumed by an AGV in the dispatch worker, advanced to completion, and persisted
 * the whole way — so the queue and AGV state reflect real backend progress instead
 * of a random simulation.
 *
 * <p>The legacy display fields ({@code type}, {@code tag}, {@code wait}) are kept so
 * existing API/UI consumers continue to work; the new fields are additive.
 */
public class DispatchTask {
    public static final String QUEUED = "QUEUED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String DONE = "DONE";

    private Long id;
    private final String plateNo;
    private final String type;
    private final String tag;
    private String wait;
    private final boolean vip;
    private String status;
    private int progress;
    private String slotId;
    private String agvId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DispatchTask(String plateNo, String type, String tag, String wait, boolean vip) {
        this(plateNo, type, tag, wait, vip, null);
    }

    public DispatchTask(String plateNo, String type, String tag, String wait, boolean vip, String slotId) {
        this(null, plateNo, type, tag, wait, vip, QUEUED, 0, slotId, null, LocalDateTime.now(), LocalDateTime.now());
    }

    public DispatchTask(Long id, String plateNo, String type, String tag, String wait, boolean vip,
                        String status, int progress, String slotId, String agvId,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.plateNo = plateNo;
        this.type = type;
        this.tag = tag;
        this.wait = wait;
        this.vip = vip;
        this.status = status == null ? QUEUED : status;
        this.progress = progress;
        this.slotId = slotId;
        this.agvId = agvId;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public String getType() {
        return type;
    }

    public String getTag() {
        return tag;
    }

    public String getWait() {
        return wait;
    }

    public void setWait(String wait) {
        this.wait = wait;
    }

    public boolean isVip() {
        return vip;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public String getAgvId() {
        return agvId;
    }

    public void setAgvId(String agvId) {
        this.agvId = agvId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return QUEUED.equals(status) || IN_PROGRESS.equals(status);
    }
}
