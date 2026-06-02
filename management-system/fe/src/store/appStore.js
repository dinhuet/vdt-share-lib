import { useState } from 'react';
import { ROUTES } from '../utils/constants';

export function useAppStore() {
  const [activeRoute, setActiveRoute] = useState(ROUTES.SHARED_APIS);

  return {
    activeRoute,
    setActiveRoute,
  };
}
