import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useToast } from '../../../hooks/useToast'
import {
  confirmIncidentSolution,
  getIncidentById,
  rejectIncidentSolution,
  type IncidentDTO,
  type IncidentStatus,
} from '../../../service/incidents.service'
import { useAuthStore } from '../../../store/authStore'

const STATUS_ORDER: IncidentStatus[] = [
  'OPEN',
  'RECEIVED',
  'IN_PROGRESS',
  'TECHNICIAN_NOTIFIED',
  'RESOLVED',
  'CLOSED',
  'CLOSED_INACTIVITY',
]

const STATUS_LABELS: Record<IncidentStatus, string> = {
  OPEN: 'Abierta',
  RECEIVED: 'Recibida por casero',
  IN_PROGRESS: 'En proceso',
  TECHNICIAN_NOTIFIED: 'Tecnico avisado',
  RESOLVED: 'Resuelta por casero',
  CLOSED: 'Cerrada',
  CLOSED_INACTIVITY: 'Cerrada por inactividad',
}

const STATUS_COLORS: Record<IncidentStatus, string> = {
  OPEN: 'bg-slate-400',
  RECEIVED: 'bg-blue-500',
  IN_PROGRESS: 'bg-amber-500',
  TECHNICIAN_NOTIFIED: 'bg-orange-500',
  RESOLVED: 'bg-lime-500',
  CLOSED: 'bg-emerald-600',
  CLOSED_INACTIVITY: 'bg-emerald-700',
}

