import { useEffect, useMemo, useState, useContext } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getApartment, type Apartment } from '../../../service/apartments.service'
import {
  createBill,
  getApartmentMembers,
  type ApartmentMemberDTO,
} from '../../../service/billing.service'
import { getUser, type User } from '../../../service/users.service'
import { ToastContext } from '../../../context/ToastContext'

/* ── helpers ──────────────────────────────────────────────── */

const MAX_AMOUNT = 99999

const CONCEPTS = [
  'Alquiler',
  'Electricidad',
  'Agua',
  'Gas',
  'Internet / WiFi',
  'Comunidad',
  'Seguro del hogar',
  'Limpieza',
  'Mantenimiento',
  'Otro',
]

const AVATAR_PALETTES = [
  { bg: 'bg-blue-100', text: 'text-blue-700' },
  { bg: 'bg-purple-100', text: 'text-purple-700' },
  { bg: 'bg-green-100', text: 'text-green-700' },
  { bg: 'bg-amber-100', text: 'text-amber-700' },
  { bg: 'bg-rose-100', text: 'text-rose-700' },
  { bg: 'bg-cyan-100', text: 'text-cyan-700' },
]

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/)
  if (parts.length >= 2) {
    return `${parts[0][0]}${parts[1][0]}`.toUpperCase()
  }
  return name.slice(0, 2).toUpperCase()
}

function getDisplayName(user: User | undefined, memberId: number): string {
  if (user?.name) return user.name
  if (user?.email) {
    const local = user.email.split('@')[0]
    return local.charAt(0).toUpperCase() + local.slice(1)
  }
  return `Inquilino #${memberId}`
}

const fmtCurrency = (v: number) =>
  new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
  }).format(v)

/* ── types ────────────────────────────────────────────────── */

interface TenantRow {
  member: ApartmentMemberDTO
  user: User | undefined
  selected: boolean
  customAmount: string
}

/* ── component ────────────────────────────────────────────── */

