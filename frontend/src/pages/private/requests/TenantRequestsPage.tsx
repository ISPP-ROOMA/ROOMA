import { AnimatePresence } from 'framer-motion'
import { CalendarDays, Loader2, MessageCircle, X } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ApartmentDetailModal from '../../../components/ApartmentDetailModal'
import type {
  ApartmentDTO,
  ApartmentMatchDTO,
  MatchStatus,
} from '../../../service/apartment.service'
import {
  cancelApartmentMatch,
  getMatchesForCandidate,
  respondToInvitation,
} from '../../../service/apartment.service'
import { getApartment } from '../../../service/apartments.service'
import { useAuthStore } from '../../../store/authStore'

type ActiveTab = 'pending' | 'match'

interface EnrichedMatch {
  matchId: number
  apartmentId: number
  matchStatus: MatchStatus
  title: string
  location: string
  price: string
  imageUrl: string
}

function statusLabel(status: MatchStatus): string {
  switch (status) {
    case 'ACTIVE':
      return 'Pendiente'
    case 'MATCH':
      return '¡Match!'
    case 'INVITED':
      return 'Invitado'
    case 'SUCCESSFUL':
      return 'Aceptada'
    case 'REJECTED':
      return 'Rechazada'
    case 'CANCELED':
      return 'Cancelada'
  }
}

function statusBadgeClass(status: MatchStatus): string {
  switch (status) {
    case 'ACTIVE':
      return 'border border-[#050505] bg-white text-[#050505]'
    case 'MATCH':
    case 'INVITED':
    case 'SUCCESSFUL':
      return 'border-0 bg-[#008080] text-white'
    case 'REJECTED':
    case 'CANCELED':
      return 'border border-[#DDDBCB] bg-[#F5F1E3] text-[#050505]/60'
  }
}

async function enrichMatches(matches: ApartmentMatchDTO[]): Promise<EnrichedMatch[]> {
  const enriched = await Promise.all(
    matches.map(async (match) => {
      const apt = await getApartment(match.apartmentId)
      return {
        matchId: match.id,
        apartmentId: match.apartmentId,
        matchStatus: match.matchStatus,
        title: apt?.title ?? `Vivienda #${match.apartmentId}`,
        location: apt?.ubication ?? '—',
        price: apt ? `${apt.price.toLocaleString('es-ES')} €` : '—',
        imageUrl:
          apt?.coverImageUrl ??
          'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80',
      } satisfies EnrichedMatch
    })
  )
  return enriched
}

