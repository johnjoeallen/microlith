package com.acme.inventory.internal;

import com.acme.inventory.api.ReservationStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservation")
class ReservationEntity {

    @Id
    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_line", joinColumns = @JoinColumn(name = "reservation_id"))
    private List<ReservedLineEmbeddable> lines = new ArrayList<>();

    protected ReservationEntity() {
    }

    ReservationEntity(String reservationId, String requestId, String orderId, List<ReservedLineEmbeddable> lines) {
        this.reservationId = reservationId;
        this.requestId = requestId;
        this.orderId = orderId;
        this.status = ReservationStatus.RESERVED;
        this.lines = new ArrayList<>(lines);
    }

    String getReservationId() {
        return reservationId;
    }

    String getOrderId() {
        return orderId;
    }

    ReservationStatus getStatus() {
        return status;
    }

    void setStatus(ReservationStatus status) {
        this.status = status;
    }

    List<ReservedLineEmbeddable> getLines() {
        return lines;
    }
}
