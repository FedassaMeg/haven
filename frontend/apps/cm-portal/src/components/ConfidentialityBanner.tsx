import React from 'react';
import { Alert, AlertDescription } from '@haven/ui';
import { ConfidentialityGuardrails } from '@haven/api-client';

interface ConfidentialityBannerProps {
  guardrails: ConfidentialityGuardrails;
  className?: string;
}

export const ConfidentialityBanner: React.FC<ConfidentialityBannerProps> = ({ 
  guardrails, 
  className = '' 
}) => {
  if (!guardrails.bannerWarningText) {
    return null;
  }

  const getSeverityStyles = (severity?: string) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-red-50 border-red-200 text-red-800';
      case 'HIGH':
        return 'bg-orange-50 border-orange-200 text-orange-800';
      case 'MEDIUM':
        return 'bg-yellow-50 border-yellow-200 text-yellow-800';
      default:
        return 'bg-blue-50 border-blue-200 text-blue-800';
    }
  };

  const getIconForSeverity = (severity?: string) => {
    switch (severity) {
      case 'CRITICAL':
        return (
          <svg className="w-5 h-5 text-red-600" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
        );
      case 'HIGH':
        return (
          <svg className="w-5 h-5 text-orange-600" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
        );
      default:
        return (
          <svg className="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd" />
          </svg>
        );
    }
  };

  return (
    <div className={`border-l-4 p-4 mb-4 ${getSeverityStyles(guardrails.bannerSeverity)} ${className}`}>
      <div className="flex">
        <div className="flex-shrink-0">
          {getIconForSeverity(guardrails.bannerSeverity)}
        </div>
        <div className="ml-3 flex-1">
          <div className="text-sm font-medium">
            {guardrails.bannerWarningText}
          </div>
          <div className="mt-2 text-xs">
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              {guardrails.isSafeAtHome && (
                <div className="flex items-center">
                  <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                    🏠 Safe at Home
                  </span>
                </div>
              )}
              {guardrails.isComparableDbOnly && (
                <div className="flex items-center">
                  <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-indigo-100 text-indigo-800">
                    📊 Comparable DB Only
                  </span>
                </div>
              )}
              {guardrails.hasConfidentialLocation && (
                <div className="flex items-center">
                  <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-pink-100 text-pink-800">
                    📍 Confidential Location
                  </span>
                </div>
              )}
              {guardrails.hasRestrictedData && (
                <div className="flex items-center">
                  <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                    🔒 Restricted Data
                  </span>
                </div>
              )}
            </div>
          </div>
          <div className="mt-2 text-xs opacity-75">
            Data System: {guardrails.dataSystem} • Visibility: {guardrails.visibilityLevel}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ConfidentialityBanner;