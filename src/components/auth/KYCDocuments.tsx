import React, { useState, useRef } from 'react'
import { Upload, FileText, CheckCircle, AlertTriangle, X, Eye, Download, Camera } from 'lucide-react'
import { useToast } from '../../contexts/ToastContext'

interface DocumentType {
  id: string
  name: string
  description: string
  required: boolean
  acceptedFormats: string[]
  maxSize: number // in MB
  status: 'pending' | 'uploaded' | 'verified' | 'rejected'
  file?: File
  uploadDate?: string
  rejectionReason?: string
}

interface KYCDocumentsProps {
  onComplete?: (verified: boolean) => void
  onCancel?: () => void
}

export function KYCDocuments({ onComplete, onCancel }: KYCDocumentsProps) {
  const [documents, setDocuments] = useState<DocumentType[]>([
    {
      id: 'aadhar',
      name: 'Aadhaar Card',
      description: 'Government issued identity proof',
      required: true,
      acceptedFormats: ['jpg', 'jpeg', 'png', 'pdf'],
      maxSize: 5,
      status: 'pending'
    },
    {
      id: 'pan',
      name: 'PAN Card',
      description: 'Permanent Account Number',
      required: true,
      acceptedFormats: ['jpg', 'jpeg', 'png', 'pdf'],
      maxSize: 5,
      status: 'pending'
    },
    {
      id: 'bank',
      name: 'Bank Statement',
      description: 'Last 3 months bank statement',
      required: true,
      acceptedFormats: ['pdf'],
      maxSize: 10,
      status: 'pending'
    },
    {
      id: 'income',
      name: 'Income Proof',
      description: 'Salary slip or ITR',
      required: false,
      acceptedFormats: ['jpg', 'jpeg', 'png', 'pdf'],
      maxSize: 5,
      status: 'pending'
    },
    {
      id: 'address',
      name: 'Address Proof',
      description: 'Utility bill or rental agreement',
      required: false,
      acceptedFormats: ['jpg', 'jpeg', 'png', 'pdf'],
      maxSize: 5,
      status: 'pending'
    }
  ])

  const [dragOver, setDragOver] = useState<string | null>(null)
  const [currentStep, setCurrentStep] = useState<'upload' | 'review' | 'complete'>('upload')
  const fileInputRefs = useRef<{ [key: string]: HTMLInputElement | null }>({})
  const { success, error, info } = useToast()

  const handleFileSelect = (documentId: string, file: File) => {
    const document = documents.find(doc => doc.id === documentId)
    if (!document) return

    // Validate file size
    if (file.size > document.maxSize * 1024 * 1024) {
      error('File Too Large', `File size should be less than ${document.maxSize}MB`)
      return
    }

    // Validate file format
    const fileExtension = file.name.split('.').pop()?.toLowerCase()
    if (!fileExtension || !document.acceptedFormats.includes(fileExtension)) {
      error('Invalid Format', `Please upload ${document.acceptedFormats.join(', ')} files only`)
      return
    }

    // Update document
    setDocuments(prev => prev.map(doc =>
      doc.id === documentId
        ? {
            ...doc,
            status: 'uploaded' as const,
            file,
            uploadDate: new Date().toISOString()
          }
        : doc
    ))

    success('Document Uploaded', `${document.name} uploaded successfully`)
  }

  const handleDrop = (e: React.DragEvent, documentId: string) => {
    e.preventDefault()
    setDragOver(null)
    
    const files = Array.from(e.dataTransfer.files)
    if (files.length > 0) {
      handleFileSelect(documentId, files[0])
    }
  }

  const handleDragOver = (e: React.DragEvent, documentId: string) => {
    e.preventDefault()
    setDragOver(documentId)
  }

  const handleDragLeave = () => {
    setDragOver(null)
  }

  const handleFileInput = (documentId: string) => {
    const input = fileInputRefs.current[documentId]
    if (input?.files && input.files[0]) {
      handleFileSelect(documentId, input.files[0])
    }
  }

  const removeDocument = (documentId: string) => {
    setDocuments(prev => prev.map(doc =>
      doc.id === documentId
        ? {
            ...doc,
            status: 'pending' as const,
            file: undefined,
            uploadDate: undefined
          }
        : doc
    ))
    info('Document Removed', 'Document removed successfully')
  }

  const submitForVerification = async () => {
    const requiredDocs = documents.filter(doc => doc.required)
    const uploadedRequiredDocs = requiredDocs.filter(doc => doc.status === 'uploaded')

    if (uploadedRequiredDocs.length < requiredDocs.length) {
      error('Missing Documents', 'Please upload all required documents')
      return
    }

    setCurrentStep('review')
    
    // Simulate verification process
    setTimeout(() => {
      // Mock verification results
      setDocuments(prev => prev.map(doc => 
        doc.status === 'uploaded'
          ? {
              ...doc,
              status: Math.random() > 0.1 ? 'verified' as const : 'rejected' as const,
              rejectionReason: Math.random() > 0.1 ? undefined : 'Document unclear, please reupload'
            }
          : doc
      ))
      setCurrentStep('complete')
    }, 3000)
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'pending': return 'text-slate-400'
      case 'uploaded': return 'text-blue-400'
      case 'verified': return 'text-green-400'
      case 'rejected': return 'text-red-400'
      default: return 'text-slate-400'
    }
  }

  const getStatusBg = (status: string) => {
    switch (status) {
      case 'pending': return 'bg-slate-500/20'
      case 'uploaded': return 'bg-blue-500/20'
      case 'verified': return 'bg-green-500/20'
      case 'rejected': return 'bg-red-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'verified': return CheckCircle
      case 'rejected': return AlertTriangle
      case 'uploaded': return FileText
      default: return Upload
    }
  }

  const allRequiredVerified = documents
    .filter(doc => doc.required)
    .every(doc => doc.status === 'verified')

  if (currentStep === 'review') {
    return (
      <div className="glass-card rounded-2xl p-8 max-w-2xl mx-auto">
        <div className="text-center mb-8">
          <div className="flex items-center justify-center w-16 h-16 mb-4 mx-auto glass-card rounded-2xl">
            <FileText className="w-8 h-8 text-purple-400 animate-pulse" />
          </div>
          <h2 className="text-2xl font-bold text-white mb-2">Verifying Documents</h2>
          <p className="text-slate-400">
            Please wait while we verify your uploaded documents
          </p>
        </div>

        <div className="space-y-4">
          {documents.filter(doc => doc.status !== 'pending').map(doc => (
            <div key={doc.id} className="flex items-center space-x-4 p-4 rounded-xl bg-slate-800/30">
              <div className="loading-dots">
                <div className="loading-dot"></div>
                <div className="loading-dot"></div>
                <div className="loading-dot"></div>
              </div>
              <div className="flex-1">
                <div className="font-medium text-white">{doc.name}</div>
                <div className="text-sm text-slate-400">Verification in progress...</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  if (currentStep === 'complete') {
    return (
      <div className="glass-card rounded-2xl p-8 max-w-2xl mx-auto">
        <div className="text-center mb-8">
          <div className={`flex items-center justify-center w-16 h-16 mb-4 mx-auto glass-card rounded-2xl ${
            allRequiredVerified ? 'border-green-500/50' : 'border-orange-500/50'
          }`}>
            {allRequiredVerified ? (
              <CheckCircle className="w-8 h-8 text-green-400" />
            ) : (
              <AlertTriangle className="w-8 h-8 text-orange-400" />
            )}
          </div>
          <h2 className="text-2xl font-bold text-white mb-2">
            {allRequiredVerified ? 'Verification Complete' : 'Verification Issues'}
          </h2>
          <p className="text-slate-400">
            {allRequiredVerified 
              ? 'Your KYC documents have been successfully verified'
              : 'Some documents need attention. Please check below.'
            }
          </p>
        </div>

        <div className="space-y-4 mb-8">
          {documents.filter(doc => doc.status !== 'pending').map(doc => {
            const StatusIcon = getStatusIcon(doc.status)
            return (
              <div key={doc.id} className={`p-4 rounded-xl border ${
                doc.status === 'rejected' ? 'bg-red-500/10 border-red-500/30' : 'bg-slate-800/30 border-slate-700/50'
              }`}>
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3">
                    <div className={`p-2 rounded-lg ${getStatusBg(doc.status)}`}>
                      <StatusIcon className={`w-4 h-4 ${getStatusColor(doc.status)}`} />
                    </div>
                    <div className="flex-1">
                      <div className="font-medium text-white">{doc.name}</div>
                      <div className={`text-sm ${getStatusColor(doc.status)} capitalize`}>
                        {doc.status}
                      </div>
                      {doc.rejectionReason && (
                        <div className="text-sm text-red-400 mt-1">
                          {doc.rejectionReason}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </div>

        <div className="flex space-x-4">
          {allRequiredVerified ? (
            <button
              onClick={() => onComplete?.(true)}
              className="flex-1 cyber-button py-3 px-6 rounded-xl font-semibold"
            >
              Continue
            </button>
          ) : (
            <>
              <button
                onClick={() => setCurrentStep('upload')}
                className="flex-1 py-3 px-6 rounded-xl font-semibold glass-card text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70"
              >
                Re-upload Documents
              </button>
              <button
                onClick={() => onComplete?.(false)}
                className="flex-1 cyber-button py-3 px-6 rounded-xl font-semibold"
              >
                Continue Anyway
              </button>
            </>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="glass-card rounded-2xl p-8 max-w-4xl mx-auto">
      <div className="text-center mb-8">
        <div className="flex items-center justify-center w-16 h-16 mb-4 mx-auto glass-card rounded-2xl">
          <FileText className="w-8 h-8 text-cyan-400" />
        </div>
        <h2 className="text-2xl font-bold text-white mb-2">KYC Document Upload</h2>
        <p className="text-slate-400">
          Upload your documents to complete the verification process
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {documents.map((document) => {
          const StatusIcon = getStatusIcon(document.status)
          return (
            <div key={document.id} className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-semibold text-white flex items-center">
                    {document.name}
                    {document.required && (
                      <span className="ml-2 text-xs bg-red-500/20 text-red-400 px-2 py-1 rounded">
                        Required
                      </span>
                    )}
                  </h3>
                  <p className="text-sm text-slate-400 mt-1">{document.description}</p>
                </div>
                <div className={`px-3 py-1 rounded-lg text-xs font-medium ${
                  getStatusBg(document.status)} ${getStatusColor(document.status)
                }`}>
                  {document.status}
                </div>
              </div>

              {document.status === 'pending' ? (
                <div
                  className={`border-2 border-dashed rounded-xl p-6 text-center transition-all ${
                    dragOver === document.id
                      ? 'border-purple-500 bg-purple-500/10'
                      : 'border-slate-600 hover:border-purple-500/50'
                  }`}
                  onDrop={(e) => handleDrop(e, document.id)}
                  onDragOver={(e) => handleDragOver(e, document.id)}
                  onDragLeave={handleDragLeave}
                >
                  <Upload className="w-8 h-8 text-slate-400 mx-auto mb-4" />
                  <p className="text-white font-medium mb-2">
                    Drop your file here or{' '}
                    <button
                      onClick={() => fileInputRefs.current[document.id]?.click()}
                      className="text-purple-400 hover:text-purple-300 underline"
                    >
                      browse
                    </button>
                  </p>
                  <p className="text-xs text-slate-400">
                    {document.acceptedFormats.join(', ').toUpperCase()}, max {document.maxSize}MB
                  </p>
                  <input
                    type="file"
                    ref={el => fileInputRefs.current[document.id] = el}
                    onChange={() => handleFileInput(document.id)}
                    accept={document.acceptedFormats.map(format => `.${format}`).join(',')}
                    className="hidden"
                  />
                </div>
              ) : (
                <div className="p-4 rounded-xl bg-slate-800/30 border border-slate-700/50">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <div className={`p-2 rounded-lg ${getStatusBg(document.status)}`}>
                        <StatusIcon className={`w-4 h-4 ${getStatusColor(document.status)}`} />
                      </div>
                      <div className="flex-1">
                        <div className="font-medium text-white">{document.file?.name}</div>
                        <div className="text-sm text-slate-400">
                          {document.file && `${(document.file.size / (1024 * 1024)).toFixed(2)} MB`}
                          {document.uploadDate && ` • ${new Date(document.uploadDate).toLocaleDateString()}`}
                        </div>
                        {document.rejectionReason && (
                          <div className="text-sm text-red-400 mt-1">
                            {document.rejectionReason}
                          </div>
                        )}
                      </div>
                    </div>
                    <div className="flex space-x-2">
                      <button
                        onClick={() => info('Preview', 'Document preview coming soon')}
                        className="p-2 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white transition-colors"
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => removeDocument(document.id)}
                        className="p-2 rounded-lg hover:bg-red-500/20 text-slate-400 hover:text-red-400 transition-colors"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* Additional Upload Options */}
              {document.status === 'pending' && (
                <div className="flex space-x-2">
                  <button
                    onClick={() => fileInputRefs.current[document.id]?.click()}
                    className="flex-1 py-2 px-3 rounded-xl glass-card text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70 text-sm flex items-center justify-center space-x-2"
                  >
                    <Upload className="w-4 h-4" />
                    <span>Browse Files</span>
                  </button>
                  <button
                    onClick={() => info('Camera', 'Camera capture coming soon')}
                    className="py-2 px-3 rounded-xl glass-card text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70 text-sm"
                  >
                    <Camera className="w-4 h-4" />
                  </button>
                </div>
              )}
            </div>
          )
        })}
      </div>

      {/* Progress Summary */}
      <div className="mt-8 p-4 rounded-xl bg-slate-800/30">
        <div className="flex items-center justify-between mb-3">
          <span className="text-sm font-medium text-white">Upload Progress</span>
          <span className="text-sm text-slate-400">
            {documents.filter(doc => doc.status !== 'pending').length} / {documents.length} uploaded
          </span>
        </div>
        <div className="w-full bg-slate-700/30 rounded-full h-2">
          <div 
            className="h-2 bg-gradient-to-r from-purple-500 to-cyan-500 rounded-full transition-all duration-500"
            style={{ 
              width: `${(documents.filter(doc => doc.status !== 'pending').length / documents.length) * 100}%` 
            }}
          />
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex space-x-4 mt-8">
        <button
          onClick={onCancel}
          className="flex-1 py-3 px-6 rounded-xl font-semibold glass-card text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70"
        >
          Skip for Now
        </button>
        <button
          onClick={submitForVerification}
          disabled={!documents.filter(doc => doc.required).every(doc => doc.status !== 'pending')}
          className="flex-1 cyber-button py-3 px-6 rounded-xl font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Submit for Verification
        </button>
      </div>

      {/* Help Text */}
      <div className="mt-6 p-4 rounded-xl bg-blue-500/10 border border-blue-500/30">
        <div className="flex items-start space-x-3">
          <FileText className="w-5 h-5 text-blue-400 mt-0.5" />
          <div className="text-sm text-blue-400">
            <p className="font-medium mb-1">Document Guidelines:</p>
            <ul className="text-blue-300 space-y-1 text-xs">
              <li>• Ensure documents are clear and all text is readable</li>
              <li>• Upload original documents, not photocopies when possible</li>
              <li>• File size should not exceed the specified limit</li>
              <li>• All required documents must be uploaded to proceed</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
}