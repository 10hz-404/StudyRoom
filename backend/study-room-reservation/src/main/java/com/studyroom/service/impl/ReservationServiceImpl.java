package com.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.config.RabbitMQConfig;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.dto.ReservationDTO;
import com.studyroom.entity.*;
import com.studyroom.mapper.*;
import com.studyroom.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {
    private final com.studyroom.client.RoomFeignClient roomFeignClient;
    private final AttendanceMapper attendanceMapper;
    private final RabbitTemplate rabbitTemplate;
    private static final int MAX_ADVANCE_DAYS = 7;
    private static final int MAX_DAILY_RESERVATIONS = 3;
    private static final int CANCEL_DEADLINE_MINUTES = 30;

    @Override @Transactional
    public Reservation create(Long userId, ReservationDTO dto) {
        if (dto.getReserveDate().isBefore(LocalDate.now())) throw new BusinessException(400, "不能预约过去的日期");
        if (dto.getReserveDate().equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            LocalTime earliestAllowedTime = now.getMinute() < 30 
                ? now.withMinute(0).withSecond(0).withNano(0) 
                : now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
            if (dto.getStartTime().isBefore(earliestAllowedTime)) {
                throw new BusinessException(400, "不能预约已经过去的时间");
            }
        }
        if (dto.getReserveDate().isAfter(LocalDate.now().plusDays(MAX_ADVANCE_DAYS))) throw new BusinessException(400, "只能预约未来7天内的日期");
        if (dto.getReserveDate().equals(LocalDate.now().plusDays(1)) && LocalTime.now().isAfter(LocalTime.of(21, 0))) throw new BusinessException(400, "21:00后不能预约明天的座位");
        if (!dto.getEndTime().isAfter(dto.getStartTime())) throw new BusinessException(400, "结束时间必须晚于开始时间");
        long minutes = Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
        if (minutes > 240) throw new BusinessException(400, "单次预约不能超过4小时");
        if (minutes < 30) throw new BusinessException(400, "单次预约不能少于30分钟");
        Seat seat = roomFeignClient.getSeatById(dto.getSeatId());
        if (seat == null) throw new BusinessException(404, "座位不存在");
        if (!"AVAILABLE".equals(seat.getStatus())) throw new BusinessException(400, "该座位当前不可预约");
        long todayCount = count(new LambdaQueryWrapper<Reservation>().eq(Reservation::getUserId, userId).eq(Reservation::getReserveDate, dto.getReserveDate()).in(Reservation::getStatus, "ACTIVE", "CHECKED_IN"));
        if (todayCount >= MAX_DAILY_RESERVATIONS) throw new BusinessException(400, "每天最多预约3次");
        List<Reservation> userReservations = list(new LambdaQueryWrapper<Reservation>().eq(Reservation::getUserId, userId).eq(Reservation::getReserveDate, dto.getReserveDate()).in(Reservation::getStatus, "ACTIVE", "CHECKED_IN"));
        for (Reservation existing : userReservations) {
            if (existing.getStartTime().isBefore(dto.getEndTime()) && existing.getEndTime().isAfter(dto.getStartTime())) throw new BusinessException(409, "您在该时段已有其他预约");
        }
        int conflict = baseMapper.countConflict(dto.getSeatId(), dto.getReserveDate(), dto.getStartTime(), dto.getEndTime());
        if (conflict > 0) throw new BusinessException(409, "该时段已被其他人预约");
        Reservation r = new Reservation();
        r.setUserId(userId); r.setSeatId(dto.getSeatId()); r.setReserveDate(dto.getReserveDate()); r.setStartTime(dto.getStartTime()); r.setEndTime(dto.getEndTime()); r.setStatus("ACTIVE"); save(r);
        sendViolationCheckMessage(r);
        Attendance att = new Attendance(); att.setReservationId(r.getId()); att.setStatus("NOT_CHECKED_IN"); attendanceMapper.insert(att);
        return r;
    }

    @Override @Transactional
    public void cancel(Long reservationId, Long userId) {
        Reservation r = getById(reservationId);
        if (r == null) throw new BusinessException(404, "预约不存在");
        if (!r.getUserId().equals(userId)) throw new BusinessException(403, "只能取消自己的预约");
        if (!"ACTIVE".equals(r.getStatus())) throw new BusinessException(400, "只能取消待签到状态的预约");
        LocalDateTime reservationStart = LocalDateTime.of(r.getReserveDate(), r.getStartTime());
        if (reservationStart.isAfter(LocalDateTime.now()) && LocalDateTime.now().plusMinutes(CANCEL_DEADLINE_MINUTES).isAfter(reservationStart))
            throw new BusinessException(400, "距开始不足30分钟，无法取消");
        r.setStatus("CANCELLED"); updateById(r);
    }

    private void sendViolationCheckMessage(Reservation r) {
        try {
            LocalDateTime checkTime = LocalDateTime.of(r.getReserveDate(), r.getStartTime()).plusMinutes(30);
            LocalDateTime endTime = LocalDateTime.of(r.getReserveDate(), r.getEndTime());
            if (checkTime.isAfter(endTime)) {
                checkTime = endTime;
            }
            long delayMs = Duration.between(LocalDateTime.now(), checkTime).toMillis();
            if (delayMs <= 0) {
                delayMs = 1000L; // 已经超时，设定 1 秒延迟让消费者立刻触发判定并关闭
            }
            String msg = new ObjectMapper().writeValueAsString(Map.of("reservationId", r.getId(), "userId", r.getUserId()));
            long finalDelayMs = delayMs;
            rabbitTemplate.convertAndSend(RabbitMQConfig.DELAY_QUEUE, (Object) msg, m -> {
                m.getMessageProperties().setExpiration(String.valueOf(finalDelayMs));
                return m;
            });
        } catch (Exception ignored) {}
    }

    @Override public List<Reservation> listByUser(Long userId) { return list(new LambdaQueryWrapper<Reservation>().eq(Reservation::getUserId, userId).orderByDesc(Reservation::getCreateTime)); }
    @Override public List<Reservation> listTodayActive() { return list(new LambdaQueryWrapper<Reservation>().eq(Reservation::getReserveDate, LocalDate.now()).in(Reservation::getStatus, "ACTIVE", "CHECKED_IN")); }
}