import { AnimatePresence, motion } from 'framer-motion'
import { Building2, Loader2, PlusCircle, ShieldCheck, Users2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ApartmentDetailModal from '../components/ApartmentDetailModal'
import SwipeableCard from '../components/SwipeableCard'
import { useToast } from '../hooks/useToast'
import type { ApartmentDTO } from '../service/apartment.service'
import { getDeckForCandidate, searchApartments, swipeApartment } from '../service/apartment.service'
import { useAuthStore } from '../store/authStore'

interface ApartmentFilters {
  ubication?: string
  minPrice?: number
  maxPrice?: number
}

interface PendingSwipe {
  apartmentId: number
  interest: boolean
  timestamp: number
}

const getFromStorage = <T,>(key: string): T | null => {
  try {
    const item = localStorage.getItem(key)
    return item ? JSON.parse(item) : null
  } catch (error) {
    console.error(`Error parsing localStorage key "${key}":`, error)
    return null
  }
}

const saveToStorage = <T,>(key: string, value: T): void => {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
    console.error(`Error saving to localStorage key "${key}":`, error)
  }
}

const getCachedApartments = (): ApartmentDTO[] => {
  return getFromStorage<ApartmentDTO[]>('cached_apartments') ?? []
}

const setCachedApartments = (apartments: ApartmentDTO[]): void => {
  saveToStorage('cached_apartments', apartments)
}

const getPendingSwipes = (): PendingSwipe[] => {
  return getFromStorage<PendingSwipe[]>('pending_swipes') ?? []
}

const setPendingSwipes = (swipes: PendingSwipe[]): void => {
  saveToStorage('pending_swipes', swipes)
}

const addPendingSwipe = (apartmentId: number, interest: boolean): void => {
  const pending = getPendingSwipes()
  const newSwipe: PendingSwipe = { apartmentId, interest, timestamp: Date.now() }
  pending.push(newSwipe)
  setPendingSwipes(pending)
}

const removePendingSwipe = (apartmentId: number): void => {
  const pending = getPendingSwipes()
  setPendingSwipes(pending.filter((s) => s.apartmentId !== apartmentId))
}

const filterOutPendingApartments = (apartments: ApartmentDTO[]): ApartmentDTO[] => {
  const pendingIds = new Set(getPendingSwipes().map((s) => s.apartmentId))
  return apartments.filter((apt) => !pendingIds.has(apt.id))
}

function HeroWrapper({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen w-full px-6 py-16 relative overflow-hidden">
      <motion.div
        initial={{ opacity: 0, y: 32 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: 'easeOut' }}
        className="relative z-10 w-full max-w-2xl"
      >
        {children}
      </motion.div>
    </div>
  )
}

