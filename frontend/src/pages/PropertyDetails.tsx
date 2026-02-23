import { Link, useParams } from "react-router-dom"
import { useMemo } from "react"

type Occupant = {
  name: string
  age?: number
}

type PropertyDetails = {
  id: string
  title?: string
  subtitle?: string
  priceMonthly?: string
  rating?: number
  description?: string
  images?: string[]
  verified?: boolean
  occupants?: Occupant[]
  amenities?: string[]
}

const fallbackProperty: PropertyDetails = {
  id: "1",
  title: "Piso luminoso con terraza y vistas",
  subtitle: "Zona centro · A 8 min de la estación",
  priceMonthly: "450€/mes",
  rating: 4.8,
  description:
    "Piso recién reformado con luz natural todo el día, espacios abiertos y una terraza tranquila. Cocina equipada, salón amplio y habitaciones cómodas. Ideal para estudiantes o jóvenes profesionales que busquen un entorno calmado sin renunciar a la cercanía del centro.",
  images: [
    "https://images.unsplash.com/photo-1505691938895-1758d7feb511?q=80&w=1600&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1502005097973-6a7082348e28?q=80&w=1600&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?q=80&w=1600&auto=format&fit=crop"
  ],
  verified: true,
  occupants: [
    { name: "Laura", age: 23 },
    { name: "Marcos", age: 25 },
    { name: "Claudia", age: 24 }
  ],
  amenities: ["Internet", "Lavadora", "Pet friendly", "Balcón"]
}

