import { useEffect, useState } from "react"
import { useParams, Link } from "react-router-dom"
import { getApartment, type Apartment } from "../../service/apartments.service"

export default function ApartmentDetail() {
  const { id } = useParams()
  const [apartment, setApartment] = useState<Apartment | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const fetch = async () => {
      if (!id) return
      try {
        const data = await getApartment(Number(id))
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
          <div><strong>Precio:</strong> {apartment.price} €/mes</div>
          <div><strong>Ubicación:</strong> {apartment.ubication}</div>
          <div><strong>Gastos:</strong> {apartment.bills || 'No especificados'}</div>
          <div><strong>Estado:</strong> {apartment.state}</div>
        </div>

        <div className="flex gap-2">
          <Link to="/apartments" className="btn btn-ghost">Volver</Link>
        </div>
      </div>
    </div>
  )
}
