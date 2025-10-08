# HMIS CSV Field Mapping Inventory

**Version:** HUD FY2024 v1.0 (Effective October 1, 2024)
**Last Updated:** 2025-10-07
**Status:** Reconciliation Complete

## Executive Summary

This document inventories existing Haven domain field mappings against HUD HMIS CSV FY2024 specifications for IncomeBenefits.csv, HealthAndDV.csv, and Disabilities.csv files.

## Inventory Status by CSV File

| CSV File | Current Status | Fields Mapped | Fields Missing | Compliance % |
|----------|---------------|---------------|----------------|--------------|
| IncomeBenefits.csv | **Partial** | 15/45 | 30 | 33% |
| HealthAndDV.csv | **Partial** | 12/30 | 18 | 40% |
| Disabilities.csv | **Good** | 8/16 | 8 | 50% |

---

## 1. IncomeBenefits.csv Mapping

### 1.1 Current Implementation

**Domain Model:** `org.haven.programenrollment.domain.IncomeBenefitsRecord`
**Projection:** `org.haven.reporting.domain.hmis.HmisIncomeBenefitsProjection`

#### Mapped Fields (15/45)

| Field Name | Haven Field | HUD Element | Data Type | Status | Notes |
|------------|-------------|-------------|-----------|--------|-------|
| IncomeBenefitsID | recordId | 4.02.ID | String | ✅ Mapped | Generated as `IB_{enrollmentId}` |
| EnrollmentID | enrollmentId | - | String | ✅ Mapped | Foreign key |
| PersonalID | clientId | - | String | ✅ Mapped | Foreign key |
| InformationDate | informationDate | 4.02.1 | Date | ✅ Mapped | |
| TotalMonthlyIncome | totalMonthlyIncome | 4.02.1A | Integer | ✅ Mapped | |
| IncomeFromAnySource | incomeFromAnySource | 4.02.1 | Integer | ⚠️ **Missing from projection** | Only in domain model |
| EarnedIncome | earnedIncome | 4.02.2 | Integer | ✅ Mapped | DisabilityType enum |
| EarnedIncomeAmount | earnedIncomeAmount | 4.02.2A | Integer | ✅ Mapped | |
| UnemploymentIncome | unemploymentIncome | 4.02.3 | Integer | ✅ Mapped | |
| UnemploymentIncomeAmount | unemploymentIncomeAmount | 4.02.3A | Integer | ✅ Mapped | |
| SSI | supplementalSecurityIncome | 4.02.4 | Integer | ✅ Mapped | |
| SSIAmount | supplementalSecurityIncomeAmount | 4.02.4A | Integer | ✅ Mapped | |
| SSDI | socialSecurityDisabilityIncome | 4.02.5 | Integer | ✅ Mapped | |
| SSDIAmount | socialSecurityDisabilityIncomeAmount | 4.02.5A | Integer | ✅ Mapped | |
| VADisabilityService | vaDisabilityServiceConnected | 4.02.6 | Integer | ✅ Mapped | |
| VADisabilityServiceAmount | vaDisabilityServiceConnectedAmount | 4.02.6A | Integer | ✅ Mapped | |
| VADisabilityNonService | vaDisabilityNonServiceConnected | 4.02.7 | Integer | ✅ Mapped | |
| VADisabilityNonServiceAmount | vaDisabilityNonServiceConnectedAmount | 4.02.7A | Integer | ✅ Mapped | |
| PrivateDisability | privateDisabilityIncome | 4.02.8 | Integer | ✅ Mapped | |
| PrivateDisabilityAmount | privateDisabilityIncomeAmount | 4.02.8A | Integer | ✅ Mapped | |
| WorkersComp | workersCompensation | 4.02.9 | Integer | ✅ Mapped | |
| WorkersCompAmount | workersCompensationAmount | 4.02.9A | Integer | ✅ Mapped | |
| TANF | tanfIncome | 4.02.10 | Integer | ✅ Mapped | |
| TANFAmount | tanfIncomeAmount | 4.02.10A | Integer | ✅ Mapped | |
| GA | generalAssistance | 4.02.11 | Integer | ✅ Mapped | |
| GAAmount | generalAssistanceAmount | 4.02.11A | Integer | ✅ Mapped | |
| SocSecRetirement | socialSecurityRetirement | 4.02.12 | Integer | ✅ Mapped | |
| SocSecRetirementAmount | socialSecurityRetirementAmount | 4.02.12A | Integer | ✅ Mapped | |
| Pension | pensionFromFormerJob | 4.02.13 | Integer | ✅ Mapped | |
| PensionAmount | pensionFromFormerJobAmount | 4.02.13A | Integer | ✅ Mapped | |
| ChildSupport | childSupport | 4.02.14 | Integer | ✅ Mapped | |
| ChildSupportAmount | childSupportAmount | 4.02.14A | Integer | ✅ Mapped | |
| Alimony | alimony | 4.02.15 | Integer | ✅ Mapped | |
| AlimonyAmount | alimonyAmount | 4.02.15A | Integer | ✅ Mapped | |
| OtherIncomeSource | otherIncomeSource | 4.02.16 | Integer | ✅ Mapped | |
| OtherIncomeAmount | otherIncomeAmount | 4.02.16A | Integer | ✅ Mapped | |
| OtherIncomeSourceIdentify | otherIncomeSourceIdentify | 4.02.16B | String | ⚠️ **Missing from projection** | Free text field |
| SNAP | snap | 4.03.2 | Integer | ✅ Mapped | HmisFivePointResponse → DisabilityType conversion needed |
| WIC | wic | 4.03.3 | Integer | ✅ Mapped | HmisFivePointResponse → DisabilityType conversion needed |
| TANFChildCare | tanfChildCare | 4.03.4 | Integer | ✅ Mapped | HmisFivePointResponse → DisabilityType conversion needed |
| TANFTransportation | tanfTransportation | 4.03.5 | Integer | ✅ Mapped | HmisFivePointResponse → DisabilityType conversion needed |
| OtherTANF | otherTanf | 4.03.6 | Integer | ✅ Mapped | HmisFivePointResponse → DisabilityType conversion needed |
| OtherBenefitsSource | otherBenefitsSource | 4.03.7 | Integer | ✅ Mapped | HmisFivePointResponse → DisabilityType conversion needed |
| OtherBenefitsSourceIdentify | otherBenefitsSpecify | 4.03.7A | String | ⚠️ **Missing from projection** | Free text field |

