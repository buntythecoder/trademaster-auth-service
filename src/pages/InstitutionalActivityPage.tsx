import React from 'react';
import { InstitutionalActivityInterface } from '../components/institutional';
import { PageLayout } from '../components/layout/PageLayout';

export const InstitutionalActivityPage: React.FC = () => {
  return (
    <PageLayout>
      <InstitutionalActivityInterface />
    </PageLayout>
  );
};

export default InstitutionalActivityPage;