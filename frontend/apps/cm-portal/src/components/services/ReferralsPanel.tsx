import { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Textarea } from '@haven/ui';
import type { Referral, ReferralType, ReferralStatus, Priority } from '@haven/api-client/src/types/services';

interface ReferralsPanelProps {
  clientId: string;
  referrals: Referral[];
  onAddReferral: () => void;
  showAddDialog: boolean;
  onCloseDialog: () => void;
}

const ReferralsPanel: React.FC<ReferralsPanelProps> = ({
  clientId,
  referrals,
  onAddReferral,
  showAddDialog,
  onCloseDialog
}) => {
  const [newReferral, setNewReferral] = useState<Partial<Referral>>({
    clientId,
    referralType: 'LEGAL_AID' as ReferralType,
    status: 'PENDING' as ReferralStatus,
    urgency: 'MEDIUM' as Priority,
    organizationName: '',
    reason: '',
    isWarmHandoff: false,
    consentProvided: false,
    sentDate: new Date().toISOString().split('T')[0]
  });

  const handleSubmit = async () => {
    // API call to create referral
    console.log('Creating referral:', newReferral);
    onCloseDialog();
  };

  const getStatusColor = (status: ReferralStatus) => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'IN_PROGRESS':
      case 'ACKNOWLEDGED':
        return 'primary';
      case 'DECLINED':
      case 'CLIENT_DECLINED':
      case 'NO_RESPONSE':
        return 'destructive';
      case 'SENT':
        return 'warning';
      default:
        return 'secondary';
    }
  };

  const getReferralIcon = (type: ReferralType) => {
    switch (type) {
      case 'LEGAL_AID':
        return '⚖️';
      case 'COUNSELING':
      case 'MENTAL_HEALTH':
        return '🧠';
      case 'MEDICAL':
        return '🏥';
      case 'HOUSING':
        return '🏠';
      case 'EMPLOYMENT':
        return '💼';
      case 'EDUCATION':
        return '📚';
      case 'CHILDCARE':
        return '👶';
      case 'DV_SHELTER':
      case 'DV_SERVICES':
        return '🛡️';
      default:
        return '📋';
    }
  };

  const getDaysAgo = (date: string) => {
    const diff = Date.now() - new Date(date).getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (days === 0) return 'Today';
    if (days === 1) return 'Yesterday';
    return `${days} days ago`;
  };

  // Mock data for demonstration
  const mockReferrals: Referral[] = [
    {
      id: '1',
      clientId,
      referralType: 'LEGAL_AID' as ReferralType,
      organizationName: 'Bay Area Legal Aid',
      contactName: 'Jennifer Smith',
      contactPhone: '(555) 123-4567',
      contactEmail: 'jsmith@bala.org',
      reason: 'Assistance with restraining order and custody arrangements',
      urgency: 'HIGH' as Priority,
      status: 'PENDING' as ReferralStatus,
      sentDate: '2024-05-15',
      followUpDate: '2024-05-20',
      isWarmHandoff: true,
      consentProvided: true,
      createdAt: '2024-05-15T00:00:00Z',
      updatedAt: '2024-05-15T00:00:00Z'
    },
    {
      id: '2',
      clientId,
      referralType: 'CHILDCARE' as ReferralType,
      organizationName: 'Sunshine Childcare Center',
      contactName: 'Maria Rodriguez',
      contactPhone: '(555) 987-6543',
      reason: 'Full-time childcare for 2 children (ages 3 and 5)',
      urgency: 'MEDIUM' as Priority,
      status: 'COMPLETED' as ReferralStatus,
      sentDate: '2024-04-28',
      responseDate: '2024-05-02',
      outcome: 'Enrollment completed, starting June 1st',
      isWarmHandoff: false,
      consentProvided: true,
      createdAt: '2024-04-28T00:00:00Z',
      updatedAt: '2024-05-02T00:00:00Z'
    }
  ];

  const displayReferrals = referrals.length > 0 ? referrals : mockReferrals;

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Referrals</CardTitle>
            <Button size="sm" variant="ghost" onClick={onAddReferral}>
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {displayReferrals.map((referral) => (
              <div key={referral.id} className="border border-secondary-200 rounded-lg p-3">
                <div className="flex items-start justify-between mb-2">
                  <div className="flex items-start space-x-2">
                    <span className="text-lg">{getReferralIcon(referral.referralType)}</span>
                    <div className="flex-1">
                      <h4 className="text-sm font-medium text-secondary-900">
                        {referral.organizationName}
                      </h4>
                      <p className="text-xs text-secondary-600">
                        {referral.referralType.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                      </p>
                    </div>
                  </div>
                  <Badge variant={getStatusColor(referral.status)} size="sm">
                    {referral.status.replace(/_/g, ' ')}
                  </Badge>
                </div>
                
                <p className="text-xs text-secondary-700 mb-2">{referral.reason}</p>
                
                {referral.contactName && (
                  <div className="text-xs text-secondary-600 mb-2">
                    Contact: {referral.contactName}
                    {referral.contactPhone && ` • ${referral.contactPhone}`}
                  </div>
                )}
                
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-3">
                    {referral.isWarmHandoff && (
                      <Badge variant="outline" size="sm">
                        <svg className="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 8h2a2 2 0 012 2v6a2 2 0 01-2 2h-2v4l-4-4H9a1.994 1.994 0 01-1.414-.586m0 0L11 14h4a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2v4l.586-.586z" />
                        </svg>
                        Warm Handoff
                      </Badge>
                    )}
                    {referral.urgency === 'HIGH' || referral.urgency === 'URGENT' && (
                      <Badge variant="destructive" size="sm">
                        {referral.urgency}
                      </Badge>
                    )}
                  </div>
                  <span className="text-secondary-500">
                    {getDaysAgo(referral.sentDate)}
                  </span>
                </div>
                
                {referral.followUpDate && referral.status !== 'COMPLETED' && (
                  <div className="mt-2 pt-2 border-t border-secondary-100">
                    <div className="flex items-center space-x-1 text-xs text-warning-700">
                      <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <span>Task due {new Date(referral.followUpDate).toLocaleDateString()}</span>
                    </div>
                  </div>
                )}
                
                {referral.outcome && (
                  <div className="mt-2 pt-2 border-t border-secondary-100">
                    <p className="text-xs text-success-700">
                      <strong>Outcome:</strong> {referral.outcome}
                    </p>
                  </div>
                )}
              </div>
            ))}
          </div>
          
          {displayReferrals.length === 0 && (
            <div className="text-center py-6">
              <svg className="w-10 h-10 mx-auto mb-3 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
              <p className="text-sm text-secondary-600">No referrals made</p>
              <Button onClick={onAddReferral} className="mt-3" size="sm">
                Make Referral
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add Referral Dialog */}
      <Dialog open={showAddDialog} onOpenChange={onCloseDialog}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Add New Referral</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="type">Referral Type</Label>
                <Select 
                  value={newReferral.referralType}
                  onValueChange={(value) => setNewReferral({ ...newReferral, referralType: value as ReferralType })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LEGAL_AID">Legal Aid</SelectItem>
                    <SelectItem value="COUNSELING">Counseling</SelectItem>
                    <SelectItem value="MEDICAL">Medical</SelectItem>
                    <SelectItem value="MENTAL_HEALTH">Mental Health</SelectItem>
                    <SelectItem value="SUBSTANCE_ABUSE">Substance Abuse</SelectItem>
                    <SelectItem value="HOUSING">Housing</SelectItem>
                    <SelectItem value="EMPLOYMENT">Employment</SelectItem>
                    <SelectItem value="EDUCATION">Education</SelectItem>
                    <SelectItem value="CHILDCARE">Childcare</SelectItem>
                    <SelectItem value="FOOD_ASSISTANCE">Food Assistance</SelectItem>
                    <SelectItem value="TRANSPORTATION">Transportation</SelectItem>
                    <SelectItem value="DV_SHELTER">DV Shelter</SelectItem>
                    <SelectItem value="DV_SERVICES">DV Services</SelectItem>
                    <SelectItem value="BENEFITS">Benefits</SelectItem>
                    <SelectItem value="OTHER">Other</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="urgency">Urgency</Label>
                <Select 
                  value={newReferral.urgency}
                  onValueChange={(value) => setNewReferral({ ...newReferral, urgency: value as Priority })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select urgency" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LOW">Low</SelectItem>
                    <SelectItem value="MEDIUM">Medium</SelectItem>
                    <SelectItem value="HIGH">High</SelectItem>
                    <SelectItem value="URGENT">Urgent</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            
            <div>
              <Label htmlFor="organization">Organization Name</Label>
              <Input
                id="organization"
                placeholder="e.g., Bay Area Legal Aid"
                value={newReferral.organizationName}
                onChange={(e) => setNewReferral({ ...newReferral, organizationName: e.target.value })}
              />
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="contactName">Contact Name (Optional)</Label>
                <Input
                  id="contactName"
                  placeholder="Contact person at organization"
                  value={newReferral.contactName || ''}
                  onChange={(e) => setNewReferral({ ...newReferral, contactName: e.target.value })}
                />
              </div>
              <div>
                <Label htmlFor="contactPhone">Contact Phone (Optional)</Label>
                <Input
                  id="contactPhone"
                  placeholder="(555) 123-4567"
                  value={newReferral.contactPhone || ''}
                  onChange={(e) => setNewReferral({ ...newReferral, contactPhone: e.target.value })}
                />
              </div>
            </div>
            
            <div>
              <Label htmlFor="contactEmail">Contact Email (Optional)</Label>
              <Input
                id="contactEmail"
                type="email"
                placeholder="contact@organization.org"
                value={newReferral.contactEmail || ''}
                onChange={(e) => setNewReferral({ ...newReferral, contactEmail: e.target.value })}
              />
            </div>
            
            <div>
              <Label htmlFor="reason">Reason for Referral</Label>
              <Textarea
                id="reason"
                placeholder="Describe the client's needs and reason for this referral..."
                value={newReferral.reason}
                onChange={(e) => setNewReferral({ ...newReferral, reason: e.target.value })}
                rows={3}
              />
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="sentDate">Date Sent</Label>
                <Input
                  id="sentDate"
                  type="date"
                  value={newReferral.sentDate}
                  onChange={(e) => setNewReferral({ ...newReferral, sentDate: e.target.value })}
                />
              </div>
              <div>
                <Label htmlFor="followUpDate">Follow-up Date (Optional)</Label>
                <Input
                  id="followUpDate"
                  type="date"
                  value={newReferral.followUpDate || ''}
                  onChange={(e) => setNewReferral({ ...newReferral, followUpDate: e.target.value })}
                />
              </div>
            </div>
            
            <div className="space-y-3">
              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="warmHandoff"
                  checked={newReferral.isWarmHandoff}
                  onChange={(e) => setNewReferral({ ...newReferral, isWarmHandoff: e.target.checked })}
                  className="rounded border-secondary-300"
                />
                <Label htmlFor="warmHandoff">This is a warm handoff (direct introduction to service provider)</Label>
              </div>
              
              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="consent"
                  checked={newReferral.consentProvided}
                  onChange={(e) => setNewReferral({ ...newReferral, consentProvided: e.target.checked })}
                  className="rounded border-secondary-300"
                />
                <Label htmlFor="consent">Client consent obtained for information sharing</Label>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={onCloseDialog}>Cancel</Button>
            <Button onClick={handleSubmit}>Create Referral</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default ReferralsPanel;