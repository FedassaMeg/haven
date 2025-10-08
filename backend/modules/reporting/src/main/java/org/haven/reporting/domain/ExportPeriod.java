package org.haven.reporting.domain;

import java.time.LocalDate;
import java.time.Month;

/**
 * Value object representing HUD reporting period
 * Operating year for CoC APR: Oct 1 - Sep 30
 */
public class ExportPeriod {
    private final LocalDate startDate;
    private final LocalDate endDate;

    public ExportPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Create CoC operating year period (Oct 1 - Sep 30)
     * @param year The calendar year in which October 1st falls
     */
    public static ExportPeriod cocOperatingYear(int year) {
        LocalDate start = LocalDate.of(year, Month.OCTOBER, 1);
        LocalDate end = LocalDate.of(year + 1, Month.SEPTEMBER, 30);
        return new ExportPeriod(start, end);
    }

    /**
     * Create calendar year period
     */
    public static ExportPeriod calendarYear(int year) {
        LocalDate start = LocalDate.of(year, Month.JANUARY, 1);
        LocalDate end = LocalDate.of(year, Month.DECEMBER, 31);
        return new ExportPeriod(start, end);
    }

    /**
     * Create custom period
     */
    public static ExportPeriod between(LocalDate start, LocalDate end) {
        return new ExportPeriod(start, end);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    @Override
    public String toString() {
        return startDate + " to " + endDate;
    }
}
