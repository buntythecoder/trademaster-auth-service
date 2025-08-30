// Unified Trading Page - Single comprehensive trading interface
// Replaces both TradingInterface and EnhancedTradingInterface

import React from 'react'
import { PageLayout } from '../components/layout/PageLayout'
import { UnifiedTradingInterface } from '../components/trading/UnifiedTradingInterface'
import { useAuthStore } from '../stores/auth.store'

export const UnifiedTradingPage: React.FC = () => {
  const { user } = useAuthStore()

  return (
    <PageLayout>
      <UnifiedTradingInterface 
        userId={user?.id || 'demo-user'} 
        defaultSymbol="RELIANCE"
        layout="desktop"
      />
    </PageLayout>
  )
}

export default UnifiedTradingPage