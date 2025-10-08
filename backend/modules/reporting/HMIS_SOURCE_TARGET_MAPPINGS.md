# HMIS Normalized Source-to-Target Field Mappings

**Version:** HUD FY2024 v1.0
**Effective:** October 1, 2024
**Last Updated:** 2025-10-07

## Document Purpose

This document provides normalized field mappings from Haven domain models to HUD HMIS CSV format, including:
1. Source field → Target CSV field mapping
2. Data type conversions
3. Value domain transformations
4. Picklist remappings
5. Default value rules for missing data
6. Version-specific gating logic

---

## 1. IncomeBenefits.csv Mappings

### 1.1 Field-Level Mappings

```yaml
source_entity: IncomeBenefitsRecord
target_csv: IncomeBenefits.csv
hud_specification: 4.02 Income and Sources, 4.03 Non-Cash Benefits

field_mappings:
  # Primary Keys & Identifiers
  - source_field: recordId
    target_field: IncomeBenefitsID
    data_type: UUID → String
    transformation: "Generate composite ID: 'IB_' + enrollmentId.value().toString()"
    required: true
    version: FY2022+

  - source_field: enrollmentId.value()
    target_field: EnrollmentID
    data_type: UUID → String
    transformation: "enrollmentId.value().toString()"
    required: true
    version: FY2022+

  - source_field: clientId.value()
    target_field: PersonalID
    data_type: UUID → String
    transformation: "clientId.value().toString()"
    required: true
    version: FY2022+

  # Data Collection Info
  - source_field: informationDate
    target_field: InformationDate
    data_type: LocalDate → String (YYYY-MM-DD)
    transformation: "informationDate.toString()"
    required: true
    version: FY2022+

  - source_field: recordType
    target_field: DataCollectionStage
    data_type: InformationDate enum → Integer
    value_mapping:
      START_OF_PROJECT: 1
      UPDATE: 2
      ANNUAL_ASSESSMENT: 5
      EXIT: 3
      MINOR_TURNING_18: 4
    transformation: |
      switch(recordType) {
        case START_OF_PROJECT -> 1;
        case UPDATE -> 2;
        case EXIT -> 3;
        case MINOR_TURNING_18 -> 4;
        case ANNUAL_ASSESSMENT -> 5;
      }
    required: true
    version: FY2024+
    notes: "FY2024 added for better data collection tracking"

  # Income Summary (4.02.1)
  - source_field: incomeFromAnySource
    target_field: IncomeFromAnySource
    data_type: HmisFivePointResponse → Integer
    value_mapping:
      YES: 1
      NO: 0
      CLIENT_DOESNT_KNOW: 8
      CLIENT_REFUSED: 9
      DATA_NOT_COLLECTED: 99
    transformation: "HmisValueConverter.toInteger(incomeFromAnySource)"
    required: true
    version: FY2022+

  - source_field: totalMonthlyIncome
    target_field: TotalMonthlyIncome
    data_type: Integer → Integer
    transformation: "totalMonthlyIncome (no conversion)"
    required: false
    default_missing: null
    validation: "If IncomeFromAnySource=1 (Yes), must be > 0"
    version: FY2022+

  # Earned Income (4.02.2)
  - source_field: earnedIncome
    target_field: Earned
    data_type: DisabilityType → Integer
    value_mapping:
      YES: 1
      NO: 0
      CLIENT_DOESNT_KNOW: 8
      CLIENT_REFUSED: 9
      DATA_NOT_COLLECTED: 99
    transformation: "earnedIncome.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: earnedIncomeAmount
    target_field: EarnedAmount
    data_type: Integer → Integer
    transformation: "earnedIncomeAmount (no conversion)"
    required: false
    default_missing: null
    validation: "If Earned=1, should be provided"
    version: FY2022+

  # Unemployment Insurance (4.02.3)
  - source_field: unemploymentIncome
    target_field: Unemployment
    data_type: DisabilityType → Integer
    transformation: "unemploymentIncome.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: unemploymentIncomeAmount
    target_field: UnemploymentAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # SSI (4.02.4)
  - source_field: supplementalSecurityIncome
    target_field: SSI
    data_type: DisabilityType → Integer
    transformation: "supplementalSecurityIncome.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: supplementalSecurityIncomeAmount
    target_field: SSIAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # SSDI (4.02.5)
  - source_field: socialSecurityDisabilityIncome
    target_field: SSDI
    data_type: DisabilityType → Integer
    transformation: "socialSecurityDisabilityIncome.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: socialSecurityDisabilityIncomeAmount
    target_field: SSDIAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # VA Disability - Service Connected (4.02.6)
  - source_field: vaDisabilityServiceConnected
    target_field: VADisabilityService
    data_type: DisabilityType → Integer
    transformation: "vaDisabilityServiceConnected.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: vaDisabilityServiceConnectedAmount
    target_field: VADisabilityServiceAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # VA Disability - Non-Service Connected (4.02.7)
  - source_field: vaDisabilityNonServiceConnected
    target_field: VADisabilityNonService
    data_type: DisabilityType → Integer
    transformation: "vaDisabilityNonServiceConnected.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: vaDisabilityNonServiceConnectedAmount
    target_field: VADisabilityNonServiceAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # Private Disability Insurance (4.02.8)
  - source_field: privateDisabilityIncome
    target_field: PrivateDisability
    data_type: DisabilityType → Integer
    transformation: "privateDisabilityIncome.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: privateDisabilityIncomeAmount
    target_field: PrivateDisabilityAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # Workers' Compensation (4.02.9)
  - source_field: workersCompensation
    target_field: WorkersComp
    data_type: DisabilityType → Integer
    transformation: "workersCompensation.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: workersCompensationAmount
    target_field: WorkersCompAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # TANF (4.02.10)
  - source_field: tanfIncome
    target_field: TANF
    data_type: DisabilityType → Integer
    transformation: "tanfIncome.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: tanfIncomeAmount
    target_field: TANFAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # General Assistance (4.02.11)
  - source_field: generalAssistance
    target_field: GA
    data_type: DisabilityType → Integer
    transformation: "generalAssistance.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: generalAssistanceAmount
    target_field: GAAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # Social Security Retirement (4.02.12)
  - source_field: socialSecurityRetirement
    target_field: SocSecRetirement
    data_type: DisabilityType → Integer
    transformation: "socialSecurityRetirement.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: socialSecurityRetirementAmount
    target_field: SocSecRetirementAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # Pension/Retirement (4.02.13)
  - source_field: pensionFromFormerJob
    target_field: Pension
    data_type: DisabilityType → Integer
    transformation: "pensionFromFormerJob.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: pensionFromFormerJobAmount
    target_field: PensionAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # Child Support (4.02.14)
  - source_field: childSupport
    target_field: ChildSupport
    data_type: DisabilityType → Integer
    transformation: "childSupport.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: childSupportAmount
    target_field: ChildSupportAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # Alimony (4.02.15)
  - source_field: alimony
    target_field: Alimony
    data_type: DisabilityType → Integer
    transformation: "alimony.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: alimonyAmount
    target_field: AlimonyAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  # Other Income Source (4.02.16)
  - source_field: otherIncomeSource
    target_field: OtherIncomeSource
    data_type: DisabilityType → Integer
    transformation: "otherIncomeSource.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: otherIncomeAmount
    target_field: OtherIncomeAmount
    data_type: Integer → Integer
    required: false
    default_missing: null
    version: FY2022+

  - source_field: otherIncomeSourceIdentify
    target_field: OtherIncomeSourceIdentify
    data_type: String → String (max 100 chars)
    transformation: "otherIncomeSourceIdentify (no conversion)"
    required: false
    default_missing: null
    validation: "Required if OtherIncomeSource=1"
    version: FY2022+

  # Benefits Summary (4.03.1)
  - source_field: benefitsFromAnySource
    target_field: BenefitsFromAnySource
    data_type: HmisFivePointResponse → Integer
    value_mapping:
      YES: 1
      NO: 0
      CLIENT_DOESNT_KNOW: 8
      CLIENT_REFUSED: 9
      DATA_NOT_COLLECTED: 99
    transformation: "HmisValueConverter.toInteger(benefitsFromAnySource)"
    required: true
    version: FY2022+

  # SNAP (4.03.2)
  - source_field: snap
    target_field: SNAP
    data_type: HmisFivePointResponse → Integer
    value_mapping:
      YES: 1
      NO: 0
      CLIENT_DOESNT_KNOW: 8
      CLIENT_REFUSED: 9
      DATA_NOT_COLLECTED: 99
    transformation: "HmisValueConverter.toInteger(snap)"
    required: conditional
    version: FY2022+

  # WIC (4.03.3)
  - source_field: wic
    target_field: WIC
    data_type: HmisFivePointResponse → Integer
    transformation: "HmisValueConverter.toInteger(wic)"
    required: conditional
    version: FY2022+

  # TANF Child Care (4.03.4)
  - source_field: tanfChildCare
    target_field: TANFChildCare
    data_type: HmisFivePointResponse → Integer
    transformation: "HmisValueConverter.toInteger(tanfChildCare)"
    required: conditional
    version: FY2022+

  # TANF Transportation (4.03.5)
  - source_field: tanfTransportation
    target_field: TANFTransportation
    data_type: HmisFivePointResponse → Integer
    transformation: "HmisValueConverter.toInteger(tanfTransportation)"
    required: conditional
    version: FY2022+

  # Other TANF (4.03.6)
  - source_field: otherTanf
    target_field: OtherTANF
    data_type: HmisFivePointResponse → Integer
    transformation: "HmisValueConverter.toInteger(otherTanf)"
    required: conditional
    version: FY2022+

  # Other Benefits Source (4.03.7)
  - source_field: otherBenefitsSource
    target_field: OtherBenefitsSource
    data_type: HmisFivePointResponse → Integer
    transformation: "HmisValueConverter.toInteger(otherBenefitsSource)"
    required: conditional
    version: FY2022+

  - source_field: otherBenefitsSpecify
    target_field: OtherBenefitsSourceIdentify
    data_type: String → String (max 100 chars)
    transformation: "otherBenefitsSpecify (no conversion)"
    required: false
    default_missing: null
    validation: "Required if OtherBenefitsSource=1"
    version: FY2022+

  # Audit Fields
  - source_field: createdAt
    target_field: DateCreated
    data_type: Instant → DateTime (ISO 8601)
    transformation: "createdAt.toString()"
    required: true
    version: FY2022+

  - source_field: updatedAt
    target_field: DateUpdated
    data_type: Instant → DateTime (ISO 8601)
    transformation: "updatedAt.toString()"
    required: true
    version: FY2022+

  - source_field: collectedBy
    target_field: UserID
    data_type: String → String
    transformation: "collectedBy (no conversion)"
    required: true
    version: FY2022+

  - source_field: (constant)
    target_field: DateDeleted
    data_type: DateTime
    transformation: "null (soft-deletes not implemented)"
    required: false
    default_missing: null
    version: FY2022+

  - source_field: (export context)
    target_field: ExportID
    data_type: String → String
    transformation: "exportId from export context"
    required: true
    version: FY2022+
```

