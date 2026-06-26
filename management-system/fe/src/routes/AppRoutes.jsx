import DashboardPage from '../features/dashboard/DashboardPage';
import DefaultConfigsPage from '../features/defaultConfigs/DefaultConfigsPage';
import AccessPoliciesPage from '../features/accessPolicies/AccessPoliciesPage';
import ClientApisPage from '../features/clientApis/ClientApisPage';
import ClientsPage from '../features/clients/ClientsPage';
import ExposedApisPage from '../features/exposedApis/ExposedApisPage';
import SecurityAlertsPage from '../features/securityAlerts/SecurityAlertsPage';
import { ROUTES } from '../utils/constants';

export default function AppRoutes({ activeRoute }) {
  if (activeRoute === ROUTES.DASHBOARD) {
    return <DashboardPage />;
  }

  if (activeRoute === ROUTES.CLIENTS) {
    return <ClientsPage />;
  }

  if (activeRoute === ROUTES.CLIENT_APIS) {
    return <ClientApisPage />;
  }

  if (activeRoute === ROUTES.DEFAULT_CONFIGS) {
    return <DefaultConfigsPage />;
  }

  if (activeRoute === ROUTES.ACCESS_POLICIES) {
    return <AccessPoliciesPage />;
  }

  if (activeRoute === ROUTES.SECURITY_ALERTS) {
    return <SecurityAlertsPage />;
  }

  return <ExposedApisPage />;
}
