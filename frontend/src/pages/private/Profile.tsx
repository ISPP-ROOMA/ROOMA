import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getUserProfile, getUser, deleteUser } from '../../service/users.service'
import type { User } from '../../service/users.service'
import { getAllApartments, type ApartmentDTO } from '../../service/apartment.service'
import { api } from '../../service/api'
import { useAuthStore } from '../../store/authStore'

const ROLE_LABELS: Record<string, string> = {
  LANDLORD: 'Propietario',
  TENANT: 'Inquilino',
  ADMIN: 'Administrador',
}

type RoommateWithMeta = {
  user?: User
  role: string
  joinDate?: string
  userId: number
}

export default function Profile() {
  const navigate = useNavigate()
  const [userData, setUserData] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [currentApartment, setCurrentApartment] = useState<ApartmentDTO | null>(null)
  const [roommates, setRoommates] = useState<Array<{
    user: User
    role: string
    joinDate?: string
  }> | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

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
              const usersWithMeta: RoommateWithMeta[] = await Promise.all(
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
                  const allResp = await api.get<User[]>('/users')
                  const allUsers = allResp.data
                  const mapped = usersWithMeta.map((x) => {
                    if (!x.user) {
                      const foundUser = allUsers.find((u) => Number(u.id) === Number(x.userId))
                      return { ...x, user: foundUser }
                    }
                    return x
                  })
                  setRoommates(
                    mapped.filter(
                      (x): x is { user: User; role: string; joinDate?: string; userId: number } =>
                        !!x.user && Number(x.user.id) !== Number(res.id)
                    )
                  )
                  return
                } catch (e) {
                  console.warn('Fallback fetch all users failed', e)
                }
              }

              setRoommates(
                usersWithMeta.filter(
                  (x): x is { user: User; role: string; joinDate?: string; userId: number } =>
                    !!x.user && Number(x.user.id) !== Number(res.id)
                )
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

  const initial =
    userData.name && userData.name.length > 0
      ? userData.name[0].toUpperCase()
      : userData.email
        ? userData.email[0].toUpperCase()
        : '?'
  const roleLabel = ROLE_LABELS[userData.role] || userData.role

  const handleDeleteAccount = async () => {
    const accepted = window.confirm(
      '¿Seguro que quieres eliminar tu cuenta? Esta acción es irreversible.'
    )
    if (!accepted) return

    setIsDeleting(true)
    const result = await deleteUser()
    setIsDeleting(false)
    if (result.success) {
      useAuthStore.getState().logout()
      navigate('/')
    } else {
      alert(result.message ?? 'Error eliminando la cuenta')
    }
  }

  return (
    <section className="min-h-[70vh] bg-[#F5F1E3] px-3 py-4 sm:px-4 sm:py-6">
      <div className="mx-auto w-full max-w-lg space-y-4">
        <article className="overflow-hidden rounded-3xl border border-[#E6E2D5] bg-white shadow-[0_10px_30px_rgba(17,24,39,0.08)]">
          <div className="h-16 bg-gradient-to-r from-[#0F9E97] via-[#21B2A9] to-[#8DD8CC]" />
          <div className="-mt-8 px-5 pb-6 text-center sm:px-6">
            <div className="mx-auto mb-3 h-20 w-20 overflow-hidden rounded-full border-4 border-white bg-[#0F9E97] text-2xl font-bold text-white shadow-sm sm:h-24 sm:w-24 sm:text-3xl">
              {userData.profilePic ? (
                <img src={userData.profilePic} alt="Perfil" className="h-full w-full object-cover" />
              ) : (
                <div className="grid h-full w-full place-items-center">{initial}</div>
              )}
            </div>
            <p className="text-base font-semibold text-[#0F172A] break-all sm:text-lg">
              {userData.name || userData.surname
                ? `${userData.name || ''} ${userData.surname || ''}`.trim()
                : userData.email}
            </p>
            <p className="mt-1 text-sm text-[#64748B] break-all">{userData.email}</p>
            <div className="mt-2 inline-flex rounded-full border border-[#A2E0D8] bg-[#E6F7F5] px-3 py-1 text-xs font-semibold text-[#0C8A80]">
              {roleLabel}
            </div>
            <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2">
              <Link
                to="/profile/edit"
                className="btn h-11 w-full rounded-xl border border-[#0F9E97] bg-white px-4 text-sm font-semibold text-[#0F9E97] hover:bg-[#E6F7F5]"
              >
                Editar Perfil
              </Link>
              <button
                className="btn h-11 w-full rounded-xl border border-[#F5C2C7] bg-white px-4 text-sm font-semibold text-[#C2414D] hover:bg-[#FFF1F2]"
                onClick={() => void handleDeleteAccount()}
                disabled={isDeleting}
              >
                {isDeleting ? 'Eliminando...' : 'Eliminar Cuenta'}
              </button>
            </div>
          </div>
        </article>

        <article className="rounded-3xl border border-[#E6E2D5] bg-white p-4 shadow-[0_10px_30px_rgba(17,24,39,0.08)] sm:p-5">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <p className="text-[11px] uppercase tracking-[0.14em] text-[#8B7355]">Teléfono</p>
              <p className="mt-1 text-sm font-medium text-[#0F172A]">{userData.phone || 'No especificado'}</p>
            </div>
            <div>
              <p className="text-[11px] uppercase tracking-[0.14em] text-[#8B7355]">Fecha de nacimiento</p>
              <p className="mt-1 text-sm font-medium text-[#0F172A]">
                {userData.birthDate ? new Date(userData.birthDate).toLocaleDateString() : 'No especificada'}
              </p>
            </div>
            <div>
              <p className="text-[11px] uppercase tracking-[0.14em] text-[#8B7355]">Género</p>
              <p className="mt-1 text-sm font-medium text-[#0F172A]">{userData.gender || 'No especificado'}</p>
            </div>
            <div>
              <p className="text-[11px] uppercase tracking-[0.14em] text-[#8B7355]">Fumador</p>
              <p className="mt-1 text-sm font-medium text-[#0F172A]">
                {userData.smoker === true ? 'Sí' : userData.smoker === false ? 'No' : 'No especificado'}
              </p>
            </div>
            <div>
              <p className="text-[11px] uppercase tracking-[0.14em] text-[#8B7355]">Profesión</p>
              <p className="mt-1 text-sm font-medium text-[#0F172A]">{userData.profession || 'No especificada'}</p>
            </div>
            <div>
              <p className="text-[11px] uppercase tracking-[0.14em] text-[#8B7355]">Fecha de alta</p>
              <p className="mt-1 text-sm font-medium text-[#0F172A]">
                {userData.createdAt ? new Date(userData.createdAt).toLocaleDateString() : 'Desconocida'}
              </p>
            </div>
          </div>

          {(userData.hobbies || userData.schedule) && (
            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
              {userData.hobbies && (
                <div>
                  <p className="text-[11px] uppercase tracking-[0.14em] text-[#8B7355]">Hobbies</p>
                  <p className="mt-1 text-sm text-[#0F172A]">{userData.hobbies}</p>
                </div>
              )}
              {userData.schedule && (
                <div>
                  <p className="text-[11px] uppercase tracking-[0.14em] text-[#8B7355]">Horario / rutina</p>
                  <p className="mt-1 text-sm text-[#0F172A]">{userData.schedule}</p>
                </div>
              )}
            </div>
          )}
        </article>

        {currentApartment ? (
          <article className="rounded-3xl border border-[#E6E2D5] bg-white p-4 shadow-[0_10px_30px_rgba(17,24,39,0.08)] sm:p-5">
            <div className="rounded-2xl bg-[#F8F6EF] p-4">
              <p className="text-[11px] uppercase tracking-[0.14em] text-[#8B7355]">Vives en</p>
              <h2 className="mt-1 text-3xl font-bold leading-tight text-[#0F172A] break-words">
                {currentApartment.title}
              </h2>
              <p className="mt-1 text-base text-[#475569]">{currentApartment.ubication}</p>
            </div>

            <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
              <Link
                to={
                  userData.role === 'LANDLORD'
                    ? `/apartments/${currentApartment.id}`
                    : `/my-home`
                }
                className="btn h-12 w-full rounded-2xl border-none bg-gradient-to-r from-[#0F9E97] to-[#13B2A6] px-5 text-base font-semibold text-white shadow-[0_10px_22px_rgba(15,158,151,0.25)] hover:brightness-105"
              >
                Ver detalle
              </Link>
              <Link
                to="/invoices"
                className="btn h-12 w-full rounded-2xl border border-[#CFC8B8] bg-white px-5 text-base font-semibold text-[#0F172A] hover:border-[#0F9E97] hover:text-[#0C8A80]"
              >
                Facturas
              </Link>
            </div>

            <div className="mt-4">
              <div className="flex items-center justify-between">
                <h3 className="text-2xl font-semibold text-[#0F172A]">Compañeros de piso</h3>
                <span className="rounded-full bg-[#F1EEE3] px-2.5 py-1 text-xs font-medium text-[#7A6F5A]">
                  {roommates?.length ?? 0} personas
                </span>
              </div>

              {roommates && roommates.length > 0 ? (
                <ul className="mt-3 space-y-2.5">
                  {roommates.map((r) => (
                    <li
                      key={r.user.id}
                      className="flex items-center gap-3 rounded-2xl border border-[#ECE7D9] bg-[#FCFBF7] px-3 py-2.5"
                    >
                      <div className="grid h-10 w-10 place-items-center rounded-full bg-[#0F9E97] text-sm font-semibold text-white shrink-0">
                        {r.user.email ? r.user.email[0].toUpperCase() : '?'}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold text-[#0F172A]">
                          {r.user.email}
                        </p>
                        <p className="text-xs text-[#64748B]">{r.role}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="mt-2 rounded-xl border border-dashed border-[#D5CEBD] bg-[#FCFBF7] px-3 py-3 text-sm text-[#64748B]">
                  No hay compañeros de piso registrados.
                </p>
              )}
            </div>
          </article>
        ) : (
          <article className="rounded-3xl border border-[#E6E2D5] bg-white p-5 text-sm text-[#64748B] shadow-[0_10px_30px_rgba(17,24,39,0.08)]">
            No estás viviendo en ningún apartamento registrado.
          </article>
        )}

        <div className="pt-1">
          <Link
            to="/"
            className="text-sm font-medium text-[#0C8A80] underline-offset-2 hover:underline"
          >
            {'<-'} Volver al inicio
          </Link>
        </div>
      </div>
    </section>
  )
}
