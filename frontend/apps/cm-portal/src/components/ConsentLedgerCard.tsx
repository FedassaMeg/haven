import React, { useState } from 'react';
import { Badge, Button, Card, CardContent, CardHeader, CardTitle, Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@haven/ui';
import { useClientConsents, ConsentLedgerEntry, ConsentStatus, ConsentType } from '@haven/api-client';
// import { formatDate } from '@haven/ui';

interface ConsentLedgerCardProps {
  clientId: string;
}

const CONSENT_TYPE_LABELS: Record<ConsentType, string> = {
  [ConsentType.INFORMATION_SHARING]: 'General Services',
  [ConsentType.HMIS_PARTICIPATION]: 'HMIS Participation',
  [ConsentType.COURT_TESTIMONY]: 'Court Testimony',
  [ConsentType.MEDICAL_INFORMATION_SHARING]: 'Medical Information',
  [ConsentType.REFERRAL_SHARING]: 'Referral Sharing',
  [ConsentType.RESEARCH_PARTICIPATION]: 'Research Participation',
  [ConsentType.LEGAL_COUNSEL_COMMUNICATION]: 'Legal Counsel Communication',
  [ConsentType.FAMILY_CONTACT]: 'Family Contact'
};

export function ConsentLedgerCard({ clientId }: ConsentLedgerCardProps) {
  const { consents, loading, error, refresh } = useClientConsents(clientId, false);
  const [showAllConsents, setShowAllConsents] = useState(false);
  const [selectedConsent, setSelectedConsent] = useState<ConsentLedgerEntry | null>(null);

  const activeConsents = consents.filter(consent => consent.status === ConsentStatus.GRANTED && !consent.isExpired);
  const expiringSoonConsents = consents.filter(consent => consent.isExpiringSoon && consent.status === ConsentStatus.GRANTED);

  const getStatusBadge = (consent: ConsentLedgerEntry) => {
    if (consent.status === ConsentStatus.REVOKED) {
      return <Badge variant="destructive">Revoked</Badge>;
    }
    if (consent.status === ConsentStatus.EXPIRED || consent.isExpired) {
      return <Badge variant="outline">Expired</Badge>;
    }
    if (consent.isExpiringSoon) {
      return <Badge variant="warning">Expiring Soon</Badge>;
    }
    return <Badge variant="success">Active</Badge>;
  };

  const getStatusColor = (consent: ConsentLedgerEntry) => {
    if (consent.status === ConsentStatus.REVOKED) return 'red';
    if (consent.status === ConsentStatus.EXPIRED || consent.isExpired) return 'gray';
    if (consent.isExpiringSoon) return 'amber';
    return 'green';
  };

  const formatExpirationText = (consent: ConsentLedgerEntry) => {
    if (!consent.expiresAt) return 'No expiration';
    if (consent.isExpired) return `Expired ${consent.expiresAt}`;
    if (consent.isExpiringSoon) {
      const days = consent.daysUntilExpiration;
      return days > 0 ? `Expires in ${days} days` : 'Expires today';
    }
    return `Active until ${consent.expiresAt}`;
  };

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Consent & Privacy</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center py-8">
            <div className="text-sm text-secondary-600">Loading consents...</div>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Consent & Privacy</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8">
            <div className="text-sm text-red-600 mb-4">Failed to load consents</div>
            <Button variant="outline" size="sm" onClick={refresh}>
              Try Again
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>Consent & Privacy</CardTitle>
          <div className="flex space-x-2">
            <Dialog open={showAllConsents} onOpenChange={setShowAllConsents}>
              <DialogTrigger asChild>
                <Button size="sm" variant="outline">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  View All
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-4xl">
                <DialogHeader>
                  <DialogTitle>Complete Consent History</DialogTitle>
                </DialogHeader>
                <div className="max-h-96 overflow-y-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Type</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Granted</TableHead>
                        <TableHead>Expires</TableHead>
                        <TableHead>Recipient</TableHead>
                        <TableHead>Actions</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {consents.map((consent) => (
                        <TableRow key={consent.id}>
                          <TableCell>
                            <div>
                              <div className="font-medium">{CONSENT_TYPE_LABELS[consent.consentType]}</div>
                              {consent.purpose && (
                                <div className="text-sm text-secondary-600">{consent.purpose}</div>
                              )}
                            </div>
                          </TableCell>
                          <TableCell>{getStatusBadge(consent)}</TableCell>
                          <TableCell>{consent.grantedAt}</TableCell>
                          <TableCell>{formatExpirationText(consent)}</TableCell>
                          <TableCell>{consent.recipientOrganization || 'Any organization'}</TableCell>
                          <TableCell>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setSelectedConsent(consent)}
                            >
                              Details
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </DialogContent>
            </Dialog>
            <Button size="sm" variant="outline">
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
              Manage Consent
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {/* Active Consents */}
          {activeConsents.length === 0 ? (
            <div className="text-center py-8 text-secondary-600">
              <svg className="w-12 h-12 mx-auto mb-4 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
              <p className="text-sm">No active consents on file</p>
              <p className="text-xs text-secondary-500 mt-1">Client consent is required for data sharing</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-3">
              {activeConsents.slice(0, 3).map((consent) => {
                const color = getStatusColor(consent);
                return (
                  <div
                    key={consent.id}
                    className={`flex items-center justify-between p-3 bg-${color}-50 border border-${color}-200 rounded-lg cursor-pointer hover:bg-${color}-100 transition-colors`}
                    onClick={() => setSelectedConsent(consent)}
                  >
                    <div className="flex items-center space-x-3">
                      <div className={`w-3 h-3 bg-${color}-500 rounded-full`}></div>
                      <div>
                        <p className={`font-medium text-${color}-900`}>
                          {CONSENT_TYPE_LABELS[consent.consentType]}
                        </p>
                        <p className={`text-sm text-${color}-700`}>
                          {formatExpirationText(consent)}
                        </p>
                        {consent.isVAWAProtected && (
                          <div className="flex items-center mt-1">
                            <svg className="w-3 h-3 text-red-600 mr-1" fill="currentColor" viewBox="0 0 20 20">
                              <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                            </svg>
                            <span className="text-xs text-red-700 font-medium">VAWA Protected</span>
                          </div>
                        )}
                      </div>
                    </div>
                    {getStatusBadge(consent)}
                  </div>
                );
              })}
              
              {activeConsents.length > 3 && (
                <Button 
                  variant="ghost" 
                  size="sm" 
                  onClick={() => setShowAllConsents(true)}
                  className="text-secondary-600 hover:text-secondary-900"
                >
                  View {activeConsents.length - 3} more active consents
                </Button>
              )}
            </div>
          )}

          {/* Warnings for expiring consents */}
          {expiringSoonConsents.length > 0 && (
            <div className="mt-6 pt-4 border-t border-secondary-200">
              <div className="flex items-center space-x-2 mb-3">
                <svg className="w-4 h-4 text-amber-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                <h4 className="text-sm font-medium text-amber-800">Consents Requiring Attention</h4>
              </div>
              <div className="text-sm text-amber-700">
                {expiringSoonConsents.length} consent{expiringSoonConsents.length !== 1 ? 's' : ''} expiring soon. Review and renew as needed.
              </div>
            </div>
          )}

          {/* VAWA protected indicator */}
          {consents.some(consent => consent.isVAWAProtected && consent.status === ConsentStatus.GRANTED) && (
            <div className="mt-6 pt-4 border-t border-secondary-200">
              <h4 className="text-sm font-medium text-secondary-700 mb-3">Safety & Confidentiality</h4>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <svg className="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                  </svg>
                  <span className="text-sm text-secondary-900">VAWA Protected Information</span>
                </div>
                <Badge variant="destructive">Protected</Badge>
              </div>
            </div>
          )}
        </div>
      </CardContent>

      {/* Consent Detail Dialog */}
      {selectedConsent && (
        <Dialog open={!!selectedConsent} onOpenChange={() => setSelectedConsent(null)}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Consent Details</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-secondary-700">Type</label>
                <p className="text-sm">{CONSENT_TYPE_LABELS[selectedConsent.consentType]}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-secondary-700">Purpose</label>
                <p className="text-sm">{selectedConsent.purpose || 'Not specified'}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-secondary-700">Status</label>
                <div className="mt-1">{getStatusBadge(selectedConsent)}</div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-secondary-700">Granted</label>
                  <p className="text-sm">{selectedConsent.grantedAt}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-secondary-700">Expires</label>
                  <p className="text-sm">{formatExpirationText(selectedConsent)}</p>
                </div>
              </div>
              {selectedConsent.recipientOrganization && (
                <div>
                  <label className="text-sm font-medium text-secondary-700">Recipient Organization</label>
                  <p className="text-sm">{selectedConsent.recipientOrganization}</p>
                </div>
              )}
              {selectedConsent.limitations && (
                <div>
                  <label className="text-sm font-medium text-secondary-700">Limitations</label>
                  <p className="text-sm">{selectedConsent.limitations}</p>
                </div>
              )}
              {selectedConsent.isVAWAProtected && (
                <div className="p-3 bg-red-50 border border-red-200 rounded">
                  <div className="flex items-center space-x-2">
                    <svg className="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                    </svg>
                    <span className="text-sm font-medium text-red-800">VAWA Protected</span>
                  </div>
                  <p className="text-xs text-red-700 mt-1">
                    This consent involves VAWA-protected information requiring special handling.
                  </p>
                </div>
              )}
            </div>
          </DialogContent>
        </Dialog>
      )}
    </Card>
  );
}