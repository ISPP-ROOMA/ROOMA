import { useEffect, useState, useContext } from 'react'
import { useNavigate, useParams, useLocation } from 'react-router-dom'
import { getMyDebts, payDebt, type TenantDebtDTO } from '../../../service/billing.service'
import { ToastContext } from '../../../context/ToastContext'

/* ── helpers ──────────────────────────────────────────────── */

const fmtCurrency = (v: number) =>
  new Intl.NumberFormat('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)

const fmtDate = (v?: string) => {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v
  return new Intl.DateTimeFormat('es-ES', {
    day: 'numeric',
    month: 'long',
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

function statusBadge(days: number | null) {
  if (days === null) return { text: 'Sin fecha', cls: 'bg-gray-100 text-gray-600' }
  if (days < 0) return { text: 'Vencido', cls: 'bg-red-50 text-red-600' }
  if (days === 0) return { text: 'Vence hoy', cls: 'bg-orange-50 text-orange-600' }
  if (days <= 3) return { text: `Vence en ${days} días`, cls: 'bg-orange-50 text-orange-600' }
  if (days <= 7) return { text: `Vence en ${days} días`, cls: 'bg-amber-50 text-amber-600' }
  return { text: `Vence en ${days} días`, cls: 'bg-teal-50 text-teal-700' }
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

export default function DebtDetail() {
  const { debtId } = useParams<{ debtId: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const toastCtx = useContext(ToastContext)
  const showToast = toastCtx?.showToast ?? (() => {})

  const [debt, setDebt] = useState<TenantDebtDTO | null>(
    (location.state as { debt?: TenantDebtDTO } | null)?.debt ?? null
  )
  const [isLoading, setIsLoading] = useState(!debt)
  const [isPaying, setIsPaying] = useState(false)

  /* fallback: load from list if no state passed */
  useEffect(() => {
    if (debt) return
    const load = async () => {
      const debts = await getMyDebts()
      const found = debts.find((d) => String(d.id) === debtId) ?? null
      setDebt(found)
      setIsLoading(false)
    }
    void load()
  }, [debt, debtId])

  const handlePay = async () => {
    if (!debt) return
    setIsPaying(true)
    try {
      const result = await payDebt(debt.id)
      if (result) {
        navigate(`/invoices/${debt.id}/success`, {
          state: { debt: { ...debt, ...result } },
          replace: true,
        })
      } else {
        showToast('No se pudo procesar el pago. Inténtalo de nuevo.', 'error')
        setIsPaying(false)
      }
    } catch {
      showToast('Error al procesar el pago.', 'error')
      setIsPaying(false)
    }
  }

  /* ── loading / not found ────────────────────────────────── */
  if (isLoading) {
    return (
      <div className="min-h-dvh bg-[#F7F4EB] flex items-center justify-center">
        <p className="text-gray-500">Cargando detalle…</p>
      </div>
    )
  }

  if (!debt) {
    return (
      <div className="min-h-dvh bg-[#F7F4EB] flex flex-col items-center justify-center gap-4">
        <p className="text-gray-500">No se encontró la deuda.</p>
        <button
          onClick={() => navigate('/invoices')}
          className="text-teal-700 font-semibold text-sm"
        >
          ← Volver a Mis Pagos
        </button>
      </div>
    )
  }

  /* ── data ────────────────────────────────────────────────── */
  const days = daysUntil(debt.bill?.duDate)
  const badge = statusBadge(days)
  const icon = conceptIcon(debt.bill?.reference ?? '')
  const isPending = debt.status === 'PENDING'

  return (
    <div className="min-h-dvh bg-[#F7F4EB] flex flex-col">
      {/* ─── Header ───────────────────────────────────────── */}
      <header className="sticky top-0 z-30 bg-[#F7F4EB] px-4 pt-5 pb-3">
        <div className="max-w-xl mx-auto flex items-center justify-between">
          <button
            onClick={() => navigate('/invoices')}
            className="w-9 h-9 flex items-center justify-center rounded-full bg-white shadow-sm"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5 text-gray-700"
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
          </button>

          <h1 className="text-lg font-bold text-teal-700">Detalle del pago</h1>

          <div className="w-9" />
        </div>
      </header>

      {/* ─── Body ─────────────────────────────────────────── */}
      <main className="flex-1 max-w-xl mx-auto w-full px-4 mt-2 space-y-5 pb-32">
        {/* Badge */}
        <div className="flex justify-center">
          <span className={`text-xs font-semibold px-3 py-1 rounded-full ${badge.cls}`}>
            {badge.text}
          </span>
        </div>

        {/* Amount hero */}
        <div className="flex flex-col items-center">
          <span
            className={`w-16 h-16 rounded-2xl flex items-center justify-center text-2xl ${icon.bg} mb-3`}
          >
            {icon.emoji}
          </span>
          <p className="text-4xl font-extrabold text-gray-900">
            {fmtCurrency(Number(debt.amount ?? 0))}{' '}
            <span className="text-xl font-medium text-gray-400">€</span>
          </p>
          <p className="text-sm text-gray-500 mt-1">{debt.bill?.reference ?? '—'}</p>
        </div>

        {/* Detail card */}
        <div className="bg-white rounded-3xl shadow-sm overflow-hidden">
          <div className="divide-y divide-gray-100">
            <DetailRow label="Concepto" value={debt.bill?.reference ?? '—'} />
            <DetailRow label="Fecha de vencimiento" value={fmtDate(debt.bill?.duDate)} />
            <DetailRow
              label="Importe total factura"
              value={`${fmtCurrency(Number(debt.bill?.totalAmount ?? 0))} €`}
            />
            <DetailRow
              label="Tu parte"
              value={`${fmtCurrency(Number(debt.amount ?? 0))} €`}
              highlight
            />
            <DetailRow
              label="Estado"
              value={isPending ? 'Pendiente de pago' : 'Pagado'}
              valueClass={
                isPending ? 'text-orange-500 font-semibold' : 'text-green-600 font-semibold'
              }
            />
          </div>
        </div>

        {/* Note */}
        <div className="bg-amber-50/70 rounded-2xl px-4 py-3 flex gap-2">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-5 w-5 text-amber-500 shrink-0 mt-0.5"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
              clipRule="evenodd"
            />
          </svg>
          <p className="text-xs text-amber-800 leading-relaxed">
            Este importe corresponde a tu parte proporcional de la factura total. Al pulsar "Pagar
            ahora" se registrará como pagado.
          </p>
        </div>
      </main>

      {/* ─── Sticky footer ────────────────────────────────── */}
      {isPending && (
        <div className="fixed bottom-0 inset-x-0 bg-[#F7F4EB] border-t border-gray-200/50 px-4 py-4 z-40">
          <div className="max-w-xl mx-auto">
            <button
              onClick={() => void handlePay()}
              disabled={isPaying}
              className="w-full py-3.5 rounded-2xl bg-teal-700 text-white font-semibold text-base transition hover:bg-teal-800 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {isPaying ? (
                <>
                  <svg
                    className="animate-spin h-5 w-5"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                    />
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                    />
                  </svg>
                  Procesando…
                </>
              ) : (
                <>Pagar {fmtCurrency(Number(debt.amount ?? 0))} €</>
              )}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

/* ── row sub-component ───────────────────────────────────── */

function DetailRow({
  label,
  value,
  highlight,
  valueClass,
}: {
  label: string
  value: string
  highlight?: boolean
  valueClass?: string
}) {
  return (
    <div
      className={`flex items-center justify-between px-5 py-3.5 ${highlight ? 'bg-teal-50/50' : ''}`}
    >
      <span className="text-sm text-gray-500">{label}</span>
      <span className={`text-sm font-medium text-right ${valueClass ?? 'text-gray-900'}`}>
        {value}
      </span>
    </div>
  )
}
