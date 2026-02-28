import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApartments, type Apartment } from '../../service/apartments.service'

const MAIN_CLASS = 'px-6 md:px-12 py-8 max-w-7xl mx-auto'
const GRID_CLASS = 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6'

export default function BrowseApartments() {
  const [apartments, setApartments] = useState<Apartment[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const fetchApartments = async () => {
      try {
        const data = await getApartments()
        setApartments(data.filter((a) => a.state === 'available'))
      } catch (error) {
        console.error(error)
      } finally {
        setIsLoading(false)
      }
    }
    fetchApartments()
  }, [])

  return (
    <div className="min-h-dvh bg-base-200/50">
      <header className="bg-base-100 px-6 pt-8 pb-6 md:px-12">
        <div className="max-w-7xl mx-auto">
          <h1 className="text-3xl font-bold text-base-content">Explorar pisos</h1>
          <p className="text-gray-400 mt-1 text-sm">Encuentra tu próximo hogar</p>
        </div>
      </header>
      <main className={MAIN_CLASS}>
        {isLoading ? (
          <p className="text-center mt-10 opacity-50">Cargando pisos disponibles...</p>
        ) : apartments.length === 0 ? (
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
            <p className="text-gray-500 text-lg font-medium">No hay pisos disponibles</p>
            <p className="text-gray-400 text-sm mt-1">
              Vuelve más tarde para ver nuevas publicaciones.
            </p>
          </div>
        ) : (
          <div className={GRID_CLASS}>
            {apartments.map((apt) => (
              <Link
                key={apt.id}
                to={`/apartments/${apt.id}`}
                className="bg-base-100 rounded-2xl shadow-md overflow-hidden hover:shadow-lg transition flex flex-col"
              >
                <div className="h-40 bg-gray-100 flex items-center justify-center">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-12 w-12 text-gray-300"
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
                <div className="p-5 flex flex-col gap-2 flex-1">
                  <h3 className="font-bold text-base text-base-content line-clamp-1">
                    {apt.title}
                  </h3>
                  <p className="text-sm text-gray-500 line-clamp-2">{apt.description}</p>
                  <div className="flex items-center justify-between mt-auto pt-2">
                    <span className="text-xl font-extrabold text-primary">
                      {apt.price} €<span className="text-sm font-normal text-gray-400"> / mes</span>
                    </span>
                    <span className="text-xs text-gray-400">{apt.ubication}</span>
                  </div>
                  {apt.bills && <p className="text-xs text-gray-400">Gastos: {apt.bills}</p>}
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
