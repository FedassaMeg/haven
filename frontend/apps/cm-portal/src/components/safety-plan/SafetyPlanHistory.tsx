import { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@haven/ui';
import type { SafetyPlan, SafetyPlanStatus } from '@haven/api-client/src/types/safety-plan';

interface SafetyPlanHistoryProps {
  plans: SafetyPlan[];
  activePlanId?: string;
  onViewPlan: (plan: SafetyPlan) => void;
  onRestorePlan: (plan: SafetyPlan) => void;
  onEditPlan: (plan: SafetyPlan) => void;
}

const SafetyPlanHistory: React.FC<SafetyPlanHistoryProps> = ({
  plans,
  activePlanId,
  onViewPlan,
  onRestorePlan,
  onEditPlan
}) => {
  const [selectedPlan, setSelectedPlan] = useState<SafetyPlan | null>(null);
  const [showPreview, setShowPreview] = useState(false);
  const [showRestoreConfirm, setShowRestoreConfirm] = useState(false);

  const getStatusVariant = (status: SafetyPlanStatus) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'DRAFT':
        return 'warning';
      case 'ARCHIVED':
        return 'secondary';
      case 'UNDER_REVIEW':
        return 'primary';
      case 'EXPIRED':
        return 'destructive';
      default:
        return 'secondary';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit'
    });
  };

  const sortedPlans = [...plans].sort((a, b) => {
    // Active plan first, then by version descending
    if (a.status === 'ACTIVE' && b.status !== 'ACTIVE') return -1;
    if (b.status === 'ACTIVE' && a.status !== 'ACTIVE') return 1;
    return b.version - a.version;
  });

  const handlePreview = (plan: SafetyPlan) => {
    setSelectedPlan(plan);
    setShowPreview(true);
  };

  const handleRestoreConfirm = (plan: SafetyPlan) => {
    setSelectedPlan(plan);
    setShowRestoreConfirm(true);
  };

  const handleRestore = () => {
    if (selectedPlan) {
      onRestorePlan(selectedPlan);
    }
    setShowRestoreConfirm(false);
    setSelectedPlan(null);
  };

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>Safety Plan History</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {sortedPlans.map((plan) => (
              <div
                key={plan.id}
                className={`
                  border rounded-lg p-4 transition-colors
                  ${plan.id === activePlanId ? 'border-primary-300 bg-primary-50' : 'border-secondary-200 bg-white hover:bg-secondary-50'}
                `}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <h4 className="font-medium text-secondary-900">
                        v{plan.version} - {plan.status === 'DRAFT' ? 'Draft' : `${plan.status.charAt(0) + plan.status.slice(1).toLowerCase()}`}
                      </h4>
                      <Badge variant={getStatusVariant(plan.status)}>
                        {plan.status === 'ACTIVE' && plan.id === activePlanId ? 'ACTIVE' : plan.status}
                      </Badge>
                      {plan.id === activePlanId && (
                        <Badge variant="outline">Current</Badge>
                      )}
                    </div>
                    
                    <div className="text-sm text-secondary-600 space-y-1">
                      <div className="flex items-center space-x-4">
                        <span>
                          Created: {formatDate(plan.createdAt)} by {plan.createdByName}
                        </span>
                        {plan.updatedAt !== plan.createdAt && (
                          <span>
                            Updated: {formatDate(plan.updatedAt)} by {plan.updatedByName || plan.createdByName}
                          </span>
                        )}
                      </div>
                      
                      {plan.activatedAt && (
                        <div>Activated: {formatDateTime(plan.activatedAt)}</div>
                      )}
                      
                      {plan.archivedAt && (
                        <div>Archived: {formatDateTime(plan.archivedAt)}</div>
                      )}
                      
                      {plan.nextReviewDate && plan.status === 'ACTIVE' && (
                        <div className="text-warning-600">
                          Review due: {formatDate(plan.nextReviewDate)}
                        </div>
                      )}
                    </div>

                    {/* Quick summary of sections */}
                    <div className="mt-3 flex flex-wrap gap-2">
                      {plan.triggersAndRisks?.content && (
                        <Badge variant="ghost" size="sm">Triggers</Badge>
                      )}
                      {plan.warningSign?.content && (
                        <Badge variant="ghost" size="sm">Warning Signs</Badge>
                      )}
                      {plan.safeContacts?.length > 0 && (
                        <Badge variant="ghost" size="sm">
                          {plan.safeContacts.length} Contact{plan.safeContacts.length !== 1 ? 's' : ''}
                        </Badge>
                      )}
                      {plan.escapePlan?.content && (
                        <Badge variant="ghost" size="sm">Escape Plan</Badge>
                      )}
                      {plan.techSafety?.content && (
                        <Badge variant="ghost" size="sm">Tech Safety</Badge>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center space-x-2 ml-4">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handlePreview(plan)}
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                      View
                    </Button>

                    {plan.status !== 'ACTIVE' && plan.id !== activePlanId && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleRestoreConfirm(plan)}
                      >
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                        </svg>
                        Restore
                      </Button>
                    )}

                    {plan.status === 'DRAFT' && (
                      <Button
                        size="sm"
                        onClick={() => onEditPlan(plan)}
                      >
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                        Edit
                      </Button>
                    )}

                    {plan.status === 'ARCHIVED' && (
                      <Button
                        size="sm"
                        variant="ghost"
                        disabled
                      >
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 8l4 4 4-4" />
                        </svg>
                        Archive
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            ))}

            {plans.length === 0 && (
              <div className="text-center py-8">
                <svg className="w-12 h-12 mx-auto mb-4 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <p className="text-secondary-600">No safety plans found</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Plan Preview Dialog */}
      <Dialog open={showPreview} onOpenChange={setShowPreview}>
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              Safety Plan v{selectedPlan?.version} Preview
            </DialogTitle>
          </DialogHeader>
          {selectedPlan && (
            <div className="space-y-4 py-4">
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <strong>Status:</strong> {selectedPlan.status}
                </div>
                <div>
                  <strong>Created:</strong> {formatDateTime(selectedPlan.createdAt)}
                </div>
                <div>
                  <strong>Created by:</strong> {selectedPlan.createdByName}
                </div>
                {selectedPlan.updatedAt !== selectedPlan.createdAt && (
                  <div>
                    <strong>Last updated:</strong> {formatDateTime(selectedPlan.updatedAt)}
                  </div>
                )}
              </div>

              <hr />

              {/* Plan sections preview */}
              {selectedPlan.triggersAndRisks?.content && (
                <div>
                  <h4 className="font-medium text-secondary-900 mb-2">Triggers / Risks</h4>
                  <p className="text-sm text-secondary-700 bg-secondary-50 p-3 rounded whitespace-pre-wrap">
                    {selectedPlan.triggersAndRisks.content}
                  </p>
                </div>
              )}

              {selectedPlan.warningSign?.content && (
                <div>
                  <h4 className="font-medium text-secondary-900 mb-2">Warning Signs</h4>
                  <p className="text-sm text-secondary-700 bg-secondary-50 p-3 rounded whitespace-pre-wrap">
                    {selectedPlan.warningSign.content}
                  </p>
                </div>
              )}

              {selectedPlan.safeContacts && selectedPlan.safeContacts.length > 0 && (
                <div>
                  <h4 className="font-medium text-secondary-900 mb-2">Safe Contacts</h4>
                  <div className="space-y-2">
                    {selectedPlan.safeContacts.map((contact) => (
                      <div key={contact.id} className="bg-secondary-50 p-3 rounded">
                        <div className="font-medium">{contact.name} ({contact.relationship})</div>
                        <div className="text-sm text-secondary-600">{contact.safetyNotes}</div>
                        {contact.phone && (
                          <div className="text-sm text-secondary-600">
                            {contact.phone} - {contact.contactMethod.replace(/_/g, ' ').toLowerCase()}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {selectedPlan.escapePlan?.content && (
                <div>
                  <h4 className="font-medium text-secondary-900 mb-2">Escape Plan</h4>
                  <p className="text-sm text-secondary-700 bg-secondary-50 p-3 rounded whitespace-pre-wrap">
                    {selectedPlan.escapePlan.content}
                  </p>
                </div>
              )}

              {selectedPlan.techSafety?.content && (
                <div>
                  <h4 className="font-medium text-secondary-900 mb-2">Tech Safety</h4>
                  <p className="text-sm text-secondary-700 bg-secondary-50 p-3 rounded whitespace-pre-wrap">
                    {selectedPlan.techSafety.content}
                  </p>
                </div>
              )}
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowPreview(false)}>Close</Button>
            {selectedPlan && (
              <Button onClick={() => {
                onViewPlan(selectedPlan);
                setShowPreview(false);
              }}>
                View Full Plan
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Restore Confirmation Dialog */}
      <Dialog open={showRestoreConfirm} onOpenChange={setShowRestoreConfirm}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Restore Safety Plan</DialogTitle>
          </DialogHeader>
          <div className="py-4">
            <p className="text-secondary-700">
              Are you sure you want to restore Safety Plan v{selectedPlan?.version}? 
              This will deactivate the current active plan and make this version the active plan.
            </p>
            {selectedPlan && (
              <div className="mt-4 p-3 bg-secondary-50 rounded">
                <div className="text-sm">
                  <div><strong>Version:</strong> {selectedPlan.version}</div>
                  <div><strong>Created:</strong> {formatDateTime(selectedPlan.createdAt)}</div>
                  <div><strong>Created by:</strong> {selectedPlan.createdByName}</div>
                </div>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowRestoreConfirm(false)}>Cancel</Button>
            <Button onClick={handleRestore}>Restore Plan</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default SafetyPlanHistory;