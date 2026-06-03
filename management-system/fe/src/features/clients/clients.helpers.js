export function filterClients(clients, filters) {
  const search = filters.search?.trim().toLowerCase();
  return clients.filter((client) => {
    const matchesStatus = !filters.status || client.status === filters.status;
    const searchable = [client.name, client.clientCode, client.email, client.description]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    const matchesSearch = !search || searchable.includes(search);
    return matchesStatus && matchesSearch;
  });
}

export function clientToForm(client) {
  return {
    name: client?.name || '',
    clientCode: client?.clientCode || '',
    email: client?.email || '',
    description: client?.description || '',
  };
}

export function clientFormToPayload(form) {
  return {
    name: form.name.trim(),
    clientCode: form.clientCode.trim(),
    email: form.email.trim() || null,
    description: form.description.trim() || null,
  };
}

export function clientStatusTone(status) {
  if (status === 'ACTIVE') return 'success';
  if (status === 'INACTIVE') return 'warning';
  if (status === 'REVOKED') return 'danger';
  return 'neutral';
}
