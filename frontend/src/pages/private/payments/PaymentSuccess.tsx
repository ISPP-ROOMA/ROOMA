import { useNavigate, useLocation, useParams } from 'react-router-dom'
import type { TenantDebtDTO } from '../../../service/billing.service'

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

export default function PaymentSuccess() {
  const navigate = useNavigate()
  const { debtId } = useParams<{ debtId: string }>()
  const location = useLocation()
  const debt = (location.state as { debt?: TenantDebtDTO } | null)?.debt ?? null

  const amount = debt ? Number(debt.amount ?? 0) : 0
  const reference = debt?.bill?.reference ?? '—'
  const dueDate = debt?.bill?.duDate
  const icon = conceptIcon(reference)

  return (
    <div className="min-h-dvh bg-[#F7F4EB] flex flex-col items-center justify-center px-4 py-10">
      <div className="max-w-md w-full flex flex-col items-center text-center space-y-6">
        {/* Animated check circle with glow */}
        <div className="relative">
          <div className="absolute inset-0 w-28 h-28 bg-teal-400/20 rounded-full blur-xl animate-pulse" />
          <div className="relative w-28 h-28 rounded-full bg-teal-600 flex items-center justify-center shadow-lg shadow-teal-600/30">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-14 w-14 text-white"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={2.5}
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
        </div>

        {/* Title */}
        <div>
          <h1 className="text-2xl font-extrabold text-gray-900">¡Pago completado con éxito!</h1>
          <p className="text-sm text-gray-400 mt-1">Tu pago ha sido registrado correctamente.</p>
        </div>

        {/* Summary card */}
        <div className="bg-white rounded-3xl shadow-sm w-full max-w-sm overflow-hidden">
          <div className="flex flex-col items-center py-5 border-b border-gray-100">
            <span
              className={`w-12 h-12 rounded-xl flex items-center justify-center text-xl ${icon.bg} mb-2`}
            >
              {icon.emoji}
            </span>
            <p className="text-3xl font-extrabold text-gray-900">
              {fmtCurrency(amount)} <span className="text-lg font-medium text-gray-400">€</span>
            </p>
          </div>
          <div className="divide-y divide-gray-100">
            <SummaryRow label="Concepto" value={reference} />
            <SummaryRow label="Vencimiento" value={fmtDate(dueDate)} />
            <SummaryRow label="Referencia" value={`#${debtId ?? '—'}`} />
            <SummaryRow label="Estado" value="Pagado" valueClass="text-green-600 font-semibold" />
          </div>
        </div>

        {/* Actions */}
        <div className="w-full max-w-sm space-y-3 pt-2">
          <button
            disabled
            className="w-full py-3 rounded-2xl border border-teal-600 text-teal-700 font-semibold text-sm transition opacity-50 cursor-not-allowed flex items-center justify-center gap-2"
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
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
              />
            </svg>
            Descargar recibo (próximamente)
          </button>

          <button
            onClick={() => navigate('/invoices', { replace: true })}
            className="w-full py-3.5 rounded-2xl bg-teal-700 text-white font-semibold text-base transition hover:bg-teal-800"
          >
            Volver a Mis Pagos
          </button>
        </div>
      </div>
    </div>
  )
}

/* ── sub-component ────────────────────────────────────────── */

function SummaryRow({
  label,
  value,
  valueClass,
}: {
  label: string
  value: string
  valueClass?: string
}) {
  return (
    <div className="flex items-center justify-between px-5 py-3">
      <span className="text-sm text-gray-500">{label}</span>
      <span className={`text-sm font-medium ${valueClass ?? 'text-gray-900'}`}>{value}</span>
    </div>
  )
}
