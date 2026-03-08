import {
  ArrowLeft,
  ChevronRight,
  Clock,
  Crown,
  Eye,
  EyeOff,
  MessageSquare,
  Reply,
  Send,
  Star,
  Users as UsersIcon,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  type PendingReviewApartment,
  type ReviewDTO,
  getMadeReviews,
  getPendingReviewApartments,
  getReceivedReviews,
  respondToReview,
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

function ReviewCard({
  review,
  type,
  onResponseSent,
}: {
  review: ReviewDTO
  type: Tab
  onResponseSent?: (reviewId: number, response: string) => void
}) {
  const personEmail = type === 'received' ? review.reviewerEmail : review.reviewedEmail
  const personName = personEmail.split('@')[0]
  const date = new Date(review.reviewDate).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })

  const commentText = review.comment?.split('\n\n[Desglose:')[0] ?? ''

  const [showResponseForm, setShowResponseForm] = useState(false)
  const [responseText, setResponseText] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const canRespond = type === 'received' && review.published && !review.response
  const hasResponse = review.response && review.response.trim().length > 0

  const handleSubmitResponse = async () => {
    if (!responseText.trim() || submitting) return
    setSubmitting(true)
    const result = await respondToReview(review.id, responseText.trim())
    setSubmitting(false)
    if (result) {
      setShowResponseForm(false)
      setResponseText('')
      onResponseSent?.(review.id, responseText.trim())
    }
  }

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

      {/* Response (if exists) */}
      {hasResponse && (
        <div className="mt-3 rounded-xl border border-[#E5E7EB] bg-[#FAFAF7] px-4 py-3">
          <p className="flex items-center gap-1.5 text-[0.8rem] font-semibold text-[#6B7280]">
            <Reply size={13} />
            {type === 'received' ? 'Tu respuesta' : 'Respuesta'}
          </p>
          <p className="mt-1 text-[0.9rem] leading-relaxed text-[#374151]">{review.response}</p>
        </div>
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

      {/* Respond button */}
      {canRespond && !showResponseForm && (
        <button
          onClick={() => setShowResponseForm(true)}
          className="mt-3 flex items-center gap-1.5 rounded-lg border border-[#0C8A80]/20 px-3 py-2 text-[0.85rem] font-medium text-[#0C8A80] transition-colors hover:bg-[#0C8A80]/5"
        >
          <Reply size={14} />
          Responder
        </button>
      )}

      {/* Response form */}
      {showResponseForm && (
        <div className="mt-3 space-y-2">
          <textarea
            value={responseText}
            onChange={(e) => setResponseText(e.target.value.slice(0, 500))}
            placeholder="Escribe tu respuesta..."
            rows={3}
            className="w-full resize-none rounded-xl border border-[#E5E7EB] bg-[#FAFAF7] px-4 py-3 text-[0.9rem] text-[#374151] placeholder-[#9CA3AF] outline-none transition-colors focus:border-[#0C8A80]"
          />
          <div className="flex items-center justify-between">
            <span className="text-[0.75rem] text-[#9CA3AF]">{responseText.length}/500</span>
            <div className="flex gap-2">
              <button
                onClick={() => {
                  setShowResponseForm(false)
                  setResponseText('')
                }}
                className="rounded-lg px-3 py-1.5 text-[0.85rem] font-medium text-[#6B7280] transition-colors hover:bg-[#F0EDE0]"
              >
                Cancelar
              </button>
              <button
                onClick={handleSubmitResponse}
                disabled={!responseText.trim() || submitting}
                className="flex items-center gap-1.5 rounded-lg bg-[#0C8A80] px-4 py-1.5 text-[0.85rem] font-medium text-white transition-colors hover:bg-[#0A7A71] disabled:opacity-50"
              >
                <Send size={13} />
                {submitting ? 'Enviando…' : 'Enviar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default function MyReviews() {
  const navigate = useNavigate()

  const [tab, setTab] = useState<Tab>('received')
  const [receivedReviews, setReceivedReviews] = useState<ReviewDTO[]>([])
  const [madeReviews, setMadeReviews] = useState<ReviewDTO[]>([])
  const [pendingApartments, setPendingApartments] = useState<PendingReviewApartment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getReceivedReviews(), getMadeReviews(), getPendingReviewApartments()])
      .then(([received, made, pending]) => {
        setReceivedReviews(received)
        setMadeReviews(made)
        setPendingApartments(pending)
      })
      .finally(() => setLoading(false))
  }, [])

  const handleResponseSent = (reviewId: number, response: string) => {
    setReceivedReviews((prev) => prev.map((r) => (r.id === reviewId ? { ...r, response } : r)))
  }

  const reviews = tab === 'received' ? receivedReviews : madeReviews

  const pendingCount = pendingApartments.reduce((acc, a) => acc + a.pendingUsers.length, 0)

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
      <div className="px-5 pt-7 pb-28 sm:px-6">
        {/* Header */}
        <header className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="flex h-8 w-8 items-center justify-center text-[#0F172A]"
            aria-label="Volver"
          >
            <ArrowLeft size={24} strokeWidth={2.5} />
          </button>
          <h1 className="text-[1.55rem] font-extrabold leading-none tracking-tight text-[#111827] sm:text-[1.75rem]">
            Mis Valoraciones
          </h1>
        </header>

        {/* Tabs */}
        <div className="mt-5 flex rounded-2xl border border-[#E5E7EB] bg-white p-1">
          <button
            onClick={() => setTab('received')}
            className={`flex-1 rounded-xl py-2.5 text-[0.78rem] font-semibold transition-colors sm:py-3 sm:text-[0.85rem] ${
              tab === 'received'
                ? 'bg-[#0C8A80] text-white shadow-sm'
                : 'text-[#6B7280] hover:text-[#111827]'
            }`}
          >
            Recibidas ({receivedReviews.length})
          </button>
          <button
            onClick={() => setTab('made')}
            className={`flex-1 rounded-xl py-2.5 text-[0.78rem] font-semibold transition-colors sm:py-3 sm:text-[0.85rem] ${
              tab === 'made'
                ? 'bg-[#0C8A80] text-white shadow-sm'
                : 'text-[#6B7280] hover:text-[#111827]'
            }`}
          >
            Realizadas ({madeReviews.length})
          </button>
          <button
            onClick={() => setTab('pending')}
            className={`relative flex-1 rounded-xl py-2.5 text-[0.78rem] font-semibold transition-colors sm:py-3 sm:text-[0.85rem] ${
              tab === 'pending'
                ? 'bg-[#0C8A80] text-white shadow-sm'
                : 'text-[#6B7280] hover:text-[#111827]'
            }`}
          >
            Pendientes
            {pendingCount > 0 && (
              <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-[#F59E0B] text-[0.7rem] font-bold text-white">
                {pendingCount}
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
                    {apt.pendingUsers.map((user) =>
                      user.youReviewedThem ? (
                        /* Ya valoraste a esta persona — esperando su valoración */
                        <div
                          key={user.id}
                          className="flex w-full items-center gap-3 rounded-xl border border-[#F59E0B]/30 bg-[#FFFBEB] px-4 py-3"
                        >
                          <div className="grid h-9 w-9 place-items-center rounded-full bg-[#F59E0B]/10">
                            {user.role === 'LANDLORD' ? (
                              <Crown size={16} className="text-[#F59E0B]" />
                            ) : (
                              <UsersIcon size={16} className="text-[#F59E0B]" />
                            )}
                          </div>
                          <div className="flex-1">
                            <p className="text-[0.95rem] font-medium text-[#111827]">
                              {user.email.split('@')[0]}
                            </p>
                            <p className="text-[0.8rem] text-[#92400E]">
                              Tu valoración está esperando a ser revelada
                            </p>
                          </div>
                          <EyeOff size={16} className="text-[#F59E0B]" />
                        </div>
                      ) : (
                        /* Todavía no has valorado a esta persona */
                        <button
                          key={user.id}
                          onClick={() =>
                            navigate(`/reviews/new/${apt.apartmentId}/form/${user.id}`)
                          }
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
                              {user.hasReviewedYou
                                ? '¡Ya te ha valorado! Valóralo para revelar ambas'
                                : user.role === 'LANDLORD'
                                  ? 'Propietario'
                                  : 'Compañero/a'}
                            </p>
                          </div>
                          <ChevronRight size={18} className="text-[#9CA3AF]" />
                        </button>
                      )
                    )}
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
            reviews.map((review) => (
              <ReviewCard
                key={review.id}
                review={review}
                type={tab}
                onResponseSent={tab === 'received' ? handleResponseSent : undefined}
              />
            ))
          )}
        </div>
      </div>
    </div>
  )
}
