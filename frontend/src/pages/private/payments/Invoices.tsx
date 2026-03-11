import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyDebts, type TenantDebtDTO } from '../../../service/billing.service'
import { getMyHomeSnapshot, type ApartmentHomeDTO } from '../../../service/apartment.service'

/* â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

const fmtCurrency = (v: number) =>
  new Intl.NumberFormat('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)

const fmtDate = (v?: string) => {
  if (!v) return 'â€”'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v
  return new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short', year: 'numeric' }).format(d)
}

/** Returns days until due. Negative = overdue. */
function daysUntil(dateStr?: string): number | null {
  if (!dateStr) return null
  const due = new Date(dateStr)
  if (Number.isNaN(due.getTime())) return null
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  due.setHours(0, 0, 0, 0)
  return Math.round((due.getTime() - now.getTime()) / 86_400_000)
}

function urgencyMeta(days: number | null) {
  if (days === null) return { label: '', color: 'text-gray-400', badge: 'bg-gray-100 text-gray-600', urgent: false }
  if (days < 0) return { label: 'Vencido', color: 'text-red-600', badge: 'bg-red-50 text-red-600', urgent: true }
  if (days === 0) return { label: 'Vence hoy', color: 'text-orange-500', badge: 'bg-orange-50 text-orange-600', urgent: true }
  if (days <= 3) return { label: `Vence en ${days}d`, color: 'text-orange-500', badge: 'bg-orange-50 text-orange-600', urgent: true }
  if (days <= 7) return { label: `Vence en ${days}d`, color: 'text-amber-500', badge: 'bg-amber-50 text-amber-600', urgent: false }
  return { label: `Vence en ${days}d`, color: 'text-gray-400', badge: 'bg-gray-100 text-gray-500', urgent: false }
}

/** Map reference keywords â†’ icon config */
function conceptIcon(ref: string) {
  const r = ref.toLowerCase()
  if (r.includes('alquiler')) return { emoji: 'ðŸ ', bg: 'bg-violet-100' }
  if (r.includes('luz') || r.includes('electric')) return { emoji: 'âš¡', bg: 'bg-amber-100' }
  if (r.includes('agua')) return { emoji: 'ðŸ’§', bg: 'bg-sky-100' }
  if (r.includes('gas')) return { emoji: 'ðŸ”¥', bg: 'bg-orange-100' }
  if (r.includes('internet') || r.includes('wifi')) return { emoji: 'ðŸ“¶', bg: 'bg-indigo-100' }
  if (r.includes('comunidad')) return { emoji: 'ðŸ¢', bg: 'bg-teal-100' }
  if (r.includes('seguro')) return { emoji: 'ðŸ›¡ï¸', bg: 'bg-emerald-100' }
  if (r.includes('limpieza')) return { emoji: 'ðŸ§¹', bg: 'bg-cyan-100' }
  return { emoji: 'ðŸ“„', bg: 'bg-gray-100' }
}

