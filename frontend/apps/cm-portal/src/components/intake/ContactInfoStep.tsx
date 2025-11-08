import { Input, FormSelect, FormCheckbox as Checkbox } from '@haven/ui';
import { IntakeFormData } from '../../pages/intake/index-legacy';

interface ContactInfoStepProps {
  data: IntakeFormData;
  errors: Record<string, string>;
  onChange: (updates: Partial<IntakeFormData>) => void;
}

export default function ContactInfoStep({ data, errors, onChange }: ContactInfoStepProps) {
  const formatPhone = (value: string) => {
    const cleaned = value.replace(/\D/g, '');
    if (cleaned.length <= 3) return cleaned;
    if (cleaned.length <= 6) return `(${cleaned.slice(0, 3)}) ${cleaned.slice(3)}`;
    return `(${cleaned.slice(0, 3)}) ${cleaned.slice(3, 6)}-${cleaned.slice(6, 10)}`;
  };

  return (
    <div className="space-y-6">
      {/* Contact Methods */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Contact Information</h3>
        {errors.contact && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md">
            <p className="text-sm text-red-600">{errors.contact}</p>
          </div>
        )}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            id="primaryPhone"
            label="Primary Phone"
            value={data.primaryPhone}
            onChange={(e) => onChange({ primaryPhone: formatPhone(e.target.value) })}
            error={errors.primaryPhone}
            placeholder="(555) 123-4567"
            helperText="Main contact number"
          />
          <Input
            id="secondaryPhone"
            label="Secondary Phone"
            value={data.secondaryPhone}
            onChange={(e) => onChange({ secondaryPhone: formatPhone(e.target.value) })}
            error={errors.secondaryPhone}
            placeholder="(555) 123-4567"
            helperText="Alternative or work number"
          />
          <Input
            id="email"
            type="email"
            label="Email Address"
            value={data.email}
            onChange={(e) => onChange({ email: e.target.value })}
            error={errors.email}
            placeholder="client@example.com"
            className="md:col-span-2"
          />
        </div>
      </div>

      {/* Communication Preferences */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Communication Preferences</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormSelect
            id="preferredContactMethod"
            label="Preferred Contact Method"
            value={data.preferredContactMethod}
            onChange={(value) => onChange({ preferredContactMethod: value })}
            options={[
              { value: 'PHONE', label: 'Phone Call' },
              { value: 'TEXT', label: 'Text Message' },
              { value: 'EMAIL', label: 'Email' },
              { value: 'MAIL', label: 'Postal Mail' },
              { value: 'IN_PERSON', label: 'In Person Only' }
            ]}
          />
          <FormSelect
            id="preferredLanguage"
            label="Preferred Language"
            value={data.preferredLanguage}
            onChange={(value) => onChange({ preferredLanguage: value })}
            options={[
              { value: 'en', label: 'English' },
              { value: 'es', label: 'Spanish' },
              { value: 'fr', label: 'French' },
              { value: 'zh', label: 'Chinese' },
              { value: 'vi', label: 'Vietnamese' },
              { value: 'ar', label: 'Arabic' },
              { value: 'ru', label: 'Russian' },
              { value: 'pt', label: 'Portuguese' },
              { value: 'other', label: 'Other' }
            ]}
          />
        </div>
      </div>

      {/* Current Address */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Current Address</h3>
        <div className="mb-4">
          <Checkbox
            id="addressConfidential"
            label="Keep address confidential for safety reasons"
            checked={data.addressConfidential}
            onCheckedChange={(checked) => onChange({ addressConfidential: checked })}
          />
        </div>
        <div className="space-y-4">
          <Input
            id="addressLine1"
            label="Address Line 1"
            value={data.addressLine1}
            onChange={(e) => onChange({ addressLine1: e.target.value })}
            placeholder="Street address, P.O. box"
            disabled={data.addressConfidential}
          />
          <Input
            id="addressLine2"
            label="Address Line 2"
            value={data.addressLine2}
            onChange={(e) => onChange({ addressLine2: e.target.value })}
            placeholder="Apartment, suite, unit, building"
            disabled={data.addressConfidential}
          />
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input
              id="city"
              label="City"
              value={data.city}
              onChange={(e) => onChange({ city: e.target.value })}
              placeholder="City"
              disabled={data.addressConfidential}
            />
            <Input
              id="state"
              label="State"
              value={data.state}
              onChange={(e) => onChange({ state: e.target.value.toUpperCase() })}
              placeholder="State"
              maxLength={2}
              disabled={data.addressConfidential}
            />
            <Input
              id="postalCode"
              label="ZIP Code"
              value={data.postalCode}
              onChange={(e) => onChange({ postalCode: e.target.value })}
              placeholder="12345"
              maxLength={10}
              disabled={data.addressConfidential}
            />
          </div>
        </div>
        {data.addressConfidential && (
          <div className="mt-3 p-3 bg-warning-50 border border-warning-200 rounded-md">
            <p className="text-sm text-warning-800">
              Address marked as confidential for client safety. This information will be restricted in the system.
            </p>
          </div>
        )}
      </div>

      {/* Emergency Contact */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Emergency Contact</h3>
        <p className="text-sm text-secondary-600 mb-4">
          Person to contact in case of emergency (optional)
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            id="emergencyContactName"
            label="Contact Name"
            value={data.emergencyContactName}
            onChange={(e) => onChange({ emergencyContactName: e.target.value })}
            placeholder="Full name"
          />
          <Input
            id="emergencyContactRelationship"
            label="Relationship"
            value={data.emergencyContactRelationship}
            onChange={(e) => onChange({ emergencyContactRelationship: e.target.value })}
            placeholder="Spouse, Parent, Friend, etc."
          />
          <Input
            id="emergencyContactPhone"
            label="Phone Number"
            value={data.emergencyContactPhone}
            onChange={(e) => onChange({ emergencyContactPhone: formatPhone(e.target.value) })}
            error={errors.emergencyContactPhone}
            placeholder="(555) 123-4567"
          />
          <Input
            id="emergencyContactEmail"
            type="email"
            label="Email Address"
            value={data.emergencyContactEmail}
            onChange={(e) => onChange({ emergencyContactEmail: e.target.value })}
            error={errors.emergencyContactEmail}
            placeholder="contact@example.com"
          />
        </div>
      </div>

      {/* Privacy Notice */}
      <div className="bg-info-50 border border-info-200 rounded-lg p-4">
        <div className="flex">
          <svg className="h-5 w-5 text-info-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-info-800">
              Privacy & Confidentiality
            </h3>
            <p className="mt-1 text-sm text-info-700">
              All contact information is protected under HIPAA and will only be used for service coordination
              and emergency purposes. Clients have the right to update or restrict their contact information at any time.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}