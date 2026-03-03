import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getUserProfile, getUser } from '../../service/users.service'
import type { User } from '../../service/users.service'
import { getAllApartments, type ApartmentDTO } from '../../service/apartment.service'
import { api } from '../../service/api'

const ROLE_LABELS: Record<string, string> = {
  LANDLORD: 'Propietario',
  TENANT: 'Inquilino',
  ADMIN: 'Administrador',
}

export default function Profile() {
  const [userData, setUserData] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [currentApartment, setCurrentApartment] = useState<ApartmentDTO | null>(null)
  const [roommates, setRoommates] = useState<Array<{
    user: User
    role: string
    joinDate?: string
  }> | null>(null)

  useEffect(() => {
    const profile = async () => {
      try {
        const res = await getUserProfile()
        setUserData(res || null)
        if (res && res.id) {
          const apartments = await getAllApartments()
          const found = apartments.find(
            (a) => !!a.members && a.members.some((m) => m.userId === Number(res.id))
          )
          setCurrentApartment(found ?? null)
          if (found && found.members && found.members.length) {
            const members = found.members

            try {
              console.debug('Apartment members from API:', members)
              const usersWithMeta = await Promise.all(
                members.map(async (m) => {
                  const u = await getUser(m.userId)
                  return {
                    user: u as User | undefined,
                    role: m.role,
                    joinDate: m.joinDate,
                    userId: m.userId,
                  }
                })
              )

              console.debug('Fetched users for members:', usersWithMeta)

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

              setRoommates(
                usersWithMeta.filter((x) => x.user && Number(x.user.id) !== Number(res.id)) as any
              )
            } catch (e) {
              console.error('Error fetching roommates', e)
              setRoommates([])
            }
          } else {
            setRoommates([])
          }
        }
      } catch (error) {
        console.error(error)
      } finally {
        setIsLoading(false)
      }
    }
    profile()
  }, [])

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Cargando...</p>

  if (!userData) return <p className="text-center mt-10 text-red-500">Error al cargar el perfil</p>

  const initial = userData.email ? userData.email[0].toUpperCase() : '?'
  const roleLabel = ROLE_LABELS[userData.role] || userData.role

  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] p-4">
      <div className="card w-full max-w-sm bg-base-100 shadow-lg">
        <div className="card-body items-center text-center">
          <div className="avatar">
            <div className="w-24 h-24 rounded-full bg-primary text-white flex items-center justify-center text-3xl font-bold">
              {initial}
            </div>
          </div>
          <p className="text-lg font-semibold">{userData.email}</p>
          <div className="badge badge-primary badge-outline mt-1">{roleLabel}</div>

          {currentApartment ? (
            <div className="w-full mt-4">
              <div className="card bg-base-200 p-3">
                <h4 className="font-semibold">Vives en:</h4>
                <p className="text-sm mt-1 font-medium">{currentApartment.title}</p>
                <p className="text-xs text-gray-500">{currentApartment.ubication}</p>
                <div className="mt-2">
                  <Link
                    to={
                      userData.role === 'LANDLORD'
                        ? `/apartments/${currentApartment.id}`
                        : `/properties/${currentApartment.id}`
                    }
                    className="btn btn-sm btn-primary"
                  >
                    Ver detalle
                  </Link>
                  <Link to="/invoices" className="btn btn-sm btn-outline">
                    Facturas
                  </Link>
                </div>
                <div className="mt-3">
                  <h5 className="font-medium">Compañeros de piso</h5>
                  {roommates && roommates.length > 0 ? (
                    <ul className="mt-2 space-y-2 text-sm">
                      {roommates.map((r) => (
                        <li key={r.user?.id} className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center text-sm font-semibold">
                            {r.user?.email ? r.user.email[0].toUpperCase() : '?'}
                          </div>
                          <div className="flex flex-col">
                            <span className="font-medium">{r.user?.email}</span>
                            <span className="text-xs text-gray-500">{r.role}</span>
                          </div>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="mt-2 text-xs text-gray-500">
                      No hay compañeros de piso registrados.
                    </p>
                  )}
                </div>
              </div>
            </div>
          ) : (
            <div className="w-full mt-4 text-sm text-gray-600">
              No estás viviendo en ningún apartamento registrado.
            </div>
          )}

          <div className="flex flex-col gap-2 mt-4 w-full">
            <Link to="/" className="link">
              ← Volver al inicio
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
