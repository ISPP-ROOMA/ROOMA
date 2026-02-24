import { useNavigate } from 'react-router-dom'

const CARD_CLASS = 'bg-base-100 rounded-3xl shadow-md overflow-hidden flex flex-col'
const IMAGE_WRAPPER_CLASS = 'relative h-48 w-full bg-gray-200'
const IMAGE_FALLBACK_CLASS = 'h-full w-full flex items-center justify-center bg-gray-100'
const STATUS_BADGE_CLASS =
  'absolute top-3 left-3 flex items-center gap-1.5 bg-white/90 backdrop-blur px-3 py-1 rounded-full text-xs font-semibold text-gray-700 shadow-sm'
const PHOTO_BADGE_CLASS =
  'absolute bottom-3 right-3 flex items-center gap-1 bg-black/50 backdrop-blur px-2.5 py-1 rounded-full text-xs text-white'
const CONTENT_CLASS = 'flex flex-col gap-3 p-5 flex-1'
const ACTION_BUTTON_CLASS =
  'flex items-center gap-1.5 px-4 py-2 rounded-full border border-gray-300 text-sm text-gray-600 hover:bg-gray-100 transition'
const VIEW_BUTTON_CLASS =
  'ml-auto flex items-center justify-center w-10 h-10 rounded-full border border-gray-300 text-gray-600 hover:bg-gray-100 transition'

const STATUS_CONFIG = {
  active: { label: 'ACTIVO', dotClass: 'bg-emerald-500' },
  paused: { label: 'PAUSADO', dotClass: 'bg-gray-400' },
} as const

export interface PropertyCardProps {
  id: number
  title: string
  price: number
  currency?: string
  period?: string
  imageUrl?: string
  photoCount: number
  status: 'active' | 'paused'
  stats: {
    requests: number
    matches: number
  }
  onEdit?: () => void
  onPause?: () => void
}

export default function PropertyCard({
  id,
  title,
  price,
  currency = '€',
  period = 'mes',
  imageUrl,
  photoCount,
  status,
  stats,
  onEdit,
  onPause,
}: PropertyCardProps) {
  const navigate = useNavigate()
  const { label: statusLabel, dotClass: statusDotClass } = STATUS_CONFIG[status]

  const pauseLabel = status === 'active' ? 'Pausar' : 'Reanudar'
  const goToDetail = () => navigate(`/apartments/${id}`)

  return (
    <div className={CARD_CLASS}>
      <div className={IMAGE_WRAPPER_CLASS}>
        {imageUrl ? (
          <img src={imageUrl} alt={title} className="h-full w-full object-cover" />
        ) : (
          <div className={IMAGE_FALLBACK_CLASS}>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-16 w-16 text-gray-300"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M3 11.5L12 4l9 7.5V20a1 1 0 01-1 1h-5v-6H9v6H4a1 1 0 01-1-1v-8.5z"
              />
            </svg>
          </div>
        )}

        <span className={STATUS_BADGE_CLASS}>
          <span className={`inline-block h-2 w-2 rounded-full ${statusDotClass}`} />
          {statusLabel}
        </span>

        <span className={PHOTO_BADGE_CLASS}>
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-3.5 w-3.5"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M3 7h2l2-3h10l2 3h2a1 1 0 011 1v11a1 1 0 01-1 1H3a1 1 0 01-1-1V8a1 1 0 011-1z"
            />
            <circle cx="12" cy="13" r="4" />
          </svg>
          {photoCount}
        </span>
      </div>

      <div className={CONTENT_CLASS}>
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-bold text-base leading-snug line-clamp-2 text-base-content">
            {title}
          </h3>
          <button className="shrink-0 p-1 rounded-full hover:bg-black/5 transition">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5 text-gray-500"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <circle cx="10" cy="4" r="1.5" />
              <circle cx="10" cy="10" r="1.5" />
              <circle cx="10" cy="16" r="1.5" />
            </svg>
          </button>
        </div>

        <div className="flex items-baseline gap-1">
          <span className="text-2xl font-extrabold text-primary">
            {price} {currency}
          </span>
          <span className="text-sm text-gray-400">/ {period}</span>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="bg-base-200 rounded-2xl flex flex-col items-center justify-center py-4">
            <span className="text-2xl font-bold text-base-content">{stats.requests}</span>
            <span className="text-xs opacity-60 mt-0.5">Solicitudes</span>
          </div>
          <div className="bg-base-200 rounded-2xl flex flex-col items-center justify-center py-4">
            <span className="text-2xl font-bold text-base-content">{stats.matches}</span>
            <span className="text-xs opacity-60 mt-0.5">Matches</span>
          </div>
        </div>

        <div className="flex items-center gap-2 mt-auto pt-2">
          <button onClick={onEdit} className={ACTION_BUTTON_CLASS}>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-4 w-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15.232 5.232l3.536 3.536M9 13l6.586-6.586a2 2 0 112.828 2.828L11.828 15.828a2 2 0 01-1.414.586H7v-3.414a2 2 0 01.586-1.414z"
              />
            </svg>
            Editar
          </button>

          <button onClick={onPause} className={ACTION_BUTTON_CLASS}>
            {status === 'active' ? (
              <>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M10 9v6m4-6v6"
                  />
                </svg>
                {pauseLabel}
              </>
            ) : (
              <>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M14.752 11.168l-6.518-3.768A1 1 0 007 8.268v7.464a1 1 0 001.234.968l6.518-3.768a1 1 0 000-1.764z"
                  />
                </svg>
                {pauseLabel}
              </>
            )}
          </button>

          <button onClick={goToDetail} className={VIEW_BUTTON_CLASS} title="Ver detalle">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
              />
            </svg>
          </button>
        </div>
      </div>
    </div>
  )
}
