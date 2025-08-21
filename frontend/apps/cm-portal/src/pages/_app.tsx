import type { AppProps } from 'next/app';
import { useCallback, useMemo, useEffect } from 'react';
import { AuthProvider, useAuth } from '@haven/auth';
import { apiClient } from '@haven/api-client';
import '../styles/globals.css';

// Component to sync auth token with API client
function AuthSync({ children }: { children: React.ReactNode }) {
  const { token } = useAuth();
  
  useEffect(() => {
    if (token) {
      apiClient.setAuthToken(token);
    } else {
      apiClient.setAuthToken(null);
    }
  }, [token]);
  
  return <>{children}</>;
}

function MyApp({ Component, pageProps }: AppProps) {
  const keycloakConfig = useMemo(() => ({
    url: process.env.NEXT_PUBLIC_KEYCLOAK_URL || 'http://localhost:8081',
    realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM || 'haven',
    clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || 'haven-frontend',
  }), []);

  const handleAuthSuccess = useCallback((user: any) => {
    console.log('User authenticated:', user);
  }, []);

  const handleAuthError = useCallback((error: any) => {
    console.error('Authentication error:', error);
  }, []);

  const initOptions = useMemo(() => ({
    onLoad: 'login-required' as const,
    checkLoginIframe: false,
    enableLogging: process.env.NODE_ENV === 'development',
  }), []);

  return (
    <AuthProvider
      config={keycloakConfig}
      onAuthSuccess={handleAuthSuccess}
      onAuthError={handleAuthError}
      initOptions={initOptions}
    >
      <AuthSync>
        <Component {...pageProps} />
      </AuthSync>
    </AuthProvider>
  );
}

export default MyApp;