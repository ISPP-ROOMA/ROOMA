import {
  ArrowLeft,
  ChevronRight,
  Clock,
  Crown,
  Eye,
  EyeOff,
  MessageSquare,
  Star,
  Users as UsersIcon,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'
import {
  type PendingReviewApartment,
  type ReviewDTO,
  getMadeReviews,
  getPendingReviewApartments,
  getReceivedReviews,
} from '../../service/review.service'

type Tab = 'received' | 'made' | 'pending'

function StarDisplay({ rating }: { rating: number }) {
  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map((star) => (
        <Star
          key={star}
          size={16}
          strokeWidth={2}
          className={star <= rating ? 'fill-[#F59E0B] text-[#F59E0B]' : 'text-[#D1D5DB]'}
        />
      ))}
    </div>
  )
}

function ReviewCard({ review, type }: { review: ReviewDTO; type: Tab }) {
  const personEmail = type === 'received' ? review.reviewerEmail : review.reviewedEmail
  const personName = personEmail.split('@')[0]
  const date = new Date(review.reviewDate).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })

  const commentText = review.comment?.split('\n\n[Desglose:')[0] ?? ''

  return (
    <div className="rounded-2xl border border-[#E5E7EB] bg-white px-5 py-4">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <p className="text-[1.05rem] font-semibold text-[#111827]">
            {type === 'received' ? `De: ${personName}` : `A: ${personName}`}
          </p>
          <p className="mt-0.5 text-[0.85rem] text-[#9CA3AF]">{date}</p>
        </div>
        <StarDisplay rating={review.rating} />
      </div>

      {/* Comment */}
      {commentText && (
        <p className="mt-3 text-[0.95rem] leading-relaxed text-[#374151]">{commentText}</p>
      )}

      {/* Published status */}
      <div className="mt-3 flex items-center gap-2">
        {review.published ? (
          <span className="flex items-center gap-1.5 text-[0.85rem] font-medium text-[#0C8A80]">
            <Eye size={14} />
            Publicada
          </span>
        ) : (
          <span className="flex items-center gap-1.5 text-[0.85rem] font-medium text-[#F59E0B]">
            <EyeOff size={14} />
            Pendiente — visible cuando ambas partes valoren o tras 30 días
          </span>
        )}
      </div>
    </div>
  )
}

