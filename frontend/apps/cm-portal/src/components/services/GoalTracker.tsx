import { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Textarea, Progress } from '@haven/ui';
import type { Goal, GoalCategory, GoalStatus, Priority } from '@haven/api-client/src/types/services';

interface GoalTrackerProps {
  clientId: string;
  goals: Goal[];
  onAddGoal: () => void;
  showAddDialog: boolean;
  onCloseDialog: () => void;
}

const GoalTracker: React.FC<GoalTrackerProps> = ({
  clientId,
  goals,
  onAddGoal,
  showAddDialog,
  onCloseDialog
}) => {
  const [newGoal, setNewGoal] = useState<Partial<Goal>>({
    clientId,
    category: 'HOUSING_STABILITY' as GoalCategory,
    status: 'NOT_STARTED' as GoalStatus,
    priority: 'MEDIUM' as Priority,
    progress: 0,
    title: '',
    description: '',
    targetDate: ''
  });

  const handleSubmit = async () => {
    // API call to create goal
    console.log('Creating goal:', newGoal);
    onCloseDialog();
  };

  const getStatusColor = (status: GoalStatus) => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'IN_PROGRESS':
        return 'primary';
      case 'AT_RISK':
        return 'destructive';
      case 'ON_HOLD':
        return 'warning';
      default:
        return 'secondary';
    }
  };

  const getPriorityIcon = (priority: Priority) => {
    switch (priority) {
      case 'URGENT':
        return <svg className="w-3 h-3 text-red-600" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
        </svg>;
      case 'HIGH':
        return <svg className="w-3 h-3 text-orange-600" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
        </svg>;
      default:
        return null;
    }
  };

  const getCategoryIcon = (category: GoalCategory) => {
    switch (category) {
      case 'HOUSING_STABILITY':
        return '🏠';
      case 'EMPLOYMENT':
        return '💼';
      case 'EDUCATION':
        return '📚';
      case 'FINANCIAL_STABILITY':
        return '💰';
      case 'LEGAL':
        return '⚖️';
      case 'HEALTH_WELLNESS':
        return '🏥';
      case 'MENTAL_HEALTH':
        return '🧠';
      case 'SAFETY':
        return '🛡️';
      default:
        return '🎯';
    }
  };

  // Mock data for demonstration
  const mockGoals: Goal[] = [
    {
      id: '1',
      clientId,
      category: 'HOUSING_STABILITY' as GoalCategory,
      title: 'Secure lease',
      description: 'Find and secure a 2-bedroom apartment in a safe neighborhood',
      targetDate: '2024-06-15',
      status: 'IN_PROGRESS' as GoalStatus,
      progress: 40,
      priority: 'HIGH' as Priority,
      assignedTo: 'Sarah Johnson',
      createdAt: '2024-04-01T00:00:00Z',
      updatedAt: '2024-05-10T00:00:00Z',
      milestones: [
        {
          id: 'm1',
          goalId: '1',
          title: 'Complete housing applications',
          targetDate: '2024-05-15',
          isCompleted: true,
          completedDate: '2024-05-10'
        },
        {
          id: 'm2',
          goalId: '1',
          title: 'View properties',
          targetDate: '2024-05-20',
          isCompleted: false
        },
        {
          id: 'm3',
          goalId: '1',
          title: 'Submit RFTA packet',
          targetDate: '2024-06-01',
          isCompleted: false
        }
      ]
    },
    {
      id: '2',
      clientId,
      category: 'EMPLOYMENT' as GoalCategory,
      title: 'Resume + apply',
      description: 'Update resume and apply to at least 10 positions',
      targetDate: '2024-07-01',
      status: 'NOT_STARTED' as GoalStatus,
      progress: 20,
      priority: 'MEDIUM' as Priority,
      assignedTo: 'Mike Rodriguez',
      createdAt: '2024-04-15T00:00:00Z',
      updatedAt: '2024-04-15T00:00:00Z'
    }
  ];

  const displayGoals = goals.length > 0 ? goals : mockGoals;

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Goal Tracker</CardTitle>
            <Button size="sm" variant="ghost" onClick={onAddGoal}>
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {displayGoals.map((goal) => (
              <div key={goal.id} className="border border-secondary-200 rounded-lg p-3">
                <div className="flex items-start justify-between mb-2">
                  <div className="flex items-start space-x-2">
                    <span className="text-lg">{getCategoryIcon(goal.category)}</span>
                    <div className="flex-1">
                      <div className="flex items-center space-x-2">
                        <h4 className="text-sm font-medium text-secondary-900">{goal.title}</h4>
                        {getPriorityIcon(goal.priority)}
                      </div>
                      <p className="text-xs text-secondary-600 mt-0.5">
                        Target: {new Date(goal.targetDate).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                  <Badge variant={getStatusColor(goal.status)} size="sm">
                    {goal.status.replace(/_/g, ' ')}
                  </Badge>
                </div>
                
                <div className="mb-2">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs text-secondary-600">Progress</span>
                    <span className="text-xs font-medium text-secondary-900">{goal.progress}%</span>
                  </div>
                  <Progress value={goal.progress} className="h-2" />
                </div>
                
                {goal.milestones && goal.milestones.length > 0 && (
                  <div className="mt-3 pt-3 border-t border-secondary-100">
                    <p className="text-xs font-medium text-secondary-700 mb-2">Milestones</p>
                    <div className="space-y-1">
                      {goal.milestones.map((milestone) => (
                        <div key={milestone.id} className="flex items-center space-x-2">
                          <input
                            type="checkbox"
                            checked={milestone.isCompleted}
                            readOnly
                            className="w-3 h-3 rounded border-secondary-300"
                          />
                          <span className={`text-xs ${milestone.isCompleted ? 'line-through text-secondary-500' : 'text-secondary-700'}`}>
                            {milestone.title}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                
                <div className="mt-3 flex items-center justify-between">
                  <span className="text-xs text-secondary-500">
                    Assigned: {goal.assignedTo}
                  </span>
                  <Button size="sm" variant="ghost">
                    <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </Button>
                </div>
              </div>
            ))}
          </div>
          
          {displayGoals.length === 0 && (
            <div className="text-center py-6">
              <svg className="w-10 h-10 mx-auto mb-3 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p className="text-sm text-secondary-600">No goals set</p>
              <Button onClick={onAddGoal} className="mt-3" size="sm">
                Set First Goal
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add Goal Dialog */}
      <Dialog open={showAddDialog} onOpenChange={onCloseDialog}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Add New Goal</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div>
              <Label htmlFor="title">Goal Title</Label>
              <Input
                id="title"
                placeholder="e.g., Secure stable housing"
                value={newGoal.title}
                onChange={(e) => setNewGoal({ ...newGoal, title: e.target.value })}
              />
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="category">Category</Label>
                <Select 
                  value={newGoal.category}
                  onValueChange={(value) => setNewGoal({ ...newGoal, category: value as GoalCategory })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select category" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="HOUSING_STABILITY">Housing Stability</SelectItem>
                    <SelectItem value="EMPLOYMENT">Employment</SelectItem>
                    <SelectItem value="EDUCATION">Education</SelectItem>
                    <SelectItem value="FINANCIAL_STABILITY">Financial Stability</SelectItem>
                    <SelectItem value="LEGAL">Legal</SelectItem>
                    <SelectItem value="HEALTH_WELLNESS">Health & Wellness</SelectItem>
                    <SelectItem value="MENTAL_HEALTH">Mental Health</SelectItem>
                    <SelectItem value="SAFETY">Safety</SelectItem>
                    <SelectItem value="BENEFITS">Benefits</SelectItem>
                    <SelectItem value="LIFE_SKILLS">Life Skills</SelectItem>
                    <SelectItem value="OTHER">Other</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="priority">Priority</Label>
                <Select 
                  value={newGoal.priority}
                  onValueChange={(value) => setNewGoal({ ...newGoal, priority: value as Priority })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select priority" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LOW">Low</SelectItem>
                    <SelectItem value="MEDIUM">Medium</SelectItem>
                    <SelectItem value="HIGH">High</SelectItem>
                    <SelectItem value="URGENT">Urgent</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="targetDate">Target Date</Label>
                <Input
                  id="targetDate"
                  type="date"
                  value={newGoal.targetDate}
                  onChange={(e) => setNewGoal({ ...newGoal, targetDate: e.target.value })}
                />
              </div>
              <div>
                <Label htmlFor="status">Initial Status</Label>
                <Select 
                  value={newGoal.status}
                  onValueChange={(value) => setNewGoal({ ...newGoal, status: value as GoalStatus })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="NOT_STARTED">Not Started</SelectItem>
                    <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                    <SelectItem value="ON_HOLD">On Hold</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            
            <div>
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                placeholder="Provide details about this goal..."
                value={newGoal.description}
                onChange={(e) => setNewGoal({ ...newGoal, description: e.target.value })}
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={onCloseDialog}>Cancel</Button>
            <Button onClick={handleSubmit}>Create Goal</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default GoalTracker;