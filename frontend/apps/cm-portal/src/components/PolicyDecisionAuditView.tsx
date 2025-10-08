import React, { useState, useEffect } from 'react';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { Box, Card, CardContent, Typography, Chip, TextField, Button } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';

interface PolicyDecision {
  decisionId: string;
  allowed: boolean;
  reason: string;
  policyRule: string;
  userId: string;
  userName: string;
  resourceId: string;
  resourceType: string;
  decidedAt: string;
  ipAddress: string;
}

export const PolicyDecisionAuditView: React.FC = () => {
  const [decisions, setDecisions] = useState<PolicyDecision[]>([]);
  const [loading, setLoading] = useState(false);
  const [startDate, setStartDate] = useState<Date | null>(null);
  const [endDate, setEndDate] = useState<Date | null>(null);
  const [filterUserId, setFilterUserId] = useState('');

  const columns: GridColDef[] = [
    {
      field: 'decidedAt',
      headerName: 'Timestamp',
      width: 180,
      valueFormatter: (params) => new Date(params.value).toLocaleString()
    },
    {
      field: 'allowed',
      headerName: 'Decision',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value ? 'ALLOW' : 'DENY'}
          color={params.value ? 'success' : 'error'}
          size="small"
        />
      )
    },
    {
      field: 'userName',
      headerName: 'User',
      width: 150
    },
    {
      field: 'resourceType',
      headerName: 'Resource Type',
      width: 150
    },
    {
      field: 'policyRule',
      headerName: 'Policy Rule',
      width: 200
    },
    {
      field: 'reason',
      headerName: 'Reason',
      width: 300,
      flex: 1
    },
    {
      field: 'ipAddress',
      headerName: 'IP Address',
      width: 130
    }
  ];

  const fetchPolicyDecisions = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (startDate) params.append('startDate', startDate.toISOString());
      if (endDate) params.append('endDate', endDate.toISOString());
      if (filterUserId) params.append('userId', filterUserId);

      const response = await fetch(`/api/admin/policy-decisions?${params.toString()}`);
      const data = await response.json();
      setDecisions(data);
    } catch (error) {
      console.error('Error fetching policy decisions:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchDeniedAccess = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/admin/policy-decisions/denied');
      const data = await response.json();
      setDecisions(data);
    } catch (error) {
      console.error('Error fetching denied access:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPolicyDecisions();
  }, []);

  return (
    <Card>
      <CardContent>
        <Typography variant="h5" gutterBottom>
          Policy Decision Audit Trail
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph>
          Centralized audit log of all access control decisions for compliance reporting
        </Typography>

        <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
          <DatePicker
            label="Start Date"
            value={startDate}
            onChange={setStartDate}
            slotProps={{ textField: { size: 'small' } }}
          />
          <DatePicker
            label="End Date"
            value={endDate}
            onChange={setEndDate}
            slotProps={{ textField: { size: 'small' } }}
          />
          <TextField
            label="User ID"
            size="small"
            value={filterUserId}
            onChange={(e) => setFilterUserId(e.target.value)}
          />
          <Button variant="contained" onClick={fetchPolicyDecisions}>
            Search
          </Button>
          <Button variant="outlined" color="error" onClick={fetchDeniedAccess}>
            Show Denied Only
          </Button>
        </Box>

        <DataGrid
          rows={decisions}
          columns={columns}
          getRowId={(row) => row.decisionId}
          loading={loading}
          autoHeight
          pageSizeOptions={[10, 25, 50, 100]}
          initialState={{
            pagination: { paginationModel: { pageSize: 25 } },
            sorting: { sortModel: [{ field: 'decidedAt', sort: 'desc' }] }
          }}
        />
      </CardContent>
    </Card>
  );
};
