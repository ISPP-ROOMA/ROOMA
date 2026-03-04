import { ArrowLeft, Send, Star } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'
import {
  type ApartmentInfo,
  getApartmentInfo,
  submitReviewAsLandlord,
  submitReviewAsTenant,
} from '../../service/review.service'

const TENANT_CATEGORIES = [
  { key: 'kindness', label: 'Amabilidad' },
  { key: 'responsibility', label: 'Responsabilidad' },
  { key: 'communication', label: 'Comunicación' },
  { key: 'maintenance', label: 'Mantenimiento' },
  { key: 'value', label: 'Relación calidad-precio' },
]

const LANDLORD_CATEGORIES = [
  { key: 'cleanliness', label: 'Limpieza' },
  { key: 'respect', label: 'Respeto' },
  { key: 'payment', label: 'Puntualidad en pagos' },
  { key: 'communication', label: 'Comunicación' },
  { key: 'responsibility', label: 'Responsabilidad' },
]

const TENANT_TO_TENANT_CATEGORIES = [
  { key: 'cleanliness', label: 'Limpieza' },
  { key: 'respect', label: 'Respeto' },
  { key: 'communication', label: 'Comunicación' },
  { key: 'coexistence', label: 'Convivencia' },
  { key: 'responsibility', label: 'Responsabilidad' },
]

function StarRating({
  value,
  onChange,
  size = 28,
}: {
  value: number
  onChange: (v: number) => void
  size?: number
}) {
  const [hover, setHover] = useState(0)

  return (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          onMouseEnter={() => setHover(star)}
          onMouseLeave={() => setHover(0)}
          onClick={() => onChange(star)}
          className="transition-transform hover:scale-110"
        >
          <Star
            size={size}
            strokeWidth={2}
            className={
              star <= (hover || value) ? 'fill-[#F59E0B] text-[#F59E0B]' : 'text-[#D1D5DB]'
            }
          />
        </button>
      ))}
    </div>
  )
}

