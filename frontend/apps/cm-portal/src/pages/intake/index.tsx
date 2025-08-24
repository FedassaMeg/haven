import { useState } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button } from '@haven/ui';
import { useCreateClient } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';
import BasicInfoStep from '../../components/intake/BasicInfoStep';
import HmisDataStep from '../../components/intake/HmisDataStep';
import ContactInfoStep from '../../components/intake/ContactInfoStep';
import HousingHistoryStep from '../../components/intake/HousingHistoryStep';
import SafetyPlanStep from '../../components/intake/SafetyPlanStep';
import DocumentsStep from '../../components/intake/DocumentsStep';
import ReviewStep from '../../components/intake/ReviewStep';

export interface IntakeFormData {
  // Step 1: Basic Information
  firstName: string;
  lastName: string;
  preferredName: string;
  aliasName: string;
  gender: string;
  birthDate: string;
  socialSecurityNumber: string;
  ssnDataQuality: number;
  
  // Step 2: HMIS Data Collection
  hmisRace: string[];
  hmisGender: string[];
  veteranStatus: string;
  disablingCondition: string;
  nameDataQuality: number;
  dobDataQuality: number;
  
  // Step 3: Contact Information
  primaryPhone: string;
  secondaryPhone: string;
  email: string;
  preferredContactMethod: string;
  preferredLanguage: string;
  
  // Address
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  addressConfidential: boolean;
  
  // Emergency Contact
  emergencyContactName: string;
  emergencyContactRelationship: string;
  emergencyContactPhone: string;
  emergencyContactEmail: string;
  
  // Step 4: Housing History
  priorLivingSituation: string;
  lengthOfStay: string;
  timesHomelessPast3Years: number;
  monthsHomelessPast3Years: number;
  dateOfEngagement: string;
  dateOfPATHEnrollment: string;
  
  // Step 5: Safety Planning
  contactSafetyPrefs: {
    okToText: boolean;
    okToVoicemail: boolean;
    codeWord: string;
    quietHoursStart: string;
    quietHoursEnd: string;
  };
  safeAtHomeParticipant: boolean;
  domesticViolenceVictim: boolean;
  domesticViolenceFleeing: boolean;
  
  // Step 6: Documents & Consent
  consentToServices: boolean;
  consentToDataSharing: boolean;
  consentToHmisParticipation: boolean;
  photoIdOnFile: boolean;
  birthCertificateOnFile: boolean;
  ssnCardOnFile: boolean;
  insuranceCardOnFile: boolean;
  
  // Metadata
  intakeWorker: string;
  intakeDate: string;
  intakeLocation: string;
  dataCollectionStage: string;
}

const initialFormData: IntakeFormData = {
  firstName: '',
  lastName: '',
  preferredName: '',
  aliasName: '',
  gender: '',
  birthDate: '',
  socialSecurityNumber: '',
  ssnDataQuality: 9,
  
  hmisRace: [],
  hmisGender: [],
  veteranStatus: 'DATA_NOT_COLLECTED',
  disablingCondition: 'DATA_NOT_COLLECTED',
  nameDataQuality: 1,
  dobDataQuality: 1,
  
  primaryPhone: '',
  secondaryPhone: '',
  email: '',
  preferredContactMethod: 'PHONE',
  preferredLanguage: 'en',
  
  addressLine1: '',
  addressLine2: '',
  city: '',
  state: '',
  postalCode: '',
  country: 'US',
  addressConfidential: false,
  
  emergencyContactName: '',
  emergencyContactRelationship: '',
  emergencyContactPhone: '',
  emergencyContactEmail: '',
  
  priorLivingSituation: '',
  lengthOfStay: '',
  timesHomelessPast3Years: 0,
  monthsHomelessPast3Years: 0,
  dateOfEngagement: '',
  dateOfPATHEnrollment: '',
  
  contactSafetyPrefs: {
    okToText: true,
    okToVoicemail: true,
    codeWord: '',
    quietHoursStart: '',
    quietHoursEnd: ''
  },
  safeAtHomeParticipant: false,
  domesticViolenceVictim: false,
  domesticViolenceFleeing: false,
  
  consentToServices: false,
  consentToDataSharing: false,
  consentToHmisParticipation: false,
  photoIdOnFile: false,
  birthCertificateOnFile: false,
  ssnCardOnFile: false,
  insuranceCardOnFile: false,
  
  intakeWorker: '',
  intakeDate: new Date().toISOString().split('T')[0],
  intakeLocation: '',
  dataCollectionStage: 'PROJECT_START'
};

const STEPS = [
  { id: 1, name: 'Basic Information', description: 'Name, DOB, and identification' },
  { id: 2, name: 'HMIS Data', description: 'Race, gender, veteran status' },
  { id: 3, name: 'Contact', description: 'Phone, address, emergency contact' },
  { id: 4, name: 'Housing History', description: 'Prior living situation and homelessness' },
  { id: 5, name: 'Safety Planning', description: 'Contact preferences and safety needs' },
  { id: 6, name: 'Documents', description: 'Consent forms and document verification' },
  { id: 7, name: 'Review', description: 'Review and submit intake' }
];

