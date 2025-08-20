import React, { useState } from 'react'
import { 
  User, 
  Camera, 
  Shield, 
  CreditCard, 
  Bell, 
  Settings, 
  Lock, 
  Eye, 
  EyeOff,
  Upload,
  Edit3,
  Phone,
  Mail,
  MapPin,
  Calendar,
  Briefcase,
  TrendingUp,
  CheckCircle,
  AlertTriangle,
  Clock,
  FileText,
  Download,
  Trash2
} from 'lucide-react'

interface ProfileData {
  personalInfo: {
    firstName: string
    lastName: string
    email: string
    phone: string
    dateOfBirth: string
    address: {
      street: string
      city: string
      state: string
      zipCode: string
      country: string
    }
  }
  professionalInfo: {
    occupation: string
    employer: string
    experience: string
    annualIncome: string
  }
  tradingProfile: {
    riskTolerance: string
    investmentGoals: string[]
    tradingExperience: string
    preferredInvestment: string
  }
  documents: {
    id: string
    name: string
    type: string
    status: 'pending' | 'approved' | 'rejected'
    uploadDate: string
    size: string
  }[]
}

export function ProfileDashboard() {
  const [activeTab, setActiveTab] = useState('personal')
  const [editMode, setEditMode] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  
  const [profileData, setProfileData] = useState<ProfileData>({
    personalInfo: {
      firstName: 'John',
      lastName: 'Trader',
      email: 'trader@trademaster.com',
      phone: '+91 98765 43210',
      dateOfBirth: '1990-01-15',
      address: {
        street: '123 Trading Street',
        city: 'Mumbai',
        state: 'Maharashtra',
        zipCode: '400001',
        country: 'India'
      }
    },
    professionalInfo: {
      occupation: 'Software Engineer',
      employer: 'Tech Solutions Pvt Ltd',
      experience: '5-10 years',
      annualIncome: '₹15-25 Lakhs'
    },
    tradingProfile: {
      riskTolerance: 'moderate',
      investmentGoals: ['Long-term Growth', 'Dividend Income'],
      tradingExperience: 'intermediate',
      preferredInvestment: 'equity'
    },
    documents: [
      {
        id: '1',
        name: 'Aadhaar Card.pdf',
        type: 'Identity Proof',
        status: 'approved',
        uploadDate: '2024-01-15',
        size: '2.1 MB'
      },
      {
        id: '2', 
        name: 'PAN Card.pdf',
        type: 'Tax Document',
        status: 'approved',
        uploadDate: '2024-01-15',
        size: '1.8 MB'
      },
      {
        id: '3',
        name: 'Bank Statement.pdf',
        type: 'Income Proof',
        status: 'pending',
        uploadDate: '2024-01-20',
        size: '5.2 MB'
      },
      {
        id: '4',
        name: 'Address Proof.pdf',
        type: 'Address Proof',
        status: 'rejected',
        uploadDate: '2024-01-18',
        size: '3.1 MB'
      }
    ]
  })

  const tabs = [
    { id: 'personal', label: 'Personal Info', icon: <User className="w-4 h-4" /> },
    { id: 'professional', label: 'Professional', icon: <Briefcase className="w-4 h-4" /> },
    { id: 'trading', label: 'Trading Profile', icon: <TrendingUp className="w-4 h-4" /> },
    { id: 'documents', label: 'Documents', icon: <FileText className="w-4 h-4" /> },
    { id: 'security', label: 'Security', icon: <Shield className="w-4 h-4" /> },
    { id: 'preferences', label: 'Preferences', icon: <Settings className="w-4 h-4" /> }
  ]

  const getDocumentStatus = (status: string) => {
    switch(status) {
      case 'approved':
        return { icon: <CheckCircle className="w-4 h-4 text-green-400" />, color: 'text-green-400', bg: 'bg-green-500/20' }
      case 'rejected':
        return { icon: <AlertTriangle className="w-4 h-4 text-red-400" />, color: 'text-red-400', bg: 'bg-red-500/20' }
      case 'pending':
        return { icon: <Clock className="w-4 h-4 text-yellow-400" />, color: 'text-yellow-400', bg: 'bg-yellow-500/20' }
      default:
        return { icon: <Clock className="w-4 h-4 text-gray-400" />, color: 'text-gray-400', bg: 'bg-gray-500/20' }
    }
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold gradient-text mb-4">Profile Center</h1>
        <p className="text-slate-400 text-lg">
          Manage your personal information, trading preferences, and account security
        </p>
      </div>

      {/* Profile Summary Card */}
      <div className="glass-card p-8 rounded-2xl">
        <div className="flex items-center space-x-6 mb-8">
          {/* Profile Picture */}
          <div className="relative">
            <div className="w-24 h-24 rounded-full bg-gradient-to-br from-purple-500/20 to-cyan-500/20 flex items-center justify-center border-2 border-purple-500/50">
              <User className="w-10 h-10 text-purple-400" />
            </div>
            <button className="absolute bottom-0 right-0 w-8 h-8 bg-purple-500 hover:bg-purple-600 rounded-full flex items-center justify-center transition-colors">
              <Camera className="w-4 h-4 text-white" />
            </button>
          </div>
          
          {/* Profile Info */}
          <div className="flex-1">
            <h2 className="text-2xl font-bold text-white mb-2">
              {profileData.personalInfo.firstName} {profileData.personalInfo.lastName}
            </h2>
            <p className="text-slate-400 mb-2">{profileData.personalInfo.email}</p>
            <div className="flex items-center space-x-4 text-sm text-slate-400">
              <span className="flex items-center">
                <Phone className="w-4 h-4 mr-2" />
                {profileData.personalInfo.phone}
              </span>
              <span className="flex items-center">
                <MapPin className="w-4 h-4 mr-2" />
                {profileData.personalInfo.address.city}, {profileData.personalInfo.address.state}
              </span>
            </div>
          </div>
          
          {/* Action Button */}
          <button 
            onClick={() => setEditMode(!editMode)}
            className={`cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2 ${editMode ? 'opacity-75' : ''}`}
          >
            <Edit3 className="w-4 h-4" />
            <span>{editMode ? 'Save Changes' : 'Edit Profile'}</span>
          </button>
        </div>

        {/* KYC Status Overview */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-slate-800/30 rounded-xl p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-slate-400 text-sm">KYC Status</span>
              <CheckCircle className="w-4 h-4 text-green-400" />
            </div>
            <div className="text-lg font-bold text-white">75% Complete</div>
            <div className="text-xs text-slate-400">3 of 4 documents approved</div>
          </div>
          
          <div className="bg-slate-800/30 rounded-xl p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-slate-400 text-sm">Account Level</span>
              <TrendingUp className="w-4 h-4 text-purple-400" />
            </div>
            <div className="text-lg font-bold text-white">Verified</div>
            <div className="text-xs text-slate-400">Full trading access</div>
          </div>
          
          <div className="bg-slate-800/30 rounded-xl p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-slate-400 text-sm">Member Since</span>
              <Calendar className="w-4 h-4 text-cyan-400" />
            </div>
            <div className="text-lg font-bold text-white">Jan 2024</div>
            <div className="text-xs text-slate-400">Active trader</div>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="glass-card p-2 rounded-2xl">
        <div className="flex flex-wrap gap-2">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center space-x-2 px-4 py-3 rounded-xl font-medium transition-all duration-300 ${
                activeTab === tab.id
                  ? 'bg-gradient-to-r from-purple-500/30 to-cyan-500/30 text-white border border-purple-500/50'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800/30'
              }`}
            >
              {tab.icon}
              <span className="hidden sm:block">{tab.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Tab Content */}
      <div className="glass-card p-8 rounded-2xl">
        {activeTab === 'personal' && (
          <div className="space-y-6">
            <h3 className="text-xl font-bold text-white mb-6 flex items-center">
              <User className="w-5 h-5 mr-2 text-purple-400" />
              Personal Information
            </h3>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">First Name</label>
                <input
                  type="text"
                  value={profileData.personalInfo.firstName}
                  disabled={!editMode}
                  className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Last Name</label>
                <input
                  type="text"
                  value={profileData.personalInfo.lastName}
                  disabled={!editMode}
                  className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Email Address</label>
                <input
                  type="email"
                  value={profileData.personalInfo.email}
                  disabled={!editMode}
                  className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Phone Number</label>
                <input
                  type="tel"
                  value={profileData.personalInfo.phone}
                  disabled={!editMode}
                  className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Date of Birth</label>
                <input
                  type="date"
                  value={profileData.personalInfo.dateOfBirth}
                  disabled={!editMode}
                  className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                />
              </div>
            </div>
            
            <div className="mt-8">
              <h4 className="text-lg font-semibold text-white mb-4">Address Information</h4>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-slate-300 mb-2">Street Address</label>
                  <input
                    type="text"
                    value={profileData.personalInfo.address.street}
                    disabled={!editMode}
                    className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">City</label>
                  <input
                    type="text"
                    value={profileData.personalInfo.address.city}
                    disabled={!editMode}
                    className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">State</label>
                  <input
                    type="text"
                    value={profileData.personalInfo.address.state}
                    disabled={!editMode}
                    className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">ZIP Code</label>
                  <input
                    type="text"
                    value={profileData.personalInfo.address.zipCode}
                    disabled={!editMode}
                    className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Country</label>
                  <input
                    type="text"
                    value={profileData.personalInfo.address.country}
                    disabled={!editMode}
                    className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'documents' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-white flex items-center">
                <FileText className="w-5 h-5 mr-2 text-cyan-400" />
                Document Management
              </h3>
              <button className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2">
                <Upload className="w-4 h-4" />
                <span>Upload Document</span>
              </button>
            </div>
            
            <div className="space-y-4">
              {profileData.documents.map((doc) => {
                const status = getDocumentStatus(doc.status)
                return (
                  <div key={doc.id} className="flex items-center justify-between p-4 rounded-xl bg-slate-800/30 hover:bg-slate-700/30 transition-colors">
                    <div className="flex items-center space-x-4">
                      <div className={`p-2 rounded-lg ${status.bg}`}>
                        <FileText className="w-5 h-5 text-slate-400" />
                      </div>
                      <div>
                        <h4 className="font-medium text-white">{doc.name}</h4>
                        <p className="text-sm text-slate-400">{doc.type} • {doc.size} • {doc.uploadDate}</p>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-4">
                      <div className={`flex items-center space-x-2 px-3 py-1 rounded-full ${status.bg}`}>
                        {status.icon}
                        <span className={`text-sm font-medium ${status.color}`}>
                          {doc.status.charAt(0).toUpperCase() + doc.status.slice(1)}
                        </span>
                      </div>
                      
                      <div className="flex items-center space-x-2">
                        <button className="p-2 text-slate-400 hover:text-purple-400 transition-colors">
                          <Download className="w-4 h-4" />
                        </button>
                        <button className="p-2 text-slate-400 hover:text-red-400 transition-colors">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )}

        {activeTab === 'security' && (
          <div className="space-y-6">
            <h3 className="text-xl font-bold text-white mb-6 flex items-center">
              <Shield className="w-5 h-5 mr-2 text-red-400" />
              Security Settings
            </h3>
            
            <div className="space-y-6">
              {/* Password Change */}
              <div className="p-6 rounded-xl bg-slate-800/30">
                <h4 className="text-lg font-semibold text-white mb-4">Change Password</h4>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Current Password</label>
                    <div className="relative">
                      <input
                        type={showPassword ? 'text' : 'password'}
                        className="cyber-input w-full px-4 py-3 pr-12 rounded-xl text-white placeholder-slate-400"
                        placeholder="Enter current password"
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-4 top-1/2 transform -translate-y-1/2 text-slate-400 hover:text-white"
                      >
                        {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                      </button>
                    </div>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">New Password</label>
                    <input
                      type="password"
                      className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                      placeholder="Enter new password"
                    />
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Confirm New Password</label>
                    <input
                      type="password"
                      className="cyber-input w-full px-4 py-3 rounded-xl text-white placeholder-slate-400"
                      placeholder="Confirm new password"
                    />
                  </div>
                  
                  <button className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2">
                    <Lock className="w-4 h-4" />
                    <span>Update Password</span>
                  </button>
                </div>
              </div>
              
              {/* Two-Factor Authentication */}
              <div className="p-6 rounded-xl bg-slate-800/30">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h4 className="text-lg font-semibold text-white">Two-Factor Authentication</h4>
                    <p className="text-slate-400 text-sm">Add an extra layer of security to your account</p>
                  </div>
                  <div className="relative inline-block w-12 mr-2 align-middle select-none">
                    <input type="checkbox" name="toggle" id="toggle-2fa" className="cyber-input focus:outline-none focus:shadow-outline" />
                    <label htmlFor="toggle-2fa" className="block overflow-hidden h-6 rounded-full bg-gray-300 cursor-pointer"></label>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}