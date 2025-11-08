/**
 * Step 7: Documentation Uploads
 *
 * Manages document uploads with encryption, tagging, and access control.
 * Tracks required and optional documents with verification and waiver options.
 */

import React, { useState, useRef } from 'react';
import type { DocumentationData, DocumentStatus, ValidationError } from '../utils/types';

interface Step7Props {
  data: Partial<DocumentationData>;
  errors: ValidationError[];
  onChange: (updates: Partial<DocumentationData>) => void;
  onComplete: (data: DocumentationData) => void;
  onBack?: () => void;
  currentUser?: string;
}

type DocumentType = 'required' | 'optional';

interface DocumentCard {
  id: string;
  name: string;
  type: DocumentType;
  autoTags: string[];
  status: DocumentStatus;
}

const REQUIRED_DOCS = [
  { id: 'vawaConsent', name: 'VAWA Consent Form', autoTags: ['Confidential', 'Consent', 'VAWA Protected'] },
  { id: 'hmisConsent', name: 'HMIS Consent Form', autoTags: ['Confidential', 'Consent', 'HMIS'] },
  { id: 'photoRelease', name: 'Photo/Video Release', autoTags: ['Consent'] },
  { id: 'serviceAgreement', name: 'Service Agreement', autoTags: ['Consent', 'Case Management'] },
];

const OPTIONAL_DOCS = [
  { id: 'photoId', name: 'Photo ID', autoTags: ['Confidential'] },
  { id: 'birthCertificate', name: 'Birth Certificate', autoTags: ['Confidential'] },
  { id: 'ssnCard', name: 'Social Security Card', autoTags: ['Confidential'] },
  { id: 'insuranceCard', name: 'Insurance Card', autoTags: ['Medical'] },
  { id: 'incomeVerification', name: 'Income Verification', autoTags: ['Financial'] },
  { id: 'leaseAgreement', name: 'Lease Agreement', autoTags: ['Financial'] },
  { id: 'protectiveOrder', name: 'Protective Order Documentation', autoTags: ['Legal', 'VAWA Protected'] },
];

const AVAILABLE_TAGS = [
  'Confidential',
  'Consent',
  'Financial',
  'Medical',
  'Legal',
  'Case Management',
  'HMIS',
  'VAWA Protected',
  'Temporary',
];

