import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AxiosError } from 'axios'
import { useToast } from '../../../hooks/useToast'
import {
    createIncident,
    getApartmentIncidents,
    landlordUpdateIncidentStatus,
    type CreateIncidentPayload,
    type IncidentCategory,
    type IncidentDTO,
    type IncidentStatus,
    type IncidentUrgency,
    type IncidentZone,
} from '../../../service/incidents.service'
import { useAuthStore } from '../../../store/authStore'

const MAX_PHOTOS = 5

const CATEGORY_OPTIONS: Array<{ value: IncidentCategory; label: string }> = [
    { value: 'PLUMBING', label: 'Fontaneria' },
    { value: 'ELECTRICITY', label: 'Electricidad' },
    { value: 'APPLIANCES', label: 'Electrodomesticos' },
    { value: 'LOCKSMITH', label: 'Cerrajeria' },
    { value: 'PAINT_WALLS', label: 'Pintura/Paredes' },
    { value: 'CLIMATE', label: 'Climatizacion' },
    { value: 'PESTS', label: 'Plagas' },
    { value: 'OTHER', label: 'Otro' },
]

const ZONE_OPTIONS: Array<{ value: IncidentZone; label: string }> = [
    { value: 'KITCHEN', label: 'Cocina' },
    { value: 'BATHROOM', label: 'Bano' },
    { value: 'LIVING_ROOM', label: 'Salon' },
    { value: 'BEDROOM', label: 'Dormitorio' },
    { value: 'COMMON_AREAS', label: 'Zonas comunes' },
    { value: 'EXTERIOR', label: 'Exterior' },
]

