import { X } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { ApartmentDTO, UserDTO } from '../service/apartment.service'
import { api } from '../service/api'

interface ApartmentDetailModalProps {
  apartment: ApartmentDTO
  onClose: () => void
}

export default function ApartmentDetailModal({ apartment, onClose }: ApartmentDetailModalProps) {
  const [roommates, setRoommates] = useState<UserDTO[]>([])

  useEffect(() => {
    // Fetch user details for each member
    const fetchRoommates = async () => {
      if (!apartment.members) return

      try {
        const userPromises = apartment.members.map((member) =>
          api.get<UserDTO>(`/users/${member.userId}`)
        )
        const responses = await Promise.all(userPromises)
        setRoommates(responses.map((res) => res.data))
      } catch (error) {
        console.error('Error fetching roommate details', error)
      }
    }

    fetchRoommates()
  }, [apartment.members])

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60 backdrop-blur-sm p-0 sm:p-4">
      <div className="bg-base-100 w-full sm:max-w-2xl sm:rounded-3xl rounded-t-3xl h-[85vh] sm:h-auto sm:max-h-[90vh] overflow-hidden flex flex-col relative animate-in slide-in-from-bottom-full sm:slide-in-from-bottom-0 sm:zoom-in-95 duration-300">
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-10 btn btn-circle btn-sm btn-ghost bg-black/20 text-white hover:bg-black/40 backdrop-blur-md"
        >
          <X size={18} />
        </button>

        {/* Header Image */}
        <div className="relative w-full h-64 shrink-0 bg-base-300">
          <img
            src={
              apartment.imageUrl ||
              'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80'
            }
            alt="Apartment"
            className="w-full h-full object-cover"
          />
          <div className="absolute bottom-0 left-0 w-full p-6 flex items-end justify-between bg-gradient-to-t from-black/80 via-black/40 to-transparent">
            <div>
              <h2 className="text-3xl font-bold text-white">{apartment.title}</h2>
              <p className="text-white/90 font-medium">{apartment.ubication}</p>
            </div>
            <div className="text-right">
              <p className="text-3xl font-bold text-white">{apartment.price}€</p>
              <p className="text-white/80 text-sm">/mes</p>
            </div>
          </div>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-6">
          <section className="mb-8">
            <h3 className="text-xl font-semibold mb-3">Descripción</h3>
            <p className="text-base-content/80 whitespace-pre-wrap">{apartment.description}</p>
          </section>

          <section className="mb-8">
            <h3 className="text-xl font-semibold mb-3">Detalles</h3>
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-base-200 p-4 rounded-2xl">
                <span className="text-sm text-base-content/60 block mb-1">Gastos incluidos</span>
                <span className="font-medium">{apartment.bills || 'No especificado'}</span>
              </div>
              <div className="bg-base-200 p-4 rounded-2xl">
                <span className="text-sm text-base-content/60 block mb-1">Estado</span>
                <span className="font-medium">
                  <div className="badge badge-success gap-2">{apartment.state}</div>
                </span>
              </div>
            </div>
          </section>

          {/* Roommates Section */}
          <section>
            <h3 className="text-xl font-semibold mb-4">Compañeros actuales ({roommates.length})</h3>

            {roommates.length === 0 ? (
              <p className="text-base-content/60 italic">Aún no hay compañeros en este piso.</p>
            ) : (
              <div className="space-y-4">
                {roommates.map((user) => (
                  <div key={user.id} className="bg-base-200 p-5 rounded-3xl">
                    <div className="flex items-center gap-4 mb-3">
                      <div className="avatar placeholder">
                        <div className="bg-primary text-primary-content rounded-full w-12 shrink-0">
                          <span className="text-xl">{user.email.charAt(0).toUpperCase()}</span>
                        </div>
                      </div>
                      <div>
                        <p className="font-bold text-lg">{user.email.split('@')[0]}</p>
                        <p className="text-sm text-base-content/60 capitalize">{user.role}</p>
                      </div>
                    </div>

                    {/* User profile details */}
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mt-4">
                      {user.profession && (
                        <div>
                          <span className="text-xs text-base-content/50 uppercase tracking-wider font-semibold block mb-1">
                            Profesión
                          </span>
                          <span className="bg-base-100 px-3 py-1.5 rounded-xl text-sm inline-block w-full">
                            {user.profession}
                          </span>
                        </div>
                      )}
                      {user.schedule && (
                        <div>
                          <span className="text-xs text-base-content/50 uppercase tracking-wider font-semibold block mb-1">
                            Horario
                          </span>
                          <span className="bg-base-100 px-3 py-1.5 rounded-xl text-sm inline-block w-full">
                            {user.schedule}
                          </span>
                        </div>
                      )}
                      {user.hobbies && (
                        <div>
                          <span className="text-xs text-base-content/50 uppercase tracking-wider font-semibold block mb-1">
                            Gustos
                          </span>
                          <span className="bg-base-100 px-3 py-1.5 rounded-xl text-sm inline-block w-full">
                            {user.hobbies}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  )
}
