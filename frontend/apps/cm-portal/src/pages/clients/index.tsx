import { useState } from 'react';
import Link from 'next/link';
import { ProtectedRoute } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Table, Button, Input, Badge, EmptyState } from '@haven/ui';
import { useClients, type Client } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

function ClientsContent() {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeOnly, setActiveOnly] = useState(true);
  
  const { clients, loading, error, refetch } = useClients({
    name: searchQuery || undefined,
    activeOnly,
  });

  const columns = [
    {
      key: 'name' as const,
      label: 'Name',
      render: (value: any, client: Client) => (
        <Link href={`/clients/${client.id}`} className="text-primary-600 hover:text-primary-700 font-medium">
          {client.name ? client.name.given.join(' ') + ' ' + client.name.family : 'Unknown'}
        </Link>
      ),
    },
    {
      key: 'gender' as const,
      label: 'Gender',
      render: (value: string) => (
        <span className="capitalize">{value?.toLowerCase()}</span>
      ),
    },
    {
      key: 'birthDate' as const,
      label: 'Date of Birth',
      render: (value: string) => (
        value ? new Date(value).toLocaleDateString() : 'N/A'
      ),
    },
    {
      key: 'telecoms' as const,
      label: 'Contact',
      render: (telecoms: any[]) => {
        const email = telecoms?.find(t => t.system === 'EMAIL');
        const phone = telecoms?.find(t => t.system === 'PHONE');
        
        return (
          <div className="text-sm">
            {email && <div>{email.value}</div>}
            {phone && <div>{phone.value}</div>}
          </div>
        );
      },
    },
    {
      key: 'status' as const,
      label: 'Status',
      render: (value: string) => (
        <Badge 
          variant={value === 'ACTIVE' ? 'success' : value === 'INACTIVE' ? 'secondary' : 'warning'}
        >
          {value}
        </Badge>
      ),
    },
    {
      key: 'createdAt' as const,
      label: 'Created',
      render: (value: string) => (
        <span className="text-sm text-secondary-600">
          {new Date(value).toLocaleDateString()}
        </span>
      ),
    },
    {
      key: 'actions' as const,
      label: '',
      width: '100px',
      render: (value: any, client: Client) => (
        <div className="flex space-x-2">
          <Link href={`/clients/${client.id}`}>
            <Button size="sm" variant="outline">View</Button>
          </Link>
          <Link href={`/clients/${client.id}/edit`}>
            <Button size="sm" variant="ghost">Edit</Button>
          </Link>
        </div>
      ),
    },
  ];

  const handleSearch = (query: string) => {
    setSearchQuery(query);
  };

  return (
    <div className="space-y-6">
      {/* Header Actions */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Clients</h1>
          <p className="text-secondary-600">Manage client profiles and information</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button onClick={() => refetch()} variant="outline">
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </Button>
          <Link href="/clients/new">
            <Button>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
              Add Client
            </Button>
          </Link>
        </div>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Search & Filter</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input
              label="Search by name"
              placeholder="Enter client name..."
              value={searchQuery}
              onChange={(e) => handleSearch(e.target.value)}
            />
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="activeOnly"
                checked={activeOnly}
                onChange={(e) => setActiveOnly(e.target.checked)}
                className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
              />
              <label htmlFor="activeOnly" className="text-sm font-medium text-secondary-700">
                Show active clients only
              </label>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Clients Table */}
      {error ? (
        <Card>
          <CardContent className="text-center py-12">
            <div className="text-error-600 mb-4">
              <svg className="w-12 h-12 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
              </svg>
              <p className="text-lg font-medium">Error loading clients</p>
              <p className="text-sm text-secondary-600">{error}</p>
            </div>
            <Button onClick={() => refetch()}>Try Again</Button>
          </CardContent>
        </Card>
      ) : clients && clients.length === 0 && !loading ? (
        <Card>
          <CardContent>
            <EmptyState
              icon={
                <svg className="mx-auto h-12 w-12 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z" />
                </svg>
              }
              title="No clients found"
              description={searchQuery ? `No clients match "${searchQuery}"` : "Get started by adding your first client"}
              action={{
                label: 'Add Client',
                onClick: () => window.location.href = '/clients/new'
              }}
            />
          </CardContent>
        </Card>
      ) : (
        <Table
          data={clients || []}
          columns={columns}
          loading={loading}
          emptyMessage="No clients found"
        />
      )}
    </div>
  );
}

export default function ClientsPage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Clients" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients' }
        ]}
      >
        <div className="p-6">
          <ClientsContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}