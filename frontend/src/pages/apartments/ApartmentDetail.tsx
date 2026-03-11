import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { getApartment, type Apartment } from '../../service/apartments.service'

const CLOUDINARY_CLOUD_NAME = 'djuqshdey'

const HEADER_CLASS = 'bg-base-100 px-6 pt-8 pb-6 md:px-12'
const MAIN_CLASS = 'px-6 md:px-12 py-8 max-w-4xl mx-auto'

const STATE_CONFIG: Record<string, { label: string; dotClass: string }> = {
  ACTIVE: { label: 'Activo', dotClass: 'bg-emerald-500' },
  MATCHING: { label: 'En matching', dotClass: 'bg-amber-500' },
  CLOSED: { label: 'Cerrado', dotClass: 'bg-gray-400' },
}

export default function ApartmentDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [apartment, setApartment] = useState<Apartment | null>(null)
  const [isLoading, setIsLoading] = useState(true)

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
      <div className="min-h-dvh bg-base-200/50 flex items-center justify-center">
        <p className="text-gray-500 opacity-50">Cargando inmueble...</p>
      </div>
    )
  }

  if (!apartment) {
    return (
      <div className="min-h-dvh bg-base-200/50 flex flex-col items-center justify-center gap-4">
        <div className="w-20 h-20 rounded-full bg-base-200 flex items-center justify-center">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-10 w-10 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <p className="text-gray-500 text-lg font-medium">Inmueble no encontrado</p>
        <Link to="/apartments/my" className="text-teal-600 hover:text-teal-700 font-semibold text-sm transition">
          Volver a Mis Inmuebles
        </Link>
      </div>
    )
  }

  const stateInfo = STATE_CONFIG[apartment.state] ?? { label: apartment.state, dotClass: 'bg-gray-400' }

  const coverSrc = apartment.coverImageUrl
    ? apartment.coverImageUrl.startsWith('http')
      ? apartment.coverImageUrl
      : `https://res.cloudinary.com/${CLOUDINARY_CLOUD_NAME}/image/upload/f_auto,q_auto/${apartment.coverImageUrl}`
    : null

  return (
    <div className="min-h-dvh bg-base-200/50">
      {/* Header */}
      <header className={HEADER_CLASS}>
        <div className="max-w-4xl mx-auto flex items-center gap-3">
          <Link
            to="/apartments/my"
            className="flex items-center justify-center w-10 h-10 rounded-full border border-gray-300 text-gray-600 hover:bg-gray-100 transition shrink-0"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </Link>
          <div className="min-w-0">
            <h1 className="text-3xl font-bold text-base-content truncate">{apartment.title}</h1>
            <p className="text-gray-400 mt-1 text-sm">Detalle del inmueble</p>
          </div>
        </div>
      </header>

      {/* Main */}
      <main className={MAIN_CLASS}>
        <div className="bg-base-100 rounded-3xl shadow-md overflow-hidden">
          {/* Cover image */}
          <div className="relative h-56 sm:h-72 w-full bg-gray-100">
            {coverSrc ? (
              <img src={coverSrc} alt={apartment.title} className="h-full w-full object-cover" loading="lazy" />
            ) : (
              <div className="h-full w-full flex items-center justify-center">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-20 w-20 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 11.5L12 4l9 7.5V20a1 1 0 01-1 1h-5v-6H9v6H4a1 1 0 01-1-1v-8.5z" />
                </svg>
              </div>
            )}

            {/* Status badge */}
            <span className="absolute top-4 left-4 flex items-center gap-1.5 bg-white/90 backdrop-blur px-3 py-1.5 rounded-full text-xs font-semibold text-gray-700 shadow-sm">
              <span className={`inline-block h-2 w-2 rounded-full ${stateInfo.dotClass}`} />
              {stateInfo.label}
            </span>
          </div>

          {/* Content */}
          <div className="p-6 sm:p-8">
            {/* Price */}
            <div className="flex items-baseline gap-1 mb-4">
              <span className="text-3xl font-extrabold text-primary">{apartment.price} €</span>
              <span className="text-sm text-gray-400">/ mes</span>
            </div>

            {/* Description */}
            {apartment.description && (
              <p className="text-gray-600 leading-relaxed mb-6">{apartment.description}</p>
            )}

            {/* Info grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
              <div className="bg-base-200 rounded-2xl p-4">
                <span className="text-xs text-gray-500 font-semibold uppercase">Ubicación</span>
                <p className="font-medium text-base-content mt-1">{apartment.ubication || 'No especificada'}</p>
              </div>
              <div className="bg-base-200 rounded-2xl p-4">
                <span className="text-xs text-gray-500 font-semibold uppercase">Gastos</span>
                <p className="font-medium text-base-content mt-1">{apartment.bills || 'No especificados'}</p>
              </div>
            </div>

            {/* Actions */}
            <div className="flex flex-wrap gap-3">
              <button
                onClick={() => navigate(`/apartments/${id}/bills`)}
                className="flex items-center gap-2 bg-teal-600 hover:bg-teal-700 text-white font-semibold px-6 py-3 rounded-full shadow-md transition text-sm"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z" />
                </svg>
                Ver Facturas
              </button>

              <button
                onClick={() => navigate(`/apartments/${id}/new-bill`)}
                className="flex items-center gap-2 bg-white border-2 border-teal-600 text-teal-700 hover:bg-teal-50 font-semibold px-6 py-3 rounded-full shadow-sm transition text-sm"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                Añadir Factura
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
