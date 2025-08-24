import { FormCheckbox as Checkbox, Input, Select } from '@haven/ui';
import { IntakeFormData } from '../../pages/intake';

interface DocumentsStepProps {
  data: IntakeFormData;
  errors: Record<string, string>;
  onChange: (updates: Partial<IntakeFormData>) => void;
}

export default function DocumentsStep({ data, errors, onChange }: DocumentsStepProps) {
  return (
    <div className="space-y-6">
      {/* Consent Forms */}
      <div>
        <h3 className="text-lg font-semibold mb-4">
          Consent Forms <span className="text-red-500">*</span>
        </h3>
        <p className="text-sm text-secondary-600 mb-4">
          Client must provide informed consent for services and data sharing
        </p>
        {errors.consent && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md">
            <p className="text-sm text-red-600">{errors.consent}</p>
          </div>
        )}
        
        <div className="space-y-4">
          <div className="p-4 border border-secondary-200 rounded-lg">
            <Checkbox
              id="consentToServices"
              label="Consent to Receive Services"
              checked={data.consentToServices}
              onCheckedChange={(checked) => onChange({ consentToServices: checked })}
              required
            />
            <p className="mt-2 ml-7 text-sm text-secondary-600">
              Client agrees to receive case management and support services
            </p>
          </div>
          
          <div className="p-4 border border-secondary-200 rounded-lg">
            <Checkbox
              id="consentToDataSharing"
              label="Consent to Data Sharing"
              checked={data.consentToDataSharing}
              onCheckedChange={(checked) => onChange({ consentToDataSharing: checked })}
            />
            <p className="mt-2 ml-7 text-sm text-secondary-600">
              Client agrees to share information with partner agencies for coordinated care
            </p>
          </div>
          
          <div className="p-4 border border-secondary-200 rounded-lg">
            <Checkbox
              id="consentToHmisParticipation"
              label="HMIS Participation Agreement"
              checked={data.consentToHmisParticipation}
              onCheckedChange={(checked) => onChange({ consentToHmisParticipation: checked })}
            />
            <p className="mt-2 ml-7 text-sm text-secondary-600">
              Client agrees to have their information entered into the HMIS system
            </p>
          </div>
        </div>
      </div>

      {/* Document Verification */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Document Verification</h3>
        <p className="text-sm text-secondary-600 mb-4">
          Check all documents that have been verified and are on file
        </p>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 bg-secondary-50 rounded-lg">
            <Checkbox
              id="photoIdOnFile"
              label="Photo ID"
              checked={data.photoIdOnFile}
              onCheckedChange={(checked) => onChange({ photoIdOnFile: checked })}
            />
            <p className="mt-1 ml-7 text-xs text-secondary-600">
              Driver's license, state ID, passport
            </p>
          </div>
          
          <div className="p-4 bg-secondary-50 rounded-lg">
            <Checkbox
              id="birthCertificateOnFile"
              label="Birth Certificate"
              checked={data.birthCertificateOnFile}
              onCheckedChange={(checked) => onChange({ birthCertificateOnFile: checked })}
            />
            <p className="mt-1 ml-7 text-xs text-secondary-600">
              Original or certified copy
            </p>
          </div>
          
          <div className="p-4 bg-secondary-50 rounded-lg">
            <Checkbox
              id="ssnCardOnFile"
              label="Social Security Card"
              checked={data.ssnCardOnFile}
              onCheckedChange={(checked) => onChange({ ssnCardOnFile: checked })}
            />
            <p className="mt-1 ml-7 text-xs text-secondary-600">
              Original card or award letter
            </p>
          </div>
          
          <div className="p-4 bg-secondary-50 rounded-lg">
            <Checkbox
              id="insuranceCardOnFile"
              label="Insurance Card"
              checked={data.insuranceCardOnFile}
              onCheckedChange={(checked) => onChange({ insuranceCardOnFile: checked })}
            />
            <p className="mt-1 ml-7 text-xs text-secondary-600">
              Medical, Medicare, Medicaid
            </p>
          </div>
        </div>
      </div>

      {/* Intake Information */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Intake Information</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            id="intakeWorker"
            label="Intake Worker Name"
            value={data.intakeWorker}
            onChange={(e) => onChange({ intakeWorker: e.target.value })}
            placeholder="Staff member conducting intake"
            required
          />
          <Input
            id="intakeDate"
            type="date"
            label="Intake Date"
            value={data.intakeDate}
            onChange={(e) => onChange({ intakeDate: e.target.value })}
            max={new Date().toISOString().split('T')[0]}
            required
          />
          <Input
            id="intakeLocation"
            label="Intake Location"
            value={data.intakeLocation}
            onChange={(e) => onChange({ intakeLocation: e.target.value })}
            placeholder="Office, shelter, outreach site, etc."
          />
          <Select
            id="dataCollectionStage"
            label="Data Collection Stage"
            value={data.dataCollectionStage}
            onChange={(value) => onChange({ dataCollectionStage: value })}
            options={[
              { value: 'PROJECT_START', label: 'Project Start' },
              { value: 'PROJECT_UPDATE', label: 'Project Update' },
              { value: 'PROJECT_ANNUAL', label: 'Annual Assessment' },
              { value: 'PROJECT_EXIT', label: 'Project Exit' },
              { value: 'POST_EXIT', label: 'Post-Exit' }
            ]}
            helperText="HMIS data collection point"
          />
        </div>
      </div>

      {/* Consent Summary */}
      <div className="bg-info-50 border border-info-200 rounded-lg p-4">
        <div className="flex">
          <svg className="h-5 w-5 text-info-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-info-800">
              Consent Requirements
            </h3>
            <p className="mt-1 text-sm text-info-700">
              Consent to services is required to complete the intake process. Other consents are voluntary
              but may affect the client's eligibility for certain programs or services.
            </p>
          </div>
        </div>
      </div>

      {/* Document Checklist Summary */}
      <div className="bg-secondary-50 rounded-lg p-4">
        <h4 className="text-sm font-semibold text-secondary-900 mb-2">Document Checklist</h4>
        <div className="grid grid-cols-2 gap-2 text-sm text-secondary-700">
          <div className="flex items-center">
            {data.consentToServices ? (
              <svg className="h-4 w-4 text-success-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
            ) : (
              <svg className="h-4 w-4 text-secondary-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            )}
            Service Consent
          </div>
          <div className="flex items-center">
            {data.photoIdOnFile ? (
              <svg className="h-4 w-4 text-success-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
            ) : (
              <svg className="h-4 w-4 text-secondary-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            )}
            Photo ID
          </div>
          <div className="flex items-center">
            {data.consentToDataSharing ? (
              <svg className="h-4 w-4 text-success-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
            ) : (
              <svg className="h-4 w-4 text-secondary-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            )}
            Data Sharing
          </div>
          <div className="flex items-center">
            {data.birthCertificateOnFile ? (
              <svg className="h-4 w-4 text-success-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
            ) : (
              <svg className="h-4 w-4 text-secondary-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            )}
            Birth Certificate
          </div>
        </div>
      </div>
    </div>
  );
}