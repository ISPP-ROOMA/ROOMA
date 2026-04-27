import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { X, Heart } from 'lucide-react'
import FavoriteButton from '../../components/FavoriteButton'
import {
  getFavorites,
  removeFavorite,
  type FavoriteItem,
  type FavoriteAvailabilityStatus,
} from '../../service/favorites.service'
import { swipeApartment, getMatchesForCandidate, type MatchStatus } from '../../service/apartment.service'
import { useToast } from '../../hooks/useToast'
import { useAuthStore } from '../../store/authStore'

const GRID_CLASS = 'grid grid-cols-1 sm:grid-cols-2 gap-6'

const AVAILABILITY_LABELS: Record<FavoriteAvailabilityStatus, string> = {
  AVAILABLE: 'Disponible',
  CLOSED: 'No disponible',
}

export default function FavoritesPage() {
  const [favorites, setFavorites] = useState<FavoriteItem[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [decidingApartmentId, setDecidingApartmentId] = useState<number | null>(null)
  const [swipedApartmentIds, setSwipedApartmentIds] = useState<Set<number>>(new Set())
  const { showToast } = useToast()
  const userId = useAuthStore((s) => s.userId) as number | null

  useEffect(() => {
    const fetchFavorites = async () => {
      try {
        setIsLoading(true)
        const data = await getFavorites()
        setFavorites(data)

        // If we have a logged user, fetch matches to know which apartments were already swiped
        if (userId) {
          const statuses: MatchStatus[] = ['ACTIVE', 'MATCH', 'REJECTED', 'SUCCESSFUL', 'INVITED', 'CANCELED', 'WAITING']
          const promises = statuses.map((s) => getMatchesForCandidate(Number(userId), s))
          const results = await Promise.all(promises)
          const ids = new Set<number>()
          results.flat().forEach((m) => ids.add(m.apartmentId))
          setSwipedApartmentIds(ids)
        }
      } catch (err) {
        console.error('Error fetching favorites', err)
        setError('No se pudieron cargar tus favoritos.')
      } finally {
        setIsLoading(false)
      }
    }

    void fetchFavorites()
  }, [userId])

  const handleFavoriteChange = (apartmentId: number, isFavorite: boolean) => {
    if (!isFavorite) {
      setFavorites((prev) => prev.filter((item) => item.apartmentId !== apartmentId))
    }
  }

  const handleDecision = async (apartmentId: number, interest: boolean) => {
    if (decidingApartmentId !== null) {
      return
    }

    try {
      setDecidingApartmentId(apartmentId)
      await swipeApartment(apartmentId, interest)
      // Also remove from favorites so it doesn't reappear when reloading
      try {
        await removeFavorite(apartmentId)
      } catch (err) {
        console.warn('Could not remove favorite after swipe', err)
      }
      setFavorites((prev) => prev.filter((item) => item.apartmentId !== apartmentId))
      showToast(
        interest ? 'Has marcado la vivienda como Like.' : 'Has marcado la vivienda como Dislike.',
        'success'
      )
    } catch (error) {
      console.error('Error registering favorite decision', error)
      showToast('No se pudo registrar tu decisión. Inténtalo de nuevo.', 'error')
    } finally {
      setDecidingApartmentId(null)
    }
  }

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
            d="M21 21l-4.35-4.35M11 19a8 8 0 100-16 8 8 0 000 16z"
          />
        </svg>
      </div>
      <p className="text-gray-500 text-lg font-medium">Todavía no tienes propiedades favoritas.</p>
      <p className="text-gray-400 text-sm mt-1">
        Explora pisos y marca tus preferidos para verlos aquí.
      </p>
    </div>
  )

  const renderErrorState = () => (
    <div className="alert alert-error shadow">
      <span>{error}</span>
    </div>
  )

  return (
    <div
      data-theme="light"
      className="mx-auto w-full max-w-5xl min-h-dvh text-[#050505] pb-28"
    >
      <header className="sticky top-0 z-10 px-4 sm:px-8 pt-5 pb-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xl sm:text-3xl font-bold text-[#050505] text-center px-2">
            Mis favoritos
          </h1>
          <div className="h-10 w-10" aria-hidden />
        </div>
      </header>

      <main className="px-4 sm:px-8 mt-5">
        {isLoading ? (
          <p className="text-center mt-10 opacity-50">Cargando favoritos...</p>
        ) : error ? (
          renderErrorState()
        ) : favorites.length === 0 ? (
          renderEmptyState()
        ) : (
          <div className={GRID_CLASS}>
            {favorites.map((fav) => {
              const isUnavailable = fav.availabilityStatus === 'CLOSED' || !fav.detailAccessible
              const CardInner = (
                <div className="bg-base-100 rounded-2xl shadow-md overflow-hidden flex flex-col h-full">
                  <div className="relative h-40 bg-gray-100">
                    {fav.mainImageUrl ? (
                      <img
                        src={fav.mainImageUrl}
                        alt={fav.title}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="h-full w-full flex items-center justify-center text-sm text-gray-400">
                        Sin imagen
                      </div>
                    )}

                    {isUnavailable && (
                      <span className="absolute top-3 left-3 px-3 py-1 rounded-full text-xs font-semibold bg-error/90 text-error-content shadow">
                        {AVAILABILITY_LABELS[fav.availabilityStatus]}
                      </span>
                    )}


                  </div>

                  <div className="p-4 flex flex-col gap-2 flex-1">
                    <h2 className="font-semibold text-base text-base-content line-clamp-2">
                      {fav.title}
                    </h2>
                    <p className="text-sm text-gray-500 line-clamp-1">{fav.city}</p>
                    <div className="flex items-center justify-between mt-auto pt-2">
                      <span className="text-xl font-extrabold text-primary">
                        {fav.price} €
                        <span className="text-sm font-normal text-gray-400"> / mes</span>
                      </span>
                    </div>

                    {!isUnavailable && !swipedApartmentIds.has(fav.apartmentId) && (
                      <div className="mt-2 flex items-center justify-center gap-6">
                        <button
                          type="button"
                          onClick={(event) => {
                            event.preventDefault()
                            event.stopPropagation()
                            void handleDecision(fav.apartmentId, false)
                          }}
                          disabled={decidingApartmentId === fav.apartmentId}
                          className="btn btn-circle shadow hover:scale-105 active:scale-95 bg-[#e5e7eb] border-[#e5e7eb]"
                          style={{ width: 52, height: 52 }}
                          aria-label={`Marcar ${fav.title} como Dislike`}
                        >
                          <X size={22} />
                        </button>
                        <div>
                          <FavoriteButton
                            apartmentId={fav.apartmentId}
                            initialIsFavorite={fav.isFavorite}
                            onChange={(isFavorite) => handleFavoriteChange(fav.apartmentId, isFavorite)}
                            className="btn btn-circle shadow"
                            style={{ width: 52, height: 52 }}
                          />
                        </div>
                        <button
                          type="button"
                          onClick={(event) => {
                            event.preventDefault()
                            event.stopPropagation()
                            void handleDecision(fav.apartmentId, true)
                          }}
                          disabled={decidingApartmentId === fav.apartmentId}
                          className="btn btn-circle shadow text-white bg-[#008080] border-[#008080]"
                          style={{ width: 52, height: 52 }}
                          aria-label={`Marcar ${fav.title} como Like`}
                        >
                          <Heart size={22} />
                          {/* visually we keep no label to match deck style, but preserve aria-label */}
                        </button>
                      </div>
                    )}

                    {!isUnavailable && swipedApartmentIds.has(fav.apartmentId) && (
                      <div className="mt-2 flex items-center justify-center">
                        <FavoriteButton
                          apartmentId={fav.apartmentId}
                          initialIsFavorite={fav.isFavorite}
                          onChange={(isFavorite) => handleFavoriteChange(fav.apartmentId, isFavorite)}
                          className="btn btn-circle shadow"
                          style={{ width: 52, height: 52 }}
                        />
                      </div>
                    )}

                    {isUnavailable && fav.statusMessage && (
                      <p className="mt-2 text-xs text-error/80 bg-error/5 rounded-lg px-3 py-2">
                        {fav.statusMessage}
                      </p>
                    )}
                  </div>
                </div>
              )

              return isUnavailable ? (
                <div key={fav.apartmentId} className="opacity-80 cursor-not-allowed">
                  {CardInner}
                </div>
              ) : (
                <Link
                  key={fav.apartmentId}
                  to={`/apartments/${fav.apartmentId}`}
                  className="block h-full"
                >
                  {CardInner}
                </Link>
              )
            })}
          </div>
        )}
      </main>
    </div>
  )
}
