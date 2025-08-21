package org.haven.clientprofile.domain;

import java.time.LocalTime;

public record ContactSafetyPrefs(
    boolean okToText,
    boolean okToVoicemail,
    String codeWord,
    QuietHours quietHours
) {
    
    public record QuietHours(
        LocalTime startTime,
        LocalTime endTime
    ) {
        public boolean isQuietTime(LocalTime time) {
            if (startTime == null || endTime == null) {
                return false;
            }
            
            if (startTime.isBefore(endTime)) {
                return !time.isBefore(startTime) && !time.isAfter(endTime);
            } else {
                return !time.isBefore(startTime) || !time.isAfter(endTime);
            }
        }
    }
    
    public static ContactSafetyPrefs defaultPrefs() {
        return new ContactSafetyPrefs(false, false, null, null);
    }
    
    public static ContactSafetyPrefs safe() {
        return new ContactSafetyPrefs(
            false, 
            false, 
            null, 
            new QuietHours(LocalTime.of(20, 0), LocalTime.of(8, 0))
        );
    }
}