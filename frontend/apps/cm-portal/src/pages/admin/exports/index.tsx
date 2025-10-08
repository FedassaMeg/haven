import React, { useState, useEffect } from 'react';
import { NextPage } from 'next';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Tabs,
  Tab,
  Chip,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import {
  Download as DownloadIcon,
  Refresh as RefreshIcon,
  Cancel as CancelIcon,
  Add as AddIcon,
} from '@mui/icons-material';
import { exportApi, ExportJobSummary, ExportJobState } from '@haven/api-client';
import { ExportWizard } from '../../../components/ExportWizard';

const ExportsPage: NextPage = () => {
  const [exportJobs, setExportJobs] = useState<ExportJobSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedTab, setSelectedTab] = useState<ExportJobState | 'ALL'>('ALL');
  const [showWizard, setShowWizard] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadExportJobs();
  }, [selectedTab]);

  const loadExportJobs = async () => {
    setLoading(true);
    setError(null);
    try {
      const state = selectedTab === 'ALL' ? undefined : selectedTab;
      const jobs = await exportApi.getAllExportJobs(state);
      setExportJobs(jobs);
    } catch (err) {
      setError('Failed to load export jobs: ' + (err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async (exportJobId: string) => {
    try {
      const downloadUrl = await exportApi.downloadExport(exportJobId);
      window.open(downloadUrl, '_blank');
    } catch (err) {
      setError('Failed to download export: ' + (err as Error).message);
    }
  };

  const handleCancel = async (exportJobId: string) => {
    try {
      await exportApi.cancelExportJob(exportJobId);
      loadExportJobs();
    } catch (err) {
      setError('Failed to cancel export: ' + (err as Error).message);
    }
  };

  const handleRetry = async (exportJobId: string) => {
    try {
      await exportApi.retryExportJob(exportJobId);
      loadExportJobs();
    } catch (err) {
      setError('Failed to retry export: ' + (err as Error).message);
    }
  };

  const columns: GridColDef[] = [
    {
      field: 'exportType',
      headerName: 'Report Type',
      width: 150,
    },
    {
      field: 'state',
      headerName: 'Status',
      width: 130,
      renderCell: (params) => (
        <Chip
          label={params.value}
          color={
            params.value === 'COMPLETE'
              ? 'success'
              : params.value === 'FAILED'
              ? 'error'
              : params.value === 'QUEUED'
              ? 'default'
              : 'primary'
          }
          size="small"
        />
      ),
    },
    {
      field: 'reportingPeriodStart',
      headerName: 'Period Start',
      width: 130,
    },
    {
      field: 'reportingPeriodEnd',
      headerName: 'Period End',
      width: 130,
    },
    {
      field: 'projectCount',
      headerName: 'Projects',
      width: 100,
    },
    {
      field: 'queuedAt',
      headerName: 'Queued At',
      width: 180,
      valueFormatter: (params) => new Date(params.value).toLocaleString(),
    },
    {
      field: 'completedAt',
      headerName: 'Completed At',
      width: 180,
      valueFormatter: (params) => (params.value ? new Date(params.value).toLocaleString() : '-'),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      sortable: false,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', gap: 1 }}>
          {params.row.state === 'COMPLETE' && (
            <Tooltip title="Download">
              <IconButton
                size="small"
                color="primary"
                onClick={() => handleDownload(params.row.exportJobId)}
              >
                <DownloadIcon />
              </IconButton>
            </Tooltip>
          )}
          {params.row.state === 'FAILED' && (
            <Tooltip title="Retry">
              <IconButton
                size="small"
                color="primary"
                onClick={() => handleRetry(params.row.exportJobId)}
              >
                <RefreshIcon />
              </IconButton>
            </Tooltip>
          )}
          {(params.row.state === 'QUEUED' ||
            params.row.state === 'MATERIALIZING' ||
            params.row.state === 'VALIDATING') && (
            <Tooltip title="Cancel">
              <IconButton
                size="small"
                color="error"
                onClick={() => handleCancel(params.row.exportJobId)}
              >
                <CancelIcon />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      ),
    },
  ];

  return (
    <Box sx={{ p: 3 }}>
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Typography variant="h5">HUD HMIS Exports</Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setShowWizard(true)}
            >
              New Export
            </Button>
          </Box>

          <Tabs
            value={selectedTab}
            onChange={(_, newValue) => setSelectedTab(newValue)}
            sx={{ mb: 2 }}
          >
            <Tab label="All" value="ALL" />
            <Tab label="Queued" value="QUEUED" />
            <Tab label="In Progress" value="MATERIALIZING" />
            <Tab label="Completed" value="COMPLETE" />
            <Tab label="Failed" value="FAILED" />
          </Tabs>

          {error && (
            <Typography color="error" sx={{ mb: 2 }}>
              {error}
            </Typography>
          )}

          <DataGrid
            rows={exportJobs}
            columns={columns}
            getRowId={(row) => row.exportJobId}
            loading={loading}
            autoHeight
            pageSizeOptions={[10, 25, 50, 100]}
            initialState={{
              pagination: { paginationModel: { pageSize: 25 } },
              sorting: { sortModel: [{ field: 'queuedAt', sort: 'desc' }] },
            }}
          />

          <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={loadExportJobs}
              disabled={loading}
            >
              Refresh
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* Export Wizard Dialog */}
      <Dialog
        open={showWizard}
        onClose={() => setShowWizard(false)}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>Create New Export</DialogTitle>
        <DialogContent>
          <ExportWizard />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowWizard(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ExportsPage;
