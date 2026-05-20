package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OperatingHour {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DayOfWeek dayOfWeek; // MON, TUE, WED, THU, FRI, SAT, SUN

    private LocalTime openTime;
    private LocalTime closeTime;

    @Column(nullable = false)
    private boolean isRegularHoliday; // 정기 휴무 여부

    public enum DayOfWeek {
        MON, TUE, WED, THU, FRI, SAT, SUN
    }
}