### 1.2 Defaulting Rules for Missing Data

```yaml
missing_data_rules:
  - field: IncomeFromAnySource
    rule: "If null, default to 99 (Data not collected)"

  - field: BenefitsFromAnySource
    rule: "If null, default to 99 (Data not collected)"

  - field: Income/Benefit source fields
    rule: "If IncomeFromAnySource=99 and individual field null, default to 99"

  - field: Amount fields
    rule: "If corresponding Yes/No field != 1 (Yes), amount must be null"

  - field: OtherIncomeSourceIdentify
    rule: "If OtherIncomeSource != 1, must be null"

  - field: OtherBenefitsSourceIdentify
    rule: "If OtherBenefitsSource != 1, must be null"
```

---

## 2. HealthAndDV.csv Mappings

### 2.1 Field-Level Mappings

```yaml
source_entities:
  - HealthInsuranceRecord (4.04)
  - PhysicalDisabilityRecord (4.05)
  - ProgramSpecificDataElements (legacy - disabilities 4.06-4.10)
  - DomesticViolenceRecord (4.11) - future
target_csv: HealthAndDV.csv

field_mappings:
  # Primary Keys
  - source_field: "composite key"
    target_field: HealthAndDVID
    transformation: "'HD_' + enrollmentId.toString() + '_' + dataCollectionStage"
    required: true
    version: FY2022+

  - source_field: enrollmentId
    target_field: EnrollmentID
    transformation: "enrollmentId.value().toString()"
    required: true
    version: FY2022+

  - source_field: clientId
    target_field: PersonalID
    transformation: "clientId.value().toString()"
    required: true
    version: FY2022+

  # Data Collection Info
  - source_field: "max(healthRecord.informationDate, disabilityRecord.informationDate)"
    target_field: InformationDate
    data_type: LocalDate → String (YYYY-MM-DD)
    transformation: "Latest information date from all contributing records"
    required: true
    version: FY2022+

  - source_field: (derived from enrollment stage)
    target_field: DataCollectionStage
    data_type: Integer
    value_mapping:
      PROJECT_START: 1
      UPDATE: 2
      PROJECT_EXIT: 3
    transformation: "Derived from enrollment stage context"
    required: true
    version: FY2024+

  # Health Insurance (4.04) - from HealthInsuranceRecord
  - source_field: coveredByHealthInsurance
    target_field: InsuranceFromAnySource
    data_type: HmisFivePointResponse → Integer
    transformation: "HmisValueConverter.toInteger(coveredByHealthInsurance)"
    required: true
    version: FY2024+

  - source_field: medicaid
    target_field: Medicaid
    data_type: boolean → Integer
    transformation: |
      if (coveredByHealthInsurance == YES && medicaid) -> 1
      else if (coveredByHealthInsurance == NO) -> 0
      else -> 99
    required: conditional
    version: FY2022+

  - source_field: medicare
    target_field: Medicare
    data_type: boolean → Integer
    transformation: "Same logic as Medicaid"
    required: conditional
    version: FY2022+

  - source_field: schip
    target_field: SCHIP
    data_type: boolean → Integer
    transformation: "Same logic as Medicaid"
    required: conditional
    version: FY2022+

  - source_field: vhaMedicalServices
    target_field: VAMedicalServices
    data_type: boolean → Integer
    transformation: "Same logic as Medicaid"
    required: conditional
    version: FY2022+

  - source_field: employerProvided
    target_field: EmployerProvided
    data_type: boolean → Integer
    transformation: "Same logic as Medicaid"
    required: conditional
    version: FY2022+

  - source_field: cobra
    target_field: COBRA
    data_type: boolean → Integer
    transformation: "Same logic as Medicaid"
    required: conditional
    version: FY2024+
    notes: "New FY2024 field - not in FY2022 spec"

  - source_field: privatePay
    target_field: PrivatePay
    data_type: boolean → Integer
    transformation: "Same logic as Medicaid"
    required: conditional
    version: FY2022+

  - source_field: stateAdultHealthInsurance
    target_field: StateHealthInsforAdults
    data_type: boolean → Integer
    transformation: "Same logic as Medicaid"
    required: conditional
    version: FY2024+
    notes: "New FY2024 field - not in FY2022 spec"

  - source_field: indianHealthService
    target_field: IndianHealthServices
    data_type: boolean → Integer
    transformation: "Same logic as Medicaid"
    required: conditional
    version: FY2024+
    notes: "New FY2024 field - not in FY2022 spec"

  - source_field: otherInsurance
    target_field: OtherInsurance
    data_type: boolean → Integer
    transformation: "Same logic as Medicaid"
    required: conditional
    version: FY2022+

  - source_field: coveredByHealthInsurance
    target_field: NoInsurance
    data_type: HmisFivePointResponse → Integer
    transformation: |
      if (coveredByHealthInsurance == NO) -> 1
      else if (coveredByHealthInsurance == YES) -> 0
      else -> 99
    required: conditional
    version: FY2022+

  # Physical Disability (4.05) - from PhysicalDisabilityRecord
  - source_field: physicalDisability
    target_field: PhysicalDisability
    data_type: HmisFivePointResponse → Integer
    transformation: "HmisValueConverter.toInteger(physicalDisability)"
    required: conditional
    version: FY2022+

  - source_field: indefiniteAndImpairs
    target_field: PhysicalDisabilityLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(indefiniteAndImpairs)"
    required: conditional
    validation: "Required if PhysicalDisability=1"
    version: FY2024+

  # Other Disabilities (4.06-4.10) - from PSDE or dedicated records
  - source_field: developmentalDisability
    target_field: DevelopmentalDisability
    data_type: DisabilityType → Integer
    transformation: "developmentalDisability.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: developmentalDisabilityIndefiniteImpairs
    target_field: DevelopmentalDisabilityLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(developmentalDisabilityIndefiniteImpairs)"
    required: conditional
    validation: "Required if DevelopmentalDisability=1"
    version: FY2024+

  - source_field: chronicHealthCondition
    target_field: ChronicHealthCondition
    data_type: DisabilityType → Integer
    transformation: "chronicHealthCondition.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: chronicHealthConditionIndefiniteImpairs
    target_field: ChronicHealthConditionLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(chronicHealthConditionIndefiniteImpairs)"
    required: conditional
    validation: "Required if ChronicHealthCondition=1"
    version: FY2024+

  - source_field: hivAids
    target_field: HIV
    data_type: DisabilityType → Integer
    transformation: "hivAids.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: hivAidsIndefiniteImpairs
    target_field: HIVLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(hivAidsIndefiniteImpairs)"
    required: conditional
    validation: "Required if HIV=1"
    version: FY2024+

  - source_field: mentalHealthDisorder
    target_field: MentalHealthDisorder
    data_type: DisabilityType → Integer
    transformation: "mentalHealthDisorder.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: mentalHealthDisorderIndefiniteImpairs
    target_field: MentalHealthDisorderLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(mentalHealthDisorderIndefiniteImpairs)"
    required: conditional
    validation: "Required if MentalHealthDisorder=1"
    version: FY2024+

  - source_field: substanceUseDisorder
    target_field: SubstanceUseDisorder
    data_type: DisabilityType → Integer
    transformation: "substanceUseDisorder.getHmisValue()"
    required: conditional
    version: FY2022+

  - source_field: substanceUseDisorderIndefiniteImpairs
    target_field: SubstanceUseDisorderLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(substanceUseDisorderIndefiniteImpairs)"
    required: conditional
    validation: "Required if SubstanceUseDisorder=1"
    version: FY2024+

  # HIV/AIDS Clinical Measures (4.08.4-4.08.8) - FY2024+
  - source_field: tCellCount
    target_field: TCellCount
    data_type: Integer → Integer
    transformation: "tCellCount (no conversion)"
    required: false
    validation: "Only if HIV=1"
    version: FY2024+

  - source_field: tCellSource
    target_field: TCellSource
    data_type: Integer → Integer
    value_mapping:
      MEDICAL_REPORT: 1
      CLIENT_REPORT: 2
      OTHER: 3
    required: false
    validation: "Required if TCellCount provided"
    version: FY2024+

  - source_field: viralLoadCount
    target_field: ViralLoadCount
    data_type: Integer → Integer
    transformation: "viralLoadCount (no conversion)"
    required: false
    validation: "Only if HIV=1"
    version: FY2024+

  - source_field: viralLoadSource
    target_field: ViralLoadSource
    data_type: Integer → Integer
    value_mapping:
      MEDICAL_REPORT: 1
      CLIENT_REPORT: 2
      OTHER: 3
    required: false
    validation: "Required if ViralLoadCount provided"
    version: FY2024+

  - source_field: antiRetroviral
    target_field: AntiRetroviral
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(antiRetroviral)"
    required: false
    validation: "Only if HIV=1"
    version: FY2024+

  # Domestic Violence (4.11) - VAWA SENSITIVE
  - source_field: domesticViolence
    target_field: DomesticViolence
    data_type: DomesticViolence enum → Integer
    value_mapping:
      YES: 1
      NO: 0
      CLIENT_DOESNT_KNOW: 8
      CLIENT_REFUSED: 9
      DATA_NOT_COLLECTED: 99
    transformation: "domesticViolence.getHmisValue()"
    required: conditional
    vawa_sensitive: true
    vawa_suppression: REDACT
    version: FY2022+

  - source_field: currentlyFleeing
    target_field: CurrentlyFleeing
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(currentlyFleeing)"
    required: conditional
    validation: "Required if DomesticViolence=1"
    vawa_sensitive: true
    vawa_suppression: REDACT
    version: FY2024+

  - source_field: dvVictimServiceProvider
    target_field: WhenOccurred
    data_type: Integer → Integer
    value_mapping:
      WITHIN_PAST_3_MONTHS: 1
      THREE_TO_SIX_MONTHS_AGO: 2
      SIX_MONTHS_TO_ONE_YEAR_AGO: 3
      ONE_YEAR_AGO_OR_MORE: 4
    required: conditional
    validation: "Required if DomesticViolence=1"
    vawa_sensitive: true
    vawa_suppression: REDACT
    version: FY2024+

  # Audit Fields
  - source_field: createdAt
    target_field: DateCreated
    data_type: Instant → DateTime
    transformation: "createdAt.toString()"
    required: true
    version: FY2022+

  - source_field: updatedAt
    target_field: DateUpdated
    data_type: Instant → DateTime
    transformation: "updatedAt.toString()"
    required: true
    version: FY2022+

  - source_field: collectedBy
    target_field: UserID
    data_type: String → String
    required: true
    version: FY2022+

  - source_field: (constant)
    target_field: DateDeleted
    transformation: null
    required: false
    version: FY2022+

  - source_field: (export context)
    target_field: ExportID
    required: true
    version: FY2022+
```

