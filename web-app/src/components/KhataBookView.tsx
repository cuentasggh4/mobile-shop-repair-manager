import { useMemo, useState } from 'react';
import {
  AlertCircle,
  BookOpen,
  CheckCircle2,
  ChevronRight,
  Phone,
  Search,
  Users,
  X,
} from 'lucide-react';
import { KhataCustomerSummary } from '../services/khataEngine';
import { digitsOnly } from '../types/repairShop';

export type KhataFilterMode = 'ALL' | 'PENDING_ONLY';

interface KhataBookViewProps {
  customers: KhataCustomerSummary[];
  currencySymbol: string;
  onSelectCustomer: (customerKey: string) => void;
}

export function KhataBookView({
  customers,
  currencySymbol,
  onSelectCustomer,
}: KhataBookViewProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterMode, setFilterMode] = useState<KhataFilterMode>('ALL');

  const summary = useMemo(() => {
    let totalMarketDues = 0;
    let totalCollected = 0;
    let pendingCustomersCount = 0;

    for (const c of customers) {
      totalCollected += c.totalPaid;
      if (c.totalPendingDue > 0.01) {
        totalMarketDues += c.totalPendingDue;
        pendingCustomersCount += 1;
      }
    }

    return {
      totalMarketDues: Math.round(totalMarketDues * 100) / 100,
      totalCollected: Math.round(totalCollected * 100) / 100,
      pendingCustomersCount,
      totalCustomersCount: customers.length,
    };
  }, [customers]);

  const filteredCustomers = useMemo(() => {
    const raw = searchQuery.trim().toLowerCase();
    const queryDigits = digitsOnly(searchQuery);

    return customers.filter((customer) => {
      if (filterMode === 'PENDING_ONLY' && customer.totalPendingDue <= 0.01) {
        return false;
      }
      if (!raw) return true;

      if (
        customer.customerName.toLowerCase().includes(raw) ||
        customer.customerPhone.toLowerCase().includes(raw)
      ) {
        return true;
      }

      if (queryDigits.length >= 2) {
        const phoneDigits = digitsOnly(customer.customerPhone);
        if (phoneDigits.includes(queryDigits)) {
          return true;
        }
      }

      return false;
    });
  }, [customers, filterMode, searchQuery]);

  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-100 text-blue-700 dark:bg-[#2563EB]/20 dark:text-blue-400">
              <BookOpen className="h-5 w-5" />
            </div>
            <div>
              <h1 className="font-display text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
                Khata Book (Customer Ledger)
              </h1>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Customer-wise billing, collected payments, and pending credit balances derived from active repair jobs
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Summary Banner */}
      <section
        aria-label="Khata Book Market Dues Summary"
        className="grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:grid-cols-3 dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none"
      >
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 dark:border-red-500/30 dark:bg-red-500/10">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-medium text-red-700 dark:text-red-200">Total Market Dues</span>
            <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400 shrink-0" />
          </div>
          <p className="mt-2 font-mono-tabular text-3xl font-bold tracking-tight text-red-800 dark:text-red-300">
            {currencySymbol}
            {summary.totalMarketDues.toLocaleString()}
          </p>
          <p className="mt-1 text-xs text-red-700/80 dark:text-red-200/80">
            Across{' '}
            <strong className="font-mono-tabular text-red-900 dark:text-red-100">
              {summary.pendingCustomersCount}
            </strong>{' '}
            {summary.pendingCustomersCount === 1 ? 'customer' : 'customers'} with pending dues
          </p>
        </div>

        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 dark:border-emerald-500/25 dark:bg-emerald-500/10">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-medium text-emerald-700 dark:text-emerald-200">Total Paid / Collected</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-600 dark:text-emerald-400 shrink-0" />
          </div>
          <p className="mt-2 font-mono-tabular text-3xl font-bold tracking-tight text-emerald-800 dark:text-emerald-300">
            {currencySymbol}
            {summary.totalCollected.toLocaleString()}
          </p>
          <p className="mt-1 text-xs text-emerald-700/80 dark:text-emerald-200/80">
            Excludes cancelled repair tickets
          </p>
        </div>

        <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700/80 dark:bg-[#0F172A]/80">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-medium text-slate-600 dark:text-slate-300">Tracked Customers</span>
            <Users className="h-4 w-4 text-blue-600 dark:text-blue-400 shrink-0" />
          </div>
          <p className="mt-2 font-mono-tabular text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
            {summary.totalCustomersCount}
          </p>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            Auto-grouped by mobile number or name
          </p>
        </div>
      </section>

      {/* Search & Filter Controls */}
      <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
        <div className="relative flex-1 sm:max-w-md">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search customer by name or phone digits..."
            aria-label="Search Khata customers"
            className="h-10 w-full rounded-xl border border-slate-300 bg-slate-50 pl-10 pr-9 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-100 dark:placeholder:text-slate-500"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              aria-label="Clear customer search"
              className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-800 dark:hover:bg-slate-800 dark:hover:text-slate-100"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => setFilterMode('ALL')}
            className={`rounded-xl border px-3.5 py-2 text-xs font-semibold transition-colors whitespace-nowrap ${
              filterMode === 'ALL'
                ? 'border-blue-600 bg-[#2563EB] text-white'
                : 'border-slate-300 bg-slate-50 text-slate-700 hover:border-slate-400 hover:text-slate-900 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-slate-600 dark:hover:text-white'
            }`}
          >
            All Customers ({summary.totalCustomersCount})
          </button>
          <button
            type="button"
            onClick={() => setFilterMode('PENDING_ONLY')}
            className={`rounded-xl border px-3.5 py-2 text-xs font-semibold transition-colors whitespace-nowrap ${
              filterMode === 'PENDING_ONLY'
                ? 'border-red-600 bg-red-600 text-white'
                : 'border-slate-300 bg-slate-50 text-slate-700 hover:border-red-400 hover:text-red-700 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-300 dark:hover:border-red-500/40 dark:hover:text-red-200'
            }`}
          >
            Pending Dues Only ({summary.pendingCustomersCount})
          </button>
        </div>
      </div>

      {/* Customer List: Empty State, Desktop Table & Mobile Cards */}
      {filteredCustomers.length === 0 ? (
        <div className="rounded-2xl border border-slate-200 bg-white px-6 py-14 text-center shadow-sm dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
          <BookOpen className="mx-auto h-10 w-10 text-slate-400 dark:text-slate-500" />
          <h3 className="mt-3 font-display text-base font-semibold text-slate-800 dark:text-slate-200">
            {customers.length === 0
              ? 'No customer records yet'
              : 'No matching customers found'}
          </h3>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            {customers.length === 0
              ? 'Create repair jobs or connect Google Sheets to build the Khata Book ledger.'
              : 'Try a different customer name, mobile number, or switch the filter pill.'}
          </p>
        </div>
      ) : (
        <>
          {/* Desktop Table */}
          <div className="hidden overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm md:block dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none">
            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50 text-[11px] font-semibold uppercase tracking-wider text-slate-500 dark:border-slate-800 dark:bg-[#0F172A]/70 dark:text-slate-400">
                    <th className="px-5 py-3.5">Customer</th>
                    <th className="px-4 py-3.5">Phone</th>
                    <th className="px-4 py-3.5 text-center">Visits</th>
                    <th className="px-4 py-3.5 text-right">Total Billed</th>
                    <th className="px-4 py-3.5 text-right">Total Paid</th>
                    <th className="px-4 py-3.5 text-right">Balance Status</th>
                    <th className="w-10 px-4 py-3.5" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-800/80">
                  {filteredCustomers.map((customer) => {
                    const hasDue = customer.totalPendingDue > 0.01;
                    return (
                      <tr
                        key={customer.customerKey}
                        onClick={() => onSelectCustomer(customer.customerKey)}
                        className="cursor-pointer transition-colors hover:bg-slate-50 dark:hover:bg-slate-800/50"
                      >
                        <td className="px-5 py-4">
                          <div className="font-semibold text-sm text-slate-900 dark:text-slate-100">
                            {customer.customerName}
                          </div>
                          <div className="mt-0.5 font-mono-tabular text-[11px] text-slate-500 dark:text-slate-400">
                            Last visit:{' '}
                            {customer.lastVisitAt
                              ? new Date(customer.lastVisitAt).toLocaleDateString()
                              : '—'}
                          </div>
                        </td>
                        <td className="px-4 py-4 font-mono-tabular text-slate-700 dark:text-slate-300">
                          {customer.customerPhone ? (
                            <span className="inline-flex items-center gap-1.5">
                              <Phone className="h-3 w-3 text-slate-400" />
                              {customer.customerPhone}
                            </span>
                          ) : (
                            <span className="text-slate-400 dark:text-slate-500">No phone</span>
                          )}
                        </td>
                        <td className="px-4 py-4 text-center">
                          <span className="inline-flex items-center rounded-lg border border-slate-200 bg-slate-100 px-2.5 py-1 font-mono-tabular text-xs font-semibold text-slate-700 dark:border-slate-700 dark:bg-[#0F172A] dark:text-slate-200">
                            {customer.totalJobsCount}{' '}
                            {customer.totalJobsCount === 1 ? 'visit' : 'visits'}
                          </span>
                        </td>
                        <td className="px-4 py-4 text-right font-mono-tabular text-slate-800 dark:text-slate-200">
                          {currencySymbol}
                          {customer.totalBilled.toLocaleString()}
                        </td>
                        <td className="px-4 py-4 text-right font-mono-tabular text-emerald-700 dark:text-emerald-300">
                          {currencySymbol}
                          {customer.totalPaid.toLocaleString()}
                        </td>
                        <td className="px-4 py-4 text-right">
                          {hasDue ? (
                            <span className="inline-flex items-center gap-1.5 rounded-lg border border-red-200 bg-red-100 px-2.5 py-1 font-mono-tabular text-xs font-semibold text-red-800 dark:border-red-500/40 dark:bg-red-500/15 dark:text-red-300">
                              Due: {currencySymbol}
                              {customer.totalPendingDue.toLocaleString()}
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-800 dark:border-emerald-500/35 dark:bg-emerald-500/15 dark:text-emerald-300">
                              <CheckCircle2 className="h-3 w-3" />
                              Settled
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-4 text-right text-slate-400">
                          <ChevronRight className="h-4 w-4" />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Mobile / Compact Cards */}
          <div className="grid grid-cols-1 gap-3 md:hidden">
            {filteredCustomers.map((customer) => {
              const hasDue = customer.totalPendingDue > 0.01;
              return (
                <button
                  key={customer.customerKey}
                  type="button"
                  onClick={() => onSelectCustomer(customer.customerKey)}
                  className="w-full rounded-2xl border border-slate-200 bg-white p-4 text-left shadow-sm transition-colors hover:border-blue-500/50 dark:border-slate-800 dark:bg-[#1E293B] dark:shadow-none dark:hover:border-blue-500/40"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <h3 className="truncate text-sm font-bold text-slate-900 dark:text-slate-100">
                        {customer.customerName}
                      </h3>
                      <p className="mt-0.5 font-mono-tabular text-xs text-slate-500 dark:text-slate-400">
                        {customer.customerPhone || 'No phone'} · {customer.totalJobsCount}{' '}
                        {customer.totalJobsCount === 1 ? 'visit' : 'visits'}
                      </p>
                    </div>
                    {hasDue ? (
                      <span className="shrink-0 rounded-lg border border-red-200 bg-red-100 px-2.5 py-1 font-mono-tabular text-xs font-semibold text-red-800 dark:border-red-500/40 dark:bg-red-500/15 dark:text-red-300">
                        Due: {currencySymbol}
                        {customer.totalPendingDue.toLocaleString()}
                      </span>
                    ) : (
                      <span className="shrink-0 inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-800 dark:border-emerald-500/35 dark:bg-emerald-500/15 dark:text-emerald-300">
                        <CheckCircle2 className="h-3 w-3" />
                        Settled
                      </span>
                    )}
                  </div>

                  <div className="mt-3 grid grid-cols-2 gap-2 border-t border-slate-200 pt-3 text-xs dark:border-slate-800">
                    <div>
                      <span className="text-slate-500 dark:text-slate-400">Billed: </span>
                      <span className="font-mono-tabular font-semibold text-slate-800 dark:text-slate-200">
                        {currencySymbol}
                        {customer.totalBilled.toLocaleString()}
                      </span>
                    </div>
                    <div className="text-right">
                      <span className="text-slate-500 dark:text-slate-400">Paid: </span>
                      <span className="font-mono-tabular font-semibold text-emerald-700 dark:text-emerald-300">
                        {currencySymbol}
                        {customer.totalPaid.toLocaleString()}
                      </span>
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
