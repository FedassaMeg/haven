import React, { useState, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  CircularProgress,
  Tooltip,
  Grid,
  Tabs,
  Tab,
} from '@mui/material';
import {
  Block as BlockIcon,
  Visibility as ViewIcon,
  Download as DownloadIcon,
  History as HistoryIcon,
  Security as SecurityIcon,
  Warning as WarningIcon,
  CheckCircle as CheckIcon,
  Cancel as CancelIcon,
  Schedule as ScheduleIcon,
} from '@mui/icons-material';
import { format, parseISO, differenceInDays } from 'date-fns';
import { useVspExportHistory, useRevokeVspExport } from '../../../hooks/useVspExports';

interface VspShareHistoryProps {
  recipient: string;
  onExportRevoked?: () => void;
}

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`share-history-tabpanel-${index}`}
      aria-labelledby={`share-history-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

export const VspShareHistory: React.FC<VspShareHistoryProps> = ({
  recipient,
  onExportRevoked,
}) => {
  const [tabValue, setTabValue] = useState(0);
  const [revokeDialogOpen, setRevokeDialogOpen] = useState(false);
  const [selectedExportId, setSelectedExportId] = useState<string | null>(null);
  const [revocationReason, setRevocationReason] = useState('');

  const { data: history, loading, error, refetch } = useVspExportHistory(recipient);
  const { mutate: revokeExport, loading: revoking } = useRevokeVspExport();

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleOpenRevokeDialog = (exportId: string) => {
    setSelectedExportId(exportId);
    setRevokeDialogOpen(true);
  };

  const handleCloseRevokeDialog = () => {
    setRevokeDialogOpen(false);
    setSelectedExportId(null);
    setRevocationReason('');
  };

  const handleConfirmRevoke = useCallback(async () => {
    if (!selectedExportId || !revocationReason.trim()) return;

    try {
      await revokeExport({
        exportId: selectedExportId,
        reason: revocationReason,
      });

      handleCloseRevokeDialog();
      refetch();
      onExportRevoked?.();
    } catch (error) {
      console.error('Failed to revoke export:', error);
    }
  }, [selectedExportId, revocationReason, revokeExport, refetch, onExportRevoked]);

  const getStatusChip = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Chip label="Active" color="success" size="small" icon={<CheckIcon />} />;
      case 'REVOKED':
        return <Chip label="Revoked" color="error" size="small" icon={<CancelIcon />} />;
      case 'EXPIRED':
        return <Chip label="Expired" color="default" size="small" icon={<ScheduleIcon />} />;
      default:
        return <Chip label={status} size="small" />;
    }
  };

  const getExpiryWarning = (expiryDate: string | null) => {
    if (!expiryDate) return null;

    const days = differenceInDays(parseISO(expiryDate), new Date());
    if (days <= 0) return null;
    if (days <= 7) {
      return (
        <Tooltip title={`Expires in ${days} days`}>
          <WarningIcon color="warning" fontSize="small" />
        </Tooltip>
      );
    }
    return null;
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" p={3}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error">
        Failed to load share history: {error.message}
      </Alert>
    );
  }

  if (!history) {
    return <Alert severity="info">No share history available</Alert>;
  }

  const activeExports = history.exports.filter(e => e.status === 'ACTIVE');
  const revokedExports = history.exports.filter(e => e.status === 'REVOKED');
  const expiredExports = history.exports.filter(e => e.status === 'EXPIRED');

  return (
    <>
      <Card>
        <CardContent>
          <Box display="flex" alignItems="center" mb={2}>
            <HistoryIcon sx={{ mr: 1 }} />
            <Typography variant="h6">VSP Export History</Typography>
          </Box>

          {/* Statistics Summary */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={3}>
              <Card variant="outlined">
                <CardContent>
                  <Typography color="textSecondary" gutterBottom>
                    Total Exports
                  </Typography>
                  <Typography variant="h4">{history.totalExports}</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={3}>
              <Card variant="outlined">
                <CardContent>
                  <Typography color="textSecondary" gutterBottom>
                    Active
                  </Typography>
                  <Typography variant="h4" color="success.main">
                    {history.activeExports}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={3}>
              <Card variant="outlined">
                <CardContent>
                  <Typography color="textSecondary" gutterBottom>
                    Revoked
                  </Typography>
                  <Typography variant="h4" color="error.main">
                    {history.revokedExports}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={3}>
              <Card variant="outlined">
                <CardContent>
                  <Typography color="textSecondary" gutterBottom>
                    Expired
                  </Typography>
                  <Typography variant="h4" color="text.secondary">
                    {history.expiredExports}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Tabs for different export statuses */}
          <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
            <Tabs value={tabValue} onChange={handleTabChange}>
              <Tab label={`All Exports (${history.exports.length})`} />
              <Tab label={`Active (${activeExports.length})`} />
              <Tab label={`Revoked (${revokedExports.length})`} />
              <Tab label={`Expired (${expiredExports.length})`} />
            </Tabs>
          </Box>

          <TabPanel value={tabValue} index={0}>
            <ExportTable
              exports={history.exports}
              onRevoke={handleOpenRevokeDialog}
              getStatusChip={getStatusChip}
              getExpiryWarning={getExpiryWarning}
            />
          </TabPanel>

          <TabPanel value={tabValue} index={1}>
            <ExportTable
              exports={activeExports}
              onRevoke={handleOpenRevokeDialog}
              getStatusChip={getStatusChip}
              getExpiryWarning={getExpiryWarning}
            />
          </TabPanel>

          <TabPanel value={tabValue} index={2}>
            <ExportTable
              exports={revokedExports}
              onRevoke={handleOpenRevokeDialog}
              getStatusChip={getStatusChip}
              getExpiryWarning={getExpiryWarning}
              showRevocationInfo
            />
          </TabPanel>

          <TabPanel value={tabValue} index={3}>
            <ExportTable
              exports={expiredExports}
              onRevoke={handleOpenRevokeDialog}
              getStatusChip={getStatusChip}
              getExpiryWarning={getExpiryWarning}
            />
          </TabPanel>
        </CardContent>
      </Card>

      {/* Revocation Dialog */}
      <Dialog open={revokeDialogOpen} onClose={handleCloseRevokeDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          <Box display="flex" alignItems="center">
            <BlockIcon sx={{ mr: 1 }} />
            Revoke Export
          </Box>
        </DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Revoking an export will notify the recipient and prevent further access to the data.
            This action cannot be undone.
          </Alert>
          <TextField
            label="Revocation Reason"
            multiline
            rows={4}
            fullWidth
            value={revocationReason}
            onChange={(e) => setRevocationReason(e.target.value)}
            placeholder="Please provide a reason for revoking this export..."
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseRevokeDialog} disabled={revoking}>
            Cancel
          </Button>
          <Button
            onClick={handleConfirmRevoke}
            color="error"
            variant="contained"
            disabled={!revocationReason.trim() || revoking}
            startIcon={revoking ? <CircularProgress size={20} /> : <BlockIcon />}
          >
            Revoke Export
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

interface ExportTableProps {
  exports: any[];
  onRevoke: (exportId: string) => void;
  getStatusChip: (status: string) => JSX.Element;
  getExpiryWarning: (expiryDate: string | null) => JSX.Element | null;
  showRevocationInfo?: boolean;
}

const ExportTable: React.FC<ExportTableProps> = ({
  exports,
  onRevoke,
  getStatusChip,
  getExpiryWarning,
  showRevocationInfo = false,
}) => {
  return (
    <TableContainer component={Paper}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Export Date</TableCell>
            <TableCell>CE Hash Key</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Expiry Date</TableCell>
            <TableCell>Share Scopes</TableCell>
            <TableCell>Consent Basis</TableCell>
            {showRevocationInfo && (
              <>
                <TableCell>Revoked By</TableCell>
                <TableCell>Reason</TableCell>
              </>
            )}
            <TableCell align="center">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {exports.map((exp) => (
            <TableRow key={exp.exportId}>
              <TableCell>
                {format(parseISO(exp.exportTimestamp), 'MMM dd, yyyy HH:mm')}
              </TableCell>
              <TableCell>
                <Tooltip title="CE-specific hash key for anonymization">
                  <code>{exp.ceHashKey}</code>
                </Tooltip>
              </TableCell>
              <TableCell>{getStatusChip(exp.status)}</TableCell>
              <TableCell>
                <Box display="flex" alignItems="center">
                  {exp.expiryDate
                    ? format(parseISO(exp.expiryDate), 'MMM dd, yyyy')
                    : 'No expiry'}
                  {getExpiryWarning(exp.expiryDate)}
                </Box>
              </TableCell>
              <TableCell>
                {exp.shareScopes.map((scope: string) => (
                  <Chip
                    key={scope}
                    label={scope}
                    size="small"
                    variant="outlined"
                    sx={{ mr: 0.5, mb: 0.5 }}
                  />
                ))}
              </TableCell>
              <TableCell>{exp.consentBasis}</TableCell>
              {showRevocationInfo && (
                <>
                  <TableCell>{exp.revokedBy || '-'}</TableCell>
                  <TableCell>{exp.revocationReason || '-'}</TableCell>
                </>
              )}
              <TableCell align="center">
                <Tooltip title="View Details">
                  <IconButton size="small">
                    <ViewIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                {exp.status === 'ACTIVE' && (
                  <Tooltip title="Revoke Export">
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => onRevoke(exp.exportId)}
                    >
                      <BlockIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default VspShareHistory;