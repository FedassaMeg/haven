import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Input, Select } from '@haven/ui';
import { useClient, useUpdateClient, type Client } from '@haven/api-client';
import AppLayout from '../../../components/AppLayout';

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

function ClientEditForm({ client }: { client: Client }) {
  const router = useRouter();
  const { updateClient, loading: updating } = useUpdateClient();
  const [formData, setFormData] = useState<FormData>(initialFormData);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (client) {
      // Pre-populate form with client data
      const primaryAddress = client.addresses?.find(addr => addr.use === 'HOME') || client.addresses?.[0];
      const primaryPhone = client.telecoms?.find(t => t.system === 'PHONE' && t.use === 'HOME') || 
                          client.telecoms?.find(t => t.system === 'PHONE');
      const secondaryPhone = client.telecoms?.find(t => t.system === 'PHONE' && t.use !== primaryPhone?.use);
      const primaryEmail = client.telecoms?.find(t => t.system === 'EMAIL');
      const emergencyContact = client.contact?.[0];

      setFormData({
        firstName: client.name?.given?.[0] || '',
        lastName: client.name?.family || '',
        preferredName: client.name?.text || '',
        gender: client.gender || '',
        birthDate: client.birthDate ? client.birthDate.split('T')[0] : '',
        maritalStatus: client.maritalStatus?.text || '',
        primaryPhone: primaryPhone?.value || '',
        secondaryPhone: secondaryPhone?.value || '',
        email: primaryEmail?.value || '',
        addressLine1: primaryAddress?.line?.[0] || '',
        addressLine2: primaryAddress?.line?.[1] || '',
        city: primaryAddress?.city || '',
        state: primaryAddress?.state || '',
        postalCode: primaryAddress?.postalCode || '',
        country: primaryAddress?.country || 'US',
        emergencyContactName: emergencyContact?.name?.family ? 
          `${emergencyContact.name.given?.join(' ') || ''} ${emergencyContact.name.family}`.trim() : '',
        emergencyContactRelationship: emergencyContact?.relationship?.[0]?.text || '',
        emergencyContactPhone: emergencyContact?.telecom?.find(t => t.system === 'PHONE')?.value || '',
        emergencyContactEmail: emergencyContact?.telecom?.find(t => t.system === 'EMAIL')?.value || '',
        preferredLanguage: client.communication?.[0]?.language?.text || 'en',
        communication: client.communication?.[0]?.preferred ? 'PHONE' : 'EMAIL',
        status: client.status || 'ACTIVE'
      });
    }
  }, [client]);

  const handleInputChange = (field: keyof FormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

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
    }
    if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }
    if (formData.primaryPhone && !/^\+?[\d\s\-\(\)]+$/.test(formData.primaryPhone)) {
      newErrors.primaryPhone = 'Please enter a valid phone number';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    try {
      // Transform form data to client format
      const updatedClient = {
        ...client,
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

      await updateClient(client.id, updatedClient);
      router.push(`/clients/${client.id}`);
    } catch (error) {
      console.error('Failed to update client:', error);
    }
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
              label="First Name"
              value={formData.firstName}
              onChange={(e) => handleInputChange('firstName', e.target.value)}
              error={errors.firstName}
              required
            />
            <Input
              label="Last Name"
              value={formData.lastName}
              onChange={(e) => handleInputChange('lastName', e.target.value)}
              error={errors.lastName}
              required
            />
            <Input
              label="Preferred Name"
              value={formData.preferredName}
              onChange={(e) => handleInputChange('preferredName', e.target.value)}
              placeholder="Optional"
            />
            <Select
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
              type="date"
              label="Date of Birth"
              value={formData.birthDate}
              onChange={(e) => handleInputChange('birthDate', e.target.value)}
              error={errors.birthDate}
              required
            />
            <Input
              label="Marital Status"
              value={formData.maritalStatus}
              onChange={(e) => handleInputChange('maritalStatus', e.target.value)}
              placeholder="Single, Married, Divorced, etc."
            />
          </div>
        </CardContent>
      </Card>

      {/* Contact Information */}
      <Card>
        <CardHeader>
          <CardTitle>Contact Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Primary Phone"
              value={formData.primaryPhone}
              onChange={(e) => handleInputChange('primaryPhone', e.target.value)}
              error={errors.primaryPhone}
              placeholder="(555) 123-4567"
            />
            <Input
              label="Secondary Phone"
              value={formData.secondaryPhone}
              onChange={(e) => handleInputChange('secondaryPhone', e.target.value)}
              placeholder="(555) 123-4567"
            />
            <Input
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
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-4">
            <Input
              label="Address Line 1"
              value={formData.addressLine1}
              onChange={(e) => handleInputChange('addressLine1', e.target.value)}
              placeholder="Street address"
            />
            <Input
              label="Address Line 2"
              value={formData.addressLine2}
              onChange={(e) => handleInputChange('addressLine2', e.target.value)}
              placeholder="Apartment, suite, unit, etc."
            />
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Input
                label="City"
                value={formData.city}
                onChange={(e) => handleInputChange('city', e.target.value)}
              />
              <Input
                label="State/Province"
                value={formData.state}
                onChange={(e) => handleInputChange('state', e.target.value)}
              />
              <Input
                label="Postal Code"
                value={formData.postalCode}
                onChange={(e) => handleInputChange('postalCode', e.target.value)}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Emergency Contact */}
      <Card>
        <CardHeader>
          <CardTitle>Emergency Contact</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Contact Name"
              value={formData.emergencyContactName}
              onChange={(e) => handleInputChange('emergencyContactName', e.target.value)}
              placeholder="Full name"
            />
            <Input
              label="Relationship"
              value={formData.emergencyContactRelationship}
              onChange={(e) => handleInputChange('emergencyContactRelationship', e.target.value)}
              placeholder="Spouse, Parent, Friend, etc."
            />
            <Input
              label="Phone Number"
              value={formData.emergencyContactPhone}
              onChange={(e) => handleInputChange('emergencyContactPhone', e.target.value)}
              placeholder="(555) 123-4567"
            />
            <Input
              type="email"
              label="Email Address"
              value={formData.emergencyContactEmail}
              onChange={(e) => handleInputChange('emergencyContactEmail', e.target.value)}
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
              label="Preferred Language"
              value={formData.preferredLanguage}
              onChange={(value) => handleInputChange('preferredLanguage', value)}
              options={[
                { value: 'en', label: 'English' },
                { value: 'es', label: 'Spanish' },
                { value: 'fr', label: 'French' },
                { value: 'de', label: 'German' },
                { value: 'other', label: 'Other' }
              ]}
            />
            <Select
              label="Preferred Contact Method"
              value={formData.communication}
              onChange={(value) => handleInputChange('communication', value)}
              options={[
                { value: 'PHONE', label: 'Phone' },
                { value: 'EMAIL', label: 'Email' },
                { value: 'MAIL', label: 'Mail' }
              ]}
            />
            <Select
              label="Status"
              value={formData.status}
              onChange={(value) => handleInputChange('status', value)}
              options={[
                { value: 'ACTIVE', label: 'Active' },
                { value: 'INACTIVE', label: 'Inactive' },
                { value: 'SUSPENDED', label: 'Suspended' }
              ]}
            />
          </div>
        </CardContent>
      </Card>

      {/* Form Actions */}
      <div className="flex items-center justify-end space-x-4 pt-6 border-t border-secondary-200">
        <Link href={`/clients/${client.id}`}>
          <Button variant="outline" disabled={updating}>
            Cancel
          </Button>
        </Link>
        <Button type="submit" disabled={updating}>
          {updating ? (
            <>
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
              Saving...
            </>
          ) : (
            'Save Changes'
          )}
        </Button>
      </div>
    </form>
  );
}

