import { AnimatePresence } from 'framer-motion'
import { Loader2, MessageCircle, Filter } from 'lucide-react'
import type { IMessage, StompSubscription } from '@stomp/stompjs'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import ApartmentDetailModal from '../../../components/ApartmentDetailModal'
import CreateAppointmentModal from '../../../components/CreateAppointmentModal'
import ViewAppointmentsModal from '../../../components/ViewAppointmentsModal'
import { useStompClient } from '../../../hooks/useStompClient'
import type {
  ApartmentDTO,
  ApartmentMatchDTO,
  MatchStatus,
} from '../../../service/apartment.service'
import {
  acceptApartmentMatch,
  cancelApartmentMatch,
  getLandlordMatchDetails,
  getMatchesForLandlord,
  rejectApartmentMatch,
  waitApartmentMatch,
} from '../../../service/apartment.service'
import { getApartment } from '../../../service/apartments.service'
import {
  CHAT_TOPIC_SUBSCRIPTION,
  getMessageHistory,
  type ChatMessageDTO,
} from '../../../service/chat.service'
import { getFilteredCandidates, type CandidateFilter } from '../../../service/requests.service'
import { useAuthStore } from '../../../store/authStore'

type ActiveTab = 'pending' | 'waiting' | 'match'

interface EnrichedMatch {
  matchId: number
  apartmentId: number
  matchStatus: MatchStatus
  title: string
  location: string
  price: string
  imageUrl: string
  tenantEmail: string
  score?: number
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
    default:
      return status
  }
}

function statusBadgeClass(status: MatchStatus): string {
  switch (status) {
    case 'ACTIVE':
      return 'border border-[#050505] bg-white text-[#050505]'
    case 'WAITING':
      return 'border border-[#e8a000] bg-[#fff7df] text-[#7a5a00]'
    case 'MATCH':
    case 'INVITED':
    case 'SUCCESSFUL':
      return 'border-0 bg-[#008080] text-white'
    case 'REJECTED':
    case 'CANCELED':
      return 'border border-[#DDDBCB] bg-[#F5F1E3] text-[#050505]/60'
    default:
      return ''
  }
}

