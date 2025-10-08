-- V36: Seed Enrollment Segments (Client, Enrollment, Exit, Services)
-- HUD HMIS Data Standards 2024 - CSV Format Specification
-- Mandatory HMIS CSV export tables

-- =============================================================================
-- Client.csv base identifiers
-- =============================================================================
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ClientProfile', 'clientId', 'CSV:Client.PersonalID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'PersonalID', 1),
('ClientProfile', 'createdDate', 'CSV:Client.DateCreated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateCreated', 101),
('ClientProfile', 'updatedDate', 'CSV:Client.DateUpdated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateUpdated', 102),
('ClientProfile', 'dataCollectionStage', 'CSV:Client.DataCollectionStage', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DataCollectionStage', 103);

-- =============================================================================
-- Enrollment.csv
-- =============================================================================
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ProgramEnrollment', 'enrollmentId', 'CSV:Enrollment.EnrollmentID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'EnrollmentID', 1),
('ProgramEnrollment', 'clientId', 'CSV:Enrollment.PersonalID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'PersonalID', 2),
('ProgramEnrollment', 'projectId', 'CSV:Enrollment.ProjectID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'ProjectID', 3),
('ProgramEnrollment', 'householdId', 'CSV:Enrollment.HouseholdID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'HouseholdID', 4),
('ProgramEnrollment', 'enrollmentCoC', 'CSV:Enrollment.EnrollmentCoC', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'EnrollmentCoC', 11),
('ProgramEnrollment', 'disablingCondition', 'CSV:Enrollment.DisablingCondition', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DisablingCondition', 18),
('ProgramEnrollment', 'createdDate', 'CSV:Enrollment.DateCreated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateCreated', 101),
('ProgramEnrollment', 'updatedDate', 'CSV:Enrollment.DateUpdated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateUpdated', 102),
('ProgramEnrollment', 'userId', 'CSV:Enrollment.UserID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'UserID', 103);

-- =============================================================================
-- Exit.csv
-- =============================================================================
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ProgramEnrollment', 'exitId', 'CSV:Exit.ExitID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'ExitID', 1),
('ProgramEnrollment', 'enrollmentId', 'CSV:Exit.EnrollmentID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'EnrollmentID', 2),
('ProgramEnrollment', 'clientId', 'CSV:Exit.PersonalID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'PersonalID', 3),
('ProgramEnrollment', 'projectCompletionStatus', 'CSV:Exit.ProjectCompletionStatus', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'C', 'ProjectCompletionStatus', 11),
('ProgramEnrollment', 'earlyExitReason', 'CSV:Exit.EarlyExitReason', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'C', 'EarlyExitReason', 12),
('ProgramEnrollment', 'exchangeForSex', 'CSV:Exit.ExchangeForSex', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'C', 'ExchangeForSex', 13),
('ProgramEnrollment', 'cmExitReason', 'CSV:Exit.CMExitReason', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'C', 'CMExitReason', 14),
('ProgramEnrollment', 'createdDate', 'CSV:Exit.DateCreated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateCreated', 101),
('ProgramEnrollment', 'updatedDate', 'CSV:Exit.DateUpdated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateUpdated', 102),
('ProgramEnrollment', 'userId', 'CSV:Exit.UserID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'UserID', 103);

-- =============================================================================
-- Services.csv (VAWA-sensitive for DV services)
-- =============================================================================
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, vawa_suppression_behavior, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order, justification) VALUES
('ServiceEpisode', 'serviceId', 'CSV:Services.ServicesID', 'HMIS_CSV', 'String', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'ServicesID', 1, 'VAWA-protected when service type is Health & DV (14) or Safety Planning Counseling'),
('ServiceEpisode', 'enrollmentId', 'CSV:Services.EnrollmentID', 'HMIS_CSV', 'String', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'EnrollmentID', 2, 'VAWA-protected when service type is DV-related'),
('ServiceEpisode', 'clientId', 'CSV:Services.PersonalID', 'HMIS_CSV', 'String', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'PersonalID', 3, 'VAWA-protected when service type is DV-related'),
('ServiceEpisode', 'recordType', 'CSV:Services.RecordType', 'HMIS_CSV', 'Integer', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'RecordType', 4, 'VAWA-protected when service type is DV-related'),
('ServiceEpisode', 'serviceDate', 'CSV:Services.DateProvided', 'HMIS_CSV', 'Date', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'DateProvided', 5, 'VAWA-protected when service type is DV-related'),
('ServiceEpisode', 'typeProvided', 'CSV:Services.TypeProvided', 'HMIS_CSV', 'Integer', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'C', 'TypeProvided', 6, 'Type 14 (Health & DV) is VAWA-protected'),
('ServiceEpisode', 'otherTypeProvided', 'CSV:Services.OtherTypeProvided', 'HMIS_CSV', 'String', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'C', 'OtherTypeProvided', 7, 'VAWA-protected when service type is DV-related'),
('ServiceEpisode', 'subTypeProvided', 'CSV:Services.SubTypeProvided', 'HMIS_CSV', 'Integer', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'C', 'SubTypeProvided', 8, 'DV sub-types are VAWA-protected'),
('ServiceEpisode', 'faAmount', 'CSV:Services.FAAmount', 'HMIS_CSV', 'Decimal', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'C', 'FAAmount', 9, 'Financial assistance for DV services is VAWA-protected'),
('ServiceEpisode', 'referralOutcome', 'CSV:Services.ReferralOutcome', 'HMIS_CSV', 'Integer', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'C', 'ReferralOutcome', 10, 'DV referrals are VAWA-protected'),
('ServiceEpisode', 'createdDate', 'CSV:Services.DateCreated', 'HMIS_CSV', 'DateTime', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'DateCreated', 101, 'VAWA-protected when service type is DV-related'),
('ServiceEpisode', 'updatedDate', 'CSV:Services.DateUpdated', 'HMIS_CSV', 'DateTime', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'DateUpdated', 102, 'VAWA-protected when service type is DV-related'),
('ServiceEpisode', 'userId', 'CSV:Services.UserID', 'HMIS_CSV', 'String', 'NONE', true, 'SUPPRESS', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'UserID', 103, 'VAWA-protected when service type is DV-related');

-- =============================================================================
-- CurrentLivingSituation.csv identifiers
-- =============================================================================
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, vawa_suppression_behavior, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order, justification) VALUES
('CurrentLivingSituation', 'currentLivingSituationId', 'CSV:CurrentLivingSituation.CurrentLivingSitID', 'HMIS_CSV', 'String', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'CurrentLivingSitID', 1, 'VAWA-protected when linked to DV victim'),
('CurrentLivingSituation', 'enrollmentId', 'CSV:CurrentLivingSituation.EnrollmentID', 'HMIS_CSV', 'String', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'EnrollmentID', 2, 'VAWA-protected when linked to DV victim'),
('CurrentLivingSituation', 'clientId', 'CSV:CurrentLivingSituation.PersonalID', 'HMIS_CSV', 'String', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'PersonalID', 3, 'VAWA-protected when linked to DV victim'),
('CurrentLivingSituation', 'createdDate', 'CSV:CurrentLivingSituation.DateCreated', 'HMIS_CSV', 'DateTime', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'DateCreated', 101, 'VAWA-protected when linked to DV victim'),
('CurrentLivingSituation', 'updatedDate', 'CSV:CurrentLivingSituation.DateUpdated', 'HMIS_CSV', 'DateTime', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'DateUpdated', 102, 'VAWA-protected when linked to DV victim'),
('CurrentLivingSituation', 'userId', 'CSV:CurrentLivingSituation.UserID', 'HMIS_CSV', 'String', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'UserID', 103, 'VAWA-protected when linked to DV victim'),
('CurrentLivingSituation', 'dataCollectionStage', 'CSV:CurrentLivingSituation.DataCollectionStage', 'HMIS_CSV', 'Integer', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 CSV Spec - VAWA Protected', 'R', 'DataCollectionStage', 104, 'VAWA-protected when linked to DV victim');

COMMENT ON TABLE reporting_field_mapping IS 'HUD 2024 enrollment segments (Client, Enrollment, Exit, Services) per HDX CSV Format Specification. Services and CurrentLivingSituation flagged as VAWA-sensitive for DV-related records.';
