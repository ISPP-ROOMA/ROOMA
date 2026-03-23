import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useToast } from '../../../hooks/useToast'
import {
  confirmIncidentSolution,
  getIncidentById,
  landlordUpdateIncidentStatus,
  rejectIncidentSolution,
  type IncidentCategory,
  type IncidentDTO,
  type IncidentStatus,
  type IncidentUrgency,
  type IncidentZone,
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

const CATEGORY_LABELS: Record<IncidentCategory, string> = {
  PLUMBING: 'Fontaneria',
  ELECTRICITY: 'Electricidad',
  APPLIANCES: 'Electrodomesticos',
  LOCKSMITH: 'Cerrajeria',
  PAINT_WALLS: 'Pintura/Paredes',
  CLIMATE: 'Climatizacion',
  PESTS: 'Plagas',
  OTHER: 'Otro',
}

const ZONE_LABELS: Record<IncidentZone, string> = {
  KITCHEN: 'Cocina',
  BATHROOM: 'Bano',
  LIVING_ROOM: 'Salon',
  BEDROOM: 'Dormitorio',
  COMMON_AREAS: 'Zonas comunes',
  EXTERIOR: 'Exterior',
}

const URGENCY_META: Record<IncidentUrgency, { label: string; badge: string }> = {
  LOW: { label: 'Baja', badge: 'bg-emerald-100 text-emerald-700' },
  MEDIUM: { label: 'Media', badge: 'bg-amber-100 text-amber-700' },
  HIGH: { label: 'Alta', badge: 'bg-orange-100 text-orange-700' },
  URGENT: { label: 'Urgente', badge: 'bg-red-100 text-red-700' },
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

const LANDLORD_ACTIONS: Partial<Record<IncidentStatus, Array<{ status: IncidentStatus; label: string }>>> = {
  OPEN: [{ status: 'RECEIVED', label: 'Marcar como recibida' }],
  RECEIVED: [{ status: 'IN_PROGRESS', label: 'Empezar gestion' }],
  IN_PROGRESS: [
    { status: 'TECHNICIAN_NOTIFIED', label: 'Tecnico avisado' },
    { status: 'RESOLVED', label: 'Marcar como resuelta' },
  ],
  TECHNICIAN_NOTIFIED: [
    { status: 'IN_PROGRESS', label: 'Volver a en proceso' },
    { status: 'RESOLVED', label: 'Marcar como resuelta' },
  ],
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
  const isLandlord = role === 'LANDLORD'
  const isTenantOwner =
    role === 'TENANT' &&
    Number.isFinite(tenantUserId) &&
    incident?.tenantId === tenantUserId
  const canTenantValidate = isTenantOwner && incident?.status === 'RESOLVED'
  const landlordActions = incident ? LANDLORD_ACTIONS[incident.status] ?? [] : []

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

  const handleLandlordStatusChange = async (nextStatus: IncidentStatus) => {
    if (!incident) return

    setIsSubmitting(true)
    try {
      await landlordUpdateIncidentStatus(apartmentId, incident.id, { status: nextStatus })
      showToast('Estado actualizado correctamente', 'success')
      await loadIncident()
    } catch (error) {
      console.error(error)
      showToast('No se pudo actualizar el estado', 'error')
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
    <section className="min-h-[75vh] bg-[radial-gradient(circle_at_top_left,rgba(20,184,166,0.08),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(15,23,42,0.06),transparent_22%),var(--fallback-b2,oklch(var(--b2)))] px-3 py-6 sm:px-4 sm:py-8">
      <div className="max-w-4xl mx-auto space-y-6">
        <header className="overflow-hidden rounded-[30px] border border-base-300/70 bg-white shadow-[0_20px_55px_rgba(15,23,42,0.08)]">
          <div className="bg-[radial-gradient(circle_at_top_left,rgba(20,184,166,0.12),transparent_32%),linear-gradient(135deg,rgba(255,255,255,1),rgba(248,250,252,0.92))] p-5 sm:p-7">
          <button
            type="button"
            className="btn btn-outline btn-sm rounded-full border-base-content/20 bg-white/80 px-5 shadow-sm transition hover:-translate-y-0.5 hover:bg-white"
            onClick={() => navigate(-1)}
          >
            Volver
          </button>
            <div className="mt-4 flex flex-wrap items-start justify-between gap-4">
              <div className="max-w-2xl">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary/70">
                  {incident.incidentCode}
                </p>
                <h1 className="mt-2 text-2xl font-semibold leading-tight sm:text-3xl">
                  {incident.title}
                </h1>
                <p className="mt-3 text-base leading-7 text-base-content/75">{incident.description}</p>
              </div>
              <span
                className={`rounded-full px-3 py-1.5 text-xs font-semibold text-white shadow-sm ${STATUS_COLORS[incident.status]}`}
              >
                {STATUS_LABELS[incident.status]}
              </span>
            </div>

            <div className="mt-5 flex flex-wrap gap-2">
              <span className="rounded-full border border-base-300 bg-white/80 px-3 py-1.5 text-xs text-base-content/65">
                Inquilino: {incident.tenantEmail || `ID ${incident.tenantId}`}
              </span>
              <span className="rounded-full border border-base-300 bg-white/80 px-3 py-1.5 text-xs text-base-content/65">
                Creada: {formatDate(incident.createdAt)}
              </span>
              <span className="rounded-full border border-base-300 bg-white/80 px-3 py-1.5 text-xs text-base-content/65">
                Actualizada: {formatDate(incident.updatedAt)}
              </span>
            </div>

            {incident.rejectionReason && (
              <p className="mt-5 rounded-2xl border border-orange-200 bg-orange-50 px-4 py-3 text-sm leading-6 text-orange-700">
                Motivo ultimo rechazo: {incident.rejectionReason}
              </p>
            )}
          </div>
        </header>

        <article className="rounded-[30px] border border-base-300/70 bg-white p-5 shadow-[0_18px_48px_rgba(15,23,42,0.08)] sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold">Ficha de la incidencia</h2>
            <span className={`rounded-full px-3 py-1 text-xs ${URGENCY_META[incident.urgency].badge}`}>
              Prioridad {URGENCY_META[incident.urgency].label}
            </span>
          </div>

          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <InfoCard label="Categoria" value={CATEGORY_LABELS[incident.category]} />
            <InfoCard label="Zona" value={ZONE_LABELS[incident.zone]} />
            <InfoCard label="Creada" value={formatDate(incident.createdAt)} />
            <InfoCard label="Actualizada" value={formatDate(incident.updatedAt)} />
          </div>

          {incident.photos.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center justify-between gap-2">
                <h3 className="font-medium">Fotos adjuntas</h3>
                <span className="text-xs text-base-content/50">{incident.photos.length} archivo(s)</span>
              </div>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {incident.photos.map((photo, index) => (
                  <a
                    key={`${photo}-${index}`}
                    href={photo}
                    target="_blank"
                    rel="noreferrer"
                    className="group overflow-hidden rounded-2xl border border-base-200/70 bg-base-200 shadow-sm transition duration-300 hover:-translate-y-1 hover:shadow-lg"
                  >
                    <img
                      src={photo}
                      alt={`Incidencia ${incident.incidentCode} foto ${index + 1}`}
                      className="h-44 w-full object-cover transition duration-300 group-hover:scale-[1.03]"
                    />
                  </a>
                ))}
              </div>
            </div>
          )}
        </article>

        <article className="rounded-[30px] border border-base-300/70 bg-white p-5 shadow-[0_18px_48px_rgba(15,23,42,0.08)] sm:p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-semibold">Timeline de estado</h2>
            <span className="rounded-full border border-base-300 bg-base-100 px-3 py-1 text-xs text-base-content/55">
              Historial completo
            </span>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            {timeline.map((step) => (
              <span
                key={`badge-${step.status}`}
                className={`rounded-full px-3 py-1 text-xs font-medium ${
                  step.status === incident.status
                    ? 'bg-primary text-white'
                    : 'border border-base-300 bg-base-100 text-base-content/60'
                }`}
              >
                {step.label}
              </span>
            ))}
          </div>
          <ol className="space-y-4">
            {timeline.map((step, idx) => (
              <li key={step.status} className="flex items-start gap-3 rounded-[22px] border border-base-200/70 bg-base-100/70 px-3 py-3 transition duration-300 hover:border-base-300 hover:bg-base-100">
                <div className="flex flex-col items-center">
                  <span className={`w-3 h-3 rounded-full ${STATUS_COLORS[step.status]}`} />
                  {idx !== timeline.length - 1 && <span className="w-px h-8 bg-base-300 mt-1" />}
                </div>
                <div className="flex-1 pb-2">
                  <p className="font-medium text-sm">{step.label}</p>
                  <p className="text-xs text-base-content/60">{formatDate(step.reachedAt)}</p>
                  {incident.statusHistory.find((item) => item.status === step.status)?.userEmail && (
                    <p className="text-xs text-base-content/50 mt-1">
                      Cambio registrado por{' '}
                      {incident.statusHistory.find((item) => item.status === step.status)?.userEmail}
                    </p>
                  )}
                </div>
              </li>
            ))}
          </ol>
        </article>

        {isLandlord && incident.status !== 'CLOSED' && incident.status !== 'CLOSED_INACTIVITY' && (
          <article className="overflow-hidden rounded-[30px] border border-base-300/70 bg-white shadow-[0_18px_50px_rgba(15,23,42,0.09)]">
            <div className="border-b border-base-200 bg-gradient-to-r from-primary/8 via-base-100 to-base-100 p-5 sm:p-6">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold">Acciones del casero</h2>
                <p className="text-sm text-base-content/65 mt-1">
                  Mueve la incidencia por el flujo de trabajo sin salir de esta vista.
                </p>
              </div>
              <span
                className={`rounded-full px-3 py-1 text-xs text-white ${STATUS_COLORS[incident.status]}`}
              >
                Estado actual: {STATUS_LABELS[incident.status]}
              </span>
            </div>
            </div>

            {landlordActions.length > 0 ? (
              <div className="grid gap-3 p-5 sm:grid-cols-2 sm:p-6">
                {landlordActions.map((action) => (
                  <button
                    key={action.status}
                    type="button"
                    className="btn btn-primary btn-sm min-h-12 rounded-2xl shadow-sm transition duration-300 hover:-translate-y-0.5"
                    disabled={isSubmitting}
                    onClick={() => void handleLandlordStatusChange(action.status)}
                  >
                    {action.label}
                  </button>
                ))}
              </div>
            ) : (
              <p className="p-5 text-sm text-base-content/60 sm:p-6">
                No hay transiciones manuales disponibles para este estado.
              </p>
            )}
          </article>
        )}

        <article className="rounded-[30px] border border-dashed border-base-300 bg-white p-5 shadow-sm sm:p-6">
          <h2 className="text-lg font-semibold">Chat tecnico</h2>
          <button type="button" className="btn btn-outline btn-sm rounded-full transition hover:-translate-y-0.5">
            Abrir conversacion (proximamente)
          </button>
        </article>

        {canTenantValidate && (
          <article className="rounded-[30px] border border-base-300/70 bg-white p-5 shadow-[0_18px_48px_rgba(15,23,42,0.08)] sm:p-6">
            <h2 className="text-lg font-semibold">Validacion del inquilino</h2>
            <p className="text-sm text-base-content/70">
              Cuando el casero marca la incidencia como resuelta, puedes confirmar o rechazar la
              solucion.
            </p>

            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                className="btn btn-success btn-sm rounded-full px-5 shadow-sm transition hover:-translate-y-0.5"
                disabled={isSubmitting}
                onClick={() => void handleConfirm()}
              >
                Confirmar solucion
              </button>
              <button
                type="button"
                className="btn btn-error btn-outline btn-sm rounded-full px-5 transition hover:-translate-y-0.5"
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
                  className="btn btn-error btn-sm rounded-full px-5 shadow-sm transition hover:-translate-y-0.5"
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

function InfoCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[22px] border border-base-200/80 bg-gradient-to-br from-base-100 to-base-200/70 px-4 py-4 shadow-[0_10px_24px_rgba(15,23,42,0.05)]">
      <p className="text-[11px] uppercase tracking-[0.16em] text-base-content/45">{label}</p>
      <p className="mt-2 font-semibold text-sm leading-6 text-base-content">{value}</p>
    </div>
  )
}
