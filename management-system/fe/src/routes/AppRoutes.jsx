import DefaultConfigsPage from '../features/defaultConfigs/DefaultConfigsPage';
import ClientApisPage from '../features/clientApis/ClientApisPage';
import ClientsPage from '../features/clients/ClientsPage';
import ExposedApisPage from '../features/exposedApis/ExposedApisPage';
import { ROUTES } from '../utils/constants';

export default function AppRoutes({ activeRoute }) {
  if (activeRoute === ROUTES.CLIENTS) {
    return <ClientsPage />;
  }

  if (activeRoute === ROUTES.CLIENT_APIS) {
    return <ClientApisPage />;
  }

  if (activeRoute === ROUTES.DEFAULT_CONFIGS) {
    return <DefaultConfigsPage />;
  }

  return <ExposedApisPage />;
}
