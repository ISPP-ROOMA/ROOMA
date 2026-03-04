import { Loader2, MapPin } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  acceptApartmentMatch,
  getLandlordMatchDetails,
  rejectApartmentMatch,
  type ApartmentMatchTenantDetailsDTO,
} from '../../../service/apartment.service'

const FALLBACK_COVER =
  'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80'

const FALLBACK_AVATAR =
  'https://ui-avatars.com/api/?background=DDDBCB&color=050505&name=Tenant&size=128'

export default function LandlordRequestDetailPage() {
  const navigate = useNavigate()
  const { apartmentMatchId } = useParams<{ apartmentMatchId: string }>()

  const [details, setDetails] = useState<ApartmentMatchTenantDetailsDTO | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<'accept' | 'reject' | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const parsedId = useMemo(() => Number(apartmentMatchId), [apartmentMatchId])

  useEffect(() => {
    if (!parsedId || Number.isNaN(parsedId)) {
      setError('Solicitud no válida.')
      setLoading(false)
      return
    }

    const fetchDetails = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await getLandlordMatchDetails(parsedId)
        if (!response) {
          setError('No se pudo cargar el detalle de la solicitud.')
          return
        }
        setDetails(response)
      } catch (err) {
        console.error('Error loading landlord request details', err)
        setError('No se pudo cargar el detalle de la solicitud.')
      } finally {
        setLoading(false)
      }
    }

    void fetchDetails()
  }, [parsedId])

  if (loading) {
    return (
      <div
        data-theme="light"
        className="mx-auto w-full max-w-2xl min-h-dvh bg-[#F5F1E3] text-[#050505] px-4 sm:px-8 pt-12"
      >
        <div className="flex flex-col items-center gap-4 pt-16 text-[#008080]">
          <Loader2 className="animate-spin" size={40} />
          <p className="font-medium animate-pulse">Cargando solicitud...</p>
        </div>
      </div>
    )
  }

  if (error || !details) {
    return (
      <div
        data-theme="light"
        className="mx-auto w-full max-w-2xl min-h-dvh bg-[#F5F1E3] text-[#050505] px-4 sm:px-8 pt-6"
      >
        <button
          className="h-10 w-10 rounded-full bg-white text-[#050505] shadow-sm flex items-center justify-center shrink-0"
          aria-label="Volver"
          onClick={() => navigate(-1)}
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-5 w-5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <path d="M15 18l-6-6 6-6" />
          </svg>
        </button>

        <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-6 text-center">
          <p className="text-red-600 font-medium">{error ?? 'No se encontró la solicitud.'}</p>
        </div>
      </div>
    )
  }

  const coverImage = details.apartment.coverImageUrl ?? FALLBACK_COVER
  const apartmentPrice = `${details.apartment.price.toLocaleString('es-ES')} €`
  const apartmentLocation = details.apartment.ubication ?? 'Ubicación no disponible'
  const tenantEmail = details.tenant?.email ?? 'Email no disponible'
  const tenantProfession = details.tenant?.profession ?? 'Profesión no disponible'
  const tenantProfileImage = (details.tenant as { profileImageUrl?: string } | undefined)?.profileImageUrl
  const tenantAvatar = tenantProfileImage ?? FALLBACK_AVATAR

  const isActiveRequest = details.matchStatus === 'ACTIVE'

  const handleReject = async () => {
    setActionLoading('reject')
    setActionError(null)
    try {
      await rejectApartmentMatch(details.id)
      navigate('/mis-solicitudes/recibidas')
    } catch (err) {
      console.error('Error rejecting match from detail', err)
      setActionError('No se pudo rechazar la solicitud. Inténtalo de nuevo.')
    } finally {
      setActionLoading(null)
    }
  }

  const handleAccept = async () => {
    setActionLoading('accept')
    setActionError(null)
    try {
      await acceptApartmentMatch(details.id)
      navigate('/mis-solicitudes/recibidas')
    } catch (err) {
      console.error('Error accepting match from detail', err)
      setActionError('No se pudo aceptar la solicitud. Inténtalo de nuevo.')
    } finally {
      setActionLoading(null)
    }
  }

  return (
    <div
      data-theme="light"
      className="mx-auto w-full max-w-2xl min-h-dvh bg-[#F5F1E3] text-[#050505] pb-10"
    >
      <header className="sticky top-0 z-10 bg-[#F5F1E3] px-4 sm:px-8 pt-5 pb-4">
        <div className="flex items-center justify-between">
          <button
            className="h-10 w-10 rounded-full bg-white text-[#050505] shadow-sm flex items-center justify-center shrink-0"
            aria-label="Volver"
            onClick={() => navigate(-1)}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path d="M15 18l-6-6 6-6" />
            </svg>
          </button>
          <h1 className="text-2xl sm:text-3xl font-bold text-[#050505]">Detalle solicitud</h1>
          <div className="h-10 w-10" aria-hidden />
        </div>
      </header>

      <section className="px-4 sm:px-8">
        <article className="overflow-hidden rounded-2xl border border-[#DDDBCB] bg-white shadow-sm">
          <div className="relative h-72 w-full">
            <img src={coverImage} alt={details.apartment.title} className="h-full w-full object-cover" />
            <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/15 to-transparent" />

            <div className="absolute bottom-0 left-0 right-0 p-5 text-white">
              <div className="flex items-end justify-between gap-3">
                <h2 className="text-2xl font-bold leading-tight line-clamp-2">{details.apartment.title}</h2>
                <span className="shrink-0 rounded-lg bg-black/35 px-2.5 py-1 text-lg font-semibold backdrop-blur-sm">
                  {apartmentPrice}
                </span>
              </div>

              <p className="mt-2 flex items-center gap-1.5 text-sm text-white/95">
                <MapPin size={15} />
                <span className="line-clamp-1">{apartmentLocation}</span>
              </p>
            </div>
          </div>

          <div className="p-5">
            <h3 className="text-lg font-bold text-[#050505]">Información del usuario</h3>

            <div className="mt-4 flex items-center gap-4 rounded-2xl border border-[#DDDBCB] bg-[#F5F1E3] p-4">
              <img
                src={tenantAvatar}
                alt="Foto de perfil del solicitante"
                className="h-16 w-16 rounded-full object-cover border border-[#DDDBCB] bg-white"
                onError={(event) => {
                  event.currentTarget.src = FALLBACK_AVATAR
                }}
              />

              <div className="min-w-0">
                <p className="text-sm text-[#050505]/60">Email</p>
                <p className="font-semibold text-[#050505] break-all">{tenantEmail}</p>

                <p className="mt-2 text-sm text-[#050505]/60">Profesión</p>
                <p className="font-medium text-[#050505]">{tenantProfession}</p>
              </div>
            </div>

            <div className="mt-4">
              <h4 className="text-base font-bold text-[#050505]">Reputación</h4>
              <div className="mt-2 rounded-2xl border border-[#DDDBCB] bg-[#F5F1E3] p-4">
                <p className="text-sm text-[#050505]/70">Próximamente...</p>
              </div>
            </div>

            <div className="mt-4">
              <h4 className="text-base font-bold text-[#050505]">Reseñas de otros caseros</h4>
              <div className="mt-2 rounded-2xl border border-[#DDDBCB] bg-[#F5F1E3] p-4">
                <p className="text-sm text-[#050505]/70">Próximamente...</p>
              </div>
            </div>

            {isActiveRequest && (
              <div className="mt-5 grid grid-cols-1 gap-3">
                <button
                  className="w-full rounded-xl border border-[#DDDBCB] bg-white px-4 py-3 text-sm font-semibold text-[#050505] transition-colors hover:bg-[#F5F1E3] disabled:cursor-not-allowed disabled:opacity-60"
                  onClick={() => void handleReject()}
                  disabled={actionLoading !== null}
                >
                  <span className="flex items-center justify-center gap-2">
                    {actionLoading === 'reject' && <Loader2 size={15} className="animate-spin" />}
                    Rechazar solicitud
                  </span>
                </button>
                <button
                  className="w-full rounded-xl bg-[#008080] px-4 py-3 text-sm font-semibold text-white transition-colors hover:bg-[#006d6d] disabled:cursor-not-allowed disabled:opacity-60"
                  onClick={() => void handleAccept()}
                  disabled={actionLoading !== null}
                >
                  <span className="flex items-center justify-center gap-2">
                    {actionLoading === 'accept' && <Loader2 size={15} className="animate-spin" />}
                    Aceptar solicitud
                  </span>
                </button>
              </div>
            )}

            {actionError && (
              <p className="mt-3 text-sm font-medium text-red-600">{actionError}</p>
            )}
          </div>
        </article>
      </section>
    </div>
  )
}
