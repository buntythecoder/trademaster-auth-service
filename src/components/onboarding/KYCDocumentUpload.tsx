import React from 'react'
import { KYCDocuments } from '../auth/KYCDocuments'

interface KYCDocumentUploadProps {
  onComplete: (data: any) => void
  onSkip?: () => void
  className?: string
}

export const KYCDocumentUpload: React.FC<KYCDocumentUploadProps> = ({ 
  onComplete, 
  onSkip, 
  className = '' 
}) => {
  const handleKYCComplete = (verified: boolean) => {
    onComplete({
      verified,
      completedAt: new Date(),
      documents: verified ? ['pan', 'aadhar', 'bank'] : []
    })
  }

  const handleKYCCancel = () => {
    if (onSkip) {
      onSkip()
    } else {
      onComplete({
        verified: false,
        skipped: true,
        completedAt: new Date()
      })
    }
  }

  return (
    <div className={className}>
      <KYCDocuments 
        onComplete={handleKYCComplete}
        onCancel={handleKYCCancel}
      />
    </div>
  )
}

export default KYCDocumentUpload