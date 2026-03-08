import { motion, useAnimation, AnimatePresence } from 'framer-motion'
import {
  Banknote,
  Briefcase,
  ChevronLeft,
  ChevronRight,
  Clock,
  Heart,
  MapPin,
  Receipt,
  Shield,
  Users,
  X,
  ZoomIn,
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { ApartmentDTO, ApartmentPhotoDTO, UserDTO } from '../service/apartment.service'
import { getApartmentPhotos } from '../service/apartment.service'
import { api } from '../service/api'

interface ApartmentDetailModalProps {
  apartment: ApartmentDTO
  onClose: () => void
}

const FALLBACK_IMG =
  'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80'

const STATE_LABELS: Record<string, { label: string; cls: string }> = {
  ACTIVE: { label: 'Disponible', cls: 'badge-success' },
  MATCHING: { label: 'En proceso', cls: 'badge-warning' },
  CLOSED: { label: 'Cerrado', cls: 'badge-error' },
}

/* ───────────────────────────────────────────────────────────────
   Swipeable Image Gallery (touch + mouse drag + arrow keys)
   ─────────────────────────────────────────────────────────────── */
function SwipeGallery({
  images,
  currentIndex,
  onIndexChange,
  onOpenLightbox,
  height,
  rounded,
}: {
  images: { url: string; id?: number }[]
  currentIndex: number
  onIndexChange: (i: number) => void
  onOpenLightbox?: () => void
  height: string
  rounded?: string
}) {
  const containerRef = useRef<HTMLDivElement>(null)
  const dragStartX = useRef(0)
  const dragStartY = useRef(0)
  const dragging = useRef(false)
  const dragDelta = useRef(0)
  const dirLock = useRef<'h' | 'v' | null>(null)
  const [offset, setOffset] = useState(0)
  const [containerWidth, setContainerWidth] = useState(400)

  useEffect(() => {
    const measure = () => {
      if (containerRef.current?.offsetWidth) {
        setContainerWidth(containerRef.current.offsetWidth)
      }
    }

    measure()
    window.addEventListener('resize', measure)
    return () => window.removeEventListener('resize', measure)
  }, [])

  const go = (dir: 1 | -1) => {
    const next = currentIndex + dir
    if (next < 0 || next >= images.length) return
    onIndexChange(next)
  }

  const handlePointerDown = (e: React.PointerEvent) => {
    if ((e.target as HTMLElement).closest('button')) return
    dragging.current = true
    dragStartX.current = e.clientX
    dragStartY.current = e.clientY
    dragDelta.current = 0
    dirLock.current = null
  }

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!dragging.current) return
    const dx = e.clientX - dragStartX.current
    const dy = e.clientY - dragStartY.current

    // Direction lock: decide on first significant movement
    if (dirLock.current === null) {
      if (Math.abs(dx) < 6 && Math.abs(dy) < 6) return
      dirLock.current = Math.abs(dx) >= Math.abs(dy) ? 'h' : 'v'
      if (dirLock.current === 'v') {
        dragging.current = false
        return
      }
      containerRef.current?.setPointerCapture(e.pointerId)
    }

    if (dirLock.current !== 'h') return

    dragDelta.current = dx
    if ((currentIndex === 0 && dx > 0) || (currentIndex === images.length - 1 && dx < 0)) {
      setOffset(dx * 0.25)
    } else {
      setOffset(dx)
    }
  }

  const handlePointerUp = () => {
    if (!dragging.current && dirLock.current !== 'h') {
      dirLock.current = null
      return
    }
    dragging.current = false
    if (dirLock.current === 'h') {
      const threshold = 50
      if (dragDelta.current < -threshold && currentIndex < images.length - 1) {
        go(1)
      } else if (dragDelta.current > threshold && currentIndex > 0) {
        go(-1)
      }
    }
    setOffset(0)
    dragDelta.current = 0
    dirLock.current = null
  }

  if (images.length === 0) {
    return (
      <div className={`relative w-full ${height} bg-base-300 ${rounded ?? ''} overflow-hidden`}>
        <img src={FALLBACK_IMG} alt="Placeholder" className="w-full h-full object-cover" />
      </div>
    )
  }

  return (
    <div
      ref={containerRef}
      className={`relative w-full ${height} ${rounded ?? ''} overflow-hidden touch-pan-y`}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerUp}
      style={{ cursor: images.length > 1 ? 'grab' : 'default' }}
    >
      {/* Sliding track */}
      <div
        className="absolute inset-y-0 flex will-change-transform"
        style={{
          width: `${images.length * 100}%`,
          transform: `translateX(calc(${-currentIndex * containerWidth}px + ${offset}px))`,
          transition: offset === 0 ? 'transform 0.35s cubic-bezier(.25,.8,.25,1)' : 'none',
        }}
      >
        {images.map((img, idx) => (
          <div
            key={img.id ?? idx}
            className="h-full shrink-0"
            style={{ width: `${containerWidth}px` }}
          >
            <img
              src={img.url}
              alt={`Foto ${idx + 1}`}
              className="w-full h-full object-cover pointer-events-none select-none"
              draggable={false}
            />
          </div>
        ))}
      </div>

      {/* Arrow buttons (desktop) */}
      {images.length > 1 && currentIndex > 0 && (
        <button
          onPointerDown={(e) => e.stopPropagation()}
          onClick={() => go(-1)}
          className="absolute left-3 top-1/2 -translate-y-1/2 z-10 btn btn-circle btn-sm bg-black/30 text-white hover:bg-black/50 backdrop-blur-md border-none hidden sm:flex"
        >
          <ChevronLeft size={18} />
        </button>
      )}
      {images.length > 1 && currentIndex < images.length - 1 && (
        <button
          onPointerDown={(e) => e.stopPropagation()}
          onClick={() => go(1)}
          className="absolute right-3 top-1/2 -translate-y-1/2 z-10 btn btn-circle btn-sm bg-black/30 text-white hover:bg-black/50 backdrop-blur-md border-none hidden sm:flex"
        >
          <ChevronRight size={18} />
        </button>
      )}

      {/* Dot indicators */}
      {images.length > 1 && images.length <= 12 && (
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 z-10 flex gap-1.5 bg-black/25 backdrop-blur-sm rounded-full px-2.5 py-1.5">
          {images.map((_, idx) => (
            <button
              key={idx}
              onPointerDown={(e) => e.stopPropagation()}
              onClick={() => onIndexChange(idx)}
              className={`rounded-full transition-all ${
                idx === currentIndex ? 'w-6 h-2 bg-white' : 'w-2 h-2 bg-white/50 hover:bg-white/70'
              }`}
            />
          ))}
        </div>
      )}

      {/* Counter badge (if many photos) */}
      {images.length > 12 && (
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 z-10 bg-black/40 backdrop-blur-md text-white text-xs font-semibold px-3 py-1.5 rounded-full">
          {currentIndex + 1} / {images.length}
        </div>
      )}

      {/* Zoom button */}
      {onOpenLightbox && (
        <button
          onPointerDown={(e) => e.stopPropagation()}
          onClick={onOpenLightbox}
          className="absolute top-3 right-3 z-10 btn btn-circle btn-sm bg-black/30 text-white hover:bg-black/50 backdrop-blur-md border-none"
        >
          <ZoomIn size={16} />
        </button>
      )}
    </div>
  )
}

