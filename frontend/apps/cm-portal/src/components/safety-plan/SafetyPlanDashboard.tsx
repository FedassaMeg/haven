import { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button } from '@haven/ui';
import type { SafetyPlan, SafetyPlanSection, SafeContact, SectionVisibility } from '@haven/api-client/src/types/safety-plan';

interface SafetyPlanDashboardProps {
  plan: SafetyPlan;
  onEdit: () => void;
  onNewVersion: () => void;
  onQuickHide: () => void;
}

const SafetyPlanDashboard: React.FC<SafetyPlanDashboardProps> = ({
  plan,
  onEdit,
  onNewVersion,
  onQuickHide
}) => {
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set(['triggers', 'warning', 'contacts', 'escape', 'tech']));
  const [showRestricted, setShowRestricted] = useState(false);

  const toggleSection = (section: string) => {
    const newExpanded = new Set(expandedSections);
    if (newExpanded.has(section)) {
      newExpanded.delete(section);
    } else {
      newExpanded.add(section);
    }
    setExpandedSections(newExpanded);
  };

  const canViewSection = (visibility: SectionVisibility): boolean => {
    // In real app, this would check user role
    if (visibility === 'STAFF_ONLY') {
      return showRestricted;
    }
    return true;
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const getVisibilityIcon = (visibility: SectionVisibility) => {
    switch (visibility) {
      case 'STAFF_ONLY':
        return '🔒';
      case 'CLIENT_ONLY':
        return '👤';
      case 'EMERGENCY_ONLY':
        return '🚨';
      case 'HIDDEN':
        return '👁️‍🗨️';
      default:
        return '';
    }
  };

  const renderSection = (
    title: string,
    section: SafetyPlanSection | undefined,
    sectionKey: string,
    icon: string = '📋'
  ) => {
    if (!section) return null;
    if (!canViewSection(section.visibility)) return null;

    const isExpanded = expandedSections.has(sectionKey);

    return (
      <div className="border border-secondary-200 rounded-lg overflow-hidden">
        <div
          className="flex items-center justify-between p-4 bg-white hover:bg-secondary-50 cursor-pointer transition-colors"
          onClick={() => toggleSection(sectionKey)}
        >
          <div className="flex items-center space-x-3">
            <span className="text-lg">{icon}</span>
            <h3 className="font-medium text-secondary-900">SECTION: {title}</h3>
            {section.visibility !== 'CLIENT_AND_CASE_MANAGER' && (
              <Badge variant="outline" size="sm">
                {getVisibilityIcon(section.visibility)} {section.visibility.replace(/_/g, ' ')}
              </Badge>
            )}
          </div>
          <svg
            className={`w-5 h-5 text-secondary-500 transform transition-transform ${isExpanded ? 'rotate-180' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </div>
        {isExpanded && (
          <div className="px-4 pb-4 bg-secondary-50">
            {section.items && section.items.length > 0 ? (
              <ul className="space-y-2">
                {section.items.map((item, index) => (
                  <li key={index} className="flex items-start">
                    <span className="text-secondary-500 mr-2">•</span>
                    <span className="text-secondary-700">{item}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-secondary-700 whitespace-pre-wrap">{section.content}</p>
            )}
            <div className="mt-3 text-xs text-secondary-500">
              Last modified: {formatDate(section.lastModified)} by {section.modifiedBy}
            </div>
          </div>
        )}
      </div>
    );
  };

  const renderContacts = () => {
    if (!plan.safeContacts || plan.safeContacts.length === 0) return null;

    const isExpanded = expandedSections.has('contacts');

    return (
      <div className="border border-secondary-200 rounded-lg overflow-hidden">
        <div
          className="flex items-center justify-between p-4 bg-white hover:bg-secondary-50 cursor-pointer transition-colors"
          onClick={() => toggleSection('contacts')}
        >
          <div className="flex items-center space-x-3">
            <span className="text-lg">☎️</span>
            <h3 className="font-medium text-secondary-900">SECTION: Safe Contacts</h3>
            <Badge variant="outline" size="sm">
              🔐 Restricted Visibility
            </Badge>
          </div>
          <svg
            className={`w-5 h-5 text-secondary-500 transform transition-transform ${isExpanded ? 'rotate-180' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </div>
        {isExpanded && (
          <div className="px-4 pb-4 bg-secondary-50">
            <ul className="space-y-3">
              {plan.safeContacts.map((contact) => {
                if (!canViewSection(contact.visibility)) {
                  return (
                    <li key={contact.id} className="flex items-start">
                      <span className="text-secondary-500 mr-2">•</span>
                      <span className="text-secondary-500 italic">[Hidden - Staff Only]</span>
                    </li>
                  );
                }
                
                return (
                  <li key={contact.id} className="flex items-start">
                    <span className="text-secondary-500 mr-2">•</span>
                    <div className="flex-1">
                      <div className="flex items-center space-x-2">
                        <span className="font-medium text-secondary-900">{contact.name}</span>
                        <span className="text-secondary-600">– {contact.safetyNotes}</span>
                        {contact.isEmergencyContact && (
                          <Badge variant="destructive" size="sm">Emergency</Badge>
                        )}
                      </div>
                      {contact.phone && contact.visibility !== 'STAFF_ONLY' && (
                        <p className="text-sm text-secondary-600 mt-1">
                          {contact.phone} ({contact.contactMethod.replace(/_/g, ' ').toLowerCase()})
                        </p>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          </div>
        )}
      </div>
    );
  };

  const reminders = [
    {
      id: '1',
      task: 'Review plan monthly',
      dueDate: plan.nextReviewDate,
      isCompleted: false
    },
    {
      id: '2',
      task: 'Update safe contacts list',
      dueDate: null,
      isCompleted: false
    }
  ];

  return (
    <div className="space-y-6">
      {/* Plan Header */}
      <Card>
        <CardContent>
          <div className="flex items-center justify-between py-4">
            <div>
              <h3 className="text-lg font-semibold text-secondary-900">
                Current Safety Plan - Active v{plan.version}
              </h3>
              <p className="text-sm text-secondary-600">
                Updated: {formatDate(plan.updatedAt)} by {plan.updatedByName || plan.createdByName}
              </p>
            </div>
            <div className="flex items-center space-x-3">
              <Badge variant="success">Active</Badge>
              {plan.quickHideEnabled && (
                <Badge variant="outline">
                  <svg className="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                  </svg>
                  Quick Hide
                </Badge>
              )}
              {plan.autoSaveEnabled && (
                <Badge variant="outline">
                  <svg className="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V2" />
                  </svg>
                  Auto-Save
                </Badge>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Plan Sections */}
      <div className="space-y-4">
        {renderSection('Triggers / Risks', plan.triggersAndRisks, 'triggers', '⚠️')}
        {renderSection('Warning Signs', plan.warningSign, 'warning', '🚨')}
        {renderContacts()}
        {renderSection('Escape Plan', plan.escapePlan, 'escape', '🏃')}
        {renderSection('Tech Safety', plan.techSafety, 'tech', '📱')}
        {plan.copingStrategies && renderSection('Coping Strategies', plan.copingStrategies, 'coping', '💪')}
        {plan.importantDocuments && renderSection('Important Documents', plan.importantDocuments, 'documents', '📄')}
        {plan.childrenSafety && renderSection('Children Safety', plan.childrenSafety, 'children', '👶')}
        {plan.petSafety && renderSection('Pet Safety', plan.petSafety, 'pets', '🐾')}
      </div>

      {/* Tasks & Reminders */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Tasks & Reminders</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {reminders.map((reminder) => (
              <div key={reminder.id} className="flex items-center space-x-3">
                <input
                  type="checkbox"
                  checked={reminder.isCompleted}
                  onChange={() => {}}
                  className="rounded border-secondary-300"
                />
                <div className="flex-1">
                  <span className="text-sm text-secondary-700">{reminder.task}</span>
                  {reminder.dueDate && (
                    <span className="text-sm text-secondary-500 ml-2">
                      (next due: {formatDate(reminder.dueDate)})
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Action Footer */}
      <div className="bg-white border border-secondary-200 rounded-lg p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <Button variant="outline" onClick={onEdit}>Save Draft</Button>
            <Button onClick={() => {}}>Activate</Button>
          </div>
          <Button 
            variant="outline"
            onClick={() => setShowRestricted(!showRestricted)}
          >
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
            </svg>
            {showRestricted ? 'Hide' : 'Show'} Restricted Access
          </Button>
        </div>
      </div>
    </div>
  );
};

export default SafetyPlanDashboard;