function ClientNotFound() {
  return (
    <div className="text-center py-12">
      <div className="max-w-md mx-auto">
        <svg className="w-24 h-24 mx-auto mb-6 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
        </svg>
        <h2 className="text-2xl font-semibold text-secondary-900 mb-2">Client Not Found</h2>
        <p className="text-secondary-600 mb-6">The client you're trying to edit doesn't exist or has been removed.</p>
        <div className="space-x-3">
          <Link href="/clients">
            <Button>Back to Clients</Button>
          </Link>
          <Link href="/dashboard">
            <Button variant="outline">Dashboard</Button>
          </Link>
        </div>
      </div>
    </div>
  );
}

export default function ClientEditPage() {
  const router = useRouter();
  const { id } = router.query;
  const { client, loading, error } = useClient(id as string);

  if (loading) {
    return (
      <ProtectedRoute>
        <AppLayout title="Loading...">
          <div className="flex items-center justify-center min-h-96">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
              <p className="text-secondary-600">Loading client details...</p>
            </div>
          </div>
        </AppLayout>
      </ProtectedRoute>
    );
  }

  if (error || !client) {
    return (
      <ProtectedRoute>
        <AppLayout 
          title="Client Not Found"
          breadcrumbs={[
            { label: 'Dashboard', href: '/dashboard' },
            { label: 'Clients', href: '/clients' },
            { label: 'Not Found' }
          ]}
        >
          <div className="p-6">
            <ClientNotFound />
          </div>
        </AppLayout>
      </ProtectedRoute>
    );
  }

  const clientName = client.name 
    ? `${client.name.given?.join(' ') || ''} ${client.name.family || ''}`.trim()
    : 'Unknown Client';

  return (
    <ProtectedRoute>
      <AppLayout 
        title={`Edit ${clientName}`}
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients', href: '/clients' },
          { label: clientName, href: `/clients/${client.id}` },
          { label: 'Edit' }
        ]}
      >
        <div className="p-6">
          <div className="max-w-4xl mx-auto">
            <div className="mb-6">
              <h1 className="text-2xl font-bold text-secondary-900">Edit Client</h1>
              <p className="text-secondary-600">Update client information and preferences</p>
            </div>
            <ClientEditForm client={client} />
          </div>
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}