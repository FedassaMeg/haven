import { IntakeFormData } from '../../pages/intake';

interface ReviewStepProps {
  data: IntakeFormData;
  onEdit: (step: number) => void;
}

export default function ReviewStep({ data, onEdit }: ReviewStepProps) {
  const formatPhone = (phone: string) => phone || 'Not provided';
  const formatDate = (date: string) => {
    if (!date) return 'Not provided';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  return (
    <div className="space-y-6">
      <div className="bg-info-50 border border-info-200 rounded-lg p-4 mb-6">
        <p className="text-sm text-info-800">
          Please review all information carefully before submitting. Click on any section to edit.
        </p>
      </div>

      {/* Basic Information */}
      <div className="border border-secondary-200 rounded-lg p-4">
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-semibold">Basic Information</h3>
          <button
            onClick={() => onEdit(1)}
            className="text-sm text-primary-600 hover:text-primary-700"
          >
            Edit
          </button>
        </div>
        <dl className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="font-medium text-secondary-600">Legal Name</dt>
            <dd className="mt-1 text-secondary-900">{data.firstName} {data.lastName}</dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Preferred Name</dt>
            <dd className="mt-1 text-secondary-900">{data.preferredName || data.firstName}</dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Date of Birth</dt>
            <dd className="mt-1 text-secondary-900">{formatDate(data.birthDate)}</dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Gender</dt>
            <dd className="mt-1 text-secondary-900">{data.gender}</dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">SSN</dt>
            <dd className="mt-1 text-secondary-900">
              {data.socialSecurityNumber ? `***-**-${data.socialSecurityNumber.slice(-4)}` : 'Not provided'}
            </dd>
          </div>
          {data.aliasName && (
            <div>
              <dt className="font-medium text-secondary-600">Alias</dt>
              <dd className="mt-1 text-secondary-900">{data.aliasName}</dd>
            </div>
          )}
        </dl>
      </div>

      {/* HMIS Data */}
      <div className="border border-secondary-200 rounded-lg p-4">
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-semibold">HMIS Data</h3>
          <button
            onClick={() => onEdit(2)}
            className="text-sm text-primary-600 hover:text-primary-700"
          >
            Edit
          </button>
        </div>
        <dl className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="font-medium text-secondary-600">Race</dt>
            <dd className="mt-1 text-secondary-900">
              {data.hmisRace.length > 0 ? data.hmisRace.join(', ') : 'Not specified'}
            </dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Gender Identity</dt>
            <dd className="mt-1 text-secondary-900">
              {data.hmisGender.length > 0 ? data.hmisGender.join(', ') : 'Not specified'}
            </dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Veteran Status</dt>
            <dd className="mt-1 text-secondary-900">{data.veteranStatus.replace(/_/g, ' ')}</dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Disabling Condition</dt>
            <dd className="mt-1 text-secondary-900">{data.disablingCondition.replace(/_/g, ' ')}</dd>
          </div>
        </dl>
      </div>

      {/* Contact Information */}
      <div className="border border-secondary-200 rounded-lg p-4">
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-semibold">Contact Information</h3>
          <button
            onClick={() => onEdit(3)}
            className="text-sm text-primary-600 hover:text-primary-700"
          >
            Edit
          </button>
        </div>
        <dl className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="font-medium text-secondary-600">Primary Phone</dt>
            <dd className="mt-1 text-secondary-900">{formatPhone(data.primaryPhone)}</dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Email</dt>
            <dd className="mt-1 text-secondary-900">{data.email || 'Not provided'}</dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Address</dt>
            <dd className="mt-1 text-secondary-900">
              {data.addressConfidential ? (
                <span className="text-warning-600">Confidential</span>
              ) : data.addressLine1 ? (
                <>
                  {data.addressLine1}<br />
                  {data.addressLine2 && <>{data.addressLine2}<br /></>}
                  {data.city}, {data.state} {data.postalCode}
                </>
              ) : (
                'Not provided'
              )}
            </dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Emergency Contact</dt>
            <dd className="mt-1 text-secondary-900">
              {data.emergencyContactName || 'Not provided'}
              {data.emergencyContactRelationship && ` (${data.emergencyContactRelationship})`}
            </dd>
          </div>
        </dl>
      </div>

      {/* Housing History */}
      <div className="border border-secondary-200 rounded-lg p-4">
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-semibold">Housing History</h3>
          <button
            onClick={() => onEdit(4)}
            className="text-sm text-primary-600 hover:text-primary-700"
          >
            Edit
          </button>
        </div>
        <dl className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="font-medium text-secondary-600">Prior Living Situation</dt>
            <dd className="mt-1 text-secondary-900">
              {data.priorLivingSituation ? data.priorLivingSituation.replace(/_/g, ' ') : 'Not specified'}
            </dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Length of Stay</dt>
            <dd className="mt-1 text-secondary-900">
              {data.lengthOfStay ? data.lengthOfStay.replace(/_/g, ' ') : 'Not specified'}
            </dd>
          </div>
          {data.timesHomelessPast3Years > 0 && (
            <div>
              <dt className="font-medium text-secondary-600">Homelessness Episodes</dt>
              <dd className="mt-1 text-secondary-900">
                {data.timesHomelessPast3Years} times in past 3 years
              </dd>
            </div>
          )}
          {data.monthsHomelessPast3Years > 0 && (
            <div>
              <dt className="font-medium text-secondary-600">Total Time Homeless</dt>
              <dd className="mt-1 text-secondary-900">
                {data.monthsHomelessPast3Years} months in past 3 years
              </dd>
            </div>
          )}
        </dl>
      </div>

      {/* Safety Planning */}
      <div className="border border-secondary-200 rounded-lg p-4">
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-semibold">Safety Planning</h3>
          <button
            onClick={() => onEdit(5)}
            className="text-sm text-primary-600 hover:text-primary-700"
          >
            Edit
          </button>
        </div>
        <dl className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="font-medium text-secondary-600">Text Messages</dt>
            <dd className="mt-1 text-secondary-900">
              {data.contactSafetyPrefs.okToText ? 'Safe to text' : 'Do not text'}
            </dd>
          </div>
          <div>
            <dt className="font-medium text-secondary-600">Voicemails</dt>
            <dd className="mt-1 text-secondary-900">
              {data.contactSafetyPrefs.okToVoicemail ? 'Safe to leave voicemail' : 'Do not leave voicemail'}
            </dd>
          </div>
          {data.contactSafetyPrefs.codeWord && (
            <div>
              <dt className="font-medium text-secondary-600">Code Word</dt>
              <dd className="mt-1 text-secondary-900">Set</dd>
            </div>
          )}
          {data.safeAtHomeParticipant && (
            <div>
              <dt className="font-medium text-secondary-600">Safe at Home</dt>
              <dd className="mt-1 text-secondary-900">Enrolled</dd>
            </div>
          )}
          {data.domesticViolenceVictim && (
            <div>
              <dt className="font-medium text-secondary-600">DV Status</dt>
              <dd className="mt-1 text-secondary-900">
                {data.domesticViolenceFleeing ? 'Currently fleeing' : 'Survivor'}
              </dd>
            </div>
          )}
        </dl>
      </div>

      {/* Documents & Consent */}
      <div className="border border-secondary-200 rounded-lg p-4">
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-semibold">Documents & Consent</h3>
          <button
            onClick={() => onEdit(6)}
            className="text-sm text-primary-600 hover:text-primary-700"
          >
            Edit
          </button>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <h4 className="font-medium text-secondary-600 text-sm mb-2">Consent Forms</h4>
            <ul className="text-sm space-y-1">
              <li className="flex items-center">
                {data.consentToServices ? (
                  <svg className="h-4 w-4 text-success-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                ) : (
                  <svg className="h-4 w-4 text-secondary-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                )}
                Service Consent
              </li>
              <li className="flex items-center">
                {data.consentToDataSharing ? (
                  <svg className="h-4 w-4 text-success-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                ) : (
                  <svg className="h-4 w-4 text-secondary-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                )}
                Data Sharing
              </li>
              <li className="flex items-center">
                {data.consentToHmisParticipation ? (
                  <svg className="h-4 w-4 text-success-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                ) : (
                  <svg className="h-4 w-4 text-secondary-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                )}
                HMIS Participation
              </li>
            </ul>
          </div>
          <div>
            <h4 className="font-medium text-secondary-600 text-sm mb-2">Documents on File</h4>
            <ul className="text-sm space-y-1">
              {data.photoIdOnFile && <li>• Photo ID</li>}
              {data.birthCertificateOnFile && <li>• Birth Certificate</li>}
              {data.ssnCardOnFile && <li>• Social Security Card</li>}
              {data.insuranceCardOnFile && <li>• Insurance Card</li>}
              {!data.photoIdOnFile && !data.birthCertificateOnFile && 
               !data.ssnCardOnFile && !data.insuranceCardOnFile && (
                <li className="text-secondary-500">No documents on file</li>
              )}
            </ul>
          </div>
        </div>
        <div className="mt-4 pt-4 border-t border-secondary-200">
          <dl className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <dt className="font-medium text-secondary-600">Intake Worker</dt>
              <dd className="mt-1 text-secondary-900">{data.intakeWorker || 'Not specified'}</dd>
            </div>
            <div>
              <dt className="font-medium text-secondary-600">Intake Date</dt>
              <dd className="mt-1 text-secondary-900">{formatDate(data.intakeDate)}</dd>
            </div>
          </dl>
        </div>
      </div>

      {/* Completion Notice */}
      <div className="bg-success-50 border border-success-200 rounded-lg p-4">
        <div className="flex">
          <svg className="h-5 w-5 text-success-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-success-800">
              Ready to Submit
            </h3>
            <p className="mt-1 text-sm text-success-700">
              All required information has been collected. Click "Complete Intake" to create the client profile.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}