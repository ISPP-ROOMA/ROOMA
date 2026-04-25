import { Calendar, Clock, Loader2, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useToast } from '../hooks/useToast'
import { bookAppointmentSlot, getAvailableSlotsForMatch, type AppointmentSlotDTO } from '../service/appointment.service'

interface Props {
  matchId: number
  onClose: () => void
  onSuccess: () => void
}

export default function BookAppointmentModal({ matchId, onClose, onSuccess }: Props) {
  const [slots, setSlots] = useState<AppointmentSlotDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [bookingId, setBookingId] = useState<number | null>(null)
  const { showToast } = useToast()

  useEffect(() => {
    getAvailableSlotsForMatch(matchId)
      .then(setSlots)
      .catch(() => showToast('Error al cargar horarios disponibles', 'error'))
      .finally(() => setLoading(false))
  }, [matchId, showToast])

  const handleBook = async (slotId: number) => {
    setBookingId(slotId)
    try {
      await bookAppointmentSlot(slotId)
      showToast('Cita reservada con éxito', 'success')
      onSuccess()
    } catch {
      showToast('Ya tienes una cita', 'error')
    } finally {
      setBookingId(null)
    }
  }

  const groupedSlots = slots.reduce((acc, slot) => {
    if (!acc[slot.blockDate]) {
      acc[slot.blockDate] = []
    }
    acc[slot.blockDate].push(slot)
    return acc
  }, {} as Record<string, AppointmentSlotDTO[]>)

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-xl overflow-hidden flex flex-col max-h-[85vh] animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between p-5 border-b border-[#DDDBCB] shrink-0">
          <h2 className="text-[#050505] text-lg font-bold flex items-center gap-2">
            <Calendar size={20} className="text-[#008080]" />
            Elige una hora
          </h2>
          <button onClick={onClose} className="p-2 bg-[#F5F1E3] hover:bg-[#DDDBCB] rounded-full transition-colors text-[#050505]">
            <X size={20} />
          </button>
        </div>

        <div className="p-5 overflow-y-auto flex-1 flex flex-col gap-6">
          {loading ? (
            <div className="flex justify-center p-8"><Loader2 className="animate-spin text-[#008080]" /></div>
          ) : slots.length === 0 ? (
            <div className="text-center p-8 text-[#050505]/60 flex flex-col items-center gap-3">
              <Clock size={32} />
              <p>No hay citas disponibles ahora mismo. Verifica más tarde o habla con el casero.</p>
            </div>
          ) : (
            Object.keys(groupedSlots).sort().map(date => (
              <div key={date}>
                <h3 className="font-bold text-[#050505] mb-3 text-sm uppercase tracking-wide border-b border-[#DDDBCB] pb-2">
                  {new Date(date).toLocaleDateString('es-ES', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
                </h3>
                <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
                  {groupedSlots[date].map(slot => (
                    <button
                      key={slot.id}
                      onClick={() => handleBook(slot.id)}
                      disabled={bookingId !== null}
                      className="flex flex-col items-center justify-center py-3 px-2 rounded-2xl border-2 border-[#DDDBCB] hover:border-[#008080] hover:bg-[#E8F7F7] focus:ring-4 focus:ring-[#008080]/20 transition-all font-medium text-[#050505]"
                    >
                      <span>{slot.startTime.substring(0, 5)}</span>
                      {bookingId === slot.id && <Loader2 size={16} className="animate-spin mt-1" />}
                    </button>
                  ))}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}