export default function MyReviews() {
  const navigate = useNavigate()
  const { userId } = useAuthStore()
  const numericUserId = Number(userId)

  const [tab, setTab] = useState<Tab>('received')
  const [receivedReviews, setReceivedReviews] = useState<ReviewDTO[]>([])
  const [madeReviews, setMadeReviews] = useState<ReviewDTO[]>([])
  const [pendingApartments, setPendingApartments] = useState<PendingReviewApartment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!numericUserId) return
    Promise.all([
      getReceivedReviews(numericUserId),
      getMadeReviews(numericUserId),
      getPendingReviewApartments(),
    ])
      .then(([received, made, pending]) => {
        setReceivedReviews(received)
        setMadeReviews(made)
        setPendingApartments(pending)
      })
      .finally(() => setLoading(false))
  }, [numericUserId])

  const reviews = tab === 'received' ? receivedReviews : madeReviews

  if (loading) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-[#F5F1E3]">
        <span className="loading loading-dots loading-lg text-[#0C8A80]"></span>
      </div>
    )
  }

  return (
    <div
      data-theme="light"
      className="mx-auto min-h-dvh w-full max-w-md bg-[#F5F1E3] text-[#1E293B]"
    >
      <div className="px-6 pt-8 pb-10">
        {/* Header */}
        <header className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="flex h-8 w-8 items-center justify-center text-[#0F172A]"
            aria-label="Volver"
          >
            <ArrowLeft size={24} strokeWidth={2.5} />
          </button>
          <h1 className="text-[1.75rem] font-extrabold leading-none tracking-tight text-[#111827]">
            Mis Valoraciones
          </h1>
        </header>

        {/* Tabs */}
        <div className="mt-6 flex rounded-2xl border border-[#E5E7EB] bg-white p-1">
          <button
            onClick={() => setTab('received')}
            className={`flex-1 rounded-xl py-3 text-[0.85rem] font-semibold transition-colors ${
              tab === 'received'
                ? 'bg-[#0C8A80] text-white shadow-sm'
                : 'text-[#6B7280] hover:text-[#111827]'
            }`}
          >
            Recibidas ({receivedReviews.length})
          </button>
          <button
            onClick={() => setTab('made')}
            className={`flex-1 rounded-xl py-3 text-[0.85rem] font-semibold transition-colors ${
              tab === 'made'
                ? 'bg-[#0C8A80] text-white shadow-sm'
                : 'text-[#6B7280] hover:text-[#111827]'
            }`}
          >
            Realizadas ({madeReviews.length})
          </button>
          <button
            onClick={() => setTab('pending')}
            className={`relative flex-1 rounded-xl py-3 text-[0.85rem] font-semibold transition-colors ${
              tab === 'pending'
                ? 'bg-[#0C8A80] text-white shadow-sm'
                : 'text-[#6B7280] hover:text-[#111827]'
            }`}
          >
            Pendientes
            {pendingApartments.length > 0 && (
              <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-[#F59E0B] text-[0.7rem] font-bold text-white">
                {pendingApartments.reduce((acc, a) => acc + a.pendingUsers.length, 0)}
              </span>
            )}
          </button>
        </div>

        {/* Content */}
        <div className="mt-6 space-y-3">
          {tab === 'pending' ? (
            pendingApartments.length === 0 ? (
              <div className="mt-12 flex flex-col items-center text-center">
                <div className="grid h-16 w-16 place-items-center rounded-full bg-[#F0EDE0]">
                  <Star size={28} className="text-[#0C8A80]" />
                </div>
                <p className="mt-4 text-[1.1rem] font-semibold text-[#111827]">¡Todo valorado!</p>
                <p className="mt-1 text-[0.9rem] text-[#6B7280]">
                  No tienes valoraciones pendientes.
                </p>
              </div>
            ) : (
              pendingApartments.map((apt) => (
                <div
                  key={apt.apartmentId}
                  className="rounded-2xl border border-[#E5E7EB] bg-white px-5 py-4"
                >
                  <p className="text-[1.05rem] font-semibold text-[#111827]">
                    {apt.apartmentTitle}
                  </p>
                  <p className="mt-0.5 text-[0.85rem] text-[#9CA3AF]">{apt.apartmentUbication}</p>
                  <div className="mt-3 space-y-2">
                    {apt.pendingUsers.map((user) => (
                      <button
                        key={user.id}
                        onClick={() => navigate(`/reviews/new/${apt.apartmentId}/form/${user.id}`)}
                        className="flex w-full items-center gap-3 rounded-xl border border-[#E5E7EB] bg-[#FAFAF7] px-4 py-3 text-left transition-colors hover:border-[#0C8A80]/30 hover:bg-[#F0EDE0]"
                      >
                        <div className="grid h-9 w-9 place-items-center rounded-full bg-[#0C8A80]/10">
                          {user.role === 'LANDLORD' ? (
                            <Crown size={16} className="text-[#0C8A80]" />
                          ) : (
                            <UsersIcon size={16} className="text-[#0C8A80]" />
                          )}
                        </div>
                        <div className="flex-1">
                          <p className="text-[0.95rem] font-medium text-[#111827]">
                            {user.email.split('@')[0]}
                          </p>
                          <p className="text-[0.8rem] text-[#9CA3AF]">
                            {user.role === 'LANDLORD' ? 'Propietario' : 'Compañero/a'}
                          </p>
                        </div>
                        <ChevronRight size={18} className="text-[#9CA3AF]" />
                      </button>
                    ))}
                  </div>
                </div>
              ))
            )
          ) : reviews.length === 0 ? (
            <div className="mt-12 flex flex-col items-center text-center">
              <div className="grid h-16 w-16 place-items-center rounded-full bg-[#F0EDE0]">
                {tab === 'received' ? (
                  <MessageSquare size={28} className="text-[#9CA3AF]" />
                ) : (
                  <Clock size={28} className="text-[#9CA3AF]" />
                )}
              </div>
              <p className="mt-4 text-[1.1rem] text-[#6B7280]">
                {tab === 'received'
                  ? 'Aún no has recibido valoraciones.'
                  : 'Aún no has realizado valoraciones.'}
              </p>
            </div>
          ) : (
            reviews.map((review) => <ReviewCard key={review.id} review={review} type={tab} />)
          )}
        </div>
      </div>
    </div>
  )
}
