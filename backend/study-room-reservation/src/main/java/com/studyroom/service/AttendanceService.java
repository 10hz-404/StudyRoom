package com.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.studyroom.entity.Attendance;

public interface AttendanceService extends IService<Attendance> {
    Attendance checkIn(Long reservationId, Long userId);
    Attendance checkOut(Long reservationId, Long userId);
    Attendance getByReservationId(Long reservationId);
}
