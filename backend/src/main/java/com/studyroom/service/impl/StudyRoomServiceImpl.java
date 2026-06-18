package com.studyroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.studyroom.entity.StudyRoom; import com.studyroom.mapper.StudyRoomMapper;
import com.studyroom.service.StudyRoomService;
import org.springframework.stereotype.Service;

@Service public class StudyRoomServiceImpl extends ServiceImpl<StudyRoomMapper, StudyRoom> implements StudyRoomService {}
