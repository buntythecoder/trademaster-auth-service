import React from 'react';
import { AgentDashboard } from '../components/agents';
import { PageLayout } from '../components/layout/PageLayout';

export const AgentDashboardPage: React.FC = () => {
  return (
    <PageLayout>
      <AgentDashboard />
    </PageLayout>
  );
};

export default AgentDashboardPage;