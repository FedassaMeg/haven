import type { AppProps } from 'next/app';
import { AuthProvider } from '@haven/auth';
import { apiClient } from '@haven/api-client';
import '@haven/ui/src/styles/globals.css';

function MyApp({ Component, pageProps }: AppProps) {
  const keycloakConfig = {
    url: process.env.NEXT_PUBLIC_KEYCLOAK_URL || 'http://localhost:8081',
    realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM || 'haven',
    clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || 'haven-frontend',
  };

  const handleAuthSuccess = (user: any) => {
    console.log('User authenticated:', user);
  };

  const handleAuthError = (error: any) => {
    console.error('Authentication error:', error);
  };

  return (
    <AuthProvider
      config={keycloakConfig}
      onAuthSuccess={handleAuthSuccess}
      onAuthError={handleAuthError}
      initOptions={{
        onLoad: 'login-required',
        checkLoginIframe: false,
        enableLogging: process.env.NODE_ENV === 'development',
        pkceMethod: 'S256',
        redirectUri: process.env.NEXT_PUBLIC_APP_URL || 'http://localhost:3000/',
      }}
    >
      <Component {...pageProps} />
    </AuthProvider>
  );
}

export default MyApp;