import { useState, useEffect, useCallback } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Input, Label, Textarea, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@haven/ui';
import type { SafetyPlan, SafetyPlanSection, SafeContact, SectionVisibility, ContactMethod } from '@haven/api-client/src/types/safety-plan';
import QuickHideButton from './QuickHideButton';

interface SafetyPlanEditorProps {
  plan: SafetyPlan;
  onSave: (plan: SafetyPlan) => void;
  onCancel: () => void;
  onQuickHide: () => void;
  autoSaveEnabled?: boolean;
}

const SafetyPlanEditor: React.FC<SafetyPlanEditorProps> = ({
  plan: initialPlan,
  onSave,
  onCancel,
  onQuickHide,
  autoSaveEnabled = true
}) => {
  const [plan, setPlan] = useState<SafetyPlan>(initialPlan);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [lastAutoSave, setLastAutoSave] = useState<Date | null>(null);

  // Auto-save functionality
  useEffect(() => {
    if (!autoSaveEnabled || !hasUnsavedChanges) return;

    const autoSaveTimer = setTimeout(() => {
      handleAutoSave();
    }, 30000); // Auto-save every 30 seconds

    return () => clearTimeout(autoSaveTimer);
  }, [plan, hasUnsavedChanges, autoSaveEnabled]);

  const handleAutoSave = useCallback(() => {
    // In real app, this would save to draft
    console.log('Auto-saving draft...', plan);
    setLastAutoSave(new Date());
    setHasUnsavedChanges(false);
  }, [plan]);

  const updateSection = (sectionName: keyof SafetyPlan, content: string) => {
    const section = plan[sectionName] as SafetyPlanSection;
    if (section && typeof section === 'object' && 'content' in section) {
      setPlan({
        ...plan,
        [sectionName]: {
          ...section,
          content,
          items: content.split('\n').filter(item => item.trim()),
          lastModified: new Date().toISOString(),
          modifiedBy: 'current-user'
        }
      });
      setHasUnsavedChanges(true);
    }
  };

  const updateSectionVisibility = (sectionName: keyof SafetyPlan, visibility: SectionVisibility) => {
    const section = plan[sectionName] as SafetyPlanSection;
    if (section && typeof section === 'object' && 'visibility' in section) {
      setPlan({
        ...plan,
        [sectionName]: {
          ...section,
          visibility
        }
      });
      setHasUnsavedChanges(true);
    }
  };

  const addSafeContact = () => {
    const newContact: SafeContact = {
      id: `sc-new-${Date.now()}`,
      name: '',
      relationship: '',
      contactMethod: 'CALL_ONLY' as ContactMethod,
      safetyNotes: '',
      isEmergencyContact: false,
      isPrimaryContact: false,
      visibility: 'CLIENT_AND_CASE_MANAGER' as SectionVisibility
    };
    
    setPlan({
      ...plan,
      safeContacts: [...plan.safeContacts, newContact]
    });
    setHasUnsavedChanges(true);
  };

  const updateSafeContact = (index: number, updates: Partial<SafeContact>) => {
    const updatedContacts = [...plan.safeContacts];
    updatedContacts[index] = { ...updatedContacts[index], ...updates };
    setPlan({ ...plan, safeContacts: updatedContacts });
    setHasUnsavedChanges(true);
  };

  const removeSafeContact = (index: number) => {
    setPlan({
      ...plan,
      safeContacts: plan.safeContacts.filter((_, i) => i !== index)
    });
    setHasUnsavedChanges(true);
  };

  const handleSave = () => {
    const updatedPlan = {
      ...plan,
      updatedAt: new Date().toISOString(),
      updatedBy: 'current-user',
      updatedByName: 'Current User'
    };
    onSave(updatedPlan);
  };

  return (
    <div className="space-y-6">
      {/* Editor Header */}
      <Card>
        <CardContent>
          <div className="flex items-center justify-between py-4">
            <div>
              <h3 className="text-lg font-semibold text-secondary-900">
                Edit Safety Plan - Version {plan.version}
              </h3>
              <div className="flex items-center space-x-4 mt-1">
                <Badge variant={plan.status === 'DRAFT' ? 'warning' : 'secondary'}>
                  {plan.status}
                </Badge>
                {autoSaveEnabled && (
                  <div className="flex items-center space-x-2 text-sm text-secondary-600">
                    <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span>Auto-Save Enabled</span>
                    {lastAutoSave && (
                      <span className="text-secondary-500">
                        (Last saved: {lastAutoSave.toLocaleTimeString()})
                      </span>
                    )}
                  </div>
                )}
              </div>
            </div>
            <QuickHideButton onClick={onQuickHide} />
          </div>
        </CardContent>
      </Card>

      {/* Section: Triggers / Risks */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Section: Triggers / Risks</CardTitle>
            <Select 
              value={plan.triggersAndRisks.visibility}
              onValueChange={(value) => updateSectionVisibility('triggersAndRisks', value as SectionVisibility)}
            >
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="CLIENT_ONLY">Client Only</SelectItem>
                <SelectItem value="CLIENT_AND_CASE_MANAGER">Client & Case Manager</SelectItem>
                <SelectItem value="STAFF_ONLY">Staff Only</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          <Textarea
            placeholder="Example: partner's release dates, stalking behaviors, specific locations to avoid..."
            value={plan.triggersAndRisks.content}
            onChange={(e) => updateSection('triggersAndRisks', e.target.value)}
            rows={4}
            className="w-full"
          />
        </CardContent>
      </Card>

      {/* Section: Warning Signs */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Section: Warning Signs</CardTitle>
            <Select 
              value={plan.warningSign.visibility}
              onValueChange={(value) => updateSectionVisibility('warningSign', value as SectionVisibility)}
            >
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="CLIENT_ONLY">Client Only</SelectItem>
                <SelectItem value="CLIENT_AND_CASE_MANAGER">Client & Case Manager</SelectItem>
                <SelectItem value="STAFF_ONLY">Staff Only</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          <Textarea
            placeholder="Example: unexpected messages, unknown calls, seeing familiar vehicles..."
            value={plan.warningSign.content}
            onChange={(e) => updateSection('warningSign', e.target.value)}
            rows={4}
            className="w-full"
          />
        </CardContent>
      </Card>

      {/* Section: Safe Contacts */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <CardTitle className="text-base">Section: Safe Contacts</CardTitle>
              <Badge variant="outline">Restricted Visibility</Badge>
            </div>
            <Button size="sm" onClick={addSafeContact}>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
              Add Contact
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {plan.safeContacts.map((contact, index) => (
              <div key={contact.id} className="border border-secondary-200 rounded-lg p-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label>Name</Label>
                    <Input
                      value={contact.name}
                      onChange={(e) => updateSafeContact(index, { name: e.target.value })}
                      placeholder="Contact name"
                    />
                  </div>
                  <div>
                    <Label>Relationship</Label>
                    <Input
                      value={contact.relationship}
                      onChange={(e) => updateSafeContact(index, { relationship: e.target.value })}
                      placeholder="e.g., Sister, Case Manager, Friend"
                    />
                  </div>
                  <div>
                    <Label>Phone (Optional)</Label>
                    <Input
                      value={contact.phone || ''}
                      onChange={(e) => updateSafeContact(index, { phone: e.target.value })}
                      placeholder="(555) 123-4567"
                    />
                  </div>
                  <div>
                    <Label>Contact Method</Label>
                    <Select 
                      value={contact.contactMethod}
                      onValueChange={(value) => updateSafeContact(index, { contactMethod: value as ContactMethod })}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="CALL_ONLY">Call Only</SelectItem>
                        <SelectItem value="TEXT_ONLY">Text Only</SelectItem>
                        <SelectItem value="EMAIL_ONLY">Email Only</SelectItem>
                        <SelectItem value="IN_PERSON">In Person</SelectItem>
                        <SelectItem value="SECURE_APP">Secure App</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="col-span-2">
                    <Label>Safety Notes</Label>
                    <Input
                      value={contact.safetyNotes}
                      onChange={(e) => updateSafeContact(index, { safetyNotes: e.target.value })}
                      placeholder="e.g., Calls only, never text"
                    />
                  </div>
                  <div>
                    <Label>Visibility</Label>
                    <Select 
                      value={contact.visibility}
                      onValueChange={(value) => updateSafeContact(index, { visibility: value as SectionVisibility })}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="CLIENT_AND_CASE_MANAGER">Client & Case Manager</SelectItem>
                        <SelectItem value="STAFF_ONLY">Staff Only (Hidden from Client)</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="flex items-end justify-between">
                    <div className="space-y-2">
                      <label className="flex items-center space-x-2">
                        <input
                          type="checkbox"
                          checked={contact.isEmergencyContact}
                          onChange={(e) => updateSafeContact(index, { isEmergencyContact: e.target.checked })}
                          className="rounded border-secondary-300"
                        />
                        <span className="text-sm">Emergency Contact</span>
                      </label>
                      <label className="flex items-center space-x-2">
                        <input
                          type="checkbox"
                          checked={contact.isPrimaryContact}
                          onChange={(e) => updateSafeContact(index, { isPrimaryContact: e.target.checked })}
                          className="rounded border-secondary-300"
                        />
                        <span className="text-sm">Primary Contact</span>
                      </label>
                    </div>
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => removeSafeContact(index)}
                    >
                      Remove
                    </Button>
                  </div>
                </div>
              </div>
            ))}
            {plan.safeContacts.length === 0 && (
              <p className="text-center text-secondary-500 py-4">
                No safe contacts added. Click "Add Contact" to add one.
              </p>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Section: Escape Plan */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Section: Escape Plan</CardTitle>
            <Select 
              value={plan.escapePlan.visibility}
              onValueChange={(value) => updateSectionVisibility('escapePlan', value as SectionVisibility)}
            >
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="CLIENT_ONLY">Client Only</SelectItem>
                <SelectItem value="CLIENT_AND_CASE_MANAGER">Client & Case Manager</SelectItem>
                <SelectItem value="STAFF_ONLY">Staff Only</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          <Textarea
            placeholder="Example: Identify exit routes, pack emergency bag, safe meeting locations, neighbor alerts..."
            value={plan.escapePlan.content}
            onChange={(e) => updateSection('escapePlan', e.target.value)}
            rows={4}
            className="w-full"
          />
        </CardContent>
      </Card>

      {/* Section: Tech Safety */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Section: Tech Safety</CardTitle>
            <Select 
              value={plan.techSafety.visibility}
              onValueChange={(value) => updateSectionVisibility('techSafety', value as SectionVisibility)}
            >
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="CLIENT_ONLY">Client Only</SelectItem>
                <SelectItem value="CLIENT_AND_CASE_MANAGER">Client & Case Manager</SelectItem>
                <SelectItem value="STAFF_ONLY">Staff Only</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          <Textarea
            placeholder="Example: Change passwords regularly, avoid geotagging, secure devices, private browsing..."
            value={plan.techSafety.content}
            onChange={(e) => updateSection('techSafety', e.target.value)}
            rows={4}
            className="w-full"
          />
        </CardContent>
      </Card>

      {/* Action Buttons */}
      <div className="bg-white border border-secondary-200 rounded-lg p-4">
        <div className="flex items-center justify-between">
          <Button variant="outline" onClick={onCancel}>Cancel</Button>
          <div className="flex items-center space-x-3">
            {hasUnsavedChanges && (
              <span className="text-sm text-warning-600">Unsaved changes</span>
            )}
            <Button onClick={handleSave}>Save Version</Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SafetyPlanEditor;