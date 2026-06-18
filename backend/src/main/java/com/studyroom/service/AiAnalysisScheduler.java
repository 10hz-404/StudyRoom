package com.studyroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.entity.AiAnalysisLog;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.Violation;
import com.studyroom.mapper.AiAnalysisLogMapper;
import com.studyroom.mapper.ReservationMapper;
import com.studyroom.mapper.ViolationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisScheduler {
    private final ViolationMapper violationMapper;
    private final ReservationMapper reservationMapper;
    private final DeepSeekService deepSeekService;
    private final AiAnalysisLogMapper aiLogMapper;

    /**
     * 每晚 23:00 自动批量执行考勤异常分析
     * 限制每批最大处理 100 条记录
     */
    @Scheduled(cron = "0 0 23 * * ?")
    public void batchAnalyzeAttendanceAnomalies() {
        log.info("开始执行考勤异常定时批量 AI 分析任务...");
        try {
            // 1. 查询待处理违规记录 (status = 'PENDING')，最多100条
            List<Violation> violations = violationMapper.selectList(
                new LambdaQueryWrapper<Violation>()
                    .eq(Violation::getStatus, "PENDING")
                    .orderByDesc(Violation::getId)
                    .last("LIMIT 100")
            );

            if (violations.isEmpty()) {
                log.info("未发现待处理的违规考勤记录，分析任务结束。");
                return;
            }

            int count = 0;
            for (Violation violation : violations) {
                Long resvId = violation.getReservationId();
                // 2. 检查是否已经对该预约分析过，避免重复调用 DeepSeek
                Long logCount = aiLogMapper.selectCount(
                    new LambdaQueryWrapper<AiAnalysisLog>()
                        .eq(AiAnalysisLog::getBizType, "ANALYSIS")
                        .eq(AiAnalysisLog::getUserId, violation.getUserId())
                        .like(AiAnalysisLog::getInputSnapshot, String.valueOf(resvId))
                );
                if (logCount > 0) {
                    continue; // 已经有分析记录了，跳过
                }

                // 3. 构建异常分析上下文
                Reservation resv = reservationMapper.selectById(resvId);
                if (resv == null) {
                    continue;
                }

                Map<String, Object> context = new HashMap<>();
                context.put("reservationId", resvId);
                context.put("violationType", violation.getViolationType());
                context.put("reserveDate", resv.getReserveDate());
                context.put("startTime", resv.getStartTime());
                context.put("endTime", resv.getEndTime());
                context.put("violationTime", violation.getCreateTime());

                // 4. 调用 AI 异常分析（内部会自动记录限额、熔断、并保存结果到 ai_analysis_log 中）
                try {
                    deepSeekService.analyzeAnomaly(violation.getUserId(), resvId, context);
                    count++;
                } catch (Exception e) {
                    log.error("定时分析单条违规记录 {} 失败: {}", violation.getId(), e.getMessage());
                }
            }
            log.info("考勤异常定时批量 AI 分析任务完成，成功分析 {} 条违规记录。", count);
        } catch (Exception e) {
            log.error("执行考勤异常定时批量 AI 分析任务异常: ", e);
        }
    }
}
