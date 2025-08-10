import React from 'react';
import { useRouter } from 'next/router';
import { useAuth, usePermissions } from './hooks';
import type { ProtectedRouteProps, PermissionGuardProps } from './types';

// Component for protecting routes based on authentication
export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  roles = [],
  groups = [],
  redirectTo = '/login',
  fallback: FallbackComponent,
}) => {
  const { isAuthenticated, isLoading, user } = useAuth();
  const router = useRouter();

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push(redirectTo);
    }
  }, [isAuthenticated, isLoading, router, redirectTo]);

  if (isLoading) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  if (!isAuthenticated) {
    return FallbackComponent ? <FallbackComponent /> : null;
  }

  // Check role requirements
  if (roles.length > 0 && user) {
    const hasRequiredRole = roles.some(role => user.roles.includes(role));
    if (!hasRequiredRole) {
      return (
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <h2 className="text-2xl font-semibold text-gray-900 mb-2">Access Denied</h2>
            <p className="text-gray-600">You don't have permission to access this page.</p>
          </div>
        </div>
      );
    }
  }

  // Check group requirements
  if (groups.length > 0 && user) {
    const hasRequiredGroup = groups.some(group => user.groups.includes(group));
    if (!hasRequiredGroup) {
      return (
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <h2 className="text-2xl font-semibold text-gray-900 mb-2">Access Denied</h2>
            <p className="text-gray-600">You don't have permission to access this page.</p>
          </div>
        </div>
      );
    }
  }

  return <>{children}</>;
};

// Component for protecting content based on permissions
export const PermissionGuard: React.FC<PermissionGuardProps> = ({
  resource,
  action,
  children,
  fallback,
}) => {
  const { hasPermission } = usePermissions();

  if (!hasPermission(resource, action)) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};

// Login form component
export interface LoginFormProps {
  onSuccess?: () => void;
  className?: string;
}

export const LoginForm: React.FC<LoginFormProps> = ({ onSuccess, className }) => {
  const { login, isLoading } = useAuth();

  const handleLogin = async () => {
    try {
      await login();
      onSuccess?.();
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  return (
    <div className={`max-w-md mx-auto ${className || ''}`}>
      <div className="bg-white shadow-md rounded-lg p-6">
        <div className="text-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">Welcome to Haven</h2>
          <p className="text-gray-600 mt-2">Please sign in to continue</p>
        </div>
        
        <button
          onClick={handleLogin}
          disabled={isLoading}
          className="w-full bg-primary-600 text-white py-2 px-4 rounded-md hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isLoading ? 'Signing in...' : 'Sign In'}
        </button>
      </div>
    </div>
  );
};

// User profile dropdown component
export interface UserProfileDropdownProps {
  className?: string;
}

export const UserProfileDropdown: React.FC<UserProfileDropdownProps> = ({ className }) => {
  const { user, logout } = useAuth();
  const [isOpen, setIsOpen] = React.useState(false);

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  if (!user) return null;

  const initials = `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  const fullName = `${user.firstName} ${user.lastName}`;

  return (
    <div className={`relative ${className || ''}`}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center space-x-3 p-2 rounded-md hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
      >
        <div className="w-8 h-8 bg-primary-600 text-white rounded-full flex items-center justify-center text-sm font-medium">
          {initials}
        </div>
        <div className="hidden md:block text-left">
          <p className="text-sm font-medium text-gray-900">{fullName}</p>
          <p className="text-xs text-gray-500">{user.email}</p>
        </div>
        <svg
          className={`w-4 h-4 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isOpen && (
        <>
          <div
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
          />
          <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg ring-1 ring-black ring-opacity-5 z-20">
            <div className="py-1">
              <div className="px-4 py-2 text-sm text-gray-900 border-b">
                <p className="font-medium">{fullName}</p>
                <p className="text-gray-500">{user.email}</p>
              </div>
              <a
                href="/profile"
                className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                onClick={() => setIsOpen(false)}
              >
                Profile Settings
              </a>
              <button
                onClick={() => {
                  setIsOpen(false);
                  handleLogout();
                }}
                className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
              >
                Sign Out
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

// Role badge component
export interface RoleBadgeProps {
  roles: string[];
  maxDisplay?: number;
  className?: string;
}

export const RoleBadge: React.FC<RoleBadgeProps> = ({ 
  roles, 
  maxDisplay = 2, 
  className 
}) => {
  const displayRoles = roles.slice(0, maxDisplay);
  const remainingCount = roles.length - maxDisplay;

  const getRoleColor = (role: string): string => {
    switch (role.toLowerCase()) {
      case 'admin':
        return 'bg-red-100 text-red-800';
      case 'case-manager':
        return 'bg-blue-100 text-blue-800';
      case 'social-worker':
        return 'bg-green-100 text-green-800';
      case 'supervisor':
        return 'bg-purple-100 text-purple-800';
      case 'viewer':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className={`flex items-center space-x-1 ${className || ''}`}>
      {displayRoles.map((role, index) => (
        <span
          key={index}
          className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${getRoleColor(role)}`}
        >
          {role.replace('-', ' ').replace(/\b\w/g, l => l.toUpperCase())}
        </span>
      ))}
      {remainingCount > 0 && (
        <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
          +{remainingCount}
        </span>
      )}
    </div>
  );
};

// Authentication status indicator
export const AuthStatus: React.FC = () => {
  const { isAuthenticated, isLoading, user } = useAuth();

  if (isLoading) {
    return (
      <div className="flex items-center space-x-2 text-sm text-gray-500">
        <div className="w-2 h-2 bg-yellow-400 rounded-full animate-pulse"></div>
        <span>Authenticating...</span>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="flex items-center space-x-2 text-sm text-gray-500">
        <div className="w-2 h-2 bg-red-400 rounded-full"></div>
        <span>Not authenticated</span>
      </div>
    );
  }

  return (
    <div className="flex items-center space-x-2 text-sm text-gray-500">
      <div className="w-2 h-2 bg-green-400 rounded-full"></div>
      <span>Signed in as {user?.username}</span>
    </div>
  );
};