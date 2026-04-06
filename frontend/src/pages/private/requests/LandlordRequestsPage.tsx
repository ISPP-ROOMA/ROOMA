import { AnimatePresence } from 'framer-motion'
import { Loader2, Filter, ChevronDown } from 'lucide-react'
import { useCallback, useEffect, useState, useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import ApartmentDetailModal from '../../../components/ApartmentDetailModal'
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
import { getFilteredCandidates, type CandidateFilter } from '../../../service/requests.service'

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
  score?: number
}

function statusLabel(status: MatchStatus): string {
  switch (status) {
    case 'ACTIVE': return 'Pendiente'
    case 'MATCH': return '¡Match!'
    case 'INVITED': return 'Invitado'
    case 'SUCCESSFUL': return 'Aceptada'
    case 'REJECTED': return 'Rechazada'
    case 'CANCELED': return 'Cancelada'
    default: return status
  }
}

function statusBadgeClass(status: MatchStatus): string {
  switch (status) {
    case 'ACTIVE': return 'border border-[#050505] bg-white text-[#050505]'
    case 'MATCH':
    case 'INVITED':
    case 'SUCCESSFUL': return 'border-0 bg-[#008080] text-white'
    case 'REJECTED':
    case 'CANCELED': return 'border border-[#DDDBCB] bg-[#F5F1E3] text-[#050505]/60'
    default: return ''
  }
}

