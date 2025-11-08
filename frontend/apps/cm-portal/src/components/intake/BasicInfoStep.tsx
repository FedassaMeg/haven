import { Input, FormSelect, Label } from '@haven/ui';
import { IntakeFormData } from '../../pages/intake/index-legacy';

interface BasicInfoStepProps {
  data: IntakeFormData;
  errors: Record<string, string>;
  onChange: (updates: Partial<IntakeFormData>) => void;
}

export default function BasicInfoStep({ data, errors, onChange }: BasicInfoStepProps) {
  const calculateAge = (birthDate: string) => {
    if (!birthDate) return 0;
    const today = new Date();
    const birth = new Date(birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  };

  const handleSSNChange = (value: string) => {
    // Format SSN as user types
    const cleaned = value.replace(/\D/g, '');
    let formatted = cleaned;
    if (cleaned.length >= 6) {
      formatted = `${cleaned.slice(0, 3)}-${cleaned.slice(3, 5)}-${cleaned.slice(5, 9)}`;
    } else if (cleaned.length >= 3) {
      formatted = `${cleaned.slice(0, 3)}-${cleaned.slice(3)}`;
    }
    
    onChange({ socialSecurityNumber: formatted });
    
    // Update SSN data quality
    let quality = 9; // Data not collected
    if (cleaned.length === 9) {
      quality = 1; // Full SSN reported
    } else if (cleaned.length > 0) {
      quality = 2; // Partial SSN reported
    }
    onChange({ ssnDataQuality: quality });
  };

  return (
    <div className="space-y-6">
      {/* Legal Name Section */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Legal Name</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="grid w-full max-w-sm items-center gap-3">
            <Label htmlFor='firstName'>First Name</Label>
            <Input
              id="firstName"
              value={data.firstName}
              onChange={(e) => onChange({ firstName: e.target.value })}
              error={errors.firstName}
              required
              placeholder="Legal first name"
            />
          </div>
          <div className="grid w-full max-w-sm items-center gap-3">
            <Label htmlFor='lastName'>Last Name</Label>
            <Input
              id="lastName"
              value={data.lastName}
              onChange={(e) => onChange({ lastName: e.target.value })}
              error={errors.lastName}
              required
              placeholder="Legal last name"
            />
          </div>
          <div className="grid w-full max-w-sm items-center gap-3">
            <Label htmlFor='preferredName'>Preferred Name</Label>
            <Input
              id="preferredName"
              value={data.preferredName}
              onChange={(e) => onChange({ preferredName: e.target.value })}
              placeholder="Name they prefer to be called"
              helperText="Leave blank to use first name"
              className="md:col-span-2"
            />
          </div>
          <div className="grid w-full max-w-sm items-center gap-3">
            <Label htmlFor='aliasName'>Alias Name</Label>
            <Input
              id="aliasName"
              value={data.aliasName}
              onChange={(e) => onChange({ aliasName: e.target.value })}
              placeholder="Other names used (optional)"
              helperText="For client safety and identification"
              className="md:col-span-2"
            />
          </div>
        </div>
      </div>

      {/* Demographics Section */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Demographics</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormSelect
            id="gender"
            label="Administrative Gender"
            value={data.gender}
            onChange={(value) => onChange({ gender: value })}
            error={errors.gender}
            required
            options={[
              { value: '', label: 'Select Gender' },
              { value: 'MALE', label: 'Male' },
              { value: 'FEMALE', label: 'Female' },
              { value: 'OTHER', label: 'Other' },
              { value: 'UNKNOWN', label: 'Unknown/Not Disclosed' }
            ]}
            helperText="Used for system records and reporting"
          />
          <div>
            <Input
              id="birthDate"
              type="date"
              label="Date of Birth"
              value={data.birthDate}
              onChange={(e) => onChange({ birthDate: e.target.value })}
              error={errors.birthDate}
              required
              max={new Date().toISOString().split('T')[0]}
            />
            {data.birthDate && (
              <p className="text-sm text-secondary-600 mt-1">
                Age: {calculateAge(data.birthDate)} years
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Identification Section */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Identification</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <Input
              id="socialSecurityNumber"
              label="Social Security Number"
              value={data.socialSecurityNumber}
              onChange={(e) => handleSSNChange(e.target.value)}
              placeholder="XXX-XX-XXXX"
              maxLength={11}
              helperText="Optional - Enter what client provides"
            />
            <p className="text-xs text-secondary-500 mt-1">
              Data Quality: {
                data.ssnDataQuality === 1 ? 'Full SSN' :
                data.ssnDataQuality === 2 ? 'Partial SSN' :
                data.ssnDataQuality === 8 ? 'Client doesn\'t know' :
                data.ssnDataQuality === 9 ? 'Not collected' :
                data.ssnDataQuality === 99 ? 'Client refused' :
                'Not provided'
              }
            </p>
          </div>
          <FormSelect
            id="ssnDataQuality"
            label="SSN Data Quality"
            value={data.ssnDataQuality.toString()}
            onChange={(value) => onChange({ ssnDataQuality: parseInt(value) })}
            options={[
              { value: '1', label: 'Full SSN reported' },
              { value: '2', label: 'Approximate or partial SSN reported' },
              { value: '8', label: 'Client doesn\'t know' },
              { value: '9', label: 'Client refused' },
              { value: '99', label: 'Data not collected' }
            ]}
          />
        </div>
      </div>

      {/* Data Quality Section */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Data Quality Assessment</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormSelect
            id="nameDataQuality"
            label="Name Data Quality"
            value={data.nameDataQuality.toString()}
            onChange={(value) => onChange({ nameDataQuality: parseInt(value) })}
            options={[
              { value: '1', label: 'Full name reported' },
              { value: '2', label: 'Partial, street name, or code name reported' },
              { value: '8', label: 'Client doesn\'t know' },
              { value: '9', label: 'Client refused' },
              { value: '99', label: 'Data not collected' }
            ]}
            helperText="Quality of name information provided"
          />
          <FormSelect
            id="dobDataQuality"
            label="DOB Data Quality"
            value={data.dobDataQuality.toString()}
            onChange={(value) => onChange({ dobDataQuality: parseInt(value) })}
            options={[
              { value: '1', label: 'Full DOB reported' },
              { value: '2', label: 'Approximate or partial DOB reported' },
              { value: '8', label: 'Client doesn\'t know' },
              { value: '9', label: 'Client refused' },
              { value: '99', label: 'Data not collected' }
            ]}
            helperText="Quality of birth date information"
          />
        </div>
      </div>

      {/* Information Box */}
      <div className="bg-info-50 border border-info-200 rounded-lg p-4">
        <div className="flex">
          <svg className="h-5 w-5 text-info-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-info-800">
              HMIS Data Standards
            </h3>
            <p className="mt-1 text-sm text-info-700">
              This information is collected according to HMIS 2024 Universal Data Elements standards.
              All fields marked as required are necessary for federal reporting compliance.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}