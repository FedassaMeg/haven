import { useEffect } from 'react';
import { useRouter } from 'next/router';
import { ProtectedRoute } from '@haven/auth';
import AppLayout from '../../components/AppLayout';


export default function CreateClientPage() {
  const router = useRouter();
  
  useEffect(() => {
    // Redirect to the new intake workflow
    router.replace('/intake');
  }, [router]);
  
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Redirecting..."
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients', href: '/clients' },
          { label: 'New Client' }
        ]}
      >
        <div className="p-6">
          <div className="max-w-4xl mx-auto">
            <div className="text-center py-12">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
              <p className="mt-4 text-secondary-600">Redirecting to intake workflow...</p>
            </div>
          </div>
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}