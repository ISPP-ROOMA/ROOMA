import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getApartment, getMyApartments, type Apartment } from '../../service/apartments.service'
import { useAuthStore } from '../../store/authStore'

const CLOUDINARY_CLOUD_NAME = 'djuqshdey'

const HEADER_CLASS = 'px-4 pb-4 pt-6 sm:px-6 md:px-10 md:pb-6 md:pt-8'
const MAIN_CLASS = 'mx-auto max-w-5xl px-4 py-6 sm:px-6 md:px-10 md:py-8'

const STATE_CONFIG: Record<string, { label: string; dotClass: string }> = {
  ACTIVE: { label: 'Activo', dotClass: 'bg-emerald-500' },
  MATCHING: { label: 'En matching', dotClass: 'bg-amber-500' },
  CLOSED: { label: 'Cerrado', dotClass: 'bg-gray-400' },
}

export default function ApartmentDetail() {
  const { id } = useParams()
  const role = useAuthStore().role
  const navigate = useNavigate()
  const [apartment, setApartment] = useState<Apartment | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isForbidden, setIsForbidden] = useState(false)

  useEffect(() => {
    const fetch = async () => {
      if (!id) {
        setIsLoading(false)
        return
      }

      const apartmentId = Number(id)
      if (!Number.isFinite(apartmentId)) {
        setIsLoading(false)
        return
      }

      try {
        if (role === 'LANDLORD') {
          const myApartments = await getMyApartments()
          const isOwner = myApartments.some((myApartment) => myApartment.id === apartmentId)
          if (!isOwner) {
            setIsForbidden(true)
            return
          }
        }

        const data = await getApartment(apartmentId)
        if (data) setApartment(data)
      } catch (error) {
        console.error(error)
      } finally {
        setIsLoading(false)
      }
    }
    fetch()
  }, [id])

  if (isLoading) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-base-200/50">
        <p className="text-gray-500 opacity-50">Cargando inmueble...</p>
      </div>
    )
  }

  if (!apartment) {
    return (
      <div className="flex min-h-dvh flex-col items-center justify-center gap-4 bg-base-200/50">
        {isForbidden ? (
          <p className="text-lg font-medium text-gray-500">No tienes permiso para ver este inmueble</p>
        ) : (
          <p className="text-lg font-medium text-gray-500">Inmueble no encontrado</p>
        )}
        <div className="flex h-20 w-20 items-center justify-center rounded-full bg-base-200">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-10 w-10 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </div>
        <Link
          to="/apartments/my"
          className="text-sm font-semibold text-teal-600 transition hover:text-teal-700"
        >
          Volver a Mis Inmuebles
        </Link>
      </div>
    )
  }

  const stateInfo = STATE_CONFIG[apartment.state] ?? {
    label: apartment.state,
    dotClass: 'bg-gray-400',
  }

  const coverSrc = apartment.coverImageUrl
    ? apartment.coverImageUrl.startsWith('http')
      ? apartment.coverImageUrl
      : `https://res.cloudinary.com/${CLOUDINARY_CLOUD_NAME}/image/upload/f_auto,q_auto/${apartment.coverImageUrl}`
    : null

  return (
    <div className="min-h-dvh bg-[radial-gradient(circle_at_top_left,rgba(20,184,166,0.08),transparent_26%),radial-gradient(circle_at_bottom_right,rgba(15,23,42,0.06),transparent_22%),var(--fallback-b2,oklch(var(--b2)))]">
      <header className={HEADER_CLASS}>
        <div className="mx-auto max-w-5xl rounded-[30px] border border-base-300/70 bg-[radial-gradient(circle_at_top_left,rgba(20,184,166,0.12),transparent_30%),linear-gradient(135deg,rgba(255,255,255,1),rgba(248,250,252,0.94))] p-5 shadow-[0_18px_48px_rgba(15,23,42,0.08)] sm:p-6">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="btn btn-outline btn-sm rounded-full border-base-content/20 bg-white/85 px-5 shadow-sm transition hover:-translate-y-0.5 hover:bg-white"
          >
            Volver
          </button>

          <div className="mt-5 min-w-0">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary/70">
              Property overview
            </p>
            <h1 className="mt-2 truncate text-3xl font-bold text-base-content sm:text-4xl">
              {apartment.title}
            </h1>
            <p className="mt-2 text-sm text-gray-400">Detalle del inmueble</p>
          </div>
        </div>
      </header>

      <main className={MAIN_CLASS}>
        <div className="overflow-hidden rounded-[32px] border border-base-300/70 bg-base-100 shadow-[0_22px_56px_rgba(15,23,42,0.09)]">
          <div className="relative h-56 w-full bg-gray-100 sm:h-72">
            {coverSrc ? (
              <img
                src={coverSrc}
                alt={apartment.title}
                className="h-full w-full object-cover"
                loading="lazy"
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-20 w-20 text-gray-300"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M3 11.5L12 4l9 7.5V20a1 1 0 01-1 1h-5v-6H9v6H4a1 1 0 01-1-1v-8.5z"
                  />
                </svg>
              </div>
            )}

            <span className="absolute left-4 top-4 flex items-center gap-1.5 rounded-full bg-white/90 px-3 py-1.5 text-xs font-semibold text-gray-700 shadow-sm backdrop-blur">
              <span className={`inline-block h-2 w-2 rounded-full ${stateInfo.dotClass}`} />
              {stateInfo.label}
            </span>

            <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/50 via-black/15 to-transparent px-5 py-6">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-white/80">
                Gestion del inmueble
              </p>
              <p className="mt-1 text-sm text-white/90">
                Facturas, estado y acceso directo a incidencias desde un mismo panel.
              </p>
            </div>
          </div>

          <div className="p-5 sm:p-8">
            <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
              <div className="flex items-baseline gap-1">
                <span className="text-3xl font-extrabold text-primary">{apartment.price} EUR</span>
                <span className="text-sm text-gray-400">/ mes</span>
              </div>

              <div className="flex flex-wrap gap-2">
                <span className="rounded-full border border-base-300 bg-base-100 px-3 py-1.5 text-xs text-base-content/60">
                  {stateInfo.label}
                </span>
                <span className="rounded-full border border-base-300 bg-base-100 px-3 py-1.5 text-xs text-base-content/60">
                  {apartment.ubication || 'Ubicacion sin definir'}
                </span>
              </div>
            </div>

            {apartment.description && (
              <p className="mb-6 text-gray-600 leading-relaxed">{apartment.description}</p>
            )}

            {apartment.idealTenantProfile && (
              <div className="mb-6 rounded-2xl bg-base-200 p-4">
                <span className="text-xs font-semibold uppercase tracking-[0.14em] text-gray-500">
                  Perfil ideal del inquilino
                </span>
                <p className="mt-1 whitespace-pre-wrap font-medium text-base-content">
                  {apartment.idealTenantProfile}
                </p>
              </div>
            )}

            <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
              <DetailMiniStat label="Estado" value={stateInfo.label} />
              <DetailMiniStat label="Precio" value={`${apartment.price} EUR`} />
              <DetailMiniStat label="Ubicacion" value={apartment.ubication || 'Pendiente'} />
              <DetailMiniStat label="Gastos" value={apartment.bills || 'Pendiente'} />
            </div>

            <div className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="rounded-[26px] border border-base-200/80 bg-gradient-to-br from-base-100 to-base-200/70 p-4 shadow-[0_10px_24px_rgba(15,23,42,0.05)] transition duration-300 hover:-translate-y-1 hover:shadow-[0_18px_38px_rgba(15,23,42,0.08)]">
                <span className="text-xs font-semibold uppercase tracking-[0.16em] text-gray-500">
                  Ubicacion
                </span>
                <p className="mt-1 font-medium text-base-content">
                  {apartment.ubication || 'No especificada'}
                </p>
              </div>
              <div className="rounded-[26px] border border-base-200/80 bg-gradient-to-br from-base-100 to-base-200/70 p-4 shadow-[0_10px_24px_rgba(15,23,42,0.05)] transition duration-300 hover:-translate-y-1 hover:shadow-[0_18px_38px_rgba(15,23,42,0.08)]">
                <span className="text-xs font-semibold uppercase tracking-[0.16em] text-gray-500">
                  Gastos
                </span>
                <p className="mt-1 font-medium text-base-content">
                  {apartment.bills || 'No especificados'}
                </p>
              </div>
            </div>

            { role === 'LANDLORD' && <div className="rounded-[30px] border border-base-200/80 bg-[linear-gradient(180deg,rgba(255,255,255,1),rgba(248,250,252,0.96))] p-4 shadow-[0_14px_34px_rgba(15,23,42,0.06)] sm:p-5">
              <div className="mb-4 flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70">
                    Acciones
                  </p>
                  <h2 className="mt-1 text-xl font-semibold">Gestion del inmueble</h2>
                </div>
                <span className="rounded-full border border-base-300 bg-white px-3 py-1 text-xs text-base-content/55">
                  Acceso rapido
                </span>
              </div>

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                <button
                  onClick={() => navigate(`/apartments/${id}/bills`)}
                  className="flex min-h-14 w-full items-center justify-center gap-2 rounded-[22px] bg-teal-600 px-6 py-3 text-sm font-semibold text-white shadow-md transition duration-300 hover:-translate-y-1 hover:shadow-lg hover:bg-teal-700"
                >
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
                      d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z"
                    />
                  </svg>
                  Ver Facturas
                </button>

                <button
                  onClick={() => navigate(`/apartments/${id}/new-bill`)}
                  className="flex min-h-14 w-full items-center justify-center gap-2 rounded-[22px] border-2 border-teal-600 bg-white px-6 py-3 text-sm font-semibold text-teal-700 shadow-sm transition duration-300 hover:-translate-y-1 hover:shadow-md hover:bg-teal-50"
                >
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
                      d="M12 4v16m8-8H4"
                    />
                  </svg>
                  Anadir Factura
                </button>

                <button
                  onClick={() => navigate(`/apartments/${id}/incidences`)}
                  className="flex min-h-14 w-full items-center justify-center gap-2 rounded-[22px] bg-slate-900 px-6 py-3 text-sm font-semibold text-white shadow-md transition duration-300 hover:-translate-y-1 hover:shadow-lg hover:bg-slate-800"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-5 w-5"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={2}
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M10.29 3.86L1.82 18a1 1 0 0 0 .86 1.5h18.64a1 1 0 0 0 .86-1.5L13.71 3.86a1 1 0 0 0-1.71 0zM12 9v4m0 4h.01"
                    />
                  </svg>
                  Ver Incidencias
                </button>
              </div>
            </div>}
          </div>
        </div>
      </main>
    </div>
  )
}

function DetailMiniStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[24px] border border-base-200/80 bg-white px-4 py-3 shadow-[0_10px_24px_rgba(15,23,42,0.04)] transition duration-300 hover:-translate-y-1 hover:shadow-[0_18px_38px_rgba(15,23,42,0.08)]">
      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-base-content/45">
        {label}
      </p>
      <p className="mt-1 line-clamp-2 text-sm font-semibold leading-6 text-base-content">
        {value}
      </p>
    </div>
  )
}