### 2.2 Defaulting Rules

```yaml
missing_data_rules:
  - fields: All insurance type fields
    rule: "If InsuranceFromAnySource=99, default all to 99"

  - fields: All disability "Longterm" fields
    rule: "If corresponding disability field != 1, must be null or 99"

  - fields: HIV clinical measures
    rule: "If HIV != 1, all must be null"

  - fields: DV sub-fields
    rule: "If DomesticViolence != 1, all must be null or 99"

  - field: NoInsurance
    rule: "Inverse of InsuranceFromAnySource: YES→0, NO→1"
```

---

## 3. Disabilities.csv Mappings

### 3.1 Field-Level Mappings

```yaml
source_entity: DisabilityRecord
target_csv: Disabilities.csv

field_mappings:
  # Primary Keys
  - source_field: "composite"
    target_field: DisabilitiesID
    transformation: "'DIS_' + enrollmentId + '_' + stage.name()"
    required: true
    version: FY2022+

  - source_field: enrollmentId
    target_field: EnrollmentID
    transformation: "enrollmentId.value().toString()"
    required: true
    version: FY2022+

  - source_field: clientId
    target_field: PersonalID
    transformation: "clientId.value().toString()"
    required: true
    version: FY2022+

  # Data Collection Info
  - source_field: informationDate
    target_field: InformationDate
    data_type: LocalDate → String (YYYY-MM-DD)
    transformation: "informationDate.toString()"
    required: true
    version: FY2022+

  - source_field: stage
    target_field: DataCollectionStage
    data_type: DataCollectionStage enum → Integer
    value_mapping:
      PROJECT_START: 1
      UPDATE: 2
      PROJECT_EXIT: 3
    transformation: |
      switch(stage) {
        case PROJECT_START -> 1;
        case UPDATE -> 2;
        case PROJECT_EXIT -> 3;
      }
    required: true
    version: FY2022+

  # Disability Type Mappings (filter by DisabilityKind)
  - source_field: hasDisability (where disabilityKind=PHYSICAL)
    target_field: PhysicalDisability
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(hasDisability)"
    required: conditional
    version: FY2022+

  - source_field: indefiniteAndImpairs (where disabilityKind=PHYSICAL)
    target_field: PhysicalDisabilityLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(indefiniteAndImpairs)"
    required: conditional
    validation: "Required if PhysicalDisability=1"
    version: FY2024+

  - source_field: hasDisability (where disabilityKind=DEVELOPMENTAL)
    target_field: DevelopmentalDisability
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(hasDisability)"
    required: conditional
    version: FY2022+

  - source_field: indefiniteAndImpairs (where disabilityKind=DEVELOPMENTAL)
    target_field: DevelopmentalDisabilityLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(indefiniteAndImpairs)"
    required: conditional
    validation: "Required if DevelopmentalDisability=1"
    version: FY2024+

  - source_field: hasDisability (where disabilityKind=CHRONIC_HEALTH_CONDITION)
    target_field: ChronicHealthCondition
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(hasDisability)"
    required: conditional
    version: FY2022+

  - source_field: indefiniteAndImpairs (where disabilityKind=CHRONIC_HEALTH_CONDITION)
    target_field: ChronicHealthConditionLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(indefiniteAndImpairs)"
    required: conditional
    validation: "Required if ChronicHealthCondition=1"
    version: FY2024+

  - source_field: hasDisability (where disabilityKind=HIV_AIDS)
    target_field: HIVAIDS
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(hasDisability)"
    required: conditional
    version: FY2022+

  - source_field: indefiniteAndImpairs (where disabilityKind=HIV_AIDS)
    target_field: HIVAIDSLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(indefiniteAndImpairs)"
    required: conditional
    validation: "Required if HIVAIDS=1"
    version: FY2024+

  - source_field: hasDisability (where disabilityKind=MENTAL_HEALTH)
    target_field: MentalHealthDisorder
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(hasDisability)"
    required: conditional
    version: FY2022+

  - source_field: indefiniteAndImpairs (where disabilityKind=MENTAL_HEALTH)
    target_field: MentalHealthDisorderLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(indefiniteAndImpairs)"
    required: conditional
    validation: "Required if MentalHealthDisorder=1"
    version: FY2024+

  - source_field: hasDisability (where disabilityKind=SUBSTANCE_USE)
    target_field: SubstanceUseDisorder
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(hasDisability)"
    required: conditional
    version: FY2022+

  - source_field: indefiniteAndImpairs (where disabilityKind=SUBSTANCE_USE)
    target_field: SubstanceUseDisorderLongterm
    data_type: HmisFivePoint → Integer
    transformation: "HmisValueConverter.toInteger(indefiniteAndImpairs)"
    required: conditional
    validation: "Required if SubstanceUseDisorder=1"
    version: FY2024+

  # Audit Fields
  - source_field: createdAt
    target_field: DateCreated
    transformation: "createdAt.toString()"
    required: true
    version: FY2022+

  - source_field: updatedAt
    target_field: DateUpdated
    transformation: "updatedAt.toString()"
    required: true
    version: FY2022+

  - source_field: collectedBy
    target_field: UserID
    required: true
    version: FY2022+

  - source_field: (constant)
    target_field: DateDeleted
    transformation: null
    required: false
    version: FY2022+

  - source_field: (export context)
    target_field: ExportID
    required: true
    version: FY2022+
```

