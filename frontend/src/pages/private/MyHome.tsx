import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getUserProfile, type User } from '../../service/users.service'
import {
  getMyHomeSnapshot,
  type ApartmentHomeDTO,
  type RoommateDTO,
} from '../../service/apartment.service'

const currencyFormatter = new Intl.NumberFormat('es-ES', {
  style: 'currency',
  currency: 'EUR',
  minimumFractionDigits: 2,
})

const dateFormatter = new Intl.DateTimeFormat('es-ES', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
})

const formatCurrency = (value?: number) => currencyFormatter.format(Number(value ?? 0))

const formatDate = (value?: string) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return dateFormatter.format(date)
}

function RoommateAvatar({ roommate }: { roommate: RoommateDTO }) {
  const initial = roommate.email ? roommate.email[0]?.toUpperCase() : '?'

  if (roommate.profileImageUrl) {
    return (
      <img
        src={roommate.profileImageUrl}
        alt={roommate.email}
        className="w-12 h-12 rounded-full object-cover"
      />
    )
  }

  return (
    <div className="w-12 h-12 rounded-full bg-primary text-white flex items-center justify-center text-lg font-semibold">
      {initial}
    </div>
  )
}

export default function MyHome() {
  const [userData, setUserData] = useState<User | null>(null)
  const [homeData, setHomeData] = useState<ApartmentHomeDTO | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedPhoto, setSelectedPhoto] = useState(0)

  useEffect(() => {
    const load = async () => {
      try {
        const profile = await getUserProfile()
        setUserData(profile ?? null)

        const snapshot = await getMyHomeSnapshot()
        if (!snapshot) {
          setHomeData(null)
          setError('Aún no tienes un piso asignado en Rooma.')
        } else {
          setHomeData(snapshot)
          setError(null)
        }
      } catch (e) {
        console.error(e)
        setError('No se pudo cargar la información del piso.')
      } finally {
        setIsLoading(false)
      }
    }

    void load()
  }, [])

  useEffect(() => {
    setSelectedPhoto(0)
  }, [homeData?.photos?.length])

  const galleryPhotos = useMemo(() => {
    if (!homeData?.photos?.length) {
      return homeData?.apartment?.coverImageUrl
        ? [
            {
              id: -1,
              url: homeData.apartment.coverImageUrl,
              publicId: 'cover',
              orden: 0,
              portada: true,
            },
          ]
        : []
    }
    return homeData.photos
  }, [homeData])

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Cargando tu piso...</p>
  if (error && !homeData) return <p className="text-center mt-10 text-red-500">{error}</p>
  if (!homeData)
    return (
      <p className="text-center mt-10 text-gray-600">
        No estás asignado a ningún piso en el sistema.
      </p>
    )

  const { apartment, roommates = [], billing } = homeData
  const currentUserMembership = roommates.find((mate) => mate.currentUser)
  const otherRoommates = roommates.filter((mate) => !mate.currentUser)
  const heroImage = galleryPhotos[selectedPhoto]?.url ?? ''
  const pendingAmount = Number(billing?.pendingAmount ?? 0)

  return (
    <section className="bg-base-200 min-h-[70vh] py-8 px-4">
      <div className="max-w-6xl mx-auto space-y-6">
        <header className="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-2">
          <p className="text-sm uppercase tracking-wide text-primary/80">Mi piso</p>
          <h1 className="text-3xl font-semibold leading-tight">{apartment.title}</h1>
          <p className="text-base text-base-content/70">{apartment.ubication}</p>
          <div className="flex flex-wrap gap-4 text-sm text-base-content/80">
            <span className="badge badge-outline">{apartment.state}</span>
            {userData && (
              <span>
                Rol en Rooma: <strong>{userData.role}</strong>
              </span>
            )}
            {currentUserMembership && (
              <span>Miembro desde {formatDate(currentUserMembership.joinDate)}</span>
            )}
            <span>{roommates.length} personas viviendo aquí</span>
          </div>
        </header>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-6">
            <article className="bg-white rounded-xl shadow overflow-hidden">
              <div className="w-full h-80 bg-base-200">
                {heroImage ? (
                  <img
                    src={heroImage}
                    alt={apartment.title}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-base-content/50">
                    Añade fotos para este piso
                  </div>
                )}
              </div>
              {galleryPhotos.length > 1 && (
                <div className="px-4 py-3 border-t flex gap-3 overflow-x-auto">
                  {galleryPhotos.map((photo, index) => (
                    <button
                      key={photo.id}
                      onClick={() => setSelectedPhoto(index)}
                      className={`w-20 h-16 rounded-md overflow-hidden border transition ${index === selectedPhoto ? 'border-primary' : 'border-base-200'}`}
                    >
                      <img
                        src={photo.url}
                        alt={`Foto ${index + 1}`}
                        className="w-full h-full object-cover"
                      />
                    </button>
                  ))}
                </div>
              )}
            </article>

            <article className="bg-white rounded-xl shadow p-6 space-y-6">
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <p className="text-sm text-base-content/60">Precio mensual</p>
                  <p className="text-3xl font-semibold">{formatCurrency(apartment.price)}</p>
                </div>
                <div>
                  <p className="text-sm text-base-content/60">Gastos incluidos</p>
                  <p className="text-lg font-medium">{apartment.bills || 'No especificado'}</p>
                </div>
                <div>
                  <p className="text-sm text-base-content/60">Estado</p>
                  <p className="text-lg font-medium">{apartment.state}</p>
                </div>
                <div>
                  <p className="text-sm text-base-content/60">Personas viviendo</p>
                  <p className="text-lg font-medium">{roommates.length}</p>
                </div>
              </div>
              <div>
                <p className="text-sm text-base-content/60 mb-1">Descripción</p>
                <p className="text-base leading-relaxed text-base-content/80">
                  {apartment.description ||
                    'El propietario todavía no ha añadido una descripción detallada.'}
                </p>
              </div>
            </article>
          </div>

          <div className="space-y-6">
            <article className="bg-white rounded-xl shadow p-5 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold">Resumen de facturas</h3>
                  <p className="text-sm text-base-content/60">Controla tus próximos pagos</p>
                </div>
                <Link to="/invoices" className="btn btn-sm btn-primary">
                  Ver facturas
                </Link>
              </div>
              <div className="rounded-lg bg-base-200/60 p-4 space-y-3">
                <div>
                  <p className="text-sm text-base-content/60">Pendiente de pago</p>
                  <p className="text-2xl font-semibold">{formatCurrency(pendingAmount)}</p>
                </div>
                <div className="flex flex-col text-sm">
                  <span className="text-base-content/60">Próximo vencimiento</span>
                  <span className="font-medium">{formatDate(billing?.nextDueDate)}</span>
                  <span className="text-xs text-base-content/50">
                    Ref. {billing?.nextReference ?? '—'}
                  </span>
                </div>
                <div className="text-sm">
                  <span className="text-base-content/60">Deudas abiertas</span>
                  <p className="font-medium">{billing?.pendingDebts ?? 0}</p>
                </div>
              </div>
            </article>

            <article className="bg-white rounded-xl shadow p-5">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Personas con las que vivo</h3>
                <span className="text-xs text-base-content/60">{roommates.length} integrantes</span>
              </div>
              {roommates.length ? (
                <ul className="space-y-4">
                  {roommates.map((roommate) => (
                    <li key={roommate.memberId} className="flex items-start gap-3">
                      <RoommateAvatar roommate={roommate} />
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="font-semibold">{roommate.email}</p>
                            <p className="text-xs text-base-content/60">
                              {roommate.memberRole}
                              {roommate.currentUser ? ' · Tú' : ''}
                            </p>
                          </div>
                          <span className="text-xs text-base-content/50">
                            {formatDate(roommate.joinDate)}
                          </span>
                        </div>
                        {(roommate.profession || roommate.hobbies || roommate.schedule) && (
                          <p className="text-xs text-base-content/70 mt-1">
                            {roommate.profession || roommate.hobbies || roommate.schedule}
                          </p>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-base-content/60">
                  No hay compañeros registrados todavía.
                </p>
              )}
              {otherRoommates.length === 0 && (
                <p className="text-xs text-base-content/50 mt-3">
                  Comparte el enlace de Rooma para invitar a tus compañeros.
                </p>
              )}
            </article>
          </div>
        </div>

        <div className="flex items-center justify-between text-sm text-base-content/60 pb-6">
          <Link to="/" className="link">
            ← Volver al inicio
          </Link>
          <span>Última actualización {formatDate(new Date().toISOString())}</span>
        </div>
      </div>
    </section>
  )
}
