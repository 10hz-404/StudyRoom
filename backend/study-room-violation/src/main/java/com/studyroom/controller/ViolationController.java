package com.studyroom.controller;

import com.studyroom.common.response.R;
import com.studyroom.dto.ViolationProcessDTO;
import com.studyroom.entity.Violation;
import com.studyroom.service.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/violations") @RequiredArgsConstructor @Tag(name = "违规")
public class ViolationController {
    private final ViolationService violationService;

    @GetMapping @Operation(summary = "违规列表")
    public R<List<Violation>> list() { return R.ok(violationService.list()); }

    @PutMapping("/{id}/process") @Operation(summary = "处理违规")
    public R<?> process(@PathVariable Long id, @RequestBody ViolationProcessDTO dto) {
        violationService.process(id, dto.getStatus(), dto.getProcessRemark()); return R.ok();
    }

    @PostMapping("/detect") @Operation(summary = "手动触发违规扫描")
    public R<?> detect() { violationService.autoDetect(); return R.ok(); }
}