const formatDate = (value?: string) => {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return new Intl.DateTimeFormat('es-ES', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(d)
}

export default function IncidentDetail() {
  const { id, incidentId } = useParams()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const { role, userId } = useAuthStore()

  const apartmentId = Number(id)
  const currentIncidentId = Number(incidentId)

  const [incident, setIncident] = useState<IncidentDTO | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [rejectReason, setRejectReason] = useState('')
  const [showRejectBox, setShowRejectBox] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const tenantUserId = Number(userId)
  const isTenantOwner =
    role === 'TENANT' &&
    Number.isFinite(tenantUserId) &&
    incident?.tenantId === tenantUserId
  const canTenantValidate = isTenantOwner && incident?.status === 'RESOLVED'

  const timeline = useMemo(() => {
    if (!incident) return []

    const historyByStatus = new Map(incident.statusHistory.map((h) => [h.status, h]))
    const currentIdx = STATUS_ORDER.indexOf(incident.status)

    return STATUS_ORDER.slice(0, currentIdx + 1).map((status) => {
      const hit = historyByStatus.get(status)
      return {
        status,
        label: STATUS_LABELS[status],
        reachedAt: hit?.changedAt,
      }
    })
  }, [incident])

  const loadIncident = async () => {
    if (!Number.isFinite(apartmentId) || !Number.isFinite(currentIncidentId)) {
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    const data = await getIncidentById(apartmentId, currentIncidentId)
    setIncident(data)
    setIsLoading(false)
  }

  useEffect(() => {
    void loadIncident()
  }, [apartmentId, currentIncidentId])

  const handleConfirm = async () => {
    if (!incident) return

    setIsSubmitting(true)
    try {
      await confirmIncidentSolution(apartmentId, incident.id)
      showToast('Incidencia cerrada correctamente', 'success')
      await loadIncident()
    } catch (error) {
      console.error(error)
      showToast('No se pudo confirmar la solucion', 'error')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleReject = async () => {
    if (!incident) return
    const reason = rejectReason.trim()
    if (!reason) {
      showToast('Debes indicar un motivo de rechazo', 'warning')
      return
    }

    setIsSubmitting(true)
    try {
      await rejectIncidentSolution(apartmentId, incident.id, reason)
      showToast('Se ha reenviado a En proceso', 'success')
      setRejectReason('')
      setShowRejectBox(false)
      await loadIncident()
    } catch (error) {
      console.error(error)
      showToast('No se pudo rechazar la solucion', 'error')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <p className="text-center mt-10 text-base-content/60">Cargando incidencia...</p>
  }

  if (!incident) {
    return <p className="text-center mt-10 text-red-500">No se pudo cargar la incidencia.</p>
  }

  return (
    <section className="min-h-[75vh] bg-base-200 px-3 py-6 sm:px-4 sm:py-8">
      <div className="max-w-4xl mx-auto space-y-6">
        <header className="bg-white rounded-2xl shadow-sm p-5 sm:p-6 space-y-3">
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => navigate(-1)}>
            Volver
          </button>
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <h1 className="text-2xl font-semibold">{incident.title}</h1>
              <p className="text-sm text-base-content/60 mt-1">{incident.incidentCode}</p>
              <p className="text-sm text-base-content/60 mt-1">
                Inquilino que reporto: {incident.tenantEmail || `ID ${incident.tenantId}`}
              </p>
            </div>
            <span
              className={`text-xs px-2 py-1 rounded-full text-white ${STATUS_COLORS[incident.status]}`}
            >
              {STATUS_LABELS[incident.status]}
            </span>
          </div>
          <p className="text-base text-base-content/80">{incident.description}</p>
          {incident.rejectionReason && (
            <p className="text-sm text-orange-700 bg-orange-50 rounded-lg p-3">
              Motivo ultimo rechazo: {incident.rejectionReason}
            </p>
          )}
        </header>

        <article className="bg-white rounded-2xl shadow-sm p-5 sm:p-6 space-y-4">
          <h2 className="text-lg font-semibold">Timeline de estado</h2>
          <ol className="space-y-4">
            {timeline.map((step, idx) => (
              <li key={step.status} className="flex gap-3 items-start">
                <div className="flex flex-col items-center">
                  <span className={`w-3 h-3 rounded-full ${STATUS_COLORS[step.status]}`} />
                  {idx !== timeline.length - 1 && <span className="w-px h-8 bg-base-300 mt-1" />}
                </div>
                <div className="flex-1 pb-2">
                  <p className="font-medium text-sm">{step.label}</p>
                  <p className="text-xs text-base-content/60">{formatDate(step.reachedAt)}</p>
                </div>
              </li>
            ))}
          </ol>
        </article>

        <article className="bg-white rounded-2xl shadow-sm p-5 sm:p-6 space-y-3">
          <h2 className="text-lg font-semibold">Chat tecnico</h2>
          <button type="button" className="btn btn-outline btn-sm">
            Abrir conversacion (proximamente)
          </button>
        </article>

        {canTenantValidate && (
          <article className="bg-white rounded-2xl shadow-sm p-5 sm:p-6 space-y-4">
            <h2 className="text-lg font-semibold">Validacion del inquilino</h2>
            <p className="text-sm text-base-content/70">
              Cuando el casero marca la incidencia como resuelta, puedes confirmar o rechazar la
              solucion.
            </p>

            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                className="btn btn-success btn-sm"
                disabled={isSubmitting}
                onClick={() => void handleConfirm()}
              >
                Confirmar solucion
              </button>
              <button
                type="button"
                className="btn btn-error btn-outline btn-sm"
                disabled={isSubmitting}
                onClick={() => setShowRejectBox((v) => !v)}
              >
                Rechazar solucion
              </button>
            </div>

            {showRejectBox && (
              <div className="space-y-2">
                <label className="text-sm font-medium">Motivo del rechazo</label>
                <textarea
                  className="textarea textarea-bordered w-full"
                  value={rejectReason}
                  maxLength={1000}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="Explica que sigue fallando o que no se ha resuelto"
                />
                <button
                  type="button"
                  className="btn btn-error btn-sm"
                  disabled={isSubmitting}
                  onClick={() => void handleReject()}
                >
                  Enviar rechazo
                </button>
              </div>
            )}
          </article>
        )}
      </div>
    </section>
  )
}
