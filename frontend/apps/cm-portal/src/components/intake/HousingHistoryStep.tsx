import { Input, Select } from '@haven/ui';
import { IntakeFormData } from '../../pages/intake';

interface HousingHistoryStepProps {
  data: IntakeFormData;
  errors: Record<string, string>;
  onChange: (updates: Partial<IntakeFormData>) => void;
}

const LIVING_SITUATIONS = [
  { 
    group: 'Homeless Situations',
    options: [
      { value: 'EMERGENCY_SHELTER', label: 'Emergency shelter' },
      { value: 'SAFE_HAVEN', label: 'Safe Haven' },
      { value: 'TRANSITIONAL_HOUSING', label: 'Transitional housing for homeless persons' },
      { value: 'PLACE_NOT_MEANT_FOR_HABITATION', label: 'Place not meant for habitation (street, car, abandoned building)' }
    ]
  },
  {
    group: 'Institutional Situations',
    options: [
      { value: 'PSYCHIATRIC_HOSPITAL', label: 'Psychiatric hospital or facility' },
      { value: 'SUBSTANCE_ABUSE_TREATMENT', label: 'Substance abuse treatment facility or detox' },
      { value: 'HOSPITAL', label: 'Hospital or medical facility' },
      { value: 'JAIL_PRISON', label: 'Jail, prison or juvenile detention' },
      { value: 'FOSTER_CARE_HOME', label: 'Foster care home or group home' },
      { value: 'LONG_TERM_CARE', label: 'Long-term care facility or nursing home' }
    ]
  },
  {
    group: 'Temporary Housing',
    options: [
      { value: 'DOUBLED_UP', label: 'Staying with family (temporary)' },
      { value: 'DOUBLED_UP_FRIENDS', label: 'Staying with friends (temporary)' },
      { value: 'HOTEL_MOTEL_NO_VOUCHER', label: 'Hotel or motel (self-paid)' }
    ]
  },
  {
    group: 'Permanent Housing',
    options: [
      { value: 'RENTAL_HOUSING', label: 'Rental (no subsidy)' },
      { value: 'RENTAL_WITH_SUBSIDY', label: 'Rental with VASH subsidy' },
      { value: 'RENTAL_WITH_OTHER_SUBSIDY', label: 'Rental with other subsidy' },
      { value: 'OWNED_BY_CLIENT', label: 'Owned by client' },
      { value: 'PERMANENT_HOUSING', label: 'Permanent housing for formerly homeless' },
      { value: 'RAPID_REHOUSING', label: 'Rapid re-housing' }
    ]
  },
  {
    group: 'Other',
    options: [
      { value: 'OTHER', label: 'Other' },
      { value: 'CLIENT_DOESNT_KNOW', label: 'Client doesn\'t know' },
      { value: 'CLIENT_PREFERS_NOT_TO_ANSWER', label: 'Client prefers not to answer' },
      { value: 'DATA_NOT_COLLECTED', label: 'Data not collected' }
    ]
  }
];

