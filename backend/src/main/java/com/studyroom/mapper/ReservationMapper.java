package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.time.LocalTime;

@Mapper public interface ReservationMapper extends BaseMapper<Reservation> {
    @Select("SELECT COUNT(*) FROM reservation WHERE seat_id=#{seatId} AND reserve_date=#{date} AND status IN ('ACTIVE','CHECKED_IN') AND start_time<#{endTime} AND end_time>#{startTime}")
    int countConflict(Long seatId, LocalDate date, LocalTime startTime, LocalTime endTime);
}
