import React, { useState, useEffect } from 'react';
import {
  useIntakePsde,
  usePsdeAccessControl,
  type IntakePsdeResponse,
  type CorrectionReason,
  type AuditTrailEntry
} from '@haven/api-client';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  History,
  AlertTriangle,
  Clock,
  User,
  FileEdit,
  Eye,
  Shield,
  CheckCircle,
  XCircle,
  Calendar,
  ArrowRight,
  Edit,
  Save,
  X
} from 'lucide-react';
import { IntakePsdeForm } from './IntakePsdeForm';

interface PsdeCorrectionWorkflowProps {
  recordId: string;
  enrollmentId: string;
  clientId: string;
  userRoles: string[];
  onCorrectionComplete?: (correctedRecord: IntakePsdeResponse) => void;
  onClose?: () => void;
}

export function PsdeCorrectionWorkflow({
  recordId,
  enrollmentId,
  clientId,
  userRoles,
  onCorrectionComplete,
  onClose
}: PsdeCorrectionWorkflowProps) {
  const [activeTab, setActiveTab] = useState('current');
  const [showCorrectionForm, setShowCorrectionForm] = useState(false);
  const [selectedCorrectionReason, setSelectedCorrectionReason] = useState<CorrectionReason | null>(null);
  const [correctionJustification, setCorrectionJustification] = useState('');
  const [currentRecord, setCurrentRecord] = useState<IntakePsdeResponse | null>(null);
  const [auditTrail, setAuditTrail] = useState<AuditTrailEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { getPsdeRecord, createCorrection, getAuditTrail } = useIntakePsde();
  const { canModifyRedactionLevel, hasAdministrativeOverride } = usePsdeAccessControl();

  // Available correction reasons
  const correctionReasons: CorrectionReason[] = [
    { code: 'DATA_ENTRY', description: 'Correction of data entry error', requiresSupervisorApproval: false },
    { code: 'CLIENT_CORRECTION', description: 'Client provided corrected information', requiresSupervisorApproval: false },
    { code: 'SYSTEM_ERROR', description: 'System or technical error correction', requiresSupervisorApproval: true },
    { code: 'POLICY_CHANGE', description: 'Correction due to policy interpretation change', requiresSupervisorApproval: true },
    { code: 'AUDIT', description: 'Correction based on audit finding', requiresSupervisorApproval: true },
    { code: 'SUPERVISOR', description: 'Correction following supervisor review', requiresSupervisorApproval: true }
  ];

  useEffect(() => {
    loadRecordData();
  }, [recordId]);

  const loadRecordData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Load current record
      const record = await getPsdeRecord(enrollmentId, recordId);
      setCurrentRecord(record);

      // Load audit trail
      const trail = await getAuditTrail(recordId);
      setAuditTrail(trail);

    } catch (err) {
      console.error('Error loading record data:', err);
      setError('Failed to load record information');
    } finally {
      setLoading(false);
    }
  };

  const handleStartCorrection = () => {
    setShowCorrectionForm(true);
  };

  const handleCorrectionSubmit = async (correctionData: any) => {
    if (!selectedCorrectionReason) {
      setError('Please select a correction reason');
      return;
    }

    try {
      setLoading(true);

      const correctedRecord = await createCorrection(
        recordId,
        correctionData,
        selectedCorrectionReason,
        correctionJustification
      );

      setShowCorrectionForm(false);
      onCorrectionComplete?.(correctedRecord);

      // Reload data to show the correction
      await loadRecordData();

    } catch (err) {
      console.error('Error creating correction:', err);
      setError('Failed to create correction record');
    } finally {
      setLoading(false);
    }
  };

  const canCreateCorrection = () => {
    return userRoles.some(role =>
      ['DV_SPECIALIST', 'ADMIN', 'CASE_MANAGER'].includes(role)
    );
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

  const formatDateTime = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  if (loading && !currentRecord) {
    return (
      <Card className="w-full max-w-4xl">
        <CardContent className="p-6">
          <div className="flex items-center justify-center space-x-2">
            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
            <span>Loading correction workflow...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error && !currentRecord) {
    return (
      <Card className="w-full max-w-4xl">
        <CardContent className="p-6">
          <Alert variant="destructive">
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="w-full max-w-6xl space-y-6">
      {/* Header */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center space-x-2">
              <FileEdit className="h-5 w-5" />
              <span>PSDE Correction Workflow</span>
            </CardTitle>
            <div className="flex items-center space-x-2">
              {currentRecord && getLifecycleStatusBadge(currentRecord.lifecycleStatus)}
              <Button variant="outline" size="sm" onClick={onClose}>
                <X className="w-4 h-4 mr-2" />
                Close
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="text-sm font-medium text-gray-600">Record ID</label>
              <p className="font-mono text-sm">{recordId}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-600">Information Date</label>
              <p>{currentRecord?.informationDate}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-600">Collection Stage</label>
              <p>{currentRecord?.collectionStage}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Main Workflow Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="current" className="flex items-center space-x-2">
            <Eye className="h-4 w-4" />
            <span>Current Data</span>
          </TabsTrigger>
          <TabsTrigger value="history" className="flex items-center space-x-2">
            <History className="h-4 w-4" />
            <span>Audit Trail</span>
          </TabsTrigger>
          <TabsTrigger value="corrections" className="flex items-center space-x-2">
            <Edit className="h-4 w-4" />
            <span>Corrections</span>
          </TabsTrigger>
          <TabsTrigger value="compliance" className="flex items-center space-x-2">
            <Shield className="h-4 w-4" />
            <span>Compliance</span>
          </TabsTrigger>
        </TabsList>

        {/* Current Data Tab */}
        <TabsContent value="current" className="space-y-4">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Current PSDE Data</CardTitle>
                {canCreateCorrection() && (
                  <Button
                    onClick={handleStartCorrection}
                    disabled={currentRecord?.lifecycleStatus !== 'ACTIVE'}
                  >
                    <Edit className="w-4 h-4 mr-2" />
                    Create Correction
                  </Button>
                )}
              </div>
            </CardHeader>
            <CardContent>
              {currentRecord ? (
                <div className="space-y-4">
                  {/* Income Information */}
                  <div className="border rounded-lg p-4">
                    <h4 className="font-medium mb-3">Income & Benefits</h4>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <label className="font-medium text-gray-600">Total Monthly Income</label>
                        <p>{currentRecord.totalMonthlyIncome || 'Not provided'}</p>
                      </div>
                      <div>
                        <label className="font-medium text-gray-600">Income from Any Source</label>
                        <p>{currentRecord.incomeFromAnySource || 'Not provided'}</p>
                      </div>
                    </div>
                  </div>

                  {/* Health Insurance */}
                  <div className="border rounded-lg p-4">
                    <h4 className="font-medium mb-3">Health Insurance</h4>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <label className="font-medium text-gray-600">Covered by Health Insurance</label>
                        <p>{currentRecord.coveredByHealthInsurance || 'Not provided'}</p>
                      </div>
                      <div>
                        <label className="font-medium text-gray-600">VAWA Protected Health Info</label>
                        <p>{currentRecord.hasVawaProtectedHealthInfo ? 'Yes' : 'No'}</p>
                      </div>
                    </div>
                  </div>

                  {/* Domestic Violence */}
                  <div className="border rounded-lg p-4">
                    <h4 className="font-medium mb-3 flex items-center space-x-2">
                      <span>Domestic Violence Information</span>
                      {currentRecord.isHighSensitivityDvCase && (
                        <Badge variant="destructive" className="text-xs">
                          High Sensitivity
                        </Badge>
                      )}
                    </h4>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <label className="font-medium text-gray-600">Domestic Violence History</label>
                        <p>{currentRecord.domesticViolence || 'Not provided'}</p>
                      </div>
                      <div>
                        <label className="font-medium text-gray-600">Currently Fleeing DV</label>
                        <p>{currentRecord.currentlyFleeingDomesticViolence || 'Not provided'}</p>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <p>No current data available</p>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Audit Trail Tab */}
        <TabsContent value="history" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Audit Trail & Version History</CardTitle>
            </CardHeader>
            <CardContent>
              {auditTrail.length > 0 ? (
                <div className="space-y-4">
                  {auditTrail.map((entry, index) => (
                    <div key={index} className="border-l-4 border-blue-500 pl-4 py-2">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                          <div className="flex items-center space-x-2">
                            <Clock className="h-4 w-4 text-gray-500" />
                            <span className="text-sm font-medium">
                              {formatDateTime(entry.timestamp)}
                            </span>
                          </div>
                          <Badge variant="outline">{entry.entryType}</Badge>
                        </div>
                        <div className="flex items-center space-x-2">
                          <User className="h-4 w-4 text-gray-500" />
                          <span className="text-sm">{entry.modifiedBy}</span>
                        </div>
                      </div>
                      <p className="text-sm text-gray-700 mt-1">{entry.description}</p>
                      {entry.changedFields.length > 0 && (
                        <div className="mt-2">
                          <span className="text-xs text-gray-500">Changed fields: </span>
                          <span className="text-xs">{entry.changedFields.join(', ')}</span>
                        </div>
                      )}
                      {entry.correctionReason && (
                        <div className="mt-2">
                          <span className="text-xs text-gray-500">Correction reason: </span>
                          <span className="text-xs">{entry.correctionReason}</span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-gray-500">No audit trail entries found</p>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Corrections Tab */}
        <TabsContent value="corrections" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Correction Management</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <Alert>
                  <AlertTriangle className="h-4 w-4" />
                  <AlertDescription>
                    Corrections create a new version of the record while preserving the original.
                    All changes are tracked in the audit trail for compliance.
                  </AlertDescription>
                </Alert>

                {/* Correction Reason Selection */}
                <div className="space-y-2">
                  <label className="text-sm font-medium">Correction Reason</label>
                  <Select
                    value={selectedCorrectionReason?.code || ''}
                    onValueChange={(value) => {
                      const reason = correctionReasons.find(r => r.code === value);
                      setSelectedCorrectionReason(reason || null);
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select correction reason" />
                    </SelectTrigger>
                    <SelectContent>
                      {correctionReasons.map((reason) => (
                        <SelectItem key={reason.code} value={reason.code}>
                          <div className="flex items-center justify-between w-full">
                            <span>{reason.description}</span>
                            {reason.requiresSupervisorApproval && (
                              <Badge variant="outline" className="ml-2 text-xs">
                                Supervisor Required
                              </Badge>
                            )}
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {/* Justification */}
                <div className="space-y-2">
                  <label className="text-sm font-medium">Justification</label>
                  <textarea
                    className="w-full p-3 border border-gray-300 rounded-md"
                    rows={3}
                    value={correctionJustification}
                    onChange={(e) => setCorrectionJustification(e.target.value)}
                    placeholder="Provide detailed justification for this correction..."
                  />
                </div>

                {/* Start Correction Button */}
                <Button
                  onClick={handleStartCorrection}
                  disabled={!selectedCorrectionReason || !correctionJustification.trim() || !canCreateCorrection()}
                  className="w-full"
                >
                  <Edit className="w-4 h-4 mr-2" />
                  Start Correction Process
                </Button>

                {selectedCorrectionReason?.requiresSupervisorApproval && !hasAdministrativeOverride() && (
                  <Alert>
                    <Shield className="h-4 w-4" />
                    <AlertDescription>
                      This correction type requires supervisor approval. The correction will be submitted for review.
                    </AlertDescription>
                  </Alert>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Compliance Tab */}
        <TabsContent value="compliance" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Compliance & Data Quality</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {/* HUD Compliance Status */}
                <div className="border rounded-lg p-4">
                  <h4 className="font-medium mb-3">HUD Data Quality</h4>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="flex items-center space-x-2">
                      {currentRecord?.meetsHmisDataQuality ? (
                        <CheckCircle className="h-5 w-5 text-green-600" />
                      ) : (
                        <XCircle className="h-5 w-5 text-red-600" />
                      )}
                      <span className="text-sm">HMIS Data Quality Requirements</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      {currentRecord?.containsSensitiveInformation ? (
                        <Shield className="h-5 w-5 text-yellow-600" />
                      ) : (
                        <CheckCircle className="h-5 w-5 text-green-600" />
                      )}
                      <span className="text-sm">Sensitive Information Handling</span>
                    </div>
                  </div>
                </div>

                {/* VAWA Compliance */}
                <div className="border rounded-lg p-4">
                  <h4 className="font-medium mb-3">VAWA Compliance</h4>
                  <div className="space-y-2">
                    <div className="flex items-center space-x-2">
                      <span className="text-sm font-medium">Redaction Level:</span>
                      <Badge variant="outline">{currentRecord?.dvRedactionLevel || 'No Redaction'}</Badge>
                    </div>
                    <div className="flex items-center space-x-2">
                      <span className="text-sm font-medium">Confidentiality Requested:</span>
                      <span className="text-sm">
                        {currentRecord?.vawaConfidentialityRequested ? 'Yes' : 'No'}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Correction Form Dialog */}
      <Dialog open={showCorrectionForm} onOpenChange={setShowCorrectionForm}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Create Correction Record</DialogTitle>
            <DialogDescription>
              Create a corrected version of this PSDE record. The original will be preserved in the audit trail.
            </DialogDescription>
          </DialogHeader>

          {currentRecord && (
            <IntakePsdeForm
              enrollmentId={enrollmentId}
              clientId={clientId}
              userRoles={userRoles}
              initialData={currentRecord}
              isCorrection={true}
              correctionReason={selectedCorrectionReason}
              onSubmit={handleCorrectionSubmit}
              onCancel={() => setShowCorrectionForm(false)}
            />
          )}
        </DialogContent>
      </Dialog>

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