export default function Home() {
  const { token, role, userId } = useAuthStore()
  const { showToast } = useToast()
  const [apartments, setApartments] = useState<ApartmentDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedApartment, setSelectedApartment] = useState<ApartmentDTO | null>(null)
  const [filterInputs, setFilterInputs] = useState({
    ubication: '',
    minPrice: '',
    maxPrice: '',
  })
  const [appliedFilters, setAppliedFilters] = useState<ApartmentFilters>({})
  const [noFilteredResults, setNoFilteredResults] = useState(false)
  const [suggestedFilters, setSuggestedFilters] = useState<ApartmentFilters | null>(null)

  const buildSearchFilters = (inputs: typeof filterInputs): ApartmentFilters => {
    const query: ApartmentFilters = {}
    if (inputs.ubication.trim()) query.ubication = inputs.ubication.trim()
    if (inputs.minPrice !== '') query.minPrice = Number(inputs.minPrice)
    if (inputs.maxPrice !== '') query.maxPrice = Number(inputs.maxPrice)
    return query
  }

  const computeRelaxedFilters = (filters: ApartmentFilters): ApartmentFilters | null => {
    const hasFilter = !!(
      filters.ubication ||
      filters.minPrice !== undefined ||
      filters.maxPrice !== undefined
    )
    if (!hasFilter) return null

    const relaxed: ApartmentFilters = {}

    if (filters.ubication) {
      relaxed.ubication = undefined
    }

    if (filters.minPrice !== undefined) {
      relaxed.minPrice = Math.max(0, filters.minPrice - 200)
    }

    if (filters.maxPrice !== undefined) {
      relaxed.maxPrice = filters.maxPrice + 200
    }

    return relaxed
  }

  const getSuggestionDescription = (filters: ApartmentFilters, relaxed: ApartmentFilters | null) => {
    if (!relaxed) return ''
    const changes: string[] = []
    if (filters.ubication && relaxed.ubication === undefined) {
      changes.push('eliminar la ubicación')
    }
    if (filters.minPrice !== undefined && relaxed.minPrice !== undefined) {
      changes.push(`bajar el precio mínimo a ${relaxed.minPrice}€`)
    }
    if (filters.maxPrice !== undefined && relaxed.maxPrice !== undefined) {
      changes.push(`subir el precio máximo a ${relaxed.maxPrice}€`)
    }
    return changes.length > 0 ? `Prueba a ${changes.join(', ')}.` : 'Prueba a ampliar los filtros.'
  }

  useEffect(() => {
    if (!token || role !== 'TENANT' || !userId) {
      setLoading(false)
      return
    }

    const fetchDeckOrFiltered = async () => {
      try {
        setLoading(true)
        setNoFilteredResults(false)
        setSuggestedFilters(null)

        let data: ApartmentDTO[] = []

        if (Object.keys(appliedFilters).length > 0) {
          data = await searchApartments(
            appliedFilters.ubication,
            appliedFilters.minPrice,
            appliedFilters.maxPrice
          )
        } else {
          data = await getDeckForCandidate(Number(userId))
          setCachedApartments(data)
        }

        const filteredData = filterOutPendingApartments(data)
        setApartments(filteredData)

        if (filteredData.length === 0 && Object.keys(appliedFilters).length > 0) {
          setNoFilteredResults(true)
          setSuggestedFilters(computeRelaxedFilters(appliedFilters))
        }
      } catch (error: any) {
        console.error('Failed to load apartments', error)

        if (
          Object.keys(appliedFilters).length === 0 &&
          (!navigator.onLine || error?.message === 'Network Error')
        ) {
          const cached = getCachedApartments()
          const filteredCached = filterOutPendingApartments(cached)
          setApartments(filteredCached)
          console.log('Loaded apartments from cache (offline mode)')
        }
      } finally {
        setLoading(false)
      }
    }

    fetchDeckOrFiltered()
  }, [token, role, userId, appliedFilters])

  useEffect(() => {
    const syncPendingSwipes = async () => {
      const pending = getPendingSwipes()

      if (pending.length === 0) {
        return
      }

      console.log(`Sincronizando ${pending.length} swipes pendientes...`)
      const successfulSwipes: number[] = []

      for (const swipe of pending) {
        try {
          await swipeApartment(swipe.apartmentId, swipe.interest)
          successfulSwipes.push(swipe.apartmentId)
          console.log(`Swipe sincronizado: apartmentId=${swipe.apartmentId}, interest=${swipe.interest}`)
        } catch (error: any) {
          const status = error?.response?.status

          if (status === 409) {
            console.warn(`Conflicto detectado para apartmentId=${swipe.apartmentId}. Considerado sincronizado.`)
            successfulSwipes.push(swipe.apartmentId)
            continue
          }

          if (!navigator.onLine) {
            console.warn(`Sin conexión. Deteniendo sincronización.`)
            break
          }

          console.error(`Error sincronizando swipe ${swipe.apartmentId}:`, error)
        }
      }

      
      if (successfulSwipes.length > 0) {
        const remaining = pending.filter((s) => !successfulSwipes.includes(s.apartmentId))
        setPendingSwipes(remaining)
        console.log(`Cola de swipes limpiada. Pendientes: ${remaining.length}`)

        if (remaining.length === 0) {
          showToast('¡Todos los swipes se sincronizaron correctamente!', 'success')
        }
      }
    }

    window.addEventListener('online', syncPendingSwipes)

    return () => {
      window.removeEventListener('online', syncPendingSwipes)
    }
  }, [showToast])

  const handleFilterChange = (
    field: keyof typeof filterInputs,
    value: string
  ) => {
    setFilterInputs((prev) => ({ ...prev, [field]: value }))
  }

  const applyFilters = () => {
    setAppliedFilters(buildSearchFilters(filterInputs))
  }

  const resetFilters = () => {
    setFilterInputs({ ubication: '', minPrice: '', maxPrice: ''})
    setAppliedFilters({})
    setNoFilteredResults(false)
    setSuggestedFilters(null)
  }

  const expandFilters = () => {
    if (!suggestedFilters) return
    setFilterInputs({
      ubication: suggestedFilters.ubication ?? '',
      minPrice: suggestedFilters.minPrice !== undefined ? String(suggestedFilters.minPrice) : '',
      maxPrice: suggestedFilters.maxPrice !== undefined ? String(suggestedFilters.maxPrice) : '',
    })
    setAppliedFilters(suggestedFilters)
  }

  const handleSwipe = async (apartmentId: number, interest: boolean) => {
    setApartments((prev) => prev.filter((apt) => apt.id !== apartmentId))
    try {
      await swipeApartment(apartmentId, interest)

      if (interest) {
        showToast('¡Solicitud enviada correctamente!', 'success')
      }
      removePendingSwipe(apartmentId)
    } catch (error: any) {
      const response = error?.response;
  
      if (!response) {
        addPendingSwipe(apartmentId, interest);
        showToast('Modo offline: El swipe se sincronizará cuando vuelvas.', 'warning');
        console.log('Swipe guardado en cola offline (No response):', { apartmentId, interest });
        return; 
      }

      const status = response.status;
      const backendMessage = response.data?.message;

      if (status === 409) {
        console.warn('Swipe duplicado detectado:', backendMessage);
        showToast('Ya has solicitado interés anteriormente.', 'warning');
        removePendingSwipe(apartmentId);
        return;
      }

      showToast(
        `Error ${status}: ${status === 404 ? 'No encontrado' : 'Fallo en el servidor'}`,
        'error'
      );
    }
  }

  if (!token) {
    return (
      <HeroWrapper>
        <div className="text-center p-4">
          <img
            src="/Logo Rooma.jpeg"
            alt="Logo"
            className="w-40 h-40 md:w-56 md:h-56 mx-auto mb-10 rounded-[2.5rem] shadow-2xl object-cover"
          />
          <h1 className="text-6xl md:text-8xl font-black tracking-tight mb-6 text-base-content drop-shadow-lg">
            Bienvenido a <span className="text-primary block sm:inline mt-2 sm:mt-0">Rooma</span>
          </h1>
          <p className="text-base-content/90 font-medium text-xl md:text-2xl lg:text-3xl mb-12 max-w-2xl mx-auto drop-shadow">
            Encuentra tu piso ideal o publica el tuyo. Regístrate como inquilino o propietario para
            empezar.
          </p>
          <div className="flex flex-col sm:flex-row gap-5 justify-center items-center w-full max-w-lg mx-auto">
            <Link
              to="/register"
              className="btn btn-lg rounded-3xl w-full sm:w-auto flex-1 shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all text-lg border-none bg-primary text-primary-content"
            >
              Crear cuenta
            </Link>
            <Link
              to="/login"
              className="btn btn-lg btn-neutral rounded-3xl w-full sm:w-auto flex-1 shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all text-lg border-none"
            >
              Iniciar sesión
            </Link>
          </div>
        </div>
      </HeroWrapper>
    )
  }

  if (role === 'LANDLORD') {
    return (
      <HeroWrapper>
        <div className="text-center bg-base-100 rounded-3xl shadow-xl border border-base-200 p-8 md:p-10">
          <div className="mb-6 inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-secondary/10 text-secondary">
            <Building2 size={32} />
          </div>
          <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight mb-3">
            Panel de Arrendador
          </h1>
          <p className="text-base-content/60 text-base mb-8 leading-relaxed">
            Gestiona tus inmuebles y encuentra a los inquilinos perfectos.
          </p>
          <div className="grid grid-cols-2 gap-3">
            <Link
              to="/apartments/my"
              className="btn btn-outline rounded-2xl flex-col h-auto py-4 gap-2 border-base-300 hover:border-primary hover:bg-primary/5"
            >
              <Building2 size={22} />
              <span className="text-xs font-semibold">Mis Inmuebles</span>
            </Link>
            <Link
              to="/apartments/publish"
              className="btn btn-primary rounded-2xl flex-col h-auto py-4 gap-2"
            >
              <PlusCircle size={22} />
              <span className="text-xs font-semibold">Publicar piso</span>
            </Link>
          </div>
        </div>
      </HeroWrapper>
    )
  }

  if (role === 'ADMIN') {
    return (
      <HeroWrapper>
        <div className="text-center bg-base-100 rounded-3xl shadow-xl border border-base-200 p-8 md:p-10">
          <div className="mb-6 inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-error/10 text-error">
            <ShieldCheck size={32} />
          </div>
          <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight mb-3">
            Panel de Administración
          </h1>
          <p className="text-base-content/60 text-base mb-8 leading-relaxed">
            Gestiona los usuarios y la plataforma Rooma.
          </p>
          <Link to="/users" className="btn btn-primary rounded-2xl w-full gap-2">
            <Users2 size={18} />
            Gestionar usuarios
          </Link>
        </div>
      </HeroWrapper>
    )
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[85vh] w-full px-4 md:px-8 py-6 overflow-hidden relative">
      <div className="w-full max-w-4xl mb-6 space-y-4">
        <div className="bg-base-100 border border-base-200 rounded-3xl shadow-sm p-6">
          <div className="flex flex-col md:flex-row gap-4 md:items-end md:justify-between">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 flex-1">
              <label className="block text-sm font-medium text-base-content/80">
                Ubicación
                <input
                  type="text"
                  value={filterInputs.ubication}
                  onChange={(e) => handleFilterChange('ubication', e.target.value)}
                  placeholder="Ciudad, barrio..."
                  className="input input-bordered input-sm w-full mt-2"
                />
              </label>
              <label className="block text-sm font-medium text-base-content/80">
                Precio mínimo
                <input
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*" 
                  value={filterInputs.minPrice}
                  onChange={(e) => {
                  const value = e.target.value.replace(/\D/g, ''); 
                  handleFilterChange('minPrice', value);
                  }}
                  placeholder="ej: 0€"
                  className="input input-bordered input-sm w-full mt-2"
                />
              </label>
              <label className="block text-sm font-medium text-base-content/80">
                Precio máximo
                <input
                  type="text" 
                  inputMode="numeric" 
                  pattern="[0-9]*" 
                  value={filterInputs.maxPrice}
                  onChange={(e) => {
                  const value = e.target.value.replace(/\D/g, ''); 
                  handleFilterChange('maxPrice', value);
                  }}
                  placeholder="ej: 999€"
                  className="input input-bordered input-sm w-full mt-2"
                />
              </label>
            </div>
            <div className="flex gap-2 self-start md:self-auto">
              <button type="button" onClick={applyFilters} className="btn btn-primary btn-sm rounded-full">
                Buscar
              </button>
              <button type="button" onClick={resetFilters} className="btn btn-outline btn-sm rounded-full">
                Limpiar
              </button>
            </div>
          </div>
          {Object.keys(appliedFilters).length > 0 && (
            <p className="mt-4 text-sm text-base-content/70">
              Mostrando resultados de búsqueda ampliada.
            </p>
          )}
        </div>
        {noFilteredResults && (
          <div className="bg-warning/10 border border-warning/30 rounded-3xl p-5 text-warning">
            <p className="font-semibold">No hay viviendas disponibles con estos filtros.</p>
            <p className="mt-2 text-sm text-base-content/70">
              {getSuggestionDescription(appliedFilters, suggestedFilters)}
            </p>
            <div className="mt-4 flex flex-wrap gap-3">
              <button type="button" onClick={expandFilters} className="btn btn-outline btn-sm rounded-full">
                Ampliar filtros
              </button>
              <button type="button" onClick={resetFilters} className="btn btn-outline btn-sm rounded-full">
                Ver todas las viviendas
              </button>
            </div>
          </div>
        )}
      </div>

      {loading ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="flex flex-col items-center gap-4 text-primary"
        >
          <Loader2 className="animate-spin" size={48} />
          <p className="font-medium animate-pulse text-lg">Buscando tus opciones...</p>
        </motion.div>
      ) : noFilteredResults ? (
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center p-10 bg-base-100 rounded-3xl shadow-lg w-full max-w-sm border border-base-200"
        >
          <div className="text-6xl mb-6">🔎</div>
          <h2 className="text-2xl font-bold mb-3">No se encontraron viviendas</h2>
          <p className="text-base-content/60 text-sm leading-relaxed">
            Ajusta o amplía tus filtros para ver más opciones.
          </p>
        </motion.div>
      ) : apartments.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center p-10 bg-base-100 rounded-3xl shadow-lg w-full max-w-sm border border-base-200"
        >
          <div className="text-6xl mb-6">💤</div>
          <h2 className="text-2xl font-bold mb-3">No hay más pisos</h2>
          <p className="text-base-content/60 text-sm leading-relaxed">
            Vuelve más tarde para ver nuevas opciones publicadas.
          </p>
        </motion.div>
      ) : (
        /* Card stack */
        <div className="relative w-full max-w-sm md:max-w-md h-[62vh] min-h-[480px] max-h-[660px] flex items-center justify-center">
          <AnimatePresence initial={false}>
            {apartments.slice(-3).map((apartment, index, array) => {
              const isTop = index === array.length - 1
              const depth = array.length - 1 - index
              return (
                <div
                  key={apartment.id}
                  className={`absolute w-full h-full will-change-transform ${!isTop ? 'pointer-events-none' : ''}`}
                  style={{
                    zIndex: index,
                    transform: `scale(${1 - depth * 0.05}) translateY(-${depth * 20}px)`,
                    transition: 'transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)',
                    opacity: 1 - depth * 0.3,
                  }}
                >
                  <SwipeableCard
                    apartment={apartment}
                    onSwipe={(interest) => handleSwipe(apartment.id, interest)}
                    onShowDetails={() => setSelectedApartment(apartment)}
                  />
                </div>
              )
            })}
          </AnimatePresence>
        </div>
      )}

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
