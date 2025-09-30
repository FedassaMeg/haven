package org.haven.api.reporting;

import org.haven.shared.vo.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class PitHicAuditService {

    private static final Logger log = LoggerFactory.getLogger(PitHicAuditService.class);

    public void auditReportGeneration(String reportType,
                                      LocalDate reportDate,
                                      String continuumCode,
                                      String organizationId,
                                      UserId requestedBy,
                                      String purpose) {
        log.info("Audit: Generated {} report on {} for continuum {} org {} by {} (purpose={})",
            reportType, reportDate, continuumCode, organizationId,
            requestedBy != null ? requestedBy.getValue() : "system",
            purpose);
    }

    public void auditReportExport(String reportType,
                                  UUID resourceId,
                                  String format,
                                  UserId requestedBy,
                                  String purpose) {
        log.info("Audit: Exported {} {} as {} by {} (purpose={})",
            reportType,
            resourceId,
            format,
            requestedBy != null ? requestedBy.getValue() : "system",
            purpose);
    }
}
