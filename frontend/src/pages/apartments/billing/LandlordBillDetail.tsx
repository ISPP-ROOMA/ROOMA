import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getApartmentBills,
  type BillDTO,
  type TenantDebtInBill,
} from '../../../service/billing.service'
import { getMyApartments } from '../../../service/apartments.service'

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

function userInitials(email: string) {
  const name = email.split('@')[0]
  return name.slice(0, 2).toUpperCase()
}

/* ── component ────────────────────────────────────────────── */

export default function LandlordBillDetail() {
  const { id: apartmentId, billId } = useParams()
  const navigate = useNavigate()
  const [bill, setBill] = useState<BillDTO | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    if (!apartmentId || !billId) return
    const load = async () => {
      try {
        const parsedApartmentId = Number(apartmentId)
        const myApartments = await getMyApartments()
        const isOwner = myApartments.some((myApartment) => myApartment.id === parsedApartmentId)
        if (!isOwner) {
          navigate('/apartments/my', { replace: true })
          return
        }

        const bills = await getApartmentBills(parsedApartmentId)
        const found = bills.find((b) => b.id === Number(billId))
        setBill(found ?? null)
      } catch (error) {
        console.error('Error loading landlord bill detail', error)
        navigate('/apartments/my', { replace: true })
      } finally {
        setIsLoading(false)
      }
    }
    void load()
  }, [apartmentId, billId, navigate])

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <span className="loading loading-spinner loading-lg text-teal-600" />
      </div>
    )
  }

  if (!bill) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center gap-3 text-gray-400">
        <p className="text-5xl">🔍</p>
        <p className="font-medium">Factura no encontrada</p>
        <button
          onClick={() => navigate(`/apartments/${apartmentId}/bills`)}
          className="mt-2 text-teal-600 text-sm underline"
        >
          Volver a facturas
        </button>
      </div>
    )
  }

  const debts = bill.tenantDebts ?? []
  const paidDebts = debts.filter((d) => d.status === 'PAID')
  const pendingDebts = debts.filter((d) => d.status !== 'PAID')
  const paidAmount = paidDebts.reduce((s, d) => s + Number(d.amount), 0)
  const pendingAmount = pendingDebts.reduce((s, d) => s + Number(d.amount), 0)
  const progressPct = debts.length > 0 ? (paidDebts.length / debts.length) * 100 : 0
  const icon = conceptIcon(bill.reference ?? '')

  const isPaid = bill.status === 'PAID'

  return (
    <div className="min-h-screen bg-[#F7F4EB]">
      {/* ── header ─────────────────────────────────── */}
      <div
        className={`px-5 pt-12 pb-10 rounded-b-3xl shadow-lg ${
          isPaid
            ? 'bg-gradient-to-br from-emerald-600 to-emerald-500'
            : 'bg-gradient-to-br from-teal-700 to-teal-600'
        } text-white`}
      >
        <button
          onClick={() => navigate(`/apartments/${apartmentId}/bills`)}
          className="mb-4 flex items-center gap-1 text-white/70 hover:text-white transition text-sm"
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
          Facturas
        </button>

        <div className="flex items-center gap-3">
          <div
            className={`w-14 h-14 ${icon.bg} rounded-2xl flex items-center justify-center text-3xl shadow-sm`}
          >
            {icon.emoji}
          </div>
          <div>
            <h1 className="text-2xl font-bold">{bill.reference}</h1>
            <p className="text-white/70 text-sm">Vence {fmtDate(bill.duDate)}</p>
          </div>
        </div>

        <div className="mt-5 flex items-end justify-between">
          <div>
            <p className="text-xs text-white/60 uppercase tracking-wide">Importe total</p>
            <p className="text-3xl font-extrabold">{fmtCurrency(bill.totalAmount)} €</p>
          </div>
          <span
            className={`text-xs font-bold px-3 py-1 rounded-full ${
              isPaid ? 'bg-white/25 text-white' : 'bg-white/20 text-white'
            }`}
          >
            {isPaid ? '✓ Pagada' : 'Pendiente'}
          </span>
        </div>
      </div>

      {/* ── progress card ──────────────────────────── */}
      <div className="px-5 -mt-5">
        <div className="bg-white rounded-2xl p-5 shadow-md">
          <div className="flex justify-between text-sm mb-2">
            <span className="text-gray-500">Progreso de cobro</span>
            <span className="font-semibold text-gray-700">
              {paidDebts.length}/{debts.length} inquilinos
            </span>
          </div>
          <div className="h-3 bg-gray-100 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                progressPct === 100 ? 'bg-emerald-400' : 'bg-teal-400'
              }`}
              style={{ width: `${progressPct}%` }}
            />
          </div>
          <div className="flex justify-between mt-2 text-xs text-gray-400">
            <span>Cobrado: {fmtCurrency(paidAmount)} €</span>
            <span>Pendiente: {fmtCurrency(pendingAmount)} €</span>
          </div>
        </div>
      </div>

      {/* ── tenant list ────────────────────────────── */}
      <div className="px-5 mt-5 pb-10">
        {/* Pending */}
        {pendingDebts.length > 0 && (
          <>
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-amber-400" />
              Pendientes de pago ({pendingDebts.length})
            </h2>
            <div className="space-y-2 mb-6">
              {pendingDebts.map((debt) => (
                <DebtRow key={debt.id} debt={debt} />
              ))}
            </div>
          </>
        )}

        {/* Paid */}
        {paidDebts.length > 0 && (
          <>
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-400" />
              Han pagado ({paidDebts.length})
            </h2>
            <div className="space-y-2">
              {paidDebts.map((debt) => (
                <DebtRow key={debt.id} debt={debt} />
              ))}
            </div>
          </>
        )}

        {debts.length === 0 && (
          <div className="text-center py-10 text-gray-400">
            <p className="text-4xl mb-2">👥</p>
            <p className="font-medium">Sin inquilinos asignados</p>
          </div>
        )}
      </div>
    </div>
  )
}

/* ── debt row ─────────────────────────────────────────────── */

function DebtRow({ debt }: { debt: TenantDebtInBill }) {
  const isPaid = debt.status === 'PAID'
  const email = debt.user?.email ?? 'Sin email'
  const initials = userInitials(email)

  return (
    <div
      className={`flex items-center gap-3 bg-white rounded-xl p-3.5 shadow-sm ${
        isPaid ? 'opacity-80' : ''
      }`}
    >
      {/* avatar */}
      <div
        className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold shrink-0 ${
          isPaid ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'
        }`}
      >
        {initials}
      </div>

      {/* info */}
      <div className="flex-1 min-w-0">
        <p className="font-medium text-gray-800 text-sm truncate">{email}</p>
        {debt.user?.profession && (
          <p className="text-xs text-gray-400 truncate">{debt.user.profession}</p>
        )}
      </div>

      {/* amount + badge */}
      <div className="text-right shrink-0 flex items-center gap-2">
        <span className="font-semibold text-gray-700 text-sm">
          {new Intl.NumberFormat('es-ES', { minimumFractionDigits: 2 }).format(debt.amount)} €
        </span>
        {isPaid ? (
          <span className="bg-emerald-50 text-emerald-600 ring-1 ring-emerald-200 text-[10px] font-bold px-2 py-0.5 rounded-full">
            Pagado
          </span>
        ) : (
          <span className="bg-amber-50 text-amber-600 ring-1 ring-amber-200 text-[10px] font-bold px-2 py-0.5 rounded-full">
            Pendiente
          </span>
        )}
      </div>
    </div>
  )
}
