package com.studyroom.client;

import com.studyroom.entity.Reservation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "study-room-reservation")
public interface ReservationFeignClient {
    @GetMapping("/api/v1/reservations/internal/{id}")
    Reservation getReservationById(@PathVariable("id") Long id);

    @PutMapping("/api/v1/reservations/internal/{id}/status")
    boolean updateReservationStatus(@PathVariable("id") Long id, @RequestParam("status") String status);
}
