import { useEffect, useState, useContext } from 'react'
import { useNavigate, useParams, useLocation } from 'react-router-dom'
import { getMyDebts, payDebt, type TenantDebtDTO } from '../../../service/billing.service'
import { ToastContext } from '../../../context/ToastContext'

/* â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

const fmtCurrency = (v: number) =>
  new Intl.NumberFormat('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)

const fmtDate = (v?: string) => {
  if (!v) return 'â€”'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v
  return new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'long', year: 'numeric' }).format(d)
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
  if (days <= 3) return { text: `Vence en ${days} dÃ­as`, cls: 'bg-orange-50 text-orange-600' }
  if (days <= 7) return { text: `Vence en ${days} dÃ­as`, cls: 'bg-amber-50 text-amber-600' }
  return { text: `Vence en ${days} dÃ­as`, cls: 'bg-teal-50 text-teal-700' }
}

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

export default function DebtDetail() {
  const { debtId } = useParams<{ debtId: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const toastCtx = useContext(ToastContext)
  const showToast = toastCtx?.showToast ?? (() => {})

  const [debt, setDebt] = useState<TenantDebtDTO | null>(
    (location.state as { debt?: TenantDebtDTO } | null)?.debt ?? null,
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
        showToast('No se pudo procesar el pago. IntÃ©ntalo de nuevo.', 'error')
        setIsPaying(false)
      }
    } catch {
      showToast('Error al procesar el pago.', 'error')
      setIsPaying(false)
    }
  }

  /* â”€â”€ loading / not found â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
  if (isLoading) {
    return (
      <div className="min-h-dvh bg-[#F7F4EB] flex items-center justify-center">
        <p className="text-gray-500">Cargando detalleâ€¦</p>
      </div>
    )
  }

  if (!debt) {
    return (
      <div className="min-h-dvh bg-[#F7F4EB] flex flex-col items-center justify-center gap-4">
        <p className="text-gray-500">No se encontrÃ³ la deuda.</p>
        <button onClick={() => { navigate('/invoices') }} className="text-teal-700 font-semibold text-sm">
          â† Volver a Mis Pagos
        </button>
      </div>
    )
  }

  /* â”€â”€ data â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
  const days = daysUntil(debt.bill?.duDate)
  const badge = statusBadge(days)
  const icon = conceptIcon(debt.bill?.reference ?? '')
  const isPending = debt.status === 'PENDING'

  return (
    <div className="min-h-dvh bg-[#F7F4EB] flex flex-col">
      {/* â”€â”€â”€ Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
      <header className="sticky top-0 z-30 bg-[#F7F4EB]/95 backdrop-blur-sm px-4 pt-3 sm:pt-5 pb-3">
        <div className="max-w-3xl mx-auto flex items-center justify-between gap-3">
          <button
            onClick={() => { navigate('/invoices') }}
            className="w-9 h-9 flex items-center justify-center rounded-full bg-white shadow-sm"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>

          <h1 className="text-lg font-bold text-teal-700">Detalle del pago</h1>

          <div className="w-9" />
        </div>
      </header>

      {/* â”€â”€â”€ Body â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
      <main className="flex-1 max-w-3xl mx-auto w-full px-4 mt-2 space-y-5 pb-32">
        {/* Badge */}
        <div className="flex justify-center">
          <span className={`text-xs font-semibold px-3 py-1 rounded-full uppercase tracking-wide ${badge.cls}`}>
            {badge.text}
          </span>
        </div>

        {/* Amount hero */}
        <div className="flex flex-col items-center rounded-3xl bg-white/60 border border-white px-4 py-5">
          <span className={`w-16 h-16 rounded-2xl flex items-center justify-center text-2xl ${icon.bg} mb-3`}>
            {icon.emoji}
          </span>
          <p className="text-4xl font-extrabold text-gray-900">
            {fmtCurrency(Number(debt.amount))}{' '}
            <span className="text-xl font-medium text-gray-400">â‚¬</span>
          </p>
          <p className="text-sm text-gray-500 mt-1">{debt.bill?.reference ?? 'â€”'}</p>
        </div>

        {/* Detail card */}
        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="divide-y divide-gray-100">
            <DetailRow label="Concepto" value={debt.bill?.reference ?? 'â€”'} />
            <DetailRow label="Fecha de vencimiento" value={fmtDate(debt.bill?.duDate)} />
            <DetailRow label="Importe total factura" value={`${fmtCurrency(Number(debt.bill?.totalAmount ?? 0))} â‚¬`} />
            <DetailRow label="Tu parte" value={`${fmtCurrency(Number(debt.amount))} â‚¬`} highlight />
            <DetailRow
              label="Estado"
              value={isPending ? 'Pendiente de pago' : 'Pagado'}
              valueClass={isPending ? 'text-orange-500 font-semibold' : 'text-green-600 font-semibold'}
            />
          </div>
        </div>

        {/* Note */}
        <div className="bg-amber-50/90 border border-amber-100 rounded-2xl px-4 py-3.5 flex gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-amber-500 shrink-0 mt-0.5" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
          <p className="text-xs text-amber-900 leading-relaxed">
            Este importe corresponde a tu parte proporcional de la factura total. Al pulsar "Pagar ahora" se registrarÃ¡ como pagado.
          </p>
        </div>
      </main>

      {/* â”€â”€â”€ Sticky footer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
      {isPending && (
        <div className="fixed inset-x-0 bottom-[4.75rem] md:bottom-0 bg-[#F7F4EB]/95 backdrop-blur-sm border-t border-gray-200/60 px-4 py-4 z-40 shadow-[0_-6px_24px_rgba(0,0,0,0.06)]">
          <div className="max-w-3xl mx-auto">
            <button
              onClick={() => void handlePay()}
              disabled={isPaying}
              className="w-full py-3.5 rounded-2xl bg-teal-700 text-white font-semibold text-base tracking-wide transition hover:bg-teal-800 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {isPaying ? (
                <>
                  <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Procesandoâ€¦
                </>
              ) : (
                <>Pagar {fmtCurrency(Number(debt.amount))} â‚¬</>
              )}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

/* â”€â”€ row sub-component â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */

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
      className={`flex flex-col items-start gap-1 px-5 py-3.5 sm:flex-row sm:items-center sm:justify-between ${highlight ? 'bg-teal-50/50' : ''}`}
    >
      <span className="text-[11px] sm:text-xs uppercase tracking-wide text-gray-500">{label}</span>
      <span className={`text-sm font-semibold text-right ${valueClass ?? 'text-gray-900'}`}>{value}</span>
    </div>
  )
}
