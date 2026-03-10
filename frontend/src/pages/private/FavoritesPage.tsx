import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FavoriteButton from '../../components/FavoriteButton'
import {
  getFavorites,
  type FavoriteItem,
  type FavoriteAvailabilityStatus,
} from '../../service/favorites.service'

const MAIN_CLASS = 'px-6 md:px-12 py-8 max-w-5xl mx-auto'
const GRID_CLASS = 'grid grid-cols-1 sm:grid-cols-2 gap-6'

const AVAILABILITY_LABELS: Record<FavoriteAvailabilityStatus, string> = {
  AVAILABLE: 'Disponible',
  CLOSED: 'No disponible',
}

export default function FavoritesPage() {
  const [favorites, setFavorites] = useState<FavoriteItem[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetchFavorites = async () => {
      try {
        setIsLoading(true)
        const data = await getFavorites()
        setFavorites(data)
      } catch (err) {
        console.error('Error fetching favorites', err)
        setError('No se pudieron cargar tus favoritos.')
      } finally {
        setIsLoading(false)
      }
    }

    void fetchFavorites()
  }, [])

  const handleFavoriteChange = (apartmentId: number, isFavorite: boolean) => {
    if (!isFavorite) {
      setFavorites((prev) => prev.filter((item) => item.apartmentId !== apartmentId))
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
      <p className="text-gray-500 text-lg font-medium">
        Todavía no tienes propiedades favoritas.
      </p>
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
    <div className="min-h-dvh bg-base-200/50">
      <header className="bg-base-100 px-6 pt-8 pb-6 md:px-12">
        <div className="max-w-5xl mx-auto">
          <h1 className="text-3xl font-bold text-base-content">Mis favoritos</h1>
          <p className="text-gray-400 mt-1 text-sm">
            Revisa rápidamente los pisos que has guardado.
          </p>
        </div>
      </header>

      <main className={MAIN_CLASS}>
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

                    <div className="absolute top-3 right-3">
                      <FavoriteButton
                        apartmentId={fav.apartmentId}
                        initialIsFavorite={fav.isFavorite}
                        onChange={(isFavorite) =>
                          handleFavoriteChange(fav.apartmentId, isFavorite)
                        }
                      />
                    </div>
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

                    {isUnavailable && fav.statusMessage && (
                      <p className="mt-2 text-xs text-error/80 bg-error/5 rounded-lg px-3 py-2">
                        {fav.statusMessage}
                      </p>
                    )}
                  </div>
                </div>
              )

              return isUnavailable ? (
                <div
                  key={fav.apartmentId}
                  className="opacity-80 cursor-not-allowed"
                >
                  {CardInner}
                </div>
              ) : (
                <Link
                  key={fav.apartmentId}
                  to={`/properties/${fav.apartmentId}`}
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