/* ───────────────────────────────────────────────────────────────
   Fullscreen Lightbox with swipe + pinch-zoom feel
   ─────────────────────────────────────────────────────────────── */
function Lightbox({
  images,
  startIndex,
  onClose,
}: {
  images: ApartmentPhotoDTO[]
  startIndex: number
  onClose: () => void
}) {
  const [idx, setIdx] = useState(startIndex)
  const thumbsRef = useRef<HTMLDivElement>(null)

  // Keyboard navigation
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight') setIdx((p) => Math.min(p + 1, images.length - 1))
      else if (e.key === 'ArrowLeft') setIdx((p) => Math.max(p - 1, 0))
      else if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [images.length, onClose])

  // Auto-scroll thumbnail into view
  useEffect(() => {
    if (!thumbsRef.current) return
    const thumb = thumbsRef.current.children[idx] as HTMLElement | undefined
    thumb?.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' })
  }, [idx])

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.2 }}
      className="fixed inset-0 z-[60] bg-black flex flex-col"
    >
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 shrink-0">
        <span className="text-white/80 text-sm font-medium tabular-nums">
          {idx + 1} / {images.length}
        </span>
        <button
          onClick={onClose}
          className="btn btn-circle btn-sm btn-ghost text-white hover:bg-white/10"
        >
          <X size={20} />
        </button>
      </div>

      {/* Swipeable main image */}
      <div className="flex-1 min-h-0 flex items-center justify-center">
        <SwipeGallery images={images} currentIndex={idx} onIndexChange={setIdx} height="h-full" />
      </div>

      {/* Thumbnails */}
      {images.length > 1 && (
        <div
          ref={thumbsRef}
          className="shrink-0 flex gap-2 px-4 py-3 overflow-x-auto scrollbar-none justify-start sm:justify-center"
        >
          {images.map((photo, i) => (
            <button
              key={photo.id}
              onClick={() => setIdx(i)}
              className={`shrink-0 w-16 h-12 rounded-lg overflow-hidden transition-all ring-2 ${
                i === idx
                  ? 'ring-white scale-105 opacity-100'
                  : 'ring-transparent opacity-40 hover:opacity-70'
              }`}
            >
              <img
                src={photo.url}
                alt={`Miniatura ${i + 1}`}
                className="w-full h-full object-cover"
              />
            </button>
          ))}
        </div>
      )}
    </motion.div>
  )
}

