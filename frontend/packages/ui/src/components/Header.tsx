import React, { useState, useRef, useEffect } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';

export interface SearchBarProps {
  placeholder?: string;
  onSearch?: (query: string) => void;
  className?: string;
}

const searchBarVariants = cva(
  'relative',
  {
    variants: {},
    defaultVariants: {},
  }
);

const searchInputVariants = cva(
  'block w-full pl-10 pr-3 py-2 border border-secondary-300 rounded-md leading-5 bg-white placeholder-secondary-500 focus:outline-none focus:placeholder-secondary-400 focus:ring-1 focus:ring-primary-500 focus:border-primary-500 sm:text-sm',
  {
    variants: {},
    defaultVariants: {},
  }
);

export const SearchBar: React.FC<SearchBarProps> = ({
  placeholder = 'Search...',
  onSearch,
  className,
}) => {
  const [query, setQuery] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch?.(query);
  };

  return (
    <form onSubmit={handleSubmit} className={searchBarVariants({ className })}>
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <svg className="h-5 w-5 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className={searchInputVariants()}
          placeholder={placeholder}
        />
      </div>
    </form>
  );
};

export interface NotificationItem {
  id: string;
  title: string;
  description?: string;
  time: string;
  read: boolean;
  type?: 'info' | 'success' | 'warning' | 'error';
}

export interface NotificationsDropdownProps {
  notifications: NotificationItem[];
  onMarkAsRead?: (id: string) => void;
  onMarkAllAsRead?: () => void;
  className?: string;
}

const notificationItemVariants = cva(
  'px-4 py-3 hover:bg-secondary-50 cursor-pointer border-b border-secondary-100',
  {
    variants: {
      read: {
        true: '',
        false: 'bg-primary-50',
      },
    },
    defaultVariants: {
      read: true,
    },
  }
);

const notificationTitleVariants = cva(
  'text-sm',
  {
    variants: {
      read: {
        true: 'text-secondary-900',
        false: 'font-medium text-secondary-900',
      },
    },
    defaultVariants: {
      read: true,
    },
  }
);

const dropdownVariants = cva(
  'relative',
  {
    variants: {},
    defaultVariants: {},
  }
);

export const NotificationsDropdown: React.FC<NotificationsDropdownProps> = ({
  notifications,
  onMarkAsRead,
  onMarkAllAsRead,
  className,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const unreadCount = notifications.filter(n => !n.read).length;

  return (
    <div className={dropdownVariants({ className })} ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 text-secondary-400 hover:text-secondary-500 focus:outline-none focus:text-secondary-500"
      >
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute top-0 right-0 block h-2 w-2 rounded-full bg-red-400 ring-2 ring-white" />
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-md shadow-lg overflow-hidden z-20">
          <div className="px-4 py-3 border-b border-secondary-200">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-medium text-secondary-900">Notifications</h3>
              {unreadCount > 0 && (
                <button
                  onClick={onMarkAllAsRead}
                  className="text-xs text-primary-600 hover:text-primary-500"
                >
                  Mark all as read
                </button>
              )}
            </div>
          </div>
          <div className="max-h-96 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="px-4 py-6 text-center text-sm text-secondary-500">
                No notifications
              </div>
            ) : (
              notifications.map((notification) => (
                <div
                  key={notification.id}
                  className={notificationItemVariants({ read: notification.read })}
                  onClick={() => onMarkAsRead?.(notification.id)}
                >
                  <div className="flex items-start">
                    <div className="flex-1">
                      <p className={notificationTitleVariants({ read: notification.read })}>
                        {notification.title}
                      </p>
                      {notification.description && (
                        <p className="mt-1 text-xs text-secondary-500">{notification.description}</p>
                      )}
                      <p className="mt-1 text-xs text-secondary-400">{notification.time}</p>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export interface UserProfile {
  name: string;
  email: string;
  avatar?: string;
}

export interface UserProfileDropdownProps {
  user: UserProfile;
  onSignOut?: () => void;
  onProfile?: () => void;
  onSettings?: () => void;
  className?: string;
}

export const UserProfileDropdown: React.FC<UserProfileDropdownProps> = ({
  user,
  onSignOut,
  onProfile,
  onSettings,
  className,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className={dropdownVariants({ className })} ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center text-sm rounded-full focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
      >
        {user.avatar ? (
          <img className="h-8 w-8 rounded-full" src={user.avatar} alt={user.name} />
        ) : (
          <div className="h-8 w-8 rounded-full bg-primary-500 flex items-center justify-center text-white font-medium">
            {user.name.charAt(0).toUpperCase()}
          </div>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg py-1 z-20">
          <div className="px-4 py-2 border-b border-secondary-200">
            <p className="text-sm font-medium text-secondary-900">{user.name}</p>
            <p className="text-xs text-secondary-500">{user.email}</p>
          </div>
          {onProfile && (
            <button
              onClick={() => {
                onProfile();
                setIsOpen(false);
              }}
              className="block w-full text-left px-4 py-2 text-sm text-secondary-700 hover:bg-secondary-100"
            >
              Your Profile
            </button>
          )}
          {onSettings && (
            <button
              onClick={() => {
                onSettings();
                setIsOpen(false);
              }}
              className="block w-full text-left px-4 py-2 text-sm text-secondary-700 hover:bg-secondary-100"
            >
              Settings
            </button>
          )}
          {onSignOut && (
            <>
              <div className="border-t border-secondary-200 my-1" />
              <button
                onClick={() => {
                  onSignOut();
                  setIsOpen(false);
                }}
                className="block w-full text-left px-4 py-2 text-sm text-secondary-700 hover:bg-secondary-100"
              >
                Sign out
              </button>
            </>
          )}
        </div>
      )}
    </div>
  );
};