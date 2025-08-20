import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';

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

const tableContainerVariants = cva(
  'card',
  {
    variants: {
      overflow: {
        true: 'overflow-hidden',
        false: '',
      },
    },
    defaultVariants: {
      overflow: false,
    },
  }
);

const tableHeaderCellVariants = cva(
  'px-6 py-3 text-xs font-medium text-secondary-500 uppercase tracking-wider',
  {
    variants: {
      align: {
        left: 'text-left',
        center: 'text-center',
        right: 'text-right',
      },
    },
    defaultVariants: {
      align: 'left',
    },
  }
);

const tableDataCellVariants = cva(
  'px-6 py-4 whitespace-nowrap text-sm',
  {
    variants: {
      align: {
        left: 'text-left',
        center: 'text-center',
        right: 'text-right',
      },
    },
    defaultVariants: {
      align: 'left',
    },
  }
);

const tableRowVariants = cva(
  'transition-colors',
  {
    variants: {
      clickable: {
        true: 'cursor-pointer hover:bg-secondary-50',
        false: '',
      },
    },
    defaultVariants: {
      clickable: false,
    },
  }
);

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
      <div className={tableContainerVariants({ className })}>
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
      <div className={tableContainerVariants({ className })}>
        <div className="text-center py-12">
          <p className="text-secondary-500">{emptyMessage}</p>
        </div>
      </div>
    );
  }

  return (
    <div className={tableContainerVariants({ overflow: true, className })}>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-secondary-200">
          <thead className="bg-secondary-50">
            <tr>
              {columns.map((column) => (
                <th
                  key={String(column.key)}
                  className={tableHeaderCellVariants({ align: column.align })}
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
                className={tableRowVariants({ clickable: !!onRowClick })}
                onClick={() => onRowClick?.(row, index)}
              >
                {columns.map((column) => (
                  <td
                    key={String(column.key)}
                    className={tableDataCellVariants({ align: column.align })}
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

const paginationContainerVariants = cva(
  'flex items-center justify-between',
  {
    variants: {},
    defaultVariants: {},
  }
);

const paginationButtonVariants = cva(
  'px-3 py-1 text-sm rounded-md',
  {
    variants: {
      active: {
        true: 'bg-primary-600 text-white',
        false: 'text-secondary-600 hover:bg-secondary-100',
      },
    },
    defaultVariants: {
      active: false,
    },
  }
);

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  className,
}) => {
  const pages = Array.from({ length: totalPages }, (_, i) => i + 1);
  
  return (
    <div className={paginationContainerVariants({ className })}>
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
              className={paginationButtonVariants({ active: page === currentPage })}
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