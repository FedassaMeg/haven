import { FormSelect, FormCheckbox } from '@haven/ui';
import { IntakeFormData } from '../../pages/intake';

interface HmisDataStepProps {
  data: IntakeFormData;
  errors: Record<string, string>;
  onChange: (updates: Partial<IntakeFormData>) => void;
}

const RACE_OPTIONS = [
  { value: 'AMERICAN_INDIAN_ALASKA_NATIVE', label: 'American Indian, Alaska Native, or Indigenous' },
  { value: 'ASIAN', label: 'Asian or Asian American' },
  { value: 'BLACK_AFRICAN_AMERICAN', label: 'Black, African American, or African' },
  { value: 'HISPANIC_LATINO', label: 'Hispanic or Latino' },
  { value: 'MIDDLE_EASTERN_NORTH_AFRICAN', label: 'Middle Eastern or North African' },
  { value: 'NATIVE_HAWAIIAN_PACIFIC_ISLANDER', label: 'Native Hawaiian or Pacific Islander' },
  { value: 'WHITE', label: 'White' },
  { value: 'CLIENT_DOESNT_KNOW', label: 'Client doesn\'t know' },
  { value: 'CLIENT_PREFERS_NOT_TO_ANSWER', label: 'Client prefers not to answer' },
  { value: 'DATA_NOT_COLLECTED', label: 'Data not collected' }
];

const GENDER_OPTIONS = [
  { value: 'WOMAN', label: 'Woman (including trans woman)' },
  { value: 'MAN', label: 'Man (including trans man)' },
  { value: 'NON_BINARY', label: 'Non-binary' },
  { value: 'CULTURALLY_SPECIFIC', label: 'Culturally specific identity' },
  { value: 'TRANSGENDER', label: 'Transgender' },
  { value: 'QUESTIONING', label: 'Questioning' },
  { value: 'DIFFERENT_IDENTITY', label: 'Different identity' },
  { value: 'CLIENT_DOESNT_KNOW', label: 'Client doesn\'t know' },
  { value: 'CLIENT_PREFERS_NOT_TO_ANSWER', label: 'Client prefers not to answer' },
  { value: 'DATA_NOT_COLLECTED', label: 'Data not collected' }
];

