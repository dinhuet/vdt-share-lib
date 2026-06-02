import DefaultConfigsPage from '../features/defaultConfigs/DefaultConfigsPage';
import ExposedApisPage from '../features/exposedApis/ExposedApisPage';
import { ROUTES } from '../utils/constants';

export default function AppRoutes({ activeRoute }) {
  if (activeRoute === ROUTES.DEFAULT_CONFIGS) {
    return <DefaultConfigsPage />;
  }

  return <ExposedApisPage />;
}