async function enrichMatches(matches: any[]): Promise<EnrichedMatch[]> {
  const enriched = await Promise.all(
    matches.map(async (match) => {
      const mId = match.id;
      const aId = match.apartmentId || (match.apartment ? match.apartment.id : null);
      
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
        imageUrl: apt?.coverImageUrl ?? 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80',
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

  // --- Estados de Ranking ---
  const [showFilters, setShowFilters] = useState(false)
  const [filters, setFilters] = useState<CandidateFilter>(location.state?.appliedFilters || {})
  const [selectedAptId, setSelectedAptId] = useState<number | null>(null)
  const [isRanking, setIsRanking] = useState(false)

  // --- Estados de Lista ---
  const [activeTab, setActiveTab] = useState<ActiveTab>('pending')
  const [pendingItems, setPendingItems] = useState<EnrichedMatch[]>([])
  const [matchItems, setMatchItems] = useState<EnrichedMatch[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [updatingId, setUpdatingId] = useState<number | null>(null)
  const [selectedApartment, setSelectedApartment] = useState<(ApartmentDTO & { imageUrl: string }) | null>(null)
  const [modalLoading, setModalLoading] = useState<number | null>(null)

  // Selector de viviendas únicas para el filtro
  const uniqueApartments = useMemo(() => {
    const all = [...pendingItems, ...matchItems]
    const map = new Map()
    all.forEach(item => {
      if (item.apartmentId) map.set(item.apartmentId, item.title)
    })
    return Array.from(map.entries())
  }, [pendingItems, matchItems])

  const fetchData = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    setError(null);
  
    try {
      const id = Number(userId);
    
      if (selectedAptId && (filters.requiredProfession || filters.allowedSmoker !== undefined)) {
        setIsRanking(true);
        const rankedData = await getFilteredCandidates(selectedAptId, filters);
        const enrichedRanked = await enrichMatches(rankedData);
        setPendingItems(enrichedRanked);
      
        const fullMatches = await getMatchesForLandlord(id, 'MATCH');
        setMatchItems(await enrichMatches(fullMatches));
      } else {
        setIsRanking(false); 
        const [activeMatches, successMatches, fullMatches] = await Promise.all([
        getMatchesForLandlord(id, 'ACTIVE'),
        getMatchesForLandlord(id, 'SUCCESSFUL'),
        getMatchesForLandlord(id, 'MATCH'),
        ]);
        setPendingItems(await enrichMatches(activeMatches));
        setMatchItems(await enrichMatches([...fullMatches, ...successMatches]));
      }
      } catch (err) {
        console.error('Error loading requests', err);
      setError('No se pudieron cargar las solicitudes.');
      } finally {
        setLoading(false);
      }
  }, [userId, selectedAptId, filters]);

  useEffect(() => {
    void fetchData()
  }, [fetchData, location.key])

  const handleFilterChange = (field: keyof CandidateFilter, value: any) => {
    setFilters(prev => ({ ...prev, [field]: value === '' ? undefined : value }))
  }

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
    
    const itemToMove = pendingItems.find((i) => i.matchId === matchId)
    if (!itemToMove) return

    setPendingItems((prev) => prev.filter((i) => i.matchId !== matchId))
    setMatchItems((prev) => [
      { ...itemToMove, matchStatus: 'SUCCESSFUL' as MatchStatus },
      ...prev 
    ])

    try {
      await acceptApartmentMatch(matchId)
    } catch (err) {
      console.error('Error accepting match', err)
      void fetchData()
    } finally {
      setUpdatingId(null)
    }
  }

  const handleCardClick = async (item: EnrichedMatch, e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest('button')) return
    if (item.matchStatus === 'ACTIVE') {
      navigate(`/mis-solicitudes/recibidas/${item.matchId}`)
      return
    }
    navigate(`/mis-solicitudes/recibidas/${item.matchId}/match`)
  }

  const visibleItems = activeTab === 'pending' ? pendingItems : matchItems

  return (
    <div data-theme="light" className="mx-auto w-full max-w-2xl min-h-dvh text-[#050505] pb-28">
      <header className="sticky top-0 z-20 px-4 sm:px-8 pt-5 pb-4 bg-[#F5F1E3]/90 backdrop-blur-md">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl sm:text-3xl font-bold text-[#050505]">Mis Solicitudes</h1>
          <button 
            onClick={() => setShowFilters(!showFilters)}
            className={`p-2 rounded-xl border transition-all ${showFilters ? 'bg-[#008080] text-white' : 'bg-white text-[#050505] border-[#DDDBCB]'}`}
          >
            <Filter size={20} />
          </button>
        </div>

        <AnimatePresence>
          {showFilters && (
            <div className="mt-4 p-4 bg-white border border-[#DDDBCB] rounded-2xl shadow-xl">
              <p className="text-xs font-bold text-[#008080] mb-3 uppercase tracking-tighter">
                Ranking de Inquilino Ideal
              </p>
              
              <div className="grid grid-cols-1 gap-4">
                {/* Filtro de Vivienda */}
                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-bold text-[#050505]/50 uppercase ml-1">Vivienda a evaluar</label>
                  <select 
                    className="w-full p-2.5 bg-[#F5F1E3] border border-[#DDDBCB] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#008080]/20"
                    onChange={(e) => setSelectedAptId(Number(e.target.value))}
                    value={selectedAptId || ''}
                  >
                    <option value="">Selecciona vivienda...</option>
                    {uniqueApartments.map(([id, title]) => (
                      <option key={id} value={id}>{title}</option>
                    ))}
                  </select>
                </div>
                
                <div className="grid grid-cols-2 gap-3">
                  {/* Filtro de Profesión */}
                  <div className="flex flex-col gap-1">
                    <label className="text-[10px] font-bold text-[#050505]/50 uppercase ml-1">Profesión requerida</label>
                    <input 
                      type="text" 
                      placeholder="Ej: Ingeniero..." 
                      className="p-2.5 bg-[#F5F1E3] border border-[#DDDBCB] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#008080]/20"
                      onChange={(e) => handleFilterChange('requiredProfession', e.target.value)}
                    />
                  </div>

                  {/* Filtro de Fumadores */}
                  <div className="flex flex-col gap-1">
                    <label className="text-[10px] font-bold text-[#050505]/50 uppercase ml-1">¿Permite fumadores?</label>
                    <select 
                      className="p-2.5 bg-[#F5F1E3] border border-[#DDDBCB] rounded-xl text-sm outline-none focus:ring-2 focus:ring-[#008080]/20"
                      onChange={(e) => handleFilterChange('allowedSmoker', e.target.value === 'true' ? true : e.target.value === 'false' ? false : undefined)}
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
            Pendientes {pendingItems.length > 0 && <span className="ml-1 px-2 py-0.5 rounded-full bg-[#008080] text-white text-[10px]">{pendingItems.length}</span>}
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
            <p className="text-[#050505]/70 font-medium">No hay solicitudes en esta sección.</p>
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
            
            {visibleItems.map((item, index) => (
              <article
                key={item.matchId}
                onClick={(e) => void handleCardClick(item, e)}
                className={`relative overflow-hidden rounded-2xl border border-[#DDDBCB] bg-white shadow-sm transition-all cursor-pointer hover:shadow-md ${index === 0 && isRanking ? 'ring-2 ring-[#008080] scale-[1.02]' : ''}`}
              >
                {index === 0 && isRanking && (
                  <div className="absolute top-3 left-3 z-10 bg-[#008080] text-white text-[10px] font-bold px-2 py-1 rounded-lg shadow-lg uppercase">
                    Mejor Match
                  </div>
                )}
                
                <div className="relative h-40 w-full">
                  <img src={item.imageUrl} alt={item.title} className="h-full w-full object-cover" />
                  <div className="absolute top-3 right-3 bg-black/40 backdrop-blur-sm px-2 py-1 rounded-lg text-white text-xs font-bold">
                    {item.price}
                  </div>
                </div>

                <div className="p-4">
                  <h2 className="text-lg font-bold text-[#050505] line-clamp-1">{item.title}</h2>
                  <p className="text-xs text-[#050505]/60 mb-2">{item.location}</p>
                  <p className="text-sm font-medium text-[#008080] truncate mb-4">{item.tenantEmail}</p>

                  <div className="flex items-center justify-between mt-auto">
                    <span className={`rounded-full px-3 py-1 text-[10px] font-bold uppercase tracking-tighter ${statusBadgeClass(item.matchStatus)}`}>
                      {statusLabel(item.matchStatus)}
                    </span>
                    
                    {item.matchStatus === 'ACTIVE' && (
                      <div className="flex gap-2">
                        <button 
                          onClick={() => handleReject(item.matchId)}
                          className="p-2 rounded-xl bg-red-50 text-red-600 hover:bg-red-100 transition-colors"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
                        </button>
                        <button 
                          onClick={() => handleAccept(item.matchId)}
                          className="p-2 rounded-xl bg-[#008080] text-white hover:bg-[#006d6d] transition-colors"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </article>
            ))}
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
    </div>
  )
}
