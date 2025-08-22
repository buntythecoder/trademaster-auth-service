import React, { useState, useRef } from 'react'
import { FileText, Download, Calendar, Calculator, TrendingUp, TrendingDown, Filter, Search, Eye, BarChart3 } from 'lucide-react'
import { useToast } from '../../contexts/ToastContext'

interface TaxTransaction {
  id: string
  symbol: string
  type: 'buy' | 'sell'
  quantity: number
  buyPrice: number
  sellPrice?: number
  buyDate: string
  sellDate?: string
  gainLoss?: number
  taxCategory: 'STCG' | 'LTCG' | 'N/A'
  holdingPeriod?: number
  status: 'realized' | 'unrealized'
}

interface TaxSummary {
  financialYear: string
  shortTermGains: number
  longTermGains: number
  shortTermLosses: number
  longTermLosses: number
  netShortTerm: number
  netLongTerm: number
  totalTaxLiability: number
}

interface TaxReportingProps {
  height?: number
}

const mockTransactions: TaxTransaction[] = [
  {
    id: '1',
    symbol: 'RELIANCE',
    type: 'sell',
    quantity: 50,
    buyPrice: 2200.00,
    sellPrice: 2547.30,
    buyDate: '2023-03-15',
    sellDate: '2024-01-10',
    gainLoss: 17365.00,
    taxCategory: 'LTCG',
    holdingPeriod: 301,
    status: 'realized'
  },
  {
    id: '2',
    symbol: 'TCS',
    type: 'sell',
    quantity: 25,
    buyPrice: 3800.00,
    sellPrice: 3642.80,
    buyDate: '2023-11-20',
    sellDate: '2024-01-05',
    gainLoss: -3930.00,
    taxCategory: 'STCG',
    holdingPeriod: 46,
    status: 'realized'
  },
  {
    id: '3',
    symbol: 'HDFCBANK',
    type: 'buy',
    quantity: 100,
    buyPrice: 1567.25,
    buyDate: '2023-12-01',
    taxCategory: 'N/A',
    status: 'unrealized'
  },
  {
    id: '4',
    symbol: 'INFY',
    type: 'sell',
    quantity: 75,
    buyPrice: 1350.00,
    sellPrice: 1423.60,
    buyDate: '2023-06-10',
    sellDate: '2024-01-12',
    gainLoss: 5520.00,
    taxCategory: 'LTCG',
    holdingPeriod: 216,
    status: 'realized'
  }
]

const mockTaxSummary: TaxSummary = {
  financialYear: 'FY 2023-24',
  shortTermGains: 2500.00,
  longTermGains: 22885.00,
  shortTermLosses: -6430.00,
  longTermLosses: 0,
  netShortTerm: -3930.00,
  netLongTerm: 22885.00,
  totalTaxLiability: 1288.50 // 10% on LTCG above 1 lakh
}

