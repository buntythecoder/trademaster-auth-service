import React from 'react';
import { BusinessIntelligenceDashboard } from '../components/analytics';
import { PageLayout } from '../components/layout/PageLayout';

export const BusinessIntelligenceDashboardPage: React.FC = () => {
  return (
    <PageLayout>
      <BusinessIntelligenceDashboard />
    </PageLayout>
  );
};

export default BusinessIntelligenceDashboardPage;