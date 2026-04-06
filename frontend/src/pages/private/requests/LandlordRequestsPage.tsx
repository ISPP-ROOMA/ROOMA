import { AnimatePresence } from 'framer-motion'
import { Loader2, MessageCircle } from 'lucide-react'
import type { IMessage, StompSubscription } from '@stomp/stompjs'
import { useCallback, useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import ApartmentDetailModal from '../../../components/ApartmentDetailModal'
import { useStompClient } from '../../../hooks/useStompClient'
import {
  CHAT_TOPIC_SUBSCRIPTION,
  getMessageHistory,
  type ChatMessageDTO,
} from '../../../service/chat.service'
import type {
  ApartmentDTO,
  ApartmentMatchDTO,
  MatchStatus,
} from '../../../service/apartment.service'
import {
  acceptApartmentMatch,
  getLandlordMatchDetails,
  getMatchesForLandlord,
  rejectApartmentMatch,
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
  tenantEmail: string
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
      const [apt, details] = await Promise.all([
        getApartment(match.apartmentId),
        getLandlordMatchDetails(match.id),
      ])
      const detailsApt = details?.apartment
      return {
        matchId: match.id,
        apartmentId: match.apartmentId,
        matchStatus: match.matchStatus,
        title: detailsApt?.title ?? apt?.title ?? `Vivienda #${match.apartmentId}`,
        location: detailsApt?.ubication ?? apt?.ubication ?? '—',
        price: detailsApt?.price
          ? `${detailsApt.price.toLocaleString('es-ES')} €`
          : apt?.price
            ? `${apt.price.toLocaleString('es-ES')} €`
            : '—',
        imageUrl:
          apt?.coverImageUrl ??
          'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80',
        tenantEmail: details?.tenant?.email ?? '—',
      } satisfies EnrichedMatch
    })
  )
  return enriched
}