export default function HousingHistoryStep({ data, errors, onChange }: HousingHistoryStepProps) {
  const isHomelessSituation = () => {
    const homelessValues = ['EMERGENCY_SHELTER', 'SAFE_HAVEN', 'TRANSITIONAL_HOUSING', 'PLACE_NOT_MEANT_FOR_HABITATION'];
    return homelessValues.includes(data.priorLivingSituation);
  };

  return (
    <div className="space-y-6">
      {/* Prior Living Situation */}
      <div>
        <h3 className="text-lg font-semibold mb-4">
          Prior Living Situation <span className="text-red-500">*</span>
        </h3>
        <p className="text-sm text-secondary-600 mb-4">
          Where was the client living immediately before entering this program?
        </p>
        {errors.priorLivingSituation && (
          <p className="text-sm text-red-600 mb-2">{errors.priorLivingSituation}</p>
        )}
        <Select
          id="priorLivingSituation"
          label="Living Situation"
          value={data.priorLivingSituation}
          onChange={(value) => onChange({ priorLivingSituation: value })}
          required
        >
          <option value="">Select prior living situation</option>
          {LIVING_SITUATIONS.map(group => (
            <optgroup key={group.group} label={group.group}>
              {group.options.map(option => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </optgroup>
          ))}
        </Select>
      </div>

      {/* Length of Stay */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Length of Stay</h3>
        <p className="text-sm text-secondary-600 mb-4">
          How long was the client in their prior living situation?
        </p>
        <Select
          id="lengthOfStay"
          label="Length of Stay"
          value={data.lengthOfStay}
          onChange={(value) => onChange({ lengthOfStay: value })}
          options={[
            { value: '', label: 'Select length of stay' },
            { value: 'ONE_NIGHT', label: 'One night or less' },
            { value: 'TWO_TO_SIX_NIGHTS', label: 'Two to six nights' },
            { value: 'ONE_WEEK_TO_MONTH', label: 'One week to one month' },
            { value: 'ONE_TO_THREE_MONTHS', label: 'More than one month, but less than 90 days' },
            { value: 'THREE_MONTHS_TO_YEAR', label: '90 days or more, but less than one year' },
            { value: 'ONE_YEAR_OR_LONGER', label: 'One year or longer' },
            { value: 'CLIENT_DOESNT_KNOW', label: 'Client doesn\'t know' },
            { value: 'CLIENT_PREFERS_NOT_TO_ANSWER', label: 'Client prefers not to answer' },
            { value: 'DATA_NOT_COLLECTED', label: 'Data not collected' }
          ]}
        />
      </div>

      {/* Homelessness History - Show if prior situation was homeless */}
      {isHomelessSituation() && (
        <>
          <div>
            <h3 className="text-lg font-semibold mb-4">Homelessness History</h3>
            <p className="text-sm text-secondary-600 mb-4">
              Information about the client's history of homelessness (past 3 years)
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="timesHomelessPast3Years" className="block text-sm font-medium text-secondary-700 mb-1">
                  Number of Times Homeless
                </label>
                <Input
                  id="timesHomelessPast3Years"
                  type="number"
                  value={data.timesHomelessPast3Years.toString()}
                  onChange={(e) => onChange({ timesHomelessPast3Years: parseInt(e.target.value) || 0 })}
                  min="0"
                  max="100"
                  helperText="In the past 3 years"
                />
              </div>
              <div>
                <label htmlFor="monthsHomelessPast3Years" className="block text-sm font-medium text-secondary-700 mb-1">
                  Total Months Homeless
                </label>
                <Input
                  id="monthsHomelessPast3Years"
                  type="number"
                  value={data.monthsHomelessPast3Years.toString()}
                  onChange={(e) => onChange({ monthsHomelessPast3Years: parseInt(e.target.value) || 0 })}
                  min="0"
                  max="36"
                  helperText="In the past 3 years"
                />
              </div>
            </div>
          </div>

          {/* Chronic Homelessness Indicator */}
          {(data.monthsHomelessPast3Years >= 12 || data.timesHomelessPast3Years >= 4) && (
            <div className="bg-warning-50 border border-warning-200 rounded-lg p-4">
              <div className="flex">
                <svg className="h-5 w-5 text-warning-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-4a1 1 0 10-2 0v4a1 1 0 102 0v-4z" clipRule="evenodd" />
                </svg>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-warning-800">
                    Potential Chronic Homelessness
                  </h3>
                  <p className="mt-1 text-sm text-warning-700">
                    Based on the reported history, this client may meet the criteria for chronic homelessness.
                    Additional documentation may be required for program eligibility.
                  </p>
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {/* Program Dates */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Program Engagement Dates</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            id="dateOfEngagement"
            type="date"
            label="Date of Engagement"
            value={data.dateOfEngagement}
            onChange={(e) => onChange({ dateOfEngagement: e.target.value })}
            max={new Date().toISOString().split('T')[0]}
            helperText="First contact with street outreach"
          />
          <Input
            id="dateOfPATHEnrollment"
            type="date"
            label="Date of PATH Enrollment"
            value={data.dateOfPATHEnrollment}
            onChange={(e) => onChange({ dateOfPATHEnrollment: e.target.value })}
            max={new Date().toISOString().split('T')[0]}
            helperText="If applicable"
          />
        </div>
      </div>

      {/* Housing Assessment Priority */}
      <div className="bg-secondary-50 rounded-lg p-4">
        <h4 className="text-sm font-semibold text-secondary-900 mb-2">Housing Assessment Summary</h4>
        <ul className="text-sm text-secondary-700 space-y-1">
          <li>• Prior situation: {data.priorLivingSituation ? data.priorLivingSituation.replace(/_/g, ' ').toLowerCase() : 'Not specified'}</li>
          <li>• Length of stay: {data.lengthOfStay ? data.lengthOfStay.replace(/_/g, ' ').toLowerCase() : 'Not specified'}</li>
          {isHomelessSituation() && (
            <>
              <li>• Episodes of homelessness: {data.timesHomelessPast3Years} times in past 3 years</li>
              <li>• Total time homeless: {data.monthsHomelessPast3Years} months in past 3 years</li>
            </>
          )}
        </ul>
      </div>
    </div>
  );
}