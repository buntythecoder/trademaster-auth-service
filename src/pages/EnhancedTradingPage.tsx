// Enhanced Trading Page - FRONT-002 Implementation
// Complete trading interface with mock profile system

import React from 'react'
import { PageLayout } from '../components/layout/PageLayout'
import { EnhancedTradingInterface } from '../components/trading/EnhancedTradingInterface'
import { useAuthStore } from '../stores/auth.store'

export const EnhancedTradingPage: React.FC = () => {
  const { user } = useAuthStore()

  return (
    <PageLayout>
      <EnhancedTradingInterface 
        userId={user?.id || 'demo-user'} 
        defaultSymbol="RELIANCE" 
      />
    </PageLayout>
  )
}

export default EnhancedTradingPage