/* â”€â”€ component â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

type Tab = 'pending' | 'history'

export default function Invoices() {
  const navigate = useNavigate()
  const [debts, setDebts] = useState<TenantDebtDTO[]>([])
  const [homeData, setHomeData] = useState<ApartmentHomeDTO | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [tab, setTab] = useState<Tab>('pending')

  useEffect(() => {
    const load = async () => {
      const [d, h] = await Promise.all([getMyDebts(), getMyHomeSnapshot()])
      setDebts(Array.isArray(d) ? d : [])
      setHomeData(h)
      setIsLoading(false)
    }
    void load()
  }, [])

  const pending = useMemo(
    () =>
      debts
        .filter((d) => d.status === 'PENDING')
        .sort((a, b) => {
          const da = daysUntil(a.bill?.duDate) ?? 999
          const db = daysUntil(b.bill?.duDate) ?? 999
          return da - db
        }),
    [debts],
  )

  const history = useMemo(() => debts.filter((d) => d.status === 'PAID'), [debts])

  const totalPending = useMemo(
    () => pending.reduce((s, d) => s + Number(d.amount ?? 0), 0),
    [pending],
  )

  const locationPill = homeData?.apartment
    ? `${homeData.apartment.title} Â· ${homeData.apartment.ubication}`
    : null

  /* â”€â”€ urgent / normal split â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
  const urgentDebts = pending.filter((d) => {
    const days = daysUntil(d.bill?.duDate)
    return days !== null && days <= 3
  })
  const normalDebts = pending.filter((d) => {
    const days = daysUntil(d.bill?.duDate)
    return days === null || days > 3
  })

  /* â”€â”€ render â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

  if (isLoading) {
    return (
      <div className="min-h-dvh bg-[#F7F4EB] flex items-center justify-center">
        <p className="text-gray-500">Cargando tus pagosâ€¦</p>
      </div>
    )
  }

  return (
    <div className="min-h-dvh bg-[#F7F4EB] pb-8">
      {/* â”€â”€â”€ Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
      <header className="sticky top-0 z-30 bg-[#F7F4EB]/95 backdrop-blur-sm px-3 sm:px-4 pt-2.5 sm:pt-5 pb-2">
        <div className="max-w-3xl mx-auto flex items-center justify-between gap-2">
          <button
            onClick={() => {
              navigate('/my-home')
            }}
            className="w-9 h-9 flex items-center justify-center rounded-full bg-white shadow-sm"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>

          <h1 className="text-[15px] sm:text-lg font-bold text-teal-700 text-center">Mis Pagos</h1>

          {/* notification bell */}
          <div className="relative w-9 h-9 flex items-center justify-center rounded-full bg-white shadow-sm">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            {pending.length > 0 && (
              <span className="absolute -top-0.5 -right-0.5 w-3 h-3 bg-red-500 rounded-full border-2 border-[#F7F4EB]" />
            )}
          </div>
        </div>

        {locationPill && (
          <div className="max-w-3xl mx-auto flex justify-center mt-2 px-2">
            <span className="inline-flex max-w-full items-center gap-1.5 px-2.5 py-1 rounded-full bg-white/80 text-[11px] text-gray-600 shadow-sm text-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5 text-teal-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <span className="truncate max-w-[220px] sm:max-w-none">{locationPill}</span>
            </span>
          </div>
        )}
      </header>

      <main className="max-w-3xl mx-auto px-3 sm:px-4 mt-3 sm:mt-4 space-y-4 sm:space-y-5 pb-14">
        {/* â”€â”€â”€ Tab selector â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
        <div className="flex bg-gray-200/70 rounded-full p-1">
          <button
            onClick={() => setTab('pending')}
            className={`flex-1 py-1.5 sm:py-2 text-[13px] sm:text-sm font-semibold rounded-full transition ${
              tab === 'pending' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'
            }`}
          >
            Pendientes{pending.length > 0 && ` (${pending.length})`}
          </button>
          <button
            onClick={() => setTab('history')}
            className={`flex-1 py-1.5 sm:py-2 text-[13px] sm:text-sm font-semibold rounded-full transition ${
              tab === 'history' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'
            }`}
          >
            Historial{history.length > 0 && ` (${history.length})`}
          </button>
        </div>

        {/* â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            TAB: PENDIENTES
           â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â• */}
        {tab === 'pending' && (
          <>
            {/* Total card */}
            <div className="bg-teal-700 rounded-3xl p-4 sm:p-6 text-white flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 sm:gap-4">
              <div>
                <p className="text-xs sm:text-sm text-teal-100 uppercase tracking-wide">Total pendiente</p>
                <p className="text-[34px] sm:text-4xl font-extrabold mt-1 leading-none">
                  {fmtCurrency(totalPending)}{' '}
                  <span className="text-lg sm:text-xl font-medium text-teal-200">â‚¬</span>
                </p>
              </div>
              {pending.length > 1 && (
                <span className="text-xs px-3 py-1 rounded-full bg-white/20 text-white font-semibold">
                  {pending.length} pagos
                </span>
              )}
            </div>

            {pending.length === 0 ? (
              <div className="flex flex-col items-center py-16 text-center">
                <div className="w-16 h-16 rounded-full bg-green-100 flex items-center justify-center mb-4">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-green-500" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                </div>
                <p className="text-gray-700 font-semibold text-lg">Â¡Todo al dÃ­a!</p>
                <p className="text-gray-400 text-sm mt-1">No tienes pagos pendientes.</p>
              </div>
            ) : (
              <>
                {/* Urgent section */}
                {urgentDebts.length > 0 && (
                  <div>
                    <div className="flex items-center gap-1.5 mb-2">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-red-500" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                      </svg>
                      <span className="text-xs font-bold uppercase tracking-wide text-red-600">
                        AtenciÃ³n requerida
                      </span>
                    </div>
                    <ul className="space-y-3">
                      {urgentDebts.map((debt) => (
                        <DebtCard
                          key={debt.id}
                          debt={debt}
                          onTap={() => {
                            navigate(`/invoices/${debt.id}`)
                          }}
                        />
                      ))}
                    </ul>
                  </div>
                )}

                {/* Normal section */}
                {normalDebts.length > 0 && (
                  <div>
                    <p className="text-xs font-bold uppercase tracking-wide text-gray-400 mb-2">
                      PrÃ³ximos pagos
                    </p>
                    <ul className="space-y-3">
                      {normalDebts.map((debt) => (
                        <DebtCard
                          key={debt.id}
                          debt={debt}
                          onTap={() => {
                            navigate(`/invoices/${debt.id}`)
                          }}
                        />
                      ))}
                    </ul>
                  </div>
                )}
              </>
            )}
          </>
        )}

        {/* â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            TAB: HISTORIAL
           â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â• */}
        {tab === 'history' && (
          <>
            {history.length === 0 ? (
              <div className="flex flex-col items-center py-16 text-center">
                <p className="text-gray-400 text-sm">No hay pagos realizados aÃºn.</p>
              </div>
            ) : (
              <ul className="space-y-3">
                {history.map((debt) => {
                  const icon = conceptIcon(debt.bill?.reference ?? '')
                  return (
                    <li
                      key={debt.id}
                      className="flex items-center gap-3 bg-white rounded-2xl px-4 py-3 shadow-sm"
                    >
                      <span className={`shrink-0 w-11 h-11 rounded-xl flex items-center justify-center text-lg ${icon.bg}`}>
                        {icon.emoji}
                      </span>
                      <div className="flex-1 min-w-0">
                        <p className="font-semibold text-gray-800 text-sm truncate">{debt.bill?.reference ?? 'â€”'}</p>
                        <p className="text-xs text-gray-400">{fmtDate(debt.bill?.duDate)}</p>
                      </div>
                      <div className="text-right">
                        <p className="font-bold text-gray-800 text-sm">{fmtCurrency(Number(debt.amount))} â‚¬</p>
                        <span className="inline-flex items-center gap-1 text-[11px] text-green-600 font-medium">
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                            <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                          </svg>
                          Pagado
                        </span>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </>
        )}
      </main>
    </div>
  )
}

/* â”€â”€ Debt card sub-component â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

function DebtCard({ debt, onTap }: { debt: TenantDebtDTO; onTap: () => void }) {
  const days = daysUntil(debt.bill?.duDate)
  const urg = urgencyMeta(days)
  const icon = conceptIcon(debt.bill?.reference ?? '')

  return (
    <li
      onClick={onTap}
      className={`bg-white rounded-2xl shadow-sm overflow-hidden cursor-pointer hover:shadow-md transition ${
        urg.urgent ? 'border-l-4 border-red-500' : ''
      }`}
    >
      <div className="flex items-start sm:items-center gap-3 px-3.5 sm:px-4 py-3">
        <span className={`shrink-0 w-10 h-10 sm:w-11 sm:h-11 rounded-xl flex items-center justify-center text-base sm:text-lg ${icon.bg}`}>
          {icon.emoji}
        </span>

        <div className="flex-1 min-w-0">
          <p className="font-semibold text-gray-800 text-sm sm:text-[15px] leading-tight truncate">
            {debt.bill?.reference ?? 'â€”'}
          </p>
          <p className={`text-xs font-semibold ${urg.color}`}>
            {urg.label || fmtDate(debt.bill?.duDate)}
          </p>
        </div>

        <p className="font-bold text-gray-900 text-sm sm:text-base text-right whitespace-nowrap mr-1">
          {fmtCurrency(Number(debt.amount ?? 0))} â‚¬
        </p>

        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
        </svg>
      </div>

      <div className="px-4 pb-3">
        <button
          onClick={(e) => { e.stopPropagation(); onTap() }}
          className={`w-full py-2 rounded-xl text-[13px] sm:text-sm font-semibold transition ${
            urg.urgent
              ? 'bg-teal-700 text-white hover:bg-teal-800'
              : 'bg-white border border-teal-600 text-teal-700 hover:bg-teal-50'
          }`}
        >
          Pagar ahora
        </button>
      </div>
    </li>
  )
}
