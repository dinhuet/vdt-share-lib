import EmptyState from './EmptyState';

export default function DataTable({ columns, rows, renderRow, emptyTitle, emptyDescription }) {
  return (
    <div className="table-shell">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((column) => <th key={column}>{column}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows.map(renderRow)}
        </tbody>
      </table>
      {rows.length === 0 ? (
        <EmptyState title={emptyTitle || 'No data'} description={emptyDescription || 'Try refreshing or changing filters.'} />
      ) : null}
    </div>
  );
}
