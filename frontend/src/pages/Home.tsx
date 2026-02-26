import { AnimatePresence } from 'framer-motion'
import { Loader2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ApartmentDetailModal from '../components/ApartmentDetailModal'
import SwipeableCard from '../components/SwipeableCard'
import type { ApartmentDTO } from '../service/apartment.service'
import { searchApartments, swipeApartment } from '../service/apartment.service'
import { useAuthStore } from '../store/authStore'
import { useToast } from '../hooks/useToast'

export default function Home() {
  const { token, userId, role } = useAuthStore()
  const { showToast } = useToast()
  const [apartments, setApartments] = useState<ApartmentDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedApartment, setSelectedApartment] = useState<ApartmentDTO | null>(null)

  useEffect(() => {
    if (!token || role === 'LANDLORD') {
      setLoading(false)
      return
    }

    const fetchDeck = async () => {
      try {
        setLoading(true)
        // Only load ACTIVE apartments for swiping
        const data = await searchApartments(undefined, undefined, undefined, 'ACTIVE')
        setApartments(data)
      } catch (error) {
        console.error('Failed to load deck', error)
      } finally {
        setLoading(false)
      }
    }

    fetchDeck()
  }, [token, role])

  const handleSwipe = async (apartmentId: number, interest: boolean) => {
    // Optimistically remove from UI
    setApartments((prev) => prev.filter((apt) => apt.id !== apartmentId))

    try {
      if (userId) {
        await swipeApartment(Number(userId), apartmentId, interest)
        if (interest) {
          showToast('¡Solicitud enviada correctamente!', 'success')
        }
      }
    } catch (error) {
      console.error('Failed to register swipe', error)
      // We could add it back to the deck if the API fails
    }
  }

  // Not logged in view
  if (!token) {
    return (
      <div className="hero min-h-[60vh] bg-base-200">
        <div className="hero-content text-center">
          <div className="max-w-2xl">
            <h1 className="text-5xl font-bold">Bienvenido a ROOMA</h1>
            <p className="py-6">
              Encuentra a tus compañeros de piso ideales o publica tu habitación libre. Inicia
              sesión o regístrate para comenzar a conectar.
            </p>
            <div className="flex flex-wrap gap-4 justify-center">
              <Link to="/login" className="btn btn-primary">
                Iniciar sesión
              </Link>
              <Link to="/register" className="btn btn-ghost">
                Registrarse
              </Link>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // Landlords shouldn't swipe
  if (role === 'LANDLORD') {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] p-4 text-center">
        <h2 className="text-3xl font-bold mb-4">Panel de Arrendador</h2>
        <p className="text-base-content/70 max-w-md">
          Como arrendador, puedes gestionar tus propiedades y revisar los perfiles de los inquilinos
          interesados.
        </p>
        <Link to="/profile" className="btn btn-primary mt-6">
          Ir a mi perfil
        </Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] w-full px-4 overflow-hidden bg-base-200 relative">
      {loading ? (
        <div className="flex flex-col items-center gap-4 text-primary">
          <Loader2 className="animate-spin" size={48} />
          <p className="font-medium animate-pulse">Buscando compañeros ideales...</p>
        </div>
      ) : apartments.length === 0 ? (
        <div className="text-center p-8 bg-base-100 rounded-3xl shadow-sm max-w-sm">
          <div className="text-6xl mb-4">💤</div>
          <h2 className="text-2xl font-bold mb-2">No hay más pisos</h2>
          <p className="text-base-content/70">
            Has visto todos los pisos disponibles por ahora. Ajusta tus filtros o vuelve más tarde.
          </p>
        </div>
      ) : (
        <div className="relative w-full max-w-sm h-[600px] flex items-center justify-center">
          <AnimatePresence>
            {/* Render from bottom to top so the last element in array is at the top of the stack */}
            {apartments.map((apartment, index) => {
              const isTop = index === apartments.length - 1
              return (
                <div
                  key={apartment.id}
                  className={`absolute w-full h-full will-change-transform ${!isTop ? 'pointer-events-none' : ''}`}
                  style={{
                    zIndex: index,
                    transform: `scale(${1 - (apartments.length - 1 - index) * 0.04}) translateY(-${(apartments.length - 1 - index) * 12}px)`,
                    transition: 'transform 0.3s ease-out',
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

      {/* Modal is rendered conditionally */}
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
