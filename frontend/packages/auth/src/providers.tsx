import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import Keycloak, { KeycloakInstance, KeycloakProfile } from 'keycloak-js';
import Cookies from 'js-cookie';
import type {
  AuthContextType,
  AuthProviderProps,
  AuthState,
  User,
  KeycloakConfig,
  UserRole,
  ResourceType,
  ActionType,
} from './types';

const AuthContext = createContext<AuthContextType | null>(null);

const TOKEN_COOKIE = 'haven-auth-token';
const REFRESH_TOKEN_COOKIE = 'haven-refresh-token';

export const AuthProvider: React.FC<AuthProviderProps> = ({
  children,
  config,
  initOptions = {
    onLoad: 'check-sso',
    silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
    checkLoginIframe: false,
    enableLogging: process.env.NODE_ENV === 'development',
  },
  onAuthSuccess,
  onAuthError,
  loadingComponent: LoadingComponent,
}) => {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
    token: null,
    refreshToken: null,
  });
  const [keycloak, setKeycloak] = useState<KeycloakInstance | null>(null);

  // Initialize Keycloak
  useEffect(() => {
    const initKeycloak = async () => {
      try {
        const kc = new Keycloak({
          url: config.url,
          realm: config.realm,
          clientId: config.clientId,
        });

        const authenticated = await kc.init({
          ...initOptions,
          token: Cookies.get(TOKEN_COOKIE),
          refreshToken: Cookies.get(REFRESH_TOKEN_COOKIE),
        });

        if (authenticated) {
          const profile = await kc.loadUserProfile();
          const user = transformKeycloakUser(kc, profile);
          
          setAuthState({
            user,
            isAuthenticated: true,
            isLoading: false,
            token: kc.token || null,
            refreshToken: kc.refreshToken || null,
          });

          // Store tokens in cookies
          if (kc.token) {
            Cookies.set(TOKEN_COOKIE, kc.token, { secure: true, sameSite: 'strict' });
          }
          if (kc.refreshToken) {
            Cookies.set(REFRESH_TOKEN_COOKIE, kc.refreshToken, { secure: true, sameSite: 'strict' });
          }

          onAuthSuccess?.(user);
        } else {
          setAuthState({
            user: null,
            isAuthenticated: false,
            isLoading: false,
            token: null,
            refreshToken: null,
          });
        }

        setKeycloak(kc);

        // Set up token refresh
        kc.onTokenExpired = () => {
          kc.updateToken(30).then((refreshed) => {
            if (refreshed) {
              if (kc.token) {
                Cookies.set(TOKEN_COOKIE, kc.token, { secure: true, sameSite: 'strict' });
              }
              setAuthState(prev => ({ ...prev, token: kc.token || null }));
            }
          }).catch(() => {
            console.error('Failed to refresh token');
            logout();
          });
        };

      } catch (error) {
        console.error('Failed to initialize Keycloak:', error);
        onAuthError?.(error);
        setAuthState({
          user: null,
          isAuthenticated: false,
          isLoading: false,
          token: null,
          refreshToken: null,
        });
      }
    };

    initKeycloak();
  }, [config, initOptions, onAuthSuccess, onAuthError]);

  const transformKeycloakUser = (kc: KeycloakInstance, profile: KeycloakProfile): User => {
    const realmRoles = kc.realmAccess?.roles || [];
    const clientRoles = kc.resourceAccess?.[config.clientId]?.roles || [];
    const groups = (kc.tokenParsed as any)?.groups || [];

    return {
      id: profile.id || kc.subject || '',
      username: profile.username || '',
      email: profile.email || '',
      firstName: profile.firstName || '',
      lastName: profile.lastName || '',
      roles: [...realmRoles, ...clientRoles],
      groups,
      attributes: profile.attributes,
    };
  };

  const login = useCallback(async (redirectUri?: string) => {
    if (keycloak) {
      await keycloak.login({ redirectUri });
    }
  }, [keycloak]);

  const logout = useCallback(async (redirectUri?: string) => {
    if (keycloak) {
      // Clear cookies
      Cookies.remove(TOKEN_COOKIE);
      Cookies.remove(REFRESH_TOKEN_COOKIE);
      
      setAuthState({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        token: null,
        refreshToken: null,
      });
      
      await keycloak.logout({ redirectUri });
    }
  }, [keycloak]);

  const register = useCallback(async (redirectUri?: string) => {
    if (keycloak) {
      await keycloak.register({ redirectUri });
    }
  }, [keycloak]);

  const updateToken = useCallback(async (minValidity = 30): Promise<boolean> => {
    if (keycloak) {
      try {
        const refreshed = await keycloak.updateToken(minValidity);
        if (refreshed && keycloak.token) {
          Cookies.set(TOKEN_COOKIE, keycloak.token, { secure: true, sameSite: 'strict' });
          setAuthState(prev => ({ ...prev, token: keycloak.token || null }));
        }
        return refreshed;
      } catch (error) {
        console.error('Failed to update token:', error);
        return false;
      }
    }
    return false;
  }, [keycloak]);

  const hasRole = useCallback((role: string): boolean => {
    return authState.user?.roles.includes(role) || false;
  }, [authState.user]);

  const hasGroup = useCallback((group: string): boolean => {
    return authState.user?.groups.includes(group) || false;
  }, [authState.user]);

  const hasPermission = useCallback((resource: string, action: string): boolean => {
    if (!authState.user) return false;

    // Admin has all permissions
    if (hasRole(UserRole.ADMIN)) return true;

    // Define permission matrix
    const permissionMatrix: Record<string, Record<string, string[]>> = {
      [ResourceType.CLIENT]: {
        [ActionType.CREATE]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER],
        [ActionType.READ]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER, UserRole.SUPERVISOR, UserRole.VIEWER],
        [ActionType.UPDATE]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER],
        [ActionType.DELETE]: [UserRole.CASE_MANAGER, UserRole.SUPERVISOR],
      },
      [ResourceType.CASE]: {
        [ActionType.CREATE]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER],
        [ActionType.READ]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER, UserRole.SUPERVISOR, UserRole.VIEWER],
        [ActionType.UPDATE]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER],
        [ActionType.ASSIGN]: [UserRole.CASE_MANAGER, UserRole.SUPERVISOR],
        [ActionType.CLOSE]: [UserRole.CASE_MANAGER, UserRole.SUPERVISOR],
      },
      [ResourceType.REPORT]: {
        [ActionType.read]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER, UserRole.SUPERVISOR, UserRole.VIEWER],
        [ActionType.CREATE]: [UserRole.SUPERVISOR],
      },
    };

    const allowedRoles = permissionMatrix[resource]?.[action] || [];
    return authState.user.roles.some(role => allowedRoles.includes(role));
  }, [authState.user, hasRole]);

  const contextValue: AuthContextType = {
    ...authState,
    keycloak,
    login,
    logout,
    register,
    updateToken,
    hasRole,
    hasGroup,
    hasPermission,
  };

  if (authState.isLoading) {
    return LoadingComponent ? <LoadingComponent /> : <div>Loading...</div>;
  }

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

// HOC for components that require authentication
export function withAuth<P extends object>(
  Component: React.ComponentType<P>
): React.ComponentType<P> {
  return function AuthenticatedComponent(props: P) {
    const { isAuthenticated, isLoading } = useAuth();

    if (isLoading) {
      return <div>Loading...</div>;
    }

    if (!isAuthenticated) {
      return <div>Please log in to access this page.</div>;
    }

    return <Component {...props} />;
  };
}