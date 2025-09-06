// Enhanced Trading Page - FRONT-002 Implementation
// Complete trading interface with mock profile system

import React from 'react'
import { PageLayout } from '../components/layout/PageLayout'
import { EnhancedTradingInterface } from '../components/trading/EnhancedTradingInterface/EnhancedTradingInterface'

export const EnhancedTradingPage: React.FC = () => {
  return (
    <PageLayout>
      <EnhancedTradingInterface />
    </PageLayout>
  )
}

export default EnhancedTradingPage