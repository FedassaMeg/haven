import React, { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button, Badge, Dialog, DialogContent, DialogHeader, DialogTitle, Input, Textarea, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@haven/ui';
import { ServiceEpisode, ServiceCompletionStatus } from '@haven/api-client';
import { format, formatDistanceToNow } from 'date-fns';

interface ServiceEpisodeListProps {
  serviceEpisodes: ServiceEpisode[];
  onStartService: (episodeId: string, location: string) => void;
  onCompleteService: (episodeId: string, outcome: string, notes: string) => void;
  onUpdateOutcome: (episodeId: string, outcome: string, followUpRequired?: string, followUpDate?: string) => void;
  loading: boolean;
  error?: any;
}

interface ActionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: any) => void;
  title: string;
  children: React.ReactNode;
  loading?: boolean;
}

function ActionModal({ isOpen, onClose, onSubmit, title, children, loading }: ActionModalProps) {
  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="space-y-4">
          {children}
          <div className="flex justify-end space-x-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              Submit
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function ServiceEpisodeList({
  serviceEpisodes,
  onStartService,
  onCompleteService,
  onUpdateOutcome,
  loading,
  error
}: ServiceEpisodeListProps) {
  const [startModalOpen, setStartModalOpen] = useState(false);
  const [completeModalOpen, setCompleteModalOpen] = useState(false);
  const [outcomeModalOpen, setOutcomeModalOpen] = useState(false);
  const [selectedEpisode, setSelectedEpisode] = useState<ServiceEpisode | null>(null);
  const [startLocation, setStartLocation] = useState('');
  const [outcome, setOutcome] = useState('');
  const [notes, setNotes] = useState('');
  const [followUpRequired, setFollowUpRequired] = useState('');
  const [followUpDate, setFollowUpDate] = useState('');

  const getStatusBadge = (status: ServiceCompletionStatus) => {
    const badgeConfig = {
      SCHEDULED: { color: 'bg-blue-100 text-blue-800', label: 'Scheduled' },
      IN_PROGRESS: { color: 'bg-yellow-100 text-yellow-800', label: 'In Progress' },
      COMPLETED: { color: 'bg-green-100 text-green-800', label: 'Completed' },
      PARTIALLY_COMPLETED: { color: 'bg-orange-100 text-orange-800', label: 'Partial' },
      CANCELLED: { color: 'bg-red-100 text-red-800', label: 'Cancelled' },
      NO_SHOW: { color: 'bg-gray-100 text-gray-800', label: 'No Show' },
      POSTPONED: { color: 'bg-purple-100 text-purple-800', label: 'Postponed' },
    };

    const config = badgeConfig[status] || badgeConfig.SCHEDULED;
    return (
      <Badge className={`${config.color} text-xs`}>
        {config.label}
      </Badge>
    );
  };

  const getDurationDisplay = (planned: number | null, actual: number | null) => {
    if (actual) {
      return `${actual}min (actual)`;
    }
    if (planned) {
      return `${planned}min (planned)`;
    }
    return 'Not specified';
  };

  const handleStartService = (episode: ServiceEpisode) => {
    setSelectedEpisode(episode);
    setStartLocation('');
    setStartModalOpen(true);
  };

  const handleCompleteService = (episode: ServiceEpisode) => {
    setSelectedEpisode(episode);
    setOutcome('');
    setNotes('');
    setCompleteModalOpen(true);
  };

  const handleUpdateOutcome = (episode: ServiceEpisode) => {
    setSelectedEpisode(episode);
    setOutcome(episode.serviceOutcome || '');
    setFollowUpRequired(episode.followUpRequired || '');
    setFollowUpDate(episode.followUpDate || '');
    setOutcomeModalOpen(true);
  };

  const submitStartService = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedEpisode) {
      onStartService(selectedEpisode.episodeId, startLocation);
      setStartModalOpen(false);
    }
  };

  const submitCompleteService = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedEpisode) {
      onCompleteService(selectedEpisode.episodeId, outcome, notes);
      setCompleteModalOpen(false);
    }
  };

  const submitUpdateOutcome = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedEpisode) {
      onUpdateOutcome(selectedEpisode.episodeId, outcome, followUpRequired, followUpDate);
      setOutcomeModalOpen(false);
    }
  };

  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-center">Loading service episodes...</div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-center text-red-600">Error loading service episodes</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {serviceEpisodes.length === 0 ? (
        <Card>
          <CardContent className="p-6">
            <div className="text-center text-slate-600">
              No service episodes found. Create the first service episode to get started.
            </div>
          </CardContent>
        </Card>
      ) : (
        serviceEpisodes.map((episode) => (
          <Card key={episode.episodeId} className="hover:shadow-md transition-shadow">
            <CardHeader className="pb-3">
              <div className="flex items-start justify-between">
                <div>
                  <CardTitle className="text-lg">
                    {episode.serviceType}
                    {episode.isConfidential && (
                      <Badge className="ml-2 bg-red-100 text-red-800 text-xs">
                        Confidential
                      </Badge>
                    )}
                  </CardTitle>
                  <div className="text-sm text-slate-600 mt-1">
                    {episode.programName} • {episode.deliveryMode}
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  {getStatusBadge(episode.completionStatus)}
                  {episode.isCourtOrdered && (
                    <Badge className="bg-purple-100 text-purple-800 text-xs">
                      Court Ordered
                    </Badge>
                  )}
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-4">
                <div>
                  <div className="text-sm font-medium text-slate-700">Service Date</div>
                  <div className="text-sm text-slate-600">
                    {format(new Date(episode.serviceDate), 'MMM d, yyyy')}
                  </div>
                </div>
                <div>
                  <div className="text-sm font-medium text-slate-700">Duration</div>
                  <div className="text-sm text-slate-600">
                    {getDurationDisplay(episode.plannedDurationMinutes, episode.actualDurationMinutes)}
                  </div>
                </div>
                <div>
                  <div className="text-sm font-medium text-slate-700">Provider</div>
                  <div className="text-sm text-slate-600">{episode.primaryProviderName}</div>
                </div>
                <div>
                  <div className="text-sm font-medium text-slate-700">Funding Source</div>
                  <div className="text-sm text-slate-600">{episode.primaryFundingSource?.funderName}</div>
                </div>
                {episode.startTime && (
                  <div>
                    <div className="text-sm font-medium text-slate-700">Started</div>
                    <div className="text-sm text-slate-600">
                      {format(new Date(episode.startTime), 'h:mm a')}
                    </div>
                  </div>
                )}
                {episode.endTime && (
                  <div>
                    <div className="text-sm font-medium text-slate-700">Completed</div>
                    <div className="text-sm text-slate-600">
                      {format(new Date(episode.endTime), 'h:mm a')}
                    </div>
                  </div>
                )}
              </div>

              {episode.serviceDescription && (
                <div className="mb-4">
                  <div className="text-sm font-medium text-slate-700 mb-1">Description</div>
                  <div className="text-sm text-slate-600">{episode.serviceDescription}</div>
                </div>
              )}

              {episode.serviceOutcome && (
                <div className="mb-4">
                  <div className="text-sm font-medium text-slate-700 mb-1">Outcome</div>
                  <div className="text-sm text-slate-600">{episode.serviceOutcome}</div>
                </div>
              )}

              {episode.notes && (
                <div className="mb-4">
                  <div className="text-sm font-medium text-slate-700 mb-1">Notes</div>
                  <div className="text-sm text-slate-600">{episode.notes}</div>
                </div>
              )}

              {episode.followUpRequired && (
                <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <div className="text-sm font-medium text-yellow-800 mb-1">Follow-up Required</div>
                  <div className="text-sm text-yellow-700">{episode.followUpRequired}</div>
                  {episode.followUpDate && (
                    <div className="text-sm text-yellow-700 mt-1">
                      Due: {format(new Date(episode.followUpDate), 'MMM d, yyyy')}
                    </div>
                  )}
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex space-x-2 pt-4 border-t">
                {episode.completionStatus === 'SCHEDULED' && (
                  <Button
                    size="sm"
                    onClick={() => handleStartService(episode)}
                    className="bg-green-600 hover:bg-green-700"
                  >
                    Start Service
                  </Button>
                )}
                {episode.completionStatus === 'IN_PROGRESS' && (
                  <Button
                    size="sm"
                    onClick={() => handleCompleteService(episode)}
                    className="bg-blue-600 hover:bg-blue-700"
                  >
                    Complete Service
                  </Button>
                )}
                {episode.completionStatus === 'COMPLETED' && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleUpdateOutcome(episode)}
                  >
                    Update Outcome
                  </Button>
                )}
              </div>
            </CardContent>
          </Card>
        ))
      )}

      {/* Start Service Modal */}
      <ActionModal
        isOpen={startModalOpen}
        onClose={() => setStartModalOpen(false)}
        onSubmit={submitStartService}
        title="Start Service Session"
      >
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Service Location
          </label>
          <Input
            value={startLocation}
            onChange={(e) => setStartLocation(e.target.value)}
            placeholder="Enter service location"
            required
          />
        </div>
      </ActionModal>

      {/* Complete Service Modal */}
      <ActionModal
        isOpen={completeModalOpen}
        onClose={() => setCompleteModalOpen(false)}
        onSubmit={submitCompleteService}
        title="Complete Service Session"
      >
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Service Outcome
          </label>
          <Textarea
            value={outcome}
            onChange={(e) => setOutcome(e.target.value)}
            placeholder="Describe the service outcome and client progress"
            rows={3}
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Additional Notes
          </label>
          <Textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Any additional notes about the service session"
            rows={2}
          />
        </div>
      </ActionModal>

      {/* Update Outcome Modal */}
      <ActionModal
        isOpen={outcomeModalOpen}
        onClose={() => setOutcomeModalOpen(false)}
        onSubmit={submitUpdateOutcome}
        title="Update Service Outcome"
      >
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Service Outcome
          </label>
          <Textarea
            value={outcome}
            onChange={(e) => setOutcome(e.target.value)}
            placeholder="Describe the service outcome and client progress"
            rows={3}
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Follow-up Required
          </label>
          <Textarea
            value={followUpRequired}
            onChange={(e) => setFollowUpRequired(e.target.value)}
            placeholder="Describe any follow-up actions needed"
            rows={2}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Follow-up Date
          </label>
          <Input
            type="date"
            value={followUpDate}
            onChange={(e) => setFollowUpDate(e.target.value)}
          />
        </div>
      </ActionModal>
    </div>
  );
}