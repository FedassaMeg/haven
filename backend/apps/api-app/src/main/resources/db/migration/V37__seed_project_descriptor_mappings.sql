-- V37: Seed Project Descriptor Data Mappings
-- HUD HMIS Data Standards 2024 - Project.csv and related descriptors
-- Project-level metadata for HMIS exports

-- =============================================================================
-- Project.csv - Project Descriptor Data
-- =============================================================================

-- Project Identifiers and Basic Info
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'projectId', 'CSV:Project.ProjectID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'ProjectID', 1),
('Project', 'organizationId', 'CSV:Project.OrganizationID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'OrganizationID', 2),
('Project', 'projectName', 'CSV:Project.ProjectName', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'ProjectName', 3),
('Project', 'projectCommonName', 'CSV:Project.ProjectCommonName', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'O', 'ProjectCommonName', 4);

-- Project Type and Operation Dates (2.02)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'projectType', 'CSV:Project.ProjectType', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.02.1', 'R', 'ProjectType', 5),
('Project', 'operatingStartDate', 'CSV:Project.OperatingStartDate', 'HMIS_CSV', 'Date', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'OperatingStartDate', 6),
('Project', 'operatingEndDate', 'CSV:Project.OperatingEndDate', 'HMIS_CSV', 'Date', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'O', 'OperatingEndDate', 7);

-- Continuum of Care Code (2.03)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'continuumOfCareCode', 'CSV:Project.ContinuumProject', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03', 'R', 'ContinuumProject', 8);

-- HMIS Participation (2.04)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'hmisParticipatingProject', 'CSV:Project.HMISParticipatingProject', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.04', 'R', 'HMISParticipatingProject', 9);

-- Project Tracking Method (2.05.1)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'trackingMethod', 'CSV:Project.TrackingMethod', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.05.1', 'C', 'TrackingMethod', 10);

-- Target Population (2.06)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'targetPopulation', 'CSV:Project.TargetPopulation', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.06', 'C', 'TargetPopulation', 11);

-- PITCount (2.07) - Point-in-Time participation
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'pitCount', 'CSV:Project.PITCount', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.07', 'C', 'PITCount', 12);

-- Housing Type (2.08)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'housingType', 'CSV:Project.HousingType', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.08', 'C', 'HousingType', 13);

-- Residential Affiliation (2.09)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'residentialAffiliation', 'CSV:Project.ResidentialAffiliation', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.09', 'C', 'ResidentialAffiliation', 14);

-- HOPWA Medical Assistance Site (2.10)
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'hopwaMedAssistanceSiteType', 'CSV:Project.HOPWAMedAssistedLivingFac', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.10', 'C', 'HOPWAMedAssistedLivingFac', 15);

-- Metadata
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Project', 'createdDate', 'CSV:Project.DateCreated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateCreated', 101),
('Project', 'updatedDate', 'CSV:Project.DateUpdated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateUpdated', 102),
('Project', 'userId', 'CSV:Project.UserID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'UserID', 103),
('Project', 'exportId', 'CSV:Project.ExportID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'ExportID', 104);

-- =============================================================================
-- Organization.csv - Organization Descriptor Data
-- =============================================================================
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('Organization', 'organizationId', 'CSV:Organization.OrganizationID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'OrganizationID', 1),
('Organization', 'organizationName', 'CSV:Organization.OrganizationName', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'OrganizationName', 2),
('Organization', 'victimServiceProvider', 'CSV:Organization.VictimServiceProvider', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - VSP Flag', 'R', 'VictimServiceProvider', 3),
('Organization', 'organizationCommonName', 'CSV:Organization.OrganizationCommonName', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'O', 'OrganizationCommonName', 4),
('Organization', 'createdDate', 'CSV:Organization.DateCreated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateCreated', 101),
('Organization', 'updatedDate', 'CSV:Organization.DateUpdated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateUpdated', 102),
('Organization', 'userId', 'CSV:Organization.UserID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'UserID', 103),
('Organization', 'exportId', 'CSV:Organization.ExportID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'ExportID', 104);

-- =============================================================================
-- Funder.csv - Funding Source Descriptor Data (2.11)
-- =============================================================================
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ProjectFunder', 'funderId', 'CSV:Funder.FunderID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.11', 'R', 'FunderID', 1),
('ProjectFunder', 'projectId', 'CSV:Funder.ProjectID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.11', 'R', 'ProjectID', 2),
('ProjectFunder', 'funder', 'CSV:Funder.Funder', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.11.1', 'R', 'Funder', 3),
('ProjectFunder', 'otherFunder', 'CSV:Funder.OtherFunder', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.11.2', 'C', 'OtherFunder', 4),
('ProjectFunder', 'grantId', 'CSV:Funder.GrantID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.11.3', 'C', 'GrantID', 5),
('ProjectFunder', 'startDate', 'CSV:Funder.StartDate', 'HMIS_CSV', 'Date', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.11.4', 'R', 'StartDate', 6),
('ProjectFunder', 'endDate', 'CSV:Funder.EndDate', 'HMIS_CSV', 'Date', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.11.5', 'C', 'EndDate', 7),
('ProjectFunder', 'createdDate', 'CSV:Funder.DateCreated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateCreated', 101),
('ProjectFunder', 'updatedDate', 'CSV:Funder.DateUpdated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateUpdated', 102),
('ProjectFunder', 'userId', 'CSV:Funder.UserID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'UserID', 103),
('ProjectFunder', 'exportId', 'CSV:Funder.ExportID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'ExportID', 104);

-- =============================================================================
-- ProjectCoC.csv - Project CoC Information (2.03)
-- =============================================================================
INSERT INTO reporting_field_mapping (source_entity, source_field, target_hud_element_id, hud_specification_type, target_data_type, transform_language, vawa_sensitive_field, effective_from, hud_notice_reference, required_flag, csv_field_name, csv_field_order) VALUES
('ProjectCoC', 'projectCoCId', 'CSV:ProjectCoC.ProjectCoCID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03', 'R', 'ProjectCoCID', 1),
('ProjectCoC', 'projectId', 'CSV:ProjectCoC.ProjectID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03', 'R', 'ProjectID', 2),
('ProjectCoC', 'cocCode', 'CSV:ProjectCoC.CoCCode', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03.1', 'R', 'CoCCode', 3),
('ProjectCoC', 'geocode', 'CSV:ProjectCoC.Geocode', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03.3', 'O', 'Geocode', 4),
('ProjectCoC', 'address1', 'CSV:ProjectCoC.Address1', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03.4', 'C', 'Address1', 5),
('ProjectCoC', 'address2', 'CSV:ProjectCoC.Address2', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03.4', 'O', 'Address2', 6),
('ProjectCoC', 'city', 'CSV:ProjectCoC.City', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03.4', 'C', 'City', 7),
('ProjectCoC', 'state', 'CSV:ProjectCoC.State', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03.4', 'C', 'State', 8),
('ProjectCoC', 'zip', 'CSV:ProjectCoC.ZIP', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03.4', 'C', 'ZIP', 9),
('ProjectCoC', 'geographyType', 'CSV:ProjectCoC.GeographyType', 'HMIS_CSV', 'Integer', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec - Element 2.03.5', 'C', 'GeographyType', 10),
('ProjectCoC', 'createdDate', 'CSV:ProjectCoC.DateCreated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateCreated', 101),
('ProjectCoC', 'updatedDate', 'CSV:ProjectCoC.DateUpdated', 'HMIS_CSV', 'DateTime', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'DateUpdated', 102),
('ProjectCoC', 'userId', 'CSV:ProjectCoC.UserID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'UserID', 103),
('ProjectCoC', 'exportId', 'CSV:ProjectCoC.ExportID', 'HMIS_CSV', 'String', 'NONE', false, '2024-10-01', 'HDX 2024 CSV Spec', 'R', 'ExportID', 104);

COMMENT ON TABLE reporting_field_mapping IS 'HUD 2024 Project Descriptor mappings per HDX CSV Format Specification. Covers Project, Organization, Funder, and ProjectCoC entities.';
