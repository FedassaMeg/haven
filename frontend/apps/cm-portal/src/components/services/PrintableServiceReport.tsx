import React from 'react';
import { ServiceEpisode } from '@haven/api-client';
import { format } from 'date-fns';

interface PrintableServiceReportProps {
  serviceEpisodes: ServiceEpisode[];
  clientId: string;
  clientName: string;
  reportTitle?: string;
  reportPeriod?: string;
  includeConfidential?: boolean;
}

export default function PrintableServiceReport({
  serviceEpisodes,
  clientId,
  clientName,
  reportTitle = 'Service Delivery Report',
  reportPeriod,
  includeConfidential = false
}: PrintableServiceReportProps) {
  // Filter out confidential services if not included
  const filteredServices = includeConfidential
    ? serviceEpisodes
    : serviceEpisodes.filter(s => !s.isConfidential);

  const completedServices = filteredServices.filter(s => s.completionStatus === 'COMPLETED');
  const totalHours = completedServices.reduce((sum, s) => sum + (s.actualDurationMinutes || 0), 0) / 60;

  const servicesByType = filteredServices.reduce((acc, service) => {
    acc[service.serviceType] = (acc[service.serviceType] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  const printStyles = `
    @media print {
      body { font-family: Arial, sans-serif; font-size: 12px; line-height: 1.4; }
      .print-page-break { page-break-before: always; }
      .print-no-break { page-break-inside: avoid; }
      .print-hide { display: none !important; }
      .print-header { margin-bottom: 20px; }
      .print-footer { margin-top: 20px; border-top: 1px solid #ccc; padding-top: 10px; }
      table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
      th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
      th { background-color: #f5f5f5; font-weight: bold; }
      .summary-box { border: 1px solid #ccc; padding: 15px; margin: 10px 0; }
      .confidential-warning { background-color: #fee; border: 2px solid #f00; padding: 10px; margin: 10px 0; }
      .page-header { position: fixed; top: 0; left: 0; right: 0; height: 50px; background: white; border-bottom: 1px solid #ccc; }
      .page-content { margin-top: 60px; }
    }
  `;

  return (
    <>
      <style dangerouslySetInnerHTML={{ __html: printStyles }} />

      <div className="print:text-black print:bg-white">
        {/* Print Header */}
        <div className="print-header">
          <div className="text-center mb-6">
            <h1 className="text-2xl font-bold mb-2">{reportTitle}</h1>
            <div className="text-lg mb-1">Client: {clientName}</div>
            <div className="text-sm text-gray-600">Client ID: {clientId}</div>
            {reportPeriod && (
              <div className="text-sm text-gray-600">Period: {reportPeriod}</div>
            )}
            <div className="text-sm text-gray-600">
              Generated: {format(new Date(), 'MMM d, yyyy h:mm a')}
            </div>
          </div>

          {/* Confidentiality Warning */}
          {includeConfidential && (
            <div className="confidential-warning">
              <strong>CONFIDENTIAL INFORMATION</strong><br/>
              This report contains confidential client information protected under applicable federal and state privacy laws.
              Unauthorized disclosure is prohibited.
            </div>
          )}
        </div>

        {/* Executive Summary */}
        <div className="summary-box print-no-break">
          <h2 className="text-lg font-bold mb-3">Executive Summary</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <strong>Total Services:</strong> {filteredServices.length}
            </div>
            <div>
              <strong>Completed:</strong> {completedServices.length}
            </div>
            <div>
              <strong>Total Hours:</strong> {totalHours.toFixed(1)}
            </div>
            <div>
              <strong>Completion Rate:</strong> {filteredServices.length > 0 ? Math.round((completedServices.length / filteredServices.length) * 100) : 0}%
            </div>
          </div>
        </div>

        {/* Service Type Summary */}
        <div className="print-no-break mb-6">
          <h2 className="text-lg font-bold mb-3">Service Type Summary</h2>
          <table>
            <thead>
              <tr>
                <th>Service Type</th>
                <th>Count</th>
                <th>Percentage</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(servicesByType)
                .sort(([,a], [,b]) => b - a)
                .map(([type, count]) => (
                  <tr key={type}>
                    <td>{type.replace(/_/g, ' ')}</td>
                    <td>{count}</td>
                    <td>{Math.round((count / filteredServices.length) * 100)}%</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>

        {/* Service Details */}
        <div className="print-page-break">
          <h2 className="text-lg font-bold mb-3">Service Details</h2>
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Service Type</th>
                <th>Provider</th>
                <th>Duration</th>
                <th>Status</th>
                <th>Outcome</th>
              </tr>
            </thead>
            <tbody>
              {filteredServices
                .sort((a, b) => new Date(b.serviceDate).getTime() - new Date(a.serviceDate).getTime())
                .map((service) => (
                  <tr key={service.episodeId}>
                    <td>{format(new Date(service.serviceDate), 'MMM d, yyyy')}</td>
                    <td>
                      {service.serviceType.replace(/_/g, ' ')}
                      {service.isConfidential && includeConfidential && (
                        <span className="text-red-600"> [CONFIDENTIAL]</span>
                      )}
                    </td>
                    <td>{service.primaryProviderName}</td>
                    <td>
                      {service.actualDurationMinutes || service.plannedDurationMinutes || 'N/A'}
                      {(service.actualDurationMinutes || service.plannedDurationMinutes) && ' min'}
                    </td>
                    <td>{service.completionStatus}</td>
                    <td className="text-xs">
                      {service.serviceOutcome ?
                        (service.serviceOutcome.length > 100 ?
                          service.serviceOutcome.substring(0, 100) + '...' :
                          service.serviceOutcome
                        ) :
                        'N/A'
                      }
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>

        {/* Detailed Service Narratives */}
        {filteredServices.some(s => s.serviceDescription || s.serviceOutcome || s.notes) && (
          <div className="print-page-break">
            <h2 className="text-lg font-bold mb-3">Service Narratives</h2>
            {filteredServices
              .filter(s => s.serviceDescription || s.serviceOutcome || s.notes)
              .sort((a, b) => new Date(b.serviceDate).getTime() - new Date(a.serviceDate).getTime())
              .map((service) => (
                <div key={service.episodeId} className="print-no-break mb-4 border-b pb-4">
                  <div className="font-bold text-sm">
                    {format(new Date(service.serviceDate), 'MMM d, yyyy')} - {service.serviceType.replace(/_/g, ' ')}
                    {service.isConfidential && includeConfidential && (
                      <span className="text-red-600"> [CONFIDENTIAL]</span>
                    )}
                  </div>
                  <div className="text-xs text-gray-600 mb-2">
                    Provider: {service.primaryProviderName} | Mode: {service.deliveryMode}
                  </div>

                  {service.serviceDescription && (
                    <div className="mb-2">
                      <strong>Description:</strong> {service.serviceDescription}
                    </div>
                  )}

                  {service.serviceOutcome && (
                    <div className="mb-2">
                      <strong>Outcome:</strong> {service.serviceOutcome}
                    </div>
                  )}

                  {service.notes && (
                    <div className="mb-2">
                      <strong>Notes:</strong> {service.notes}
                    </div>
                  )}

                  {service.followUpRequired && (
                    <div className="mb-2 text-orange-700">
                      <strong>Follow-up Required:</strong> {service.followUpRequired}
                      {service.followUpDate && (
                        <span> (Due: {format(new Date(service.followUpDate), 'MMM d, yyyy')})</span>
                      )}
                    </div>
                  )}
                </div>
              ))}
          </div>
        )}

        {/* Print Footer */}
        <div className="print-footer text-xs text-gray-600 text-center">
          <div>Page generated on {format(new Date(), 'MMM d, yyyy h:mm a')}</div>
          <div>Haven Client Management System - Service Delivery Report</div>
          {includeConfidential && (
            <div className="text-red-600 font-bold">
              CONFIDENTIAL - Authorized Personnel Only
            </div>
          )}
        </div>
      </div>
    </>
  );
}