export default function NewBill() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toastCtx = useContext(ToastContext)
  const showToast = toastCtx?.showToast ?? (() => {})

  const apartmentId = Number(id)

  /* form state */
  const [concept, setConcept] = useState('')
  const [totalAmount, setTotalAmount] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [file, setFile] = useState<File | null>(null)

  /* data */
  const [apartment, setApartment] = useState<Apartment | null>(null)
  const [tenants, setTenants] = useState<TenantRow[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSending, setIsSending] = useState(false)

  /* ── load apartment + members ──────────────────────────── */

  useEffect(() => {
    const load = async () => {
      if (!Number.isFinite(apartmentId)) {
        setIsLoading(false)
        return
      }

      try {
        const [apt, members] = await Promise.all([
          getApartment(apartmentId),
          getApartmentMembers(apartmentId),
        ])

        if (apt) setApartment(apt)

        const rows: TenantRow[] = await Promise.all(
          members.map(async (m) => {
            let user: User | undefined
            try {
              user = await getUser(m.userId)
            } catch {
              /* fallback */
            }
            return { member: m, user, selected: true, customAmount: '' }
          }),
        )

        setTenants(rows)
      } catch (err) {
        console.error(err)
      } finally {
        setIsLoading(false)
      }
    }

    void load()
  }, [apartmentId])

  /* ── derived: amounts ──────────────────────────────────── */

  const total = parseFloat(totalAmount) || 0

  const manualSum = useMemo(
    () =>
      tenants
        .filter((t) => !t.selected)
        .reduce((acc, t) => acc + (parseFloat(t.customAmount) || 0), 0),
    [tenants],
  )

  const autoPool = Math.max(0, total - manualSum)
  const autoCount = useMemo(() => tenants.filter((t) => t.selected).length, [tenants])
  const autoShare = autoCount > 0 ? autoPool / autoCount : 0
  const totalAssigned = manualSum + (autoCount > 0 ? autoPool : 0)

  const isDistributionValid =
    total > 0 &&
    Math.abs(totalAssigned - total) < 0.01 &&
    (autoCount > 0 || manualSum > 0) &&
    manualSum <= total

  const unassigned = total - totalAssigned

  const locationLabel = apartment?.ubication
    ? apartment.ubication.split(',')[0]?.trim()
    : apartment?.title ?? ''

  /* ── handlers ──────────────────────────────────────────── */

  const toggleTenant = (memberId: number) => {
    setTenants((prev) =>
      prev.map((t) =>
        t.member.id === memberId
          ? { ...t, selected: !t.selected, customAmount: '' }
          : t,
      ),
    )
  }

  const setCustomAmount = (memberId: number, value: string) => {
    const num = parseFloat(value)
    if (value !== '' && !Number.isNaN(num) && num < 0) return
    setTenants((prev) =>
      prev.map((t) => (t.member.id === memberId ? { ...t, customAmount: value } : t)),
    )
  }

  const handleSubmit = async () => {
    if (!concept) {
      showToast('Selecciona un concepto', 'error')
      return
    }
    if (!totalAmount || total <= 0) {
      showToast('Introduce un importe válido', 'error')
      return
    }
    if (total > MAX_AMOUNT) {
      showToast(`El importe máximo es ${fmtCurrency(MAX_AMOUNT)}`, 'error')
      return
    }
    if (!dueDate) {
      showToast('Selecciona una fecha de vencimiento', 'error')
      return
    }
    if (autoCount === 0 && manualSum === 0) {
      showToast('Asigna la factura a al menos un inquilino', 'error')
      return
    }
    if (!isDistributionValid) {
      showToast('El reparto no cuadra. Revisa los importes manuales.', 'warning')
      return
    }

    setIsSending(true)
    try {
      await createBill(apartmentId, {
        reference: concept,
        totalAmount: total,
        duDate: dueDate,
      })
      showToast('Factura creada y notificada a los inquilinos', 'success')
      navigate(`/apartments/${apartmentId}`)
    } catch {
      showToast('Error al crear la factura', 'error')
    } finally {
      setIsSending(false)
    }
  }

  /* ── render ────────────────────────────────────────────── */

  if (isLoading) {
    return (
      <div className="min-h-dvh bg-[#F7F4EB] flex items-center justify-center">
        <p className="text-gray-500">Cargando datos del piso…</p>
      </div>
    )
  }

  return (
    <div className="min-h-dvh bg-[#F7F4EB] pb-32">
      {/* ─── Header ──────────────────────────────────────── */}
      <header className="sticky top-0 z-30 bg-[#F7F4EB] px-4 pt-5 pb-3">
        <div className="max-w-xl mx-auto flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="w-9 h-9 flex items-center justify-center rounded-full hover:bg-white/60 transition"
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

          <h1 className="text-lg font-bold text-gray-900 flex-1 text-center pr-9">
            Nueva Factura – {locationLabel}
          </h1>
        </div>

        <p className="max-w-xl mx-auto mt-1 text-sm text-gray-500 text-center">
          Registra los gastos de esta propiedad y divídelos entre tus inquilinos.
        </p>
      </header>

      <main className="max-w-xl mx-auto px-4 space-y-5 mt-2">
        {/* ─── Concepto ────────────────────────────────────── */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Concepto</label>
          <div className="relative">
            <select
              value={concept}
              onChange={(e) => setConcept(e.target.value)}
              className="w-full appearance-none bg-white border border-gray-200 rounded-xl px-4 py-3 pr-10 text-gray-700 focus:outline-none focus:ring-2 focus:ring-teal-400 transition"
            >
              <option value="" disabled>
                Seleccionar servicio…
              </option>
              {CONCEPTS.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="absolute right-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400 pointer-events-none"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </div>
        </div>

        {/* ─── Importe + Vencimiento ───────────────────────── */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Importe Total</label>
            <input
              type="number"
              min="0"
              max={MAX_AMOUNT}
              step="0.01"
              value={totalAmount}
              onChange={(e) => {
                const v = parseFloat(e.target.value)
                if (e.target.value === '' || (v >= 0 && v <= MAX_AMOUNT)) {
                  setTotalAmount(e.target.value)
                }
              }}
              placeholder="0,00 €"
              className="w-full bg-white border border-gray-200 rounded-xl px-4 py-3 text-gray-700 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-400 transition"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Vencimiento</label>
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className="w-full bg-white border border-gray-200 rounded-xl px-4 py-3 text-gray-700 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-400 transition"
            />
          </div>
        </div>

        {/* ─── Comprobante ──────────────────────────────────── */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Comprobante</label>
          <label className="flex flex-col items-center justify-center gap-2 border-2 border-dashed border-teal-400 bg-teal-50/50 rounded-xl py-8 cursor-pointer hover:bg-teal-50 transition">
            <input
              type="file"
              accept="image/*,application/pdf"
              className="hidden"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-8 w-8 text-teal-700"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M3 7h2l2-3h10l2 3h2a1 1 0 011 1v11a1 1 0 01-1 1H3a1 1 0 01-1-1V8a1 1 0 011-1z"
              />
              <circle cx="12" cy="13" r="4" stroke="currentColor" strokeWidth={1.5} fill="none" />
            </svg>

            {file ? (
              <span className="text-sm font-medium text-teal-700 truncate max-w-[80%]">
                {file.name}
              </span>
            ) : (
              <span className="text-sm font-medium text-teal-700">Adjuntar PDF o Foto</span>
            )}
          </label>
        </div>

        {/* ─── Asignar a inquilino ──────────────────────────── */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-base font-bold text-gray-900">Asignar a inquilino</h2>
            {total > 0 && (
              <span
                className={`text-xs font-semibold px-2.5 py-1 rounded-full ${
                  Math.abs(unassigned) < 0.01
                    ? 'bg-green-100 text-green-700'
                    : 'bg-amber-100 text-amber-700'
                }`}
              >
                {Math.abs(unassigned) < 0.01
                  ? '✓ Reparto completo'
                  : `Faltan ${fmtCurrency(Math.max(0, unassigned))}`}
              </span>
            )}
          </div>

          {tenants.length === 0 ? (
            <p className="text-sm text-gray-500 bg-white rounded-xl p-4 text-center">
              Este piso no tiene inquilinos registrados.
            </p>
          ) : (
            <ul className="space-y-3">
              {tenants.map((t, idx) => {
                const palette = AVATAR_PALETTES[idx % AVATAR_PALETTES.length]
                const displayName = getDisplayName(t.user, t.member.id)
                const initials = getInitials(displayName)

                const effectiveAmount = t.selected
                  ? autoShare
                  : parseFloat(t.customAmount) || 0

                const otherManual = tenants
                  .filter((o) => !o.selected && o.member.id !== t.member.id)
                  .reduce((s, o) => s + (parseFloat(o.customAmount) || 0), 0)
                const manualMax = Math.max(0, total - otherManual)

                return (
                  <li
                    key={t.member.id}
                    className={`bg-white rounded-2xl px-4 py-3 border-2 transition ${
                      t.selected
                        ? 'border-teal-600 shadow-sm'
                        : effectiveAmount > 0
                          ? 'border-teal-300'
                          : 'border-gray-200'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <button
                        type="button"
                        onClick={() => toggleTenant(t.member.id)}
                        className={`shrink-0 w-6 h-6 rounded-md flex items-center justify-center transition ${
                          t.selected ? 'bg-teal-600' : 'border-2 border-gray-300 bg-white'
                        }`}
                      >
                        {t.selected && (
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            className="h-4 w-4 text-white"
                            viewBox="0 0 20 20"
                            fill="currentColor"
                          >
                            <path
                              fillRule="evenodd"
                              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                              clipRule="evenodd"
                            />
                          </svg>
                        )}
                      </button>

                      <div className="flex-1 min-w-0">
                        <span className="font-semibold text-gray-900">{displayName}</span>
                        <span className="ml-2 text-sm text-gray-400">
                          {t.selected
                            ? fmtCurrency(autoShare)
                            : effectiveAmount > 0
                              ? fmtCurrency(effectiveAmount)
                              : '—'}
                        </span>
                        {t.selected && (
                          <span className="ml-1.5 text-[11px] text-teal-600 font-medium">auto</span>
                        )}
                      </div>

                      <span
                        className={`shrink-0 w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold ${palette.bg} ${palette.text}`}
                      >
                        {initials}
                      </span>
                    </div>

                    {!t.selected && (
                      <div className="mt-2.5 pl-9">
                        <label className="text-xs text-gray-500 mb-0.5 block">
                          Importe manual (máx. {fmtCurrency(manualMax)})
                        </label>
                        <input
                          type="number"
                          min="0"
                          max={manualMax}
                          step="0.01"
                          value={t.customAmount}
                          onClick={(e) => e.stopPropagation()}
                          onChange={(e) => {
                            const v = parseFloat(e.target.value)
                            if (e.target.value === '' || (v >= 0 && v <= manualMax)) {
                              setCustomAmount(t.member.id, e.target.value)
                            }
                          }}
                          placeholder="0,00 €"
                          className="w-full bg-gray-50 border border-gray-200 rounded-xl px-3 py-2 text-sm text-gray-700 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-400 transition"
                        />
                      </div>
                    )}
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </main>

      {/* ─── Footer button ──────────────────────────────── */}
      <div className="fixed bottom-0 inset-x-0 bg-[#F7F4EB]/90 backdrop-blur px-4 py-4">
        <div className="max-w-xl mx-auto space-y-1.5">
          {total > 0 && !isDistributionValid && (
            <p className="text-center text-xs text-amber-600 font-medium">
              Reparte el 100 % del importe antes de enviar
            </p>
          )}

          <button
            disabled={isSending || !isDistributionValid}
            onClick={() => void handleSubmit()}
            className="w-full flex items-center justify-center gap-2 bg-teal-700 hover:bg-teal-800 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold py-4 rounded-2xl shadow-lg transition text-base"
          >
            {isSending ? (
              'Enviando…'
            ) : (
              <>
                Subir y Notificar
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
                  />
                </svg>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