export default function TenantRequestsPage() {
  const { userId } = useAuthStore()
  const navigate = useNavigate()

  const [activeTab, setActiveTab] = useState<ActiveTab>('pending')
  const [pendingItems, setPendingItems] = useState<EnrichedMatch[]>([])
  const [matchItems, setMatchItems] = useState<EnrichedMatch[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [cancellingId, setCancellingId] = useState<number | null>(null)
  const [invitationActionId, setInvitationActionId] = useState<number | null>(null)
  const [selectedApartment, setSelectedApartment] = useState<
    (ApartmentDTO & { imageUrl: string }) | null
  >(null)
  const [modalLoading, setModalLoading] = useState<number | null>(null)

  const fetchData = useCallback(async () => {
    if (!userId) return
    setLoading(true)
    setError(null)
    try {
      const id = Number(userId)
      const [activeMatches, invitedMatches, fullMatches] = await Promise.all([
        getMatchesForCandidate(id, 'ACTIVE'),
        getMatchesForCandidate(id, 'INVITED'),
        getMatchesForCandidate(id, 'MATCH'),
      ])

      const [enrichedPending, enrichedInvited, enrichedFull] = await Promise.all([
        enrichMatches(activeMatches),
        enrichMatches(invitedMatches),
        enrichMatches(fullMatches),
      ])

      setPendingItems(enrichedPending)
      setMatchItems([...enrichedFull, ...enrichedInvited])
    } catch (err) {
      console.error('Error loading requests', err)
      setError('No se pudieron cargar tus solicitudes. Inténtalo de nuevo.')
    } finally {
      setLoading(false)
    }
  }, [userId])

  useEffect(() => {
    void fetchData()
  }, [fetchData])

  const handleCancel = async (matchId: number) => {
    setCancellingId(matchId)
    // Optimistic update
    setPendingItems((prev) => prev.filter((i) => i.matchId !== matchId))
    try {
      await cancelApartmentMatch(matchId)
    } catch (err) {
      console.error('Error cancelling match', err)
      // On failure, re-fetch to restore accurate state
      void fetchData()
    } finally {
      setCancellingId(null)
    }
  }

  const handleCardClick = async (item: EnrichedMatch, e: React.MouseEvent) => {
    // Don't open modal when clicking the cancel button
    if ((e.target as HTMLElement).closest('button')) return
    setModalLoading(item.matchId)
    try {
      const apt = await getApartment(item.apartmentId)
      if (apt) {
        setSelectedApartment({
          ...apt,
          imageUrl: apt.coverImageUrl ?? item.imageUrl,
        } as ApartmentDTO & { imageUrl: string })
      }
    } catch (err) {
      console.error('Error loading apartment details', err)
    } finally {
      setModalLoading(null)
    }
  }

  const handleInvitationResponse = async (matchId: number, accepted: boolean) => {
    setInvitationActionId(matchId)
    setMatchItems((prev) => prev.filter((i) => i.matchId !== matchId))
    try {
      await respondToInvitation(matchId, accepted)
    } catch (err) {
      console.error('Error responding to invitation', err)
      void fetchData()
    } finally {
      setInvitationActionId(null)
    }
  }

  const visibleItems = activeTab === 'pending' ? pendingItems : matchItems

  return (
    <div
      data-theme="light"
      className="mx-auto w-full max-w-5xl min-h-dvh bg-[#F5F1E3] text-[#050505] pb-28"
    >
      {/* ── Header ── */}
      <header className="sticky top-0 z-10 bg-[#FAF5EE] px-4 sm:px-8 pt-5 pb-4">
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
          <h1 className="text-xl sm:text-3xl font-bold text-[#050505] text-center px-2">
            Mis Solicitudes
          </h1>
          <div className="h-10 w-10" aria-hidden /> {/* spacer */}
        </div>
      </header>

      {/* ── Tabs ── */}
      <section className="px-4 sm:px-8">
        <div className="flex rounded-xl bg-[#DDDBCB] p-1">
          <button
            className={`flex-1 rounded-lg py-2 text-base font-medium transition-colors ${
              activeTab === 'pending' ? 'bg-white text-[#050505] shadow-sm' : 'text-[#050505]/70'
            }`}
            onClick={() => setActiveTab('pending')}
          >
            Pendientes
            {pendingItems.length > 0 && (
              <span className="ml-2 inline-flex h-5 w-5 items-center justify-center rounded-full bg-[#008080] text-white text-xs font-bold">
                {pendingItems.length}
              </span>
            )}
          </button>
          <button
            className={`flex-1 rounded-lg py-2 text-base font-medium transition-colors ${
              activeTab === 'match' ? 'bg-white text-[#050505] shadow-sm' : 'text-[#050505]/70'
            }`}
            onClick={() => setActiveTab('match')}
          >
            Match
            {matchItems.length > 0 && (
              <span className="ml-2 inline-flex h-5 w-5 items-center justify-center rounded-full bg-[#008080] text-white text-xs font-bold">
                {matchItems.length}
              </span>
            )}
          </button>
        </div>
      </section>

      {/* ── Body ── */}
      <section className="mt-5 px-4 sm:px-8">
        {loading ? (
          <div className="flex flex-col items-center gap-4 pt-16 text-[#008080]">
            <Loader2 className="animate-spin" size={40} />
            <p className="font-medium animate-pulse">Cargando solicitudes...</p>
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-center">
            <p className="text-red-600 font-medium">{error}</p>
            <button
              className="mt-4 rounded-xl bg-[#008080] px-4 py-2 text-sm text-white font-medium"
              onClick={() => void fetchData()}
            >
              Reintentar
            </button>
          </div>
        ) : visibleItems.length === 0 ? (
          <div className="rounded-2xl border border-[#DDDBCB] bg-white p-10 text-center">
            <div className="text-5xl mb-4">{activeTab === 'pending' ? '📋' : '🤝'}</div>
            <p className="text-[#050505]/70 font-medium">
              {activeTab === 'pending'
                ? 'Aún no tienes solicitudes activas. ¡Desliza pisos en el inicio!'
                : 'Todavía no tienes matches. ¡Sigue explorando!'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
            {visibleItems.map((item) => {
              const isCancelled = item.matchStatus === 'CANCELED' || item.matchStatus === 'REJECTED'
              const isMatch = item.matchStatus === 'MATCH' || item.matchStatus === 'INVITED'

              return (
                <article
                  key={item.matchId}
                  onClick={(e) => void handleCardClick(item, e)}
                  className={`overflow-hidden rounded-2xl border border-[#DDDBCB] bg-white shadow-sm transition-opacity cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-transform ${
                    isCancelled ? 'opacity-60' : ''
                  }`}
                >
                  {/* Image */}
                  <div className={`relative h-40 sm:h-44 w-full ${isCancelled ? 'grayscale' : ''}`}>
                    <img
                      src={item.imageUrl}
                      alt={item.title}
                      className="h-full w-full object-cover"
                    />
                    {/* Modal loading spinner on card */}
                    {modalLoading === item.matchId && (
                      <div className="absolute inset-0 bg-black/30 flex items-center justify-center">
                        <Loader2 size={28} className="animate-spin text-white" />
                      </div>
                    )}
                    {/* Match glow overlay */}
                    {isMatch && (
                      <div className="absolute inset-0 bg-gradient-to-t from-[#008080]/40 to-transparent pointer-events-none" />
                    )}
                    {/* Cancel button (only on ACTIVE) */}
                    {item.matchStatus === 'ACTIVE' && (
                      <button
                        className="absolute top-3 right-3 h-8 w-8 rounded-full bg-white/90 text-[#050505]/70 flex items-center justify-center shadow hover:bg-white hover:text-red-500 transition-colors disabled:opacity-50"
                        aria-label="Cancelar solicitud"
                        disabled={cancellingId === item.matchId}
                        onClick={() => void handleCancel(item.matchId)}
                      >
                        {cancellingId === item.matchId ? (
                          <Loader2 size={14} className="animate-spin" />
                        ) : (
                          <X size={14} />
                        )}
                      </button>
                    )}
                  </div>

                  {/* Info */}
                  <div className="p-4">
                    <div className="flex items-start justify-between gap-2">
                      <h2 className="text-lg sm:text-xl font-bold leading-tight text-[#050505] line-clamp-1">
                        {item.title}
                      </h2>
                      <span
                        className={`shrink-0 text-lg font-semibold ${
                          isCancelled ? 'text-[#050505]/50' : 'text-[#008080]'
                        }`}
                      >
                        {item.price}
                      </span>
                    </div>

                    <p className="mt-0.5 text-sm text-[#050505]/70 line-clamp-1">{item.location}</p>

                    <div className="my-3 h-px w-full bg-[#DDDBCB]" />

                    <div className="flex items-center justify-between gap-2">
                      <span
                        className={`rounded-full px-3 py-1 text-xs font-semibold ${statusBadgeClass(item.matchStatus)}`}
                      >
                        {statusLabel(item.matchStatus)}
                      </span>
                      {item.matchStatus === 'MATCH' && (
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            className="h-8 w-8 rounded-full border border-[#DDDBCB] bg-white text-[#008080] flex items-center justify-center"
                            aria-label="Abrir chat"
                            onClick={(e) => {
                              e.stopPropagation()
                              navigate(`/chat/${item.matchId}`)
                            }}
                          >
                            <MessageCircle size={16} />
                          </button>
                          <button
                            type="button"
                            className="h-8 w-8 rounded-full border border-[#DDDBCB] bg-white text-[#008080] flex items-center justify-center"
                            aria-label="Agendar cita"
                          >
                            <CalendarDays size={16} />
                          </button>
                        </div>
                      )}
                      {item.matchStatus === 'INVITED' && (
                        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
                          <button
                            type="button"
                            className="btn btn-sm btn-error btn-outline"
                            onClick={() => {
                              void handleInvitationResponse(item.matchId, false)
                            }}
                            disabled={invitationActionId === item.matchId}
                          >
                            {invitationActionId === item.matchId ? (
                              <Loader2 size={12} className="animate-spin" />
                            ) : (
                              'Rechazar'
                            )}
                          </button>
                          <button
                            type="button"
                            className="btn btn-sm btn-success"
                            onClick={() => {
                              void handleInvitationResponse(item.matchId, true)
                            }}
                            disabled={invitationActionId === item.matchId}
                          >
                            {invitationActionId === item.matchId ? (
                              <Loader2 size={12} className="animate-spin" />
                            ) : (
                              'Aceptar'
                            )}
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </section>

      {/* ── Detail Modal ── */}
      <AnimatePresence>
        {selectedApartment && (
          <ApartmentDetailModal
            apartment={selectedApartment}
            onClose={() => setSelectedApartment(null)}
          />
        )}
      </AnimatePresence>
    </div>
  )
}
