import { useState } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Input, Select } from '@haven/ui';
import { useCreateClient } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

interface FormData {
  // Basic Info
  firstName: string;
  lastName: string;
  preferredName: string;
  gender: string;
  birthDate: string;
  maritalStatus: string;
  
  // Contact Info
  primaryPhone: string;
  secondaryPhone: string;
  email: string;
  
  // Address
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  
  // Emergency Contact
  emergencyContactName: string;
  emergencyContactRelationship: string;
  emergencyContactPhone: string;
  emergencyContactEmail: string;
  
  // Other
  preferredLanguage: string;
  communication: string;
  status: string;
}

const initialFormData: FormData = {
  firstName: '',
  lastName: '',
  preferredName: '',
  gender: '',
  birthDate: '',
  maritalStatus: '',
  primaryPhone: '',
  secondaryPhone: '',
  email: '',
  addressLine1: '',
  addressLine2: '',
  city: '',
  state: '',
  postalCode: '',
  country: 'US',
  emergencyContactName: '',
  emergencyContactRelationship: '',
  emergencyContactPhone: '',
  emergencyContactEmail: '',
  preferredLanguage: 'en',
  communication: 'PHONE',
  status: 'ACTIVE'
};

function CreateClientForm() {
  const router = useRouter();
  const { createClient, loading: creating } = useCreateClient();
  const [formData, setFormData] = useState<FormData>(initialFormData);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleInputChange = (field: keyof FormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Required fields
    if (!formData.firstName.trim()) {
      newErrors.firstName = 'First name is required';
    }
    if (!formData.lastName.trim()) {
      newErrors.lastName = 'Last name is required';
    }
    if (!formData.gender) {
      newErrors.gender = 'Gender is required';
    }
    if (!formData.birthDate) {
      newErrors.birthDate = 'Date of birth is required';
    } else {
      // Check if birth date is not in the future
      const birthDate = new Date(formData.birthDate);
      const today = new Date();
      if (birthDate > today) {
        newErrors.birthDate = 'Birth date cannot be in the future';
      }
      // Check if birth date is reasonable (not too old)
      const maxAge = 150;
      const minBirthDate = new Date();
      minBirthDate.setFullYear(today.getFullYear() - maxAge);
      if (birthDate < minBirthDate) {
        newErrors.birthDate = 'Please enter a valid birth date';
      }
    }

    // Validation for optional fields
    if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }
    if (formData.primaryPhone && !/^\+?[\d\s\-\(\)]+$/.test(formData.primaryPhone)) {
      newErrors.primaryPhone = 'Please enter a valid phone number';
    }
    if (formData.secondaryPhone && !/^\+?[\d\s\-\(\)]+$/.test(formData.secondaryPhone)) {
      newErrors.secondaryPhone = 'Please enter a valid phone number';
    }
    if (formData.emergencyContactPhone && !/^\+?[\d\s\-\(\)]+$/.test(formData.emergencyContactPhone)) {
      newErrors.emergencyContactPhone = 'Please enter a valid phone number';
    }
    if (formData.emergencyContactEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.emergencyContactEmail)) {
      newErrors.emergencyContactEmail = 'Please enter a valid email address';
    }

    // Ensure at least one contact method
    if (!formData.primaryPhone && !formData.email) {
      newErrors.primaryPhone = 'Please provide at least a phone number or email address';
      newErrors.email = 'Please provide at least a phone number or email address';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      // Scroll to first error
      const firstErrorField = Object.keys(errors)[0];
      const element = document.getElementById(firstErrorField);
      element?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }

    try {
      // Transform form data to client format
      const newClient = {
        name: {
          use: 'OFFICIAL',
          family: formData.lastName,
          given: [formData.firstName],
          text: formData.preferredName || `${formData.firstName} ${formData.lastName}`
        },
        gender: formData.gender as any,
        birthDate: formData.birthDate,
        maritalStatus: formData.maritalStatus ? {
          text: formData.maritalStatus
        } : undefined,
        addresses: [{
          use: 'HOME',
          line: [formData.addressLine1, formData.addressLine2].filter(Boolean),
          city: formData.city,
          state: formData.state,
          postalCode: formData.postalCode,
          country: formData.country
        }].filter(addr => addr.line.length > 0 || addr.city),
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
        communication: [{
          language: {
            text: formData.preferredLanguage
          },
          preferred: formData.communication === 'PHONE'
        }],
        status: formData.status as any
      };

      const createdClient = await createClient(newClient);
      router.push(`/clients/${createdClient.id}`);
    } catch (error: any) {
      console.error('Failed to create client:', error);
      
      // Handle validation errors from API
      if (error.response?.data?.errors) {
        const apiErrors = error.response.data.errors;
        const newErrors: Record<string, string> = {};
        
        apiErrors.forEach((err: any) => {
          if (err.field && err.message) {
            newErrors[err.field] = err.message;
          }
        });
        
        if (Object.keys(newErrors).length > 0) {
          setErrors(newErrors);
        }
      }
    }
  };

  const handleReset = () => {
    setFormData(initialFormData);
    setErrors({});
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Basic Information */}
      <Card>
        <CardHeader>
          <CardTitle>Basic Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              id="firstName"
              label="First Name"
              value={formData.firstName}
              onChange={(e) => handleInputChange('firstName', e.target.value)}
              error={errors.firstName}
              required
              placeholder="Enter first name"
            />
            <Input
              id="lastName"
              label="Last Name"
              value={formData.lastName}
              onChange={(e) => handleInputChange('lastName', e.target.value)}
              error={errors.lastName}
              required
              placeholder="Enter last name"
            />
            <Input
              id="preferredName"
              label="Preferred Name"
              value={formData.preferredName}
              onChange={(e) => handleInputChange('preferredName', e.target.value)}
              placeholder="How they prefer to be called (optional)"
              helperText="Leave blank to use first name"
            />
            <Select
              id="gender"
              label="Gender"
              value={formData.gender}
              onChange={(value) => handleInputChange('gender', value)}
              error={errors.gender}
              required
              options={[
                { value: '', label: 'Select Gender' },
                { value: 'MALE', label: 'Male' },
                { value: 'FEMALE', label: 'Female' },
                { value: 'OTHER', label: 'Other' },
                { value: 'UNKNOWN', label: 'Prefer not to say' }
              ]}
            />
            <Input
              id="birthDate"
              type="date"
              label="Date of Birth"
              value={formData.birthDate}
              onChange={(e) => handleInputChange('birthDate', e.target.value)}
              error={errors.birthDate}
              required
              max={new Date().toISOString().split('T')[0]} // Prevent future dates
            />
            <Input
              id="maritalStatus"
              label="Marital Status"
              value={formData.maritalStatus}
              onChange={(e) => handleInputChange('maritalStatus', e.target.value)}
              placeholder="Single, Married, Divorced, etc. (optional)"
            />
          </div>
        </CardContent>
      </Card>

      {/* Contact Information */}
      <Card>
        <CardHeader>
          <CardTitle>Contact Information</CardTitle>
          <p className="text-sm text-secondary-600">Please provide at least one contact method</p>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              id="primaryPhone"
              label="Primary Phone"
              value={formData.primaryPhone}
              onChange={(e) => handleInputChange('primaryPhone', e.target.value)}
              error={errors.primaryPhone}
              placeholder="(555) 123-4567"
              helperText="Main contact number"
            />
            <Input
              id="secondaryPhone"
              label="Secondary Phone"
              value={formData.secondaryPhone}
              onChange={(e) => handleInputChange('secondaryPhone', e.target.value)}
              error={errors.secondaryPhone}
              placeholder="(555) 123-4567 (optional)"
              helperText="Work or alternative number"
            />
            <Input
              id="email"
              type="email"
              label="Email Address"
              value={formData.email}
              onChange={(e) => handleInputChange('email', e.target.value)}
              error={errors.email}
              placeholder="client@example.com"
              className="md:col-span-2"
            />
          </div>
        </CardContent>
      </Card>

      {/* Address Information */}
      <Card>
        <CardHeader>
          <CardTitle>Primary Address</CardTitle>
          <p className="text-sm text-secondary-600">Current residential address (optional)</p>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-4">
            <Input
              id="addressLine1"
              label="Address Line 1"
              value={formData.addressLine1}
              onChange={(e) => handleInputChange('addressLine1', e.target.value)}
              placeholder="Street address, P.O. box, company name"
            />
            <Input
              id="addressLine2"
              label="Address Line 2"
              value={formData.addressLine2}
              onChange={(e) => handleInputChange('addressLine2', e.target.value)}
              placeholder="Apartment, suite, unit, building, floor, etc."
            />
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Input
                id="city"
                label="City"
                value={formData.city}
                onChange={(e) => handleInputChange('city', e.target.value)}
                placeholder="City"
              />
              <Input
                id="state"
                label="State/Province"
                value={formData.state}
                onChange={(e) => handleInputChange('state', e.target.value)}
                placeholder="State or Province"
              />
              <Input
                id="postalCode"
                label="Postal Code"
                value={formData.postalCode}
                onChange={(e) => handleInputChange('postalCode', e.target.value)}
                placeholder="ZIP or Postal Code"
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Emergency Contact */}
      <Card>
        <CardHeader>
          <CardTitle>Emergency Contact</CardTitle>
          <p className="text-sm text-secondary-600">Person to contact in case of emergency (optional)</p>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              id="emergencyContactName"
              label="Contact Name"
              value={formData.emergencyContactName}
              onChange={(e) => handleInputChange('emergencyContactName', e.target.value)}
              placeholder="Full name"
            />
            <Input
              id="emergencyContactRelationship"
              label="Relationship"
              value={formData.emergencyContactRelationship}
              onChange={(e) => handleInputChange('emergencyContactRelationship', e.target.value)}
              placeholder="Spouse, Parent, Friend, etc."
            />
            <Input
              id="emergencyContactPhone"
              label="Phone Number"
              value={formData.emergencyContactPhone}
              onChange={(e) => handleInputChange('emergencyContactPhone', e.target.value)}
              error={errors.emergencyContactPhone}
              placeholder="(555) 123-4567"
            />
            <Input
              id="emergencyContactEmail"
              type="email"
              label="Email Address"
              value={formData.emergencyContactEmail}
              onChange={(e) => handleInputChange('emergencyContactEmail', e.target.value)}
              error={errors.emergencyContactEmail}
              placeholder="contact@example.com"
            />
          </div>
        </CardContent>
      </Card>

      {/* Preferences & Status */}
      <Card>
        <CardHeader>
          <CardTitle>Preferences & Status</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Select
              id="preferredLanguage"
              label="Preferred Language"
              value={formData.preferredLanguage}
              onChange={(value) => handleInputChange('preferredLanguage', value)}
              options={[
                { value: 'en', label: 'English' },
                { value: 'es', label: 'Spanish' },
                { value: 'fr', label: 'French' },
                { value: 'de', label: 'German' },
                { value: 'zh', label: 'Chinese' },
                { value: 'ar', label: 'Arabic' },
                { value: 'other', label: 'Other' }
              ]}
            />
            <Select
              id="communication"
              label="Preferred Contact Method"
              value={formData.communication}
              onChange={(value) => handleInputChange('communication', value)}
              options={[
                { value: 'PHONE', label: 'Phone' },
                { value: 'EMAIL', label: 'Email' },
                { value: 'MAIL', label: 'Mail' },
                { value: 'TEXT', label: 'Text Message' }
              ]}
            />
            <Select
              id="status"
              label="Initial Status"
              value={formData.status}
              onChange={(value) => handleInputChange('status', value)}
              options={[
                { value: 'ACTIVE', label: 'Active' },
                { value: 'INACTIVE', label: 'Inactive' }
              ]}
              helperText="New clients are usually set to Active"
            />
          </div>
        </CardContent>
      </Card>

      {/* Form Actions */}
      <div className="flex items-center justify-between pt-6 border-t border-secondary-200">
        <Button 
          type="button" 
          variant="ghost" 
          onClick={handleReset}
          disabled={creating}
        >
          Reset Form
        </Button>
        
        <div className="flex items-center space-x-4">
          <Link href="/clients">
            <Button variant="outline" disabled={creating}>
              Cancel
            </Button>
          </Link>
          <Button type="submit" disabled={creating}>
            {creating ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                Creating Client...
              </>
            ) : (
              'Create Client'
            )}
          </Button>
        </div>
      </div>
    </form>
  );
}

export default function CreateClientPage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="New Client"
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients', href: '/clients' },
          { label: 'New Client' }
        ]}
      >
        <div className="p-6">
          <div className="max-w-4xl mx-auto">
            <div className="mb-6">
              <h1 className="text-2xl font-bold text-secondary-900">Create New Client</h1>
              <p className="text-secondary-600">Enter client information to create a new profile</p>
            </div>
            
            {/* Progress indicator */}
            <div className="mb-8">
              <div className="flex items-center space-x-4 text-sm">
                <div className="flex items-center">
                  <div className="w-8 h-8 bg-primary-600 text-white rounded-full flex items-center justify-center text-xs font-medium">
                    1
                  </div>
                  <span className="ml-2 text-primary-600 font-medium">Client Information</span>
                </div>
                <div className="flex-1 h-0.5 bg-secondary-200"></div>
                <div className="flex items-center text-secondary-400">
                  <div className="w-8 h-8 bg-secondary-200 rounded-full flex items-center justify-center text-xs">
                    2
                  </div>
                  <span className="ml-2">Review & Create</span>
                </div>
              </div>
            </div>

            <CreateClientForm />
          </div>
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}