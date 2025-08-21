import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
} from "react";
import Keycloak, { KeycloakInstance, KeycloakProfile } from "keycloak-js";
import { UserRole, ResourceType, ActionType } from "./types";
import type {
  AuthContextType,
  AuthProviderProps,
  AuthState,
  User,
  KeycloakConfig,
} from "./types";

const AuthContext = createContext<AuthContextType | null>(null);

const TOKEN_KEY = "haven-auth-token";
const REFRESH_TOKEN_KEY = "haven-refresh-token";

export const AuthProvider: React.FC<AuthProviderProps> = ({
  children,
  config,
  initOptions = {
    onLoad: "login-required" as const,
    checkLoginIframe: false,
    enableLogging: process.env.NODE_ENV === "development",
    pkceMethod: "S256" as "S256",
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

  // Define transformKeycloakUser before useEffect
  const transformKeycloakUser = useCallback(
    (kc: KeycloakInstance, profile: KeycloakProfile): User => {
      const realmRoles = kc.realmAccess?.roles || [];
      const clientRoles = kc.resourceAccess?.[config.clientId]?.roles || [];
      const groups = (kc.tokenParsed as any)?.groups || [];

      return {
        id: profile.id || kc.subject || "",
        username: profile.username || "",
        email: profile.email || "",
        firstName: profile.firstName || "",
        lastName: profile.lastName || "",
        roles: [...realmRoles, ...clientRoles],
        groups,
        attributes: profile.attributes,
      };
    },
    [config.clientId]
  );

  // Initialize Keycloak - only once on mount
  useEffect(() => {
    let mounted = true;
    
    const initKeycloak = async () => {
      try {
        const kc = new Keycloak({
          url: config.url,
          realm: config.realm,
          clientId: config.clientId,
        });

        console.log('[AuthProvider] Initializing Keycloak with config:', {
          url: config.url,
          realm: config.realm,
          clientId: config.clientId,
        });

        const authenticated = await kc.init({
          onLoad: initOptions.onLoad,
          checkLoginIframe: false, // Disable iframe check to prevent redirect loops
          enableLogging: initOptions.enableLogging,
          pkceMethod: initOptions.pkceMethod,
          token: sessionStorage.getItem(TOKEN_KEY) || undefined,
          refreshToken: sessionStorage.getItem(REFRESH_TOKEN_KEY) || undefined,
          silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        });

        console.log('[AuthProvider] Keycloak init result:', authenticated);

        if (!mounted) return;

        if (authenticated) {
          console.log('[AuthProvider] User is authenticated, loading profile...');
          const profile = await kc.loadUserProfile();
          const user = transformKeycloakUser(kc, profile);
          console.log('[AuthProvider] User profile loaded:', user);

          setAuthState({
            user,
            isAuthenticated: true,
            isLoading: false,
            token: kc.token || null,
            refreshToken: kc.refreshToken || null,
          });

          // Store tokens in sessionStorage
          if (kc.token) {
            sessionStorage.setItem(TOKEN_KEY, kc.token);
          }
          if (kc.refreshToken) {
            sessionStorage.setItem(REFRESH_TOKEN_KEY, kc.refreshToken);
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
          kc.updateToken(30)
            .then((refreshed) => {
              if (refreshed) {
                if (kc.token) {
                  sessionStorage.setItem(TOKEN_KEY, kc.token);
                }
                setAuthState((prev) => ({ ...prev, token: kc.token || null }));
              }
            })
            .catch(() => {
              console.error("Failed to refresh token");
              logout();
            });
        };
      } catch (error) {
        if (!mounted) return;
        console.error("Failed to initialize Keycloak:", error);
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
    
    return () => {
      mounted = false;
    };
  }, [config.url, config.realm, config.clientId, transformKeycloakUser]); // Include transformKeycloakUser in deps

  const login = useCallback(
    async (redirectUri?: string) => {
      if (keycloak) {
        await keycloak.login({ redirectUri });
      }
    },
    [keycloak]
  );

  const logout = useCallback(
    async (redirectUri?: string) => {
      if (keycloak) {
        // Clear sessionStorage
        sessionStorage.removeItem(TOKEN_KEY);
        sessionStorage.removeItem(REFRESH_TOKEN_KEY);

        setAuthState({
          user: null,
          isAuthenticated: false,
          isLoading: false,
          token: null,
          refreshToken: null,
        });

        await keycloak.logout({ redirectUri });
      }
    },
    [keycloak]
  );

  const register = useCallback(
    async (redirectUri?: string) => {
      if (keycloak) {
        await keycloak.register({ redirectUri });
      }
    },
    [keycloak]
  );

  const updateToken = useCallback(
    async (minValidity = 30): Promise<boolean> => {
      if (keycloak) {
        try {
          const refreshed = await keycloak.updateToken(minValidity);
          if (refreshed && keycloak.token) {
            sessionStorage.setItem(TOKEN_KEY, keycloak.token);
            setAuthState((prev) => ({
              ...prev,
              token: keycloak.token || null,
            }));
          }
          return refreshed;
        } catch (error) {
          console.error("Failed to update token:", error);
          return false;
        }
      }
      return false;
    },
    [keycloak]
  );

  const hasRole = useCallback(
    (role: string): boolean => {
      return authState.user?.roles.includes(role) || false;
    },
    [authState.user]
  );

  const hasGroup = useCallback(
    (group: string): boolean => {
      return authState.user?.groups.includes(group) || false;
    },
    [authState.user]
  );

  const hasPermission = useCallback(
    (resource: string, action: string): boolean => {
      if (!authState.user) return false;

      // Admin has all permissions
      if (hasRole(UserRole.ADMIN)) return true;

      // Define permission matrix
      const permissionMatrix: Record<string, Record<string, string[]>> = {
        [ResourceType.CLIENT]: {
          [ActionType.CREATE]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER],
          [ActionType.READ]: [
            UserRole.CASE_MANAGER,
            UserRole.SOCIAL_WORKER,
            UserRole.SUPERVISOR,
            UserRole.VIEWER,
          ],
          [ActionType.UPDATE]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER],
          [ActionType.DELETE]: [UserRole.CASE_MANAGER, UserRole.SUPERVISOR],
        },
        [ResourceType.CASE]: {
          [ActionType.CREATE]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER],
          [ActionType.READ]: [
            UserRole.CASE_MANAGER,
            UserRole.SOCIAL_WORKER,
            UserRole.SUPERVISOR,
            UserRole.VIEWER,
          ],
          [ActionType.UPDATE]: [UserRole.CASE_MANAGER, UserRole.SOCIAL_WORKER],
          [ActionType.ASSIGN]: [UserRole.CASE_MANAGER, UserRole.SUPERVISOR],
          [ActionType.CLOSE]: [UserRole.CASE_MANAGER, UserRole.SUPERVISOR],
        },
        [ResourceType.REPORT]: {
          [ActionType.READ]: [
            UserRole.CASE_MANAGER,
            UserRole.SOCIAL_WORKER,
            UserRole.SUPERVISOR,
            UserRole.VIEWER,
          ],
          [ActionType.CREATE]: [UserRole.SUPERVISOR],
        },
      };

      const allowedRoles = permissionMatrix[resource]?.[action] || [];
      return authState.user.roles.some((role) => allowedRoles.includes(role));
    },
    [authState.user, hasRole]
  );

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
    <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
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