### 3.2 Defaulting Rules

```yaml
missing_data_rules:
  - fields: All disability type fields
    rule: "If no record for specific DisabilityKind + DataCollectionStage, default to 99"

  - fields: All "Longterm" fields
    rule: "If corresponding disability field != 1 (Yes), must be null or 99"

  - field: DataCollectionStage
    rule: "Required - determine from enrollment context (start, annual, exit)"
```

---

## 4. Common Value Domain Conversions

### 4.1 Enum Conversion Functions

```java
/**
 * Converts HmisFivePointResponse to HMIS Integer value
 */
public static Integer toInteger(HmisFivePointResponse response) {
    if (response == null) return 99; // DATA_NOT_COLLECTED
    return switch (response) {
        case YES -> 1;
        case NO -> 0;
        case CLIENT_DOESNT_KNOW -> 8;
        case CLIENT_REFUSED -> 9;
        case DATA_NOT_COLLECTED -> 99;
    };
}

/**
 * Converts HmisFivePoint to HMIS Integer value
 */
public static Integer toInteger(HmisFivePoint response) {
    if (response == null) return 99;
    return switch (response) {
        case YES -> 1;
        case NO -> 0;
        case CLIENT_DOESNT_KNOW -> 8;
        case CLIENT_REFUSED -> 9;
        case DATA_NOT_COLLECTED -> 99;
    };
}

/**
 * Converts DomesticViolence enum to HMIS Integer value
 */
public static Integer toInteger(DomesticViolence dv) {
    if (dv == null) return 99;
    return switch (dv) {
        case YES -> 1;
        case NO -> 0;
        case CLIENT_DOESNT_KNOW -> 8;
        case CLIENT_REFUSED -> 9;
        case DATA_NOT_COLLECTED -> 99;
    };
}

/**
 * Converts DisabilityType to HMIS Integer value
 */
public static Integer toInteger(DisabilityType type) {
    if (type == null) return 99;
    return type.getHmisValue(); // Uses existing method
}

/**
 * Converts InformationDate enum to DataCollectionStage Integer
 */
public static Integer toDataCollectionStage(InformationDate informationDate) {
    if (informationDate == null) return 1; // Default to project start
    return switch (informationDate) {
        case START_OF_PROJECT -> 1;
        case UPDATE -> 2;
        case EXIT -> 3;
        case MINOR_TURNING_18 -> 4;
        case ANNUAL_ASSESSMENT -> 5;
    };
}

/**
 * Converts DataCollectionStage enum to Integer
 */
public static Integer toInteger(DataCollectionStage stage) {
    if (stage == null) return 1;
    return switch (stage) {
        case PROJECT_START -> 1;
        case UPDATE -> 2;
        case PROJECT_EXIT -> 3;
    };
}
```

