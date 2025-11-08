import { Input, FormCheckbox as Checkbox, FormSelect } from '@haven/ui';
import { IntakeFormData } from '../../pages/intake/index-legacy';

interface SafetyPlanStepProps {
  data: IntakeFormData;
  errors: Record<string, string>;
  onChange: (updates: Partial<IntakeFormData>) => void;
}

export default function SafetyPlanStep({ data, errors, onChange }: SafetyPlanStepProps) {
  const handleSafetyPrefsChange = (field: keyof IntakeFormData['contactSafetyPrefs'], value: any) => {
    onChange({
      contactSafetyPrefs: {
        ...data.contactSafetyPrefs,
        [field]: value
      }
    });
  };

  return (
    <div className="space-y-6">
      {/* Contact Safety Preferences */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Contact Safety Preferences</h3>
        <p className="text-sm text-secondary-600 mb-4">
          These settings help ensure we contact the client safely and respect their privacy
        </p>
        
        <div className="space-y-4">
          <Checkbox
            id="okToText"
            label="Safe to send text messages"
            checked={data.contactSafetyPrefs.okToText}
            onCheckedChange={(checked) => handleSafetyPrefsChange('okToText', checked)}
            helperText="Client has confirmed it's safe to receive text messages"
          />
          
          <Checkbox
            id="okToVoicemail"
            label="Safe to leave voicemails"
            checked={data.contactSafetyPrefs.okToVoicemail}
            onCheckedChange={(checked) => handleSafetyPrefsChange('okToVoicemail', checked)}
            helperText="Client has confirmed it's safe to leave voice messages"
          />
          
          <div>
            <Input
              id="codeWord"
              label="Safety Code Word"
              value={data.contactSafetyPrefs.codeWord}
              onChange={(e) => handleSafetyPrefsChange('codeWord', e.target.value)}
              placeholder="Optional code word for verification"
              helperText="A word or phrase to verify identity when calling"
            />
          </div>
        </div>
      </div>

      {/* Quiet Hours */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Quiet Hours</h3>
        <p className="text-sm text-secondary-600 mb-4">
          Specify times when the client should not be contacted (optional)
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            id="quietHoursStart"
            type="time"
            label="Quiet Hours Start"
            value={data.contactSafetyPrefs.quietHoursStart}
            onChange={(e) => handleSafetyPrefsChange('quietHoursStart', e.target.value)}
          />
          <Input
            id="quietHoursEnd"
            type="time"
            label="Quiet Hours End"
            value={data.contactSafetyPrefs.quietHoursEnd}
            onChange={(e) => handleSafetyPrefsChange('quietHoursEnd', e.target.value)}
          />
        </div>
      </div>

      {/* Domestic Violence */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Domestic Violence Assessment</h3>
        <p className="text-sm text-secondary-600 mb-4">
          This information helps determine appropriate services and safety planning
        </p>
        
        <div className="space-y-4">
          <div className="p-4 bg-secondary-50 rounded-lg">
            <label className="block text-sm font-medium text-secondary-700 mb-2">
              Domestic Violence Victim/Survivor
            </label>
            <FormSelect
              id="domesticViolenceVictim"
              value={data.domesticViolenceVictim ? 'YES' : 'NO'}
              onChange={(value) => onChange({ domesticViolenceVictim: value === 'YES' })}
              options={[
                { value: '', label: 'Select...' },
                { value: 'NO', label: 'No' },
                { value: 'YES', label: 'Yes' },
                { value: 'UNKNOWN', label: 'Client doesn\'t know' },
                { value: 'REFUSED', label: 'Client prefers not to answer' }
              ]}
              helperText="Has the client ever been a victim of domestic violence?"
            />
          </div>
          
          {data.domesticViolenceVictim && (
            <div className="p-4 bg-warning-50 border border-warning-200 rounded-lg">
              <label className="block text-sm font-medium text-warning-800 mb-2">
                Currently Fleeing Domestic Violence
              </label>
              <FormSelect
                id="domesticViolenceFleeing"
                value={data.domesticViolenceFleeing ? 'YES' : 'NO'}
                onChange={(value) => onChange({ domesticViolenceFleeing: value === 'YES' })}
                options={[
                  { value: '', label: 'Select...' },
                  { value: 'NO', label: 'No' },
                  { value: 'YES', label: 'Yes - Currently fleeing' }
                ]}
                helperText="Is the client currently fleeing a domestic violence situation?"
              />
            </div>
          )}
        </div>
      </div>

      {/* Safe at Home Program */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Safe at Home Program</h3>
        <div className="p-4 bg-info-50 border border-info-200 rounded-lg">
          <Checkbox
            id="safeAtHomeParticipant"
            label="Enroll in Safe at Home address confidentiality program"
            checked={data.safeAtHomeParticipant}
            onCheckedChange={(checked) => onChange({ safeAtHomeParticipant: checked })}
          />
          <p className="mt-2 text-sm text-info-700">
            The Safe at Home program provides a confidential substitute mailing address for 
            survivors of domestic violence, sexual assault, stalking, or human trafficking.
          </p>
        </div>
      </div>

      {/* Safety Plan Summary */}
      {(data.domesticViolenceFleeing || data.safeAtHomeParticipant || !data.contactSafetyPrefs.okToText || !data.contactSafetyPrefs.okToVoicemail) && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex">
            <svg className="h-5 w-5 text-red-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-4a1 1 0 10-2 0v4a1 1 0 102 0v-4z" clipRule="evenodd" />
            </svg>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">
                Safety Considerations Active
              </h3>
              <ul className="mt-2 text-sm text-red-700 list-disc list-inside">
                {data.domesticViolenceFleeing && <li>Client is currently fleeing domestic violence</li>}
                {data.safeAtHomeParticipant && <li>Client enrolled in Safe at Home program</li>}
                {!data.contactSafetyPrefs.okToText && <li>Do not send text messages</li>}
                {!data.contactSafetyPrefs.okToVoicemail && <li>Do not leave voicemails</li>}
                {data.contactSafetyPrefs.codeWord && <li>Use code word for verification</li>}
                {data.addressConfidential && <li>Address is confidential</li>}
              </ul>
            </div>
          </div>
        </div>
      )}

      {/* Staff Notes */}
      <div className="bg-secondary-50 rounded-lg p-4">
        <h4 className="text-sm font-semibold text-secondary-900 mb-2">Safety Planning Notes</h4>
        <p className="text-sm text-secondary-700 mb-3">
          Additional safety considerations or special instructions for staff:
        </p>
        <textarea
          className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
          rows={4}
          placeholder="Enter any additional safety notes or considerations..."
        />
      </div>
    </div>
  );
}