async function enrichMatches(matches: ApartmentMatchDTO[]): Promise<EnrichedMatch[]> {
  const enriched = await Promise.all(
    matches.map(async (match) => {
      const mId = match.id
      const aId = match.apartmentId ?? null

      const [apt, details] = await Promise.all([
        aId ? getApartment(aId) : Promise.resolve(null),
        getLandlordMatchDetails(mId),
      ])

      const detailsApt = details?.apartment
      return {
        matchId: mId,
        apartmentId: aId,
        matchStatus: match.matchStatus || 'ACTIVE',
        title: detailsApt?.title ?? apt?.title ?? `Vivienda #${aId}`,
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
  const [searchParams] = useSearchParams()

  const apartmentFilterId = useMemo(() => {
    const rawApartmentId = searchParams.get('apartmentId')
    if (!rawApartmentId) return null

    const parsedApartmentId = Number(rawApartmentId)
    return Number.isNaN(parsedApartmentId) ? null : parsedApartmentId
  }, [searchParams])

  const [showFilters, setShowFilters] = useState(false)
  const [filters, setFilters] = useState<CandidateFilter>(location.state?.appliedFilters || {})
  const [selectedAptId, setSelectedAptId] = useState<number | null>(null)
  const [isRanking, setIsRanking] = useState(false)
  const initialTab = useMemo<ActiveTab>(() => {
    const t = searchParams.get('tab')
    return t === 'match' ? 'match' : t === 'waiting' ? 'waiting' : 'pending'
  }, [searchParams])
  const [activeTab, setActiveTab] = useState<ActiveTab>(initialTab)
  const [pendingItems, setPendingItems] = useState<EnrichedMatch[]>([])
  const [waitingItems, setWaitingItems] = useState<EnrichedMatch[]>([])
  const [matchItems, setMatchItems] = useState<EnrichedMatch[]>([])
  const [loading, setLoading] = useState(true)
  const [_error, _setError] = useState<string | null>(null)
  const [updatingId, setUpdatingId] = useState<number | null>(null)

  const [filteredApartmentLabel, setFilteredApartmentLabel] = useState<string | null>(null)
  const [selectedApartment, setSelectedApartment] = useState<
    (ApartmentDTO & { imageUrl: string }) | null
  >(null)
  const [showAppointmentModal, setShowAppointmentModal] = useState(false)
  const [showViewAppointmentsModal, setShowViewAppointmentsModal] = useState(false)
  const [showAptSelectModal, setShowAptSelectModal] = useState<'create' | 'view' | null>(null)
  const [modalAptId, setModalAptId] = useState<number | null>(null)
  const [modalAptTitle, setModalAptTitle] = useState<string>('')
  const [modalLoading] = useState<number | null>(null)
  const [unreadMatches, setUnreadMatches] = useState<Set<number>>(new Set())
  const { client, connected } = useStompClient()

  useEffect(() => {
    setActiveTab(initialTab)
  }, [initialTab])

  const uniqueApartments = useMemo(() => {
    const all = [...pendingItems, ...waitingItems, ...matchItems]
    const map = new Map()
    all.forEach((item) => {
      if (item.apartmentId) map.set(item.apartmentId, item.title)
    })
    return Array.from(map.entries())
  }, [pendingItems, waitingItems, matchItems])

  const fetchData = useCallback(async () => {
    if (!userId) return
    setLoading(true)

    try {
      const id = Number(userId)

      const filteredApartment = apartmentFilterId
        ? await getApartment(apartmentFilterId)
        : undefined
      setFilteredApartmentLabel(
        apartmentFilterId ? (filteredApartment?.title ?? `Vivienda #${apartmentFilterId}`) : null
      )

      const matchesForApartment = (matches: ApartmentMatchDTO[]) =>
        apartmentFilterId
          ? matches.filter((match) => match.apartmentId === apartmentFilterId)
          : matches

      if (selectedAptId && (filters.requiredProfession || filters.allowedSmoker !== undefined)) {
        setIsRanking(true)
        const rankedData = await getFilteredCandidates(selectedAptId, filters)
        const enrichedRanked = await enrichMatches(rankedData)
        setPendingItems(enrichedRanked)

        const fullMatches = await getMatchesForLandlord(id, 'MATCH')
        setMatchItems(await enrichMatches(matchesForApartment(fullMatches)))
      } else {
        setIsRanking(false)
        const [activeMatches, waitingMatches, successMatches, fullMatches] = await Promise.all([
          getMatchesForLandlord(id, 'ACTIVE'),
          getMatchesForLandlord(id, 'WAITING'),
          getMatchesForLandlord(id, 'SUCCESSFUL'),
          getMatchesForLandlord(id, 'MATCH'),
        ])

        const [enrichedActive, enrichedWaiting, enrichedSuccess, enrichedFull] = await Promise.all([
          enrichMatches(matchesForApartment(activeMatches)),
          enrichMatches(matchesForApartment(waitingMatches)),
          enrichMatches(matchesForApartment(successMatches)),
          enrichMatches(matchesForApartment(fullMatches)),
        ])

        setPendingItems(enrichedActive)
        setWaitingItems(enrichedWaiting)
        setMatchItems([...enrichedFull, ...enrichedSuccess])
      }
    } catch (err) {
      console.error('Error loading requests', err)
    } finally {
      setLoading(false)
    }
  }, [userId, apartmentFilterId, selectedAptId, filters])

  useEffect(() => {
    void fetchData()
  }, [fetchData, location.search])

  useEffect(() => {
    const chatableMatches = matchItems.filter(
      (item) =>
        item.matchStatus === 'MATCH' ||
        item.matchStatus === 'INVITED' ||
        item.matchStatus === 'SUCCESSFUL'
    )

    if (chatableMatches.length === 0 || !userId) return

    let isMounted = true
    const subscriptions: StompSubscription[] = []

    const initializeUnread = async () => {
      const newUnread = new Set<number>()
      for (const item of chatableMatches) {
        try {
          const history = await getMessageHistory({ type: 'match', id: item.matchId })
          const hasUnread = history.some(
            (m) => m.senderId !== Number(userId) && m.status !== 'READ'
          )
          if (hasUnread) {
            newUnread.add(item.matchId)
          }
        } catch {
          // Ignore error
        }
      }
      if (isMounted) {
        setUnreadMatches((prev) => {
          const merged = new Set(prev)
          newUnread.forEach((id) => merged.add(id))
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
              setUnreadMatches((prev) => new Set(prev).add(item.matchId))
            }
          }
        )
        subscriptions.push(sub)
      }
    }

    return () => {
      isMounted = false
      subscriptions.forEach((sub) => sub.unsubscribe())
    }
  }, [matchItems, userId, connected, client])

  const handleFilterChange = (
    field: keyof CandidateFilter,
    value: CandidateFilter[keyof CandidateFilter] | ''
  ) => {
    setFilters((prev) => ({ ...prev, [field]: value === '' ? undefined : value }))
  }

  const handleReject = async (matchId: number) => {
    setUpdatingId(matchId)
    setPendingItems((prev) => prev.filter((i) => i.matchId !== matchId))
    setWaitingItems((prev) => prev.filter((i) => i.matchId !== matchId))

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

    const itemToMove =
      pendingItems.find((i) => i.matchId === matchId) ||
      waitingItems.find((i) => i.matchId === matchId)
    if (!itemToMove) return

    setPendingItems((prev) => prev.filter((i) => i.matchId !== matchId))
    setWaitingItems((prev) => prev.filter((i) => i.matchId !== matchId))
    setMatchItems((prev) => [{ ...itemToMove, matchStatus: 'MATCH' as MatchStatus }, ...prev])

    try {
      await acceptApartmentMatch(matchId)
    } catch (err) {
      console.error('Error accepting match', err)
      void fetchData()
    } finally {
      setUpdatingId(null)
    }
  }

  const handleWait = async (matchId: number) => {
    setUpdatingId(matchId)
    const item = pendingItems.find((i) => i.matchId === matchId)
    if (item) {
      setPendingItems((prev) => prev.filter((i) => i.matchId !== matchId))
      setWaitingItems((prev) => [{ ...item, matchStatus: 'WAITING' as MatchStatus }, ...prev])
    }

    try {
      await waitApartmentMatch(matchId)
    } catch (err) {
      console.error('Error setting match to waiting', err)
      void fetchData()
    } finally {
      setUpdatingId(null)
    }
  }

  const handleCancel = async (matchId: number) => {
    setUpdatingId(matchId)
    setMatchItems((prev) => prev.filter((item) => item.matchId !== matchId))

    try {
      await cancelApartmentMatch(matchId)
    } catch (err) {
      console.error('Error cancelling match', err)
      void fetchData()
    } finally {
      setUpdatingId(null)
    }
  }

  const handleCardClick = async (item: EnrichedMatch, e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest('button')) return
    if (item.matchStatus === 'ACTIVE' || item.matchStatus === 'WAITING') {
      navigate(`/mis-solicitudes/recibidas/${item.matchId}`)
      return
    }
    navigate(`/mis-solicitudes/recibidas/${item.matchId}/match`)
  }

  const visibleItems =
    activeTab === 'pending' ? pendingItems : activeTab === 'waiting' ? waitingItems : matchItems
  const hasApartmentFilter = apartmentFilterId !== null

  return (
    <div data-theme="light" className="mx-auto w-full max-w-2xl min-h-dvh text-[#050505] pb-28">
      <header className="sticky top-0 z-20 px-4 sm:px-8 pt-5 pb-4 bg-[#F5F1E3]/90 backdrop-blur-md">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl sm:text-3xl font-bold text-[#050505]">Mis Solicitudes</h1>
          <div className="flex items-center gap-2">
            <button
              onClick={() => {
                if (apartmentFilterId && filteredApartmentLabel) {
                  setModalAptId(apartmentFilterId)
                  setModalAptTitle(filteredApartmentLabel)
                  setShowViewAppointmentsModal(true)
                } else {
                  setShowAptSelectModal('view')
                }
              }}
              className="bg-white border border-[#DDDBCB] text-[#008080] px-3 py-2 rounded-xl text-xs sm:text-sm font-semibold hover:bg-[#F5F1E3] transition-colors shadow-sm"
            >
              Ver Visitas
            </button>
            <button
              onClick={() => {
                if (apartmentFilterId && filteredApartmentLabel) {
                  setModalAptId(apartmentFilterId)
                  setModalAptTitle(filteredApartmentLabel)
                  setShowAppointmentModal(true)
                } else {
                  setShowAptSelectModal('create')
                }
              }}
              className="bg-[#008080] text-white px-3 py-2 rounded-xl text-xs sm:text-sm font-semibold hover:bg-[#006d6d] transition-colors shadow-sm"
            >
              Organizar Visitas
            </button>
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`p-2 rounded-xl border transition-all ${showFilters ? 'bg-[#008080] text-white' : 'bg-white text-[#050505] border-[#DDDBCB]'}`}
            >
              <Filter size={20} />
            </button>
          </div>
        </div>

        {hasApartmentFilter && (
          <div className="mt-3 flex flex-wrap items-center gap-2 rounded-2xl border border-[#DDDBCB] bg-white px-4 py-3 text-sm text-[#050505]/80 shadow-sm">
            <span className="font-medium text-[#050505]">Filtrado por inmueble</span>
            <span className="rounded-full bg-[#F5F1E3] px-3 py-1 font-medium">
              {filteredApartmentLabel ?? `Vivienda #${apartmentFilterId}`}
            </span>
            <button
              type="button"
              className="ml-auto rounded-full bg-[#008080] px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-[#006d6d]"
              onClick={() => navigate('/mis-solicitudes/recibidas')}
            >
              Ver todas
            </button>
          </div>
        )}

        <AnimatePresence>
          {showFilters && (
            <div className="mt-4 p-4 bg-white border border-[#DDDBCB] rounded-2xl shadow-xl">
              <p className="text-xs font-bold text-[#008080] mb-3 uppercase tracking-tighter">
                Ranking de Inquilino Ideal
              </p>

              <div className="grid grid-cols-1 gap-4">
                {/* Filtro de Vivienda */}
                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-bold text-[#050505]/50 uppercase ml-1">
                    Vivienda a evaluar
                  </label>
                  <select
                    className="w-full p-2.5 bg-[#F5F1E3] border border-[#DDDBCB] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#008080]/20"
                    onChange={(e) => setSelectedAptId(Number(e.target.value))}
                    value={selectedAptId || ''}
                  >
                    <option value="">Selecciona vivienda...</option>
                    {uniqueApartments.map(([id, title]) => (
                      <option key={id} value={id}>
                        {title}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  {/* Filtro de Profesión */}
                  <div className="flex flex-col gap-1">
                    <label className="text-[10px] font-bold text-[#050505]/50 uppercase ml-1">
                      Profesión requerida
                    </label>
                    <input
                      type="text"
                      placeholder="Ej: Ingeniero..."
                      className="p-2.5 bg-[#F5F1E3] border border-[#DDDBCB] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#008080]/20"
                      onChange={(e) => handleFilterChange('requiredProfession', e.target.value)}
                    />
                  </div>

                  {/* Filtro de Fumadores */}
                  <div className="flex flex-col gap-1">
                    <label className="text-[10px] font-bold text-[#050505]/50 uppercase ml-1">
                      ¿Permite fumadores?
                    </label>
                    <select
                      className="p-2.5 bg-[#F5F1E3] border border-[#DDDBCB] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#008080]/20"
                      onChange={(e) =>
                        handleFilterChange(
                          'allowedSmoker',
                          e.target.value === 'true'
                            ? true
                            : e.target.value === 'false'
                              ? false
                              : undefined
                        )
                      }
                    >
                      <option value="">Cualquiera</option>
                      <option value="false">No</option>
                      <option value="true">Sí</option>
                    </select>
                  </div>
                </div>

                <button
                  onClick={() => void fetchData()}
                  disabled={!selectedAptId || loading}
                  className="w-full py-3 mt-2 bg-[#008080] text-white rounded-xl text-sm font-bold shadow-md hover:bg-[#006d6d] transition-all disabled:opacity-50"
                >
                  {isRanking ? 'Calculando mejores perfiles...' : 'Ver ranking de candidatos'}
                </button>
              </div>
            </div>
          )}
        </AnimatePresence>
      </header>

      <section className="px-4 sm:px-8 mt-2">
        <div className="flex rounded-xl bg-[#DDDBCB] p-1">
          <button
            className={`flex-1 rounded-lg py-2 text-base font-medium transition-all ${activeTab === 'pending' ? 'bg-white text-[#050505] shadow-sm' : 'text-[#050505]/70'}`}
            onClick={() => setActiveTab('pending')}
          >
            Pendientes{' '}
            {pendingItems.length > 0 && (
              <span className="ml-1 px-2 py-0.5 rounded-full bg-[#008080] text-white text-[10px]">
                {pendingItems.length}
              </span>
            )}
          </button>

          <button
            className={`flex-1 rounded-lg py-2 text-base font-medium transition-all ${activeTab === 'waiting' ? 'bg-white text-[#050505] shadow-sm' : 'text-[#050505]/70'}`}
            onClick={() => setActiveTab('waiting')}
          >
            En espera
            {waitingItems.length > 0 && (
              <span className="ml-2 inline-flex h-5 w-5 items-center justify-center rounded-full bg-[#e8a000] text-[#7a5a00] text-xs font-bold">
                {waitingItems.length}
              </span>
            )}
          </button>

          <button
            className={`flex-1 rounded-lg py-2 text-base font-medium transition-all ${activeTab === 'match' ? 'bg-white text-[#050505] shadow-sm' : 'text-[#050505]/70'}`}
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

      <section className="mt-6 px-4 sm:px-8">
        {loading && !isRanking ? (
          <div className="flex flex-col items-center gap-4 pt-16 text-[#008080]">
            <Loader2 className="animate-spin" size={40} />
            <p className="font-medium animate-pulse">Actualizando solicitudes...</p>
          </div>
        ) : visibleItems.length === 0 ? (
          <div className="rounded-2xl border border-[#DDDBCB] bg-white p-10 text-center">
            <div className="text-5xl mb-4">
              {activeTab === 'pending' ? '📋' : activeTab === 'waiting' ? '⏳' : '🤝'}
            </div>
            <p className="text-[#050505]/70 font-medium">
              {hasApartmentFilter
                ? activeTab === 'pending'
                  ? 'Este inmueble no tiene solicitudes activas.'
                  : activeTab === 'waiting'
                    ? 'Este inmueble no tiene candidatos en espera.'
                    : 'Este inmueble no tiene matches.'
                : activeTab === 'pending'
                  ? 'Aún no tienes solicitudes activas. ¡Desliza pisos en el inicio!'
                  : activeTab === 'waiting'
                    ? 'No hay candidatos en espera.'
                    : 'Todavía no tienes matches. ¡Sigue explorando!'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {activeTab === 'pending' && isRanking && (
              <div className="col-span-full mb-2">
                <span className="text-[10px] font-bold text-[#008080] bg-[#008080]/10 px-3 py-1.5 rounded-full uppercase tracking-wider">
                  Ordenado por coincidencia con tu inquilino ideal
                </span>
              </div>
            )}

            {visibleItems.map((item, index) => {
              const isCancelled = item.matchStatus === 'CANCELED' || item.matchStatus === 'REJECTED'
              const isTopRanked = index === 0 && isRanking && activeTab === 'pending'

              return (
                <article
                  key={item.matchId}
                  onClick={(e) => void handleCardClick(item, e)}
                  className={`relative overflow-hidden rounded-2xl border border-[#DDDBCB] bg-white shadow-sm transition-all cursor-pointer hover:shadow-md hover:-translate-y-0.5 ${
                    isCancelled ? 'opacity-60' : ''
                  } ${isTopRanked ? 'ring-2 ring-[#008080] scale-[1.02]' : ''}`}
                >
                  {isTopRanked && (
                    <div className="absolute top-3 left-3 z-20 bg-[#008080] text-white text-[10px] font-bold px-2 py-1 rounded-lg shadow-lg uppercase">
                      Mejor Match
                    </div>
                  )}

                  {/* Image */}
                  <div className={`relative h-44 w-full ${isCancelled ? 'grayscale' : ''}`}>
                    <img
                      src={item.imageUrl}
                      alt={item.title}
                      className="h-full w-full object-cover"
                    />
                    <div className="absolute top-3 right-3 z-10 bg-black/40 backdrop-blur-sm px-2 py-1 rounded-lg text-white text-xs font-bold">
                      {item.price}
                    </div>
                    {/* Modal loading spinner on card */}
                    {modalLoading === item.matchId && (
                      <div className="absolute inset-0 z-10 bg-black/30 flex items-center justify-center">
                        <Loader2 size={28} className="animate-spin text-white" />
                      </div>
                    )}
                  </div>

                  <div className="p-4 flex flex-col h-[calc(100%-11rem)]">
                    <h2 className="text-lg font-bold text-[#050505] line-clamp-1">{item.title}</h2>
                    <p className="text-xs text-[#050505]/60 mb-2">{item.location}</p>
                    <p className="text-sm font-medium text-[#008080] truncate mb-4">
                      {item.tenantEmail}
                    </p>

                    <div className="mt-auto">
                      <div className="flex items-center justify-between gap-2">
                        <span
                          className={`rounded-full px-3 py-1 text-[10px] font-bold uppercase tracking-tighter ${statusBadgeClass(item.matchStatus)}`}
                        >
                          {statusLabel(item.matchStatus)}
                        </span>

                        <div className="flex items-center gap-2">
                          {(item.matchStatus === 'MATCH' ||
                            item.matchStatus === 'INVITED' ||
                            item.matchStatus === 'SUCCESSFUL') && (
                            <button
                              type="button"
                              className="relative h-8 w-8 rounded-full border border-[#DDDBCB] bg-white text-[#008080] flex items-center justify-center transition-colors hover:bg-[#F5F1E3]"
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
                          )}

                          {item.matchStatus === 'MATCH' && (
                            <button
                              type="button"
                              className="min-w-[104px] rounded-xl border border-[#DDDBCB] bg-white px-4 py-2 text-xs font-semibold text-[#b42318] transition-colors hover:bg-[#F5F1E3] disabled:cursor-not-allowed disabled:opacity-60"
                              onClick={(e) => {
                                e.stopPropagation()
                                void handleCancel(item.matchId)
                              }}
                              disabled={updatingId === item.matchId}
                            >
                              {updatingId === item.matchId ? 'Cancelando...' : 'Cancelar'}
                            </button>
                          )}
                        </div>
                      </div>

                      {item.matchStatus === 'ACTIVE' && (
                        <div className="mt-2 grid grid-cols-3 gap-2">
                          <button
                            className="w-full rounded-xl border border-[#DDDBCB] bg-white px-2 py-2 text-[11px] font-semibold text-[#050505] transition-colors hover:bg-[#F5F1E3] disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={(e) => {
                              e.stopPropagation()
                              void handleReject(item.matchId)
                            }}
                            disabled={updatingId === item.matchId}
                          >
                            <span className="flex items-center justify-center gap-1">
                              {updatingId === item.matchId && (
                                <Loader2 size={11} className="animate-spin" />
                              )}
                              Rechazar
                            </span>
                          </button>

                          <button
                            className="w-full rounded-xl border border-[#e8a000] bg-[#fff7df] px-2 py-2 text-[11px] font-semibold text-[#7a5a00] transition-colors hover:bg-[#ffefbf] disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={(e) => {
                              e.stopPropagation()
                              void handleWait(item.matchId)
                            }}
                            disabled={updatingId === item.matchId}
                          >
                            <span className="flex items-center justify-center gap-1">
                              {updatingId === item.matchId && (
                                <Loader2 size={11} className="animate-spin" />
                              )}
                              En espera
                            </span>
                          </button>

                          <button
                            className="w-full rounded-xl bg-[#008080] px-2 py-2 text-[11px] font-semibold text-white transition-colors hover:bg-[#006d6d] disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={(e) => {
                              e.stopPropagation()
                              void handleAccept(item.matchId)
                            }}
                            disabled={updatingId === item.matchId}
                          >
                            <span className="flex items-center justify-center gap-1">
                              {updatingId === item.matchId && (
                                <Loader2 size={11} className="animate-spin" />
                              )}
                              Aceptar
                            </span>
                          </button>
                        </div>
                      )}

                      {item.matchStatus === 'WAITING' && (
                        <div className="mt-2 grid grid-cols-2 gap-2">
                          <button
                            className="w-full rounded-xl border border-[#DDDBCB] bg-white px-2 py-2 text-[11px] font-semibold text-[#050505] transition-colors hover:bg-[#F5F1E3] disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={(e) => {
                              e.stopPropagation()
                              void handleReject(item.matchId)
                            }}
                            disabled={updatingId === item.matchId}
                          >
                            <span className="flex items-center justify-center gap-1">
                              {updatingId === item.matchId && (
                                <Loader2 size={11} className="animate-spin" />
                              )}
                              Rechazar
                            </span>
                          </button>

                          <button
                            className="w-full rounded-xl bg-[#008080] px-2 py-2 text-[11px] font-semibold text-white transition-colors hover:bg-[#006d6d] disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={(e) => {
                              e.stopPropagation()
                              void handleAccept(item.matchId)
                            }}
                            disabled={updatingId === item.matchId}
                          >
                            <span className="flex items-center justify-center gap-1">
                              {updatingId === item.matchId && (
                                <Loader2 size={11} className="animate-spin" />
                              )}
                              Aceptar
                            </span>
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

      <AnimatePresence>
        {selectedApartment && (
          <ApartmentDetailModal
            apartment={selectedApartment}
            onClose={() => setSelectedApartment(null)}
          />
        )}
      </AnimatePresence>

      {showAptSelectModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm bg-white rounded-3xl shadow-xl overflow-hidden p-5 animate-in fade-in zoom-in-95 duration-200">
            <h2 className="text-[#050505] text-lg font-bold mb-1">Selecciona un inmueble</h2>
            <p className="text-xs text-[#050505]/50 mb-4">
              {showAptSelectModal === 'view'
                ? 'Para ver sus visitas programadas'
                : 'Para crear nuevas franjas horarias'}
            </p>
            <div className="flex flex-col gap-3">
              {uniqueApartments.length === 0 ? (
                <p className="text-sm text-[#050505]/60">
                  Aún no tienes inmuebles con historial de solicitudes.
                </p>
              ) : (
                uniqueApartments.map(([id, title]) => (
                  <button
                    key={id as number}
                    onClick={() => {
                      const action = showAptSelectModal
                      setShowAptSelectModal(null)
                      setModalAptId(id as number)
                      setModalAptTitle(title as string)
                      if (action === 'view') setShowViewAppointmentsModal(true)
                      else setShowAppointmentModal(true)
                    }}
                    className="w-full text-left p-3 rounded-xl border border-[#DDDBCB] hover:border-[#008080] hover:bg-[#F5F1E3] transition-colors font-medium text-[#050505]"
                  >
                    {title as string}
                  </button>
                ))
              )}
            </div>
            <button
              onClick={() => setShowAptSelectModal(null)}
              className="mt-5 w-full py-2 bg-[#F5F1E3] text-[#050505] font-semibold rounded-xl hover:bg-[#DDDBCB] transition-colors"
            >
              Cancelar
            </button>
          </div>
        </div>
      )}

      {showAppointmentModal && modalAptId && (
        <CreateAppointmentModal
          apartmentId={modalAptId}
          apartmentTitle={modalAptTitle}
          onClose={() => setShowAppointmentModal(false)}
          onSuccess={() => setShowAppointmentModal(false)}
        />
      )}
      {showViewAppointmentsModal && modalAptId && (
        <ViewAppointmentsModal
          apartmentId={modalAptId}
          apartmentTitle={modalAptTitle}
          onClose={() => setShowViewAppointmentsModal(false)}
        />
      )}
    </div>
  )
}