export default function PropertyDetails() {
  const { id } = useParams()

  const property = useMemo<PropertyDetails>(() => {
    if (id === fallbackProperty.id) {
      return fallbackProperty
    }

    return {
      id: id ?? "0",
      title: "Detalle de inmueble",
      subtitle: "Ubicación aproximada",
      priceMonthly: "450€/mes",
      rating: 4.7,
      description:
        "Descripción no disponible todavía. Añade más información del inmueble para que los interesados puedan conocer mejor el espacio.",
      images: [],
      verified: false,
      occupants: [],
      amenities: []
    }
  }, [id])

  const isLoading = false
  const hasError = false
  const isPrivacyUnlocked = false
  const images = property.images ?? []
  const occupants = property.occupants ?? []
  const amenities = property.amenities ?? []

  const handleShare = () => {
    window.alert("Compartir (demo)")
  }

  const handleLike = () => {
    window.alert("Solicitud de Like enviada (demo)")
  }

  if (isLoading) {
    return (
      <section className="px-4 py-6">
        <div className="skeleton h-8 w-40 mb-6" />
        <div className="skeleton h-64 w-full mb-6 rounded-2xl" />
        <div className="skeleton h-6 w-3/4 mb-3" />
        <div className="skeleton h-6 w-1/2" />
      </section>
    )
  }

  if (hasError) {
    return (
      <section className="px-4 py-6">
        <div className="alert alert-error shadow">
          <span>No se pudo cargar el detalle del inmueble.</span>
        </div>
      </section>
    )
  }

  return (
    <section className="px-4 py-6 pb-28 max-w-4xl mx-auto bg-[#F5F1E3] min-h-[calc(100vh-64px)] text-[#050505]">
      <header className="flex items-center justify-between mb-6">
        <Link to="/" className="btn btn-ghost btn-sm">Volver</Link>
        <h1 className="text-lg font-semibold text-center flex-1">Detalle de inmueble</h1>
        <button onClick={handleShare} className="btn btn-ghost btn-sm">
          <span className="sr-only">Compartir</span>
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path strokeLinecap="round" strokeLinejoin="round" d="M8 12h8m-8 0l3-3m-3 3l3 3M16 6a2 2 0 110 4 2 2 0 010-4zM16 14a2 2 0 110 4 2 2 0 010-4z" />
          </svg>
        </button>
      </header>

      <div className="relative mb-6">
        <div className="overflow-hidden rounded-2xl shadow-md bg-[#DDDBCB]">
          {images.length > 0 ? (
            <img
              src={images[0]}
              alt={property.title ?? "Imagen del inmueble"}
              className="h-64 w-full object-cover"
            />
          ) : (
            <div className="h-64 w-full flex items-center justify-center text-sm text-gray-500">
              No hay imágenes disponibles
            </div>
          )}
        </div>

        {property.verified && (
          <div className="absolute top-4 left-4 bg-[#DDDBCB] text-[#008080] text-xs font-semibold px-3 py-1 rounded-full shadow">
            Verificado
          </div>
        )}

        {images.length > 1 && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2 bg-white/80 px-3 py-1 rounded-full">
            {images.map((_, index) => (
              <span
                key={index}
                className={`h-2 w-2 rounded-full ${index === 0 ? "bg-[#050505]" : "bg-[#050505]/40"}`}
              />
            ))}
          </div>
        )}
      </div>

      <div className="bg-white rounded-2xl shadow-md p-4 mb-6">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xl font-bold text-[#008080]">{property.priceMonthly ?? "Precio no disponible"}</span>
          <span className="text-xs bg-[#DDDBCB] text-[#050505] rounded-full px-2 py-1">⭐ {property.rating?.toFixed(1) ?? "4.7"}</span>
        </div>
        <h2 className="text-xl font-semibold mb-1">{property.title ?? "Sin título"}</h2>
        <p className="text-sm text-[#050505]/70 mb-3">{property.subtitle ?? "Ubicación aproximada"}</p>
        <p className="text-sm text-[#050505]/70 max-h-20 overflow-hidden">
          {property.description ?? "Descripción no disponible."}
        </p>
        <button className="btn btn-link px-0 text-[#008080] hover:text-[#006b6b]">Leer más</button>
      </div>

      <div className="bg-white rounded-2xl shadow-md p-4 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold">Quién vive aquí</h3>
          <span className="badge bg-[#DDDBCB] text-[#050505] border-0">{occupants.length} inquilinos</span>
        </div>
        {occupants.length === 0 ? (
          <p className="text-sm text-[#050505]/70">No hay inquilinos registrados todavía.</p>
        ) : (
          <div className="flex flex-wrap gap-3">
            {occupants.map((person) => (
              <div key={person.name} className="flex items-center gap-3 bg-[#DDDBCB] rounded-full px-3 py-2">
                <div className="h-10 w-10 rounded-full bg-white flex items-center justify-center text-sm font-semibold text-[#050505]">
                  {person.name.slice(0, 2).toUpperCase()}
                </div>
                <div className="text-sm">
                  <p className="font-medium">{person.name}</p>
                  <p className="text-xs text-[#050505]/70">{person.age ? `${person.age} años` : "Edad privada"}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="bg-white rounded-2xl shadow-md p-4 mb-6">
        <h3 className="text-lg font-semibold mb-4">Privacidad y Ubicación</h3>
        <div className="rounded-2xl overflow-hidden bg-[#DDDBCB] mb-4">
          <div className="h-40 w-full bg-[#DDDBCB] blur-[1px]" />
        </div>
        <div className="flex items-start gap-3 bg-[#DDDBCB] rounded-xl p-3 mb-4">
          <div className="mt-1">
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path strokeLinecap="round" strokeLinejoin="round" d="M7 11V8a5 5 0 0110 0v3m-9 0h8a2 2 0 012 2v5a2 2 0 01-2 2H8a2 2 0 01-2-2v-5a2 2 0 012-2z" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold">Datos sensibles bloqueados</p>
            <p className="text-xs text-[#050505]/70">Datos sensibles bloqueados hasta Match verificado</p>
          </div>
        </div>
        <div className="space-y-3">
          <div className={`flex items-center justify-between rounded-xl bg-white px-4 py-3 ${isPrivacyUnlocked ? "" : "opacity-60"}`}>
            <span className={`text-sm text-[#050505]/70 ${isPrivacyUnlocked ? "" : "blur-sm"}`}>Dirección exacta</span>
            <span className="text-xs text-[#050505]/70">Bloqueado</span>
          </div>
          <div className={`flex items-center justify-between rounded-xl bg-white px-4 py-3 ${isPrivacyUnlocked ? "" : "opacity-60"}`}>
            <span className={`text-sm text-[#050505]/70 ${isPrivacyUnlocked ? "" : "blur-sm"}`}>Teléfono</span>
            <span className="text-xs text-[#050505]/70">Bloqueado</span>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-md p-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold">Amenidades</h3>
          <span className="text-xs text-[#050505]/70">{amenities.length} disponibles</span>
        </div>
        {amenities.length === 0 ? (
          <p className="text-sm text-[#050505]/70">No hay amenidades registradas.</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {amenities.map((amenity) => (
              <span key={amenity} className="badge bg-[#DDDBCB] text-[#050505] border-0 text-sm py-3 px-4 rounded-full">
                {amenity}
              </span>
            ))}
          </div>
        )}
      </div>

      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-[#DDDBCB] shadow-[0_-8px_24px_rgba(0,0,0,0.08)]">
        <div className="max-w-4xl mx-auto px-4 py-3 flex items-center gap-3">
          <div className="flex-1">
            <p className="text-xs text-[#050505]/70">Total mensual</p>
            <p className="text-lg font-semibold text-[#008080]">{property.priceMonthly ?? "Precio no disponible"}</p>
          </div>
          <button onClick={handleLike} className="btn flex-1 h-12 text-base bg-[#008080] hover:bg-[#006b6b] text-white border-0 rounded-2xl shadow-lg w-full">
            Solicitar Like
          </button>
        </div>
      </div>
    </section>
  )
}
