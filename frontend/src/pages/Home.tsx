import { AnimatePresence } from 'framer-motion'
import { Loader2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ApartmentDetailModal from '../components/ApartmentDetailModal'
import SwipeableCard from '../components/SwipeableCard'
import { useToast } from '../hooks/useToast'
import type { ApartmentDTO } from '../service/apartment.service'
import { getDeckForCandidate, swipeApartment } from '../service/apartment.service'
import { useAuthStore } from '../store/authStore'

export default function Home() {
  const { token, userId, role } = useAuthStore()
  const { showToast } = useToast()
  const [apartments, setApartments] = useState<ApartmentDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedApartment, setSelectedApartment] = useState<ApartmentDTO | null>(null)

  useEffect(() => {
    if (!token || role !== 'TENANT' || !userId) {
      setLoading(false)
      return
    }

    const fetchDeck = async () => {
      try {
        setLoading(true)
        const data = await getDeckForCandidate(Number(userId))
        setApartments(data)
      } catch (error) {
        console.error('Failed to load deck', error)
      } finally {
        setLoading(false)
      }
    }

    fetchDeck()
  }, [token, role, userId])

  const handleSwipe = async (apartmentId: number, interest: boolean) => {
    setApartments((prev) => prev.filter((apt) => apt.id !== apartmentId))
    try {
      if (userId) {
        await swipeApartment(Number(userId), apartmentId, interest)
        if (interest) {
          showToast('¡Solicitud enviada correctamente!', 'success')
        }
      }
    } catch (error: unknown) {
      const axiosError = error as { response?: { status: number; data: unknown } }
      console.error('Failed to register swipe', axiosError?.response?.status, axiosError?.response?.data)
      showToast(`Error al registrar el swipe (${axiosError?.response?.status ?? 'red'})`, 'error')
    }
  }

  // --- NON-TENANT VIEWS ---
  if (!token) {
    return (
      <div className="hero min-h-[80vh] bg-base-200 px-6">
        <div className="hero-content text-center">
          <div className="max-w-xl">
            <h1 className="text-4xl md:text-5xl font-bold tracking-tight">Bienvenido a Rooma</h1>
            <p className="py-6 text-lg text-base-content/80">
              Encuentra tu piso ideal o publica el tuyo. Regístrate como inquilino o propietario para empezar.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link to="/register" className="btn btn-primary btn-wide sm:btn-md">Crear cuenta</Link>
              <Link to="/login" className="btn btn-ghost btn-wide sm:btn-md">Iniciar sesión</Link>
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (role === 'LANDLORD') {
    return (
      <div className="hero min-h-[80vh] bg-base-200 px-6">
        <div className="hero-content text-center">
          <div className="max-w-xl">
            <h1 className="text-4xl md:text-5xl font-bold tracking-tight">Panel de Arrendador</h1>
            <p className="py-6 text-lg text-base-content/80">Gestiona tus inmuebles y encuentra a los inquilinos perfectos.</p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link to="/apartments/my" className="btn btn-primary btn-wide sm:btn-md">Mis Inmuebles</Link>
              <Link to="/apartments/publish" className="btn btn-ghost btn-wide sm:btn-md">Publicar piso</Link>
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (role === 'ADMIN') {
    return (
      <div className="hero min-h-[80vh] bg-base-200 px-6">
        <div className="hero-content text-center">
          <div className="max-w-xl">
            <h1 className="text-4xl md:text-5xl font-bold tracking-tight">Panel de Administración</h1>
            <p className="py-6 text-lg text-base-content/80">Gestiona los usuarios y la plataforma Rooma.</p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link to="/users" className="btn btn-primary btn-wide sm:btn-md">Gestionar usuarios</Link>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // --- TENANT SWIPING VIEW ---
  return (
    <div className="flex flex-col items-center justify-center min-h-[85vh] w-full px-6 md:px-8 py-8 overflow-hidden bg-base-200 relative">
      {loading ? (
        <div className="flex flex-col items-center gap-4 text-primary">
          <Loader2 className="animate-spin" size={48} />
          <p className="font-medium animate-pulse text-lg">Buscando tus opciones...</p>
        </div>
      ) : apartments.length === 0 ? (
        <div className="text-center p-10 bg-base-100 rounded-3xl shadow-lg w-full max-w-md border border-base-300">
          <div className="text-6xl mb-6">💤</div>
          <h2 className="text-2xl font-bold mb-3">No hay más pisos</h2>
          <p className="text-base-content/70">Vuelve más tarde para ver nuevas opciones publicadas.</p>
        </div>
      ) : (
        /* Card Container - Responsive Height & Constrained Width */
        <div className="relative w-full max-w-md h-[65vh] min-h-[500px] max-h-[650px] flex items-center justify-center mt-4">
          <AnimatePresence>
            {apartments.map((apartment, index) => {
              const isTop = index === apartments.length - 1
              return (
                <div
                  key={apartment.id}
                  className={`absolute w-full h-full will-change-transform ${!isTop ? 'pointer-events-none' : ''}`}
                  style={{
                    zIndex: index,
                    transform: `scale(${1 - (apartments.length - 1 - index) * 0.04}) translateY(-${(apartments.length - 1 - index) * 16}px)`,
                    transition: 'transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1)', // Smoother spring-like transition
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