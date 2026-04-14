import React from 'react';

interface Column<T> {
  header: string;
  key: keyof T;
  width?: string;
  render?: (value: any, row: T) => React.ReactNode;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  onRowClick?: (row: T) => void;
  loading?: boolean;
  emptyMessage?: string;
}

export default function DataTable<T extends { id?: number | string }>({
  columns,
  data,
  onRowClick,
  loading,
  emptyMessage = 'No data available',
}: DataTableProps<T>) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr className="border-b border-border">
            {columns.map((col, i) => (
              <th
                key={i}
                className="text-left text-[11px] font-semibold text-text-muted uppercase tracking-[0.5px] py-3 px-4"
                style={{ width: col.width }}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={columns.length} className="text-center py-12 text-text-muted">
                Loading...
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="text-center py-12 text-text-muted">
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((row, i) => (
              <tr
                key={row.id ?? i}
                onClick={() => onRowClick?.(row)}
                className={`border-b border-border last:border-0 ${
                  onRowClick ? 'cursor-pointer hover:bg-bg transition-colors' : ''
                }`}
              >
                {columns.map((col, j) => (
                  <td key={j} className="py-3 px-4 text-[13px] text-text-primary">
                    {col.render ? col.render(row[col.key], row) : String(row[col.key] ?? '')}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
