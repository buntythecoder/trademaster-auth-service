import React, { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Calculator, 
  Download, 
  FileText, 
  TrendingUp, 
  TrendingDown, 
  AlertCircle,
  DollarSign,
  Calendar,
  Target,
  Info
} from 'lucide-react'
import { PortfolioData } from '../../hooks/usePortfolioWebSocket'
import { useToast } from '../../contexts/ToastContext'
import { cn } from '../../lib/utils'

interface TaxSummaryProps {
  portfolio: PortfolioData | null
}

interface TaxHarvestingOpportunity {
  symbol: string
  unrealizedLoss: number
  taxSaving: number
  recommendation: string
  confidence: number
}

export function TaxSummary({ portfolio }: TaxSummaryProps) {
  const [selectedTaxYear, setSelectedTaxYear] = useState('2024-25')
  const [showHarvestingDetails, setShowHarvestingDetails] = useState(false)
  const { success, info } = useToast()

  const taxYears = ['2024-25', '2023-24', '2022-23', '2021-22']

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount)
  }

  const calculateTaxLiability = () => {
    if (!portfolio) return { stcgTax: 0, ltcgTax: 0, totalTax: 0 }

    const stcgRate = 0.15 // 15% for STCG
    const ltcgRate = 0.10 // 10% for LTCG above ₹1 lakh
    const ltcgExemption = 100000 // ₹1 lakh exemption

    const stcgTax = Math.max(0, portfolio.taxInfo.netShortTerm * stcgRate)
    const ltcgTaxable = Math.max(0, portfolio.taxInfo.netLongTerm - ltcgExemption)
    const ltcgTax = ltcgTaxable * ltcgRate
    const totalTax = stcgTax + ltcgTax

    return { stcgTax, ltcgTax, totalTax }
  }

  const getHarvestingOpportunities = (): TaxHarvestingOpportunity[] => {
    if (!portfolio) return []

    // Mock tax harvesting opportunities based on holdings
    return [
      {
        symbol: 'HDFC',
        unrealizedLoss: -8500,
        taxSaving: 2550,
        recommendation: 'Sell to realize loss and offset gains',
        confidence: 85
      },
      {
        symbol: 'WIPRO',
        unrealizedLoss: -5200,
        taxSaving: 1560,
        recommendation: 'Consider selling before year-end',
        confidence: 70
      },
      {
        symbol: 'ONGC',
        unrealizedLoss: -3100,
        taxSaving: 930,
        recommendation: 'Small tax benefit available',
        confidence: 60
      }
    ].filter(opp => opp.confidence > 65)
  }

  const generateTaxReport = () => {
    info('Tax Report', 'Generating comprehensive tax report...')
    // Mock download after delay
    setTimeout(() => {
      success('Download Complete', 'Tax report has been generated and downloaded')
    }, 2000)
  }

  const downloadITRHelper = () => {
    info('ITR Helper', 'Preparing ITR filing assistance document...')
    setTimeout(() => {
      success('Download Ready', 'ITR helper document is ready for download')
    }, 1500)
  }

  if (!portfolio) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-6 bg-slate-700 rounded w-1/3"></div>
          <div className="grid grid-cols-2 gap-4">
            <div className="h-24 bg-slate-700 rounded"></div>
            <div className="h-24 bg-slate-700 rounded"></div>
          </div>
          <div className="h-32 bg-slate-700 rounded"></div>
        </div>
      </div>
    )
  }

  const taxLiability = calculateTaxLiability()
  const harvestingOpportunities = getHarvestingOpportunities()
  const totalHarvestingSavings = harvestingOpportunities.reduce((sum, opp) => sum + opp.taxSaving, 0)

  return (
    <div className="glass-card rounded-2xl p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="p-2 rounded-lg bg-orange-500/20">
            <Calculator className="w-5 h-5 text-orange-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-white">
              Tax Summary FY {selectedTaxYear}
            </h3>
            <p className="text-sm text-slate-400">Capital gains and tax planning insights</p>
          </div>
        </div>

        {/* Controls */}
        <div className="flex items-center space-x-3">
          <select
            value={selectedTaxYear}
            onChange={(e) => setSelectedTaxYear(e.target.value)}
            className="cyber-input px-3 py-2 text-sm rounded-xl"
          >
            {taxYears.map(year => (
              <option key={year} value={year}>FY {year}</option>
            ))}
          </select>

          <button
            onClick={generateTaxReport}
            className="cyber-button px-4 py-2 text-sm rounded-xl flex items-center space-x-2"
          >
            <Download className="w-4 h-4" />
            <span>Export</span>
          </button>
        </div>
      </div>

      {/* Capital Gains Summary */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Short Term Capital Gains */}
        <div className="glass-card p-6 rounded-xl">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <TrendingUp className="w-5 h-5 text-red-400" />
              <h4 className="text-white font-semibold">Short Term Capital Gains</h4>
            </div>
            <div className="text-xs text-slate-400">Holding &lt; 1 year</div>
          </div>

          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Realized Gains</span>
              <span className="text-green-400 font-medium">
                {formatCurrency(portfolio.taxInfo.shortTermGains)}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Realized Losses</span>
              <span className="text-red-400 font-medium">
                {formatCurrency(portfolio.taxInfo.shortTermLosses)}
              </span>
            </div>
            <div className="pt-3 border-t border-slate-700/50">
              <div className="flex justify-between items-center">
                <span className="text-white font-medium">Net STCG</span>
                <span className={cn(
                  "text-lg font-bold",
                  portfolio.taxInfo.netShortTerm >= 0 ? "text-green-400" : "text-red-400"
                )}>
                  {formatCurrency(portfolio.taxInfo.netShortTerm)}
                </span>
              </div>
              <div className="flex justify-between items-center mt-2">
                <span className="text-slate-400 text-sm">Tax @ 15%</span>
                <span className="text-orange-400 font-medium">
                  {formatCurrency(taxLiability.stcgTax)}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Long Term Capital Gains */}
        <div className="glass-card p-6 rounded-xl">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <TrendingUp className="w-5 h-5 text-green-400" />
              <h4 className="text-white font-semibold">Long Term Capital Gains</h4>
            </div>
            <div className="text-xs text-slate-400">Holding ≥ 1 year</div>
          </div>

          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Realized Gains</span>
              <span className="text-green-400 font-medium">
                {formatCurrency(portfolio.taxInfo.longTermGains)}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Realized Losses</span>
              <span className="text-red-400 font-medium">
                {formatCurrency(portfolio.taxInfo.longTermLosses)}
              </span>
            </div>
            <div className="pt-3 border-t border-slate-700/50">
              <div className="flex justify-between items-center">
                <span className="text-white font-medium">Net LTCG</span>
                <span className={cn(
                  "text-lg font-bold",
                  portfolio.taxInfo.netLongTerm >= 0 ? "text-green-400" : "text-red-400"
                )}>
                  {formatCurrency(portfolio.taxInfo.netLongTerm)}
                </span>
              </div>
              <div className="flex justify-between items-center mt-1">
                <span className="text-slate-400 text-sm">Exemption</span>
                <span className="text-blue-400 font-medium">₹1,00,000</span>
              </div>
              <div className="flex justify-between items-center mt-2">
                <span className="text-slate-400 text-sm">Tax @ 10%</span>
                <span className="text-orange-400 font-medium">
                  {formatCurrency(taxLiability.ltcgTax)}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Tax Summary */}
      <div className="glass-card p-6 rounded-xl">
        <h4 className="text-white font-semibold mb-4 flex items-center space-x-2">
          <DollarSign className="w-5 h-5 text-green-400" />
          <span>Tax Liability Summary</span>
        </h4>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="text-center">
            <div className="text-2xl font-bold text-orange-400 mb-1">
              {formatCurrency(taxLiability.totalTax)}
            </div>
            <div className="text-sm text-slate-400">Total Tax Due</div>
          </div>
          
          <div className="text-center">
            <div className="text-2xl font-bold text-green-400 mb-1">
              {formatCurrency(portfolio.taxInfo.dividendIncome)}
            </div>
            <div className="text-sm text-slate-400">Dividend Income</div>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-cyan-400 mb-1">
              {formatCurrency(totalHarvestingSavings)}
            </div>
            <div className="text-sm text-slate-400">Potential Tax Savings</div>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-purple-400 mb-1">
              {formatCurrency(taxLiability.totalTax - totalHarvestingSavings)}
            </div>
            <div className="text-sm text-slate-400">Net Tax (After Optimization)</div>
          </div>
        </div>
      </div>

      {/* Tax Harvesting Opportunities */}
      {harvestingOpportunities.length > 0 && (
        <div className="glass-card p-6 rounded-xl">
          <div className="flex items-center justify-between mb-4">
            <h4 className="text-white font-semibold flex items-center space-x-2">
              <Target className="w-5 h-5 text-cyan-400" />
              <span>Tax Harvesting Opportunities</span>
            </h4>
            <button
              onClick={() => setShowHarvestingDetails(!showHarvestingDetails)}
              className="text-cyan-400 hover:text-cyan-300 text-sm flex items-center space-x-1"
            >
              <Info className="w-4 h-4" />
              <span>{showHarvestingDetails ? 'Hide Details' : 'Show Details'}</span>
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            {harvestingOpportunities.slice(0, 3).map((opportunity, index) => (
              <motion.div
                key={opportunity.symbol}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
                className="bg-blue-500/10 border border-blue-500/20 rounded-lg p-4"
              >
                <div className="flex items-center justify-between mb-2">
                  <div className="font-medium text-white">{opportunity.symbol}</div>
                  <div className={cn(
                    "px-2 py-1 rounded text-xs",
                    opportunity.confidence >= 80 ? "bg-green-500/20 text-green-400" :
                    opportunity.confidence >= 70 ? "bg-yellow-500/20 text-yellow-400" :
                    "bg-orange-500/20 text-orange-400"
                  )}>
                    {opportunity.confidence}% confidence
                  </div>
                </div>
                <div className="space-y-1 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Loss</span>
                    <span className="text-red-400">{formatCurrency(opportunity.unrealizedLoss)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Tax Saving</span>
                    <span className="text-green-400">{formatCurrency(opportunity.taxSaving)}</span>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>

          <AnimatePresence>
            {showHarvestingDetails && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="space-y-3"
              >
                {harvestingOpportunities.map((opportunity, index) => (
                  <div
                    key={opportunity.symbol}
                    className="bg-slate-800/30 border border-slate-700/50 rounded-lg p-4"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="font-medium text-white">{opportunity.symbol}</div>
                      <div className="text-green-400 font-medium">
                        Save {formatCurrency(opportunity.taxSaving)}
                      </div>
                    </div>
                    <div className="text-slate-300 text-sm mb-2">
                      {opportunity.recommendation}
                    </div>
                    <div className="text-xs text-slate-400">
                      Unrealized loss: {formatCurrency(opportunity.unrealizedLoss)}
                    </div>
                  </div>
                ))}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex flex-col sm:flex-row gap-3">
        <button
          onClick={generateTaxReport}
          className="flex-1 cyber-button py-3 rounded-xl flex items-center justify-center space-x-2"
        >
          <FileText className="w-4 h-4" />
          <span>Generate Tax Report</span>
        </button>
        
        <button
          onClick={downloadITRHelper}
          className="flex-1 glass-card py-3 rounded-xl flex items-center justify-center space-x-2 text-slate-400 hover:text-white transition-colors"
        >
          <Download className="w-4 h-4" />
          <span>Download ITR Helper</span>
        </button>
      </div>

      {/* Tax Tips */}
      <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
        <div className="flex items-start space-x-3">
          <AlertCircle className="w-5 h-5 text-amber-400 mt-0.5 flex-shrink-0" />
          <div className="space-y-2">
            <div className="text-amber-400 font-medium text-sm">Tax Planning Tips</div>
            <div className="text-amber-300 text-sm space-y-1">
              <div>• Consider harvesting losses before March 31st to offset gains</div>
              <div>• Hold equity investments for more than 1 year to benefit from LTCG exemption</div>
              <div>• Dividend income is taxable as per your income tax slab</div>
              <div>• Maintain proper records of all buy/sell transactions</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}