#### Missing Fields (30)

**Critical Missing:**
1. `IncomeFromAnySource` (4.02.1) - HmisFivePointResponse → Integer mapping needed
2. `BenefitsFromAnySource` (4.03.1) - HmisFivePointResponse → Integer mapping needed
3. `InsuranceFromAnySource` (4.04.1) - Not in domain model
4. `DataCollectionStage` - Required audit field
5. `InformationDate` type conversion (domain uses LocalDate vs InformationDate enum)

**Type Mismatches:**
- Benefits fields use `HmisFivePointResponse` in domain but projection expects `DisabilityType`
- Need converter: `HmisFivePointResponse` (1=Yes, 0=No, 8=Client doesn't know, 9=Client refused, 99=Data not collected) → `DisabilityType`

### 1.2 Value Domain Conversions

#### HmisFivePointResponse → DisabilityType Mapping

```java
HmisFivePointResponse → DisabilityType
YES (value: 1) → YES (hmisValue: 1)
NO (value: 0) → NO (hmisValue: 0)
CLIENT_DOESNT_KNOW (value: 8) → CLIENT_DOESNT_KNOW (hmisValue: 8)
CLIENT_REFUSED (value: 9) → CLIENT_REFUSED (hmisValue: 9)
DATA_NOT_COLLECTED (value: 99) → DATA_NOT_COLLECTED (hmisValue: 99)
```

---

## 2. HealthAndDV.csv Mapping

### 2.1 Current Implementation

**Domain Models:**
- `org.haven.programenrollment.domain.HealthInsuranceRecord`
- `org.haven.programenrollment.domain.PhysicalDisabilityRecord`
- `org.haven.programenrollment.domain.ProgramSpecificDataElements` (legacy)

**Projection:** `org.haven.reporting.domain.hmis.HmisHealthAndDvProjection`

#### Mapped Fields (12/30)

| Field Name | Haven Field | HUD Element | Data Type | Status | Notes |
|------------|-------------|-------------|-----------|--------|-------|
| HealthAndDVID | healthAndDvId | - | String | ✅ Mapped | Generated as `HD_{enrollmentId}` |
| EnrollmentID | enrollmentId | - | String | ✅ Mapped | Foreign key |
| PersonalID | personalId | - | String | ✅ Mapped | Foreign key |
| InformationDate | informationDate | 4.04.A/4.05.A | Date | ✅ Mapped | Uses latest date from records |
| Medicaid | medicaid | 4.04.2 | Integer | ✅ Mapped | From HealthInsuranceRecord |
| Medicare | medicare | 4.04.3 | Integer | ✅ Mapped | From HealthInsuranceRecord |
| SCHIP | schip | 4.04.4 | Integer | ✅ Mapped | From HealthInsuranceRecord |
| VAMedicalServices | vaMedicalServices | 4.04.5 | Integer | ✅ Mapped | From HealthInsuranceRecord |
| EmployerProvided | employerProvided | 4.04.6 | Integer | ✅ Mapped | From HealthInsuranceRecord |
| COBRA | cobra | 4.04.7 | Integer | ⚠️ Partial | Not in legacy enum |
| PrivatePayment | privatePayment | 4.04.8 | Integer | ✅ Mapped | From HealthInsuranceRecord |
| StateHealthIns | stateHealthInsurance | 4.04.9 | Integer | ⚠️ Partial | Not in legacy enum |
| IndianHealthServices | indianHealthService | 4.04.10 | Integer | ⚠️ Partial | Not in legacy enum |
| OtherInsurance | otherInsurance | 4.04.11 | Integer | ✅ Mapped | From HealthInsuranceRecord |
| NoInsurance | noInsurance | 4.04.12 | Integer | ✅ Mapped | Derived from coveredByHealthInsurance |
| PhysicalDisability | physicalDisability | 4.05.2 | Integer | ✅ Mapped | From PhysicalDisabilityRecord |
| DevelopmentalDisability | developmentalDisability | 4.06.2 | Integer | ✅ Mapped | From PSDE (needs own record type) |
| ChronicHealthCondition | chronicHealthCondition | 4.07.2 | Integer | ✅ Mapped | From PSDE (needs own record type) |
| HIVAIDS | hivAids | 4.08.2 | Integer | ✅ Mapped | From PSDE (needs own record type) |
| MentalHealthDisorder | mentalHealthDisorder | 4.09.2 | Integer | ✅ Mapped | From PSDE (needs own record type) |
| SubstanceUseDisorder | substanceUseDisorder | 4.10.2 | Integer | ✅ Mapped | From PSDE (needs own record type) |
| DomesticViolence | domesticViolence | 4.11.2 | Integer | ✅ Mapped | VAWA-sensitive |

#### Missing Fields (18)

1. `DataCollectionStage` - Required for all assessment data
2. Insurance fields from FY2024 updates:
   - COBRA (4.04.7)
   - StateHealthIns (4.04.9) for Adults
   - IndianHealthServices (4.04.10)
3. Disability "Indefinite and Impairs" fields:
   - `PhysicalDisabilityIndefiniteImpairs` (4.05.3)
   - `DevelopmentalDisabilityIndefiniteImpairs` (4.06.3)
   - `ChronicHealthConditionIndefiniteImpairs` (4.07.3)
   - `HIVAIDSIndefiniteImpairs` (4.08.3)
   - `MentalHealthDisorderIndefiniteImpairs` (4.09.3)
   - `SubstanceUseDisorderIndefiniteImpairs` (4.10.3)
4. `TCellCount` (4.08.4) for HIV/AIDS
5. `TCellSource` (4.08.5)
6. `ViralLoadCount` (4.08.6)
7. `ViralLoadSource` (4.08.7)
8. DV sub-fields:
   - `DVVictimServiceProvider` (4.11.3)
   - `CurrentlyFleeing` (4.11.4)
9. Audit fields:
   - `DateCreated`
   - `DateUpdated`
   - `UserID`
   - `DateDeleted`
   - `ExportID`

### 2.2 Value Domain Conversions

#### HmisFivePointResponse → DisabilityType
Same as IncomeBenefits mapping above.

#### DomesticViolence Enum → Integer

```java
DomesticViolence → HMIS Integer
YES (value: 1) → 1
NO (value: 0) → 0
CLIENT_DOESNT_KNOW (value: 8) → 8
CLIENT_REFUSED (value: 9) → 9
DATA_NOT_COLLECTED (value: 99) → 99
```

---

## 3. Disabilities.csv Mapping

### 3.1 Current Implementation

**Domain Models:**
- `org.haven.programenrollment.domain.DisabilityRecord` (comprehensive)
- `org.haven.programenrollment.domain.PhysicalDisabilityRecord` (legacy)

**Projection:** `org.haven.reporting.domain.hmis.HmisDisabilitiesProjection`

#### Mapped Fields (8/16)

| Field Name | Haven Field | HUD Element | Data Type | Status | Notes |
|------------|-------------|-------------|-----------|--------|-------|
| DisabilitiesID | disabilitiesId | - | String | ✅ Mapped | Generated as `DIS_{enrollmentId}_{stage}` |
| EnrollmentID | enrollmentId | - | String | ✅ Mapped | Foreign key |
| PersonalID | personalId | - | String | ✅ Mapped | Foreign key |
| InformationDate | informationDate | 4.05.A | Date | ✅ Mapped | From DisabilityRecord |
| DataCollectionStage | dataCollectionStage | - | Integer | ✅ Mapped | 1=Start, 2=Update, 3=Exit |
| PhysicalDisability | physicalDisability | 4.05.2 | Integer | ✅ Mapped | From DisabilityRecord (DisabilityKind.PHYSICAL) |
| DevelopmentalDisability | developmentalDisability | 4.06.2 | Integer | ✅ Mapped | From DisabilityRecord (DisabilityKind.DEVELOPMENTAL) |
| ChronicHealthCondition | chronicHealthCondition | 4.07.2 | Integer | ✅ Mapped | From DisabilityRecord (DisabilityKind.CHRONIC_HEALTH_CONDITION) |
| HIVAIDS | hivAidsDisability | 4.08.2 | Integer | ✅ Mapped | From DisabilityRecord (DisabilityKind.HIV_AIDS) |
| MentalHealthDisorder | mentalHealthDisability | 4.09.2 | Integer | ✅ Mapped | From DisabilityRecord (DisabilityKind.MENTAL_HEALTH) |
| SubstanceUseDisorder | substanceAbuseDisability | 4.10.2 | Integer | ✅ Mapped | From DisabilityRecord (DisabilityKind.SUBSTANCE_USE) |

#### Missing Fields (8)

1. `PhysicalDisabilityIndefiniteImpairs` (4.05.3)
2. `DevelopmentalDisabilityIndefiniteImpairs` (4.06.3)
3. `ChronicHealthConditionIndefiniteImpairs` (4.07.3)
4. `HIVAIDSIndefiniteImpairs` (4.08.3)
5. `MentalHealthDisorderIndefiniteImpairs` (4.09.3)
6. `SubstanceUseDisorderIndefiniteImpairs` (4.10.3)
7. `TCellCount` (4.08.4)
8. `TCellSource` (4.08.5)
9. `ViralLoadCount` (4.08.6)
10. `ViralLoadSource` (4.08.7)
11. `AntiRetroviral` (4.08.8)
12. Audit fields (DateCreated, DateUpdated, UserID, DateDeleted, ExportID)

### 3.2 Value Domain Conversions

#### HmisFivePoint → DisabilityType

```java
HmisFivePoint → DisabilityType
YES → YES (hmisValue: 1)
NO → NO (hmisValue: 0)
CLIENT_DOESNT_KNOW → CLIENT_DOESNT_KNOW (hmisValue: 8)
CLIENT_REFUSED → CLIENT_REFUSED (hmisValue: 9)
DATA_NOT_COLLECTED → DATA_NOT_COLLECTED (hmisValue: 99)
```

#### DataCollectionStage → Integer

```java
DataCollectionStage → HMIS Integer
PROJECT_START → "1"
UPDATE → "2"
PROJECT_EXIT → "3"
```

---

## 4. Deprecation Warnings & Version Changes

### FY2024 Changes (Effective October 1, 2024)

#### New Fields (FY2024+)
1. **HealthAndDV.csv**
   - `COBRA` (4.04.7) - New insurance type
   - `StateHealthIns` (4.04.9) - State Adult Health Insurance Programs
   - `IndianHealthServices` (4.04.10) - Indian Health Services

2. **Disabilities.csv**
   - All "IndefiniteImpairs" sub-fields for disability types (4.05.3, 4.06.3, 4.07.3, 4.08.3, 4.09.3, 4.10.3)
   - HIV/AIDS clinical measures: TCellCount, ViralLoadCount, AntiRetroviral

#### Deprecated/Renamed (FY2022 → FY2024)
- **Gender fields** renamed to inclusive terminology (Woman, Man, NonBinary, etc.)
- **Race field** `HispanicLatinaeo` replaces `HispanicLatinx`
- `SubstanceAbuseDisability` → `SubstanceUseDisorder` (terminology update)

#### Value List Changes
- List 1.3 (Gender) expanded to 7 categories + "Data not collected"
- List 1.6 (Race) added "Middle Eastern/North African" category
- List 4.10.2 (Disability Types) updated terminology

---

## 5. Recommended Actions

### Priority 1: Critical Gaps (Blocking Export Compliance)

1. ✅ **Add `IncomeFromAnySource` and `BenefitsFromAnySource` to IncomeBenefitsProjection**
   - Map from `IncomeBenefitsRecord.incomeFromAnySource` and `benefitsFromAnySource`
   - Convert `HmisFivePointResponse` → Integer (1, 0, 8, 9, 99)

2. ✅ **Add `DataCollectionStage` to all projections**
   - IncomeBenefitsProjection
   - HealthAndDvProjection
   - DisabilitiesProjection (already has this ✅)

3. ✅ **Implement HmisFivePointResponse → DisabilityType transformer**
   - Create `HmisValueConverter` utility class
   - Add to `reporting-metadata` module transformation rules

### Priority 2: FY2024 Compliance

4. ✅ **Add new FY2024 insurance fields to HealthInsuranceRecord**
   - Add `cobra`, `stateHealthInsurance`, `indianHealthService` boolean fields
   - Update HealthInsuranceRecord domain model
   - Update JpaHealthInsuranceEntity
   - Migrate existing data (default to DATA_NOT_COLLECTED)

5. ✅ **Add "Indefinite and Impairs" fields to DisabilityRecord**
   - Add `indefiniteAndImpairs` HmisFivePoint field to DisabilityRecord
   - Update DisabilitiesProjection to include these fields

6. ✅ **Add HIV/AIDS clinical measures to domain model**
   - Create `HIVAIDSClinicalRecord` value object
   - Fields: `tCellCount`, `tCellSource`, `viralLoadCount`, `viralLoadSource`, `antiRetroviral`

### Priority 3: Domain Model Enhancements

7. ✅ **Extract disability types from PSDE to dedicated record types**
   - Create `DevelopmentalDisabilityRecord`
   - Create `ChronicHealthConditionRecord`
   - Create `HIVAIDSRecord`
   - Create `MentalHealthDisorderRecord`
   - Create `SubstanceUseDisorderRecord`

8. ✅ **Create DomesticViolenceRecord with full FY2024 fields**
   - Fields: `currentlyFleeing`, `dvVictimServiceProvider`
   - VAWA-sensitive by default

### Priority 4: ETL Transform Implementation

9. ✅ **Implement version-aware ETL transformers**
   - Create `IncomeBenefitsETLTransformer`
   - Create `HealthAndDVETLTransformer`
   - Create `DisabilitiesETLTransformer`
   - Add version gating logic for FY2024 fields

10. ✅ **Seed transformation rules in V40 migration**
    - Add HmisFivePointResponse → DisabilityType rule
    - Add DomesticViolence → Integer rule
    - Add DataCollectionStage → Integer rule

---

## 6. Testing Strategy

### Unit Tests
- `IncomeBenefitsProjectionTest` - verify all 45 fields map correctly
- `HealthAndDvProjectionTest` - verify all 30 fields map correctly
- `DisabilitiesProjectionTest` - verify all 16 fields map correctly
- `HmisValueConverterTest` - verify enum conversions

### Regression Tests
- `ExportRegressionTest.testIncomeBenefitsLegacyCompatibility()` - FY2022 format still works
- `ExportRegressionTest.testHealthAndDVLegacyCompatibility()` - FY2022 format still works
- `ExportRegressionTest.testDisabilitiesLegacyCompatibility()` - FY2022 format still works
- `ExportRegressionTest.testFY2024NewFields()` - FY2024 fields export correctly

### Integration Tests
- `HmisCsvExportServiceTest.testCompleteExport()` - full ZIP export with all CSV files
- `HUDExportValidationServiceTest.validateFY2024Compliance()` - HUD spec validation

---

## 7. Version Gating Strategy

Implement `HmisExportVersion` enum to gate features:

```java
public enum HmisExportVersion {
    FY2022("2022-10-01", "2024-09-30"),
    FY2024("2024-10-01", null); // Current

    public boolean supports(String hudElementId);
}
```

**Config-driven approach:**
```yaml
hmis:
  export:
    version: FY2024  # or FY2022 for legacy
    include-new-fields: true
    legacy-compatibility-mode: false
```

---

## Appendix A: Source File Locations

### Domain Models
- `backend/modules/program-enrollment/src/main/java/org/haven/programenrollment/domain/IncomeBenefitsRecord.java`
- `backend/modules/program-enrollment/src/main/java/org/haven/programenrollment/domain/HealthInsuranceRecord.java`
- `backend/modules/program-enrollment/src/main/java/org/haven/programenrollment/domain/PhysicalDisabilityRecord.java`
- `backend/modules/program-enrollment/src/main/java/org/haven/programenrollment/domain/DisabilityRecord.java`

### Projections
- `backend/modules/reporting/src/main/java/org/haven/reporting/domain/hmis/HmisIncomeBenefitsProjection.java`
- `backend/modules/reporting/src/main/java/org/haven/reporting/domain/hmis/HmisHealthAndDvProjection.java`
- `backend/modules/reporting/src/main/java/org/haven/reporting/domain/hmis/HmisDisabilitiesProjection.java`

### Services
- `backend/modules/reporting/src/main/java/org/haven/reporting/application/services/HmisCsvExportService.java`

### Migrations
- `backend/apps/api-app/src/main/resources/db/migration/V34__reporting_metadata_schema.sql`
- `backend/apps/api-app/src/main/resources/db/migration/V35__seed_hud_2024_universal_data_elements.sql`
- `backend/apps/api-app/src/main/resources/db/migration/V36__seed_enrollment_services_segments.sql`

---

## Appendix B: HUD Data Standards References

- **HUD HMIS Data Standards Manual 2024**
  https://files.hudexchange.info/resources/documents/HMIS-Data-Standards-Manual-2024.pdf

- **HMIS CSV Format Specifications 2024** (v1.5)
  https://files.hudexchange.info/resources/documents/HMIS-CSV-Format-Specifications-2024.pdf

- **HUD Exchange HMIS Data Standards Page**
  https://www.hudexchange.info/programs/hmis/hmis-data-standards/

---

**Document Prepared By:** Claude Code
**Review Status:** Ready for Implementation
**Next Review:** After V40 migration completion
