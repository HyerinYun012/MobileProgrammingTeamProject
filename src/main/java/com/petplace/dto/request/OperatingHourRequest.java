package com.petplace.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.petplace.entity.OperatingHour;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalTime;

@Data
@Schema(description = "요일별 영업시간 정보 요청 DTO")
public class OperatingHourRequest {

    @Schema(description = "요일 (MON, TUE, WED, THU, FRI, SAT, SUN)", example = "MON")
    private OperatingHour.DayOfWeek dayOfWeek;

    @Schema(description = "오픈 시간", example = "09:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime;

    @Schema(description = "마감 시간", example = "21:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime;

    @Schema(description = "정기 휴무 여부", example = "false")
    private boolean isRegularHoliday;

    public OperatingHour toEntity() {
        return new OperatingHour(dayOfWeek, openTime, closeTime, isRegularHoliday);
    }
}