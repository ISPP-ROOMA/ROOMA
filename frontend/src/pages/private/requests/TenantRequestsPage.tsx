import type { IMessage, StompSubscription } from '@stomp/stompjs'
import axios from 'axios'
import { AnimatePresence } from 'framer-motion'
import Lottie from 'lottie-react'
import { Info, Loader2, MessageCircle, X } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ApartmentDetailModal from '../../../components/ApartmentDetailModal'
import Grainient from '../../../components/ui/Grainient'
import { useStompClient } from '../../../hooks/useStompClient'
import type {
  ApartmentDTO,
  ApartmentMatchDTO,
  MatchStatus,
} from '../../../service/apartment.service'
import {
  cancelApartmentMatch,
  getMatchesForCandidate,
  markTenantMatchDetailsAsViewed,
  respondToInvitation,
} from '../../../service/apartment.service'
import { useToast } from '../../../hooks/useToast'
import { getApartment } from '../../../service/apartments.service'
import {
  CHAT_TOPIC_SUBSCRIPTION,
  getMessageHistory,
  type ChatMessageDTO,
} from '../../../service/chat.service'
import { useAuthStore } from '../../../store/authStore'

type ActiveTab = 'pending' | 'match'

interface EnrichedMatch {
  matchId: number
  apartmentId: number
  matchStatus: MatchStatus
  tenantHasOpenedMatchDetails: boolean
  title: string
  location: string
  price: string
  imageUrl: string
}

