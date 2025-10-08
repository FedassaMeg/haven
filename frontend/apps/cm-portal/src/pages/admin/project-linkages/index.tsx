import React, { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Button,
  Input,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Badge,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  Alert,
  AlertDescription,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@haven/ui/src/components";
import {
  Plus,
  Edit,
  Trash2,
  Search,
  AlertTriangle,
  Clock,
  CheckCircle,
  XCircle,
  Link,
  Unlink,
} from "lucide-react";
import { ProjectLinkageForm } from "../../../components/ProjectLinkageForm";
import { DataQualityDashboard } from "../../../components/DataQualityDashboard";
import { AuditTrailView } from "../../../components/AuditTrailView";
import { projectLinkageApi } from "@haven/api-client/src/project-linkage-api";
import { toast } from "sonner";

interface ProjectLinkage {
  linkageId: string;
  thProjectId: string;
  rrhProjectId: string;
  thHudProjectId: string;
  rrhHudProjectId: string;
  thProjectName: string;
  rrhProjectName: string;
  linkageEffectiveDate: string;
  linkageEndDate?: string;
  status: "ACTIVE" | "REVOKED" | "EXPIRED";
  linkageReason: string;
  linkageNotes?: string;
  createdBy: string;
  lastModifiedBy: string;
  createdAt: string;
  lastModifiedAt: string;
}

export default function ProjectLinkagesPage() {
  const [linkages, setLinkages] = useState<ProjectLinkage[]>([]);
  const [filteredLinkages, setFilteredLinkages] = useState<ProjectLinkage[]>(
    []
  );
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [selectedLinkage, setSelectedLinkage] = useState<ProjectLinkage | null>(
    null
  );
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showEditForm, setShowEditForm] = useState(false);
  const [showAuditTrail, setShowAuditTrail] = useState(false);

  useEffect(() => {
    loadLinkages();
  }, []);

  useEffect(() => {
    filterLinkages();
  }, [linkages, searchTerm, statusFilter]);

  const loadLinkages = async () => {
    try {
      setLoading(true);
      // In a real implementation, this would load all linkages
      // For now, we'll use placeholder data
      const mockLinkages: ProjectLinkage[] = [
        {
          linkageId: "1",
          thProjectId: "th-1",
          rrhProjectId: "rrh-1",
          thHudProjectId: "TH-2024-001",
          rrhHudProjectId: "RRH-2024-001",
          thProjectName: "Downtown Transitional Housing",
          rrhProjectName: "City Rapid Rehousing Program",
          linkageEffectiveDate: "2024-01-01",
          status: "ACTIVE",
          linkageReason: "Coordinated continuum of care",
          linkageNotes: "Joint program serving families with children",
          createdBy: "Program Manager",
          lastModifiedBy: "Program Manager",
          createdAt: "2024-01-01T08:00:00Z",
          lastModifiedAt: "2024-01-01T08:00:00Z",
        },
      ];
      setLinkages(mockLinkages);
    } catch (error) {
      console.error("Failed to load linkages:", error);
      toast("Failed to load project linkages");
    } finally {
      setLoading(false);
    }
  };

  const filterLinkages = () => {
    let filtered = linkages;

    // Apply search filter
    if (searchTerm) {
      filtered = filtered.filter(
        (linkage) =>
          linkage.thProjectName
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          linkage.rrhProjectName
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          linkage.thHudProjectId
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          linkage.rrhHudProjectId
            .toLowerCase()
            .includes(searchTerm.toLowerCase())
      );
    }

    // Apply status filter
    if (statusFilter !== "all") {
      filtered = filtered.filter((linkage) => linkage.status === statusFilter);
    }

    setFilteredLinkages(filtered);
  };

  const handleCreateLinkage = async (linkageData: any) => {
    try {
      await projectLinkageApi.createLinkage(linkageData);
      toast("Project linkage created successfully");
      setShowCreateForm(false);
      loadLinkages();
    } catch (error) {
      console.error("Failed to create linkage:", error);
      toast("Failed to create project linkage");
    }
  };

  const handleEditLinkage = async (linkageData: any) => {
    if (!selectedLinkage) return;

    try {
      await projectLinkageApi.modifyLinkage(
        selectedLinkage.linkageId,
        linkageData
      );
      toast("Project linkage updated successfully");
      setShowEditForm(false);
      setSelectedLinkage(null);
      loadLinkages();
    } catch (error) {
      console.error("Failed to update linkage:", error);
      toast("Failed to update project linkage");
    }
  };

  const handleRevokeLinkage = async (linkage: ProjectLinkage) => {
    if (!confirm("Are you sure you want to revoke this linkage?")) return;

    try {
      const revocationData = {
        revocationDate: new Date().toISOString().split("T")[0],
        revocationReason: "Manual revocation by administrator",
      };

      await projectLinkageApi.revokeLinkage(linkage.linkageId, revocationData);
      toast("Project linkage revoked successfully");
      loadLinkages();
    } catch (error) {
      console.error("Failed to revoke linkage:", error);
      toast("Failed to revoke project linkage");
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case "ACTIVE":
        return <CheckCircle className="h-4 w-4 text-green-600" />;
      case "REVOKED":
        return <XCircle className="h-4 w-4 text-red-600" />;
      case "EXPIRED":
        return <Clock className="h-4 w-4 text-yellow-600" />;
      default:
        return <AlertTriangle className="h-4 w-4 text-gray-600" />;
    }
  };

  const getStatusBadge = (status: string) => {
    const variants = {
      ACTIVE: "default",
      REVOKED: "destructive",
      EXPIRED: "secondary",
    } as const;

    return (
      <Badge variant={variants[status as keyof typeof variants] || "outline"}>
        {getStatusIcon(status)}
        <span className="ml-1">{status}</span>
      </Badge>
    );
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Project Linkages</h1>
          <p className="text-muted-foreground">
            Manage TH/RRH project linkages and data quality monitoring
          </p>
        </div>
        <Dialog open={showCreateForm} onOpenChange={setShowCreateForm}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="h-4 w-4 mr-2" />
              Create Linkage
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>Create Project Linkage</DialogTitle>
            </DialogHeader>
            <ProjectLinkageForm
              onSubmit={handleCreateLinkage}
              onCancel={() => setShowCreateForm(false)}
            />
          </DialogContent>
        </Dialog>
      </div>

      <Tabs defaultValue="linkages" className="space-y-4">
        <TabsList>
          <TabsTrigger value="linkages">
            <Link className="h-4 w-4 mr-2" />
            Linkages
          </TabsTrigger>
          <TabsTrigger value="dashboard">
            <AlertTriangle className="h-4 w-4 mr-2" />
            Data Quality
          </TabsTrigger>
          <TabsTrigger value="audit">
            <Clock className="h-4 w-4 mr-2" />
            Audit Trail
          </TabsTrigger>
        </TabsList>

        <TabsContent value="linkages" className="space-y-4">
          {/* Search and Filter Controls */}
          <Card>
            <CardHeader>
              <CardTitle>Filters</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex gap-4">
                <div className="flex-1">
                  <div className="relative">
                    <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                    <Input
                      placeholder="Search projects..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="pl-8"
                    />
                  </div>
                </div>
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="px-3 py-2 border border-input bg-background rounded-md"
                >
                  <option value="all">All Statuses</option>
                  <option value="ACTIVE">Active</option>
                  <option value="REVOKED">Revoked</option>
                  <option value="EXPIRED">Expired</option>
                </select>
              </div>
            </CardContent>
          </Card>

          {/* Linkages Table */}
          <Card>
            <CardHeader>
              <CardTitle>
                Project Linkages ({filteredLinkages.length})
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>TH Project</TableHead>
                    <TableHead>RRH Project</TableHead>
                    <TableHead>HUD IDs</TableHead>
                    <TableHead>Effective Date</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Created By</TableHead>
                    <TableHead>Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredLinkages.map((linkage) => (
                    <TableRow key={linkage.linkageId}>
                      <TableCell>
                        <div className="font-medium">
                          {linkage.thProjectName}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {linkage.thProjectId}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="font-medium">
                          {linkage.rrhProjectName}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {linkage.rrhProjectId}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="text-sm">{linkage.thHudProjectId}</div>
                        <div className="text-sm">{linkage.rrhHudProjectId}</div>
                      </TableCell>
                      <TableCell>
                        {new Date(
                          linkage.linkageEffectiveDate
                        ).toLocaleDateString()}
                        {linkage.linkageEndDate && (
                          <div className="text-sm text-muted-foreground">
                            End:{" "}
                            {new Date(
                              linkage.linkageEndDate
                            ).toLocaleDateString()}
                          </div>
                        )}
                      </TableCell>
                      <TableCell>{getStatusBadge(linkage.status)}</TableCell>
                      <TableCell>{linkage.createdBy}</TableCell>
                      <TableCell>
                        <div className="flex gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setSelectedLinkage(linkage);
                              setShowEditForm(true);
                            }}
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                          {linkage.status === "ACTIVE" && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleRevokeLinkage(linkage)}
                            >
                              <Unlink className="h-4 w-4" />
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {filteredLinkages.length === 0 && (
                <div className="text-center py-8 text-muted-foreground">
                  No project linkages found
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="dashboard">
          <DataQualityDashboard />
        </TabsContent>

        <TabsContent value="audit">
          <AuditTrailView />
        </TabsContent>
      </Tabs>

      {/* Edit Linkage Dialog */}
      <Dialog open={showEditForm} onOpenChange={setShowEditForm}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Edit Project Linkage</DialogTitle>
          </DialogHeader>
          {selectedLinkage && (
            <ProjectLinkageForm
              initialData={selectedLinkage}
              onSubmit={handleEditLinkage}
              onCancel={() => {
                setShowEditForm(false);
                setSelectedLinkage(null);
              }}
              isEditing
            />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