export function TaxReporting({ height = 600 }: TaxReportingProps) {
  const [transactions] = useState<TaxTransaction[]>(mockTransactions)
  const [taxSummary] = useState<TaxSummary>(mockTaxSummary)
  const [selectedYear, setSelectedYear] = useState<string>('FY 2023-24')
  const [filterType, setFilterType] = useState<'all' | 'realized' | 'unrealized'>('all')
  const [searchSymbol, setSearchSymbol] = useState<string>('')
  const [activeTab, setActiveTab] = useState<'summary' | 'transactions' | 'reports'>('summary')
  const reportRef = useRef<HTMLDivElement>(null)
  const { success, info, error } = useToast()

  const filteredTransactions = transactions.filter(tx => {
    const matchesFilter = filterType === 'all' || tx.status === filterType
    const matchesSearch = searchSymbol === '' || tx.symbol.toLowerCase().includes(searchSymbol.toLowerCase())
    return matchesFilter && matchesSearch
  })

  const generateReport = (format: 'pdf' | 'excel' | 'csv') => {
    success(`${format.toUpperCase()} Generated`, `Tax report has been generated in ${format.toUpperCase()} format`)
  }

  const exportTransactions = () => {
    info('Export Complete', 'Transaction data exported successfully')
  }

  const viewTaxCalculation = (transactionId: string) => {
    info('Tax Calculation', 'Detailed tax calculation shown')
  }

  const getTaxCategoryColor = (category: string) => {
    switch (category) {
      case 'STCG': return 'text-orange-400'
      case 'LTCG': return 'text-green-400'
      default: return 'text-slate-400'
    }
  }

  const getTaxCategoryBg = (category: string) => {
    switch (category) {
      case 'STCG': return 'bg-orange-500/20'
      case 'LTCG': return 'bg-green-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const formatCurrency = (amount: number) => {
    return `₹${Math.abs(amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center">
            <Calculator className="w-5 h-5 mr-2 text-cyan-400" />
            Tax Reporting
          </h3>
          <p className="text-sm text-slate-400 mt-1">
            Capital gains calculation and tax reporting for {selectedYear}
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <select
            value={selectedYear}
            onChange={(e) => setSelectedYear(e.target.value)}
            className="cyber-input px-3 py-2 text-sm rounded-xl"
          >
            <option value="FY 2023-24">FY 2023-24</option>
            <option value="FY 2022-23">FY 2022-23</option>
            <option value="FY 2021-22">FY 2021-22</option>
          </select>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="flex space-x-2 mb-6">
        {(['summary', 'transactions', 'reports'] as const).map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 text-sm rounded-xl transition-colors capitalize ${
              activeTab === tab
                ? 'bg-purple-500/20 text-purple-400'
                : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {activeTab === 'summary' && (
        <div className="space-y-6">
          {/* Tax Summary Cards */}
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <div className="glass-card p-4 rounded-xl border border-green-500/30">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm text-slate-400">Long Term Gains</div>
                  <div className="text-xl font-bold text-green-400">{formatCurrency(taxSummary.longTermGains)}</div>
                </div>
                <TrendingUp className="w-6 h-6 text-green-400/50" />
              </div>
            </div>
            
            <div className="glass-card p-4 rounded-xl border border-orange-500/30">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm text-slate-400">Short Term Gains</div>
                  <div className="text-xl font-bold text-orange-400">{formatCurrency(taxSummary.shortTermGains)}</div>
                </div>
                <TrendingDown className="w-6 h-6 text-orange-400/50" />
              </div>
            </div>
            
            <div className="glass-card p-4 rounded-xl border border-red-500/30">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm text-slate-400">Total Losses</div>
                  <div className="text-xl font-bold text-red-400">{formatCurrency(taxSummary.shortTermLosses)}</div>
                </div>
                <TrendingDown className="w-6 h-6 text-red-400/50" />
              </div>
            </div>
            
            <div className="glass-card p-4 rounded-xl border border-purple-500/30">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm text-slate-400">Tax Liability</div>
                  <div className="text-xl font-bold text-purple-400">{formatCurrency(taxSummary.totalTaxLiability)}</div>
                </div>
                <Calculator className="w-6 h-6 text-purple-400/50" />
              </div>
            </div>
          </div>

          {/* Tax Breakdown */}
          <div className="grid gap-6 lg:grid-cols-2">
            <div className="glass-card p-6 rounded-xl">
              <h4 className="font-semibold text-white mb-4">Tax Calculation Breakdown</h4>
              <div className="space-y-4">
                <div className="flex justify-between items-center p-3 rounded-lg bg-slate-800/30">
                  <span className="text-slate-300">Long Term Capital Gains</span>
                  <span className="font-mono text-green-400">{formatCurrency(taxSummary.netLongTerm)}</span>
                </div>
                <div className="flex justify-between items-center p-3 rounded-lg bg-slate-800/30">
                  <span className="text-slate-300">LTCG Tax (10% above ₹1L)</span>
                  <span className="font-mono text-orange-400">₹{((taxSummary.netLongTerm - 100000) * 0.1).toLocaleString('en-IN')}</span>
                </div>
                <div className="flex justify-between items-center p-3 rounded-lg bg-slate-800/30">
                  <span className="text-slate-300">Short Term Losses (Set-off)</span>
                  <span className="font-mono text-red-400">{formatCurrency(taxSummary.netShortTerm)}</span>
                </div>
                <div className="border-t border-slate-700/50 pt-3">
                  <div className="flex justify-between items-center font-semibold">
                    <span className="text-white">Total Tax Liability</span>
                    <span className="font-mono text-purple-400">{formatCurrency(taxSummary.totalTaxLiability)}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="glass-card p-6 rounded-xl">
              <h4 className="font-semibold text-white mb-4">Tax Optimization Tips</h4>
              <div className="space-y-3 text-sm">
                <div className="p-3 rounded-lg bg-green-500/10 border border-green-500/30">
                  <div className="font-medium text-green-400 mb-1">Hold for Long Term</div>
                  <div className="text-green-300">Hold investments for more than 1 year to qualify for LTCG (10% tax)</div>
                </div>
                
                <div className="p-3 rounded-lg bg-blue-500/10 border border-blue-500/30">
                  <div className="font-medium text-blue-400 mb-1">Loss Harvesting</div>
                  <div className="text-blue-300">Book losses to offset gains and reduce tax liability</div>
                </div>
                
                <div className="p-3 rounded-lg bg-purple-500/10 border border-purple-500/30">
                  <div className="font-medium text-purple-400 mb-1">LTCG Exemption</div>
                  <div className="text-purple-300">First ₹1 lakh of LTCG is tax-free in each financial year</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'transactions' && (
        <div className="space-y-6">
          {/* Filters */}
          <div className="flex flex-wrap items-center gap-4">
            <div className="flex items-center space-x-2">
              <Search className="w-4 h-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search symbol..."
                value={searchSymbol}
                onChange={(e) => setSearchSymbol(e.target.value)}
                className="cyber-input px-3 py-2 text-sm rounded-xl w-40"
              />
            </div>
            
            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value as any)}
              className="cyber-input px-3 py-2 text-sm rounded-xl"
            >
              <option value="all">All Transactions</option>
              <option value="realized">Realized Only</option>
              <option value="unrealized">Unrealized Only</option>
            </select>

            <button
              onClick={exportTransactions}
              className="cyber-button px-4 py-2 text-sm rounded-xl flex items-center space-x-2"
            >
              <Download className="w-4 h-4" />
              <span>Export</span>
            </button>
          </div>

          {/* Transactions Table */}
          <div className="glass-card rounded-xl p-4">
            <div className="space-y-3">
              {filteredTransactions.map((transaction) => (
                <div key={transaction.id} className="p-4 bg-slate-800/30 rounded-xl hover:bg-slate-700/30 transition-colors">
                  <div className="grid gap-4 md:grid-cols-2">
                    {/* Left Column */}
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <div className="font-semibold text-white text-lg">{transaction.symbol}</div>
                        <div className={`px-2 py-1 rounded text-xs font-medium ${
                          getTaxCategoryBg(transaction.taxCategory)} ${getTaxCategoryColor(transaction.taxCategory)
                        }`}>
                          {transaction.taxCategory}
                        </div>
                      </div>
                      
                      <div className="grid grid-cols-2 gap-4 text-sm">
                        <div>
                          <div className="text-slate-400">Type</div>
                          <div className={`capitalize font-medium ${
                            transaction.type === 'buy' ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {transaction.type}
                          </div>
                        </div>
                        <div>
                          <div className="text-slate-400">Quantity</div>
                          <div className="text-white font-medium">{transaction.quantity}</div>
                        </div>
                      </div>
                    </div>

                    {/* Right Column */}
                    <div className="space-y-3">
                      <div className="grid grid-cols-2 gap-4 text-sm">
                        <div>
                          <div className="text-slate-400">Buy Price</div>
                          <div className="font-mono text-white">₹{transaction.buyPrice.toFixed(2)}</div>
                        </div>
                        <div>
                          <div className="text-slate-400">Sell Price</div>
                          <div className="font-mono text-white">
                            {transaction.sellPrice ? `₹${transaction.sellPrice.toFixed(2)}` : '-'}
                          </div>
                        </div>
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="text-slate-400 text-sm">Gain/Loss</div>
                          <div className={`font-mono font-semibold ${
                            transaction.gainLoss && transaction.gainLoss > 0 ? 'text-green-400' : 
                            transaction.gainLoss && transaction.gainLoss < 0 ? 'text-red-400' : 'text-slate-400'
                          }`}>
                            {transaction.gainLoss ? 
                              `${transaction.gainLoss > 0 ? '+' : ''}₹${transaction.gainLoss.toFixed(2)}` : '-'
                            }
                          </div>
                        </div>
                        
                        {transaction.status === 'realized' && (
                          <button
                            onClick={() => viewTaxCalculation(transaction.id)}
                            className="p-2 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white transition-colors"
                            title="View tax calculation"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {filteredTransactions.length === 0 && (
            <div className="text-center py-8">
              <FileText className="w-12 h-12 text-slate-500 mx-auto mb-4" />
              <p className="text-slate-400">No transactions found</p>
              <p className="text-slate-500 text-sm">Try adjusting your filters</p>
            </div>
          )}
        </div>
      )}

      {activeTab === 'reports' && (
        <div className="space-y-6">
          {/* Report Generation */}
          <div className="grid gap-6 lg:grid-cols-2">
            <div className="glass-card p-6 rounded-xl">
              <h4 className="font-semibold text-white mb-4 flex items-center">
                <FileText className="w-5 h-5 mr-2 text-cyan-400" />
                Generate Tax Reports
              </h4>
              <div className="space-y-4">
                <button
                  onClick={() => generateReport('pdf')}
                  className="w-full py-3 px-4 rounded-xl glass-card text-slate-400 hover:text-white transition-colors flex items-center justify-center space-x-3"
                >
                  <FileText className="w-5 h-5" />
                  <span>Download PDF Report</span>
                  <Download className="w-4 h-4" />
                </button>
                
                <button
                  onClick={() => generateReport('excel')}
                  className="w-full py-3 px-4 rounded-xl glass-card text-slate-400 hover:text-white transition-colors flex items-center justify-center space-x-3"
                >
                  <BarChart3 className="w-5 h-5" />
                  <span>Download Excel Report</span>
                  <Download className="w-4 h-4" />
                </button>
                
                <button
                  onClick={() => generateReport('csv')}
                  className="w-full py-3 px-4 rounded-xl glass-card text-slate-400 hover:text-white transition-colors flex items-center justify-center space-x-3"
                >
                  <FileText className="w-5 h-5" />
                  <span>Download CSV Data</span>
                  <Download className="w-4 h-4" />
                </button>
              </div>
            </div>

            <div className="glass-card p-6 rounded-xl">
              <h4 className="font-semibold text-white mb-4">Report Features</h4>
              <div className="space-y-3 text-sm">
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 rounded-full bg-green-400 mt-2"></div>
                  <div>
                    <div className="text-white font-medium">Comprehensive Tax Summary</div>
                    <div className="text-slate-400">STCG, LTCG calculations with tax liability</div>
                  </div>
                </div>
                
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 rounded-full bg-blue-400 mt-2"></div>
                  <div>
                    <div className="text-white font-medium">Transaction Details</div>
                    <div className="text-slate-400">Complete buy/sell history with dates</div>
                  </div>
                </div>
                
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 rounded-full bg-purple-400 mt-2"></div>
                  <div>
                    <div className="text-white font-medium">ITR Ready Format</div>
                    <div className="text-slate-400">Data formatted for income tax return filing</div>
                  </div>
                </div>
                
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 rounded-full bg-orange-400 mt-2"></div>
                  <div>
                    <div className="text-white font-medium">CA/Advisor Friendly</div>
                    <div className="text-slate-400">Professional format for tax consultants</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Important Information */}
          <div className="p-6 rounded-xl bg-yellow-500/10 border border-yellow-500/30">
            <h4 className="font-semibold text-white mb-4 flex items-center">
              <Calculator className="w-5 h-5 mr-2 text-yellow-400" />
              Important Tax Information
            </h4>
            <div className="space-y-3 text-sm text-yellow-300">
              <div>
                <strong>Tax Rates (FY 2023-24):</strong>
                <ul className="mt-2 space-y-1 ml-4">
                  <li>• Short Term Capital Gains (STCG): Taxed at your income slab rate</li>
                  <li>• Long Term Capital Gains (LTCG): 10% on gains above ₹1 lakh (no indexation)</li>
                  <li>• Securities Transaction Tax (STT) is already deducted by broker</li>
                </ul>
              </div>
              
              <div>
                <strong>Holding Period:</strong>
                <ul className="mt-2 space-y-1 ml-4">
                  <li>• Equity shares: &gt;12 months for LTCG, ≤12 months for STCG</li>
                  <li>• Equity mutual funds: &gt;12 months for LTCG, ≤12 months for STCG</li>
                </ul>
              </div>
              
              <div className="text-yellow-400">
                <strong>Disclaimer:</strong> This is for informational purposes only. 
                Please consult a qualified CA or tax advisor for professional advice.
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}