async function enrichMatches(matches: ApartmentMatchDTO[]): Promise<EnrichedMatch[]> {
  const enriched = await Promise.all(
    matches.map(async (match) => {
      const apt = await getApartment(match.apartmentId)
      return {
        matchId: match.id,
        apartmentId: match.apartmentId,
        matchStatus: match.matchStatus,
        tenantHasOpenedMatchDetails: Boolean(match.tenantHasOpenedMatchDetails),
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
  const { showToast } = useToast()

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
  const [introMatchId, setIntroMatchId] = useState<number | null>(null)
  const [newMatchAnimationData, setNewMatchAnimationData] = useState<object | null>(null)
  const [unreadMatches, setUnreadMatches] = useState<Set<number>>(new Set())
  const [invitationModalItem, setInvitationModalItem] = useState<EnrichedMatch | null>(null)
  const { client, connected } = useStompClient()

  useEffect(() => {
    let isMounted = true

    const loadAnimation = async () => {
      try {
        const response = await fetch('/animations/new-match.json')
        if (!response.ok) return
        const data = (await response.json()) as object
        if (isMounted) {
          setNewMatchAnimationData(data)
        }
      } catch {
        // Fallback to text-only intro if JSON is missing or invalid.
      }
    }

    void loadAnimation()

    return () => {
      isMounted = false
    }
  }, [])

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
        } catch {
          // no pasa nada
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

  const handleCancel = async (matchId: number) => {
    setCancellingId(matchId)
    try {
      await cancelApartmentMatch(matchId)
      setPendingItems((prev) => prev.filter((i) => i.matchId !== matchId))
    } catch (err) {
      console.error('Error cancelling match', err)
      const message = axios.isAxiosError(err)
        ? ((err.response?.data as { message?: string } | undefined)?.message ?? 'No se pudo cancelar la solicitud')
        : 'No se pudo cancelar la solicitud'
      setError(message)
      void fetchData()
    } finally {
      setCancellingId(null)
    }
  }

  const openApartmentDetails = async (item: EnrichedMatch) => {
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

  const handleCardClick = async (item: EnrichedMatch, e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest('button')) return
    if (introMatchId !== null) return

    if (item.matchStatus === 'MATCH' && !item.tenantHasOpenedMatchDetails) {
      try {
        await markTenantMatchDetailsAsViewed(item.matchId)
      } catch (err) {
        console.error('Error marking match details as viewed', err)
      } finally {
        setMatchItems((prev) =>
          prev.map((entry) =>
            entry.matchId === item.matchId
              ? { ...entry, tenantHasOpenedMatchDetails: true }
              : entry
          )
        )
      }

      setIntroMatchId(item.matchId)
      await new Promise((resolve) => setTimeout(resolve, 1800))
      setIntroMatchId(null)
    }

    await openApartmentDetails(item)
  }

  const handleInvitationResponse = async (matchId: number, accepted: boolean) => {
    setInvitationActionId(matchId)
    try {
      await respondToInvitation(matchId, accepted)
      setMatchItems((prev) => prev.filter((i) => i.matchId !== matchId))
      setInvitationModalItem((prev) => (prev?.matchId === matchId ? null : prev))
    } catch (err) {
      console.error('Error responding to invitation', err)
      const message = axios.isAxiosError(err)
        ? ((err.response?.data as { message?: string } | undefined)?.message ??
          'No se pudo responder a la invitacion')
        : 'No se pudo responder a la invitacion'
      showToast(message, 'error')
      void fetchData()
    } finally {
      setInvitationActionId(null)
    }
  }

  const visibleItems = activeTab === 'pending' ? pendingItems : matchItems

  return (
    <div
      data-theme="light"
      className="mx-auto w-full max-w-5xl min-h-dvh text-[#050505] pb-28"
    >
      {/* ── Header ── */}
      <header className="sticky top-0 z-10  px-4 sm:px-8 pt-5 pb-4">
        <div className="flex items-center justify-between">
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
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
            {visibleItems.map((item) => {
              const isCancelled = item.matchStatus === 'CANCELED' || item.matchStatus === 'REJECTED'
              const isMatch = item.matchStatus === 'MATCH' || item.matchStatus === 'INVITED'
              const isNewUnopenedMatch =
                item.matchStatus === 'MATCH' && !item.tenantHasOpenedMatchDetails

              return (
                <article
                  key={item.matchId}
                  onClick={(e) => void handleCardClick(item, e)}
                  className={`overflow-hidden rounded-2xl border border-[#DDDBCB] bg-white shadow-sm transition-opacity cursor-pointer hover:shadow-md hover:-translate-y-0.5 transition-transform ${isCancelled ? 'opacity-60' : ''
                    }`}
                >
                  {isNewUnopenedMatch ? (
                    <div className="relative h-[260px] sm:h-[280px] w-full overflow-hidden">
                      <div className="absolute inset-0">
                        <Grainient variant="roomaMatch" />
                      </div>
                      <div className="absolute inset-0 bg-black/15" />
                      <div className="relative z-10 h-full w-full flex flex-col items-center justify-center px-6 text-center text-white">
                        <p className="text-2xl sm:text-3xl font-black tracking-tight drop-shadow-lg">
                          ¡Nuevo Match!
                        </p>
                        <p className="mt-2 text-sm sm:text-base font-medium text-white/90 drop-shadow">
                          Pulsa para descubrir los detalles
                        </p>
                      </div>
                    </div>
                  ) : (
                    <>
                      {/* Image */}
                      <div className={`relative h-40 sm:h-44 w-full ${isCancelled ? 'grayscale' : ''}`}>
                        <img
                          src={item.imageUrl}
                          alt={item.title}
                          className="h-full w-full object-cover"
                        />
                        {/* Cancel button (only on ACTIVE) */}
                        {item.matchStatus === 'ACTIVE' && (
                          <button
                            className="absolute top-3 right-3 h-8 w-8 rounded-full bg-white/90 text-[#050505]/70 flex items-center justify-center shadow hover:bg-white hover:text-red-500 transition-colors disabled:opacity-50"
                            aria-label="Cancelar solicitud"
                            disabled={cancellingId === item.matchId}
                            onClick={(e) => {
                              e.stopPropagation()
                              void handleCancel(item.matchId)
                            }}
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
                            className={`shrink-0 text-lg font-semibold ${isCancelled ? 'text-[#050505]/50' : 'text-[#008080]'
                              }`}
                          >
                            {item.price}
                          </span>
                        </div>

                        <p className="mt-0.5 text-sm text-[#050505]/70 line-clamp-1">{item.location}</p>

                        <div className="my-3 h-px w-full bg-[#DDDBCB]" />

                        <div className="flex items-center justify-end gap-2">
                          {item.matchStatus === 'MATCH' && (
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
                          {item.matchStatus === 'INVITED' && (
                            <div className="mr-auto flex items-center gap-2">
                              <button
                                type="button"
                                className="rounded-full border border-[#DDDBCB] bg-white p-2 text-[#008080] flex items-center justify-center hover:bg-[#F5F1E3] transition-colors"
                                aria-label="Ver invitacion"
                                onClick={(e) => {
                                  e.stopPropagation()
                                }}
                              >
                                <Info size={16} />
                              </button>
                              <button
                                type="button"
                                className="text-left text-xs font-medium text-[#008080] hover:underline"
                                onClick={(e) => {
                                  e.stopPropagation()
                                  setInvitationModalItem(item)
                                }}
                                aria-label="Abrir invitacion"
                              >
                                Tienes una invitacion para unirte al inmueble. Pulsa aquí
                              </button>
                            </div>
                          )}
                        </div>
                      </div>
                    </>
                  )}

                  {modalLoading === item.matchId && (
                    <div className="absolute inset-0 bg-black/30 flex items-center justify-center">
                      <Loader2 size={28} className="animate-spin text-white" />
                    </div>
                  )}
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

      {invitationModalItem && (
        <div
          className="fixed inset-0 z-[75] flex items-center justify-center bg-black/45 px-4"
          onClick={() => {
            if (invitationActionId !== invitationModalItem.matchId) {
              setInvitationModalItem(null)
            }
          }}
          role="presentation"
        >
          <div
            className="w-full max-w-md rounded-2xl border border-[#DDDBCB] bg-white p-5 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="invitation-dialog-title"
          >
            <div className="flex items-start justify-between gap-3">
              <h2 id="invitation-dialog-title" className="text-lg font-bold text-[#050505] leading-tight">
                Invitacion para unirte
              </h2>
              <button
                type="button"
                className="rounded-full p-1.5 text-[#050505]/65 hover:bg-[#F5F1E3] hover:text-[#050505] transition-colors disabled:opacity-40"
                onClick={() => setInvitationModalItem(null)}
                aria-label="Cerrar invitacion"
                disabled={invitationActionId === invitationModalItem.matchId}
              >
                <X size={16} />
              </button>
            </div>

            <p className="mt-3 text-sm text-[#050505]/80">
              Has recibido una invitacion para unirte al inmueble
              <span className="font-semibold text-[#050505]"> {invitationModalItem.title}</span>.
            </p>
            <p className="mt-1 text-xs text-[#050505]/60">{invitationModalItem.location}</p>

            <div className="mt-5 flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
              <button
                type="button"
                className="rounded-xl border border-[#DDDBCB] bg-white px-3 py-2 text-xs font-semibold text-[#050505] transition-colors hover:bg-[#F5F1E3] disabled:cursor-not-allowed disabled:opacity-60"
                onClick={() => {
                  void handleInvitationResponse(invitationModalItem.matchId, false)
                }}
                disabled={invitationActionId === invitationModalItem.matchId}
              >
                <span className="flex items-center justify-center gap-1.5">
                  {invitationActionId === invitationModalItem.matchId && (
                    <Loader2 size={12} className="animate-spin" />
                  )}
                  Rechazar
                </span>
              </button>
              <button
                type="button"
                className="rounded-xl border border-[#008080] bg-white px-3 py-2 text-xs font-semibold text-[#008080] transition-colors hover:bg-[#E8F7F7] disabled:cursor-not-allowed disabled:opacity-60"
                onClick={() => {
                  void handleInvitationResponse(invitationModalItem.matchId, true)
                }}
                disabled={invitationActionId === invitationModalItem.matchId}
              >
                <span className="flex items-center justify-center gap-1.5">
                  {invitationActionId === invitationModalItem.matchId && (
                    <Loader2 size={12} className="animate-spin" />
                  )}
                  Aceptar
                </span>
              </button>
            </div>
          </div>
        </div>
      )}

      {introMatchId !== null && (
        <div className="fixed inset-0 z-[70] pointer-events-none">
          <div className="absolute inset-0">
            <Grainient variant="roomaMatch" />
          </div>
          <div className="absolute inset-0 bg-black/15" />
          <div className="relative z-10 flex h-full w-full flex-col items-center justify-center px-6 text-center">
            <div className="w-[240px] max-w-[75vw] sm:w-[320px]">
              {newMatchAnimationData ? (
                <Lottie animationData={newMatchAnimationData} loop={false} autoplay={true} />
              ) : (
                <Loader2 size={56} className="mx-auto animate-spin text-white" />
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
