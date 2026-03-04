import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { getApartment, type Apartment } from '../../service/apartments.service'

export default function ApartmentDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [apartment, setApartment] = useState<Apartment | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const fetch = async () => {
      if (!id) {
        setIsLoading(false)
        return
      }

      const apartmentId = Number(id)
      if (!Number.isFinite(apartmentId)) {
        setIsLoading(false)
        return
      }

      try {
        const data = await getApartment(apartmentId)
        if (data) setApartment(data)
      } catch (error) {
        console.error(error)
      } finally {
        setIsLoading(false)
      }
    }
    fetch()
  }, [id])

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Cargando inmueble...</p>
  if (!apartment) return <p className="text-center mt-10 text-red-500">Inmueble no encontrado.</p>

  return (
    <div className="p-6 min-h-[70vh]">
      <div className="max-w-3xl mx-auto">
        <h1 className="text-3xl font-semibold mb-4">{apartment.title}</h1>
        <p className="text-gray-600 mb-4">{apartment.description}</p>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
          <div>
            <strong>Precio:</strong> {apartment.price} €/mes
          </div>
          <div>
            <strong>Ubicación:</strong> {apartment.ubication}
          </div>
          <div>
            <strong>Gastos:</strong> {apartment.bills || 'No especificados'}
          </div>
          <div>
            <strong>Estado:</strong> {apartment.state}
          </div>
        </div>

        <div className="flex gap-2 flex-wrap">
          <Link to="/apartments/my" className="btn btn-ghost">
            Volver
          </Link>

          <button
            onClick={() => navigate(`/apartments/${id}/bills`)}
            className="flex items-center gap-2 bg-teal-600 hover:bg-teal-700 text-white font-semibold px-5 py-2.5 rounded-full shadow-md transition text-sm"
          >
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
                d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z"
              />
            </svg>
            Ver Facturas
          </button>

          <button
            onClick={() => navigate(`/apartments/${id}/new-bill`)}
            className="flex items-center gap-2 bg-white border-2 border-teal-600 text-teal-700 hover:bg-teal-50 font-semibold px-5 py-2.5 rounded-full shadow-sm transition text-sm"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Añadir Factura
          </button>
        </div>
      </div>
    </div>
  )
}
