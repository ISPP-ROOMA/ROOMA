import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyApartments, type Apartment, updateApartment } from '../../service/apartments.service'
import { getMatchesForLandlord, type MatchStatus } from '../../service/apartment.service'
import PropertyCard from '../../components/PropertyCard'
import { useAuthStore } from '../../store/authStore'

const HEADER_CLASS = 'bg-base-100 px-6 pt-8 pb-6 md:px-12'
const CTA_CLASS =
  'mt-5 w-full flex items-center justify-center gap-2 bg-teal-600 hover:bg-teal-700 text-white font-semibold py-3.5 rounded-full shadow-md transition text-base'
const MAIN_CLASS = 'px-6 md:px-12 py-8 max-w-7xl mx-auto'
const GRID_CLASS = 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8'

export default function Apartments() {
  const navigate = useNavigate()
  const { token, userId } = useAuthStore()
  const [apartments, setApartments] = useState<Apartment[]>([])
  const [statsByApartmentId, setStatsByApartmentId] = useState<
    Record<number, { requests: number; matches: number }>
  >({})
  const [isLoading, setIsLoading] = useState(true)

  const hasApartments = apartments.length > 0

  const handlePublishNavigation = () => {
    navigate('/apartments/publish')
  }

  const toggleApartmentState = async (apartment: Apartment) => {
    const isAvailable = apartment.state === 'ACTIVE' || apartment.state === 'MATCHING'
    const nextState = isAvailable ? 'CLOSED' : 'ACTIVE'

    setApartments((prev) =>
      prev.map((item) => (item.id === apartment.id ? { ...item, state: nextState } : item))
    )

    try {
      const updated = await updateApartment(apartment.id, {
        title: apartment.title,
        description: apartment.description,
        price: apartment.price,
        bills: apartment.bills,
        ubication: apartment.ubication,
        state: nextState,
      })

      setApartments((prev) => prev.map((item) => (item.id === apartment.id ? updated : item)))
    } catch (error) {
      setApartments((prev) =>
        prev.map((item) => (item.id === apartment.id ? { ...item, state: apartment.state } : item))
      )
      console.error(error)
    }
  }

  useEffect(() => {
    if (!token) {
      setIsLoading(false)
      navigate('/login')
      return
    }

    const fetchApartments = async () => {
      try {
        const data = await getMyApartments()
        setApartments(data)

        if (!userId) {
          setStatsByApartmentId({})
          return
        }

        const landlordId = Number(userId)
        if (Number.isNaN(landlordId)) {
          setStatsByApartmentId({})
          return
        }

        const requestStatuses: MatchStatus[] = ['ACTIVE']
        const matchStatuses: MatchStatus[] = ['MATCH', 'SUCCESSFUL', 'INVITED']

        const [requestGroups, matchGroups] = await Promise.all([
          Promise.all(requestStatuses.map((status) => getMatchesForLandlord(landlordId, status))),
          Promise.all(matchStatuses.map((status) => getMatchesForLandlord(landlordId, status))),
        ])

        const nextStats: Record<number, { requests: number; matches: number }> = {}

        for (const group of requestGroups) {
          for (const match of group) {
            if (!nextStats[match.apartmentId]) {
              nextStats[match.apartmentId] = { requests: 0, matches: 0 }
            }
            nextStats[match.apartmentId].requests += 1
          }
        }

        for (const group of matchGroups) {
          for (const match of group) {
            if (!nextStats[match.apartmentId]) {
              nextStats[match.apartmentId] = { requests: 0, matches: 0 }
            }
            nextStats[match.apartmentId].matches += 1
          }
        }

        setStatsByApartmentId(nextStats)
      } catch (error) {
        console.error(error)
      } finally {
        setIsLoading(false)
      }
    }
    fetchApartments()
  }, [navigate, token, userId])

  const renderEmptyState = () => (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="w-20 h-20 rounded-full bg-base-200 flex items-center justify-center mb-4">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-10 w-10 text-gray-400"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M3 11.5L12 4l9 7.5V20a1 1 0 01-1 1h-5v-6H9v6H4a1 1 0 01-1-1v-8.5z"
          />
        </svg>
      </div>
      <p className="text-gray-500 text-lg font-medium">¡Añade tu primer inmueble!</p>
      <p className="text-gray-400 text-sm mt-1">Pulsa el botón de arriba para empezar.</p>
    </div>
  )

  const renderApartments = () => (
    <div className={GRID_CLASS}>
      {apartments.map((apt) => (
        <PropertyCard
          key={apt.id}
          id={apt.id}
          title={apt.title}
          price={apt.price}
          coverImageUrl={apt.coverImageUrl}
          photoCount={0}
          status={apt.state === 'ACTIVE' || apt.state === 'MATCHING' ? 'active' : 'paused'}
          stats={statsByApartmentId[apt.id] ?? { requests: 0, matches: 0 }}
          onEdit={() => {
            navigate(`/apartments/${apt.id}/edit`)
          }}
          onPause={() => toggleApartmentState(apt)}
        />
      ))}
    </div>
  )

  return (
    <div className="min-h-dvh bg-base-200/50">
      <header className={HEADER_CLASS}>
        <div className="max-w-7xl mx-auto">
          <h1 className="text-3xl font-bold text-base-content">Mis Inmuebles</h1>
          <p className="text-gray-400 mt-1 text-sm">Gestiona tu cartera de propiedades</p>

          <button onClick={handlePublishNavigation} className={CTA_CLASS}>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4v16m8-8H4"
              />
            </svg>
            Publicar nuevo inmueble
          </button>
        </div>
      </header>

      <main className={MAIN_CLASS}>
        {isLoading ? (
          <p className="text-center mt-10 opacity-50">Cargando inmuebles...</p>
        ) : !hasApartments ? (
          renderEmptyState()
        ) : (
          renderApartments()
        )}
      </main>
    </div>
  )
}
