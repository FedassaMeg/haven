import React, { useState } from 'react';
import {
  Button,
  Input,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Textarea
} from '@haven/ui';

interface ProjectLinkageFormProps {
  initialData?: any;
  onSubmit: (data: any) => Promise<void>;
  onCancel: () => void;
  isEditing?: boolean;
}

export function ProjectLinkageForm({
  initialData,
  onSubmit,
  onCancel,
  isEditing = false
}: ProjectLinkageFormProps) {
  const [formData, setFormData] = useState({
    thProjectId: initialData?.thProjectId || '',
    rrhProjectId: initialData?.rrhProjectId || '',
    linkageEffectiveDate: initialData?.linkageEffectiveDate || '',
    linkageReason: initialData?.linkageReason || '',
    linkageNotes: initialData?.linkageNotes || '',
  });

  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await onSubmit(formData);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="thProjectId">TH Project ID</Label>
        <Input
          id="thProjectId"
          value={formData.thProjectId}
          onChange={(e) => setFormData({ ...formData, thProjectId: e.target.value })}
          required
          disabled={isEditing}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="rrhProjectId">RRH Project ID</Label>
        <Input
          id="rrhProjectId"
          value={formData.rrhProjectId}
          onChange={(e) => setFormData({ ...formData, rrhProjectId: e.target.value })}
          required
          disabled={isEditing}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="linkageEffectiveDate">Effective Date</Label>
        <Input
          id="linkageEffectiveDate"
          type="date"
          value={formData.linkageEffectiveDate}
          onChange={(e) => setFormData({ ...formData, linkageEffectiveDate: e.target.value })}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="linkageReason">Linkage Reason</Label>
        <Input
          id="linkageReason"
          value={formData.linkageReason}
          onChange={(e) => setFormData({ ...formData, linkageReason: e.target.value })}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="linkageNotes">Notes (Optional)</Label>
        <Textarea
          id="linkageNotes"
          value={formData.linkageNotes}
          onChange={(e) => setFormData({ ...formData, linkageNotes: e.target.value })}
          rows={3}
        />
      </div>

      <div className="flex gap-2 justify-end">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Saving...' : isEditing ? 'Update Linkage' : 'Create Linkage'}
        </Button>
      </div>
    </form>
  );
}