export default function LandlordRequestsPage() {
  const { userId } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()

  const [activeTab, setActiveTab] = useState<ActiveTab>('pending')
  const [pendingItems, setPendingItems] = useState<EnrichedMatch[]>([])
  const [matchItems, setMatchItems] = useState<EnrichedMatch[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [updatingId, setUpdatingId] = useState<number | null>(null)
  const [selectedApartment, setSelectedApartment] = useState<
    (ApartmentDTO & { imageUrl: string }) | null
  >(null)
  const [modalLoading, setModalLoading] = useState<number | null>(null)
  const [unreadMatches, setUnreadMatches] = useState<Set<number>>(new Set())
  const { client, connected } = useStompClient()

  const fetchData = useCallback(async () => {
    if (!userId) return
    setLoading(true)
    setError(null)
    try {
      const id = Number(userId)
      const [activeMatches, successMatches, fullMatches] = await Promise.all([
        getMatchesForLandlord(id, 'ACTIVE'),
        getMatchesForLandlord(id, 'SUCCESSFUL'),
        getMatchesForLandlord(id, 'MATCH'),
      ])

      const [enrichedPending, enrichedSuccess, enrichedFull] = await Promise.all([
        enrichMatches(activeMatches),
        enrichMatches(successMatches),
        enrichMatches(fullMatches),
      ])

      setPendingItems(enrichedPending)
      setMatchItems([...enrichedFull, ...enrichedSuccess])
    } catch (err) {
      console.error('Error loading requests', err)
      setError('No se pudieron cargar tus solicitudes. Inténtalo de nuevo.')
    } finally {
      setLoading(false)
    }
  }, [userId])

  useEffect(() => {
    void fetchData()
  }, [fetchData, location.key])

  useEffect(() => {
    const chatableMatches = matchItems.filter(item => 
      item.matchStatus === 'MATCH' || item.matchStatus === 'INVITED' || item.matchStatus === 'SUCCESSFUL'
    )
    
    if (chatableMatches.length === 0 || !userId) return

    let isMounted = true
    const subscriptions: StompSubscription[] = []

    const initializeUnread = async () => {
      const newUnread = new Set<number>()
      for (const item of chatableMatches) {
        try {
          const history = await getMessageHistory({ type: 'match', id: item.matchId })
          const hasUnread = history.some(m => m.senderId !== Number(userId) && m.status !== 'READ')
          if (hasUnread) {
            newUnread.add(item.matchId)
          }
        } catch (error) {
        }
      }
      if (isMounted) {
        setUnreadMatches(prev => {
           const merged = new Set(prev)
           newUnread.forEach(id => merged.add(id))
           return merged
        })
      }
    }

    void initializeUnread()

    if (connected && client) {
      for (const item of chatableMatches) {
        const sub = client.subscribe(
          CHAT_TOPIC_SUBSCRIPTION({ type: 'match', id: item.matchId }),
          (payload: IMessage) => {
             const newMessage = JSON.parse(payload.body) as ChatMessageDTO
             if (newMessage.senderId !== Number(userId) && newMessage.status !== 'READ') {
               setUnreadMatches(prev => new Set(prev).add(item.matchId))
             }
          }
        )
        subscriptions.push(sub)
      }
    }

    return () => {
      isMounted = false
      subscriptions.forEach(sub => sub.unsubscribe())
    }
  }, [matchItems, userId, connected, client])

  const handleReject = async (matchId: number) => {
    setUpdatingId(matchId)
    setPendingItems((prev) => prev.filter((i) => i.matchId !== matchId))
    try {
      await rejectApartmentMatch(matchId)
    } catch (err) {
      console.error('Error rejecting match', err)
      void fetchData()
    } finally {
      setUpdatingId(null)
    }
  }

  const handleAccept = async (matchId: number) => {
    setUpdatingId(matchId)
    setPendingItems((prev) => prev.filter((i) => i.matchId !== matchId))
    try {
      await acceptApartmentMatch(matchId)
      const accepted = pendingItems.find((i) => i.matchId === matchId)
      if (accepted) {
        setMatchItems((prev) => [...prev, { ...accepted, matchStatus: 'SUCCESSFUL' }])
      }
    } catch (err) {
      console.error('Error accepting match', err)
      void fetchData()
    } finally {
      setUpdatingId(null)
    }
  }

  const handleCardClick = async (item: EnrichedMatch, e: React.MouseEvent) => {
    // Don't open modal when clicking the cancel button
    if ((e.target as HTMLElement).closest('button')) return

    if (item.matchStatus === 'ACTIVE') {
      navigate(`/mis-solicitudes/recibidas/${item.matchId}`)
      return
    }

    if (
      item.matchStatus === 'MATCH' ||
      item.matchStatus === 'INVITED' ||
      item.matchStatus === 'SUCCESSFUL'
    ) {
      navigate(`/mis-solicitudes/recibidas/${item.matchId}/match`)
      return
    }

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

  const visibleItems = activeTab === 'pending' ? pendingItems : matchItems

  return (
    <div
      data-theme="light"
      className="mx-auto w-full max-w-2xl min-h-dvh text-[#050505] pb-28"
    >
      {/* ── Header ── */}
      <header className="sticky top-0 z-10 px-4 sm:px-8 pt-5 pb-4">
        <div className="flex items-center justify-between">

          <h1 className="text-2xl sm:text-3xl font-bold text-[#050505]">Mis Solicitudes</h1>
          <div className="h-10 w-10" aria-hidden /> {/* spacer */}
        </div>
      </header>

      {/* ── Tabs ── */}
      <section className="px-4 sm:px-8">
        <div className="flex rounded-xl bg-[#DDDBCB] p-1">
          <button
            className={`flex-1 rounded-lg py-2 text-base font-medium transition-colors ${activeTab === 'pending' ? 'bg-white text-[#050505] shadow-sm' : 'text-[#050505]/70'
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
            className={`flex-1 rounded-lg py-2 text-base font-medium transition-colors ${activeTab === 'match' ? 'bg-white text-[#050505] shadow-sm' : 'text-[#050505]/70'
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
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {visibleItems.map((item) => {
              const isCancelled = item.matchStatus === 'CANCELED' || item.matchStatus === 'REJECTED'
              const isMatch = item.matchStatus === 'MATCH' || item.matchStatus === 'INVITED'

              return (
                <article
                  key={item.matchId}
                  onClick={(e) => void handleCardClick(item, e)}
                  className={`overflow-hidden rounded-2xl border border-[#DDDBCB] bg-white shadow-sm transition-opacity cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-transform ${isCancelled ? 'opacity-60' : ''
                    }`}
                >
                  {/* Image */}
                  <div className={`relative h-44 w-full ${isCancelled ? 'grayscale' : ''}`}>
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
                  </div>

                  {/* Info */}
                  <div className="p-4">
                    <div className="flex items-start justify-between gap-2">
                      <h2 className="text-xl font-bold leading-tight text-[#050505] line-clamp-1">
                        {item.title}
                      </h2>
                      <span
                        className={`shrink-0 text-lg font-semibold ${isCancelled ? 'text-[#050505]/50' : 'text-[#008080]'
                          }`}
                      >
                        {item.price}
                      </span>
                    </div>

                    <p className="mt-0.5 text-sm text-[#050505]/70 line-clamp-1">{item.location}</p>
                    <p className="mt-1 text-sm text-[#008080] line-clamp-1">{item.tenantEmail}</p>

                    <div className="my-3 h-px w-full bg-[#DDDBCB]" />

                    <div className="flex items-center justify-between gap-2">
                      <span
                        className={`rounded-full px-3 py-1 text-xs font-semibold ${statusBadgeClass(item.matchStatus)}`}
                      >
                        {statusLabel(item.matchStatus)}
                      </span>
                      {item.matchStatus === 'ACTIVE' && (
                        <div className="flex items-center gap-2">
                          <button
                            className="rounded-xl border border-[#DDDBCB] bg-white px-3 py-2 text-xs font-semibold text-[#050505] transition-colors hover:bg-[#F5F1E3] disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={() => void handleReject(item.matchId)}
                            disabled={updatingId === item.matchId}
                          >
                            <span className="flex items-center justify-center gap-1.5">
                              {updatingId === item.matchId && (
                                <Loader2 size={12} className="animate-spin" />
                              )}
                              Rechazar
                            </span>
                          </button>
                          <button
                            className="rounded-xl bg-[#008080] px-3 py-2 text-xs font-semibold text-white transition-colors hover:bg-[#006d6d] disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={() => void handleAccept(item.matchId)}
                            disabled={updatingId === item.matchId}
                          >
                            <span className="flex items-center justify-center gap-1.5">
                              {updatingId === item.matchId && (
                                <Loader2 size={12} className="animate-spin" />
                              )}
                              Aceptar
                            </span>
                          </button>
                        </div>
                      )}
                      {(item.matchStatus === 'MATCH' || item.matchStatus === 'INVITED' || item.matchStatus === 'SUCCESSFUL') && (
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            className="relative h-8 w-8 rounded-full border border-[#DDDBCB] bg-white text-[#008080] flex items-center justify-center"
                            aria-label="Abrir chat"
                            onClick={(e) => {
                              e.stopPropagation()
                              navigate(`/chat/${item.matchId}`)
                            }}
                          >
                            <MessageCircle size={16} />
                            {unreadMatches.has(item.matchId) && (
                              <span className="absolute top-0 right-0 h-2.5 w-2.5 rounded-full bg-red-500 border border-white" />
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
