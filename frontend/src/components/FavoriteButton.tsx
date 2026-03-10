import { Heart } from 'lucide-react'
import { type MouseEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useToast } from '../hooks/useToast'
import { addFavorite, removeFavorite } from '../service/favorites.service'
import { useAuthStore } from '../store/authStore'

interface FavoriteButtonProps {
  apartmentId: number
  initialIsFavorite?: boolean
  disabled?: boolean
  onChange?: (isFavorite: boolean) => void
  className?: string
}

export default function FavoriteButton({
  apartmentId,
  initialIsFavorite = false,
  disabled = false,
  onChange,
  className,
}: FavoriteButtonProps) {
  const { token } = useAuthStore()
  const navigate = useNavigate()
  const { showToast } = useToast()

  const [isFavorite, setIsFavorite] = useState(initialIsFavorite)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    setIsFavorite(initialIsFavorite)
  }, [initialIsFavorite])

  const handleClick = async (event: MouseEvent<HTMLButtonElement>) => {
    event.preventDefault()
    event.stopPropagation()

    if (disabled || isSubmitting) {
      return
    }

    if (!token) {
      navigate('/login')
      return
    }

    try {
      setIsSubmitting(true)
      if (isFavorite) {
        const response = await removeFavorite(apartmentId)
        setIsFavorite(response.isFavorite)
        onChange?.(response.isFavorite)
        if (!response.isFavorite && response.message) {
          showToast(response.message, 'info')
        }
      } else {
        const response = await addFavorite(apartmentId)
        setIsFavorite(response.isFavorite)
        onChange?.(response.isFavorite)
        if (response.isFavorite && response.message) {
          showToast(response.message, 'success')
        }
      }
    } catch (error) {
      console.error('Error toggling favorite', error)
      showToast('No se pudo actualizar favoritos. Inténtalo de nuevo.', 'error')
    } finally {
      setIsSubmitting(false)
    }
  }

  const ariaLabel = isFavorite ? 'Quitar de favoritos' : 'Añadir a favoritos'

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={disabled || isSubmitting}
      aria-label={ariaLabel}
      aria-pressed={isFavorite}
      className={`inline-flex items-center justify-center rounded-full p-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-base-100 ${isFavorite ? 'bg-primary text-primary-content' : 'bg-base-100/90 text-base-content/70 hover:bg-base-100'} ${disabled || isSubmitting ? 'opacity-60 cursor-not-allowed' : ''} ${className ?? ''}`}
    >
      <Heart size={18} className={isFavorite ? 'fill-current' : ''} aria-hidden="true" />
    </button>
  )
}
