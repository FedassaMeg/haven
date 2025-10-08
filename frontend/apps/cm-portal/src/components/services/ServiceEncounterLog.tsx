import { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Textarea } from '@haven/ui';
import type { ServiceEncounter, ServiceEncounterType, ServiceLocation } from '@haven/api-client/src/types/services';

interface ServiceEncounterLogProps {
  clientId: string;
  encounters: ServiceEncounter[];
  onAddEncounter: () => void;
  showAddDialog: boolean;
  onCloseDialog: () => void;
}

const ServiceEncounterLog: React.FC<ServiceEncounterLogProps> = ({
  clientId,
  encounters,
  onAddEncounter,
  showAddDialog,
  onCloseDialog
}) => {
  const [newEncounter, setNewEncounter] = useState<Partial<ServiceEncounter>>({
    clientId,
    type: 'CASE_MANAGEMENT' as ServiceEncounterType,
    location: 'OFFICE' as ServiceLocation,
    date: new Date().toISOString().split('T')[0],
    notes: ''
  });

  const handleSubmit = async () => {
    // API call to create encounter
    console.log('Creating encounter:', newEncounter);
    onCloseDialog();
  };

  const getEncounterIcon = (type: ServiceEncounterType) => {
    switch (type) {
      case 'CASE_MANAGEMENT':
        return <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>;
      case 'COUNSELING':
        return <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
        </svg>;
      case 'HOUSING_NAVIGATION':
        return <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>;
      default:
        return <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>;
    }
  };

  const getLocationBadgeVariant = (location: ServiceLocation) => {
    switch (location) {
      case 'VIRTUAL':
      case 'PHONE':
        return 'primary';
      case 'HOME':
      case 'SAFE_LOCATION':
        return 'warning';
      case 'COURT':
        return 'destructive';
      default:
        return 'secondary';
    }
  };

  // Mock data for demonstration
  const mockEncounters: ServiceEncounter[] = [
    {
      id: '1',
      clientId,
      date: '2024-05-01',
      type: 'CASE_MANAGEMENT' as ServiceEncounterType,
      location: 'SAFE_LOCATION' as ServiceLocation,
      provider: 'Sarah Johnson',
      providerId: 'provider-1',
      notes: 'Met at safe café. Updated housing search. Client expressed interest in 2BR units in North district.',
      duration: 60,
      createdAt: '2024-05-01T10:00:00Z',
      updatedAt: '2024-05-01T10:00:00Z'
    },
    {
      id: '2',
      clientId,
      date: '2024-05-03',
      type: 'COUNSELING' as ServiceEncounterType,
      location: 'PHONE' as ServiceLocation,
      provider: 'Dr. Emily Chen',
      providerId: 'provider-2',
      notes: 'Phone call; discussed anxiety coping skills. Client reported improved sleep patterns.',
      duration: 45,
      createdAt: '2024-05-03T14:00:00Z',
      updatedAt: '2024-05-03T14:00:00Z'
    },
    {
      id: '3',
      clientId,
      date: '2024-05-10',
      type: 'HOUSING_NAVIGATION' as ServiceEncounterType,
      location: 'COMMUNITY' as ServiceLocation,
      provider: 'Mike Rodriguez',
      providerId: 'provider-3',
      notes: 'Toured 2 units. RFTA packet started. Unit A: $1200/mo, available June 1. Unit B: $1100/mo, available immediately.',
      duration: 120,
      followUpRequired: true,
      followUpDate: '2024-05-15',
      createdAt: '2024-05-10T09:00:00Z',
      updatedAt: '2024-05-10T09:00:00Z'
    }
  ];

  const displayEncounters = encounters.length > 0 ? encounters : mockEncounters;

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Service Log Timeline</CardTitle>
            <div className="flex items-center space-x-2">
              <Select defaultValue="all">
                <SelectTrigger className="w-32">
                  <SelectValue placeholder="Filter" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Types</SelectItem>
                  <SelectItem value="case_mgmt">Case Mgmt</SelectItem>
                  <SelectItem value="counseling">Counseling</SelectItem>
                  <SelectItem value="housing">Housing</SelectItem>
                  <SelectItem value="legal">Legal</SelectItem>
                </SelectContent>
              </Select>
              <Button size="sm" variant="outline">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {displayEncounters.map((encounter, index) => (
              <div key={encounter.id} className="relative">
                {/* Timeline connector */}
                {index < displayEncounters.length - 1 && (
                  <div className="absolute left-6 top-12 bottom-0 w-0.5 bg-secondary-200"></div>
                )}
                
                {/* Encounter Entry */}
                <div className="flex items-start space-x-4">
                  {/* Date and Icon */}
                  <div className="flex-shrink-0">
                    <div className="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center text-primary-600">
                      {getEncounterIcon(encounter.type)}
                    </div>
                  </div>
                  
                  {/* Content */}
                  <div className="flex-1 bg-white border border-secondary-200 rounded-lg p-4">
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <div className="flex items-center space-x-2 mb-1">
                          <span className="text-sm font-medium text-secondary-900">
                            {new Date(encounter.date).toLocaleDateString('en-US', { 
                              month: 'short', 
                              day: 'numeric',
                              year: 'numeric'
                            })}
                          </span>
                          <Badge variant="outline" size="sm">
                            {encounter.type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                          </Badge>
                          <Badge 
                            variant={getLocationBadgeVariant(encounter.location)} 
                            size="sm"
                          >
                            {encounter.location}
                          </Badge>
                          {encounter.duration && (
                            <span className="text-xs text-secondary-500">
                              {encounter.duration} min
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-secondary-600">
                          Provider: {encounter.provider}
                        </p>
                      </div>
                      <Button size="sm" variant="ghost">
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
                        </svg>
                      </Button>
                    </div>
                    
                    <p className="text-sm text-secondary-700 mb-2">{encounter.notes}</p>
                    
                    {encounter.followUpRequired && (
                      <div className="flex items-center space-x-2 pt-2 border-t border-secondary-100">
                        <svg className="w-4 h-4 text-warning-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <span className="text-xs text-warning-700">
                          Follow-up required by {encounter.followUpDate && new Date(encounter.followUpDate).toLocaleDateString()}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
          
          {displayEncounters.length === 0 && (
            <div className="text-center py-8">
              <svg className="w-12 h-12 mx-auto mb-4 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <p className="text-secondary-600">No service encounters recorded</p>
              <Button onClick={onAddEncounter} className="mt-4" size="sm">
                Add First Encounter
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add Encounter Dialog */}
      <Dialog open={showAddDialog} onOpenChange={onCloseDialog}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Add Service Encounter</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="date">Date</Label>
                <Input
                  id="date"
                  type="date"
                  value={newEncounter.date}
                  onChange={(e) => setNewEncounter({ ...newEncounter, date: e.target.value })}
                />
              </div>
              <div>
                <Label htmlFor="duration">Duration (minutes)</Label>
                <Input
                  id="duration"
                  type="number"
                  placeholder="60"
                  value={newEncounter.duration || ''}
                  onChange={(e) => setNewEncounter({ ...newEncounter, duration: parseInt(e.target.value) })}
                />
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="type">Service Type</Label>
                <Select 
                  value={newEncounter.type}
                  onValueChange={(value) => setNewEncounter({ ...newEncounter, type: value as ServiceEncounterType })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="CASE_MANAGEMENT">Case Management</SelectItem>
                    <SelectItem value="COUNSELING">Counseling</SelectItem>
                    <SelectItem value="HOUSING_NAVIGATION">Housing Navigation</SelectItem>
                    <SelectItem value="LEGAL_ADVOCACY">Legal Advocacy</SelectItem>
                    <SelectItem value="SAFETY_PLANNING">Safety Planning</SelectItem>
                    <SelectItem value="CRISIS_INTERVENTION">Crisis Intervention</SelectItem>
                    <SelectItem value="GROUP_SESSION">Group Session</SelectItem>
                    <SelectItem value="PHONE_CALL">Phone Call</SelectItem>
                    <SelectItem value="EMAIL">Email</SelectItem>
                    <SelectItem value="OTHER">Other</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="location">Location</Label>
                <Select 
                  value={newEncounter.location}
                  onValueChange={(value) => setNewEncounter({ ...newEncounter, location: value as ServiceLocation })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select location" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="OFFICE">Office</SelectItem>
                    <SelectItem value="HOME">Home</SelectItem>
                    <SelectItem value="COMMUNITY">Community</SelectItem>
                    <SelectItem value="COURT">Court</SelectItem>
                    <SelectItem value="VIRTUAL">Virtual</SelectItem>
                    <SelectItem value="PHONE">Phone</SelectItem>
                    <SelectItem value="SAFE_LOCATION">Safe Location</SelectItem>
                    <SelectItem value="OTHER">Other</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            
            <div>
              <Label htmlFor="notes">Notes / Actions Taken</Label>
              <Textarea
                id="notes"
                placeholder="Describe the service provided and any important details..."
                value={newEncounter.notes}
                onChange={(e) => setNewEncounter({ ...newEncounter, notes: e.target.value })}
                rows={4}
              />
            </div>
            
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="followup"
                checked={newEncounter.followUpRequired || false}
                onChange={(e) => setNewEncounter({ ...newEncounter, followUpRequired: e.target.checked })}
                className="rounded border-secondary-300"
              />
              <Label htmlFor="followup">Follow-up required</Label>
            </div>
            
            {newEncounter.followUpRequired && (
              <div>
                <Label htmlFor="followup-date">Follow-up Date</Label>
                <Input
                  id="followup-date"
                  type="date"
                  value={newEncounter.followUpDate || ''}
                  onChange={(e) => setNewEncounter({ ...newEncounter, followUpDate: e.target.value })}
                />
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={onCloseDialog}>Cancel</Button>
            <Button onClick={handleSubmit}>Add Encounter</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default ServiceEncounterLog;