import { useCallback } from 'react';
import { useAuth as useAuthContext } from './providers';
import { UserRole, ResourceType, ActionType } from './types';

// Re-export useAuth from providers for convenience
export { useAuth } from './providers';

// Custom hooks for common auth operations
export function usePermissions() {
  const { hasRole, hasGroup, hasPermission } = useAuthContext();

  const checkRole = useCallback((role: UserRole | string) => {
    return hasRole(role);
  }, [hasRole]);

  const checkGroup = useCallback((group: string) => {
    return hasGroup(group);
  }, [hasGroup]);

  const checkPermission = useCallback((resource: ResourceType | string, action: ActionType | string) => {
    return hasPermission(resource, action);
  }, [hasPermission]);

  return {
    hasRole: checkRole,
    hasGroup: checkGroup,
    hasPermission: checkPermission,
  };
}

export function useAuthActions() {
  const { login, logout, register, updateToken } = useAuthContext();

  const handleLogin = useCallback((redirectUri?: string) => {
    return login(redirectUri);
  }, [login]);

  const handleLogout = useCallback((redirectUri?: string) => {
    return logout(redirectUri);
  }, [logout]);

  const handleRegister = useCallback((redirectUri?: string) => {
    return register(redirectUri);
  }, [register]);

  const refreshToken = useCallback((minValidity?: number) => {
    return updateToken(minValidity);
  }, [updateToken]);

  return {
    login: handleLogin,
    logout: handleLogout,
    register: handleRegister,
    refreshToken,
  };
}

export function useCurrentUser() {
  const { user, isAuthenticated } = useAuthContext();

  const fullName = user ? `${user.firstName} ${user.lastName}`.trim() : '';
  const initials = user 
    ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase()
    : '';

  return {
    user,
    isAuthenticated,
    fullName,
    initials,
    isAdmin: user?.roles.includes(UserRole.ADMIN) || false,
    isCaseManager: user?.roles.includes(UserRole.CASE_MANAGER) || false,
    isSocialWorker: user?.roles.includes(UserRole.SOCIAL_WORKER) || false,
    isSupervisor: user?.roles.includes(UserRole.SUPERVISOR) || false,
  };
}

export function useTokenManagement() {
  const { token, refreshToken, updateToken } = useAuthContext();

  const ensureValidToken = useCallback(async (minValidity = 30): Promise<string | null> => {
    if (!token) return null;
    
    const refreshed = await updateToken(minValidity);
    return refreshed ? token : null;
  }, [token, updateToken]);

  return {
    token,
    refreshToken,
    ensureValidToken,
  };
}