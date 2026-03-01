import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getUserProfile, getUser } from '../../service/users.service'
import type { User } from '../../service/users.service'
import { getAllApartments, getApartmentPhotos, type ApartmentDTO, type ApartmentPhotoDTO, type ApartmentMemberDTO } from '../../service/apartment.service'
import { api } from '../../service/api'

export default function MyHome() {
  const [userData, setUserData] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [currentApartment, setCurrentApartment] = useState<ApartmentDTO | null>(null)
  const [roommates, setRoommates] = useState<Array<{ user: User; role: string; joinDate?: string }> | null>(null)
  const [photos, setPhotos] = useState<ApartmentPhotoDTO[]>([])
  const [selectedPhoto, setSelectedPhoto] = useState<number>(0)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await getUserProfile()
        setUserData(res ?? null)

        if (res && res.id) {
          const apartments = await getAllApartments()
          const found = apartments.find((a) =>
            !!a.members && a.members.some((m) => m.userId === Number(res.id))
          )
          setCurrentApartment(found ?? null)

          if (found && found.members && found.members.length) {
            // Map members to include user details and joinDate
            const members: ApartmentMemberDTO[] = found.members

            try {
              console.debug('Apartment members from API:', members)
              const usersWithMeta = await Promise.all(
                members.map(async (m) => {
                  const u = await getUser(m.userId)
                  return { user: u as User | undefined, role: m.role, joinDate: m.joinDate, userId: m.userId }
                })
              )
              console.debug('Fetched users for members:', usersWithMeta)

              // If some users are missing, try a fallback: request all users and map by id
              const missing = usersWithMeta.filter((x) => !x.user)
              if (missing.length > 0) {
                try {
                  const allResp = await api.get('/users')
                  const allUsers = allResp.data
                  const mapped = usersWithMeta.map((x) => {
                    if (!x.user) {
                      const foundUser = allUsers.find((u: any) => Number(u.id) === Number(x.userId))
                      return { ...x, user: foundUser }
                    }
                    return x
                  })
                  console.debug('Fallback mapped users:', mapped)
                  setRoommates(mapped.filter((x) => x.user && Number(x.user.id) !== Number(res.id)))
                  return
                } catch (e) {
                  console.warn('Fallback fetch all users failed', e)
                }
              }

              setRoommates(usersWithMeta.filter((x) => x.user && Number(x.user.id) !== Number(res.id)) as any)
            } catch (e) {
              console.error('Error fetching roommates', e)
              setRoommates([])
            }
          } else {
            setRoommates([])
          }
        }
      } catch (e) {
        console.error(e)
      } finally {
        setIsLoading(false)
      }
    }

    void load()
  }, [])

  useEffect(() => {
    if (!currentApartment) return

    const loadPhotos = async () => {
      try {
        const imgs = await getApartmentPhotos(currentApartment.id)
        setPhotos(imgs)
        setSelectedPhoto(0)
      } catch (e) {
        console.error('Error loading photos', e)
        setPhotos([])
      }
    }

    void loadPhotos()
  }, [currentApartment])

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Cargando...</p>
  if (!userData) return <p className="text-center mt-10 text-red-500">Error al cargar el perfil</p>

  return (
    <div className="flex flex-col items-center justify-start min-h-[70vh] p-6 bg-base-200">
      <div className="w-full max-w-5xl">
        <div className="bg-white rounded-lg shadow-md overflow-hidden">
          <div className="md:flex">
            <div className="md:w-2/3 p-4">
              {currentApartment ? (
                <>
                  <div className="rounded-md overflow-hidden bg-gray-100">
                    <img
                      src={photos && photos.length ? photos[selectedPhoto].url : currentApartment.coverImageUrl ?? ''}
                      alt={currentApartment.title}
                      className="w-full h-64 md:h-80 object-cover"
                    />
                  </div>

                  {photos && photos.length > 1 && (
                    <div className="mt-3 flex gap-2 overflow-x-auto">
                      {photos.map((p, idx) => (
                        <button
                          key={p.id}
                          onClick={() => setSelectedPhoto(idx)}
                          className={`w-20 h-14 rounded overflow-hidden border ${idx === selectedPhoto ? 'border-primary' : 'border-transparent'}`}
                        >
                          <img src={p.url} alt={`img-${idx}`} className="w-full h-full object-cover" />
                        </button>
                      ))}
                    </div>
                  )}

                  <div className="mt-4">
                    <h2 className="text-2xl font-semibold">{currentApartment.title}</h2>
                    <p className="text-sm text-gray-500">{currentApartment.ubication}</p>
                    <p className="mt-3 text-gray-700 leading-relaxed">{currentApartment.description}</p>

                    <div className="mt-4 flex flex-col sm:flex-row sm:items-center sm:gap-4 gap-2">
                      <span className="text-lg font-medium">€{currentApartment.price}</span>
                      <span className="badge badge-outline">{currentApartment.state}</span>
                      <span className="text-sm text-gray-500">{currentApartment.bills}</span>
                    </div>
                  </div>
                </>
              ) : (
                <p className="text-sm text-gray-600 mt-4">No estás asignado a ningún piso en el sistema.</p>
              )}
            </div>

            <aside className="md:w-1/3 p-4 border-l">
              <div className="mb-4">
                <Link to={`/invoices?apartmentId=${currentApartment?.id ?? ''}`} className="btn btn-primary btn-block">
                  Ver facturas
                </Link>
              </div>

              <div className="bg-base-100 p-3 rounded">
                <h4 className="font-semibold">Compañeros de piso</h4>
                {roommates && roommates.length > 0 ? (
                  <ul className="mt-3 space-y-3">
                    {roommates.map((r) => (
                      <li key={(r.user && r.user.id) || Math.random()} className="flex items-start gap-3">
                        <div className="w-12 h-12 rounded-full bg-primary text-white flex items-center justify-center font-semibold text-lg">
                          {r.user && r.user.email ? r.user.email[0].toUpperCase() : '?'}
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center justify-between">
                            <span className="font-medium">{r.user?.email}</span>
                            <span className="text-xs text-gray-400">{r.role}</span>
                          </div>
                          <div className="mt-1 text-xs text-gray-500">{r.user?.profession ?? r.user?.hobbies ?? ''}</div>
                          <div className="mt-1 text-xs text-gray-400">Ingreso: {r.joinDate ?? '-'}</div>
                        </div>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="mt-2 text-xs text-gray-500">No hay compañeros de piso registrados.</p>
                )}
              </div>
            </aside>
          </div>
          <div className="p-4 border-t">
            <Link to="/" className="link">
              ← Volver al inicio
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
