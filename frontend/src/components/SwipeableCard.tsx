import { motion, useAnimation, useMotionValue, useTransform } from 'framer-motion'
import { Check, ChevronUp, Info, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { ApartmentDTO } from '../service/apartment.service'

interface SwipeableCardProps {
  apartment: ApartmentDTO
  onSwipe: (interest: boolean) => void
  onShowDetails: () => void
}

export default function SwipeableCard({ apartment, onSwipe, onShowDetails }: SwipeableCardProps) {
  const x = useMotionValue(0)
  const y = useMotionValue(0)
  const controls = useAnimation()
  const [exitX, setExitX] = useState<number | string>(0)

  const rotate = useTransform(x, [-200, 200], [-30, 30])
  const opacity = useTransform(x, [-200, -100, 0, 100, 200], [0, 1, 1, 1, 0])

  // Scale up when dragging upward — responds faster
  const dragScale = useTransform(y, [0, -60], [1, 1.06])

  // Up-swipe indicator opacity — appears quickly
  const upIndicatorOpacity = useTransform(y, [0, -20], [0, 1])

  useEffect(() => {
    controls.start({ scale: 1, opacity: 1, x: 0, y: 0 })
  }, [controls])

  // Color overlays based on swipe direction
  const background = useTransform(
    x,
    [-100, 0, 100],
    [
      'linear-gradient(180deg, rgba(239, 68, 68, 0.4) 0%, rgba(0,0,0,0) 100%)',
      'linear-gradient(180deg, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 100%)',
      'linear-gradient(180deg, rgba(34, 197, 94, 0.4) 0%, rgba(0,0,0,0) 100%)',
    ]
  )

  // Upward swipe overlay — responds faster
  const upOverlayOpacity = useTransform(y, [0, -40], [0, 0.35])

  const handleDragEnd = async (_: any, info: any) => {
    const absX = Math.abs(info.offset.x)
    const absY = Math.abs(info.offset.y)

    if (absY > absX && info.offset.y < -35) {
      // Upward swipe → immediately show details, no intermediate animation
      onShowDetails()
      controls.set({ x: 0, y: 0, scale: 1, rotate: 0 })
    } else if (absX > absY && info.offset.x > 100) {
      setExitX(1000)
      onSwipe(true)
    } else if (absX > absY && info.offset.x < -100) {
      setExitX(-1000)
      onSwipe(false)
    } else {
      controls.start({ x: 0, y: 0, rotate: 0 })
    }
  }

  const handleManualSwipe = (interest: boolean) => {
    setExitX(interest ? 1000 : -1000)
    onSwipe(interest)
  }

  return (
    <motion.div
      style={{
        x,
        y,
        rotate,
        opacity,
        scale: dragScale,
      }}
      initial={{ scale: 0.95, opacity: 0 }}
      exit={{ x: exitX, opacity: 0, transition: { duration: 0.3 } }}
      drag
      dragDirectionLock
      dragConstraints={{ left: 0, right: 0, top: 0, bottom: 0 }}
      dragElastic={{ left: 0.7, right: 0.7, top: 0.5, bottom: 0.1 }}
      onDragEnd={handleDragEnd}
      animate={controls}
      className="absolute w-full h-full p-4 rounded-3xl shadow-xl bg-base-100 flex flex-col justify-between overflow-hidden cursor-grab active:cursor-grabbing border border-base-300"
    >
      {/* Horizontal swipe overlay */}
      <motion.div style={{ background }} className="absolute inset-0 z-10 pointer-events-none rounded-3xl" />

      {/* Upward swipe overlay */}
      <motion.div
        style={{ opacity: upOverlayOpacity }}
        className="absolute inset-0 z-10 pointer-events-none bg-info/30 rounded-3xl"
      />

      {/* Swipe up indicator */}
      <motion.div
        style={{ opacity: upIndicatorOpacity }}
        className="absolute top-6 left-1/2 -translate-x-1/2 z-20 pointer-events-none flex flex-col items-center gap-1"
      >
        <ChevronUp size={28} className="text-info" />
        <span className="text-info font-semibold text-sm bg-base-100/80 backdrop-blur-sm px-3 py-1.5 rounded-full shadow-sm">
          Ver detalles
        </span>
      </motion.div>

      {/* Image container */}
      <div className="relative w-full h-[65%] rounded-2xl overflow-hidden bg-base-300">
        <img
          src={
            apartment.coverImageUrl ||
            'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80'
          }
          alt={apartment.title}
          className="object-cover w-full h-full pointer-events-none"
        />
        <div className="absolute bottom-0 left-0 w-full p-4 bg-gradient-to-t from-black/80 to-transparent">
          <h2 className="text-3xl font-bold text-white drop-shadow-md">{apartment.price} €/mes</h2>
          <p className="text-lg text-white/90 drop-shadow-md">{apartment.ubication}</p>
        </div>
      </div>

      {/* Info container */}
      <div className="flex-1 flex flex-col justify-between py-4">
        <div>
          <h3 className="text-2xl font-bold leading-tight">{apartment.title}</h3>
          <p className="text-base-content/70 mt-1 line-clamp-2">{apartment.description}</p>
        </div>

        <div className="flex items-center justify-center gap-6 mt-4 z-20">
          <button
            onClick={() => handleManualSwipe(false)}
            className="btn btn-circle btn-error btn-lg shadow-lg hover:scale-110 transition-transform"
          >
            <X size={32} />
          </button>
          <button
            onClick={onShowDetails}
            className="btn btn-circle btn-info btn-sm shadow-md hover:scale-110 transition-transform"
          >
            <Info size={20} />
          </button>
          <button
            onClick={() => handleManualSwipe(true)}
            className="btn btn-circle btn-success btn-lg shadow-lg hover:scale-110 transition-transform text-white"
          >
            <Check size={32} />
          </button>
        </div>
      </div>
    </motion.div>
  )
}
