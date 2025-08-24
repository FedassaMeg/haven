import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button } from '@haven/ui';
import { useClient, type Client } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

interface InfoSectionProps {
  title: string;
  children: React.ReactNode;
}

const InfoSection: React.FC<InfoSectionProps> = ({ title, children }) => (
  <div className="space-y-3">
    <h3 className="text-lg font-medium text-secondary-900">{title}</h3>
    <div className="bg-secondary-50 rounded-lg p-4">
      {children}
    </div>
  </div>
);

interface InfoRowProps {
  label: string;
  value?: string | React.ReactNode;
  span?: boolean;
}

const InfoRow: React.FC<InfoRowProps> = ({ label, value, span = false }) => (
  <div className={`grid ${span ? 'grid-cols-1' : 'grid-cols-3'} gap-2`}>
    <dt className="text-sm font-medium text-secondary-500">{label}:</dt>
    <dd className={`text-sm text-secondary-900 ${span ? '' : 'col-span-2'}`}>
      {value || 'N/A'}
    </dd>
  </div>
);

function ClientDetailContent({ client }: { client: Client }) {
  const router = useRouter();
  
  const fullName = client.name 
    ? `${client.name.given?.join(' ') || ''} ${client.name.family || ''}`.trim()
    : 'Unknown';

  const primaryAddress = client.addresses?.find(addr => addr.use === 'HOME') || client.addresses?.[0];
  const primaryPhone = client.telecoms?.find(t => t.system === 'PHONE' && t.use === 'HOME') || 
                      client.telecoms?.find(t => t.system === 'PHONE');
  const primaryEmail = client.telecoms?.find(t => t.system === 'EMAIL');

  const formatAddress = (address: any) => {
    if (!address) return null;
    const parts = [
      address.line?.join(', '),
      address.city,
      address.state,
      address.postalCode
    ].filter(Boolean);
    return parts.join(', ');
  };

  const formatPeriod = (period: any) => {
    if (!period) return null;
    const start = period.start ? new Date(period.start).toLocaleDateString() : 'Unknown';
    const end = period.end ? new Date(period.end).toLocaleDateString() : 'Present';
    return `${start} - ${end}`;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center space-x-3">
            <h1 className="text-2xl font-bold text-secondary-900">{fullName}</h1>
            <Badge variant={client.status === 'ACTIVE' ? 'success' : 
                          client.status === 'INACTIVE' ? 'secondary' : 'warning'}>
              {client.status}
            </Badge>
          </div>
          <p className="text-secondary-600">Client ID: {client.id}</p>
          {client.createdAt && (
            <p className="text-sm text-secondary-500">
              Created: {new Date(client.createdAt).toLocaleDateString()}
            </p>
          )}
        </div>
        <div className="flex items-center space-x-3">
          <Link href={`/clients/${client.id}/edit`}>
            <Button>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              Edit Client
            </Button>
          </Link>
          <Link href="/cases/new" className="state={{ clientId: client.id }}">
            <Button variant="outline">
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              Open Case
            </Button>
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Information */}
        <div className="lg:col-span-2 space-y-6">
          {/* Demographics */}
          <Card>
            <CardHeader>
              <CardTitle>Demographics</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-3">
                  <InfoRow label="Full Name" value={fullName} />
                  <InfoRow label="Preferred Name" value={client.name?.text} />
                  <InfoRow label="Gender" value={client.gender?.toLowerCase().replace('_', ' ')} />
                  <InfoRow label="Date of Birth" 
                    value={client.birthDate ? new Date(client.birthDate).toLocaleDateString() : undefined} />
                </div>
                <div className="space-y-3">
                  <InfoRow label="Marital Status" value={client.maritalStatus?.text} />
                  <InfoRow label="Language" value={client.communication?.[0]?.language?.text} />
                  <InfoRow label="Preferred Contact" value={client.communication?.[0]?.preferred ? 'Phone' : undefined} />
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Contact Information */}
          <Card>
            <CardHeader>
              <CardTitle>Contact Information</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <InfoRow label="Primary Phone" value={primaryPhone?.value} />
                <InfoRow label="Email" value={primaryEmail?.value} />
                <InfoRow label="Primary Address" value={formatAddress(primaryAddress)} span />
                
                {/* All Addresses */}
                {client.addresses && client.addresses.length > 1 && (
                  <div className="mt-4 pt-4 border-t border-secondary-200">
                    <h4 className="text-sm font-medium text-secondary-700 mb-3">All Addresses</h4>
                    <div className="space-y-3">
                      {client.addresses.map((address, index) => (
                        <div key={index} className="bg-white p-3 rounded border">
                          <div className="flex items-center justify-between mb-2">
                            <Badge variant="outline">{address.use || 'Other'}</Badge>
                            {address.period && (
                              <span className="text-xs text-secondary-500">
                                {formatPeriod(address.period)}
                              </span>
                            )}
                          </div>
                          <p className="text-sm text-secondary-900">{formatAddress(address)}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* All Contact Points */}
                {client.telecoms && client.telecoms.length > 0 && (
                  <div className="mt-4 pt-4 border-t border-secondary-200">
                    <h4 className="text-sm font-medium text-secondary-700 mb-3">All Contact Methods</h4>
                    <div className="space-y-2">
                      {client.telecoms.map((telecom, index) => (
                        <div key={index} className="flex items-center justify-between py-2 px-3 bg-white rounded border">
                          <div className="flex items-center space-x-3">
                            <Badge variant="outline" size="sm">
                              {telecom.system}
                            </Badge>
                            <span className="text-sm text-secondary-900">{telecom.value}</span>
                          </div>
                          <div className="flex items-center space-x-2">
                            {telecom.use && (
                              <span className="text-xs text-secondary-500 capitalize">
                                {telecom.use.toLowerCase()}
                              </span>
                            )}
                            {telecom.period && (
                              <span className="text-xs text-secondary-500">
                                {formatPeriod(telecom.period)}
                              </span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Household Members */}
          {client.householdMembers && client.householdMembers.length > 0 && (
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle>Household Members</CardTitle>
                  <Button size="sm" variant="outline">
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    Add Member
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {client.householdMembers.map((member, index) => (
                    <div key={index} className="border border-secondary-200 rounded-lg p-4">
                      <div className="flex items-center justify-between mb-3">
                        <div>
                          <h4 className="font-medium text-secondary-900">
                            {member.name?.family ? 
                              `${member.name.given?.join(' ') || ''} ${member.name.family}`.trim() :
                              'Household Member'
                            }
                          </h4>
                          <p className="text-sm text-secondary-600">
                            {member.birthDate && `Born: ${new Date(member.birthDate).toLocaleDateString()}`}
                          </p>
                        </div>
                        <div className="flex items-center space-x-2">
                          <Badge variant="outline">
                            {member.relationship || 'Member'}
                          </Badge>
                          {member.gender && (
                            <Badge variant="ghost" size="sm">
                              {member.gender}
                            </Badge>
                          )}
                        </div>
                      </div>
                      {member.notes && (
                        <div className="mt-3 pt-3 border-t border-secondary-200">
                          <p className="text-sm text-secondary-700">{member.notes}</p>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Consent Management */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Consent & Privacy</CardTitle>
                <Button size="sm" variant="outline">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                  Manage Consent
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {/* Active Consents */}
                <div className="grid grid-cols-1 gap-3">
                  <div className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                      <div>
                        <p className="font-medium text-green-900">General Services</p>
                        <p className="text-sm text-green-700">Active until Dec 2024</p>
                      </div>
                    </div>
                    <Badge variant="success">Active</Badge>
                  </div>
                  
                  <div className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                      <div>
                        <p className="font-medium text-green-900">Housing Services</p>
                        <p className="text-sm text-green-700">Active until Mar 2025</p>
                      </div>
                    </div>
                    <Badge variant="success">Active</Badge>
                  </div>
                  
                  <div className="flex items-center justify-between p-3 bg-amber-50 border border-amber-200 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="w-3 h-3 bg-amber-500 rounded-full"></div>
                      <div>
                        <p className="font-medium text-amber-900">Legal Services</p>
                        <p className="text-sm text-amber-700">Expires in 30 days</p>
                      </div>
                    </div>
                    <Badge variant="warning">Expiring</Badge>
                  </div>
                </div>

                {/* Safety & Confidentiality Settings */}
                <div className="mt-6 pt-4 border-t border-secondary-200">
                  <h4 className="text-sm font-medium text-secondary-700 mb-3">Safety & Confidentiality</h4>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        <svg className="w-4 h-4 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2V7" />
                        </svg>
                        <span className="text-sm text-secondary-900">Safe at Home Participant</span>
                      </div>
                      <Badge variant="primary">Enrolled</Badge>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        <svg className="w-4 h-4 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        </svg>
                        <span className="text-sm text-secondary-900">Confidential Location</span>
                      </div>
                      <Badge variant="destructive">Restricted</Badge>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        <svg className="w-4 h-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.367 2.684 3 3 0 00-5.367-2.684z" />
                        </svg>
                        <span className="text-sm text-secondary-900">Data Sharing Restricted</span>
                      </div>
                      <Badge variant="secondary">Limited</Badge>
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Emergency Contact */}
          {client.contact && client.contact.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Emergency Contacts</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {client.contact.map((contact, index) => (
                    <div key={index} className="p-4 bg-secondary-50 rounded-lg">
                      <div className="flex items-center justify-between mb-3">
                        <h4 className="font-medium text-secondary-900">
                          {contact.name?.family ? 
                            `${contact.name.given?.join(' ') || ''} ${contact.name.family}`.trim() :
                            'Emergency Contact'
                          }
                        </h4>
                        <Badge variant="outline">
                          {contact.relationship?.[0]?.text || 'Contact'}
                        </Badge>
                      </div>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                        {contact.telecom?.map((tel, telIndex) => (
                          <div key={telIndex} className="flex items-center space-x-2">
                            <Badge variant="ghost" size="sm">{tel.system}</Badge>
                            <span className="text-sm">{tel.value}</span>
                          </div>
                        ))}
                      </div>
                      {contact.address && (
                        <div className="mt-3 pt-3 border-t border-secondary-200">
                          <p className="text-sm text-secondary-700">{formatAddress(contact.address)}</p>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Quick Stats */}
          <Card>
            <CardHeader>
              <CardTitle>Quick Stats</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-secondary-600">Active Cases</span>
                  <Badge variant="primary">3</Badge>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-secondary-600">Total Cases</span>
                  <span className="text-sm font-medium">7</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-secondary-600">Last Contact</span>
                  <span className="text-sm font-medium">2 days ago</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-secondary-600">Case Manager</span>
                  <span className="text-sm font-medium">Sarah Johnson</span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Recent Activity */}
          <Card>
            <CardHeader>
              <CardTitle>Recent Activity</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 bg-primary-500 rounded-full mt-2"></div>
                  <div className="flex-1">
                    <p className="text-sm text-secondary-900">Case updated</p>
                    <p className="text-xs text-secondary-500">2 hours ago</p>
                  </div>
                </div>
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 bg-success-500 rounded-full mt-2"></div>
                  <div className="flex-1">
                    <p className="text-sm text-secondary-900">Address verified</p>
                    <p className="text-xs text-secondary-500">1 day ago</p>
                  </div>
                </div>
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 bg-warning-500 rounded-full mt-2"></div>
                  <div className="flex-1">
                    <p className="text-sm text-secondary-900">Document requested</p>
                    <p className="text-xs text-secondary-500">3 days ago</p>
                  </div>
                </div>
              </div>
              <div className="mt-4 pt-4 border-t border-secondary-200">
                <Link href={`/clients/${client.id}/activity`} className="text-sm text-primary-600 hover:text-primary-700">
                  View all activity
                </Link>
              </div>
            </CardContent>
          </Card>

          {/* Actions */}
          <Card>
            <CardHeader>
              <CardTitle>Actions</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <Link href={`/cases?clientId=${client.id}`}>
                  <Button variant="outline" className="w-full justify-start">
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    View Cases
                  </Button>
                </Link>
                <Link href={`/services?clientId=${client.id}`}>
                  <Button variant="outline" className="w-full justify-start">
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                    View Services
                  </Button>
                </Link>
                <Link href={`/services/new?clientId=${client.id}`}>
                  <Button variant="outline" className="w-full justify-start">
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    Create Service
                  </Button>
                </Link>
                <Button variant="outline" className="w-full justify-start">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  Generate Report
                </Button>
                <Button variant="outline" className="w-full justify-start">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2h3a1 1 0 110 2h-1v12a2 2 0 01-2 2H7a2 2 0 01-2-2V6H4a1 1 0 110-2h3z" />
                  </svg>
                  Archive Client
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
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
        <p className="text-secondary-600 mb-6">The client you're looking for doesn't exist or has been removed.</p>
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

export default function ClientDetailPage() {
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
        title={clientName}
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients', href: '/clients' },
          { label: clientName }
        ]}
      >
        <div className="p-6">
          <ClientDetailContent client={client} />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}