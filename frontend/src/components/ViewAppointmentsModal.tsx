import { useEffect, useState } from 'react'
import { CalendarCheck, ChevronDown, ChevronUp, Loader2, User, X } from 'lucide-react'
import { getBlocksForApartment, cancelAppointmentSlot, type AvailabilityBlockDTO } from '../service/appointment.service'
import { useToast } from '../hooks/useToast'

interface Props {
  apartmentId: number
  apartmentTitle: string
  onClose: () => void
}

export default function ViewAppointmentsModal({ apartmentId, apartmentTitle, onClose }: Props) {
  const [blocks, setBlocks] = useState<AvailabilityBlockDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [expandedBlock, setExpandedBlock] = useState<number | null>(null)
  const [cancelingId, setCancelingId] = useState<number | null>(null)
  const { showToast } = useToast()

  const loadBlocks = () => {
    setLoading(true)
    getBlocksForApartment(apartmentId)
      .then(data => {
        setBlocks(data)
        if (data.length > 0) setExpandedBlock(data[0].id)
      })
      .catch(() => showToast('Error al cargar las visitas', 'error'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadBlocks()
  }, [apartmentId])

  const handleCancel = async (slotId: number) => {
    setCancelingId(slotId)
    try {
      await cancelAppointmentSlot(slotId)
      showToast('Cita cancelada', 'success')
      loadBlocks()
    } catch {
      showToast('Error al cancelar la cita', 'error')
    } finally {
      setCancelingId(null)
    }
  }

  const bookedCount = blocks.reduce((acc, b) => acc + b.slots.filter(s => s.status === 'BOOKED').length, 0)

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-xl overflow-hidden flex flex-col max-h-[85vh] animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-[#DDDBCB] shrink-0">
          <div>
            <h2 className="text-[#050505] text-lg font-bold flex items-center gap-2">
              <CalendarCheck size={20} className="text-[#008080]" />
              Visitas Programadas
            </h2>
            <p className="text-xs text-[#050505]/60 mt-0.5 truncate max-w-[260px]">{apartmentTitle}</p>
          </div>
          <button onClick={onClose} className="p-2 bg-[#F5F1E3] hover:bg-[#DDDBCB] rounded-full transition-colors text-[#050505]">
            <X size={20} />
          </button>
        </div>

        {/* Summary badge */}
        {!loading && bookedCount > 0 && (
          <div className="mx-5 mt-4 px-4 py-2 rounded-2xl bg-[#E8F7F7] border border-[#008080]/20 text-sm text-[#008080] font-semibold">
            {bookedCount} visita{bookedCount !== 1 ? 's' : ''} confirmada{bookedCount !== 1 ? 's' : ''}
          </div>
        )}

        {/* Content */}
        <div className="p-5 overflow-y-auto flex-1 flex flex-col gap-4">
          {loading ? (
            <div className="flex justify-center p-8">
              <Loader2 className="animate-spin text-[#008080]" />
            </div>
          ) : blocks.length === 0 ? (
            <div className="text-center p-8 text-[#050505]/60 flex flex-col items-center gap-3">
              <CalendarCheck size={32} />
              <p>Aún no has creado ningún bloque de visitas para este inmueble.</p>
            </div>
          ) : (
            blocks.map(block => {
              const isExpanded = expandedBlock === block.id
              const booked = block.slots.filter(s => s.status === 'BOOKED')
              const available = block.slots.filter(s => s.status === 'AVAILABLE')
              const dateLabel = new Date(block.blockDate).toLocaleDateString('es-ES', {
                weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
              })

              return (
                <div key={block.id} className="rounded-2xl border border-[#DDDBCB] overflow-hidden">
                  {/* Block header */}
                  <button
                    onClick={() => setExpandedBlock(isExpanded ? null : block.id)}
                    className="w-full flex items-center justify-between p-4 bg-[#F5F1E3] hover:bg-[#EBE7D5] transition-colors"
                  >
                    <div className="text-left">
                      <p className="font-bold text-[#050505] capitalize text-sm">{dateLabel}</p>
                      <p className="text-xs text-[#050505]/60 mt-0.5">
                        {block.startTime.substring(0, 5)} – {block.endTime.substring(0, 5)}
                        {' · '}
                        <span className="text-[#008080] font-semibold">{booked.length} reservada{booked.length !== 1 ? 's' : ''}</span>
                        {' · '}
                        <span>{available.length} libre{available.length !== 1 ? 's' : ''}</span>
                      </p>
                    </div>
                    {isExpanded ? <ChevronUp size={18} className="text-[#050505]/40 shrink-0" /> : <ChevronDown size={18} className="text-[#050505]/40 shrink-0" />}
                  </button>

                  {/* Slots list */}
                  {isExpanded && (
                    <div className="divide-y divide-[#DDDBCB]">
                      {block.slots.map(slot => {
                        const isBooked = slot.status === 'BOOKED'
                        return (
                          <div key={slot.id} className={`flex items-center justify-between px-4 py-3 ${isBooked ? 'bg-white' : 'bg-[#F5F1E3]/40'}`}>
                            <div className="flex items-center gap-3">
                              <span className="text-sm font-bold text-[#050505] w-12 shrink-0">
                                {slot.startTime.substring(0, 5)}
                              </span>
                              {isBooked && slot.tenant ? (
                                <div className="flex items-center gap-2">
                                  <div className="flex h-7 w-7 items-center justify-center rounded-full bg-[#008080] text-white shrink-0">
                                    <User size={13} />
                                  </div>
                                  <div>
                                    <p className="text-xs font-semibold text-[#050505] leading-tight">
                                      {slot.tenant.name ?? slot.tenant.email}
                                    </p>
                                    <p className="text-[10px] text-[#050505]/50 leading-tight">{slot.tenant.email}</p>
                                  </div>
                                </div>
                              ) : (
                                <span className="text-xs text-[#050505]/40">Libre</span>
                              )}
                            </div>

                            {isBooked && (
                              <button
                                onClick={() => handleCancel(slot.id)}
                                disabled={cancelingId === slot.id}
                                className="text-xs font-semibold text-red-500 hover:text-red-700 transition-colors disabled:opacity-50 shrink-0 ml-2"
                              >
                                {cancelingId === slot.id ? 'Cancelando...' : 'Cancelar'}
                              </button>
                            )}
                          </div>
                        )
                      })}
                    </div>
                  )}
                </div>
              )
            })
          )}
        </div>
      </div>
    </div>
  )
}