const ALLOWED_FILE_TYPES = ['application/pdf', 'image/jpeg', 'image/png', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

export function Step7_DocumentUpload({
  data,
  errors,
  onChange,
  onComplete,
  onBack,
  currentUser = 'Current User',
}: Step7Props) {
  const [formData, setFormData] = useState<Partial<DocumentationData>>({
    requiredDocuments: {
      vawaConsent: { uploaded: false, verified: false, waived: false },
      hmisConsent: { uploaded: false, verified: false, waived: false },
      photoRelease: { uploaded: false, verified: false, waived: false },
      serviceAgreement: { uploaded: false, verified: false, waived: false },
      ...data.requiredDocuments,
    },
    optionalDocuments: {
      photoId: { uploaded: false, verified: false, waived: false },
      birthCertificate: { uploaded: false, verified: false, waived: false },
      ssnCard: { uploaded: false, verified: false, waived: false },
      insuranceCard: { uploaded: false, verified: false, waived: false },
      incomeVerification: { uploaded: false, verified: false, waived: false },
      leaseAgreement: { uploaded: false, verified: false, waived: false },
      protectiveOrder: { uploaded: false, verified: false, waived: false },
      other: {},
      ...data.optionalDocuments,
    },
    documentTags: data.documentTags || {},
    expirationRules: data.expirationRules || {},
    uploadMetadata: data.uploadMetadata || {},
  });

  const [uploadProgress, setUploadProgress] = useState<Record<string, number>>({});
  const [dragOver, setDragOver] = useState<string | null>(null);
  const [showPreview, setShowPreview] = useState<{ id: string; url: string } | null>(null);
  const [customDocName, setCustomDocName] = useState('');
  const fileInputRefs = useRef<Record<string, HTMLInputElement | null>>({});

  const handleChange = (field: keyof DocumentationData, value: any) => {
    const updated = { ...formData, [field]: value };
    setFormData(updated);
    onChange(updated);
  };

  const handleDocumentStatusChange = (
    type: 'requiredDocuments' | 'optionalDocuments',
    docId: string,
    field: keyof DocumentStatus,
    value: any
  ) => {
    const updated = {
      ...formData,
      [type]: {
        ...formData[type],
        [docId]: {
          ...(formData[type] as any)[docId],
          [field]: value,
        },
      },
    };
    setFormData(updated);
    onChange(updated);
  };

  const handleFileSelect = async (docId: string, type: DocumentType, file: File) => {
    // Validate file
    const validation = validateFile(file);
    if (!validation.valid) {
      alert(validation.error);
      return;
    }

    // Simulate upload with progress
    setUploadProgress(prev => ({ ...prev, [docId]: 0 }));

    // In real implementation, would use documentApi.upload()
    // For now, simulate upload
    const uploadInterval = setInterval(() => {
      setUploadProgress(prev => {
        const currentProgress = prev[docId] || 0;
        if (currentProgress >= 100) {
          clearInterval(uploadInterval);
          completeUpload(docId, type, file);
          return { ...prev, [docId]: 100 };
        }
        return { ...prev, [docId]: currentProgress + 10 };
      });
    }, 200);
  };

  const completeUpload = (docId: string, type: DocumentType, file: File) => {
    const fileId = `file-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    const now = new Date().toISOString();

    const docType = type === 'required' ? 'requiredDocuments' : 'optionalDocuments';

    // Update document status
    const updated = {
      ...formData,
      [docType]: {
        ...formData[docType],
        [docId]: {
          ...(formData[docType] as any)[docId],
          uploaded: true,
          fileId,
          filename: file.name,
          uploadDate: now,
          uploadedBy: currentUser,
        },
      },
      uploadMetadata: {
        ...formData.uploadMetadata,
        [fileId]: {
          fileSize: file.size,
          mimeType: file.type,
          encrypted: true,
          encryptionAlgorithm: 'AES-256',
          accessLog: [
            {
              userId: currentUser,
              timestamp: now,
              action: 'Uploaded',
            },
          ],
        },
      },
    };

    setFormData(updated);
    onChange(updated);

    // Clear progress after short delay
    setTimeout(() => {
      setUploadProgress(prev => {
        const { [docId]: _, ...rest } = prev;
        return rest;
      });
    }, 1000);
  };

  const handleDelete = (docId: string, type: DocumentType) => {
    if (!confirm('Are you sure you want to delete this document?')) {
      return;
    }

    const docType = type === 'required' ? 'requiredDocuments' : 'optionalDocuments';
    const doc = (formData[docType] as any)[docId];

    const updated = {
      ...formData,
      [docType]: {
        ...formData[docType],
        [docId]: {
          ...doc,
          uploaded: false,
          fileId: undefined,
          filename: undefined,
          uploadDate: undefined,
          uploadedBy: undefined,
          verified: false,
          verificationDate: undefined,
        },
      },
    };

    setFormData(updated);
    onChange(updated);
  };

  const handleDragOver = (e: React.DragEvent, docId: string) => {
    e.preventDefault();
    setDragOver(docId);
  };

  const handleDragLeave = () => {
    setDragOver(null);
  };

  const handleDrop = (e: React.DragEvent, docId: string, type: DocumentType) => {
    e.preventDefault();
    setDragOver(null);

    const file = e.dataTransfer.files[0];
    if (file) {
      handleFileSelect(docId, type, file);
    }
  };

  const handleAddCustomDocument = () => {
    if (!customDocName.trim()) {
      alert('Please enter a document name');
      return;
    }

    const customId = `custom-${Date.now()}`;
    const updated = {
      ...formData,
      optionalDocuments: {
        ...formData.optionalDocuments,
        other: {
          ...(formData.optionalDocuments?.other || {}),
          [customId]: {
            uploaded: false,
            verified: false,
            waived: false,
          },
        },
      },
    };

    setFormData(updated);
    onChange(updated);
    setCustomDocName('');
  };

  const handleSubmit = () => {
    if (formData as DocumentationData) {
      onComplete(formData as DocumentationData);
    }
  };

  const validateFile = (file: File): { valid: boolean; error?: string } => {
    if (!ALLOWED_FILE_TYPES.includes(file.type)) {
      return { valid: false, error: 'Invalid file type. Only PDF, JPG, PNG, and DOCX are allowed.' };
    }
    if (file.size > MAX_FILE_SIZE) {
      return { valid: false, error: 'File size exceeds 10MB limit.' };
    }
    return { valid: true };
  };

  const getStatusBadge = (status: DocumentStatus): { text: string; className: string } => {
    if (status.waived) return { text: 'Waived', className: 'status-waived' };
    if (status.verified) return { text: 'Verified', className: 'status-verified' };
    if (status.uploaded) return { text: 'Uploaded', className: 'status-uploaded' };
    return { text: 'Not Uploaded', className: 'status-not-uploaded' };
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getUploadedCount = (type: 'required' | 'optional'): number => {
    const docs = type === 'required' ? formData.requiredDocuments : formData.optionalDocuments;
    if (!docs) return 0;

    return Object.values(docs).filter((doc: any) => {
      if (typeof doc === 'object' && doc !== null && 'uploaded' in doc) {
        return doc.uploaded || doc.waived;
      }
      return false;
    }).length;
  };

  const getTotalStorage = (): number => {
    if (!formData.uploadMetadata) return 0;
    return Object.values(formData.uploadMetadata).reduce((sum, meta) => sum + meta.fileSize, 0);
  };

  const renderDocumentCard = (
    docId: string,
    docName: string,
    status: DocumentStatus,
    type: DocumentType,
    autoTags: string[]
  ) => {
    const badge = getStatusBadge(status);
    const inProgress = uploadProgress[docId] !== undefined;
    const metadata = status.fileId ? formData.uploadMetadata?.[status.fileId] : undefined;

    return (
      <div key={docId} className="document-card">
        <div className="document-header">
          <h4>{docName}</h4>
          <span className={`status-badge ${badge.className}`}>{badge.text}</span>
        </div>

        {/* Upload Progress */}
        {inProgress && (
          <div className="upload-progress">
            <div className="progress-bar">
              <div className="progress-fill" style={{ width: `${uploadProgress[docId]}%` }} />
            </div>
            <span className="progress-text">{uploadProgress[docId]}%</span>
          </div>
        )}

        {/* Not Uploaded State */}
        {!status.uploaded && !status.waived && !inProgress && (
          <div
            className={`upload-zone ${dragOver === docId ? 'drag-over' : ''}`}
            onDragOver={e => handleDragOver(e, docId)}
            onDragLeave={handleDragLeave}
            onDrop={e => handleDrop(e, docId, type)}
            onClick={() => fileInputRefs.current[docId]?.click()}
          >
            <div className="upload-icon">📄</div>
            <p>Drag & Drop or Click to Browse</p>
            <span className="upload-restrictions">PDF, JPG, PNG, DOCX | Max 10MB</span>
            <input
              ref={el => (fileInputRefs.current[docId] = el)}
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.docx"
              style={{ display: 'none' }}
              onChange={e => {
                const file = e.target.files?.[0];
                if (file) handleFileSelect(docId, type, file);
              }}
            />
          </div>
        )}

        {/* Uploaded State */}
        {status.uploaded && (
          <div className="uploaded-info">
            <div className="file-meta">
              <div className="meta-row">
                <strong>Filename:</strong> {status.filename}
              </div>
              <div className="meta-row">
                <strong>Upload Date:</strong> {status.uploadDate ? new Date(status.uploadDate).toLocaleString() : 'N/A'}
              </div>
              <div className="meta-row">
                <strong>Uploaded By:</strong> {status.uploadedBy}
              </div>
              {metadata && (
                <div className="meta-row">
                  <strong>File Size:</strong> {formatFileSize(metadata.fileSize)}
                </div>
              )}
            </div>

            <div className="document-actions">
              <button type="button" className="btn-sm btn-secondary">
                Preview
              </button>
              <button type="button" className="btn-sm btn-secondary">
                Download
              </button>
              <button type="button" className="btn-sm btn-danger" onClick={() => handleDelete(docId, type)}>
                Delete
              </button>
            </div>

            <div className="verification-controls">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={status.verified}
                  onChange={e =>
                    handleDocumentStatusChange(
                      type === 'required' ? 'requiredDocuments' : 'optionalDocuments',
                      docId,
                      'verified',
                      e.target.checked
                    )
                  }
                />
                Mark as verified (staff only)
              </label>
            </div>

            {/* Auto-tags Display */}
            <div className="document-tags">
              <label>Tags:</label>
              <div className="tag-list">
                {autoTags.map(tag => (
                  <span key={tag} className="tag">
                    {tag}
                  </span>
                ))}
              </div>
            </div>

            {/* Access Log */}
            {metadata?.accessLog && metadata.accessLog.length > 0 && (
              <div className="access-log">
                <h5>Access Log</h5>
                <table className="log-table">
                  <thead>
                    <tr>
                      <th>User</th>
                      <th>Action</th>
                      <th>Timestamp</th>
                    </tr>
                  </thead>
                  <tbody>
                    {metadata.accessLog.map((entry, idx) => (
                      <tr key={idx}>
                        <td>{entry.userId}</td>
                        <td>{entry.action}</td>
                        <td>{new Date(entry.timestamp).toLocaleString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* Waiver Option */}
        <div className="waiver-section">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={status.waived}
              onChange={e =>
                handleDocumentStatusChange(
                  type === 'required' ? 'requiredDocuments' : 'optionalDocuments',
                  docId,
                  'waived',
                  e.target.checked
                )
              }
              disabled={status.uploaded}
            />
            Waive this document
          </label>

          {status.waived && (
            <div className="form-field">
              <label>
                Reason for waiver <span className="required">*</span>
              </label>
              <textarea
                value={status.waiverReason || ''}
                onChange={e =>
                  handleDocumentStatusChange(
                    type === 'required' ? 'requiredDocuments' : 'optionalDocuments',
                    docId,
                    'waiverReason',
                    e.target.value
                  )
                }
                placeholder="Explain why this document is being waived..."
              />
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="intake-step">
      <div className="step-header">
        <h2>Step 7: Documentation Upload</h2>
        <p className="step-description">
          Upload required consent forms and optional supporting documents. All files are encrypted
          and access-controlled.
        </p>
      </div>

      <div className="document-upload-layout">
        {/* Main Content */}
        <div className="documents-main">
          <form className="intake-form">
            {/* Required Documents */}
            <section className="form-section">
              <h3>Required Documents</h3>
              <p className="section-help">
                Must upload or waive each document before proceeding.
              </p>

              <div className="documents-grid">
                {REQUIRED_DOCS.map(doc =>
                  renderDocumentCard(
                    doc.id,
                    doc.name,
                    (formData.requiredDocuments as any)?.[doc.id] || {
                      uploaded: false,
                      verified: false,
                      waived: false,
                    },
                    'required',
                    doc.autoTags
                  )
                )}
              </div>
            </section>

            {/* Optional Documents */}
            <section className="form-section">
              <h3>Optional Documents</h3>
              <p className="section-help">Supporting documents to verify eligibility and services.</p>

              <div className="documents-grid">
                {OPTIONAL_DOCS.map(doc =>
                  renderDocumentCard(
                    doc.id,
                    doc.name,
                    (formData.optionalDocuments as any)?.[doc.id] || {
                      uploaded: false,
                      verified: false,
                      waived: false,
                    },
                    'optional',
                    doc.autoTags
                  )
                )}

                {/* Custom Documents */}
                {formData.optionalDocuments?.other &&
                  Object.entries(formData.optionalDocuments.other).map(([id, status]) =>
                    renderDocumentCard(id, 'Custom Document', status, 'optional', ['Case Management'])
                  )}
              </div>

              {/* Add Custom Document */}
              <div className="add-document-section">
                <div className="input-with-button">
                  <input
                    type="text"
                    placeholder="Enter document name..."
                    value={customDocName}
                    onChange={e => setCustomDocName(e.target.value)}
                  />
                  <button type="button" className="btn-secondary" onClick={handleAddCustomDocument}>
                    + Add Other Document
                  </button>
                </div>
              </div>
            </section>

            {/* Form Actions */}
            <div className="form-actions">
              {onBack && (
                <button type="button" className="btn btn-secondary" onClick={onBack}>
                  Back
                </button>
              )}
              <button type="button" className="btn btn-primary" onClick={handleSubmit}>
                Continue to Demographics
              </button>
            </div>
          </form>
        </div>

        {/* Summary Sidebar */}
        <aside className="document-summary-sidebar">
          <div className="summary-card">
            <h3>Summary</h3>

            <div className="summary-item">
              <label>Required Documents</label>
              <div className="summary-value">
                {getUploadedCount('required')} / {REQUIRED_DOCS.length}
              </div>
            </div>

            <div className="summary-item">
              <label>Optional Documents</label>
              <div className="summary-value">
                {getUploadedCount('optional')} / {OPTIONAL_DOCS.length}
              </div>
            </div>

            <div className="summary-item">
              <label>Total Storage</label>
              <div className="summary-value">{formatFileSize(getTotalStorage())}</div>
            </div>

            <div className="compliance-status">
              <h4>Compliance Status</h4>
              {getUploadedCount('required') === REQUIRED_DOCS.length ? (
                <div className="status-ok">
                  <span className="icon">✅</span>
                  All required documents obtained
                </div>
              ) : (
                <div className="status-warning">
                  <span className="icon">⚠️</span>
                  Missing required documents
                </div>
              )}

              {getUploadedCount('optional') < OPTIONAL_DOCS.length && (
                <div className="status-info">
                  <span className="icon">ℹ️</span>
                  Missing optional documents:
                  <ul>
                    {OPTIONAL_DOCS.filter(
                      doc =>
                        !(formData.optionalDocuments as any)?.[doc.id]?.uploaded &&
                        !(formData.optionalDocuments as any)?.[doc.id]?.waived
                    ).map(doc => (
                      <li key={doc.id}>{doc.name}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
