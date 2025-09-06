import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import {
  Shield, Lock, Eye, AlertTriangle, CheckCircle, XCircle, Users,
  FileText, Database, Globe, Server, Settings, BarChart3, Clock,
  Target, Zap, Hash, Scan, UserCheck, Key, Fingerprint, Activity
} from 'lucide-react'

interface SecurityComplianceManagementProps {
  initialTab?: 'dashboard' | 'compliance' | 'audits' | 'vulnerabilities' | 'access'
}

export const SecurityComplianceManagement: React.FC<SecurityComplianceManagementProps> = ({ initialTab = 'dashboard' }) => {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'compliance' | 'audits' | 'vulnerabilities' | 'access'>(initialTab)

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-6">
      <div className="mb-8">
        <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-400 to-cyan-400 bg-clip-text text-transparent mb-2">
          Security & Compliance Management
        </h1>
        <p className="text-slate-400 text-lg">
          Comprehensive security monitoring and regulatory compliance management
        </p>
      </div>

      {/* Navigation Tabs */}
      <div className="mb-8">
        <div className="flex flex-wrap gap-2 p-1 bg-slate-800/50 rounded-xl border border-slate-700/50">
          {[
            { id: 'dashboard', label: 'Security Dashboard', icon: Shield },
            { id: 'compliance', label: 'Compliance Status', icon: FileText },
            { id: 'audits', label: 'Security Audits', icon: Scan },
            { id: 'vulnerabilities', label: 'Vulnerabilities', icon: AlertTriangle },
            { id: 'access', label: 'Access Control', icon: Users }
          ].map(tab => {
            const Icon = tab.icon
            return (
              <motion.button
                key={tab.id}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-200 ${
                  activeTab === tab.id
                    ? 'bg-gradient-to-r from-purple-500 to-cyan-500 text-white shadow-lg'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="whitespace-nowrap">{tab.label}</span>
              </motion.button>
            )
          })}
        </div>
      </div>

      {/* Content */}
      <div className="space-y-6">
        {activeTab === 'dashboard' && <SecurityDashboardTab />}
        {activeTab === 'compliance' && <ComplianceTab />}
        {activeTab === 'audits' && <AuditsTab />}
        {activeTab === 'vulnerabilities' && <VulnerabilitiesTab />}
        {activeTab === 'access' && <AccessControlTab />}
      </div>
    </div>
  )
}

