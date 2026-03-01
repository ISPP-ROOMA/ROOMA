import { AnimatePresence, motion } from 'framer-motion'
import { Building2, Loader2, PlusCircle, ShieldCheck, Users2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ApartmentDetailModal from '../components/ApartmentDetailModal'
import SwipeableCard from '../components/SwipeableCard'
import { useToast } from '../hooks/useToast'
import type { ApartmentDTO } from '../service/apartment.service'
import { getDeckForCandidate, swipeApartment } from '../service/apartment.service'
import { useAuthStore } from '../store/authStore'

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
      await swipeApartment(Number(userId), apartmentId, interest)
      if (interest) {
        showToast('¡Solicitud enviada correctamente!', 'success')
      }
    } catch (error: unknown) {
      const axiosError = error as { response?: { status: number; data: unknown } }
      console.error('Failed to register swipe', axiosError?.response?.status, axiosError?.response?.data)
      showToast(`Error al registrar el swipe (${axiosError?.response?.status ?? 'red'})`, 'error')
    }
  }

  if (!token) {
    return (
      <HeroWrapper>
        <div className="text-center p-4">
          <img src="/Logo Rooma.jpeg" alt="Logo" className="w-40 h-40 md:w-56 md:h-56 mx-auto mb-10 rounded-[2.5rem] shadow-2xl object-cover" />
          <h1 className="text-6xl md:text-8xl font-black tracking-tight mb-6 text-base-content drop-shadow-lg">
            Bienvenido a <span className="text-primary block sm:inline mt-2 sm:mt-0">Rooma</span>
          </h1>
          <p className="text-base-content/90 font-medium text-xl md:text-2xl lg:text-3xl mb-12 max-w-2xl mx-auto drop-shadow">
            Encuentra tu piso ideal o publica el tuyo. Regístrate como inquilino o propietario para empezar.
          </p>
          <div className="flex flex-col sm:flex-row gap-5 justify-center items-center w-full max-w-lg mx-auto">
            <Link to="/register" className="btn btn-lg rounded-3xl w-full sm:w-auto flex-1 shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all text-lg border-none bg-primary text-primary-content">
              Crear cuenta
            </Link>
            <Link to="/login" className="btn btn-lg btn-neutral rounded-3xl w-full sm:w-auto flex-1 shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all text-lg border-none">
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
          <Link
            to="/users"
            className="btn btn-primary rounded-2xl w-full gap-2"
          >
            <Users2 size={18} />
            Gestionar usuarios
          </Link>
        </div>
      </HeroWrapper>
    )
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[85vh] w-full px-4 md:px-8 py-6 overflow-hidden relative">
      {loading ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="flex flex-col items-center gap-4 text-primary"
        >
          <Loader2 className="animate-spin" size={48} />
          <p className="font-medium animate-pulse text-lg">Buscando tus opciones...</p>
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
                    transition: 'transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1)',
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
