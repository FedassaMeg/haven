package org.haven.api.reporting;

import org.haven.reporting.domain.pithic.HicInventoryData;
import org.haven.reporting.domain.pithic.PitCensusData;

import java.util.List;

public record AnnualPitHicReport(int year,
                                 String continuumCode,
                                 String organizationId,
                                 List<PitCensusData> pitReports,
                                 List<HicInventoryData> hicReports) {
}
