import { useEffect, useState } from 'react';
import { getRouteFromPath, ROUTE_PATHS, ROUTES } from '../utils/constants';

export function useAppStore() {
  const [activeRoute, setActiveRouteState] = useState(() => getRouteFromPath(window.location.pathname));

  useEffect(() => {
    function handlePopState() {
      setActiveRouteState(getRouteFromPath(window.location.pathname));
    }

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  function setActiveRoute(route) {
    setActiveRouteState(route);

    const nextPath = ROUTE_PATHS[route] || ROUTE_PATHS[ROUTES.SHARED_APIS];
    if (window.location.pathname !== nextPath) {
      window.history.pushState({}, '', nextPath);
    }
  }

  return {
    activeRoute,
    setActiveRoute,
  };
}
