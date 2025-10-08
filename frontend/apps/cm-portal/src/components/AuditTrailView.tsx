import React from 'react';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Badge
} from '@haven/ui';
import { User, Clock } from 'lucide-react';

export function AuditTrailView() {
  const mockAuditEntries = [
    {
      id: '1',
      action: 'CREATED',
      resourceType: 'PROJECT_LINKAGE',
      resourceId: 'linkage-1',
      userId: 'user-123',
      userName: 'John Doe',
      timestamp: new Date().toISOString(),
      details: 'Created new TH/RRH linkage',
      changes: {
        thProjectId: 'th-1',
        rrhProjectId: 'rrh-1'
      }
    },
    {
      id: '2',
      action: 'MODIFIED',
      resourceType: 'PROJECT_LINKAGE',
      resourceId: 'linkage-1',
      userId: 'user-456',
      userName: 'Jane Smith',
      timestamp: new Date(Date.now() - 86400000).toISOString(),
      details: 'Updated linkage effective date',
      changes: {
        linkageEffectiveDate: '2024-01-15'
      }
    },
    {
      id: '3',
      action: 'REVOKED',
      resourceType: 'PROJECT_LINKAGE',
      resourceId: 'linkage-2',
      userId: 'user-789',
      userName: 'Bob Johnson',
      timestamp: new Date(Date.now() - 172800000).toISOString(),
      details: 'Revoked linkage due to program termination',
      changes: {
        status: 'REVOKED',
        revocationReason: 'Program terminated'
      }
    }
  ];

  const getActionBadge = (action: string) => {
    const variants = {
      CREATED: 'default',
      MODIFIED: 'secondary',
      REVOKED: 'destructive'
    } as const;

    return (
      <Badge variant={variants[action as keyof typeof variants] || 'outline'}>
        {action}
      </Badge>
    );
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Audit Trail</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Timestamp</TableHead>
              <TableHead>Action</TableHead>
              <TableHead>User</TableHead>
              <TableHead>Details</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {mockAuditEntries.map((entry) => (
              <TableRow key={entry.id}>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <Clock className="h-4 w-4 text-muted-foreground" />
                    <div>
                      <div className="text-sm">
                        {new Date(entry.timestamp).toLocaleDateString()}
                      </div>
                      <div className="text-xs text-muted-foreground">
                        {new Date(entry.timestamp).toLocaleTimeString()}
                      </div>
                    </div>
                  </div>
                </TableCell>
                <TableCell>{getActionBadge(entry.action)}</TableCell>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <User className="h-4 w-4 text-muted-foreground" />
                    <span>{entry.userName}</span>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="text-sm">{entry.details}</div>
                  {Object.keys(entry.changes).length > 0 && (
                    <div className="text-xs text-muted-foreground mt-1">
                      Changed: {Object.keys(entry.changes).join(', ')}
                    </div>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