export default function LeaveReview() {
  const { contractId, userId: reviewedUserIdParam } = useParams()
  const navigate = useNavigate()
  const apartmentId = Number(contractId)
  const reviewedUserId = Number(reviewedUserIdParam)
  const { role } = useAuthStore()

  const [apartment, setApartment] = useState<ApartmentInfo | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [categoryRatings, setCategoryRatings] = useState<Record<string, number>>({})
  const [comment, setComment] = useState('')

  const isLandlord = role === 'LANDLORD'

  const isReviewingLandlord = apartment ? apartment.ownerId === reviewedUserId : false

  const categories = isLandlord
    ? LANDLORD_CATEGORIES
    : isReviewingLandlord
      ? TENANT_CATEGORIES
      : TENANT_TO_TENANT_CATEGORIES

  useEffect(() => {
    if (!apartmentId) return
    getApartmentInfo(apartmentId)
      .then((data) => setApartment(data))
      .finally(() => setLoading(false))
  }, [apartmentId])

  const getReviewedName = () => {
    if (!apartment) return 'esta persona'
    if (isReviewingLandlord) {
      return apartment.ownerEmail.split('@')[0]
    }
    if (isLandlord) {
      return 'tu inquilino/a'
    }
    return 'tu compañero/a'
  }

  const reviewedName = getReviewedName()

  const averageRating = () => {
    const vals = Object.values(categoryRatings).filter((v) => v > 0)
    if (vals.length === 0) return 0
    const avg = vals.reduce((a, b) => a + b, 0) / vals.length
    return Math.round(avg)
  }

  const handleSubmit = async () => {
    const allCategoriesFilled = categories.every((c) => (categoryRatings[c.key] || 0) > 0)
    if (!allCategoriesFilled) {
      setError('Por favor, valora todas las categorías.')
      return
    }
    const finalRating = averageRating()
    if (finalRating < 1) {
      setError('Por favor, selecciona al menos una valoración con estrellas.')
      return
    }
    if (!reviewedUserId) {
      setError('No se pudo identificar al usuario a valorar.')
      return
    }

    setSubmitting(true)
    setError(null)

    const categorySummary = categories
      .map((c) => `${c.label}: ${categoryRatings[c.key] || 0}/5`)
      .join(' | ')

    const fullComment = comment.trim()
      ? `${comment.trim()}\n\n[Desglose: ${categorySummary}]`
      : `[Desglose: ${categorySummary}]`

    try {
      const payload = {
        reviewedUserId,
        apartmentId,
        rating: finalRating,
        comment: fullComment.slice(0, 500),
      }

      if (isLandlord) {
        await submitReviewAsLandlord(payload)
      } else {
        await submitReviewAsTenant(payload)
      }

      setSubmitted(true)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al enviar la valoración.'
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-[#F5F1E3]">
        <span className="loading loading-dots loading-lg text-[#0C8A80]"></span>
      </div>
    )
  }

  if (submitted) {
    return (
      <div
        data-theme="light"
        className="mx-auto flex min-h-dvh w-full max-w-md flex-col items-center justify-center bg-[#F5F1E3] px-6 text-center"
      >
        <div className="grid h-24 w-24 place-items-center rounded-full bg-[#D6E4DC]">
          <Star size={40} className="fill-[#F59E0B] text-[#F59E0B]" />
        </div>
        <h2 className="mt-8 text-[2.2rem] font-extrabold text-[#111827]">¡Reseña Enviada!</h2>
        <p className="mt-4 text-[1.15rem] leading-relaxed text-[#6B7280]">
          Tu valoración de <span className="font-semibold text-[#1F2937]">{reviewedName}</span> en{' '}
          <span className="font-semibold text-[#1F2937]">{apartment?.title}</span> (
          {apartment?.ubication}) ha sido registrada.
        </p>
        <p className="mt-3 text-[1rem] text-[#0C8A80] font-medium">
          Se publicará cuando ambas partes hayan valorado o tras 30 días.
        </p>
        <button
          onClick={() => navigate(`/reviews/new/${contractId}/select`)}
          className="mt-10 h-14 w-full rounded-[16px] bg-[#0C8A80] text-[1.3rem] font-bold text-white shadow-md"
        >
          Valorar a otra persona
        </button>
        <button
          onClick={() => navigate('/')}
          className="mt-4 h-14 w-full rounded-[16px] border-2 border-[#0C8A80] bg-transparent text-[1.3rem] font-bold text-[#0C8A80]"
        >
          Volver al inicio
        </button>
      </div>
    )
  }

  const reviewTypeLabel = isLandlord
    ? 'Valora a tu inquilino'
    : isReviewingLandlord
      ? 'Valora a tu casero'
      : 'Valora a tu compañero/a'

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
            {reviewTypeLabel}
          </h1>
        </header>

        {/* Apartment info */}
        <div className="mt-6 rounded-2xl bg-white/70 px-5 py-4 border border-[#E5E7EB]">
          <p className="text-[1rem] text-[#6B7280]">Piso</p>
          <p className="text-[1.2rem] font-bold text-[#111827]">{apartment?.title}</p>
          <p className="text-[1rem] text-[#6B7280] mt-1">{apartment?.ubication}</p>
          <p className="text-[0.95rem] text-[#0C8A80] font-medium mt-2">
            Valorando a: {reviewedName}
          </p>
        </div>

        {/* Category ratings */}
        <section className="mt-8">
          <h3 className="text-[1.3rem] font-bold text-[#1F2937]">Valora por categorías</h3>
          <p className="mt-1 text-[0.85rem] text-[#9CA3AF]">
            La nota final será la media de todas las categorías (
            {averageRating() > 0 ? `${averageRating()}/5` : '—'})
          </p>
          <div className="mt-4 space-y-5">
            {categories.map((cat) => (
              <div key={cat.key} className="flex items-center justify-between">
                <span className="text-[1.05rem] text-[#374151]">{cat.label}</span>
                <StarRating
                  value={categoryRatings[cat.key] || 0}
                  onChange={(v) => setCategoryRatings((prev) => ({ ...prev, [cat.key]: v }))}
                  size={24}
                />
              </div>
            ))}
          </div>
        </section>

        {/* Comment */}
        <section className="mt-8">
          <h3 className="text-[1.3rem] font-bold text-[#1F2937]">Comentario</h3>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            maxLength={300}
            rows={4}
            placeholder="Comparte tu experiencia... (opcional)"
            className="mt-3 w-full rounded-2xl border border-[#D1D5DB] bg-white px-4 py-3 text-[1rem] text-[#1F2937] placeholder-[#9CA3AF] focus:border-[#0C8A80] focus:outline-none focus:ring-1 focus:ring-[#0C8A80]"
          />
          <p className="mt-1 text-right text-sm text-[#9CA3AF]">{comment.length}/300</p>
        </section>

        {/* Error */}
        {error && (
          <div className="mt-4 rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-[0.95rem] text-red-700">
            {error}
          </div>
        )}

        {/* Submit */}
        <button
          onClick={handleSubmit}
          disabled={submitting}
          className="mt-8 flex h-14 w-full items-center justify-center gap-3 rounded-[18px] bg-[#0C8A80] text-[1.4rem] font-extrabold text-white shadow-[0_6px_14px_rgba(12,138,128,0.3)] disabled:opacity-50"
        >
          {submitting ? (
            <span className="loading loading-spinner loading-md"></span>
          ) : (
            <>
              Enviar Valoración
              <Send size={20} strokeWidth={2.5} />
            </>
          )}
        </button>
      </div>
    </div>
  )
}