### 4.2 Date/Time Conversions

```java
/**
 * Converts LocalDate to HMIS CSV date format (YYYY-MM-DD)
 */
public static String formatDate(LocalDate date) {
    if (date == null) return "";
    return date.toString(); // Already ISO 8601 YYYY-MM-DD
}

/**
 * Converts Instant to HMIS CSV datetime format (ISO 8601)
 */
public static String formatDateTime(Instant instant) {
    if (instant == null) return "";
    return instant.toString(); // ISO 8601 with timezone
}
```

---

## 5. Version Gating Implementation

### 5.1 Configuration-Based Gating

```yaml
# application.yml
hmis:
  export:
    version: FY2024  # FY2022, FY2024
    compatibility-mode: strict  # strict, legacy-fallback

    # FY2024-specific features
    fy2024:
      enabled: true
      include-data-collection-stage: true
      include-indefinite-impairs: true
      include-new-insurance-types: true
      include-hiv-clinical-measures: true
```

### 5.2 Runtime Version Check

```java
public class HmisExportVersionService {

    private final HmisExportVersion configuredVersion;

    public boolean supportsField(String fieldName, String csvFile) {
        return switch (fieldName) {
            case "DataCollectionStage" -> configuredVersion.isAtLeast(FY2024);
            case "COBRA", "StateHealthInsforAdults", "IndianHealthServices" ->
                configuredVersion.isAtLeast(FY2024);
            case "PhysicalDisabilityLongterm", "DevelopmentalDisabilityLongterm" ->
                configuredVersion.isAtLeast(FY2024);
            case "TCellCount", "ViralLoadCount", "AntiRetroviral" ->
                configuredVersion.isAtLeast(FY2024);
            case "CurrentlyFleeing", "WhenOccurred" ->
                configuredVersion.isAtLeast(FY2024);
            default -> true; // Field exists in all versions
        };
    }
}
```

