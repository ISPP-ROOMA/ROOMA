import { ArrowLeft, CheckCircle, ChevronRight, Crown, Star, Users } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  type ApartmentInfo,
  type ReviewableUser,
  getApartmentInfo,
  getReviewableUsers,
} from '../../service/review.service'

export default function SelectReviewTarget() {
  const navigate = useNavigate()
  const { contractId } = useParams()
  const apartmentId = Number(contractId)

  const [apartment, setApartment] = useState<ApartmentInfo | null>(null)
  const [reviewableUsers, setReviewableUsers] = useState<ReviewableUser[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!apartmentId) return
    Promise.all([getApartmentInfo(apartmentId), getReviewableUsers(apartmentId)])
      .then(([apt, users]) => {
        setApartment(apt)
        setReviewableUsers(users)
      })
      .finally(() => setLoading(false))
  }, [apartmentId])

  if (loading) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-[#F5F1E3]">
        <span className="loading loading-dots loading-lg text-[#0C8A80]"></span>
      </div>
    )
  }

  const allReviewed = reviewableUsers.length === 0

  return (
    <div
      data-theme="light"
      className="mx-auto min-h-dvh w-full max-w-md bg-[#F5F1E3] text-[#1E293B]"
    >
      <div className="px-6 pt-8 pb-10">
        {/* Header */}
        <header className="flex items-center gap-3">
          <button
            onClick={() => navigate(`/reviews/new/${contractId}`)}
            className="flex h-8 w-8 items-center justify-center text-[#0F172A]"
            aria-label="Volver"
          >
            <ArrowLeft size={24} strokeWidth={2.5} />
          </button>
          <h1 className="text-[1.75rem] font-extrabold leading-none tracking-tight text-[#111827]">
            ¿A quién valorar?
          </h1>
        </header>

        {/* Apartment card */}
        <div className="mt-6 rounded-2xl border border-[#E5E7EB] bg-white/70 px-5 py-4">
          <p className="text-[1rem] text-[#6B7280]">Piso</p>
          <p className="text-[1.2rem] font-bold text-[#111827]">{apartment?.title}</p>
          <p className="mt-1 text-[1rem] text-[#6B7280]">{apartment?.ubication}</p>
        </div>

        {/* User list */}
        {allReviewed ? (
          <div className="mt-12 flex flex-col items-center text-center">
            <div className="grid h-20 w-20 place-items-center rounded-full bg-[#D6E4DC]">
              <CheckCircle size={36} className="text-[#0C8A80]" />
            </div>
            <h2 className="mt-6 text-[1.6rem] font-extrabold text-[#111827]">¡Todo valorado!</h2>
            <p className="mt-3 max-w-[280px] text-[1.1rem] leading-relaxed text-[#6B7280]">
              Ya has valorado a todas las personas de este piso. Gracias por tu colaboración.
            </p>
            <button
              onClick={() => navigate('/')}
              className="mt-8 h-14 w-full rounded-[16px] bg-[#0C8A80] text-[1.3rem] font-bold text-white shadow-md"
            >
              Volver al inicio
            </button>
          </div>
        ) : (
          <section className="mt-8">
            <h3 className="text-[1.15rem] font-bold text-[#1F2937]">
              Selecciona a quien quieres valorar
            </h3>
            <div className="mt-4 space-y-3">
              {reviewableUsers.map((user) => {
                const isLandlord = user.role === 'LANDLORD'
                const displayName = user.email.split('@')[0]

                return (
                  <button
                    key={user.id}
                    onClick={() => navigate(`/reviews/new/${contractId}/form/${user.id}`)}
                    className="flex w-full items-center gap-4 rounded-2xl border border-[#E5E7EB] bg-white px-5 py-4 text-left transition-shadow hover:shadow-md active:scale-[0.98]"
                  >
                    {/* Avatar */}
                    <div
                      className={`grid h-12 w-12 flex-none place-items-center rounded-full ${
                        isLandlord ? 'bg-amber-100 text-amber-600' : 'bg-teal-100 text-teal-600'
                      }`}
                    >
                      {isLandlord ? (
                        <Crown size={22} strokeWidth={2.25} />
                      ) : (
                        <Users size={22} strokeWidth={2.25} />
                      )}
                    </div>

                    {/* Info */}
                    <div className="flex-1 min-w-0">
                      <p className="truncate text-[1.1rem] font-semibold text-[#111827]">
                        {displayName}
                      </p>
                      <p className="text-[0.9rem] text-[#6B7280]">
                        {isLandlord ? 'Propietario' : 'Compañero de piso'}
                      </p>
                    </div>

                    {/* Arrow + stars hint */}
                    <div className="flex items-center gap-1 text-[#9CA3AF]">
                      <Star size={16} className="text-[#D1D5DB]" />
                      <ChevronRight size={20} />
                    </div>
                  </button>
                )
              })}
            </div>
          </section>
        )}
      </div>
    </div>
  )
}
