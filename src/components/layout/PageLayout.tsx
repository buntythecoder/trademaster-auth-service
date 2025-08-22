import React from 'react'
import { Navigation } from './Navigation'

interface PageLayoutProps {
  children: React.ReactNode
  title?: string
  showWelcome?: boolean
  className?: string
}

export function PageLayout({ 
  children, 
  title = "TradeMaster", 
  showWelcome = false,
  className = ""
}: PageLayoutProps) {
  return (
    <div className="min-h-screen bg-slate-900">
      <Navigation title={title} showWelcome={showWelcome} />
      <main className={`container mx-auto px-6 py-8 ${className}`}>
        {children}
      </main>
    </div>
  )
}