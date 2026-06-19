package com.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.entity.*;
import com.studyroom.mapper.*;
import com.studyroom.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class AttendanceServiceImpl extends ServiceImpl<AttendanceMapper, Attendance> implements AttendanceService {
    private final ReservationMapper reservationMapper;
    private static final int CHECKIN_WINDOW_MINUTES = 30;

    @Override @Transactional
    public Attendance checkIn(Long reservationId, Long userId) {
        Attendance att = getByReservationId(reservationId);
        if (att == null) throw new BusinessException("考勤记录不存在");
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null || !r.getUserId().equals(userId)) throw new BusinessException(403, "只能签到自己的预约");
        if (!"NOT_CHECKED_IN".equals(att.getStatus())) throw new BusinessException("已签到或已签退，不可重复操作");
        if (!"ACTIVE".equals(r.getStatus())) throw new BusinessException("预约状态异常，不可签到");
        // 签到窗口：开始时间前后30分钟
        LocalDateTime reservationStart = LocalDateTime.of(r.getReserveDate(), r.getStartTime());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(reservationStart.minusMinutes(CHECKIN_WINDOW_MINUTES)))
            throw new BusinessException("预约开始前30分钟才能签到");
        if (now.isAfter(reservationStart.plusMinutes(CHECKIN_WINDOW_MINUTES)))
            throw new BusinessException("已超过签到时间，无法签到");
        att.setCheckInTime(now); att.setStatus("CHECKED_IN");
        r.setStatus("CHECKED_IN"); updateById(att); reservationMapper.updateById(r);
        return att;
    }

    @Override @Transactional
    public Attendance checkOut(Long reservationId, Long userId) {
        Attendance att = getByReservationId(reservationId);
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null || !r.getUserId().equals(userId)) throw new BusinessException(403, "只能签退自己的预约");
        if (!"CHECKED_IN".equals(att.getStatus())) throw new BusinessException("未签到，无法签退");
        att.setCheckOutTime(LocalDateTime.now()); att.setStatus("CHECKED_OUT");
        r.setStatus("COMPLETED"); updateById(att); reservationMapper.updateById(r);
        return att;
    }

    @Override
    public Attendance getByReservationId(Long reservationId) {
        return getOne(new LambdaQueryWrapper<Attendance>().eq(Attendance::getReservationId, reservationId));
    }
}