const URGENCY_OPTIONS: Array<{ value: IncidentUrgency; label: string; className: string }> = [
    { value: 'LOW', label: 'Baja', className: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
    {
        value: 'MEDIUM',
        label: 'Media',
        className: 'bg-amber-100 text-amber-700 border-amber-200',
    },
    {
        value: 'HIGH',
        label: 'Alta',
        className: 'bg-orange-100 text-orange-700 border-orange-200',
    },
    { value: 'URGENT', label: 'Urgente', className: 'bg-red-100 text-red-700 border-red-200' },
]

const STATUS_META: Record<
    IncidentStatus,
    { label: string; bar: string; badge: string; column: string }
> = {
    OPEN: {
        label: 'Abierta',
        bar: 'bg-slate-400',
        badge: 'bg-slate-100 text-slate-700',
        column: 'bg-slate-50 border-slate-200',
    },
    RECEIVED: {
        label: 'Recibida',
        bar: 'bg-blue-500',
        badge: 'bg-blue-100 text-blue-700',
        column: 'bg-blue-50 border-blue-200',
    },
    IN_PROGRESS: {
        label: 'En proceso',
        bar: 'bg-amber-500',
        badge: 'bg-amber-100 text-amber-700',
        column: 'bg-amber-50 border-amber-200',
    },
    TECHNICIAN_NOTIFIED: {
        label: 'Tecnico avisado',
        bar: 'bg-orange-500',
        badge: 'bg-orange-100 text-orange-700',
        column: 'bg-orange-50 border-orange-200',
    },
    RESOLVED: {
        label: 'Resuelta',
        bar: 'bg-lime-500',
        badge: 'bg-lime-100 text-lime-700',
        column: 'bg-lime-50 border-lime-200',
    },
    CLOSED: {
        label: 'Cerrada',
        bar: 'bg-emerald-600',
        badge: 'bg-emerald-100 text-emerald-700',
        column: 'bg-emerald-50 border-emerald-200',
    },
    CLOSED_INACTIVITY: {
        label: 'Cerrada por inactividad',
        bar: 'bg-emerald-700',
        badge: 'bg-emerald-100 text-emerald-700',
        column: 'bg-emerald-50 border-emerald-200',
    },
}

const KANBAN_STATUSES: IncidentStatus[] = [
    'OPEN',
    'RECEIVED',
    'IN_PROGRESS',
    'TECHNICIAN_NOTIFIED',
    'RESOLVED',
]

const LANDLORD_TRANSITIONS: Partial<Record<IncidentStatus, IncidentStatus[]>> = {
    OPEN: ['RECEIVED'],
    RECEIVED: ['IN_PROGRESS'],
    IN_PROGRESS: ['TECHNICIAN_NOTIFIED', 'RESOLVED'],
    TECHNICIAN_NOTIFIED: ['IN_PROGRESS', 'RESOLVED'],
}

const formatDate = (value: string) => {
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

const getReporterLabel = (incident: IncidentDTO) => {
    return incident.tenantEmail
}

const getDropStatusAtPoint = (x: number, y: number): IncidentStatus | null => {
    const directMatch = document
        .elementFromPoint(x, y)
        ?.closest<HTMLElement>('[data-kanban-status]')
    const directStatus = directMatch?.dataset.kanbanStatus

    if (KANBAN_STATUSES.includes(directStatus as IncidentStatus)) {
        return directStatus as IncidentStatus
    }

    const sections = Array.from(document.querySelectorAll<HTMLElement>('[data-kanban-status]'))
    let closestStatus: IncidentStatus | null = null
    let closestDistance = Number.POSITIVE_INFINITY

    sections.forEach((section) => {
        const status = section.dataset.kanbanStatus as IncidentStatus | undefined
        if (!status || !KANBAN_STATUSES.includes(status)) return

        const rect = section.getBoundingClientRect()
        const centerX = rect.left + rect.width / 2
        const centerY = rect.top + rect.height / 2
        const distance = Math.hypot(centerX - x, centerY - y)

        if (distance < closestDistance) {
            closestDistance = distance
            closestStatus = status
        }
    })

    return closestStatus
}

export default function ApartmentIncidences() {
    const { id } = useParams()
    const navigate = useNavigate()
    const { showToast } = useToast()
    const { role } = useAuthStore()

    const apartmentId = Number(id)
    const galleryInputRef = useRef<HTMLInputElement | null>(null)
    const cameraInputRef = useRef<HTMLInputElement | null>(null)
    const suppressClickRef = useRef(false)

    const [isLoading, setIsLoading] = useState(true)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [incidents, setIncidents] = useState<IncidentDTO[]>([])
    const [showForm, setShowForm] = useState(false)
    const [images, setImages] = useState<File[]>([])
    const [movingIncidentId, setMovingIncidentId] = useState<number | null>(null)
    const [dragState, setDragState] = useState<{
        incident: IncidentDTO
        pointerId: number
        startX: number
        startY: number
        currentX: number
        currentY: number
        offsetX: number
        offsetY: number
        sourceStatus: IncidentStatus
        hoveredStatus: IncidentStatus | null
        dragging: boolean
    } | null>(null)

    const [form, setForm] = useState<Omit<CreateIncidentPayload, 'photos'>>({
        title: '',
        description: '',
        category: 'OTHER',
        zone: 'COMMON_AREAS',
        urgency: 'MEDIUM',
    })

    const loadIncidents = async () => {
        if (!Number.isFinite(apartmentId)) {
            setIsLoading(false)
            return
        }

        setIsLoading(true)
        const data = await getApartmentIncidents(apartmentId)
        setIncidents(data)
        setIsLoading(false)
    }

    useEffect(() => {
        void loadIncidents()
    }, [apartmentId])

    const activeIncidents = useMemo(
        () => incidents.filter((i) => i.status !== 'CLOSED' && i.status !== 'CLOSED_INACTIVITY'),
        [incidents]
    )

    const closedIncidents = useMemo(
        () => incidents.filter((i) => i.status === 'CLOSED' || i.status === 'CLOSED_INACTIVITY'),
        [incidents]
    )

    const kanbanColumns = useMemo(
        () =>
            KANBAN_STATUSES.map((status) => ({
                status,
                meta: STATUS_META[status],
                incidents: incidents.filter((incident) => incident.status === status),
            })),
        [incidents]
    )

    const landlordSummary = useMemo(
        () => [
            {
                label: 'Activas',
                value: activeIncidents.length,
                helper: 'Pendientes de seguimiento',
                accent: 'from-sky-500/20 to-blue-500/5',
                dot: 'bg-sky-500',
            },
            {
                label: 'Cerradas',
                value: closedIncidents.length,
                helper: 'Ya completadas',
                accent: 'from-emerald-500/18 to-emerald-500/5',
                dot: 'bg-emerald-500',
            },
            {
                label: 'Urgentes',
                value: activeIncidents.filter((incident) => incident.urgency === 'URGENT').length,
                helper: 'Requieren prioridad',
                accent: 'from-rose-500/18 to-orange-500/5',
                dot: 'bg-rose-500',
            },
        ],
        [activeIncidents, closedIncidents]
    )

    const handleFilesPicked = (files: FileList | null) => {
        if (!files) return
        const picked = Array.from(files).filter((f) => f.type.startsWith('image/'))
        const next = [...images, ...picked].slice(0, MAX_PHOTOS)
        setImages(next)
    }

    const removeImage = (index: number) => {
        setImages((prev) => prev.filter((_, i) => i !== index))
    }

    const resetForm = () => {
        setForm({
            title: '',
            description: '',
            category: 'OTHER',
            zone: 'COMMON_AREAS',
            urgency: 'MEDIUM',
        })
        setImages([])
    }

    const handleCreateIncident = async () => {
        const title = form.title.trim()
        const description = form.description.trim()

        if (!title) {
            showToast('El titulo es obligatorio', 'error')
            return
        }
        if (title.length > 100) {
            showToast('El titulo no puede superar 100 caracteres', 'error')
            return
        }
        if (!description) {
            showToast('La descripcion es obligatoria', 'error')
            return
        }
        if (description.length > 1000) {
            showToast('La descripcion no puede superar 1000 caracteres', 'error')
            return
        }
        if (images.length > MAX_PHOTOS) {
            showToast('Solo puedes adjuntar hasta 5 fotos', 'error')
            return
        }

        setIsSubmitting(true)
        try {
            await createIncident(apartmentId, {
                title,
                description,
                category: form.category,
                zone: form.zone,
                urgency: form.urgency,
            }, images)
            showToast('Incidencia creada correctamente', 'success')
            resetForm()
            setShowForm(false)
            await loadIncidents()
        } catch (error) {
            console.error(error)
            const message =
                error instanceof AxiosError
                    ? (error.response?.data as { message?: string })?.message ?? 'No se pudo crear la incidencia'
                    : 'No se pudo crear la incidencia'
            showToast(message, 'error')
        } finally {
            setIsSubmitting(false)
        }
    }

    const moveIncidentToStatus = async (incident: IncidentDTO, nextStatus: IncidentStatus) => {
        if (incident.status === nextStatus) return

        setMovingIncidentId(incident.id)
        try {
            await landlordUpdateIncidentStatus(apartmentId, incident.id, { status: nextStatus })
            showToast('Estado actualizado correctamente', 'success')
            await loadIncidents()
        } catch (error) {
            console.error(error)
            showToast('No se pudo mover la incidencia', 'error')
        } finally {
            setMovingIncidentId(null)
        }
    }

    useEffect(() => {
        if (!dragState) return

        const handlePointerMove = (event: PointerEvent) => {
            if (event.pointerId !== dragState.pointerId) return

            const nextX = event.clientX
            const nextY = event.clientY
            const movedEnough =
                Math.abs(nextX - dragState.startX) > 10 || Math.abs(nextY - dragState.startY) > 10

            if (movedEnough) {
                event.preventDefault()
            }

            setDragState((prev) =>
                prev
                    ? {
                          ...prev,
                          currentX: nextX,
                          currentY: nextY,
                          dragging: prev.dragging || movedEnough,
                          hoveredStatus:
                              prev.dragging || movedEnough
                                  ? getDropStatusAtPoint(nextX, nextY) ?? prev.hoveredStatus
                                  : prev.hoveredStatus,
                      }
                    : prev
            )
        }

        const handlePointerUp = (event: PointerEvent) => {
            if (event.pointerId !== dragState.pointerId) return

            const allowedTargets = LANDLORD_TRANSITIONS[dragState.sourceStatus] ?? []
            const finalHoveredStatus = dragState.dragging
                ? getDropStatusAtPoint(event.clientX, event.clientY) ?? dragState.hoveredStatus
                : null

            if (dragState.dragging) {
                suppressClickRef.current = true
                window.setTimeout(() => {
                    suppressClickRef.current = false
                }, 220)
            }

            if (
                dragState.dragging &&
                finalHoveredStatus &&
                finalHoveredStatus !== dragState.sourceStatus &&
                allowedTargets.includes(finalHoveredStatus)
            ) {
                void moveIncidentToStatus(dragState.incident, finalHoveredStatus)
            } else if (dragState.dragging && finalHoveredStatus && finalHoveredStatus !== dragState.sourceStatus) {
                showToast('Suelta la incidencia en una fase compatible', 'warning')
            }

            document.body.style.userSelect = ''
            document.body.style.webkitUserSelect = ''
            document.documentElement.style.userSelect = ''
            document.documentElement.style.webkitUserSelect = ''
            setDragState(null)
        }

        window.addEventListener('pointermove', handlePointerMove)
        window.addEventListener('pointerup', handlePointerUp)
        window.addEventListener('pointercancel', handlePointerUp)

        return () => {
            document.body.style.userSelect = ''
            document.body.style.webkitUserSelect = ''
            document.documentElement.style.userSelect = ''
            document.documentElement.style.webkitUserSelect = ''
            window.removeEventListener('pointermove', handlePointerMove)
            window.removeEventListener('pointerup', handlePointerUp)
            window.removeEventListener('pointercancel', handlePointerUp)
        }
    }, [dragState, showToast])

    useEffect(() => {
        if (!dragState?.dragging) return

        document.body.style.userSelect = 'none'
        document.body.style.webkitUserSelect = 'none'
        document.documentElement.style.userSelect = 'none'
        document.documentElement.style.webkitUserSelect = 'none'

        return () => {
            document.body.style.userSelect = ''
            document.body.style.webkitUserSelect = ''
            document.documentElement.style.userSelect = ''
            document.documentElement.style.webkitUserSelect = ''
        }
    }, [dragState?.dragging])

    const startDraggingIncident = (incident: IncidentDTO, event: React.PointerEvent<HTMLElement>) => {
        if (movingIncidentId === incident.id) return

        event.preventDefault()
        event.stopPropagation()
        event.currentTarget.setPointerCapture?.(event.pointerId)

        const rect = event.currentTarget.getBoundingClientRect()
        setDragState({
            incident,
            pointerId: event.pointerId,
            startX: event.clientX,
            startY: event.clientY,
            currentX: event.clientX,
            currentY: event.clientY,
            offsetX: event.clientX - rect.left,
            offsetY: event.clientY - rect.top,
            sourceStatus: incident.status,
            hoveredStatus: null,
            dragging: false,
        })
    }

    const renderIncidentCard = (incident: IncidentDTO, compact = false) => {
        const currentMeta = STATUS_META[incident.status]
        const allowedTransitions = LANDLORD_TRANSITIONS[incident.status] ?? []
        const isBusy = movingIncidentId === incident.id

        return (
            <div
                key={incident.id}
                role="button"
                tabIndex={0}
                onClick={() => {
                    if (suppressClickRef.current) return
                    if (dragState?.dragging) return
                    navigate(`/apartments/${apartmentId}/incidences/${incident.id}`)
                }}
                onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        if (suppressClickRef.current) return
                        if (dragState?.dragging) return
                        navigate(`/apartments/${apartmentId}/incidences/${incident.id}`)
                    }
                }}
                className={`group w-full select-none rounded-[26px] border border-base-200/80 bg-[linear-gradient(180deg,rgba(255,255,255,1),rgba(248,250,252,0.98))] p-4 text-left shadow-[0_16px_36px_rgba(15,23,42,0.08)] transition duration-300 ease-out active:scale-[0.99] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30 ${
                    isBusy
                        ? 'opacity-60'
                        : dragState?.incident.id === incident.id && dragState.dragging
                            ? 'opacity-10'
                            : 'hover:-translate-y-1 hover:border-primary/15 hover:shadow-[0_24px_52px_rgba(15,23,42,0.14)]'
                }`}
            >
                <div className={`h-1.5 rounded-full ${currentMeta.bar}`} />
                <div className="space-y-3 pt-3">
                    <div className="flex items-start justify-between gap-2">
                        <div className="space-y-1">
                            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-base-content/40">
                                {currentMeta.label}
                            </p>
                            <h4 className="max-w-[70%] text-[1.02rem] font-semibold leading-tight text-base-content transition-colors group-hover:text-primary">
                                {incident.title}
                            </h4>
                        </div>
                        <div className="flex items-center gap-2">
                            <span
                                className={`shrink-0 rounded-full px-2 py-1 text-[11px] ${
                                    URGENCY_OPTIONS.find((option) => option.value === incident.urgency)?.className ??
                                    'bg-base-200 text-base-content/70 border-base-200'
                                }`}
                            >
                                {URGENCY_OPTIONS.find((option) => option.value === incident.urgency)?.label ?? incident.urgency}
                            </span>
                            {role === 'LANDLORD' && (
                                <button
                                    type="button"
                                    aria-label="Arrastrar incidencia"
                                    onPointerDown={(event) => startDraggingIncident(incident, event)}
                                    onClick={(event) => event.stopPropagation()}
                                    className="flex h-9 w-9 touch-none select-none cursor-grab items-center justify-center rounded-full border border-base-300 bg-base-100 text-base-content/55 shadow-sm transition hover:-translate-y-0.5 hover:border-primary/20 hover:text-primary active:scale-95 active:cursor-grabbing"
                                >
                                    <span className="text-base leading-none">::</span>
                                </button>
                            )}
                        </div>
                    </div>
                    <p className={`${compact ? 'line-clamp-4' : 'line-clamp-3'} text-sm leading-6 text-base-content/70`}>
                        {incident.description}
                    </p>
                    <div className="flex flex-wrap gap-2">
                        <span className="rounded-full bg-base-200/80 px-2.5 py-1 text-[11px] font-medium text-base-content/65">
                            {ZONE_OPTIONS.find((option) => option.value === incident.zone)?.label ?? incident.zone}
                        </span>
                        <span className="rounded-full bg-base-200/80 px-2.5 py-1 text-[11px] font-medium text-base-content/65">
                            {getReporterLabel(incident)}
                        </span>
                    </div>
                    <div className="space-y-1 text-xs text-base-content/55">
                        <p>Abierta: {formatDate(incident.createdAt)}</p>
                        <p>{compact ? 'Toca para ver el detalle o usa el asa para arrastrar.' : 'Accede al detalle para consultar el historial completo.'}</p>
                    </div>
                    {role === 'LANDLORD' && allowedTransitions.length > 0 && (
                        <div className="grid grid-cols-1 gap-2 pt-1 sm:grid-cols-2">
                            {allowedTransitions.map((status) => (
                                <button
                                    key={`${incident.id}-${status}`}
                                    type="button"
                                    disabled={isBusy}
                                    onClick={(event) => {
                                        event.stopPropagation()
                                        
                                        void moveIncidentToStatus(incident, status)
                                    }}
                                    className="min-h-11 rounded-2xl border border-primary/20 bg-gradient-to-r from-primary/10 to-primary/5 px-3 py-2 text-xs font-semibold text-primary shadow-sm transition duration-300 hover:-translate-y-0.5 hover:border-primary/30 hover:from-primary/15 hover:to-primary/10 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
                                >
                                    {STATUS_META[status].label}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        )
    }

    return (
        <section className="min-h-[75vh] bg-[radial-gradient(circle_at_top_left,rgba(20,184,166,0.08),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(15,23,42,0.06),transparent_24%),var(--fallback-b2,oklch(var(--b2)))] px-3 py-6 sm:px-4 sm:py-8">
            <div className="max-w-5xl mx-auto space-y-6">
                <header className="overflow-hidden rounded-[32px] border border-base-300/70 bg-white shadow-[0_20px_55px_rgba(15,23,42,0.08)]">
                    <div className="bg-[radial-gradient(circle_at_top_left,rgba(20,184,166,0.14),transparent_38%),linear-gradient(135deg,rgba(255,255,255,1),rgba(248,250,252,0.92))] px-5 py-6 sm:px-7">
                        <div className="flex flex-col items-start gap-5">
                            <div className="w-full">
                                <div className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/5 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-primary/80">
                                    Incidence board
                                </div>
                                <h1 className="mt-4 text-[2rem] leading-tight sm:text-[2.45rem] sm:leading-tight font-semibold">
                                    Incidencias de la vivienda
                                </h1>
                                <p className="mt-3 max-w-xl text-sm leading-6 text-base-content/60">
                                {role === 'LANDLORD'
                                    ? 'Controla el estado de cada incidencia, prioriza los casos urgentes y mueve el flujo sin perder contexto.'
                                    : 'Reporta problemas y sigue su estado hasta el cierre.'}
                                </p>
                                {role === 'LANDLORD' && (
                                    <div className="mt-4 flex flex-wrap gap-2">
                                        <span className="rounded-full border border-base-300 bg-white/85 px-3 py-1.5 text-xs text-base-content/60 shadow-sm">
                                            {activeIncidents.length} activas
                                        </span>
                                        <span className="rounded-full border border-base-300 bg-white/85 px-3 py-1.5 text-xs text-base-content/60 shadow-sm">
                                            {closedIncidents.length} cerradas
                                        </span>
                                        <span className="rounded-full border border-base-300 bg-white/85 px-3 py-1.5 text-xs text-base-content/60 shadow-sm">
                                            {activeIncidents.filter((incident) => incident.urgency === 'URGENT').length} urgentes
                                        </span>
                                    </div>
                                )}
                            </div>
                            <div className="flex w-full flex-wrap gap-3">
                                <button
                                    onClick={() => navigate(-1)}
                                    className="btn btn-outline btn-sm min-h-11 rounded-full border-base-content/20 bg-white/80 px-5 shadow-sm backdrop-blur transition hover:-translate-y-0.5 hover:bg-white"
                                >
                                    Volver
                                </button>
                                {role === 'TENANT' && (
                                    <button
                                        onClick={() => setShowForm((v) => !v)}
                                        className="btn btn-primary btn-sm min-h-11 rounded-full px-5 shadow-sm transition hover:-translate-y-0.5"
                                        type="button"
                                    >
                                        {showForm ? 'Cerrar formulario' : 'Nueva incidencia'}
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>
                </header>

                {role === 'TENANT' && showForm && (
                    <article className="overflow-hidden rounded-[30px] border border-base-300/70 bg-white shadow-[0_18px_45px_rgba(15,23,42,0.07)]">
                        <div className="border-b border-base-200/80 bg-gradient-to-r from-primary/8 via-base-100 to-base-100 px-5 py-5 sm:px-6">
                            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70">
                                Nuevo reporte
                            </p>
                            <h2 className="mt-2 text-xl font-semibold">Formulario de nueva incidencia</h2>
                            <p className="mt-2 max-w-lg text-sm leading-6 text-base-content/60">
                                Describe el problema con claridad para que el casero pueda priorizarlo y gestionarlo cuanto antes.
                            </p>
                        </div>
                        <div className="space-y-4 p-5 sm:p-6">

                        <div className="grid gap-4 sm:grid-cols-2">
                            <label className="form-control">
                                <span className="label-text text-sm mb-1">Titulo</span>
                                <input
                                    className="input input-bordered transition focus:border-primary/30 focus:outline-none focus:ring-2 focus:ring-primary/10"
                                    value={form.title}
                                    maxLength={100}
                                    onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
                                    placeholder="Ej. Fuga en lavabo del bano"
                                />
                            </label>

                            <label className="form-control">
                                <span className="label-text text-sm mb-1">Categoria</span>
                                <select
                                    className="select select-bordered transition focus:border-primary/30 focus:outline-none focus:ring-2 focus:ring-primary/10"
                                    value={form.category}
                                    onChange={(e) =>
                                        setForm((prev) => ({ ...prev, category: e.target.value as IncidentCategory }))
                                    }
                                >
                                    {CATEGORY_OPTIONS.map((option) => (
                                        <option key={option.value} value={option.value}>
                                            {option.label}
                                        </option>
                                    ))}
                                </select>
                            </label>

                            <label className="form-control sm:col-span-2">
                                <span className="label-text text-sm mb-1">Descripcion</span>
                                <textarea
                                    className="textarea textarea-bordered min-h-28 transition focus:border-primary/30 focus:outline-none focus:ring-2 focus:ring-primary/10"
                                    value={form.description}
                                    maxLength={1000}
                                    onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
                                    placeholder="Describe el problema con detalle"
                                />
                            </label>

                            <label className="form-control">
                                <span className="label-text text-sm mb-1">Zona</span>
                                <select
                                    className="select select-bordered transition focus:border-primary/30 focus:outline-none focus:ring-2 focus:ring-primary/10"
                                    value={form.zone}
                                    onChange={(e) => setForm((prev) => ({ ...prev, zone: e.target.value as IncidentZone }))}
                                >
                                    {ZONE_OPTIONS.map((option) => (
                                        <option key={option.value} value={option.value}>
                                            {option.label}
                                        </option>
                                    ))}
                                </select>
                            </label>
                        </div>

                        <div className="space-y-2">
                            <p className="text-sm font-medium">Urgencia</p>
                            <div className="flex flex-wrap gap-2">
                                {URGENCY_OPTIONS.map((option) => (
                                    <button
                                        key={option.value}
                                        type="button"
                                        onClick={() => setForm((prev) => ({ ...prev, urgency: option.value }))}
                                        className={`rounded-full border px-3 py-1.5 text-sm transition duration-300 hover:-translate-y-0.5 ${
                                            form.urgency === option.value
                                                ? option.className
                                                : 'bg-white text-base-content/70 border-base-200'
                                        }`}
                                    >
                                        {option.label}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="space-y-3">
                            <p className="text-sm font-medium">Fotos (max. {MAX_PHOTOS})</p>
                            <div className="flex flex-wrap gap-2">
                                <input
                                    ref={galleryInputRef}
                                    className="hidden"
                                    type="file"
                                    accept="image/*"
                                    multiple
                                    onChange={(e) => {
                                        handleFilesPicked(e.target.files)
                                        e.target.value = ''
                                    }}
                                />
                                <input
                                    ref={cameraInputRef}
                                    className="hidden"
                                    type="file"
                                    accept="image/*"
                                    capture="environment"
                                    onChange={(e) => {
                                        handleFilesPicked(e.target.files)
                                        e.target.value = ''
                                    }}
                                />
                                <button type="button" className="btn btn-outline btn-sm rounded-full transition hover:-translate-y-0.5" onClick={() => galleryInputRef.current?.click()}>
                                    Elegir de galeria
                                </button>
                                <button type="button" className="btn btn-outline btn-sm rounded-full transition hover:-translate-y-0.5" onClick={() => cameraInputRef.current?.click()}>
                                    Tomar foto
                                </button>
                            </div>

                            {images.length > 0 && (
                                <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                                    {images.map((file, index) => (
                                        <div key={`${file.name}-${index}`} className="relative rounded-xl overflow-hidden bg-base-200 aspect-square">
                                            <img
                                                src={URL.createObjectURL(file)}
                                                alt={file.name}
                                                className="w-full h-full object-cover"
                                            />
                                            <button
                                                type="button"
                                                className="btn btn-xs btn-circle absolute top-1 right-1"
                                                onClick={() => removeImage(index)}
                                            >
                                                ✕
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>

                        <div className="flex justify-end gap-2">
                            <button type="button" className="btn btn-ghost btn-sm rounded-full" onClick={resetForm}>
                                Limpiar
                            </button>
                            <button
                                type="button"
                                className="btn btn-primary btn-sm rounded-full px-5 shadow-sm transition hover:-translate-y-0.5"
                                disabled={isSubmitting}
                                onClick={() => void handleCreateIncident()}
                            >
                                {isSubmitting ? 'Creando...' : 'Crear incidencia'}
                            </button>
                        </div>
                        </div>
                    </article>
                )}

                {isLoading ? (
                    <p className="text-center py-10 text-base-content/60">Cargando incidencias...</p>
                ) : role === 'LANDLORD' ? (
                    <>
                        <article className="overflow-hidden rounded-[34px] border border-base-300/80 bg-white shadow-[0_26px_65px_rgba(15,23,42,0.1)]">
                            <div className="border-b border-base-200/80 bg-[radial-gradient(circle_at_top_left,rgba(20,184,166,0.14),transparent_32%),linear-gradient(135deg,rgba(255,255,255,1),rgba(248,250,252,0.94))] px-5 py-6 sm:px-6">
                                <div className="grid gap-5 lg:grid-cols-[1.2fr_0.8fr] lg:items-end">
                                    <div>
                                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary/70">
                                            Panel del casero
                                        </p>
                                        <h2 className="mt-2 text-2xl font-semibold leading-tight text-base-content sm:text-[2rem]">
                                            Seguimiento rapido
                                        </h2>
                                        <p className="mt-3 max-w-lg text-sm leading-6 text-base-content/60">
                                            Vista pensada para resolver rapido: prioriza, mueve estados y entra al detalle solo cuando realmente haga falta.
                                        </p>
                                    </div>
                                    <div className="rounded-[26px] border border-base-200/80 bg-white/90 p-4 shadow-[0_14px_34px_rgba(15,23,42,0.08)] backdrop-blur">
                                        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-base-content/40">
                                            Modo de uso
                                        </p>
                                        <p className="mt-2 text-sm leading-6 text-base-content/65">
                                            Toca una tarjeta para abrirla. Usa el asa lateral para arrastrarla o los cambios rapidos si vas con prisa.
                                        </p>
                                    </div>
                                </div>
                            </div>
                            <div className="grid grid-cols-1 gap-3 p-5 sm:grid-cols-3 sm:p-6">
                                {landlordSummary.map((item) => (
                                    <div
                                        key={item.label}
                                        className={`rounded-[26px] border border-base-200/80 bg-gradient-to-br ${item.accent} px-4 py-4 shadow-[0_14px_30px_rgba(15,23,42,0.06)] transition duration-300 hover:-translate-y-1 hover:shadow-[0_18px_38px_rgba(15,23,42,0.1)]`}
                                    >
                                        <div className="mb-3 flex items-center justify-between">
                                            <span className={`h-2.5 w-2.5 rounded-full ${item.dot}`} />
                                            <span className="text-[10px] font-semibold uppercase tracking-[0.16em] text-base-content/40">
                                                {item.label}
                                            </span>
                                        </div>
                                        <p className="text-4xl font-semibold leading-none text-base-content">
                                            {item.value}
                                        </p>
                                        <p className="mt-2 text-xs text-base-content/55">
                                            {item.helper}
                                        </p>
                                    </div>
                                ))}
                            </div>
                        </article>

                        <article className="space-y-4">
                            <div className="flex flex-col gap-3 rounded-[28px] border border-base-300/80 bg-white/70 p-4 shadow-sm sm:flex-row sm:items-end sm:justify-between sm:p-5">
                                <div>
                                    <h2 className="text-2xl font-semibold leading-tight">Tablero de gestion</h2>
                                    <p className="mt-2 max-w-xl text-sm leading-6 text-base-content/60">
                                        En movil el flujo se entiende como una ruta vertical clara. En escritorio mantiene una lectura de tablero por columnas.
                                    </p>
                                </div>
                                <span className="inline-flex w-fit rounded-full border border-base-300 bg-base-100 px-3 py-1.5 text-xs font-medium text-base-content/60">
                                    Toca para abrir. Usa el asa para mover.
                                </span>
                            </div>

                            <div className="md:hidden space-y-4">
                                <div className="rounded-[30px] border border-base-300/80 bg-white p-4 shadow-[0_18px_40px_rgba(15,23,42,0.08)]">
                                    <div className="flex flex-col gap-4">
                                        <div className="flex items-start justify-between gap-3">
                                            <div>
                                            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70">
                                                Vista rapida
                                            </p>
                                            <h3 className="mt-2 text-lg font-semibold text-base-content">
                                                Estados del proceso
                                            </h3>
                                            <p className="mt-1 text-sm leading-6 text-base-content/55">
                                                Revisa de un vistazo en que punto esta cada incidencia.
                                            </p>
                                            </div>
                                            <span className="inline-flex w-fit rounded-full border border-base-300 bg-base-100 px-3 py-1 text-xs text-base-content/60">
                                                Flujo vertical
                                            </span>
                                        </div>
                                        <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 snap-x snap-mandatory">
                                            {kanbanColumns.map((column) => (
                                                <div
                                                    key={`workflow-${column.status}`}
                                                    className="min-w-[96px] snap-start rounded-[22px] border border-base-200/70 bg-[linear-gradient(180deg,rgba(255,255,255,1),rgba(248,250,252,0.96))] px-3 py-3 text-center shadow-sm transition duration-300 hover:-translate-y-0.5 hover:bg-base-100"
                                                >
                                                    <span className={`mx-auto mb-2 block h-2.5 w-2.5 rounded-full ${column.meta.bar}`} />
                                                    <p className="min-h-8 text-[10px] font-semibold uppercase leading-4 tracking-[0.12em] text-base-content/45">
                                                        {column.meta.label.replace('Tecnico avisado', 'Tecnico')}
                                                    </p>
                                                    <p className="mt-2 text-lg font-semibold leading-none text-base-content">
                                                        {column.incidents.length}
                                                    </p>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                </div>

                                <div className="space-y-4">
                                    {kanbanColumns.map(({ status, meta, incidents: columnIncidents }) => (
                                        <section
                                            key={status}
                                            data-kanban-status={status}
                                            className={`rounded-[30px] border p-4 space-y-3 shadow-[0_14px_32px_rgba(15,23,42,0.06)] transition ${
                                                dragState?.hoveredStatus === status && dragState.dragging
                                                    ? 'ring-2 ring-primary/30 shadow-[0_12px_30px_rgba(13,148,136,0.18)]'
                                                    : ''
                                            } ${meta.column}`}
                                        >
                                            <div className="flex items-center justify-between gap-2">
                                                <div>
                                                    <h3 className="font-semibold">{meta.label}</h3>
                                                    <p className="text-xs text-base-content/55">
                                                        {columnIncidents.length} incidencia
                                                        {columnIncidents.length === 1 ? '' : 's'}
                                                    </p>
                                                </div>
                                                <span className={`h-2.5 w-2.5 rounded-full ${meta.bar}`} />
                                            </div>

                                            {columnIncidents.length === 0 ? (
                                                <div className="rounded-[22px] border border-dashed border-base-300 bg-white/85 px-4 py-5 text-center">
                                                    <p className="text-sm font-medium text-base-content/65">Sin incidencias en esta fase</p>
                                                    <p className="mt-1 text-xs text-base-content/45">Cuando haya actividad aparecerá aquí en el flujo.</p>
                                                </div>
                                            ) : (
                                                <div className="space-y-3">
                                                    {columnIncidents.map((incident) =>
                                                        renderIncidentCard(incident, true)
                                                    )}
                                                </div>
                                            )}
                                        </section>
                                    ))}
                                </div>
                            </div>

                            <div className="hidden gap-4 overflow-x-auto pb-2 pr-1 snap-x snap-mandatory md:grid md:grid-cols-5 md:overflow-visible">
                                {kanbanColumns.map(({ status, meta, incidents: columnIncidents }) => (
                                    <section
                                        key={status}
                                        data-kanban-status={status}
                                        className={`min-w-[84vw] snap-center rounded-[30px] border p-4 space-y-3 shadow-[0_16px_34px_rgba(15,23,42,0.06)] transition md:min-w-0 ${
                                            dragState?.hoveredStatus === status && dragState.dragging
                                                ? 'ring-2 ring-primary/30 shadow-[0_12px_30px_rgba(13,148,136,0.18)]'
                                                : ''
                                        } ${meta.column}`}
                                    >
                                        <div className="flex items-center justify-between gap-2">
                                            <div>
                                                <h3 className="font-semibold">{meta.label}</h3>
                                                <p className="text-xs text-base-content/55">
                                                    {columnIncidents.length} incidencia
                                                    {columnIncidents.length === 1 ? '' : 's'}
                                                </p>
                                            </div>
                                            <span className={`h-2.5 w-2.5 rounded-full ${meta.bar}`} />
                                        </div>

                                        {columnIncidents.length === 0 ? (
                                            <div className="rounded-[22px] border border-dashed border-base-300 bg-white/85 px-4 py-5 text-center">
                                                <p className="text-sm font-medium text-base-content/65">Sin incidencias en esta fase</p>
                                                <p className="mt-1 text-xs text-base-content/45">La siguiente incidencia aparecerá aquí automáticamente.</p>
                                            </div>
                                        ) : (
                                            <div className="space-y-3">
                                                {columnIncidents.map((incident) => renderIncidentCard(incident))}
                                            </div>
                                        )}
                                    </section>
                                ))}
                            </div>
                        </article>

                        <article className="rounded-[30px] border border-base-300/80 bg-white/80 p-4 shadow-[0_16px_38px_rgba(15,23,42,0.06)] backdrop-blur sm:p-5">
                            <div className="space-y-4">
                            <div className="flex flex-wrap items-center justify-between gap-3">
                                <div>
                                    <h2 className="text-xl font-semibold">Archivo</h2>
                                    <p className="mt-1 text-sm text-base-content/55">
                                        Historial de incidencias cerradas o finalizadas por inactividad.
                                    </p>
                                </div>
                                <span className="rounded-full border border-base-300 bg-white px-3 py-1 text-xs text-base-content/55">
                                    {closedIncidents.length} registradas
                                </span>
                            </div>
                            {closedIncidents.length === 0 ? (
                                <div className="rounded-[26px] border border-base-300/80 bg-white p-5 text-center shadow-sm">
                                    <p className="text-sm font-medium text-base-content/65">No hay incidencias cerradas</p>
                                    <p className="mt-1 text-xs text-base-content/45">Cuando el flujo termine, quedarán archivadas aquí.</p>
                                </div>
                            ) : (
                                <ul className="space-y-3">
                                    {closedIncidents.map((incident) => {
                                        const meta = STATUS_META[incident.status]
                                        return (
                                            <li key={incident.id}>
                                                <button
                                                    type="button"
                                                    onClick={() => navigate(`/apartments/${apartmentId}/incidences/${incident.id}`)}
                                                    className="w-full overflow-hidden rounded-[24px] border border-base-300/70 bg-white text-left shadow-[0_14px_32px_rgba(15,23,42,0.06)] transition hover:-translate-y-0.5 hover:shadow-[0_18px_42px_rgba(15,23,42,0.1)]"
                                                >
                                                    <div className={`h-1.5 ${meta.bar}`} />
                                                    <div className="p-4 sm:p-5 space-y-2">
                                                        <div className="flex flex-wrap items-center justify-between gap-2">
                                                            <h3 className="font-semibold text-base sm:text-lg">{incident.title}</h3>
                                                            <span className={`text-xs px-2 py-1 rounded-full ${meta.badge}`}>
                                                                {meta.label}
                                                            </span>
                                                        </div>
                                                        <p className="text-sm text-base-content/70 line-clamp-2">{incident.description}</p>
                                                        <p className="text-xs text-base-content/55">
                                                            Reportada por: {getReporterLabel(incident)}
                                                        </p>
                                                        <p className="text-xs text-base-content/50">
                                                            Cerrada: {formatDate(incident.closedAt ?? incident.updatedAt)}
                                                        </p>
                                                    </div>
                                                </button>
                                            </li>
                                        )
                                    })}
                                </ul>
                            )}
                            </div>
                        </article>
                    </>
                ) : (
                    <>
                        <article className="rounded-[30px] border border-base-300/80 bg-white/80 p-4 shadow-[0_16px_38px_rgba(15,23,42,0.06)] backdrop-blur sm:p-5">
                            <div className="space-y-3">
                            <div className="flex flex-wrap items-center justify-between gap-3">
                                <div>
                                    <h1 className="text-xl font-semibold">Activas</h1>
                                    <p className="mt-1 text-sm text-base-content/55">
                                        Sigue el estado de las incidencias abiertas y consulta su detalle.
                                    </p>
                                </div>
                                <span className="rounded-full border border-base-300 bg-white px-3 py-1 text-xs text-base-content/55">
                                    {activeIncidents.length} activas
                                </span>
                            </div>
                            {activeIncidents.length === 0 ? (
                                <div className="rounded-[26px] border border-base-300/80 bg-white p-5 text-center shadow-sm">
                                    <p className="text-sm font-medium text-base-content/65">No hay incidencias activas</p>
                                    <p className="mt-1 text-xs text-base-content/45">Las incidencias pendientes aparecerán aquí con su estado y detalle.</p>
                                </div>
                            ) : (
                                <ul className="space-y-3">
                                    {activeIncidents.map((incident) => {
                                        const meta = STATUS_META[incident.status]
                                        return (
                                            <li key={incident.id}>
                                                <button
                                                    onClick={() => navigate(`/apartments/${apartmentId}/incidences/${incident.id}`)}
                                                    className="w-full overflow-hidden rounded-[24px] border border-base-300/70 bg-white text-left shadow-[0_14px_32px_rgba(15,23,42,0.06)] transition hover:-translate-y-0.5 hover:shadow-[0_18px_42px_rgba(15,23,42,0.1)]"
                                                >
                                                    <div className={`h-1.5 ${meta.bar}`} />
                                                    <div className="p-4 sm:p-5 space-y-2">
                                                        <div className="flex flex-wrap items-center justify-between gap-2">
                                                            <h3 className="font-semibold text-base sm:text-lg">{incident.title}</h3>
                                                            <span className={`text-xs px-2 py-1 rounded-full ${meta.badge}`}>
                                                                {meta.label}
                                                            </span>
                                                        </div>
                                                        <p className="text-sm text-base-content/70 line-clamp-2">{incident.description}</p>
                                                        <p className="text-xs text-base-content/55">
                                                            Reportada por: {getReporterLabel(incident)}
                                                        </p>
                                                        <p className="text-xs text-base-content/50">Abierta: {formatDate(incident.createdAt)}</p>
                                                    </div>
                                                </button>
                                            </li>
                                        )
                                    })}
                                </ul>
                            )}
                            </div>
                        </article>

                        <article className="rounded-[30px] border border-base-300/80 bg-white/80 p-4 shadow-[0_16px_38px_rgba(15,23,42,0.06)] backdrop-blur sm:p-5">
                            <div className="space-y-3">
                            <div className="flex flex-wrap items-center justify-between gap-3">
                                <div>
                                    <h1 className="text-xl font-semibold">Solucionadas</h1>
                                    <p className="mt-1 text-sm text-base-content/55">
                                        Incidencias ya cerradas o validadas dentro del flujo de resolucion.
                                    </p>
                                </div>
                                <span className="rounded-full border border-base-300 bg-white px-3 py-1 text-xs text-base-content/55">
                                    {closedIncidents.length} cerradas
                                </span>
                            </div>
                            {closedIncidents.length === 0 ? (
                                <div className="rounded-[26px] border border-base-300/80 bg-white p-5 text-center shadow-sm">
                                    <p className="text-sm font-medium text-base-content/65">No hay incidencias cerradas</p>
                                    <p className="mt-1 text-xs text-base-content/45">Cuando una incidencia se cierre, la verás resumida en este bloque.</p>
                                </div>
                            ) : (
                                <ul className="space-y-3">
                                    {closedIncidents.map((incident) => {
                                        const meta = STATUS_META[incident.status]
                                        return (
                                            <li key={incident.id}>
                                                <button
                                                    onClick={() => navigate(`/apartments/${apartmentId}/incidences/${incident.id}`)}
                                                    className="w-full overflow-hidden rounded-[24px] border border-base-300/70 bg-white text-left shadow-[0_14px_32px_rgba(15,23,42,0.06)] transition hover:-translate-y-0.5 hover:shadow-[0_18px_42px_rgba(15,23,42,0.1)]"
                                                >
                                                    <div className={`h-1.5 ${meta.bar}`} />
                                                    <div className="p-4 sm:p-5 space-y-2">
                                                        <div className="flex flex-wrap items-center justify-between gap-2">
                                                            <h3 className="font-semibold text-base sm:text-lg">{incident.title}</h3>
                                                            <span className={`text-xs px-2 py-1 rounded-full ${meta.badge}`}>
                                                                {meta.label}
                                                            </span>
                                                        </div>
                                                        <p className="text-sm text-base-content/70 line-clamp-2">{incident.description}</p>
                                                        <p className="text-xs text-base-content/55">
                                                            Reportada por: {getReporterLabel(incident)}
                                                        </p>
                                                        <p className="text-xs text-base-content/50">
                                                            Cerrada: {formatDate(incident.closedAt ?? incident.updatedAt)}
                                                        </p>
                                                    </div>
                                                </button>
                                            </li>
                                        )
                                    })}
                                </ul>
                            )}
                            </div>
                        </article>
                    </>
                )}
            </div>
            {dragState?.dragging && (
                <div className="pointer-events-none fixed inset-0 z-[70]">
                    <div
                        className="absolute w-[min(22rem,78vw)] rounded-2xl border border-primary/20 bg-white p-4 text-left shadow-[0_24px_60px_rgba(15,23,42,0.22)]"
                        style={{
                            left: dragState.currentX - dragState.offsetX,
                            top: dragState.currentY - dragState.offsetY,
                        }}
                    >
                        <div className={`h-1.5 rounded-full ${STATUS_META[dragState.incident.status].bar}`} />
                        <div className="pt-3 space-y-2">
                            <div className="flex items-start justify-between gap-2">
                                <h4 className="font-semibold leading-tight">{dragState.incident.title}</h4>
                                <span
                                    className={`shrink-0 rounded-full px-2 py-1 text-[11px] ${
                                        URGENCY_OPTIONS.find((option) => option.value === dragState.incident.urgency)?.className ??
                                        'bg-base-200 text-base-content/70 border-base-200'
                                    }`}
                                >
                                    {URGENCY_OPTIONS.find((option) => option.value === dragState.incident.urgency)?.label ??
                                        dragState.incident.urgency}
                                </span>
                            </div>
                            <p className="line-clamp-2 text-sm text-base-content/70">
                                {dragState.incident.description}
                            </p>
                            <p className="text-xs text-base-content/50">
                                Modo arrastre activo. Suelta en una columna compatible para moverla.
                            </p>
                        </div>
                    </div>
                </div>
            )}
        </section>
    )
}

