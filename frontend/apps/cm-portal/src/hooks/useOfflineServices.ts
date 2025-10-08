import { useState, useEffect } from 'react';
import { ServiceEpisode } from '@haven/api-client';

interface OfflineServiceData {
  id: string;
  clientId: string;
  enrollmentId: string;
  serviceType: string;
  deliveryMode: string;
  serviceDate: string;
  startTime: string;
  endTime: string;
  primaryProviderName: string;
  serviceDescription: string;
  serviceOutcome: string;
  notes: string;
  isConfidential: boolean;
  location: string;
  createdOffline: boolean;
  timestamp: string;
  syncStatus: 'pending' | 'syncing' | 'synced' | 'failed';
  syncError?: string;
}

const STORAGE_KEY = 'haven_offline_services';

export function useOfflineServices() {
  const [offlineServices, setOfflineServices] = useState<OfflineServiceData[]>([]);
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [syncInProgress, setSyncInProgress] = useState(false);

  // Load offline services from localStorage on mount
  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        const services = JSON.parse(stored);
        setOfflineServices(services);
      } catch (error) {
        console.error('Failed to parse stored offline services:', error);
        localStorage.removeItem(STORAGE_KEY);
      }
    }
  }, []);

  // Monitor online/offline status
  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      // Auto-sync when coming back online
      syncOfflineServices();
    };

    const handleOffline = () => {
      setIsOnline(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Save offline services to localStorage
  const saveToStorage = (services: OfflineServiceData[]) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(services));
    } catch (error) {
      console.error('Failed to save offline services:', error);
    }
  };

  // Save a new offline service
  const saveOfflineService = (serviceData: Omit<OfflineServiceData, 'syncStatus'>) => {
    const newService: OfflineServiceData = {
      ...serviceData,
      syncStatus: 'pending'
    };

    const updatedServices = [...offlineServices, newService];
    setOfflineServices(updatedServices);
    saveToStorage(updatedServices);

    // Try to sync immediately if online
    if (isOnline) {
      syncOfflineServices();
    }

    return newService.id;
  };

  // Update an offline service
  const updateOfflineService = (id: string, updates: Partial<OfflineServiceData>) => {
    const updatedServices = offlineServices.map(service =>
      service.id === id
        ? { ...service, ...updates, syncStatus: 'pending' as const }
        : service
    );

    setOfflineServices(updatedServices);
    saveToStorage(updatedServices);

    // Try to sync if online
    if (isOnline) {
      syncOfflineServices();
    }
  };

  // Delete an offline service
  const deleteOfflineService = (id: string) => {
    const updatedServices = offlineServices.filter(service => service.id !== id);
    setOfflineServices(updatedServices);
    saveToStorage(updatedServices);
  };

  // Convert offline service to API format
  const convertToApiFormat = (offlineService: OfflineServiceData) => {
    return {
      clientId: offlineService.clientId,
      enrollmentId: offlineService.enrollmentId,
      programId: '', // Would need to be provided or looked up
      programName: '', // Would need to be provided or looked up
      serviceType: offlineService.serviceType,
      deliveryMode: offlineService.deliveryMode,
      serviceDate: offlineService.serviceDate,
      plannedDurationMinutes: calculateDuration(offlineService.startTime, offlineService.endTime),
      primaryProviderName: offlineService.primaryProviderName,
      fundingSource: null, // Would need to be provided
      serviceDescription: offlineService.serviceDescription,
      isConfidential: offlineService.isConfidential
    };
  };

  // Calculate duration from start and end times
  const calculateDuration = (startTime: string, endTime: string): number => {
    if (!startTime || !endTime) return 30; // Default 30 minutes

    const start = new Date(`1970-01-01T${startTime}:00`);
    const end = new Date(`1970-01-01T${endTime}:00`);
    const diffMs = end.getTime() - start.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));

    return diffMinutes > 0 ? diffMinutes : 30;
  };

  // Sync offline services to the server
  const syncOfflineServices = async () => {
    if (!isOnline || syncInProgress) return;

    const pendingServices = offlineServices.filter(service => service.syncStatus === 'pending');
    if (pendingServices.length === 0) return;

    setSyncInProgress(true);

    for (const service of pendingServices) {
      try {
        // Update status to syncing
        updateServiceSyncStatus(service.id, 'syncing');

        // Convert to API format and send to server
        const apiData = convertToApiFormat(service);

        // This would be the actual API call
        // const response = await createServiceEpisode(apiData);

        // For now, simulate successful sync
        await new Promise(resolve => setTimeout(resolve, 1000));

        // Mark as synced
        updateServiceSyncStatus(service.id, 'synced');

      } catch (error) {
        console.error(`Failed to sync service ${service.id}:`, error);
        updateServiceSyncStatus(service.id, 'failed', error instanceof Error ? error.message : 'Sync failed');
      }
    }

    setSyncInProgress(false);

    // Clean up synced services after a delay
    setTimeout(() => {
      const remainingServices = offlineServices.filter(service => service.syncStatus !== 'synced');
      setOfflineServices(remainingServices);
      saveToStorage(remainingServices);
    }, 5000);
  };

  // Update sync status for a specific service
  const updateServiceSyncStatus = (id: string, status: OfflineServiceData['syncStatus'], error?: string) => {
    setOfflineServices(prev => prev.map(service =>
      service.id === id
        ? { ...service, syncStatus: status, syncError: error }
        : service
    ));
  };

  // Retry failed syncs
  const retryFailedSyncs = () => {
    const failedServices = offlineServices.filter(service => service.syncStatus === 'failed');
    failedServices.forEach(service => {
      updateServiceSyncStatus(service.id, 'pending');
    });

    if (isOnline && failedServices.length > 0) {
      syncOfflineServices();
    }
  };

  // Get services by client ID
  const getOfflineServicesByClient = (clientId: string) => {
    return offlineServices.filter(service => service.clientId === clientId);
  };

  // Get sync statistics
  const getSyncStats = () => {
    const pending = offlineServices.filter(s => s.syncStatus === 'pending').length;
    const syncing = offlineServices.filter(s => s.syncStatus === 'syncing').length;
    const failed = offlineServices.filter(s => s.syncStatus === 'failed').length;
    const synced = offlineServices.filter(s => s.syncStatus === 'synced').length;

    return { pending, syncing, failed, synced, total: offlineServices.length };
  };

  return {
    offlineServices,
    isOnline,
    syncInProgress,
    saveOfflineService,
    updateOfflineService,
    deleteOfflineService,
    syncOfflineServices,
    retryFailedSyncs,
    getOfflineServicesByClient,
    getSyncStats
  };
}