/* ───────────────────────────────────────────────────────────────
   Main Modal
   ─────────────────────────────────────────────────────────────── */
export default function ApartmentDetailModal({ apartment, onClose }: ApartmentDetailModalProps) {
  const [roommates, setRoommates] = useState<UserDTO[]>([])
  const [photos, setPhotos] = useState<ApartmentPhotoDTO[]>([])
  const [currentPhoto, setCurrentPhoto] = useState(0)
  const [lightboxOpen, setLightboxOpen] = useState(false)

  // Animation controls
  const modalControls = useAnimation()
  const backdropControls = useAnimation()

  // Scroll container ref
  const scrollRef = useRef<HTMLDivElement>(null)

  // Touch overscroll-dismiss refs
  const touchStartY = useRef(0)
  const touchScrollTop = useRef(0)
  const pulling = useRef(false)
  const pullDelta = useRef(0)

  // Mouse drag-to-dismiss refs (handle bar)
  const dragStartY = useRef(0)
  const isDragging = useRef(false)
  const currentDragY = useRef(0)

  // ── Data fetching ──────────────────────────────────────────────

  useEffect(() => {
    const fetchRoommates = async () => {
      if (!apartment.members || apartment.members.length === 0) return
      try {
        const userPromises = apartment.members.map((m) => api.get<UserDTO>(`/users/${m.userId}`))
        const responses = await Promise.all(userPromises)
        setRoommates(responses.map((r) => r.data))
      } catch (err) {
        console.error('Error fetching roommate details', err)
      }
    }
    fetchRoommates()
  }, [apartment.members])

  useEffect(() => {
    const fetchPhotos = async () => {
      const imgs = await getApartmentPhotos(apartment.id)
      imgs.sort((a, b) => (a.orden ?? 0) - (b.orden ?? 0))
      setPhotos(imgs)
    }
    fetchPhotos()
  }, [apartment.id])

  // ── Animations ─────────────────────────────────────────────────

  useEffect(() => {
    modalControls.start({
      scale: 1,
      borderRadius: '0px',
      opacity: 1,
      transition: { type: 'spring', damping: 32, stiffness: 400, mass: 0.6 },
    })
    backdropControls.start({
      opacity: 1,
      transition: { duration: 0.2, ease: 'easeOut' },
    })
  }, [modalControls, backdropControls])

  const dismiss = useCallback(async () => {
    backdropControls.start({ opacity: 0, transition: { duration: 0.2, ease: 'easeOut' } })
    await modalControls.start({
      scale: 0.85,
      borderRadius: '24px',
      opacity: 0,
      transition: { duration: 0.25, ease: [0.32, 0.72, 0, 1] },
    })
    onClose()
  }, [modalControls, backdropControls, onClose])

  // ── Shared pull helpers ────────────────────────────────────────

  const applyPull = useCallback(
    (delta: number) => {
      const progress = Math.min(delta / 180, 1)
      modalControls.set({
        scale: 1 - progress * 0.18,
        borderRadius: `${progress * 24}px`,
        opacity: Math.max(0.2, 1 - progress * 0.8),
      })
      backdropControls.set({ opacity: Math.max(0, 1 - progress * 1.5) })
    },
    [modalControls, backdropControls]
  )

  const resetPull = useCallback(() => {
    modalControls.start({
      scale: 1,
      borderRadius: '0px',
      opacity: 1,
      transition: { type: 'spring', damping: 30, stiffness: 400 },
    })
    backdropControls.start({
      opacity: 1,
      transition: { type: 'spring', damping: 30, stiffness: 400 },
    })
  }, [modalControls, backdropControls])

  // ── Touch overscroll dismiss (mobile) ──────────────────────────

  useEffect(() => {
    const el = scrollRef.current
    if (!el) return

    const onTouchStart = (e: TouchEvent) => {
      touchStartY.current = e.touches[0].clientY
      touchScrollTop.current = el.scrollTop
      pulling.current = false
      pullDelta.current = 0
    }

    const onTouchMove = (e: TouchEvent) => {
      const dy = e.touches[0].clientY - touchStartY.current
      if (touchScrollTop.current <= 0 && dy > 0) {
        if (!pulling.current && dy > 8) pulling.current = true
        if (pulling.current) {
          e.preventDefault()
          pullDelta.current = dy
          applyPull(dy)
        }
      } else if (pulling.current) {
        pulling.current = false
        pullDelta.current = 0
        modalControls.set({ scale: 1, borderRadius: '0px', opacity: 1 })
        backdropControls.set({ opacity: 1 })
      }
    }

    const onTouchEnd = () => {
      if (pulling.current) {
        if (pullDelta.current > 60) dismiss()
        else resetPull()
      }
      pulling.current = false
      pullDelta.current = 0
    }

    el.addEventListener('touchstart', onTouchStart, { passive: true })
    el.addEventListener('touchmove', onTouchMove, { passive: false })
    el.addEventListener('touchend', onTouchEnd, { passive: true })

    return () => {
      el.removeEventListener('touchstart', onTouchStart)
      el.removeEventListener('touchmove', onTouchMove)
      el.removeEventListener('touchend', onTouchEnd)
    }
  }, [dismiss, applyPull, resetPull, modalControls, backdropControls])

  // ── Pointer drag-to-dismiss on handle (desktop) ───────────────

  const handlePointerDown = (e: React.PointerEvent) => {
    if ((e.target as HTMLElement).closest('button')) return
    dragStartY.current = e.clientY
    isDragging.current = true
    currentDragY.current = 0
    ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
  }

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!isDragging.current) return
    const delta = e.clientY - dragStartY.current
    if (delta <= 0) {
      if (currentDragY.current > 0) {
        currentDragY.current = 0
        modalControls.set({ scale: 1, borderRadius: '0px', opacity: 1 })
        backdropControls.set({ opacity: 1 })
      }
      return
    }
    currentDragY.current = delta
    applyPull(delta)
  }

  const handlePointerUp = () => {
    if (!isDragging.current) return
    isDragging.current = false
    if (currentDragY.current > 60) dismiss()
    else if (currentDragY.current > 0) resetPull()
    currentDragY.current = 0
  }

  const stateInfo = STATE_LABELS[apartment.state] ?? {
    label: apartment.state,
    cls: 'badge-ghost',
  }

  // Build image list: photos from API, or fallback to coverImageUrl
  const galleryImages: { url: string; id?: number }[] =
    photos.length > 0
      ? photos.map((p) => ({ url: p.url, id: p.id }))
      : apartment.coverImageUrl
        ? [{ url: apartment.coverImageUrl }]
        : [{ url: FALLBACK_IMG }]

  // ── Render ─────────────────────────────────────────────────────

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={backdropControls}
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={dismiss}
      />

      {/* Modal */}
      <motion.div
        initial={{ scale: 0.82, borderRadius: '24px', opacity: 0 }}
        animate={modalControls}
        className="absolute inset-0 sm:inset-4 lg:inset-8 bg-base-100 overflow-hidden will-change-transform origin-center sm:rounded-2xl shadow-2xl"
      >
        {/* Close button – always visible */}
        <button
          onClick={dismiss}
          className="absolute top-3 right-3 sm:top-4 sm:right-4 z-30 btn btn-circle btn-sm bg-black/30 text-white hover:bg-black/50 backdrop-blur-md border-none shadow-lg"
        >
          <X size={18} />
        </button>

        {/* ─── Single scroll container ─── */}
        <div ref={scrollRef} className="h-full overflow-y-auto overscroll-none">
          {/* Drag handle (desktop pull-to-dismiss) */}
          <div
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerCancel={handlePointerUp}
            className="flex justify-center pt-3 pb-1 cursor-grab active:cursor-grabbing select-none"
          >
            <div className="w-10 h-1.5 rounded-full bg-base-content/20" />
          </div>

          {/* ─── Hero Gallery ─── */}
          <div className="relative" data-gallery>
            <SwipeGallery
              images={galleryImages}
              currentIndex={currentPhoto}
              onIndexChange={setCurrentPhoto}
              onOpenLightbox={photos.length > 0 ? () => setLightboxOpen(true) : undefined}
              height="h-64 sm:h-80 md:h-[26rem]"
            />

            {/* Title + Price overlay */}
            <div className="absolute bottom-0 inset-x-0 px-5 pb-4 pt-16 bg-gradient-to-t from-black/80 via-black/40 to-transparent pointer-events-none z-[5]">
              <div className="flex items-end justify-between gap-4">
                <div className="min-w-0">
                  <h2 className="text-2xl sm:text-3xl font-bold text-white leading-tight truncate">
                    {apartment.title}
                  </h2>
                  <p className="flex items-center gap-1.5 text-white/90 text-sm mt-1">
                    <MapPin size={14} className="shrink-0" />
                    {apartment.ubication}
                  </p>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-2xl sm:text-3xl font-extrabold text-white">
                    {apartment.price}€
                  </p>
                  <p className="text-white/70 text-xs">/mes</p>
                </div>
              </div>
            </div>
          </div>

          {/* ─── Content ─── */}
          <div className="max-w-2xl mx-auto px-4 sm:px-5 py-6 space-y-8">
            {/* Quick stats */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="bg-base-200 rounded-2xl p-4 flex flex-col items-center text-center gap-1">
                <Banknote size={20} className="text-primary" />
                <span className="text-lg font-bold">{apartment.price}€</span>
                <span className="text-xs text-base-content/50">Precio/mes</span>
              </div>
              <div className="bg-base-200 rounded-2xl p-4 flex flex-col items-center text-center gap-1">
                <Receipt size={20} className="text-secondary" />
                <span className="text-sm font-semibold leading-tight">
                  {apartment.bills || '—'}
                </span>
                <span className="text-xs text-base-content/50">Gastos</span>
              </div>
              <div className="bg-base-200 rounded-2xl p-4 flex flex-col items-center text-center gap-1">
                <Shield size={20} className={stateInfo.cls.replace('badge-', 'text-')} />
                <span className={`badge ${stateInfo.cls} badge-sm font-semibold`}>
                  {stateInfo.label}
                </span>
                <span className="text-xs text-base-content/50">Estado</span>
              </div>
            </div>

            {/* Description */}
            <section>
              <h3 className="text-lg font-bold mb-2 flex items-center gap-2">
                <span className="w-1 h-5 bg-primary rounded-full" />
                Descripción
              </h3>
              <p className="text-base-content/75 leading-relaxed whitespace-pre-wrap">
                {apartment.description}
              </p>
            </section>

            {/* Details */}
            <section>
              <h3 className="text-lg font-bold mb-3 flex items-center gap-2">
                <span className="w-1 h-5 bg-primary rounded-full" />
                Información del piso
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <DetailCard
                  icon={<MapPin size={18} />}
                  label="Ubicación"
                  value={apartment.ubication}
                />
                <DetailCard
                  icon={<Banknote size={18} />}
                  label="Precio mensual"
                  value={`${apartment.price}€ / mes`}
                />
                <DetailCard
                  icon={<Receipt size={18} />}
                  label="Gastos incluidos"
                  value={apartment.bills || 'No especificado'}
                />
                <DetailCard
                  icon={<Shield size={18} />}
                  label="Estado"
                  value={stateInfo.label}
                  badge={stateInfo.cls}
                />
              </div>
            </section>

            {/* Photo gallery grid */}
            {photos.length > 0 && (
              <section>
                <h3 className="text-lg font-bold mb-3 flex items-center gap-2">
                  <span className="w-1 h-5 bg-primary rounded-full" />
                  Todas las fotos
                  <span className="text-sm font-normal text-base-content/50">
                    ({photos.length})
                  </span>
                </h3>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                  {photos.map((photo, idx) => (
                    <button
                      key={photo.id}
                      onClick={() => {
                        setCurrentPhoto(idx)
                        setLightboxOpen(true)
                      }}
                      className={`group relative aspect-[4/3] rounded-xl overflow-hidden ring-2 transition-all ${
                        idx === currentPhoto
                          ? 'ring-primary shadow-lg'
                          : 'ring-transparent hover:ring-primary/40'
                      }`}
                    >
                      <img
                        src={photo.url}
                        alt={`Foto ${idx + 1}`}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      />
                      <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors flex items-center justify-center">
                        <ZoomIn
                          size={20}
                          className="text-white opacity-0 group-hover:opacity-100 transition-opacity drop-shadow-lg"
                        />
                      </div>
                    </button>
                  ))}
                </div>
              </section>
            )}

            {/* Roommates */}
            <section className="pb-4">
              <h3 className="text-lg font-bold mb-3 flex items-center gap-2">
                <span className="w-1 h-5 bg-primary rounded-full" />
                <Users size={18} />
                Compañeros actuales
                <span className="text-sm font-normal text-base-content/50">
                  ({roommates.length})
                </span>
              </h3>

              {roommates.length === 0 ? (
                <div className="bg-base-200 rounded-2xl p-6 text-center">
                  <Users size={32} className="mx-auto text-base-content/30 mb-2" />
                  <p className="text-base-content/50">Aún no hay compañeros en este piso.</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {roommates.map((user) => (
                    <div
                      key={user.id}
                      className="bg-base-200 rounded-2xl p-4 hover:shadow-md transition-shadow"
                    >
                      <div className="flex items-center gap-3">
                        <div className="avatar placeholder">
                          <div className="bg-primary text-primary-content rounded-full w-11 h-11 shrink-0">
                            <span className="text-lg font-bold">
                              {user.email.charAt(0).toUpperCase()}
                            </span>
                          </div>
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="font-bold truncate">{user.email.split('@')[0]}</p>
                          <p className="text-xs text-base-content/50 capitalize">{user.role}</p>
                        </div>
                      </div>
                      {(user.profession || user.schedule || user.hobbies) && (
                        <div className="mt-3 pt-3 border-t border-base-300 grid grid-cols-1 sm:grid-cols-3 gap-2">
                          {user.profession && (
                            <RoommateTag
                              icon={<Briefcase size={13} />}
                              label="Profesión"
                              value={user.profession}
                            />
                          )}
                          {user.schedule && (
                            <RoommateTag
                              icon={<Clock size={13} />}
                              label="Horario"
                              value={user.schedule}
                            />
                          )}
                          {user.hobbies && (
                            <RoommateTag
                              icon={<Heart size={13} />}
                              label="Gustos"
                              value={user.hobbies}
                            />
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </section>
          </div>
        </div>
      </motion.div>

      {/* ─── Fullscreen Lightbox ─── */}
      <AnimatePresence>
        {lightboxOpen && photos.length > 0 && (
          <Lightbox
            images={photos}
            startIndex={currentPhoto}
            onClose={() => setLightboxOpen(false)}
          />
        )}
      </AnimatePresence>
    </div>
  )
}

/* ─── Sub-components ─────────────────────────────────────────── */

function DetailCard({
  icon,
  label,
  value,
  badge,
}: {
  icon: React.ReactNode
  label: string
  value: string
  badge?: string
}) {
  return (
    <div className="bg-base-200 rounded-2xl p-4 flex items-start gap-3">
      <div className="shrink-0 w-9 h-9 rounded-xl bg-base-300 flex items-center justify-center text-base-content/60">
        {icon}
      </div>
      <div className="min-w-0">
        <p className="text-xs text-base-content/50 font-medium uppercase tracking-wider">{label}</p>
        {badge ? (
          <span className={`badge ${badge} badge-sm mt-1 font-semibold`}>{value}</span>
        ) : (
          <p className="font-semibold text-sm mt-0.5 break-words">{value}</p>
        )}
      </div>
    </div>
  )
}

function RoommateTag({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode
  label: string
  value: string
}) {
  return (
    <div className="flex items-center gap-2 bg-base-100 px-3 py-2 rounded-xl">
      <span className="text-base-content/40 shrink-0">{icon}</span>
      <div className="min-w-0">
        <p className="text-[10px] uppercase tracking-wider text-base-content/40 font-semibold leading-none mb-0.5">
          {label}
        </p>
        <p className="text-sm truncate">{value}</p>
      </div>
    </div>
  )
}
