package com.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.studyroom.entity.Violation;

public interface ViolationService extends IService<Violation> {
    void autoDetect();
    void process(Long violationId, String status, String remark);
}
