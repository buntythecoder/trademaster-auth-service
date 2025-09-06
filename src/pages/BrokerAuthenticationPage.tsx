import React from 'react'
import BrokerAuthenticationInterface from '../components/trading/BrokerAuthenticationInterface'

export function BrokerAuthenticationPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
      <div className="container mx-auto px-4 py-8">
        <BrokerAuthenticationInterface />
      </div>
    </div>
  )
}

export default BrokerAuthenticationPage