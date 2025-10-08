import { useState, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button, Badge, Modal, Textarea } from '@haven/ui';
import { 
  useServiceEpisode,
  useAddMilestone,
  useUpdateMilestone,
  type ServiceEpisode 
} from '@haven/api-client';

interface Milestone {
  id: string;
  title: string;
  description?: string;
  targetMinute: number;
  completed: boolean;
  completedAt?: string;
  notes?: string;
  type: 'duration' | 'task' | 'assessment' | 'custom';
}

interface ServiceMilestonesProps {
  episodeId: string;
  currentDuration: number;
  expectedDuration: number;
  isActive: boolean;
}

const DEFAULT_MILESTONES = {
  INDIVIDUAL_COUNSELING: [
    { title: 'Opening & Check-in', targetMinute: 5, type: 'task' },
    { title: 'Primary Topic Discussion', targetMinute: 15, type: 'task' },
    { title: 'Mid-session Assessment', targetMinute: 30, type: 'assessment' },
    { title: 'Skill Building/Intervention', targetMinute: 45, type: 'task' },
    { title: 'Session Summary & Planning', targetMinute: 55, type: 'task' },
  ],
  CRISIS_INTERVENTION: [
    { title: 'Safety Assessment', targetMinute: 2, type: 'assessment' },
    { title: 'Immediate Needs Identified', targetMinute: 5, type: 'task' },
    { title: 'Safety Plan Activated', targetMinute: 10, type: 'task' },
    { title: 'Resources Coordinated', targetMinute: 20, type: 'task' },
    { title: 'Follow-up Scheduled', targetMinute: 25, type: 'task' },
  ],
  CASE_MANAGEMENT: [
    { title: 'Current Status Review', targetMinute: 10, type: 'assessment' },
    { title: 'Goal Progress Check', targetMinute: 20, type: 'task' },
    { title: 'Resource Coordination', targetMinute: 35, type: 'task' },
    { title: 'Next Steps Planning', targetMinute: 50, type: 'task' },
  ],
  GROUP_COUNSELING: [
    { title: 'Group Opening & Check-ins', targetMinute: 10, type: 'task' },
    { title: 'Topic Introduction', targetMinute: 20, type: 'task' },
    { title: 'Group Discussion', targetMinute: 45, type: 'task' },
    { title: 'Skill Practice', targetMinute: 70, type: 'task' },
    { title: 'Closing & Assignments', targetMinute: 85, type: 'task' },
  ],
} as const;

