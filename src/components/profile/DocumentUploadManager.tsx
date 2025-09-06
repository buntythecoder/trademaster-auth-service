import React, { useState, useRef } from 'react';
import { Upload, FileText, Image, X, CheckCircle, AlertTriangle, Clock, Download, Trash2, Eye, RotateCcw } from 'lucide-react';
import LoadingSpinner from '../common/LoadingSpinner';
import ConfirmationModal from '../common/ConfirmationModal';
import StatusBadge from '../common/StatusBadge';
import { useAuthStore } from '../../stores/auth.store';

interface DocumentFile {
  id: string;
  name: string;
  type: 'identity_proof' | 'address_proof' | 'income_proof' | 'bank_statement' | 'tax_document' | 'other';
  category: string;
  file: File | null;
  url?: string;
  size: number;
  uploadDate: Date;
  status: 'pending' | 'uploading' | 'processing' | 'approved' | 'rejected' | 'expired';
  rejectionReason?: string;
  metadata: {
    mimeType: string;
    dimensions?: { width: number; height: number };
    pages?: number;
    checksum?: string;
  };
}

interface UploadProgress {
  fileId: string;
  progress: number;
}

const documentTypes = [
  { value: 'identity_proof', label: 'Identity Proof', description: 'Passport, Driver\'s License, Aadhaar Card' },
  { value: 'address_proof', label: 'Address Proof', description: 'Utility Bill, Bank Statement, Rental Agreement' },
  { value: 'income_proof', label: 'Income Proof', description: 'Salary Certificate, ITR, Form 16' },
  { value: 'bank_statement', label: 'Bank Statement', description: 'Last 3-6 months bank statements' },
  { value: 'tax_document', label: 'Tax Document', description: 'ITR, TDS Certificate, Tax Returns' },
  { value: 'other', label: 'Other', description: 'Additional supporting documents' }
];

const maxFileSize = 10 * 1024 * 1024; // 10MB
const allowedFileTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];

