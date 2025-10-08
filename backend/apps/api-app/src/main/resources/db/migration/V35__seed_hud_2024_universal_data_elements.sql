-- V35: Seed HUD 2024 Universal Data Elements (UDE) Mappings
-- Based on HUD HMIS Data Standards 2024 v1.0 (effective October 1, 2024)
-- Universal Data Elements: 3.01 - 3.917

-- 3.01 Name
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ClientProfile', 'firstName', 'CSV:Client.FirstName', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'FirstName', 3),
('ClientProfile', 'middleName', 'CSV:Client.MiddleName', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'O', 'MiddleName', 4),
('ClientProfile', 'lastName', 'CSV:Client.LastName', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'LastName', 5),
('ClientProfile', 'nameSuffix', 'CSV:Client.NameSuffix', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'O', 'NameSuffix', 6),
('ClientProfile', 'nameDataQuality', 'CSV:Client.NameDataQuality', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'NameDataQuality', 7);

-- 3.02 Social Security Number
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_expression, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ClientProfile', 'socialSecurityNumber', 'CSV:Client.SSN', 'HMIS_CSV', 'String', 'REPLACE(socialSecurityNumber, ''-'', '''')', 'SQL', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'SSN', 8),
('ClientProfile', 'ssnDataQuality', 'CSV:Client.SSNDataQuality', 'HMIS_CSV', 'Integer', 'NONE', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'SSNDataQuality', 9);

-- 3.03 Date of Birth
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ClientProfile', 'dateOfBirth', 'CSV:Client.DOB', 'HMIS_CSV', 'Date', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'DOB', 10),
('ClientProfile', 'dobDataQuality', 'CSV:Client.DOBDataQuality', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'DOBDataQuality', 11);

-- 3.04 Race
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ClientProfile', 'raceAmIndAkNative', 'CSV:Client.AmIndAKNative', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'AmIndAKNative', 12),
('ClientProfile', 'raceAsian', 'CSV:Client.Asian', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'Asian', 13),
('ClientProfile', 'raceBlackAfAmerican', 'CSV:Client.BlackAfAmerican', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'BlackAfAmerican', 14),
('ClientProfile', 'raceHispanicLatinaeo', 'CSV:Client.HispanicLatinaeo', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'HispanicLatinaeo', 15),
('ClientProfile', 'raceMidEastNAfrican', 'CSV:Client.MidEastNAfrican', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'MidEastNAfrican', 16),
('ClientProfile', 'raceNatHIPacific', 'CSV:Client.NatHIPacific', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'NatHIPacific', 17),
('ClientProfile', 'raceWhite', 'CSV:Client.White', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'White', 18),
('ClientProfile', 'raceNone', 'CSV:Client.RaceNone', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'C', 'RaceNone', 19);

-- 3.05 Ethnicity
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ClientProfile', 'ethnicity', 'CSV:Client.Ethnicity', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'Ethnicity', 20);

-- 3.06 Gender
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ClientProfile', 'genderWoman', 'CSV:Client.Woman', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'Woman', 21),
('ClientProfile', 'genderMan', 'CSV:Client.Man', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'Man', 22),
('ClientProfile', 'genderNonBinary', 'CSV:Client.NonBinary', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'NonBinary', 23),
('ClientProfile', 'genderCulturallySpecific', 'CSV:Client.CulturallySpecific', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'CulturallySpecific', 24),
('ClientProfile', 'genderTransgender', 'CSV:Client.Transgender', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'Transgender', 25),
('ClientProfile', 'genderQuestioning', 'CSV:Client.Questioning', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'Questioning', 26),
('ClientProfile', 'genderDifferentIdentity', 'CSV:Client.DifferentIdentity', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'DifferentIdentity', 27),
('ClientProfile', 'genderNone', 'CSV:Client.GenderNone', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'C', 'GenderNone', 28);

-- 3.07 Different Identity Text (3.6.D)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ClientProfile', 'differentIdentityText', 'CSV:Client.DifferentIdentityText', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'C', 'DifferentIdentityText', 29);

-- 3.08 Veteran Status
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ClientProfile', 'veteranStatus', 'CSV:Client.VeteranStatus', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'VeteranStatus', 30);

-- 3.10 Project Start Date (Enrollment)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ProgramEnrollment', 'entryDate', 'CSV:Enrollment.EntryDate', 'HMIS_CSV', 'Date', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'EntryDate', 10);

-- 3.11 Project Exit Date
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ProgramEnrollment', 'exitDate', 'CSV:Exit.ExitDate', 'HMIS_CSV', 'Date', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'ExitDate', 8);

-- 3.12 Destination
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ProgramEnrollment', 'destination', 'CSV:Exit.Destination', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'Destination', 9),
('ProgramEnrollment', 'otherDestination', 'CSV:Exit.OtherDestination', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'C', 'OtherDestination', 10);

-- 3.15 Relationship to Head of Household
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ProgramEnrollment', 'relationshipToHoH', 'CSV:Enrollment.RelationshipToHoH', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'RelationshipToHoH', 12);

-- 3.16 Client Location (not collected in Haven - out of scope)

-- 3.20 Housing Move-In Date
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ProgramEnrollment', 'housingMoveInDate', 'CSV:Enrollment.MoveInDate', 'HMIS_CSV', 'Date', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'C', 'MoveInDate', 13);

-- 3.30 Current Living Situation (VAWA-sensitive due to DV indicator linkage)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, vawa_suppression_behavior, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order, justification) VALUES
('CurrentLivingSituation', 'informationDate', 'CSV:CurrentLivingSituation.InformationDate', 'HMIS_CSV', 'Date', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 v1.0 - VAWA Protected', 'R', 'InformationDate', 8, 'VAWA-protected when linked to DV services or CurrentLivingSituation with DV indicators'),
('CurrentLivingSituation', 'currentLivingSituation', 'CSV:CurrentLivingSituation.CurrentLivingSituation', 'HMIS_CSV', 'Integer', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 v1.0 - VAWA Protected', 'R', 'CurrentLivingSituation', 9, 'VAWA-protected when linked to DV services'),
('CurrentLivingSituation', 'verifiedBy', 'CSV:CurrentLivingSituation.VerifiedBy', 'HMIS_CSV', 'Integer', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 v1.0 - VAWA Protected', 'C', 'VerifiedBy', 10, 'VAWA-protected when linked to DV services'),
('CurrentLivingSituation', 'leavingHomelessSituation', 'CSV:CurrentLivingSituation.LeavingHomelessSituation', 'HMIS_CSV', 'Integer', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 v1.0 - VAWA Protected', 'C', 'LeavingHomelessSituation', 11, 'VAWA-protected when linked to DV services'),
('CurrentLivingSituation', 'subsequentResidence', 'CSV:CurrentLivingSituation.SubsequentResidence', 'HMIS_CSV', 'Integer', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 v1.0 - VAWA Protected', 'C', 'SubsequentResidence', 12, 'VAWA-protected when linked to DV services'),
('CurrentLivingSituation', 'resourcesUsed', 'CSV:CurrentLivingSituation.ResourcesUsed', 'HMIS_CSV', 'Integer', 'NONE', true, 'REDACT', '2024-10-01', 'HDX 2024 v1.0 - VAWA Protected', 'C', 'ResourcesUsed', 13, 'VAWA-protected when linked to DV services');

-- 3.917 Prior Living Situation
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ProgramEnrollment', 'priorLivingSituation', 'CSV:Enrollment.LivingSituation', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'LivingSituation', 14),
('ProgramEnrollment', 'lengthOfStay', 'CSV:Enrollment.LengthOfStay', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'R', 'LengthOfStay', 15),
('ProgramEnrollment', 'lohUnderThreshold', 'CSV:Enrollment.LOSUnderThreshold', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'C', 'LOSUnderThreshold', 16),
('ProgramEnrollment', 'previousStreetESSH', 'CSV:Enrollment.PreviousStreetESSH', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 v1.0', 'C', 'PreviousStreetESSH', 17);

COMMENT ON TABLE reporting_field_mapping IS 'HUD 2024 Universal Data Elements (3.01-3.917) seeded per HDX 2024 v1.0 effective October 1, 2024';
