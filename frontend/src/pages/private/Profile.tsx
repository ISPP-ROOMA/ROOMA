import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getUserProfile, deleteUser } from '../../service/users.service'
import type { User } from '../../service/users.service'
import {
  getAllApartments,
  type ApartmentDTO,
  type ApartmentMemberDTO,
} from '../../service/apartment.service'
import { useAuthStore } from '../../store/authStore'

const ROLE_LABELS: Record<string, string> = {
  LANDLORD: 'Propietario',
  TENANT: 'Inquilino',
  ADMIN: 'Administrador',
}

export default function Profile() {
  const navigate = useNavigate()
  const [userData, setUserData] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [currentApartment, setCurrentApartment] = useState<ApartmentDTO | null>(null)
  const [roommates, setRoommates] = useState<ApartmentMemberDTO[]>([])
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
            setRoommates(found.members.filter((m) => Number(m.userId) !== Number(res.id)))
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

  const initial =
    userData.name && userData.name.length > 0
      ? userData.name[0].toUpperCase()
      : userData.email
        ? userData.email[0].toUpperCase()
        : '?'
  const roleLabel = ROLE_LABELS[userData.role] || userData.role

  const handleDeleteAccount = async () => {
    setIsDeleting(true)
    const success = await deleteUser()
    setIsDeleting(false)
    if (success) {
      useAuthStore.getState().logout()
      navigate('/')
    } else {
      alert('Error eliminando la cuenta')
    }
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] p-4">
      <div className="card w-full max-w-2xl bg-base-100 shadow-lg">
        <div className="card-body">
          <div className="flex flex-col md:flex-row items-center md:items-start gap-6 border-b pb-6">
            <div className="avatar">
              <div className="w-24 h-24 rounded-full bg-primary text-white flex items-center justify-center text-3xl font-bold overflow-hidden">
                {userData.profilePic ? (
                  <img
                    src={userData.profilePic}
                    alt="Perfil"
                    className="object-cover w-full h-full"
                  />
                ) : (
                  initial
                )}
              </div>
            </div>

            <div className="flex-1 text-center md:text-left">
              <h2 className="text-2xl font-bold">
                {userData.name || userData.surname
                  ? `${userData.name || ''} ${userData.surname || ''}`.trim()
                  : 'Usuario sin nombre'}
              </h2>
              <p className="text-gray-500">{userData.email}</p>
              <div className="badge badge-primary badge-outline mt-2">{roleLabel}</div>
            </div>

            <div className="flex flex-col gap-2 mt-4 md:mt-0 w-full md:w-auto">
              <Link to="/profile/edit" className="btn btn-sm btn-outline btn-primary w-full">
                Editar Perfil
              </Link>
              <button
                className="btn btn-sm btn-outline btn-error w-full"
                onClick={() => {
                  ;(document.getElementById('delete_modal') as HTMLDialogElement).showModal()
                }}
              >
                Eliminar Cuenta
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
            <div>
              <span className="text-xs text-gray-500 font-semibold uppercase">Teléfono</span>
              <p className="font-medium">{userData.phone || 'No especificado'}</p>
            </div>
            <div>
              <span className="text-xs text-gray-500 font-semibold uppercase">
                Fecha de Nacimiento
              </span>
              <p className="font-medium">
                {userData.birthDate
                  ? new Date(userData.birthDate).toLocaleDateString()
                  : 'No especificada'}
              </p>
            </div>
            <div>
              <span className="text-xs text-gray-500 font-semibold uppercase">Género</span>
              <p className="font-medium">{userData.gender || 'No especificado'}</p>
            </div>
            <div>
              <span className="text-xs text-gray-500 font-semibold uppercase">Fumador</span>
              <p className="font-medium">
                {userData.smoker === true
                  ? 'Sí'
                  : userData.smoker === false
                    ? 'No'
                    : 'No especificado'}
              </p>
            </div>
            <div>
              <span className="text-xs text-gray-500 font-semibold uppercase">Profesión</span>
              <p className="font-medium">{userData.profession || 'No especificada'}</p>
            </div>
            <div>
              <span className="text-xs text-gray-500 font-semibold uppercase">Fecha de Alta</span>
              <p className="font-medium">
                {userData.createdAt
                  ? new Date(userData.createdAt).toLocaleDateString()
                  : 'Desconocida'}
              </p>
            </div>
          </div>

          {(userData.hobbies || userData.schedule) && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
              {userData.hobbies && (
                <div>
                  <span className="text-xs text-gray-500 font-semibold uppercase">Hobbies</span>
                  <p className="font-medium">{userData.hobbies}</p>
                </div>
              )}
              {userData.schedule && (
                <div>
                  <span className="text-xs text-gray-500 font-semibold uppercase">
                    Horario / Rutina
                  </span>
                  <p className="font-medium">{userData.schedule}</p>
                </div>
              )}
            </div>
          )}

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
                        : `/my-home`
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
                  {roommates.length > 0 ? (
                    <ul className="mt-2 space-y-2 text-sm">
                      {roommates.map((r) => (
                        <li key={r.userId} className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center text-sm font-semibold">
                            {r.userEmail ? r.userEmail[0].toUpperCase() : '?'}
                          </div>
                          <div className="flex flex-col">
                            <span className="font-medium">{r.userEmail}</span>
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

          <div className="flex flex-col items-center gap-2 mt-6 w-full">
            <Link to="/" className="link">
              ← Volver al inicio
            </Link>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      <dialog id="delete_modal" className="modal modal-bottom sm:modal-middle">
        <div className="modal-box">
          <h3 className="font-bold text-lg text-error">Relevante: ¡Acción irreversible!</h3>
          <p className="py-4">
            ¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer y
            perderás todo el acceso y tus datos asociados.
          </p>
          <div className="modal-action">
            <form method="dialog" className="flex gap-2">
              <button className="btn">Cancelar</button>
              <button
                className="btn btn-error"
                onClick={(e) => {
                  e.preventDefault()
                  void handleDeleteAccount()
                }}
                disabled={isDeleting}
              >
                {isDeleting ? 'Eliminando...' : 'Sí, eliminar cuenta'}
              </button>
            </form>
          </div>
        </div>
        <form method="dialog" className="modal-backdrop">
          <button>close</button>
        </form>
      </dialog>
    </div>
  )
}
