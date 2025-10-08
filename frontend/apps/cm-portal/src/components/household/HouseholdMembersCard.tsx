import React, { useState } from 'react';
import {
  useActiveHouseholdForClient,
  useHouseholdManagement,
  useIntakePsde,
  type HouseholdMember,
  type HouseholdComposition,
  type IntakePsdeData
} from '@haven/api-client';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Calendar,
  Users,
  UserPlus,
  UserMinus,
  Clock,
  Crown,
  AlertTriangle,
  Shield,
  FileText,
  Eye,
  EyeOff,
  Info
} from 'lucide-react';
import { IntakePsdeForm } from '../intake/IntakePsdeForm';

interface HouseholdMembersCardProps {
  clientId: string;
  asOfDate?: string;
  userRoles: string[];
  showPsdeIntegration?: boolean;
}

export function HouseholdMembersCard({
  clientId,
  asOfDate,
  userRoles = [],
  showPsdeIntegration = true
}: HouseholdMembersCardProps) {
  const [selectedDate, setSelectedDate] = useState<string>(asOfDate || new Date().toISOString().split('T')[0]);
  const [showPsdeForm, setShowPsdeForm] = useState(false);
  const [selectedMemberId, setSelectedMemberId] = useState<string | null>(null);
  const [psdeDataVisible, setPsdeDataVisible] = useState<{[key: string]: boolean}>({});

  const { household, loading, error, refetch } = useActiveHouseholdForClient(clientId, selectedDate);
  const { addMember, removeMember, loading: actionLoading } = useHouseholdManagement();
  const { psdeData, createPsdeRecord, updatePsdeRecord, loading: psdeLoading } = useIntakePsde();

  // Check user permissions
  const canAccessDvData = userRoles.some(role =>
    ['DV_SPECIALIST', 'ADMIN', 'CASE_MANAGER', 'SAFETY_COORDINATOR'].includes(role));
  const canAccessSensitiveData = userRoles.some(role =>
    ['DV_SPECIALIST', 'ADMIN'].includes(role));
  const canCreatePsde = userRoles.some(role =>
    ['DV_SPECIALIST', 'ADMIN', 'CASE_MANAGER', 'DATA_ENTRY_SPECIALIST'].includes(role));

  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-center space-x-2">
            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
            <span className="text-secondary-600">Loading household information...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center space-x-2 text-red-600">
            <AlertTriangle className="h-4 w-4" />
            <span>Error loading household: {error}</span>
            <Button size="sm" variant="outline" onClick={refetch}>
              Retry
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!household) {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center space-x-2">
              <Users className="h-5 w-5" />
              <span>Household Composition</span>
            </CardTitle>
            <Button size="sm" variant="outline">
              <UserPlus className="w-4 h-4 mr-2" />
              Create Household
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="text-center text-secondary-600">
            <Users className="h-12 w-12 mx-auto mb-4 text-secondary-300" />
            <p>No active household found for this client.</p>
            <p className="text-sm mt-2">Create a household composition to manage family members.</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center space-x-2">
            <Users className="h-5 w-5" />
            <span>Household Composition</span>
            <Badge variant="outline" className="ml-2">
              {household.householdType.replace('_', ' ').toLowerCase()}
            </Badge>
          </CardTitle>
          <div className="flex items-center space-x-2">
            <div className="flex items-center space-x-2 text-sm text-secondary-600">
              <Calendar className="h-4 w-4" />
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="border border-secondary-300 rounded px-2 py-1"
              />
            </div>
            <Button size="sm" variant="outline">
              <UserPlus className="w-4 h-4 mr-2" />
              Add Member
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          
          {/* Household Statistics */}
          <div className="grid grid-cols-3 gap-4 p-4 bg-secondary-50 rounded-lg">
            <div className="text-center">
              <div className="text-2xl font-bold text-primary">{household.currentHouseholdSize}</div>
              <div className="text-sm text-secondary-600">Total Members</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">{household.activeChildrenCount}</div>
              <div className="text-sm text-secondary-600">Children</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">
                {household.activeMembers.filter(m => !m.isHeadOfHousehold).length}
              </div>
              <div className="text-sm text-secondary-600">Other Members</div>
            </div>
          </div>

          {/* Head of Household */}
          <div className="border border-secondary-200 rounded-lg p-4 bg-amber-50">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <Crown className="h-5 w-5 text-amber-600" />
                <h4 className="font-medium text-secondary-900">Head of Household</h4>
              </div>
              <Badge variant="secondary">
                Since {new Date(household.compositionDate).toLocaleDateString()}
              </Badge>
            </div>
            <div>
              <p className="font-medium">{household.headOfHouseholdFullName}</p>
              {household.headOfHouseholdDateOfBirth && (
                <p className="text-sm text-secondary-600">
                  Born: {new Date(household.headOfHouseholdDateOfBirth).toLocaleDateString()}
                </p>
              )}
            </div>
          </div>

          {/* Active Members */}
          {household.activeMembers
            .filter(member => !member.isHeadOfHousehold)
            .map((member) => (
              <HouseholdMemberCard 
                key={member.membershipId} 
                member={member}
                householdId={household.id}
                onRemove={async (reason) => {
                  await removeMember(household.id, member.memberId, {
                    effectiveDate: new Date().toISOString().split('T')[0],
                    recordedBy: 'current-user', // This should come from auth context
                    reason: reason
                  });
                  refetch();
                }}
                loading={actionLoading}
              />
            ))}

          {/* Custody Changes */}
          {household.custodyChanges.length > 0 && (
            <div className="mt-6">
              <h4 className="font-medium text-secondary-900 mb-3 flex items-center space-x-2">
                <Clock className="h-4 w-4" />
                <span>Recent Custody Changes</span>
              </h4>
              <div className="space-y-2">
                {household.custodyChanges.map((change) => (
                  <div key={change.membershipId} className="border-l-4 border-orange-400 pl-4 py-2 bg-orange-50">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium">
                          {change.childFirstName} {change.childLastName}
                        </p>
                        <p className="text-xs text-secondary-600">
                          Custody change effective {new Date(change.effectiveDate).toLocaleDateString()}
                        </p>
                      </div>
                      <Badge variant="outline" size="sm">
                        {change.newRelationshipCode}
                      </Badge>
                    </div>
                    {change.courtOrderReference && (
                      <p className="text-xs text-secondary-500 mt-1">
                        Court Order: {change.courtOrderReference}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Household Notes */}
          {household.notes && (
            <div className="mt-4 p-3 bg-blue-50 rounded-lg">
              <h5 className="text-sm font-medium text-blue-900 mb-1">Notes</h5>
              <p className="text-sm text-blue-800">{household.notes}</p>
            </div>
          )}
        </div>
      </CardContent>

      {/* PSDE Form Modal */}
      {showPsdeForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-semibold">PSDE Data Collection</h2>
                <Button
                  variant="ghost"
                  onClick={() => {
                    setShowPsdeForm(false);
                    setSelectedMemberId(null);
                  }}
                  aria-label="Close PSDE form"
                >
                  ×
                </Button>
              </div>
              <IntakePsdeForm
                enrollmentId="" // This would need to be passed from enrollment context
                clientId={selectedMemberId || clientId}
                userRoles={userRoles}
                onSubmit={async (data) => {
                  try {
                    if (selectedMemberId) {
                      await createPsdeRecord(selectedMemberId, data);
                    } else {
                      await createPsdeRecord(clientId, data);
                    }
                    setShowPsdeForm(false);
                    setSelectedMemberId(null);
                    refetch();
                  } catch (error) {
                    console.error('Error creating PSDE record:', error);
                  }
                }}
                onCancel={() => {
                  setShowPsdeForm(false);
                  setSelectedMemberId(null);
                }}
              />
            </div>
          </div>
        </div>
      )}
    </Card>
  );
}

interface HouseholdMemberCardProps {
  member: HouseholdMember;
  householdId: string;
  onRemove: (reason: string) => Promise<void>;
  loading: boolean;
}

function HouseholdMemberCard({ member, householdId, onRemove, loading }: HouseholdMemberCardProps) {
  const [showRemoveDialog, setShowRemoveDialog] = useState(false);

  const getRelationshipBadgeColor = (relationship?: string) => {
    if (!relationship) return 'secondary';
    const rel = relationship.toLowerCase();
    if (rel.includes('child') || rel.includes('son') || rel.includes('daughter')) return 'blue';
    if (rel.includes('spouse') || rel.includes('partner')) return 'green';
    if (rel.includes('parent') || rel.includes('mother') || rel.includes('father')) return 'purple';
    return 'secondary';
  };

  const isTemporary = member.membershipEndDate !== null;

  return (
    <div className={`border rounded-lg p-4 ${isTemporary ? 'border-orange-200 bg-orange-50' : 'border-secondary-200'}`}>
      <div className="flex items-center justify-between mb-3">
        <div>
          <h4 className="font-medium text-secondary-900">
            {member.memberFullName}
          </h4>
          {member.memberDateOfBirth && (
            <p className="text-sm text-secondary-600">
              Born: {new Date(member.memberDateOfBirth).toLocaleDateString()}
            </p>
          )}
        </div>
        <div className="flex items-center space-x-2">
          <Badge variant={getRelationshipBadgeColor(member.relationshipCode)}>
            {member.relationshipDisplay || member.relationshipCode || 'Member'}
          </Badge>
          {isTemporary && (
            <Badge variant="outline" size="sm">
              Temporary
            </Badge>
          )}
          <Button
            size="sm"
            variant="outline"
            onClick={() => setShowRemoveDialog(true)}
            disabled={loading}
          >
            <UserMinus className="w-3 h-3" />
          </Button>
        </div>
      </div>

      <div className="flex items-center justify-between text-sm text-secondary-600">
        <div className="flex items-center space-x-4">
          <span>
            Member since: {new Date(member.membershipStartDate).toLocaleDateString()}
          </span>
          {member.membershipEndDate && (
            <span>
              Until: {new Date(member.membershipEndDate).toLocaleDateString()}
            </span>
          )}
        </div>
        <span>{member.membershipDurationDays} days</span>
      </div>

      {member.reason && (
        <div className="mt-3 pt-3 border-t border-secondary-200">
          <p className="text-sm text-secondary-700">{member.reason}</p>
        </div>
      )}

      {/* Simple remove confirmation dialog */}
      {showRemoveDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg max-w-md">
            <h3 className="text-lg font-medium mb-4">Remove Household Member</h3>
            <p className="text-secondary-600 mb-4">
              Are you sure you want to remove {member.memberFullName} from the household?
            </p>
            <div className="flex space-x-2">
              <Button
                variant="outline"
                onClick={() => setShowRemoveDialog(false)}
                disabled={loading}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={async () => {
                  await onRemove('Removed by case manager');
                  setShowRemoveDialog(false);
                }}
                disabled={loading}
              >
                {loading ? 'Removing...' : 'Remove'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}