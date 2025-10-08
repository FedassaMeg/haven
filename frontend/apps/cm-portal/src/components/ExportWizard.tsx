import React, { useState, useEffect } from 'react';
import {
  Stepper,
  Step,
  StepLabel,
  Button,
  Box,
  Card,
  CardContent,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  FormControlLabel,
  Checkbox,
  Alert,
  Chip,
  List,
  ListItem,
  ListItemText,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { exportApi, ReportType, EligibleProject, ConsentWarning, ExportJobStatus } from '@haven/api-client';
import { PolicyDecisionAuditView } from './PolicyDecisionAuditView';

const steps = ['Report Selection', 'Period Configuration', 'Project Filters', 'Consent Review', 'Confirmation'];

export const ExportWizard: React.FC = () => {
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Step 1: Report Selection
  const [reportType, setReportType] = useState<ReportType>('CoC_APR');

  // Step 2: Period Configuration
  const [operatingYearStart, setOperatingYearStart] = useState<Date | null>(null);
  const [operatingYearEnd, setOperatingYearEnd] = useState<Date | null>(null);

  // Step 3: Project Filters
  const [eligibleProjects, setEligibleProjects] = useState<EligibleProject[]>([]);
  const [selectedProjectIds, setSelectedProjectIds] = useState<string[]>([]);
  const [includeAggregateOnly, setIncludeAggregateOnly] = useState(false);

  // Step 4: Consent Review
  const [consentWarnings, setConsentWarnings] = useState<ConsentWarning[]>([]);
  const [hasBlockingIssues, setHasBlockingIssues] = useState(false);
  const [exportJobId, setExportJobId] = useState<string | null>(null);

  // Step 5: Confirmation
  const [exportStatus, setExportStatus] = useState<ExportJobStatus | null>(null);
  const [showPolicyAudit, setShowPolicyAudit] = useState(false);

  useEffect(() => {
    if (activeStep === 2) {
      loadEligibleProjects();
    }
  }, [activeStep, reportType]);

  const loadEligibleProjects = async () => {
    setLoading(true);
    setError(null);
    try {
      const projects = await exportApi.getEligibleProjects(reportType);
      setEligibleProjects(projects);
    } catch (err) {
      setError('Failed to load eligible projects: ' + (err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const handleNext = async () => {
    if (activeStep === 2) {
      // After project selection, create export configuration and check consent
      await createExportAndCheckConsent();
    } else if (activeStep === 3) {
      // After consent review, proceed to confirmation
      setActiveStep((prevStep) => prevStep + 1);
    } else if (activeStep === 4) {
      // Final step - no action needed
      return;
    } else {
      setActiveStep((prevStep) => prevStep + 1);
    }
  };

  const handleBack = () => {
    setActiveStep((prevStep) => prevStep - 1);
  };

  const handleReset = () => {
    setActiveStep(0);
    setReportType('CoC_APR');
    setOperatingYearStart(null);
    setOperatingYearEnd(null);
    setSelectedProjectIds([]);
    setIncludeAggregateOnly(false);
    setConsentWarnings([]);
    setExportJobId(null);
    setExportStatus(null);
    setError(null);
  };

  const createExportAndCheckConsent = async () => {
    setLoading(true);
    setError(null);

    try {
      // Create export configuration
      const response = await exportApi.createExportConfiguration({
        reportType,
        operatingYearStart: operatingYearStart?.toISOString().split('T')[0] || '',
        operatingYearEnd: operatingYearEnd?.toISOString().split('T')[0] || '',
        projectIds: selectedProjectIds,
        includeAggregateOnly,
      });

      if (response.messages && response.messages.length > 0) {
        const hasErrors = response.messages.some(m => m.toLowerCase().includes('error') || m.toLowerCase().includes('invalid'));
        if (hasErrors) {
          setError(response.messages.join(', '));
          setLoading(false);
          return;
        }
      }

      setExportJobId(response.exportJobId);

      // Check for consent warnings
      const warningsResponse = await exportApi.getConsentWarnings(response.exportJobId);
      setConsentWarnings(warningsResponse.warnings);
      setHasBlockingIssues(warningsResponse.hasBlockingIssues);

      // Move to consent review step
      setActiveStep((prevStep) => prevStep + 1);

      // Start polling export status
      pollExportStatus(response.exportJobId);
    } catch (err) {
      setError('Failed to create export: ' + (err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const pollExportStatus = async (jobId: string) => {
    try {
      await exportApi.pollExportJobStatus(
        jobId,
        (status) => {
          setExportStatus(status);
        },
        2000,
        150
      );
    } catch (err) {
      setError('Export job polling failed: ' + (err as Error).message);
    }
  };

  const handleCancelExport = async () => {
    if (!exportJobId) return;

    setLoading(true);
    try {
      await exportApi.cancelExportJob(exportJobId);
      setError('Export job cancelled');
      handleReset();
    } catch (err) {
      setError('Failed to cancel export: ' + (err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const handleRetryExport = async () => {
    if (!exportJobId) return;

    setLoading(true);
    try {
      await exportApi.retryExportJob(exportJobId);
      pollExportStatus(exportJobId);
    } catch (err) {
      setError('Failed to retry export: ' + (err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async () => {
    if (!exportJobId) return;

    try {
      const downloadUrl = await exportApi.downloadExport(exportJobId);
      window.open(downloadUrl, '_blank');
    } catch (err) {
      setError('Failed to download export: ' + (err as Error).message);
    }
  };

  const renderStepContent = () => {
    switch (activeStep) {
      case 0:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Select Report Type
            </Typography>
            <FormControl fullWidth sx={{ mt: 2 }}>
              <InputLabel>Report Type</InputLabel>
              <Select
                value={reportType}
                label="Report Type"
                onChange={(e) => setReportType(e.target.value as ReportType)}
              >
                <MenuItem value="CoC_APR">CoC Annual Performance Report (APR)</MenuItem>
                <MenuItem value="ESG_CAPER">ESG CAPER</MenuItem>
                <MenuItem value="SPM">System Performance Measures (SPM)</MenuItem>
                <MenuItem value="PIT">Point-in-Time Count (PIT)</MenuItem>
                <MenuItem value="HIC">Housing Inventory Count (HIC)</MenuItem>
              </Select>
            </FormControl>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              {reportType === 'CoC_APR' && 'Annual report for CoC program compliance (Oct 1 - Sep 30)'}
              {reportType === 'ESG_CAPER' && 'Consolidated Annual Performance and Evaluation Report for ESG'}
              {reportType === 'SPM' && 'System-wide performance metrics for CoC'}
              {reportType === 'PIT' && 'Point-in-Time count of homeless persons'}
              {reportType === 'HIC' && 'Inventory of homeless housing and services'}
            </Typography>
          </Box>
        );

      case 1:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Configure Reporting Period
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              {reportType === 'CoC_APR'
                ? 'CoC APR requires a full operating year (Oct 1 - Sep 30)'
                : 'Select the reporting period for this export'}
            </Typography>
            <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
              <DatePicker
                label="Operating Year Start"
                value={operatingYearStart}
                onChange={setOperatingYearStart}
                slotProps={{ textField: { fullWidth: true } }}
              />
              <DatePicker
                label="Operating Year End"
                value={operatingYearEnd}
                onChange={setOperatingYearEnd}
                slotProps={{ textField: { fullWidth: true } }}
              />
            </Box>
            {reportType === 'CoC_APR' && (
              <Alert severity="info" sx={{ mt: 2 }}>
                CoC APR operating year must be October 1 through September 30
              </Alert>
            )}
          </Box>
        );

      case 2:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Select Projects
            </Typography>
            <FormControlLabel
              control={
                <Checkbox
                  checked={includeAggregateOnly}
                  onChange={(e) => setIncludeAggregateOnly(e.target.checked)}
                />
              }
              label="Include aggregate-only data (excludes individual client records)"
            />
            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                <CircularProgress />
              </Box>
            ) : (
              <DataGrid
                rows={eligibleProjects}
                columns={projectColumns}
                getRowId={(row) => row.projectId}
                checkboxSelection
                onRowSelectionModelChange={(newSelection) => {
                  setSelectedProjectIds(newSelection as string[]);
                }}
                rowSelectionModel={selectedProjectIds}
                autoHeight
                sx={{ mt: 2 }}
              />
            )}
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              {selectedProjectIds.length} project(s) selected
            </Typography>
          </Box>
        );

      case 3:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              VAWA Consent Review
            </Typography>
            {consentWarnings.length === 0 ? (
              <Alert severity="success">
                No VAWA consent issues detected. All selected projects are ready for export.
              </Alert>
            ) : (
              <>
                <Alert severity={hasBlockingIssues ? 'error' : 'warning'} sx={{ mb: 2 }}>
                  {hasBlockingIssues
                    ? `Found ${consentWarnings.length} client(s) with VAWA consent issues blocking individual-level data export.`
                    : `Found ${consentWarnings.length} client(s) with VAWA consent warnings.`}
                  {hasBlockingIssues && !includeAggregateOnly && (
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      You must either resolve these consent issues or enable aggregate-only mode to proceed.
                    </Typography>
                  )}
                </Alert>
                <DataGrid
                  rows={consentWarnings}
                  columns={consentWarningColumns}
                  getRowId={(row) => row.clientId}
                  autoHeight
                />
                <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
                  <Button
                    variant="outlined"
                    onClick={() => setShowPolicyAudit(true)}
                  >
                    View Policy Decision Audit
                  </Button>
                  {hasBlockingIssues && (
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={includeAggregateOnly}
                          onChange={(e) => setIncludeAggregateOnly(e.target.checked)}
                        />
                      }
                      label="Switch to aggregate-only mode"
                    />
                  )}
                </Box>
              </>
            )}
          </Box>
        );

      case 4:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Export Status
            </Typography>
            {exportStatus && (
              <Card sx={{ mt: 2 }}>
                <CardContent>
                  <Typography variant="body1" gutterBottom>
                    <strong>Job ID:</strong> {exportStatus.exportJobId}
                  </Typography>
                  <Typography variant="body1" gutterBottom>
                    <strong>State:</strong>{' '}
                    <Chip
                      label={exportStatus.state}
                      color={
                        exportStatus.state === 'COMPLETE'
                          ? 'success'
                          : exportStatus.state === 'FAILED'
                          ? 'error'
                          : 'primary'
                      }
                    />
                  </Typography>
                  <Typography variant="body1" gutterBottom>
                    <strong>Report Type:</strong> {exportStatus.exportType}
                  </Typography>
                  <Typography variant="body1" gutterBottom>
                    <strong>Period:</strong> {exportStatus.reportingPeriodStart} to{' '}
                    {exportStatus.reportingPeriodEnd}
                  </Typography>
                  {exportStatus.totalRecords !== undefined && (
                    <Typography variant="body1" gutterBottom>
                      <strong>Total Records:</strong> {exportStatus.totalRecords}
                    </Typography>
                  )}
                  {exportStatus.vawaSupressedRecords !== undefined && (
                    <Typography variant="body1" gutterBottom>
                      <strong>VAWA Suppressed Records:</strong> {exportStatus.vawaSupressedRecords}
                    </Typography>
                  )}
                  {exportStatus.sha256Hash && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                      <strong>SHA-256:</strong> {exportStatus.sha256Hash}
                    </Typography>
                  )}
                  {exportStatus.errorMessage && (
                    <Alert severity="error" sx={{ mt: 2 }}>
                      {exportStatus.errorMessage}
                    </Alert>
                  )}
                </CardContent>
              </Card>
            )}
            {exportStatus?.state === 'COMPLETE' && (
              <Box sx={{ mt: 2 }}>
                <Button variant="contained" color="primary" onClick={handleDownload}>
                  Download Export
                </Button>
              </Box>
            )}
            {exportStatus?.state === 'FAILED' && (
              <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
                <Button variant="contained" color="primary" onClick={handleRetryExport}>
                  Retry Export
                </Button>
                <Button variant="outlined" onClick={handleReset}>
                  Start New Export
                </Button>
              </Box>
            )}
            {exportStatus?.state !== 'COMPLETE' && exportStatus?.state !== 'FAILED' && (
              <Box sx={{ mt: 2, display: 'flex', gap: 2, alignItems: 'center' }}>
                <CircularProgress size={24} />
                <Typography>Processing export...</Typography>
                <Button variant="outlined" color="error" onClick={handleCancelExport}>
                  Cancel
                </Button>
              </Box>
            )}
          </Box>
        );

      default:
        return null;
    }
  };

  const isStepComplete = () => {
    switch (activeStep) {
      case 0:
        return reportType !== '';
      case 1:
        return operatingYearStart !== null && operatingYearEnd !== null;
      case 2:
        return selectedProjectIds.length > 0;
      case 3:
        return !hasBlockingIssues || includeAggregateOnly;
      default:
        return true;
    }
  };

  return (
    <Card>
      <CardContent>
        <Typography variant="h5" gutterBottom>
          HUD HMIS Export Wizard
        </Typography>
        <Stepper activeStep={activeStep} sx={{ mt: 3, mb: 3 }}>
          {steps.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {renderStepContent()}

        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
          <Button disabled={activeStep === 0} onClick={handleBack}>
            Back
          </Button>
          <Box sx={{ display: 'flex', gap: 1 }}>
            {activeStep === steps.length - 1 ? (
              <Button variant="contained" onClick={handleReset}>
                Start New Export
              </Button>
            ) : (
              <Button
                variant="contained"
                onClick={handleNext}
                disabled={!isStepComplete() || loading}
              >
                {activeStep === 2 ? 'Create Export & Check Consent' : 'Next'}
              </Button>
            )}
          </Box>
        </Box>
      </CardContent>

      {/* Policy Decision Audit Dialog */}
      <Dialog
        open={showPolicyAudit}
        onClose={() => setShowPolicyAudit(false)}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>Policy Decision Audit Trail</DialogTitle>
        <DialogContent>
          <PolicyDecisionAuditView />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowPolicyAudit(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
};

// DataGrid columns for eligible projects
const projectColumns: GridColDef[] = [
  {
    field: 'projectName',
    headerName: 'Project Name',
    width: 250,
    flex: 1,
  },
  {
    field: 'projectType',
    headerName: 'Type',
    width: 150,
  },
  {
    field: 'hudProjectId',
    headerName: 'HUD Project ID',
    width: 150,
  },
  {
    field: 'userHasAccess',
    headerName: 'Access',
    width: 100,
    renderCell: (params) => (
      <Chip
        label={params.value ? 'Yes' : 'No'}
        color={params.value ? 'success' : 'error'}
        size="small"
      />
    ),
  },
];

// DataGrid columns for consent warnings
const consentWarningColumns: GridColDef[] = [
  {
    field: 'clientInitials',
    headerName: 'Client',
    width: 100,
  },
  {
    field: 'warningType',
    headerName: 'Warning Type',
    width: 150,
  },
  {
    field: 'consentStatus',
    headerName: 'Status',
    width: 100,
    renderCell: (params) => (
      <Chip
        label={params.value}
        color={params.value === 'DENIED' ? 'error' : 'warning'}
        size="small"
      />
    ),
  },
  {
    field: 'affectedDataElements',
    headerName: 'Affected Data Elements',
    width: 250,
    flex: 1,
  },
  {
    field: 'recommendation',
    headerName: 'Recommendation',
    width: 300,
  },
];
