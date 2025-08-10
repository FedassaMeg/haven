import React from 'react';
import { clsx } from 'clsx';

export interface Column<T> {
  key: keyof T | 'actions';
  label: string;
  sortable?: boolean;
  render?: (value: any, row: T, index: number) => React.ReactNode;
  width?: string;
  align?: 'left' | 'center' | 'right';
}

export interface TableProps<T> {
  data: T[];
  columns: Column<T>[];
  loading?: boolean;
  emptyMessage?: string;
  onRowClick?: (row: T, index: number) => void;
  className?: string;
}

export function Table<T extends Record<string, any>>({
  data,
  columns,
  loading = false,
  emptyMessage = 'No data available',
  onRowClick,
  className,
}: TableProps<T>) {
  if (loading) {
    return (
      <div className={clsx('card', className)}>
        <div className="animate-pulse">
          <div className="h-4 bg-secondary-200 rounded w-full mb-4"></div>
          <div className="space-y-2">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-4 bg-secondary-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className={clsx('card', className)}>
        <div className="text-center py-12">
          <p className="text-secondary-500">{emptyMessage}</p>
        </div>
      </div>
    );
  }

  return (
    <div className={clsx('card overflow-hidden', className)}>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-secondary-200">
          <thead className="bg-secondary-50">
            <tr>
              {columns.map((column) => (
                <th
                  key={String(column.key)}
                  className={clsx(
                    'px-6 py-3 text-xs font-medium text-secondary-500 uppercase tracking-wider',
                    column.align === 'center' && 'text-center',
                    column.align === 'right' && 'text-right'
                  )}
                  style={{ width: column.width }}
                >
                  {column.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-secondary-200">
            {data.map((row, index) => (
              <tr
                key={index}
                className={clsx(
                  onRowClick && 'cursor-pointer hover:bg-secondary-50',
                  'transition-colors'
                )}
                onClick={() => onRowClick?.(row, index)}
              >
                {columns.map((column) => (
                  <td
                    key={String(column.key)}
                    className={clsx(
                      'px-6 py-4 whitespace-nowrap text-sm',
                      column.align === 'center' && 'text-center',
                      column.align === 'right' && 'text-right'
                    )}
                  >
                    {column.render
                      ? column.render(
                          column.key === 'actions' ? null : row[column.key],
                          row,
                          index
                        )
                      : column.key === 'actions'
                      ? null
                      : row[column.key]}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  className,
}) => {
  const pages = Array.from({ length: totalPages }, (_, i) => i + 1);
  
  return (
    <div className={clsx('flex items-center justify-between', className)}>
      <div className="flex items-center space-x-2">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          className="btn-outline btn-sm disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Previous
        </button>
        
        <div className="flex items-center space-x-1">
          {pages.map((page) => (
            <button
              key={page}
              onClick={() => onPageChange(page)}
              className={clsx(
                'px-3 py-1 text-sm rounded-md',
                page === currentPage
                  ? 'bg-primary-600 text-white'
                  : 'text-secondary-600 hover:bg-secondary-100'
              )}
            >
              {page}
            </button>
          ))}
        </div>
        
        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="btn-outline btn-sm disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Next
        </button>
      </div>
      
      <p className="text-sm text-secondary-500">
        Page {currentPage} of {totalPages}
      </p>
    </div>
  );
};