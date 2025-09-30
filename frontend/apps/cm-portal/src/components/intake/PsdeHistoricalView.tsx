import React, { useState, useEffect } from 'react';
import {
  useIntakePsde,
  usePsdeAccessControl,
  type IntakePsdeResponse,
  type AuditTrailEntry
} from '@haven/api-client';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  History,
  Clock,
  User,
  FileEdit,
  Eye,
  Flag,
  AlertTriangle,
  Shield,
  CheckCircle,
  XCircle,
  Calendar,
  ArrowRight,
  Edit,
  RotateCcw
} from 'lucide-react';
import { PsdeCorrectionWorkflow } from './PsdeCorrectionWorkflow';

interface PsdeHistoricalViewProps {
  enrollmentId: string;
  clientId: string;
  userRoles: string[];
  onRecordSelect?: (record: IntakePsdeResponse) => void;
}

interface HistoricalRecord extends IntakePsdeResponse {
  isFlagged?: boolean;
  flagReason?: string;
  requiresReview?: boolean;
  complianceScore?: number;
}

export function PsdeHistoricalView({
  enrollmentId,
  clientId,
  userRoles,
  onRecordSelect
}: PsdeHistoricalViewProps) {
  const [records, setRecords] = useState<HistoricalRecord[]>([]);
  const [selectedRecord, setSelectedRecord] = useState<HistoricalRecord | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [showCorrectionWorkflow, setShowCorrectionWorkflow] = useState(false);
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [sortField, setSortField] = useState<string>('informationDate');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { getPsdeRecordsForEnrollment, flagRecord } = useIntakePsde();
  const { canAccessDvData, canModifyRedactionLevel } = usePsdeAccessControl();

  useEffect(() => {
    loadHistoricalRecords();
  }, [enrollmentId]);

  const loadHistoricalRecords = async () => {
    try {
      setLoading(true);
      setError(null);

      const allRecords = await getPsdeRecordsForEnrollment(enrollmentId);

      // Enhance records with flagging and compliance data
      const enhancedRecords = await Promise.all(
        allRecords.map(async (record) => {
          const enhanced: HistoricalRecord = {
            ...record,
            isFlagged: await checkIfRecordFlagged(record.recordId),
            flagReason: await getFlagReason(record.recordId),
            requiresReview: checkIfRequiresReview(record),
            complianceScore: calculateComplianceScore(record)
          };
          return enhanced;
        })
      );

      setRecords(enhancedRecords);
    } catch (err) {
      console.error('Error loading historical records:', err);
      setError('Failed to load historical records');
    } finally {
      setLoading(false);
    }
  };

  const checkIfRecordFlagged = async (recordId: string): Promise<boolean> => {
    // TODO: Implement API call to check flag status
    return Math.random() > 0.8; // Mock implementation
  };

  const getFlagReason = async (recordId: string): Promise<string | undefined> => {
    // TODO: Implement API call to get flag reason
    const reasons = [
      'Data quality concerns',
      'VAWA compliance review needed',
      'Unusual correction pattern',
      'Missing supervisor approval'
    ];
    return reasons[Math.floor(Math.random() * reasons.length)];
  };

  const checkIfRequiresReview = (record: IntakePsdeResponse): boolean => {
    return (
      record.isHighSensitivityDvCase ||
      !record.meetsHmisDataQuality ||
      record.lifecycleStatus === 'CORRECTED'
    );
  };

  const calculateComplianceScore = (record: IntakePsdeResponse): number => {
    let score = 0;
    let maxScore = 0;

    // HUD data quality
    maxScore += 30;
    if (record.meetsHmisDataQuality) score += 30;

    // VAWA compliance
    maxScore += 25;
    if (record.vawaConfidentialityRequested && record.dvRedactionLevel !== 'NO_REDACTION') {
      score += 25;
    } else if (!record.vawaConfidentialityRequested) {
      score += 25;
    }

    // Data completeness
    maxScore += 25;
    if (record.totalMonthlyIncome !== null) score += 5;
    if (record.coveredByHealthInsurance !== null) score += 5;
    if (record.domesticViolence !== null) score += 10;
    if (record.physicalDisability !== null) score += 5;

    // Lifecycle integrity
    maxScore += 20;
    if (record.lifecycleStatus === 'ACTIVE') score += 20;
    else if (record.lifecycleStatus === 'SUPERSEDED') score += 15;
    else if (record.lifecycleStatus === 'CORRECTED') score += 10;

    return Math.round((score / maxScore) * 100);
  };

  const handleFlagRecord = async (record: HistoricalRecord, reason: string) => {
    try {
      await flagRecord(record.recordId, reason);
      await loadHistoricalRecords(); // Reload to show updated flag status
    } catch (err) {
      console.error('Error flagging record:', err);
      setError('Failed to flag record');
    }
  };

  const handleViewDetails = (record: HistoricalRecord) => {
    setSelectedRecord(record);
    setShowDetails(true);
    onRecordSelect?.(record);
  };

  const handleStartCorrection = (record: HistoricalRecord) => {
    setSelectedRecord(record);
    setShowCorrectionWorkflow(true);
  };

  const getLifecycleStatusBadge = (status: string) => {
    const statusConfig = {
      'ACTIVE': { variant: 'default' as const, icon: CheckCircle, color: 'text-green-600' },
      'SUPERSEDED': { variant: 'secondary' as const, icon: ArrowRight, color: 'text-yellow-600' },
      'CORRECTED': { variant: 'destructive' as const, icon: Edit, color: 'text-red-600' },
      'DELETED': { variant: 'outline' as const, icon: XCircle, color: 'text-gray-600' }
    };

    const config = statusConfig[status] || statusConfig['ACTIVE'];
    const IconComponent = config.icon;

    return (
      <Badge variant={config.variant} className="flex items-center space-x-1">
        <IconComponent className={`h-3 w-3 ${config.color}`} />
        <span>{status}</span>
      </Badge>
    );
  };

  const getComplianceScoreBadge = (score: number) => {
    let variant: 'default' | 'secondary' | 'destructive' | 'outline' = 'default';
    let color = 'text-green-600';

    if (score >= 90) {
      variant = 'default';
      color = 'text-green-600';
    } else if (score >= 75) {
      variant = 'secondary';
      color = 'text-yellow-600';
    } else {
      variant = 'destructive';
      color = 'text-red-600';
    }

    return (
      <Badge variant={variant} className={`${color}`}>
        {score}%
      </Badge>
    );
  };

  const filteredAndSortedRecords = records
    .filter(record => {
      if (filterStatus === 'all') return true;
      if (filterStatus === 'flagged') return record.isFlagged;
      if (filterStatus === 'review') return record.requiresReview;
      if (filterStatus === 'active') return record.lifecycleStatus === 'ACTIVE';
      return record.lifecycleStatus === filterStatus;
    })
    .sort((a, b) => {
      const aValue = a[sortField as keyof HistoricalRecord];
      const bValue = b[sortField as keyof HistoricalRecord];

      if (sortDirection === 'asc') {
        return aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
      } else {
        return aValue > bValue ? -1 : aValue < bValue ? 1 : 0;
      }
    });

  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-center space-x-2">
            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
            <span>Loading historical records...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header with Controls */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center space-x-2">
              <History className="h-5 w-5" />
              <span>PSDE Historical Records</span>
              <Badge variant="outline">{records.length} records</Badge>
            </CardTitle>
            <div className="flex items-center space-x-2">
              {/* Filter Controls */}
              <select
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                className="px-3 py-1 border border-gray-300 rounded text-sm"
              >
                <option value="all">All Records</option>
                <option value="active">Active Only</option>
                <option value="flagged">Flagged</option>
                <option value="review">Needs Review</option>
                <option value="SUPERSEDED">Superseded</option>
                <option value="CORRECTED">Corrected</option>
              </select>

              {/* Sort Controls */}
              <select
                value={sortField}
                onChange={(e) => setSortField(e.target.value)}
                className="px-3 py-1 border border-gray-300 rounded text-sm"
              >
                <option value="informationDate">Information Date</option>
                <option value="createdAt">Created Date</option>
                <option value="lifecycleStatus">Status</option>
                <option value="complianceScore">Compliance Score</option>
              </select>

              <Button
                variant="outline"
                size="sm"
                onClick={() => setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')}
              >
                {sortDirection === 'asc' ? '↑' : '↓'}
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Records Table */}
      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Information Date</TableHead>
                <TableHead>Collection Stage</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Compliance</TableHead>
                <TableHead>Flags</TableHead>
                <TableHead>Version</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredAndSortedRecords.map((record) => (
                <TableRow key={record.recordId}>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Calendar className="h-4 w-4 text-gray-500" />
                      <span>{record.informationDate}</span>
                    </div>
                  </TableCell>
                  <TableCell>{record.collectionStage}</TableCell>
                  <TableCell>{getLifecycleStatusBadge(record.lifecycleStatus)}</TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      {getComplianceScoreBadge(record.complianceScore || 0)}
                      {record.requiresReview && (
                        <AlertTriangle className="h-4 w-4 text-yellow-600" />
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    {record.isFlagged ? (
                      <div className="flex items-center space-x-1">
                        <Flag className="h-4 w-4 text-red-600" />
                        <span className="text-xs text-red-600">{record.flagReason}</span>
                      </div>
                    ) : (
                      <span className="text-gray-400">-</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <span>v{record.version || 1}</span>
                      {record.isCorrection && (
                        <Badge variant="outline" className="text-xs">
                          Correction
                        </Badge>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleViewDetails(record)}
                      >
                        <Eye className="h-4 w-4" />
                      </Button>
                      {canModifyRedactionLevel() && record.lifecycleStatus === 'ACTIVE' && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleStartCorrection(record)}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                      )}
                      {!record.isFlagged && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleFlagRecord(record, 'Manual review requested')}
                        >
                          <Flag className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {filteredAndSortedRecords.length === 0 && (
            <div className="p-6 text-center text-gray-500">
              No records found matching the current filters.
            </div>
          )}
        </CardContent>
      </Card>

      {/* Record Details Dialog */}
      <Dialog open={showDetails} onOpenChange={setShowDetails}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>PSDE Record Details</DialogTitle>
            <DialogDescription>
              Detailed view of historical PSDE record
            </DialogDescription>
          </DialogHeader>

          {selectedRecord && (
            <div className="space-y-4">
              {/* Record Metadata */}
              <div className="grid grid-cols-3 gap-4 p-4 bg-gray-50 rounded-lg">
                <div>
                  <label className="text-sm font-medium text-gray-600">Record ID</label>
                  <p className="font-mono text-sm">{selectedRecord.recordId}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">Version</label>
                  <p>v{selectedRecord.version || 1}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-600">Created</label>
                  <p>{new Date(selectedRecord.createdAt).toLocaleString()}</p>
                </div>
              </div>

              {/* Data Content */}
              <div className="space-y-4">
                {/* Income Data */}
                <div className="border rounded-lg p-4">
                  <h4 className="font-medium mb-3">Income & Benefits</h4>
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <label className="font-medium text-gray-600">Total Monthly Income</label>
                      <p>{selectedRecord.totalMonthlyIncome || 'Not provided'}</p>
                    </div>
                    <div>
                      <label className="font-medium text-gray-600">Income from Any Source</label>
                      <p>{selectedRecord.incomeFromAnySource || 'Not provided'}</p>
                    </div>
                  </div>
                </div>

                {/* DV Data (if accessible) */}
                {canAccessDvData() && (
                  <div className="border rounded-lg p-4">
                    <h4 className="font-medium mb-3 flex items-center space-x-2">
                      <span>Domestic Violence Information</span>
                      {selectedRecord.isHighSensitivityDvCase && (
                        <Badge variant="destructive" className="text-xs">
                          High Sensitivity
                        </Badge>
                      )}
                    </h4>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <label className="font-medium text-gray-600">DV History</label>
                        <p>{selectedRecord.domesticViolence || 'Not provided'}</p>
                      </div>
                      <div>
                        <label className="font-medium text-gray-600">Currently Fleeing</label>
                        <p>{selectedRecord.currentlyFleeingDomesticViolence || 'Not provided'}</p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Correction Workflow Dialog */}
      {showCorrectionWorkflow && selectedRecord && (
        <Dialog open={showCorrectionWorkflow} onOpenChange={setShowCorrectionWorkflow}>
          <DialogContent className="max-w-6xl max-h-[95vh] overflow-y-auto">
            <PsdeCorrectionWorkflow
              recordId={selectedRecord.recordId}
              enrollmentId={enrollmentId}
              clientId={clientId}
              userRoles={userRoles}
              onCorrectionComplete={() => {
                setShowCorrectionWorkflow(false);
                loadHistoricalRecords();
              }}
              onClose={() => setShowCorrectionWorkflow(false)}
            />
          </DialogContent>
        </Dialog>
      )}

      {/* Error Display */}
      {error && (
        <Alert variant="destructive">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
    </div>
  );
}