---

## Appendix: Transformation Rule Registration

```sql
-- V40 Migration: Register transformation rules
INSERT INTO transformation_rule (rule_name, category, description, expression_template, transform_language, return_data_type, hud_reference) VALUES
('HMIS_FIVE_POINT_RESPONSE_TO_INTEGER', 'VALUE_CONVERSION', 'Convert HmisFivePointResponse enum to HMIS Integer',
 'HmisValueConverter.toInteger(${field})', 'JAVA_EL', 'Integer', 'HMIS Data Standards FY2024'),

('DISABILITY_TYPE_TO_INTEGER', 'VALUE_CONVERSION', 'Convert DisabilityType enum to HMIS Integer',
 '${field}.getHmisValue()', 'JAVA_EL', 'Integer', 'HMIS Data Standards FY2024'),

('DOMESTIC_VIOLENCE_TO_INTEGER', 'VALUE_CONVERSION', 'Convert DomesticViolence enum to HMIS Integer',
 'HmisValueConverter.toInteger(${field})', 'JAVA_EL', 'Integer', 'HMIS Data Standards FY2024 - VAWA Protected'),

('DATA_COLLECTION_STAGE_TO_INTEGER', 'VALUE_CONVERSION', 'Convert DataCollectionStage enum to HMIS Integer',
 'HmisValueConverter.toInteger(${field})', 'JAVA_EL', 'Integer', 'HMIS Data Standards FY2024'),

('INFORMATION_DATE_TO_STAGE', 'VALUE_CONVERSION', 'Convert InformationDate enum to DataCollectionStage Integer',
 'HmisValueConverter.toDataCollectionStage(${field})', 'JAVA_EL', 'Integer', 'HMIS Data Standards FY2024');
```

---

**Document Version:** 1.0
**Status:** Ready for Implementation
**Next Steps:** Implement ETL transformers in backend/modules/reporting/src/main/java/org/haven/reporting/application/transformers/
