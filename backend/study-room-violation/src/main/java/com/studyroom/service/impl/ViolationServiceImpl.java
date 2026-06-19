package com.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.config.RabbitMQConfig;
import com.studyroom.entity.*;
import com.studyroom.mapper.*;
import com.studyroom.service.ViolationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j @Service @RequiredArgsConstructor
public class ViolationServiceImpl extends ServiceImpl<ViolationMapper, Violation> implements ViolationService {
    private final ReservationMapper reservationMapper;
    private final AttendanceMapper attendanceMapper;
    private final com.studyroom.client.ReservationFeignClient reservationFeignClient;

    @Override
    @Scheduled(cron = "0 */15 * * * ?")
    public void autoDetect() {
        log.info("开始定时任务：扫描当天超过30分钟未签到违规...");
        try {
            LocalDate today = LocalDate.now();
            LocalTime nowTime = LocalTime.now();
            List<Reservation> activeReservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getStatus, "ACTIVE")
                    .le(Reservation::getReserveDate, today)
            );
            for (Reservation r : activeReservations) {
                boolean isExpired = false;
                if (r.getReserveDate().isBefore(today)) {
                    isExpired = true;
                } else if (r.getReserveDate().equals(today)) {
                    if (r.getStartTime().plusMinutes(30).isBefore(nowTime)) {
                        isExpired = true;
                    }
                }
                if (isExpired) {
                    Long reservationId = r.getId();
                    boolean hasViolation = lambdaQuery()
                        .eq(Violation::getReservationId, reservationId)
                        .eq(Violation::getViolationType, "NO_SHOW")
                        .exists();
                    if (!hasViolation) {
                        Violation v = new Violation();
                        v.setUserId(r.getUserId());
                        v.setReservationId(reservationId);
                        v.setViolationType("NO_SHOW");
                        v.setStatus("PENDING");
                        save(v);
                        log.info("定时扫描：成功判定违规并关闭，预约#{}", reservationId);
                    }
                    reservationFeignClient.updateReservationStatus(reservationId, "NO_SHOW");
                }
            }
        } catch (Exception e) {
            log.error("定时扫描未签到违规异常", e);
        }
    }

    /** 消费延迟消息：检查预约是否已签到 */
    @RabbitListener(queues = RabbitMQConfig.TARGET_QUEUE)
    public void handleViolationCheck(org.springframework.amqp.core.Message message) {
        try {
            String body = new String(message.getBody());
            Map<String, Object> data = new ObjectMapper().readValue(body, Map.class);
            Long reservationId = Long.valueOf(data.get("reservationId").toString());
            Reservation r = reservationFeignClient.getReservationById(reservationId);
            if (r == null || !"ACTIVE".equals(r.getStatus())) return;

            // 未签到 → 自动取消 + 记录违规
            if (!lambdaQuery().eq(Violation::getReservationId, reservationId).eq(Violation::getViolationType, "NO_SHOW").exists()) {
                Violation v = new Violation(); v.setUserId(r.getUserId()); v.setReservationId(reservationId);
                v.setViolationType("NO_SHOW"); v.setStatus("PENDING"); save(v);
                log.info("自动记录违规: 预约#{} 未签到", reservationId);
            }
            reservationFeignClient.updateReservationStatus(r.getId(), "NO_SHOW");
        } catch (Exception e) {
            log.error("处理违规检查消息失败", e);
        }
    }

    @Override
    public void process(Long violationId, String status, String remark) {
        Violation v = getById(violationId);
        v.setStatus(status);
        v.setProcessRemark(remark);
        updateById(v);

        // 级联更新预约记录状态，以保证业务逻辑和用户体验的一致性
        Reservation r = reservationFeignClient.getReservationById(v.getReservationId());
        if (r != null) {
            if ("DISMISSED".equals(status)) {
                // 驳回违约：免除处罚，把预约状态同步改写为已取消（CANCELLED）
                reservationFeignClient.updateReservationStatus(v.getReservationId(), "CANCELLED");
            } else if ("PROCESSED".equals(status)) {
                // 确认违约：保留/变更为未签到（NO_SHOW）状态
                reservationFeignClient.updateReservationStatus(v.getReservationId(), "NO_SHOW");
            }
        }
    }
}