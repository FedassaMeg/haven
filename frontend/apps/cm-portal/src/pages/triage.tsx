import { useTriageAlerts, useTriageDashboard } from "@haven/api-client";
import { ProtectedRoute, useCurrentUser } from "@haven/auth";
import {
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Table,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@haven/ui";
import { useState } from "react";
import AppLayout from "../components/AppLayout";

function TriageDashboardContent() {
  const { user } = useCurrentUser();
  const [activeTab, setActiveTab] = useState("alerts");
  const [selectedFilter, setSelectedFilter] = useState<string>("all");
  const [selectedSeverity, setSelectedSeverity] = useState<string>("");

  const { dashboard, loading: dashboardLoading } = useTriageDashboard();
  const {
    alerts,
    loading: alertsLoading,
    acknowledgeAlert,
    resolveAlert,
  } = useTriageAlerts({
    severity: selectedSeverity || undefined,
    status: selectedFilter === "active" ? "ACTIVE" : undefined,
  });

  const severityColors = {
    CRITICAL: "bg-red-100 text-red-800 border-red-200",
    HIGH: "bg-orange-100 text-orange-800 border-orange-200",
    MEDIUM: "bg-yellow-100 text-yellow-800 border-yellow-200",
    LOW: "bg-blue-100 text-blue-800 border-blue-200",
  };

  const alertTypeLabels = {
    HIGH_RISK_CLIENT: "High Risk Client",
    COURT_DATE: "Court Date",
    ROI_EXPIRING: "ROI Expiring",
    LEASE_RENEWAL: "Lease Renewal",
    RECERTIFICATION_DUE: "Recertification Due",
    PAYMENT_PENDING: "Payment Pending",
    SAFETY_CHECK_NEEDED: "Safety Check Needed",
    PROTECTION_ORDER_EXPIRING: "Protection Order Expiring",
    CONSENT_EXPIRING: "Consent Expiring",
    DOCUMENTATION_MISSING: "Documentation Missing",
    FUNDING_DEADLINE: "Funding Deadline",
    COMPLIANCE_REVIEW: "Compliance Review Required",
    CONFIDENTIALITY_ALERT: "Confidentiality Protocol Alert",
  };

  const handleAcknowledge = async (alertId: string) => {
    await acknowledgeAlert(alertId);
  };

  const handleResolve = async (alertId: string) => {
    await resolveAlert(alertId);
  };

  const alertColumns = [
    {
      key: "severity" as const,
      label: "Priority",
      width: "100px",
      render: (value: string) => (
        <Badge
          variant="secondary"
          className={severityColors[value as keyof typeof severityColors]}
        >
          {value}
        </Badge>
      ),
    },
    {
      key: "alertType" as const,
      label: "Type",
      render: (value: string) =>
        alertTypeLabels[value as keyof typeof alertTypeLabels] || value,
    },
    {
      key: "clientName" as const,
      label: "Client",
      render: (value: string, alert: any) => (
        <div>
          <div className="font-medium">{value}</div>
          {alert.caseNumber && (
            <div className="text-sm text-slate-600">{alert.caseNumber}</div>
          )}
          {alert.isSafeAtHome && (
            <Badge
              variant="secondary"
              className="bg-purple-100 text-purple-800 border-purple-200 text-xs mt-1"
            >
              🏠 Safe at Home
            </Badge>
          )}
          {alert.isComparableDbOnly && (
            <Badge
              variant="secondary"
              className="bg-amber-100 text-amber-800 border-amber-200 text-xs mt-1"
            >
              📊 Comparable DB
            </Badge>
          )}
          {alert.hasConfidentialLocation && (
            <Badge
              variant="secondary"
              className="bg-pink-100 text-pink-800 border-pink-200 text-xs mt-1"
            >
              📍 Confidential
            </Badge>
          )}
        </div>
      ),
    },
    {
      key: "description" as const,
      label: "Description",
      render: (value: string) => (
        <div className="max-w-xs truncate" title={value}>
          {value}
        </div>
      ),
    },
    {
      key: "dueDate" as const,
      label: "Due Date",
      render: (value: string, alert: any) => (
        <div className={alert.isOverdue ? "text-red-600 font-medium" : ""}>
          {new Date(value).toLocaleDateString()}
          {alert.isOverdue && (
            <div className="text-xs text-red-600">
              {Math.abs(alert.daysUntilDue)} days overdue
            </div>
          )}
        </div>
      ),
    },
    {
      key: "assignedWorkerName" as const,
      label: "Assigned To",
      render: (value: string) => value || "Unassigned",
    },
    {
      key: "actions" as const,
      label: "Actions",
      width: "180px",
      render: (value: any, alert: any) => (
        <div className="flex space-x-2">
          {alert.status === "ACTIVE" && (
            <>
              <Button
                size="sm"
                variant="outline"
                onClick={() => handleAcknowledge(alert.id)}
              >
                Acknowledge
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => handleResolve(alert.id)}
              >
                Resolve
              </Button>
            </>
          )}
          {alert.status === "ACKNOWLEDGED" && (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => handleResolve(alert.id)}
            >
              Resolve
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Triage Center</h1>
          <p className="text-slate-600">
            Critical alerts and upcoming deadlines requiring immediate attention
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <Button variant="outline" size="sm">
            <svg
              className="w-4 h-4 mr-2"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
              />
            </svg>
            Refresh
          </Button>
        </div>
      </div>

      {/* Alert Summary Cards */}
      {dashboard && (
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
          <Card className="border-red-200 bg-red-50">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-red-600">Critical</p>
                  <p className="text-2xl font-bold text-red-700">
                    {dashboard.criticalCount}
                  </p>
                </div>
                <div className="h-8 w-8 bg-red-200 rounded-full flex items-center justify-center">
                  <svg
                    className="w-4 h-4 text-red-600"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                      clipRule="evenodd"
                    />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="border-orange-200 bg-orange-50">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-orange-600">High</p>
                  <p className="text-2xl font-bold text-orange-700">
                    {dashboard.highCount}
                  </p>
                </div>
                <div className="h-8 w-8 bg-orange-200 rounded-full flex items-center justify-center">
                  <svg
                    className="w-4 h-4 text-orange-600"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                      clipRule="evenodd"
                    />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="border-yellow-200 bg-yellow-50">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-yellow-600">Medium</p>
                  <p className="text-2xl font-bold text-yellow-700">
                    {dashboard.mediumCount}
                  </p>
                </div>
                <div className="h-8 w-8 bg-yellow-200 rounded-full flex items-center justify-center">
                  <svg
                    className="w-4 h-4 text-yellow-600"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"
                      clipRule="evenodd"
                    />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="border-blue-200 bg-blue-50">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-blue-600">Low</p>
                  <p className="text-2xl font-bold text-blue-700">
                    {dashboard.lowCount}
                  </p>
                </div>
                <div className="h-8 w-8 bg-blue-200 rounded-full flex items-center justify-center">
                  <svg
                    className="w-4 h-4 text-blue-600"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                      clipRule="evenodd"
                    />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="border-gray-200 bg-gray-50">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Overdue</p>
                  <p className="text-2xl font-bold text-gray-700">
                    {dashboard.overdueCount}
                  </p>
                </div>
                <div className="h-8 w-8 bg-gray-200 rounded-full flex items-center justify-center">
                  <svg
                    className="w-4 h-4 text-gray-600"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M10 18a8 8 0 100-16 8 8 0 000 16zm.707-10.293a1 1 0 00-1.414-1.414l-3 3a1 1 0 001.414 1.414l2.293-2.293V15a1 1 0 102 0V8a1 1 0 00-.293-.707z"
                      clipRule="evenodd"
                    />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Filters</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-4">
            <div className="flex items-center space-x-2">
              <label className="text-sm font-medium text-slate-700">
                Status:
              </label>
              <select
                value={selectedFilter}
                onChange={(e) => setSelectedFilter(e.target.value)}
                className="border border-slate-300 rounded px-3 py-1 text-sm"
              >
                <option value="all">All Alerts</option>
                <option value="active">Active Only</option>
                <option value="acknowledged">Acknowledged</option>
                <option value="resolved">Resolved</option>
              </select>
            </div>
            <div className="flex items-center space-x-2">
              <label className="text-sm font-medium text-slate-700">
                Severity:
              </label>
              <select
                value={selectedSeverity}
                onChange={(e) => setSelectedSeverity(e.target.value)}
                className="border border-slate-300 rounded px-3 py-1 text-sm"
              >
                <option value="">All Severities</option>
                <option value="CRITICAL">Critical</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="alerts">System Alerts</TabsTrigger>
          <TabsTrigger value="protocols">Crisis Protocols</TabsTrigger>
        </TabsList>

        <TabsContent value="alerts" className="mt-6">
          {/* Alerts Table */}
          <Card>
            <CardHeader>
              <CardTitle>Active Alerts</CardTitle>
            </CardHeader>
            <CardContent>
              <Table
                data={alerts || []}
                columns={alertColumns}
                loading={alertsLoading}
                emptyMessage="No alerts found"
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="protocols" className="mt-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle className="text-red-700">
                  Emergency Protocols
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
                    <h4 className="font-medium text-red-900 mb-2">
                      🚨 Immediate Danger
                    </h4>
                    <p className="text-sm text-red-700 mb-3">
                      Client reports immediate physical danger or threat to life
                    </p>
                    <Button className="bg-red-600 hover:bg-red-700 text-white w-full">
                      Call 911 / Emergency Services
                    </Button>
                  </div>

                  <div className="p-4 bg-orange-50 border border-orange-200 rounded-lg">
                    <h4 className="font-medium text-orange-900 mb-2">
                      ⚠️ Safety Concerns
                    </h4>
                    <p className="text-sm text-orange-700 mb-3">
                      Escalating threats, stalking, or violation of protection
                      orders
                    </p>
                    <Button
                      variant="outline"
                      className="border-orange-300 text-orange-700 w-full"
                    >
                      Crisis Hotline: 1-800-799-7233
                    </Button>
                  </div>

                  <div className="p-4 bg-purple-50 border border-purple-200 rounded-lg">
                    <h4 className="font-medium text-purple-900 mb-2">
                      🏠 Housing Crisis
                    </h4>
                    <p className="text-sm text-purple-700 mb-3">
                      Immediate shelter needed, eviction, or unsafe housing
                    </p>
                    <Button
                      variant="outline"
                      className="border-purple-300 text-purple-700 w-full"
                    >
                      Emergency Shelter Network
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-blue-700">
                  Support Resources
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                    <h4 className="font-medium text-blue-900 mb-2">
                      📞 24/7 Crisis Line
                    </h4>
                    <p className="text-sm text-blue-700 mb-2">
                      National DV Hotline
                    </p>
                    <p className="font-mono text-lg text-blue-900">
                      1-800-799-7233
                    </p>
                  </div>

                  <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
                    <h4 className="font-medium text-green-900 mb-2">
                      🏥 Medical Emergency
                    </h4>
                    <p className="text-sm text-green-700 mb-2">
                      Injury, medical crisis, or immediate medical needs
                    </p>
                    <p className="font-mono text-lg text-green-900">
                      911 or Emergency Room
                    </p>
                  </div>

                  <div className="p-4 bg-indigo-50 border border-indigo-200 rounded-lg">
                    <h4 className="font-medium text-indigo-900 mb-2">
                      ⚖️ Legal Emergency
                    </h4>
                    <p className="text-sm text-indigo-700 mb-2">
                      Protection order violations, court issues
                    </p>
                    <p className="font-mono text-lg text-indigo-900">
                      Legal Aid: 1-800-LAW-HELP
                    </p>
                  </div>

                  <div className="p-4 bg-gray-50 border border-gray-200 rounded-lg">
                    <h4 className="font-medium text-gray-900 mb-2">
                      🧠 Mental Health Crisis
                    </h4>
                    <p className="text-sm text-gray-700 mb-2">
                      Suicide risk, severe mental health episode
                    </p>
                    <p className="font-mono text-lg text-gray-900">
                      988 Suicide & Crisis Lifeline
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}

export default function TriagePage() {
  return (
    <ProtectedRoute requiredRoles={["admin", "supervisor", "case_manager"]}>
      <AppLayout
        title="Triage Center"
        breadcrumbs={[
          { label: "Dashboard", href: "/dashboard" },
          { label: "Triage Center" },
        ]}
      >
        <div className="p-6">
          <TriageDashboardContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