export const DocumentUploadManager: React.FC = () => {
  const { user } = useAuthStore();
  const isAdmin = user?.role === 'ADMIN';
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  const [documents, setDocuments] = useState<DocumentFile[]>([
    {
      id: '1',
      name: 'Aadhaar_Card_Front.pdf',
      type: 'identity_proof',
      category: 'Identity Proof',
      file: null,
      size: 2.1 * 1024 * 1024,
      uploadDate: new Date('2024-01-15'),
      status: 'approved',
      metadata: { mimeType: 'application/pdf', pages: 1 }
    },
    {
      id: '2',
      name: 'Electricity_Bill_Jan2024.pdf',
      type: 'address_proof',
      category: 'Address Proof',
      file: null,
      size: 1.8 * 1024 * 1024,
      uploadDate: new Date('2024-01-18'),
      status: 'rejected',
      rejectionReason: 'Document is older than 3 months. Please upload a recent utility bill.',
      metadata: { mimeType: 'application/pdf', pages: 2 }
    },
    {
      id: '3',
      name: 'Bank_Statement_Dec2023.pdf',
      type: 'bank_statement',
      category: 'Bank Statement',
      file: null,
      size: 5.2 * 1024 * 1024,
      uploadDate: new Date('2024-01-20'),
      status: 'processing',
      metadata: { mimeType: 'application/pdf', pages: 12 }
    }
  ]);

  const [selectedDocumentType, setSelectedDocumentType] = useState<string>('');
  const [dragOver, setDragOver] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<UploadProgress[]>([]);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const [previewDocument, setPreviewDocument] = useState<DocumentFile | null>(null);

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const validateFile = (file: File): { isValid: boolean; error?: string } => {
    if (file.size > maxFileSize) {
      return { isValid: false, error: 'File size must be less than 10MB' };
    }
    
    if (!allowedFileTypes.includes(file.type)) {
      return { isValid: false, error: 'Only PDF, JPEG, PNG, and WebP files are allowed' };
    }
    
    return { isValid: true };
  };

  const handleFileSelect = (files: FileList | null) => {
    if (!files || !selectedDocumentType) return;

    Array.from(files).forEach(file => {
      const validation = validateFile(file);
      if (!validation.isValid) {
        alert(validation.error);
        return;
      }

      const newDocument: DocumentFile = {
        id: Date.now().toString() + Math.random().toString(36).substr(2, 9),
        name: file.name,
        type: selectedDocumentType as any,
        category: documentTypes.find(t => t.value === selectedDocumentType)?.label || 'Unknown',
        file,
        size: file.size,
        uploadDate: new Date(),
        status: 'pending',
        metadata: {
          mimeType: file.type,
          checksum: 'pending'
        }
      };

      setDocuments(prev => [...prev, newDocument]);
      uploadDocument(newDocument);
    });
  };

  const uploadDocument = async (document: DocumentFile) => {
    if (!document.file) return;

    setDocuments(prev => 
      prev.map(doc => 
        doc.id === document.id 
          ? { ...doc, status: 'uploading' }
          : doc
      )
    );

    setUploadProgress(prev => [...prev, { fileId: document.id, progress: 0 }]);

    try {
      // Simulate upload progress
      for (let progress = 0; progress <= 100; progress += 10) {
        await new Promise(resolve => setTimeout(resolve, 200));
        setUploadProgress(prev => 
          prev.map(p => 
            p.fileId === document.id 
              ? { ...p, progress }
              : p
          )
        );
      }

      // Simulate processing
      setDocuments(prev => 
        prev.map(doc => 
          doc.id === document.id 
            ? { ...doc, status: 'processing' }
            : doc
        )
      );

      // Remove upload progress
      setUploadProgress(prev => prev.filter(p => p.fileId !== document.id));

      // Simulate final status (randomly approve/reject for demo)
      setTimeout(() => {
        const finalStatus = Math.random() > 0.3 ? 'approved' : 'rejected';
        setDocuments(prev => 
          prev.map(doc => 
            doc.id === document.id 
              ? { 
                  ...doc, 
                  status: finalStatus,
                  rejectionReason: finalStatus === 'rejected' ? 'Document quality is poor. Please upload a clearer image.' : undefined
                }
              : doc
          )
        );
      }, 3000);

    } catch (error) {
      setDocuments(prev => 
        prev.map(doc => 
          doc.id === document.id 
            ? { ...doc, status: 'rejected', rejectionReason: 'Upload failed. Please try again.' }
            : doc
        )
      );
      setUploadProgress(prev => prev.filter(p => p.fileId !== document.id));
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    handleFileSelect(e.dataTransfer.files);
  };

  const handleDeleteDocument = async (documentId: string) => {
    try {
      setDocuments(prev => prev.filter(doc => doc.id !== documentId));
      setDeleteConfirm(null);
    } catch (error) {
      console.error('Failed to delete document:', error);
    }
  };

  const handleReUpload = (document: DocumentFile) => {
    if (fileInputRef.current) {
      setSelectedDocumentType(document.type);
      fileInputRef.current.click();
    }
  };

  const adminApproveDocument = (documentId: string) => {
    setDocuments(prev => 
      prev.map(doc => 
        doc.id === documentId 
          ? { ...doc, status: 'approved', rejectionReason: undefined }
          : doc
      )
    );
  };

  const adminRejectDocument = (documentId: string, reason: string) => {
    setDocuments(prev => 
      prev.map(doc => 
        doc.id === documentId 
          ? { ...doc, status: 'rejected', rejectionReason: reason }
          : doc
      )
    );
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'approved': return <CheckCircle className="w-5 h-5 text-green-400" />;
      case 'rejected': return <AlertTriangle className="w-5 h-5 text-red-400" />;
      case 'processing': case 'uploading': return <Clock className="w-5 h-5 text-yellow-400 animate-spin" />;
      default: return <Clock className="w-5 h-5 text-gray-400" />;
    }
  };

  const getDocumentStats = () => {
    const stats = documents.reduce((acc, doc) => {
      acc[doc.status] = (acc[doc.status] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

    return {
      total: documents.length,
      approved: stats.approved || 0,
      rejected: stats.rejected || 0,
      pending: (stats.pending || 0) + (stats.processing || 0) + (stats.uploading || 0)
    };
  };

  const stats = getDocumentStats();

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-2xl font-bold text-white mb-2">Document Management</h3>
          <p className="text-slate-400">Upload and manage your verification documents</p>
        </div>
      </div>

      {/* Document Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-slate-800/30 rounded-xl p-4">
          <div className="text-2xl font-bold text-white">{stats.total}</div>
          <div className="text-sm text-slate-400">Total Documents</div>
        </div>
        <div className="bg-slate-800/30 rounded-xl p-4">
          <div className="text-2xl font-bold text-green-400">{stats.approved}</div>
          <div className="text-sm text-slate-400">Approved</div>
        </div>
        <div className="bg-slate-800/30 rounded-xl p-4">
          <div className="text-2xl font-bold text-yellow-400">{stats.pending}</div>
          <div className="text-sm text-slate-400">Pending</div>
        </div>
        <div className="bg-slate-800/30 rounded-xl p-4">
          <div className="text-2xl font-bold text-red-400">{stats.rejected}</div>
          <div className="text-sm text-slate-400">Rejected</div>
        </div>
      </div>

      {/* Upload Section */}
      {!isAdmin && (
        <div className="bg-slate-800/30 rounded-xl p-6">
          <h4 className="text-lg font-semibold text-white mb-4">Upload New Document</h4>
          
          <div className="mb-4">
            <label className="block text-sm font-medium text-slate-300 mb-2">Document Type</label>
            <select
              value={selectedDocumentType}
              onChange={(e) => setSelectedDocumentType(e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 text-white rounded-lg px-3 py-2"
            >
              <option value="">Select document type</option>
              {documentTypes.map(type => (
                <option key={type.value} value={type.value}>
                  {type.label} - {type.description}
                </option>
              ))}
            </select>
          </div>

          <div
            className={`border-2 border-dashed rounded-xl p-8 text-center transition-colors ${
              dragOver
                ? 'border-blue-400 bg-blue-500/10'
                : 'border-slate-600 bg-slate-700/30 hover:border-slate-500'
            }`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <Upload className="w-12 h-12 text-slate-400 mx-auto mb-4" />
            <p className="text-slate-300 mb-2">
              Drag and drop files here, or{' '}
              <button
                onClick={() => selectedDocumentType && fileInputRef.current?.click()}
                className="text-blue-400 hover:text-blue-300 underline"
                disabled={!selectedDocumentType}
              >
                browse files
              </button>
            </p>
            <p className="text-sm text-slate-400">
              Supports: PDF, JPEG, PNG, WebP (Max 10MB)
            </p>
          </div>

          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept=".pdf,.jpg,.jpeg,.png,.webp"
            onChange={(e) => handleFileSelect(e.target.files)}
            className="hidden"
          />
        </div>
      )}

      {/* Documents List */}
      <div className="space-y-4">
        {documents.map(document => {
          const progress = uploadProgress.find(p => p.fileId === document.id);
          
          return (
            <div key={document.id} className="bg-slate-800/30 rounded-xl p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <div className="p-2 bg-slate-700/50 rounded-lg">
                    {document.metadata.mimeType.startsWith('image/') ? (
                      <Image className="w-6 h-6 text-slate-400" />
                    ) : (
                      <FileText className="w-6 h-6 text-slate-400" />
                    )}
                  </div>
                  
                  <div>
                    <h4 className="font-medium text-white">{document.name}</h4>
                    <div className="flex items-center space-x-2 text-sm text-slate-400">
                      <span>{document.category}</span>
                      <span>•</span>
                      <span>{formatFileSize(document.size)}</span>
                      <span>•</span>
                      <span>{document.uploadDate.toLocaleDateString()}</span>
                      {isAdmin && <span>• User: John Trader</span>}
                    </div>
                    {document.rejectionReason && (
                      <p className="text-sm text-red-400 mt-1">{document.rejectionReason}</p>
                    )}
                  </div>
                </div>

                <div className="flex items-center space-x-4">
                  <StatusBadge status={document.status} size="sm" />
                  
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => setPreviewDocument(document)}
                      className="p-2 text-slate-400 hover:text-blue-400 transition-colors"
                      title="Preview"
                    >
                      <Eye className="w-4 h-4" />
                    </button>
                    
                    {document.url && (
                      <button className="p-2 text-slate-400 hover:text-green-400 transition-colors" title="Download">
                        <Download className="w-4 h-4" />
                      </button>
                    )}
                    
                    {isAdmin ? (
                      <>
                        <button
                          onClick={() => adminApproveDocument(document.id)}
                          className="p-2 text-green-400 hover:text-green-300 transition-colors"
                          title="Approve"
                        >
                          <CheckCircle className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => adminRejectDocument(document.id, 'Admin rejected')}
                          className="p-2 text-red-400 hover:text-red-300 transition-colors"
                          title="Reject"
                        >
                          <AlertTriangle className="w-4 h-4" />
                        </button>
                      </>
                    ) : (
                      <>
                        {document.status === 'rejected' && (
                          <button
                            onClick={() => handleReUpload(document)}
                            className="p-2 text-yellow-400 hover:text-yellow-300 transition-colors"
                            title="Re-upload"
                          >
                            <RotateCcw className="w-4 h-4" />
                          </button>
                        )}
                        <button
                          onClick={() => setDeleteConfirm(document.id)}
                          className="p-2 text-slate-400 hover:text-red-400 transition-colors"
                          title="Delete"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>

              {/* Upload Progress */}
              {progress && (
                <div className="mt-4">
                  <div className="flex justify-between text-sm text-slate-400 mb-1">
                    <span>Uploading...</span>
                    <span>{progress.progress}%</span>
                  </div>
                  <div className="w-full bg-slate-700 rounded-full h-2">
                    <div
                      className="bg-blue-500 h-2 rounded-full transition-all"
                      style={{ width: `${progress.progress}%` }}
                    />
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Preview Document Modal */}
      {previewDocument && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-800 rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col">
            {/* Modal Header */}
            <div className="flex items-center justify-between p-6 border-b border-slate-700/50 flex-shrink-0">
              <div>
                <h2 className="text-xl font-bold text-white">{previewDocument.name}</h2>
                <p className="text-slate-400 text-sm">{previewDocument.category} • {formatFileSize(previewDocument.size)}</p>
              </div>
              <button
                onClick={() => setPreviewDocument(null)}
                className="p-2 hover:bg-slate-700/50 rounded-xl transition-colors"
              >
                <X className="w-5 h-5 text-slate-400" />
              </button>
            </div>

            {/* Modal Content */}
            <div className="flex-1 overflow-y-auto p-6">
              <div className="flex items-center justify-center h-full min-h-[400px] bg-slate-900/50 rounded-xl">
                {previewDocument.metadata.mimeType.startsWith('image/') ? (
                  <div className="text-center">
                    <Image className="w-12 h-12 text-slate-400 mx-auto mb-4" />
                    <p className="text-slate-300 mb-2">Image Preview</p>
                    <p className="text-slate-400 text-sm">
                      {previewDocument.metadata.dimensions ? 
                        `${previewDocument.metadata.dimensions.width} × ${previewDocument.metadata.dimensions.height} pixels` : 
                        'Image file'}
                    </p>
                  </div>
                ) : previewDocument.metadata.mimeType === 'application/pdf' ? (
                  <div className="text-center">
                    <FileText className="w-12 h-12 text-slate-400 mx-auto mb-4" />
                    <p className="text-slate-300 mb-2">PDF Document</p>
                    <p className="text-slate-400 text-sm">
                      {previewDocument.metadata.pages ? `${previewDocument.metadata.pages} pages` : 'PDF file'}
                    </p>
                  </div>
                ) : (
                  <div className="text-center">
                    <FileText className="w-12 h-12 text-slate-400 mx-auto mb-4" />
                    <p className="text-slate-300 mb-2">Document Preview</p>
                    <p className="text-slate-400 text-sm">File type: {previewDocument.metadata.mimeType}</p>
                  </div>
                )}
              </div>

              {/* Document Info */}
              <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-slate-800/30 rounded-xl p-4">
                  <h3 className="text-sm font-semibold text-slate-300 mb-3">Document Details</h3>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-slate-400">Type:</span>
                      <span className="text-white">{previewDocument.category}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Size:</span>
                      <span className="text-white">{formatFileSize(previewDocument.size)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Uploaded:</span>
                      <span className="text-white">{previewDocument.uploadDate.toLocaleDateString()}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Status:</span>
                      <StatusBadge status={previewDocument.status} size="sm" />
                    </div>
                  </div>
                </div>

                <div className="bg-slate-800/30 rounded-xl p-4">
                  <h3 className="text-sm font-semibold text-slate-300 mb-3">File Information</h3>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-slate-400">Format:</span>
                      <span className="text-white">{previewDocument.metadata.mimeType.split('/')[1].toUpperCase()}</span>
                    </div>
                    {previewDocument.metadata.checksum && (
                      <div className="flex justify-between">
                        <span className="text-slate-400">Checksum:</span>
                        <span className="text-white font-mono text-xs">{previewDocument.metadata.checksum.substring(0, 16)}...</span>
                      </div>
                    )}
                    {previewDocument.rejectionReason && (
                      <div>
                        <span className="text-slate-400">Rejection Reason:</span>
                        <p className="text-red-400 text-sm mt-1">{previewDocument.rejectionReason}</p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="flex items-center justify-end space-x-3 p-6 border-t border-slate-700/50 flex-shrink-0">
              {previewDocument.url && (
                <button className="px-4 py-2 rounded-xl bg-green-500/20 text-green-400 hover:bg-green-500/30 transition-colors font-medium flex items-center space-x-2">
                  <Download className="w-4 h-4" />
                  <span>Download</span>
                </button>
              )}
              <button
                onClick={() => setPreviewDocument(null)}
                className="px-4 py-2 text-slate-400 hover:text-white transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      <ConfirmationModal
        isOpen={deleteConfirm !== null}
        onClose={() => setDeleteConfirm(null)}
        onConfirm={() => deleteConfirm && handleDeleteDocument(deleteConfirm)}
        title="Delete Document"
        message="Are you sure you want to delete this document? This action cannot be undone."
        variant="danger"
        confirmText="Delete"
      />
    </div>
  );
};

export default DocumentUploadManager;