import React from 'react';
import { GamificationDashboard } from '../components/gamification';
import { PageLayout } from '../components/layout/PageLayout';

export const GamificationDashboardPage: React.FC = () => {
  return (
    <PageLayout>
      <GamificationDashboard />
    </PageLayout>
  );
};

export default GamificationDashboardPage;