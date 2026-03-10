import { useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getApartmentBills, type BillDTO, type BillStatus } from '../../../service/billing.service'

/* ── helpers ──────────────────────────────────────────────── */

const fmtCurrency = (v: number) =>
  new Intl.NumberFormat('es-ES', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(v)

const fmtDate = (v?: string) => {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v
  return new Intl.DateTimeFormat('es-ES', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(d)
}

function daysUntil(dateStr?: string): number | null {
  if (!dateStr) return null
  const due = new Date(dateStr)
  if (Number.isNaN(due.getTime())) return null
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  due.setHours(0, 0, 0, 0)
  return Math.round((due.getTime() - now.getTime()) / 86_400_000)
}

function statusMeta(status: BillStatus, duDate?: string) {
  if (status === 'PAID')
    return {
      label: 'Pagada',
      bg: 'bg-emerald-50',
      text: 'text-emerald-700',
      ring: 'ring-emerald-200',
    }
  const days = daysUntil(duDate)
  if (days !== null && days < 0)
    return { label: 'Vencida', bg: 'bg-red-50', text: 'text-red-700', ring: 'ring-red-200' }
  if (days !== null && days <= 3)
    return {
      label: 'Próx. vencimiento',
      bg: 'bg-orange-50',
      text: 'text-orange-700',
      ring: 'ring-orange-200',
    }
  return { label: 'Pendiente', bg: 'bg-amber-50', text: 'text-amber-700', ring: 'ring-amber-200' }
}

function conceptIcon(ref: string) {
  const r = ref.toLowerCase()
  if (r.includes('alquiler')) return { emoji: '🏠', bg: 'bg-violet-100' }
  if (r.includes('luz') || r.includes('electric')) return { emoji: '⚡', bg: 'bg-amber-100' }
  if (r.includes('agua')) return { emoji: '💧', bg: 'bg-sky-100' }
  if (r.includes('gas')) return { emoji: '🔥', bg: 'bg-orange-100' }
  if (r.includes('internet') || r.includes('wifi')) return { emoji: '📶', bg: 'bg-indigo-100' }
  if (r.includes('comunidad')) return { emoji: '🏢', bg: 'bg-teal-100' }
  if (r.includes('seguro')) return { emoji: '🛡️', bg: 'bg-emerald-100' }
  if (r.includes('limpieza')) return { emoji: '🧹', bg: 'bg-cyan-100' }
  return { emoji: '📄', bg: 'bg-gray-100' }
}

/* ── component ────────────────────────────────────────────── */

type Tab = 'pending' | 'paid'

export default function ApartmentBills() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [bills, setBills] = useState<BillDTO[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [tab, setTab] = useState<Tab>('pending')

  useEffect(() => {
    if (!id) return
    const load = async () => {
      const data = await getApartmentBills(Number(id))
      setBills(data)
      setIsLoading(false)
    }
    void load()
  }, [id])

  const pending = useMemo(
    () =>
      bills
        .filter((b) => b.status !== 'PAID')
        .sort((a, b) => {
          const da = daysUntil(a.duDate) ?? 999
          const db = daysUntil(b.duDate) ?? 999
          return da - db
        }),
    [bills]
  )

  const paid = useMemo(() => bills.filter((b) => b.status === 'PAID'), [bills])

  const totalPending = useMemo(
    () => pending.reduce((s, b) => s + Number(b.totalAmount ?? 0), 0),
    [pending]
  )

  const list = tab === 'pending' ? pending : paid

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <span className="loading loading-spinner loading-lg text-teal-600" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-[#F7F4EB]">
      {/* ── header ─────────────────────────────────── */}
      <div className="bg-gradient-to-br from-teal-700 to-teal-600 text-white px-5 pt-12 pb-10 rounded-b-3xl shadow-lg">
        <button
          onClick={() => navigate(`/apartments/${id}`)}
          className="mb-4 flex items-center gap-1 text-teal-100 hover:text-white transition text-sm"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-4 w-4"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M15 19l-7-7 7-7"
            />
          </svg>
          Volver al piso
        </button>

        <h1 className="text-2xl font-bold">Facturas del piso</h1>
        <p className="text-teal-100 text-sm mt-1">
          {bills.length} factura{bills.length !== 1 ? 's' : ''} en total
        </p>

        {totalPending > 0 && (
          <div className="mt-4 bg-white/15 backdrop-blur rounded-2xl px-5 py-3">
            <p className="text-xs text-teal-100 uppercase tracking-wide">Pendiente de cobro</p>
            <p className="text-3xl font-extrabold mt-1">{fmtCurrency(totalPending)} €</p>
          </div>
        )}
      </div>

      {/* ── tab switcher ───────────────────────────── */}
      <div className="flex gap-2 px-5 -mt-4">
        {(['pending', 'paid'] as Tab[]).map((t) => {
          const active = tab === t
          const count = t === 'pending' ? pending.length : paid.length
          return (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`flex-1 py-2.5 rounded-xl text-sm font-semibold transition shadow-sm ${
                active ? 'bg-white text-teal-700 shadow-md' : 'bg-white/60 text-gray-500'
              }`}
            >
              {t === 'pending' ? 'Pendientes' : 'Pagadas'}{' '}
              <span className="opacity-60">({count})</span>
            </button>
          )
        })}
      </div>

      {/* ── list ──────────────────────────────────── */}
      <div className="px-5 mt-5 pb-28 space-y-3">
        {list.length === 0 && (
          <div className="text-center py-16 text-gray-400">
            <p className="text-5xl mb-3">{tab === 'pending' ? '🎉' : '📭'}</p>
            <p className="font-medium">
              {tab === 'pending' ? 'No hay facturas pendientes' : 'Sin historial'}
            </p>
          </div>
        )}

        {list.map((bill) => {
          const icon = conceptIcon(bill.reference ?? '')
          const st = statusMeta(bill.status, bill.duDate)
          const paidCount = (bill.tenantDebts ?? []).filter((d) => d.status === 'PAID').length
          const totalCount = (bill.tenantDebts ?? []).length

          return (
            <button
              key={bill.id}
              onClick={() => navigate(`/apartments/${id}/bills/${bill.id}`)}
              className="w-full bg-white rounded-2xl p-4 shadow-sm hover:shadow-md transition flex items-center gap-4 text-left"
            >
              {/* icon */}
              <div
                className={`w-12 h-12 ${icon.bg} rounded-xl flex items-center justify-center text-2xl shrink-0`}
              >
                {icon.emoji}
              </div>

              {/* info */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-gray-800 truncate">{bill.reference}</span>
                  <span
                    className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ring-1 ${st.bg} ${st.text} ${st.ring}`}
                  >
                    {st.label}
                  </span>
                </div>
                <p className="text-xs text-gray-400 mt-0.5">Vence {fmtDate(bill.duDate)}</p>
                {totalCount > 0 && (
                  <div className="flex items-center gap-2 mt-1.5">
                    {/* mini progress bar */}
                    <div className="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-emerald-400 rounded-full transition-all"
                        style={{ width: `${(paidCount / totalCount) * 100}%` }}
                      />
                    </div>
                    <span className="text-[10px] text-gray-400 whitespace-nowrap">
                      {paidCount}/{totalCount} pagadas
                    </span>
                  </div>
                )}
              </div>

              {/* amount */}
              <div className="text-right shrink-0">
                <p className="font-bold text-gray-800">{fmtCurrency(bill.totalAmount)} €</p>
              </div>

              {/* chevron */}
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5 text-gray-300 shrink-0"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 5l7 7-7 7"
                />
              </svg>
            </button>
          )
        })}
      </div>

      {/* ── FAB: new bill ─────────────────────────── */}
      <div className="fixed bottom-6 right-6">
        <button
          onClick={() => navigate(`/apartments/${id}/new-bill`)}
          className="bg-teal-600 hover:bg-teal-700 text-white w-14 h-14 rounded-full shadow-lg flex items-center justify-center transition"
          title="Nueva factura"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-7 w-7"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
        </button>
      </div>
    </div>
  )
}
