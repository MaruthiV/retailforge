package com.retailforge.inventory.repo;

import com.retailforge.inventory.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
    List<Reservation> findByTransactionIdAndStatus(String transactionId, Reservation.Status status);
    List<Reservation> findByTransactionId(String transactionId);
}