export default function HmisDataStep({ data, errors, onChange }: HmisDataStepProps) {
  const handleRaceChange = (value: string, checked: boolean) => {
    const newRace = checked
      ? [...data.hmisRace, value]
      : data.hmisRace.filter(r => r !== value);
    onChange({ hmisRace: newRace });
  };

  const handleGenderChange = (value: string, checked: boolean) => {
    const newGender = checked
      ? [...data.hmisGender, value]
      : data.hmisGender.filter(g => g !== value);
    onChange({ hmisGender: newGender });
  };

  const calculateAge = () => {
    if (!data.birthDate) return 0;
    const today = new Date();
    const birth = new Date(data.birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  };

  const isAdult = calculateAge() >= 18;

  return (
    <div className="space-y-6">
      {/* Race Section */}
      <div>
        <h3 className="text-lg font-semibold mb-4">
          Race <span className="text-red-500">*</span>
        </h3>
        <p className="text-sm text-secondary-600 mb-4">
          Select all that apply. Client may identify with multiple racial categories.
        </p>
        {errors.hmisRace && (
          <p className="text-sm text-red-600 mb-2">{errors.hmisRace}</p>
        )}
        <div className="space-y-3">
          {RACE_OPTIONS.map(option => (
            <FormCheckbox
              key={option.value}
              id={`race-${option.value}`}
              label={option.label}
              checked={data.hmisRace.includes(option.value)}
              onCheckedChange={(checked) => handleRaceChange(option.value, checked)}
            />
          ))}
        </div>
      </div>

      {/* Gender Section */}
      <div>
        <h3 className="text-lg font-semibold mb-4">
          Gender Identity <span className="text-red-500">*</span>
        </h3>
        <p className="text-sm text-secondary-600 mb-4">
          Select all that apply. This represents the client's self-identification of gender.
        </p>
        {errors.hmisGender && (
          <p className="text-sm text-red-600 mb-2">{errors.hmisGender}</p>
        )}
        <div className="space-y-3">
          {GENDER_OPTIONS.map(option => (
            <FormCheckbox
              key={option.value}
              id={`gender-${option.value}`}
              label={option.label}
              checked={data.hmisGender.includes(option.value)}
              onCheckedChange={(checked) => handleGenderChange(option.value, checked)}
            />
          ))}
        </div>
      </div>

      {/* Veteran Status Section - Only show for adults */}
      {isAdult && (
        <div>
          <h3 className="text-lg font-semibold mb-4">Veteran Status</h3>
          <p className="text-sm text-secondary-600 mb-4">
            Has the client ever served in the United States Armed Forces?
          </p>
          <FormSelect
            id="veteranStatus"
            label="Veteran Status"
            value={data.veteranStatus}
            onChange={(value) => onChange({ veteranStatus: value })}
            options={[
              { value: '', label: 'Select veteran status' },
              { value: 'NO', label: 'No' },
              { value: 'YES', label: 'Yes' },
              { value: 'CLIENT_DOESNT_KNOW', label: 'Client doesn\'t know' },
              { value: 'CLIENT_PREFERS_NOT_TO_ANSWER', label: 'Client prefers not to answer' },
              { value: 'DATA_NOT_COLLECTED', label: 'Data not collected' }
            ]}
            required
            error={errors.veteranStatus}
          />
        </div>
      )}

      {/* Disabling Condition Section */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Disabling Condition</h3>
        <p className="text-sm text-secondary-600 mb-4">
          Does the client have a disabling condition that is expected to be of long-continued
          and indefinite duration and substantially impairs their ability to live independently?
        </p>
        <FormSelect
          id="disablingCondition"
          label="Disabling Condition"
          value={data.disablingCondition}
          onChange={(value) => onChange({ disablingCondition: value })}
          options={[
            { value: '', label: 'Select disabling condition status' },
            { value: 'NO', label: 'No' },
            { value: 'YES', label: 'Yes' },
            { value: 'CLIENT_DOESNT_KNOW', label: 'Client doesn\'t know' },
            { value: 'CLIENT_PREFERS_NOT_TO_ANSWER', label: 'Client prefers not to answer' },
            { value: 'DATA_NOT_COLLECTED', label: 'Data not collected' }
          ]}
          helperText="This information helps determine program eligibility"
          required
          error={errors.disablingCondition}
        />
      </div>

      {/* HMIS Compliance Note */}
      <div className="bg-warning-50 border border-warning-200 rounded-lg p-4">
        <div className="flex">
          <svg className="h-5 w-5 text-warning-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-warning-800">
              Required for Federal Reporting
            </h3>
            <p className="mt-1 text-sm text-warning-700">
              Race and Gender Identity are required Universal Data Elements under HMIS 2024 standards.
              These fields must be collected for all clients to maintain compliance with federal reporting requirements.
            </p>
          </div>
        </div>
      </div>

      {/* Data Quality Summary */}
      <div className="bg-secondary-50 rounded-lg p-4">
        <h4 className="text-sm font-semibold text-secondary-900 mb-2">Data Quality Summary</h4>
        <ul className="text-sm text-secondary-700 space-y-1">
          <li>• Race selections: {data.hmisRace.length} {data.hmisRace.length === 1 ? 'category' : 'categories'}</li>
          <li>• Gender selections: {data.hmisGender.length} {data.hmisGender.length === 1 ? 'identity' : 'identities'}</li>
          <li>• Veteran status: {data.veteranStatus.replace(/_/g, ' ').toLowerCase()}</li>
          <li>• Disabling condition: {data.disablingCondition.replace(/_/g, ' ').toLowerCase()}</li>
        </ul>
      </div>
    </div>
  );
}