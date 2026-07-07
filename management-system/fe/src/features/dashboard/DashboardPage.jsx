const kibanaDashboardUrl = (
  window.__APP_CONFIG__?.KIBANA_DASHBOARD_URL || import.meta.env.VITE_KIBANA_DASHBOARD_URL || ''
).trim();

function getDashboardUrlError(url) {
  if (!url) {
    return 'Kibana dashboard URL is not configured. Set KIBANA_DASHBOARD_URL for the frontend container.';
  }

  try {
    const parsedUrl = new URL(url);
    if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
      return 'Kibana dashboard URL must use http or https.';
    }
  } catch (err) {
    return 'Kibana dashboard URL is invalid. Please check KIBANA_DASHBOARD_URL.';
  }

  return '';
}

export default function DashboardPage() {
  const urlError = getDashboardUrlError(kibanaDashboardUrl);

  return (
    <div className="page-content dashboard-page">
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p>Monitor platform security and traffic metrics from Kibana.</p>
        </div>
      </div>

      {urlError ? (
        <div className="notice">{urlError}</div>
      ) : (
        <section className="dashboard-frame-card" aria-label="Kibana dashboard">
          <iframe
            title="Kibana dashboard"
            src={kibanaDashboardUrl}
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
            allowFullScreen
          />
        </section>
      )}
    </div>
  );
}