export default function ServiceMilestones({ 
  episodeId, 
  currentDuration, 
  expectedDuration, 
  isActive 
}: ServiceMilestonesProps) {
  const { serviceEpisode, loading } = useServiceEpisode(episodeId);
  const { addMilestone, loading: addLoading } = useAddMilestone();
  const { updateMilestone, loading: updateLoading } = useUpdateMilestone();

  const [milestones, setMilestones] = useState<Milestone[]>([]);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showNotesModal, setShowNotesModal] = useState<string | null>(null);
  const [notes, setNotes] = useState('');

  // Initialize milestones based on service type
  useEffect(() => {
    if (serviceEpisode && !loading) {
      const serviceType = serviceEpisode.serviceType as keyof typeof DEFAULT_MILESTONES;
      const defaultMilestones = DEFAULT_MILESTONES[serviceType] || [];
      
      // Convert to full milestone objects
      const initialMilestones: Milestone[] = defaultMilestones.map((milestone, index) => ({
        id: `default-${index}`,
        title: milestone.title,
        targetMinute: milestone.targetMinute,
        completed: false,
        type: milestone.type as Milestone['type']
      }));

      // Add duration-based milestones
      const durationMilestones: Milestone[] = [
        { 
          id: 'duration-25', 
          title: '25% Complete', 
          targetMinute: Math.floor(expectedDuration * 0.25), 
          completed: false, 
          type: 'duration' 
        },
        { 
          id: 'duration-50', 
          title: '50% Complete', 
          targetMinute: Math.floor(expectedDuration * 0.5), 
          completed: false, 
          type: 'duration' 
        },
        { 
          id: 'duration-75', 
          title: '75% Complete', 
          targetMinute: Math.floor(expectedDuration * 0.75), 
          completed: false, 
          type: 'duration' 
        },
        { 
          id: 'duration-100', 
          title: 'Expected Duration Reached', 
          targetMinute: expectedDuration, 
          completed: false, 
          type: 'duration' 
        },
      ];

      setMilestones([...initialMilestones, ...durationMilestones].sort((a, b) => a.targetMinute - b.targetMinute));
    }
  }, [serviceEpisode, expectedDuration, loading]);

  // Auto-complete milestones based on current duration
  useEffect(() => {
    setMilestones(prev => prev.map(milestone => {
      if (!milestone.completed && currentDuration >= milestone.targetMinute) {
        return { ...milestone, completed: true, completedAt: new Date().toISOString() };
      }
      return milestone;
    }));
  }, [currentDuration]);

  const handleToggleMilestone = async (milestoneId: string) => {
    const milestone = milestones.find(m => m.id === milestoneId);
    if (!milestone) return;

    try {
      const updatedMilestone = {
        ...milestone,
        completed: !milestone.completed,
        completedAt: !milestone.completed ? new Date().toISOString() : undefined
      };

      await updateMilestone(episodeId, milestoneId, updatedMilestone);
      
      setMilestones(prev => prev.map(m => 
        m.id === milestoneId ? updatedMilestone : m
      ));
    } catch (error) {
      console.error('Failed to update milestone:', error);
    }
  };

  const handleAddNotes = async (milestoneId: string) => {
    if (!notes.trim()) return;

    try {
      await updateMilestone(episodeId, milestoneId, { notes: notes.trim() });
      
      setMilestones(prev => prev.map(m => 
        m.id === milestoneId ? { ...m, notes: notes.trim() } : m
      ));
      
      setShowNotesModal(null);
      setNotes('');
    } catch (error) {
      console.error('Failed to add notes:', error);
    }
  };

  const getMilestoneIcon = (type: Milestone['type'], completed: boolean) => {
    const baseClasses = "w-4 h-4";
    const color = completed ? "text-green-600" : "text-secondary-400";
    
    switch (type) {
      case 'assessment':
        return (
          <svg className={`${baseClasses} ${color}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        );
      case 'task':
        return (
          <svg className={`${baseClasses} ${color}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
          </svg>
        );
      case 'duration':
        return (
          <svg className={`${baseClasses} ${color}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        );
      default:
        return (
          <svg className={`${baseClasses} ${color}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        );
    }
  };

  const getTimeStatus = (targetMinute: number) => {
    if (currentDuration >= targetMinute) {
      return { status: 'reached', color: 'text-green-600', label: 'Reached' };
    } else if (currentDuration >= targetMinute - 5) {
      return { status: 'approaching', color: 'text-yellow-600', label: 'Approaching' };
    } else {
      return { status: 'upcoming', color: 'text-secondary-500', label: `In ${targetMinute - currentDuration}min` };
    }
  };

  const completedMilestones = milestones.filter(m => m.completed).length;
  const totalMilestones = milestones.length;
  const progressPercent = totalMilestones > 0 ? (completedMilestones / totalMilestones) * 100 : 0;

  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-center">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Service Milestones</CardTitle>
            <div className="flex items-center space-x-2">
              <Badge variant="outline">
                {completedMilestones}/{totalMilestones}
              </Badge>
              <span className="text-sm text-secondary-600">
                {Math.round(progressPercent)}%
              </span>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {/* Progress Bar */}
          <div className="mb-6">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-secondary-700">Overall Progress</span>
              <span className="text-sm text-secondary-600">{Math.round(progressPercent)}%</span>
            </div>
            <div className="w-full bg-secondary-200 rounded-full h-2">
              <div 
                className="bg-green-500 h-2 rounded-full transition-all duration-300"
                style={{ width: `${progressPercent}%` }}
              ></div>
            </div>
          </div>

          {/* Milestone List */}
          <div className="space-y-3">
            {milestones.map((milestone) => {
              const timeStatus = getTimeStatus(milestone.targetMinute);
              return (
                <div 
                  key={milestone.id} 
                  className={`border rounded-lg p-3 transition-all ${
                    milestone.completed 
                      ? 'border-green-200 bg-green-50' 
                      : timeStatus.status === 'approaching'
                      ? 'border-yellow-200 bg-yellow-50'
                      : 'border-secondary-200 bg-white'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <button 
                        onClick={() => handleToggleMilestone(milestone.id)}
                        disabled={!isActive}
                        className="p-1 rounded hover:bg-secondary-100 disabled:opacity-50"
                      >
                        {getMilestoneIcon(milestone.type, milestone.completed)}
                      </button>
                      <div>
                        <h4 className={`font-medium ${milestone.completed ? 'text-green-900 line-through' : 'text-secondary-900'}`}>
                          {milestone.title}
                        </h4>
                        <div className="flex items-center space-x-3 text-sm">
                          <span className="text-secondary-600">
                            Target: {milestone.targetMinute}min
                          </span>
                          <span className={timeStatus.color}>
                            {timeStatus.label}
                          </span>
                          {milestone.type !== 'duration' && (
                            <Badge variant="ghost" className="text-xs">
                              {milestone.type}
                            </Badge>
                          )}
                        </div>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      {milestone.completed && milestone.completedAt && (
                        <span className="text-xs text-green-600">
                          {new Date(milestone.completedAt).toLocaleTimeString()}
                        </span>
                      )}
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          setShowNotesModal(milestone.id);
                          setNotes(milestone.notes || '');
                        }}
                        className="text-xs px-2 py-1"
                      >
                        {milestone.notes ? 'Edit Notes' : 'Add Notes'}
                      </Button>
                    </div>
                  </div>
                  
                  {milestone.description && (
                    <p className="text-sm text-secondary-600 mt-2 ml-7">
                      {milestone.description}
                    </p>
                  )}
                  
                  {milestone.notes && (
                    <div className="mt-2 ml-7 p-2 bg-blue-50 border border-blue-200 rounded">
                      <p className="text-sm text-blue-800">{milestone.notes}</p>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {/* Add Custom Milestone */}
          {isActive && (
            <div className="mt-4 pt-4 border-t border-secondary-200">
              <Button 
                variant="outline" 
                onClick={() => setShowAddModal(true)}
                className="w-full"
              >
                <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                </svg>
                Add Custom Milestone
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Notes Modal */}
      <Modal isOpen={showNotesModal !== null} onClose={() => setShowNotesModal(null)}>
        <div className="p-6">
          <h3 className="text-lg font-medium text-secondary-900 mb-4">
            Milestone Notes
          </h3>
          
          <div className="space-y-4">
            <Textarea
              placeholder="Add notes about this milestone..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={4}
            />
          </div>

          <div className="flex items-center justify-end space-x-3 mt-6">
            <Button variant="outline" onClick={() => setShowNotesModal(null)}>
              Cancel
            </Button>
            <Button 
              onClick={() => showNotesModal && handleAddNotes(showNotesModal)} 
              loading={updateLoading}
            >
              Save Notes
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}