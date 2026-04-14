import { useState } from 'react'
import { Calendar, Clock, X, Loader2 } from 'lucide-react'
import { createAvailabilityBlock } from '../service/appointment.service'
import { useToast } from '../hooks/useToast'
import axios from 'axios'

interface Props {
  apartmentId: number
  apartmentTitle: string
  onClose: () => void
  onSuccess: () => void
}

export default function CreateAppointmentModal({ apartmentId, apartmentTitle, onClose, onSuccess }: Props) {
  const [date, setDate] = useState('')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [loading, setLoading] = useState(false)
  const { showToast } = useToast()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!date || !startTime || !endTime) {
      showToast('Por favor, rellena todos los campos', 'error')
      return
    }

    setLoading(true)
    try {
      await createAvailabilityBlock(apartmentId, {
        blockDate: date,
        startTime: startTime + ':00',
        endTime: endTime + ':00',
        slotDurationMinutes: 30
      })
      showToast('Visitas organizadas con éxito', 'success')
      onSuccess()
    } catch (error) {
      const msg = axios.isAxiosError(error) ? error.response?.data?.message : 'Error al organizar visitas'
      showToast(msg || 'Error al organizar visitas', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between p-5 border-b border-[#DDDBCB]">
          <h2 className="text-[#050505] text-lg font-bold">Organizar Visitas</h2>
          <button onClick={onClose} className="p-2 bg-[#F5F1E3] hover:bg-[#DDDBCB] rounded-full transition-colors text-[#050505]">
            <X size={20} />
          </button>
        </div>
        
        <form onSubmit={handleSubmit} className="p-5 flex flex-col gap-5">
          <p className="text-sm text-[#050505]/70">
            Define un día y un rango de horas para <strong>{apartmentTitle}</strong>. 
            Se generarán citas de 30 minutos automáticamente y se avisará a todos los candidatos que hayan hecho match.
          </p>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-[#050505]/50 uppercase tracking-tight flex items-center gap-2">
              <Calendar size={14} /> Fecha de las visitas
            </label>
            <input 
              type="date" 
              value={date}
              onChange={(e) => setDate(e.target.value)}
              min={new Date().toISOString().split('T')[0]}
              className="w-full px-4 py-3 bg-[#F5F1E3]/50 border border-[#DDDBCB] rounded-xl focus:outline-none focus:ring-2 focus:ring-[#008080] text-[#050505] font-medium" 
              required
            />
          </div>

          <div className="flex gap-4">
            <div className="flex flex-col gap-1.5 flex-1">
              <label className="text-xs font-bold text-[#050505]/50 uppercase tracking-tight flex items-center gap-2">
                <Clock size={14} /> Desde las
              </label>
              <input 
                type="time" 
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="w-full px-4 py-3 bg-[#F5F1E3]/50 border border-[#DDDBCB] rounded-xl focus:outline-none focus:ring-2 focus:ring-[#008080] text-[#050505] font-medium" 
                required
              />
            </div>
            <div className="flex flex-col gap-1.5 flex-1">
              <label className="text-xs font-bold text-[#050505]/50 uppercase tracking-tight flex items-center gap-2">
                <Clock size={14} /> Hasta las
              </label>
              <input 
                type="time" 
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                className="w-full px-4 py-3 bg-[#F5F1E3]/50 border border-[#DDDBCB] rounded-xl focus:outline-none focus:ring-2 focus:ring-[#008080] text-[#050505] font-medium" 
                required
              />
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="w-full py-3.5 mt-2 rounded-xl bg-[#008080] text-white font-bold text-sm tracking-wide shadow-lg hover:shadow-xl hover:bg-[#007070] transition-all disabled:opacity-50 flex items-center justify-center gap-2"
          >
            {loading ? <Loader2 size={18} className="animate-spin" /> : <Calendar size={18} />}
            CREAR VISITAS AUTOMÁTICAS
          </button>
        </form>
      </div>
    </div>
  )
}
