import {
  motion,
  useAnimation,
  useMotionValue,
  useTransform,
  type PanInfo,
} from 'framer-motion'
import { Check, MapPin, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { ApartmentDTO } from '../service/apartment.service'

interface SwipeableCardProps {
  apartment: ApartmentDTO
  onSwipe: (interest: boolean) => void
  onShowDetails: () => void
}

export default function SwipeableCard({ apartment, onSwipe, onShowDetails }: SwipeableCardProps) {
  /* ── Horizontal swipe (like / dislike) ── */
  const x = useMotionValue(0)
  const controls = useAnimation()
  const [exitX, setExitX] = useState<number | string>(0)

  const rotate = useTransform(x, [-200, 200], [-18, 18])
  const cardOpacity = useTransform(x, [-220, -100, 0, 100, 220], [0, 1, 1, 1, 0])

  // Green/red overlay driven by horizontal drag
  const likeOpacity = useTransform(x, [20, 120], [0, 1])
  const nopeOpacity = useTransform(x, [-20, -120], [0, 1])

  useEffect(() => {
    controls.start({ scale: 1, opacity: 1, x: 0, y: 0 })
  }, [controls])

  /*
  const handleDragStart = (_: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
    dragStartY.current = info.point.y
  }
  */

  const handleDragEnd = async (_: MouseEvent | TouchEvent | PointerEvent,
  info: PanInfo) => {
    const ox = info.offset.x
    const oy = info.offset.y

    // Swipe left / right (horizontal dominant)
    if (Math.abs(ox) > Math.abs(oy)) {
      if (ox > 90) {
        setExitX(1200)
        onSwipe(true)
      } else if (ox < -90) {
        setExitX(-1200)
        onSwipe(false)
      } else {
        controls.start({ x: 0, rotate: 0 })
      }
      return
    }

    // Swipe up → open apartment modal details
    if (oy < -60) {
      onShowDetails()
    }
    controls.start({ x: 0, y: 0, rotate: 0 })
  }

  const handleManualSwipe = (interest: boolean) => {
    setExitX(interest ? 1200 : -1200)
    onSwipe(interest)
  }

  return (
    <motion.div
      style={{ x, rotate, opacity: cardOpacity }}
      initial={{ scale: 0.95, opacity: 0 }}
      exit={{ x: exitX, opacity: 0, transition: { duration: 0.28 } }}
      drag
      dragConstraints={{ left: 0, right: 0, top: 0, bottom: 0 }}
      dragElastic={0.15}
      onDragEnd={handleDragEnd}
      animate={controls}
      className="absolute inset-0 rounded-3xl shadow-2xl overflow-hidden cursor-grab active:cursor-grabbing select-none touch-none"
    >
      {/* ── Full-bleed photo ── */}
      <img
        src={
          apartment.coverImageUrl ||
          'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80'
        }
        alt={apartment.title}
        className="absolute inset-0 w-full h-full object-cover pointer-events-none"
        draggable={false}
      />

      {/* ── Like / Nope overlays ── */}
      <motion.div
        style={{ opacity: likeOpacity }}
        className="absolute inset-0 bg-success/30 flex items-start justify-start p-6 z-10 pointer-events-none"
      >
        <span className="text-4xl font-black text-success drop-shadow-lg border-4 border-success rounded-xl px-4 py-1 rotate-[-20deg]">
          LIKE
        </span>
      </motion.div>
      <motion.div
        style={{ opacity: nopeOpacity }}
        className="absolute inset-0 bg-error/30 flex items-start justify-end p-6 z-10 pointer-events-none"
      >
        <span className="text-4xl font-black text-error drop-shadow-lg border-4 border-error rounded-xl px-4 py-1 rotate-[20deg]">
          NOPE
        </span>
      </motion.div>

      {/* ── Bottom gradient + always-visible info ── */}
      <div className="absolute bottom-0 inset-x-0 h-2/3 bg-gradient-to-t from-black/90 via-black/40 to-transparent pointer-events-none z-20" />

      <div className="absolute bottom-0 inset-x-0 z-30 px-5 pb-24">
        {/* Swipe-up hint */}
        <div className="flex justify-center mb-2">
          <div className="flex flex-col items-center gap-0.5 opacity-70 animate-bounce">
            <div className="w-8 h-1 bg-white/80 rounded-full" />
            <div className="w-5 h-1 bg-white/50 rounded-full" />
          </div>
        </div>

        <h2 className="text-3xl font-bold text-white leading-tight drop-shadow-md">
          {apartment.title}
        </h2>
        <div className="flex items-center gap-1.5 mt-1 text-white/90">
          <MapPin size={15} className="shrink-0" />
          <span className="text-base">{apartment.ubication}</span>
        </div>
        <p className="text-2xl font-bold text-primary-content mt-1 drop-shadow">
          <span className="bg-primary px-2 py-0.5 rounded-lg">{apartment.price} €<span className="text-sm font-normal">/mes</span></span>
        </p>
      </div>

      {/* ── Action buttons ── */}
      <div className="absolute bottom-5 inset-x-0 z-50 flex items-center justify-center gap-8">
        <button
          onClick={() => handleManualSwipe(false)}
          className="btn btn-circle btn-error shadow-xl hover:scale-110 active:scale-95 transition-transform"
          style={{ width: 64, height: 64 }}
        >
          <X size={30} />
        </button>
        <button
          onClick={() => handleManualSwipe(true)}
          className="btn btn-circle btn-success shadow-xl hover:scale-110 active:scale-95 transition-transform text-white"
          style={{ width: 64, height: 64 }}
        >
          <Check size={30} />
        </button>
      </div>
    </motion.div>
  )
}