const SecurityDashboardTab = () => (
  <div className="space-y-6">
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
            <Shield className="h-6 w-6 text-green-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">98.7%</div>
          </div>
        </div>
        <h3 className="text-green-400 font-semibold mb-1">Security Score</h3>
        <p className="text-slate-400 text-sm">overall security posture</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-red-500/20 to-red-600/20">
            <AlertTriangle className="h-6 w-6 text-red-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">3</div>
          </div>
        </div>
        <h3 className="text-red-400 font-semibold mb-1">Critical Alerts</h3>
        <p className="text-slate-400 text-sm">requires immediate attention</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
            <FileText className="h-6 w-6 text-blue-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">94.2%</div>
          </div>
        </div>
        <h3 className="text-blue-400 font-semibold mb-1">Compliance Rate</h3>
        <p className="text-slate-400 text-sm">regulatory compliance</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
            <Users className="h-6 w-6 text-purple-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">12,847</div>
          </div>
        </div>
        <h3 className="text-purple-400 font-semibold mb-1">Active Sessions</h3>
        <p className="text-slate-400 text-sm">authenticated users</p>
      </div>
    </div>

    <div className="grid gap-6 md:grid-cols-2">
      <div className="glass-card p-6 rounded-2xl">
        <h3 className="text-xl font-bold text-white mb-6 flex items-center">
          <Lock className="w-5 h-5 mr-2 text-yellow-400" />
          Security Compliance Status
        </h3>
        <div className="space-y-4">
          {[
            { name: 'PCI DSS', status: 'compliant', score: '98%', color: 'green' },
            { name: 'GDPR', status: 'compliant', score: '96%', color: 'green' },
            { name: 'SOC 2 Type II', status: 'in_progress', score: '87%', color: 'yellow' },
            { name: 'ISO 27001', status: 'compliant', score: '94%', color: 'green' }
          ].map((compliance) => (
            <div key={compliance.name} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
              <div className="flex items-center space-x-3">
                <div className={`w-3 h-3 rounded-full ${
                  compliance.color === 'green' ? 'bg-green-400' : 'bg-yellow-400'
                }`} />
                <span className="text-white font-medium">{compliance.name}</span>
              </div>
              <div className="text-right">
                <div className="text-white font-semibold">{compliance.score}</div>
                <div className={`text-sm ${
                  compliance.color === 'green' ? 'text-green-400' : 'text-yellow-400'
                }`}>
                  {compliance.status === 'compliant' ? 'Compliant' : 'In Progress'}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <h3 className="text-xl font-bold text-white mb-6 flex items-center">
          <Activity className="w-5 h-5 mr-2 text-red-400" />
          Recent Security Events
        </h3>
        <div className="space-y-4">
          {[
            { type: 'info', message: 'Security scan completed successfully', time: '2 minutes ago' },
            { type: 'warning', message: 'Failed login attempt detected', time: '15 minutes ago' },
            { type: 'success', message: 'SSL certificate renewed', time: '1 hour ago' },
            { type: 'critical', message: 'Suspicious API access pattern', time: '2 hours ago' }
          ].map((event, index) => (
            <div key={index} className="flex items-start space-x-3 p-4 bg-slate-800/30 rounded-xl">
              <div className={`p-1 rounded-full ${
                event.type === 'success' ? 'bg-green-500/20' :
                event.type === 'warning' ? 'bg-yellow-500/20' :
                event.type === 'critical' ? 'bg-red-500/20' :
                'bg-blue-500/20'
              }`}>
                {event.type === 'success' && <CheckCircle className="w-4 h-4 text-green-400" />}
                {event.type === 'warning' && <AlertTriangle className="w-4 h-4 text-yellow-400" />}
                {event.type === 'critical' && <XCircle className="w-4 h-4 text-red-400" />}
                {event.type === 'info' && <Shield className="w-4 h-4 text-blue-400" />}
              </div>
              <div className="flex-1">
                <p className="text-white font-medium">{event.message}</p>
                <p className="text-slate-400 text-sm">{event.time}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  </div>
)

const ComplianceTab = () => (
  <div className="space-y-6">
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">Regulatory Compliance Status</h3>
      <div className="grid gap-6 md:grid-cols-2">
        {[
          { name: 'PCI DSS 4.0', status: 'compliant', lastAudit: '15 days ago', nextAudit: '90 days', score: '98%' },
          { name: 'GDPR', status: 'compliant', lastAudit: '30 days ago', nextAudit: '365 days', score: '96%' },
          { name: 'SOC 2 Type II', status: 'in_progress', lastAudit: '45 days ago', nextAudit: '180 days', score: '87%' },
          { name: 'ISO 27001', status: 'compliant', lastAudit: '60 days ago', nextAudit: '365 days', score: '94%' }
        ].map((compliance) => (
          <div key={compliance.name} className="bg-slate-800/30 rounded-xl p-4">
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-white font-semibold">{compliance.name}</h4>
              <div className={`w-3 h-3 rounded-full ${
                compliance.status === 'compliant' ? 'bg-green-400' : 
                compliance.status === 'in_progress' ? 'bg-yellow-400' : 'bg-red-400'
              }`} />
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-400">Compliance Score</span>
                <span className="text-white font-medium">{compliance.score}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Last Audit</span>
                <span className="text-white">{compliance.lastAudit}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Next Audit</span>
                <span className="text-white">{compliance.nextAudit}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
    
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">Compliance Tasks</h3>
      <div className="space-y-3">
        {[
          { task: 'Update data retention policy documentation', priority: 'high', due: '3 days', assignee: 'Legal Team' },
          { task: 'Quarterly security awareness training', priority: 'medium', due: '7 days', assignee: 'HR Team' },
          { task: 'Annual penetration testing report', priority: 'high', due: '14 days', assignee: 'Security Team' },
          { task: 'GDPR data processing agreement review', priority: 'low', due: '30 days', assignee: 'Legal Team' }
        ].map((task, index) => (
          <div key={index} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div>
              <div className="text-white font-medium">{task.task}</div>
              <div className="text-slate-400 text-sm">Assigned to: {task.assignee}</div>
            </div>
            <div className="text-right">
              <div className={`text-sm font-medium mb-1 ${
                task.priority === 'high' ? 'text-red-400' : 
                task.priority === 'medium' ? 'text-yellow-400' : 'text-green-400'
              }`}>
                {task.priority.toUpperCase()}
              </div>
              <div className="text-slate-400 text-sm">Due in {task.due}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
)

const AuditsTab = () => (
  <div className="space-y-6">
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">Security Audit Trail</h3>
      <div className="space-y-3">
        {[
          { id: 'AUD-001', event: 'Admin login', user: 'admin@trademaster.com', severity: 'low', time: '2 hours ago', ip: '192.168.1.100' },
          { id: 'AUD-002', event: 'Failed authentication', user: 'unknown@attacker.com', severity: 'high', time: '4 hours ago', ip: '203.0.113.1' },
          { id: 'AUD-003', event: 'Permission change', user: 'admin@trademaster.com', severity: 'medium', time: '6 hours ago', ip: '192.168.1.100' },
          { id: 'AUD-004', event: 'System configuration', user: 'system@trademaster.com', severity: 'medium', time: '8 hours ago', ip: '127.0.0.1' },
          { id: 'AUD-005', event: 'Data export', user: 'analyst@trademaster.com', severity: 'medium', time: '12 hours ago', ip: '192.168.1.150' }
        ].map((audit) => (
          <div key={audit.id} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div className="flex items-center space-x-4">
              <div className={`w-2 h-2 rounded-full ${
                audit.severity === 'high' ? 'bg-red-400' :
                audit.severity === 'medium' ? 'bg-yellow-400' : 'bg-green-400'
              }`} />
              <div>
                <div className="text-white font-medium">{audit.event}</div>
                <div className="text-slate-400 text-sm">{audit.user} • {audit.ip}</div>
              </div>
            </div>
            <div className="text-right">
              <div className={`text-sm font-medium mb-1 ${
                audit.severity === 'high' ? 'text-red-400' :
                audit.severity === 'medium' ? 'text-yellow-400' : 'text-green-400'
              }`}>
                {audit.severity.toUpperCase()}
              </div>
              <div className="text-slate-400 text-sm">{audit.time}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
)

const VulnerabilitiesTab = () => (
  <div className="space-y-6">
    <div className="grid gap-6 md:grid-cols-3">
      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-red-500/20 to-red-600/20">
            <AlertTriangle className="h-6 w-6 text-red-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">2</div>
          </div>
        </div>
        <h3 className="text-red-400 font-semibold mb-1">Critical</h3>
        <p className="text-slate-400 text-sm">high priority issues</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-yellow-500/20 to-yellow-600/20">
            <AlertTriangle className="h-6 w-6 text-yellow-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">5</div>
          </div>
        </div>
        <h3 className="text-yellow-400 font-semibold mb-1">Medium</h3>
        <p className="text-slate-400 text-sm">moderate priority</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
            <CheckCircle className="h-6 w-6 text-blue-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">12</div>
          </div>
        </div>
        <h3 className="text-blue-400 font-semibold mb-1">Low</h3>
        <p className="text-slate-400 text-sm">minor issues</p>
      </div>
    </div>

    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">Vulnerability Details</h3>
      <div className="space-y-3">
        {[
          { id: 'CVE-2024-001', title: 'SQL Injection vulnerability in login form', severity: 'critical', status: 'open', age: '2 days' },
          { id: 'CVE-2024-002', title: 'Cross-site scripting in user profile', severity: 'critical', status: 'patching', age: '5 days' },
          { id: 'CVE-2024-003', title: 'Weak password policy enforcement', severity: 'medium', status: 'open', age: '7 days' },
          { id: 'CVE-2024-004', title: 'Outdated dependency with known issues', severity: 'medium', status: 'reviewing', age: '12 days' },
          { id: 'CVE-2024-005', title: 'Missing security headers', severity: 'low', status: 'open', age: '15 days' }
        ].map((vuln) => (
          <div key={vuln.id} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div>
              <div className="text-white font-medium">{vuln.title}</div>
              <div className="text-slate-400 text-sm">{vuln.id} • Age: {vuln.age}</div>
            </div>
            <div className="text-right">
              <div className={`text-sm font-medium mb-1 ${
                vuln.severity === 'critical' ? 'text-red-400' :
                vuln.severity === 'medium' ? 'text-yellow-400' : 'text-blue-400'
              }`}>
                {vuln.severity.toUpperCase()}
              </div>
              <div className="text-slate-400 text-sm capitalize">{vuln.status}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
)

const AccessControlTab = () => (
  <div className="space-y-6">
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">Role-Based Access Control</h3>
      <div className="space-y-4">
        {[
          { role: 'Super Admin', users: 2, permissions: ['All'], color: 'red' },
          { role: 'Admin', users: 5, permissions: ['User Management', 'System Config', 'Reports'], color: 'orange' },
          { role: 'Manager', users: 12, permissions: ['User View', 'Reports'], color: 'yellow' },
          { role: 'Analyst', users: 25, permissions: ['Reports', 'Analytics'], color: 'blue' },
          { role: 'User', users: 12800, permissions: ['Trading', 'Portfolio'], color: 'green' }
        ].map((role) => (
          <div key={role.role} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div>
              <div className="text-white font-semibold">{role.role}</div>
              <div className="text-slate-400 text-sm">{role.permissions.join(', ')}</div>
            </div>
            <div className="text-right">
              <div className="text-white font-semibold">{role.users.toLocaleString()}</div>
              <div className="text-slate-400 text-sm">users assigned</div>
            </div>
          </div>
        ))}
      </div>
    </div>

    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-xl font-bold text-white mb-6">Recent Access Changes</h3>
      <div className="space-y-3">
        {[
          { user: 'john.doe@trademaster.com', action: 'Role changed to Manager', by: 'admin@trademaster.com', time: '2 hours ago' },
          { user: 'jane.smith@trademaster.com', action: 'Permissions updated', by: 'admin@trademaster.com', time: '4 hours ago' },
          { user: 'mike.wilson@trademaster.com', action: 'Access revoked', by: 'admin@trademaster.com', time: '6 hours ago' },
          { user: 'sarah.johnson@trademaster.com', action: 'New user created', by: 'admin@trademaster.com', time: '8 hours ago' }
        ].map((change, index) => (
          <div key={index} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div>
              <div className="text-white font-medium">{change.action}</div>
              <div className="text-slate-400 text-sm">User: {change.user}</div>
              <div className="text-cyan-400 text-sm">By: {change.by}</div>
            </div>
            <div className="text-right">
              <div className="text-slate-400 text-sm">{change.time}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
)

export default SecurityComplianceManagement