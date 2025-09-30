import React, { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  Button,
  Input,
  Textarea,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Label,
  Switch,
  Card,
  CardContent
} from '@haven/ui';

interface SealNoteModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSeal: (sealReason: string, legalBasis: string, isTemporary: boolean, expiresAt?: string) => void;
  loading?: boolean;
}

export default function SealNoteModal({ isOpen, onClose, onSeal, loading = false }: SealNoteModalProps) {
  const [sealReason, setSealReason] = useState('');
  const [legalBasis, setLegalBasis] = useState('');
  const [isTemporary, setIsTemporary] = useState(false);
  const [expiresAt, setExpiresAt] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!sealReason.trim() || !legalBasis.trim()) {
      return;
    }

    onSeal(sealReason.trim(), legalBasis.trim(), isTemporary, isTemporary ? expiresAt : undefined);
  };

  const handleClose = () => {
    setSealReason('');
    setLegalBasis('');
    setIsTemporary(false);
    setExpiresAt('');
    onClose();
  };

  const legalBasisOptions = [
    { value: 'CLIENT_SAFETY', label: 'Client Safety Concerns' },
    { value: 'LEGAL_PROCEEDING', label: 'Ongoing Legal Proceeding' },
    { value: 'INVESTIGATION', label: 'Active Investigation' },
    { value: 'CONFIDENTIALITY_REQUEST', label: 'Client Confidentiality Request' },
    { value: 'COURT_ORDER', label: 'Court Order' },
    { value: 'THERAPEUTIC_PRIVILEGE', label: 'Therapeutic Privilege' },
    { value: 'OTHER', label: 'Other Legal Basis' }
  ];

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center space-x-2">
            <span className="text-red-600">🚫</span>
            <span>Seal Restricted Note</span>
          </DialogTitle>
          <DialogDescription>
            Sealing this note will restrict access to authorized personnel only. 
            This action will be logged for audit purposes.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="legalBasis">Legal Basis for Sealing *</Label>
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
            <Label htmlFor="sealReason">Detailed Reason for Sealing *</Label>
            <Textarea
              id="sealReason"
              value={sealReason}
              onChange={(e) => setSealReason(e.target.value)}
              placeholder="Provide specific details about why this note needs to be sealed..."
              required
              rows={3}
            />
          </div>

          <div className="space-y-3">
            <div className="flex items-center space-x-2">
              <Switch
                id="temporary"
                checked={isTemporary}
                onCheckedChange={setIsTemporary}
              />
              <Label htmlFor="temporary">Temporary seal (expires automatically)</Label>
            </div>

            {isTemporary && (
              <div className="space-y-2 ml-6">
                <Label htmlFor="expiresAt">Expiration Date & Time</Label>
                <Input
                  id="expiresAt"
                  type="datetime-local"
                  value={expiresAt}
                  onChange={(e) => setExpiresAt(e.target.value)}
                  min={new Date().toISOString().slice(0, 16)}
                  required={isTemporary}
                />
              </div>
            )}
          </div>

          <Card className="border-amber-200 bg-amber-50">
            <CardContent className="pt-4">
              <div className="flex items-start space-x-2 text-sm text-amber-800">
                <span className="mt-0.5">⚠️</span>
                <div>
                  <p className="font-medium">Important Notice:</p>
                  <ul className="mt-1 space-y-1 text-xs">
                    <li>• Sealed notes are only accessible to users who sealed them</li>
                    <li>• This action creates an audit trail for compliance</li>
                    <li>• Unsealing requires proper authorization and justification</li>
                  </ul>
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
              disabled={loading || !sealReason.trim() || !legalBasis.trim()}
              className="flex-1 bg-red-600 hover:bg-red-700 text-white"
            >
              {loading ? 'Sealing...' : 'Seal Note'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}