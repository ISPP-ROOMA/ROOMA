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
  const [roommates, setRoommates] = useState<
    Array<{
      user: User
      role: string
      joinDate?: string
    }> | null
  >(null)

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

    void profile()
  }, [])

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Cargando...</p>
  if (!userData) return <p className="text-center mt-10 text-red-500">Error al cargar el perfil</p>

  const initial = userData.email ? userData.email[0].toUpperCase() : '?'
  const roleLabel = ROLE_LABELS[userData.role] || userData.role

  return (
    <section className="min-h-[70vh] bg-base-200 px-3 py-4 sm:px-4 sm:py-6">
      <div className="mx-auto w-full max-w-lg space-y-4">
        <article className="rounded-2xl bg-base-100 shadow-md p-5 sm:p-6 text-center">
          <div className="mx-auto mb-3 w-20 h-20 sm:w-24 sm:h-24 rounded-full bg-primary text-white flex items-center justify-center text-2xl sm:text-3xl font-bold">
            {initial}
          </div>
          <p className="text-base sm:text-lg font-semibold break-all">{userData.email}</p>
          <div className="badge badge-primary badge-outline mt-2">{roleLabel}</div>
        </article>

        {currentApartment ? (
          <article className="rounded-2xl bg-base-100 shadow-md p-4 sm:p-5 space-y-4">
            <div>
              <p className="text-xs uppercase tracking-wide text-base-content/60">Vives en</p>
              <h2 className="mt-1 text-lg sm:text-xl font-semibold leading-tight break-words">
                {currentApartment.title}
              </h2>
              <p className="mt-1 text-sm text-base-content/70">{currentApartment.ubication}</p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <Link
                to={
                  userData.role === 'LANDLORD'
                    ? `/apartments/${currentApartment.id}`
                    : `/properties/${currentApartment.id}`
                }
                className="btn btn-primary w-full"
              >
                Ver detalle
              </Link>
              <Link to="/invoices" className="btn btn-outline w-full">
                Facturas
              </Link>
            </div>

            <div>
              <div className="flex items-center justify-between">
                <h3 className="font-semibold">Compañeros de piso</h3>
                <span className="text-xs text-base-content/60">{roommates?.length ?? 0} personas</span>
              </div>
              {roommates && roommates.length > 0 ? (
                <ul className="mt-3 space-y-2.5">
                  {roommates.map((r) => (
                    <li
                      key={r.user.id}
                      className="rounded-xl border border-base-200 px-3 py-2 flex items-center gap-3"
                    >
                      <div className="w-9 h-9 rounded-full bg-primary text-white flex items-center justify-center text-sm font-semibold shrink-0">
                        {r.user.email ? r.user.email[0].toUpperCase() : '?'}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium truncate">{r.user.email}</p>
                        <p className="text-xs text-base-content/60">{r.role}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="mt-2 text-sm text-base-content/60">No hay compañeros de piso registrados.</p>
              )}
            </div>
          </article>
        ) : (
          <article className="rounded-2xl bg-base-100 shadow-md p-5 text-sm text-base-content/70">
            No estás viviendo en ningún apartamento registrado.
          </article>
        )}

        <div className="pt-1">
          <Link to="/" className="link link-hover text-sm">
            {'<-'} Volver al inicio
          </Link>
        </div>
      </div>
    </section>
  )
}
