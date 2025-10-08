import React from 'react';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Badge,
  Alert,
  AlertDescription
} from '@haven/ui';
import { AlertTriangle, CheckCircle, XCircle } from 'lucide-react';

export function DataQualityDashboard() {
  const mockQualityIssues = [
    {
      id: '1',
      type: 'MISSING_DATA',
      severity: 'HIGH',
      description: 'Missing exit dates for 3 TH enrollments',
      count: 3,
      affectedLinkages: ['linkage-1']
    },
    {
      id: '2',
      type: 'INCONSISTENT_DATA',
      severity: 'MEDIUM',
      description: 'Service dates outside enrollment period',
      count: 5,
      affectedLinkages: ['linkage-1', 'linkage-2']
    }
  ];

  const getSeverityIcon = (severity: string) => {
    switch (severity) {
      case 'HIGH':
        return <XCircle className="h-4 w-4 text-red-600" />;
      case 'MEDIUM':
        return <AlertTriangle className="h-4 w-4 text-yellow-600" />;
      case 'LOW':
        return <CheckCircle className="h-4 w-4 text-green-600" />;
      default:
        return <AlertTriangle className="h-4 w-4 text-gray-600" />;
    }
  };

  const getSeverityBadge = (severity: string) => {
    const variants = {
      HIGH: 'destructive',
      MEDIUM: 'secondary',
      LOW: 'default'
    } as const;

    return (
      <Badge variant={variants[severity as keyof typeof variants] || 'outline'}>
        {getSeverityIcon(severity)}
        <span className="ml-1">{severity}</span>
      </Badge>
    );
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Data Quality Overview</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">87%</div>
              <div className="text-sm text-muted-foreground">Quality Score</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-yellow-600">8</div>
              <div className="text-sm text-muted-foreground">Issues Found</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">12</div>
              <div className="text-sm text-muted-foreground">Records Checked</div>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Data Quality Issues</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {mockQualityIssues.map((issue) => (
              <Alert key={issue.id}>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      {getSeverityBadge(issue.severity)}
                      <span className="font-medium">{issue.type.replace(/_/g, ' ')}</span>
                    </div>
                    <AlertDescription>
                      {issue.description}
                    </AlertDescription>
                    <div className="text-sm text-muted-foreground mt-1">
                      Affects {issue.count} record(s)
                    </div>
                  </div>
                </div>
              </Alert>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
