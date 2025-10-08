import React, { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  Button,
  Textarea,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Label,
  Card,
  CardContent
} from '@haven/ui';

interface UnsealNoteModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUnseal: (unsealReason: string, legalBasis: string) => void;
  loading?: boolean;
}

export default function UnsealNoteModal({ isOpen, onClose, onUnseal, loading = false }: UnsealNoteModalProps) {
  const [unsealReason, setUnsealReason] = useState('');
  const [legalBasis, setLegalBasis] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!unsealReason.trim() || !legalBasis.trim()) {
      return;
    }

    onUnseal(unsealReason.trim(), legalBasis.trim());
  };

  const handleClose = () => {
    setUnsealReason('');
    setLegalBasis('');
    onClose();
  };

  const legalBasisOptions = [
    { value: 'INVESTIGATION_COMPLETE', label: 'Investigation Completed' },
    { value: 'COURT_ORDER_LIFTED', label: 'Court Order Lifted' },
    { value: 'CLIENT_CONSENT', label: 'Client Provided Consent' },
    { value: 'SAFETY_CONCERN_RESOLVED', label: 'Safety Concern Resolved' },
    { value: 'LEGAL_PROCEEDING_CONCLUDED', label: 'Legal Proceeding Concluded' },
    { value: 'THERAPEUTIC_NECESSITY', label: 'Therapeutic Necessity' },
    { value: 'ADMINISTRATIVE_REVIEW', label: 'Administrative Review' },
    { value: 'OTHER', label: 'Other Legal Basis' }
  ];

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center space-x-2">
            <span className="text-green-600">🔓</span>
            <span>Unseal Restricted Note</span>
          </DialogTitle>
          <DialogDescription>
            Unsealing this note will restore normal access based on the note's visibility scope. 
            This action requires proper authorization and will be logged for audit purposes.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="legalBasis">Legal Basis for Unsealing *</Label>
            <Select value={legalBasis} onValueChange={setLegalBasis} required>
              <SelectTrigger>
                <SelectValue placeholder="Select legal basis..." />
              </SelectTrigger>
              <SelectContent>
                {legalBasisOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="unsealReason">Detailed Justification for Unsealing *</Label>
            <Textarea
              id="unsealReason"
              value={unsealReason}
              onChange={(e) => setUnsealReason(e.target.value)}
              placeholder="Provide specific details about why this note should be unsealed..."
              required
              rows={3}
            />
          </div>

          <Card className="border-blue-200 bg-blue-50">
            <CardContent className="pt-4">
              <div className="flex items-start space-x-2 text-sm text-blue-800">
                <span className="mt-0.5">ℹ️</span>
                <div>
                  <p className="font-medium">Unsealing Effects:</p>
                  <ul className="mt-1 space-y-1 text-xs">
                    <li>• Note will be accessible based on its original visibility scope</li>
                    <li>• Access will be restored to authorized team members</li>
                    <li>• This action creates an audit trail for compliance</li>
                    <li>• Previous seal reason will be preserved in audit history</li>
                  </ul>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="border-amber-200 bg-amber-50">
            <CardContent className="pt-4">
              <div className="flex items-start space-x-2 text-sm text-amber-800">
                <span className="mt-0.5">⚠️</span>
                <div>
                  <p className="font-medium">Authorization Required:</p>
                  <p className="mt-1 text-xs">
                    Only users with appropriate privileges can unseal notes. 
                    Ensure you have proper authorization before proceeding.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <div className="flex space-x-2 pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={loading}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={loading || !unsealReason.trim() || !legalBasis.trim()}
              className="flex-1 bg-green-600 hover:bg-green-700 text-white"
            >
              {loading ? 'Unsealing...' : 'Unseal Note'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}