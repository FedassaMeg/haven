package org.haven.programenrollment.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Value object representing the determination of chronic homelessness status
 * Based on HUD's definition of chronic homelessness
 */
public record ChronicallyHomelessDetermination(
    boolean isChronicallyHomeless,
    int monthsHomelessInThreeYears,
    int timesHomelessInThreeYears,
    boolean hasDisablingCondition,
    LocalDate determinationDate,
    String determinationReason,
    List<String> qualifyingFactors
) {
    
    public static ChronicallyHomelessDetermination notChronicallyHomeless(String reason) {
        return new ChronicallyHomelessDetermination(
            false,
            0,
            0,
            false,
            LocalDate.now(),
            reason,
            List.of()
        );
    }
    
    public static ChronicallyHomelessDetermination chronicallyHomeless(
            int monthsHomeless,
            int timesHomeless,
            boolean hasDisablingCondition,
            List<String> qualifyingFactors) {
        return new ChronicallyHomelessDetermination(
            true,
            monthsHomeless,
            timesHomeless,
            hasDisablingCondition,
            LocalDate.now(),
            "Meets HUD chronic homelessness definition",
            qualifyingFactors
        );
    }
}