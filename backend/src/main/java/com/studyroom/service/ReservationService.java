package com.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.studyroom.dto.ReservationDTO;
import com.studyroom.entity.Reservation;
import java.util.List;

public interface ReservationService extends IService<Reservation> {
    Reservation create(Long userId, ReservationDTO dto);
    void cancel(Long reservationId, Long userId);
    List<Reservation> listByUser(Long userId);
    List<Reservation> listTodayActive();
}
