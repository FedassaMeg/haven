# Service Episode Migration Guide

## Overview

This document outlines the migration from CaseNote-centric service tracking to ServiceEpisode-based service delivery management.

## Key Changes

### Before (CaseNote-centric)
- Services tracked as simple text notes in cases
- No duration tracking
- No funding source association
- Limited billing capabilities
- No outcome tracking
- No standardized service types

### After (ServiceEpisode-based)
- Comprehensive service episode tracking
- Duration and billing management
- Multiple funding source support
- Standardized service types and categories
- Outcome tracking and follow-up management
- HMIS-compatible reporting

## New Components

### 1. Service Value Objects
- `ServiceType` - Comprehensive enumeration of service types
- `ServiceCategory` - Grouping for reporting and billing
- `ServiceDeliveryMode` - How services are delivered
- `FundingSource` - Funding source with compliance rules

### 2. ServiceEpisode Aggregate
- Complete service tracking from creation to completion
- Duration tracking (planned vs actual)
- Multiple provider support
- Funding source allocation
- Confidentiality management
- Court order tracking
- Document attachment
- Outcome tracking

### 3. Application Services
- `ServiceDeliveryAppService` - Main orchestration service
- `ServiceBillingService` - Billing calculation and validation
- `ServiceReportingService` - Statistics and compliance reporting

## Migration Strategy

### Phase 1: Parallel Implementation
1. Deploy new ServiceEpisode infrastructure
2. Keep existing CaseNote system operational
3. Start creating ServiceEpisodes for new services
4. Gradually migrate existing case notes

### Phase 2: Data Migration
1. Create migration scripts to convert CaseNotes to ServiceEpisodes
2. Map existing case note types to new ServiceTypes
3. Associate historical services with funding sources
4. Update ProgramEnrollment links

### Phase 3: UI Migration
1. Update service creation forms to use ServiceEpisode
2. Add duration tracking to service delivery workflow
3. Implement billing and outcome tracking interfaces
4. Add reporting dashboards

### Phase 4: Deprecation
1. Phase out CaseNote creation for services
2. Maintain read-only access to historical CaseNotes
3. Complete migration of all active cases
4. Archive legacy system

## Service Creation Workflow

### Old Workflow
```java
// Simple case note creation
caseService.addNote(caseId, "Provided counseling session", providerId);
```

### New Workflow
```java
// Comprehensive service episode creation
var cmd = new CreateServiceEpisodeCmd(
    clientId,
    enrollmentId,
    programId,
    "Counseling Program",
    ServiceType.INDIVIDUAL_COUNSELING,
    ServiceDeliveryMode.IN_PERSON,
    LocalDate.now(),
    50, // planned duration in minutes
    providerId,
    providerName,
    "CAL-OES", // funder ID
    "California Office of Emergency Services",
    "GRANT-2024-001",
    "Individual trauma counseling session",
    true, // confidential
    currentUserId
);

var episodeId = serviceDeliveryService.createServiceEpisode(cmd);

// Start service
serviceDeliveryService.startService(new StartServiceCmd(
    episodeId,
    LocalDateTime.now(),
    "Counseling Room A"
));

// Complete service
serviceDeliveryService.completeService(new CompleteServiceCmd(
    episodeId,
    LocalDateTime.now().plusMinutes(45),
    "Client made progress on coping strategies",
    ServiceCompletionStatus.COMPLETED,
    "Next session scheduled for next week"
));
```

## Quick Service Creation Methods

For common scenarios, use simplified creation methods:

```java
// Crisis intervention
var episodeId = serviceDeliveryService.createCrisisInterventionService(
    clientId, enrollmentId, programId, providerId, providerName, true, currentUserId
);

// Counseling session
var episodeId = serviceDeliveryService.createCounselingSession(
    clientId, enrollmentId, programId, ServiceType.TRAUMA_COUNSELING, 
    providerId, providerName, currentUserId
);

// Case management contact
var episodeId = serviceDeliveryService.createCaseManagementContact(
    clientId, enrollmentId, programId, ServiceDeliveryMode.PHONE,
    providerId, providerName, "Weekly check-in call", currentUserId
);
```

## Reporting Benefits

### Service Statistics
```java
var stats = serviceReportingService.generateStatistics(
    serviceDeliveryService.getServicesByDateRange(startDate, endDate)
);

// Access comprehensive metrics
stats.getTotalServices();
stats.getCompletionRate();
stats.getServiceTypeBreakdown();
stats.getFundingSourceBreakdown();
```

### Billing Reports
```java
var billingSummary = billingService.generateBillingSummary(
    fundingSource, startDate, endDate, billingRecords
);

// Get funding-specific billing information
billingSummary.totalAmount();
billingSummary.totalHours();
billingSummary.serviceBreakdown();
```

### Compliance Monitoring
```java
var complianceReport = reportingService.generateFunderCompliance(
    fundingSource, fundedServices
);

// Monitor compliance rates and issues
complianceReport.complianceRate();
complianceReport.complianceIssues();
complianceReport.outcomeTrackingRate();
```

## Database Schema Changes

### New Tables
- `service_episodes` - Main service episode data
- `service_episode_providers` - Additional providers
- `service_episode_funding` - Multiple funding sources
- `service_episode_documents` - Attached documents
- `billing_rates` - Configurable billing rates

### Updated Tables
- `program_enrollments` - Links to service episodes
- `case_records` - References to service episodes

## Configuration Requirements

### Funding Sources
Configure standard funding sources in application:
```yaml
funding:
  sources:
    - id: "HUD-COC"
      name: "HUD Continuum of Care"
      type: "FEDERAL"
      requires_outcome_tracking: true
      allows_confidential_services: false
    - id: "DOJ-VAWA"
      name: "DOJ Violence Against Women Act"
      type: "FEDERAL"
      requires_outcome_tracking: true
      allows_confidential_services: true
```

### Billing Rates
Configure billing rates by funding source and service type:
```yaml
billing:
  rates:
    federal:
      counseling: 75.00
      case_management: 50.00
      legal: 100.00
    state:
      counseling: 65.00
      case_management: 45.00
```

## Benefits

### For Staff
- Clearer service documentation
- Automatic billing calculation
- Follow-up tracking
- Compliance validation

### For Administration
- Accurate billing and invoicing
- Comprehensive reporting
- Funder compliance monitoring
- Program utilization analysis

### For Funders
- Standardized reporting
- Outcome tracking
- Service type analysis
- Billing transparency

## Migration Timeline

1. **Week 1-2**: Deploy infrastructure, begin parallel tracking
2. **Week 3-4**: Train staff on new service creation workflow
3. **Week 5-8**: Migrate historical data, update UI
4. **Week 9-10**: Complete migration, deprecate old system
5. **Week 11-12**: Monitoring and optimization

## Support and Training

- Technical documentation in `/docs/service-delivery/`
- Staff training materials in `/training/service-episodes/`
- API documentation at `/api/service-delivery/docs`
- Support contact: technical-support@haven.org