export default function IntakePage() {
  const router = useRouter();
  const { createClient, loading: creating } = useCreateClient();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState<IntakeFormData>(initialFormData);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [stepValidation, setStepValidation] = useState<Record<number, boolean>>({});

  const updateFormData = (updates: Partial<IntakeFormData>) => {
    setFormData(prev => ({ ...prev, ...updates }));
  };

  const validateStep = (step: number): boolean => {
    const newErrors: Record<string, string> = {};
    let isValid = true;

    switch (step) {
      case 1: // Basic Information
        if (!formData.firstName.trim()) {
          newErrors.firstName = 'First name is required';
          isValid = false;
        }
        if (!formData.lastName.trim()) {
          newErrors.lastName = 'Last name is required';
          isValid = false;
        }
        if (!formData.gender) {
          newErrors.gender = 'Gender is required';
          isValid = false;
        }
        if (!formData.birthDate) {
          newErrors.birthDate = 'Date of birth is required';
          isValid = false;
        }
        break;
      
      case 2: // HMIS Data
        if (formData.hmisRace.length === 0) {
          newErrors.hmisRace = 'At least one race selection is required';
          isValid = false;
        }
        if (formData.hmisGender.length === 0) {
          newErrors.hmisGender = 'At least one gender selection is required';
          isValid = false;
        }
        break;
      
      case 3: // Contact Information
        if (!formData.primaryPhone && !formData.email) {
          newErrors.contact = 'At least one contact method is required';
          isValid = false;
        }
        break;
      
      case 4: // Housing History
        if (!formData.priorLivingSituation) {
          newErrors.priorLivingSituation = 'Prior living situation is required';
          isValid = false;
        }
        break;
      
      case 6: // Documents & Consent
        if (!formData.consentToServices) {
          newErrors.consent = 'Consent to services is required';
          isValid = false;
        }
        break;
    }

    setErrors(newErrors);
    setStepValidation(prev => ({ ...prev, [step]: isValid }));
    return isValid;
  };

  const handleNext = () => {
    if (validateStep(currentStep)) {
      setCurrentStep(prev => Math.min(prev + 1, STEPS.length));
    }
  };

  const handlePrevious = () => {
    setCurrentStep(prev => Math.max(prev - 1, 1));
  };

  const handleSubmit = async () => {
    try {
      // Transform intake data to client creation format
      const clientData = {
        // Basic demographics
        name: {
          use: 'OFFICIAL',
          family: formData.lastName,
          given: [formData.firstName],
          text: formData.preferredName || `${formData.firstName} ${formData.lastName}`
        },
        gender: formData.gender,
        birthDate: formData.birthDate,
        
        // HMIS data
        hmisRace: formData.hmisRace,
        hmisGender: formData.hmisGender,
        veteranStatus: formData.veteranStatus,
        disablingCondition: formData.disablingCondition,
        socialSecurityNumber: formData.socialSecurityNumber,
        nameDataQuality: formData.nameDataQuality,
        ssnDataQuality: formData.ssnDataQuality,
        dobDataQuality: formData.dobDataQuality,
        
        // Contact information
        addresses: formData.addressLine1 ? [{
          use: 'HOME',
          line: [formData.addressLine1, formData.addressLine2].filter(Boolean),
          city: formData.city,
          state: formData.state,
          postalCode: formData.postalCode,
          country: formData.country
        }] : [],
        
        telecoms: [
          formData.primaryPhone && {
            system: 'PHONE',
            value: formData.primaryPhone,
            use: 'HOME'
          },
          formData.secondaryPhone && {
            system: 'PHONE',
            value: formData.secondaryPhone,
            use: 'WORK'
          },
          formData.email && {
            system: 'EMAIL',
            value: formData.email,
            use: 'HOME'
          }
        ].filter(Boolean),
        
        // Emergency contact
        contact: formData.emergencyContactName ? [{
          relationship: [{
            text: formData.emergencyContactRelationship || 'Emergency Contact'
          }],
          name: {
            text: formData.emergencyContactName
          },
          telecom: [
            formData.emergencyContactPhone && {
              system: 'PHONE',
              value: formData.emergencyContactPhone
            },
            formData.emergencyContactEmail && {
              system: 'EMAIL',
              value: formData.emergencyContactEmail
            }
          ].filter(Boolean)
        }] : [],
        
        // Safety preferences
        aliasName: formData.aliasName,
        contactSafetyPrefs: formData.contactSafetyPrefs,
        addressConfidentiality: formData.addressConfidential ? {
          level: 'CONFIDENTIAL',
          reason: 'CLIENT_SAFETY'
        } : null,
        safeAtHomeParticipant: formData.safeAtHomeParticipant,
        
        // Housing history
        priorLivingSituation: formData.priorLivingSituation,
        lengthOfStay: formData.lengthOfStay,
        timesHomelessPast3Years: formData.timesHomelessPast3Years,
        monthsHomelessPast3Years: formData.monthsHomelessPast3Years,
        
        // Consent and documents
        consentToServices: formData.consentToServices,
        consentToDataSharing: formData.consentToDataSharing,
        consentToHmisParticipation: formData.consentToHmisParticipation,
        
        // Metadata
        intakeDate: formData.intakeDate,
        intakeWorker: formData.intakeWorker,
        intakeLocation: formData.intakeLocation,
        dataCollectionStage: formData.dataCollectionStage,
        
        status: 'ACTIVE'
      };

      const createdClient = await createClient(clientData);
      router.push(`/clients/${createdClient.id}?intake=complete`);
    } catch (error) {
      console.error('Failed to complete intake:', error);
    }
  };

  const renderStep = () => {
    switch (currentStep) {
      case 1:
        return (
          <BasicInfoStep
            data={formData}
            errors={errors}
            onChange={updateFormData}
          />
        );
      case 2:
        return (
          <HmisDataStep
            data={formData}
            errors={errors}
            onChange={updateFormData}
          />
        );
      case 3:
        return (
          <ContactInfoStep
            data={formData}
            errors={errors}
            onChange={updateFormData}
          />
        );
      case 4:
        return (
          <HousingHistoryStep
            data={formData}
            errors={errors}
            onChange={updateFormData}
          />
        );
      case 5:
        return (
          <SafetyPlanStep
            data={formData}
            errors={errors}
            onChange={updateFormData}
          />
        );
      case 6:
        return (
          <DocumentsStep
            data={formData}
            errors={errors}
            onChange={updateFormData}
          />
        );
      case 7:
        return (
          <ReviewStep
            data={formData}
            onEdit={(step) => setCurrentStep(step)}
          />
        );
      default:
        return null;
    }
  };

  return (
    <ProtectedRoute>
      <AppLayout
        title="Client Intake"
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients', href: '/clients' },
          { label: 'New Intake' }
        ]}
      >
        <div className="p-6">
          <div className="max-w-5xl mx-auto">
            {/* Header */}
            <div className="mb-6">
              <h1 className="text-3xl font-bold text-secondary-900">Client Intake Workflow</h1>
              <p className="text-secondary-600 mt-2">
                Complete intake process following HMIS 2024 standards
              </p>
            </div>

            {/* Progress Steps */}
            <div className="mb-8">
              <nav aria-label="Progress">
                <ol className="flex items-center justify-between">
                  {STEPS.map((step, index) => (
                    <li key={step.id} className="relative flex-1">
                      {index !== 0 && (
                        <div
                          className={`absolute left-0 top-5 -ml-px mt-0.5 h-0.5 w-full ${
                            currentStep > step.id
                              ? 'bg-primary-600'
                              : currentStep === step.id
                              ? 'bg-primary-300'
                              : 'bg-secondary-200'
                          }`}
                        />
                      )}
                      <button
                        onClick={() => currentStep > step.id && setCurrentStep(step.id)}
                        disabled={currentStep < step.id}
                        className={`group relative flex flex-col items-center ${
                          currentStep < step.id ? 'cursor-not-allowed' : 'cursor-pointer'
                        }`}
                      >
                        <span
                          className={`flex h-10 w-10 items-center justify-center rounded-full ${
                            currentStep > step.id
                              ? 'bg-primary-600 text-white'
                              : currentStep === step.id
                              ? 'border-2 border-primary-600 bg-white text-primary-600'
                              : 'border-2 border-secondary-300 bg-white text-secondary-500'
                          }`}
                        >
                          {stepValidation[step.id] ? (
                            <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                              <path
                                fillRule="evenodd"
                                d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                                clipRule="evenodd"
                              />
                            </svg>
                          ) : (
                            step.id
                          )}
                        </span>
                        <span className="mt-2 text-xs font-medium text-secondary-900">
                          {step.name}
                        </span>
                        <span className="text-xs text-secondary-500 hidden sm:block">
                          {step.description}
                        </span>
                      </button>
                    </li>
                  ))}
                </ol>
              </nav>
            </div>

            {/* Step Content */}
            <Card className="mb-6">
              <CardHeader>
                <CardTitle>
                  Step {currentStep}: {STEPS[currentStep - 1].name}
                </CardTitle>
              </CardHeader>
              <CardContent>{renderStep()}</CardContent>
            </Card>

            {/* Navigation Buttons */}
            <div className="flex items-center justify-between">
              <Button
                type="button"
                variant="outline"
                onClick={handlePrevious}
                disabled={currentStep === 1}
              >
                Previous
              </Button>

              <div className="flex items-center space-x-4">
                <Link href="/clients">
                  <Button variant="ghost">Cancel Intake</Button>
                </Link>
                
                {currentStep < STEPS.length ? (
                  <Button onClick={handleNext}>
                    Next Step
                  </Button>
                ) : (
                  <Button 
                    onClick={handleSubmit} 
                    disabled={creating}
                    className="bg-success-600 hover:bg-success-700"
                  >
                    {creating ? 'Completing Intake...' : 'Complete Intake'}
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}