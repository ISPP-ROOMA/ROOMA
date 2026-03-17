import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AxiosError } from 'axios'
import { useToast } from '../../../hooks/useToast'
import {
    createIncident,
    getApartmentIncidents,
    type CreateIncidentPayload,
    type IncidentCategory,
    type IncidentDTO,
    type IncidentStatus,
    type IncidentUrgency,
    type IncidentZone,
} from '../../../service/incidents.service'

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
    { label: string; bar: string; badge: string }
> = {
    OPEN: { label: 'Abierta', bar: 'bg-slate-400', badge: 'bg-slate-100 text-slate-700' },
    RECEIVED: { label: 'Recibida', bar: 'bg-blue-500', badge: 'bg-blue-100 text-blue-700' },
    IN_PROGRESS: { label: 'En proceso', bar: 'bg-amber-500', badge: 'bg-amber-100 text-amber-700' },
    TECHNICIAN_NOTIFIED: {
        label: 'Tecnico avisado',
        bar: 'bg-orange-500',
        badge: 'bg-orange-100 text-orange-700',
    },
    RESOLVED: { label: 'Resuelta', bar: 'bg-lime-500', badge: 'bg-lime-100 text-lime-700' },
    CLOSED: { label: 'Cerrada', bar: 'bg-emerald-600', badge: 'bg-emerald-100 text-emerald-700' },
    CLOSED_INACTIVITY: {
        label: 'Cerrada por inactividad',
        bar: 'bg-emerald-700',
        badge: 'bg-emerald-100 text-emerald-700',
    },
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

export default function ApartmentIncidences() {
    const { id } = useParams()
    const navigate = useNavigate()
    const { showToast } = useToast()

    const apartmentId = Number(id)
    const galleryInputRef = useRef<HTMLInputElement | null>(null)
    const cameraInputRef = useRef<HTMLInputElement | null>(null)

    const [isLoading, setIsLoading] = useState(true)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [incidents, setIncidents] = useState<IncidentDTO[]>([])
    const [showForm, setShowForm] = useState(false)
    const [images, setImages] = useState<File[]>([])

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

    return (
        <section className="min-h-[75vh] bg-base-200 px-3 py-6 sm:px-4 sm:py-8">
            <div className="max-w-5xl mx-auto space-y-6">
                <header className="bg-white rounded-2xl shadow-sm p-5 sm:p-6">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                        <div>
                            <h1 className="text-2xl sm:text-3xl font-semibold">Incidencias de la vivienda</h1>
                            <p className="text-sm text-base-content/60 mt-1">
                                Reporta problemas y sigue su estado hasta el cierre.
                            </p>
                        </div>
                        <div className="flex gap-2">
                            <button onClick={() => navigate(-1)} className="btn btn-ghost btn-sm">
                                Volver
                            </button>
                            <button
                                onClick={() => setShowForm((v) => !v)}
                                className="btn btn-primary btn-sm"
                                type="button"
                            >
                                {showForm ? 'Cerrar formulario' : 'Nueva incidencia'}
                            </button>
                        </div>
                    </div>
                </header>

                {showForm && (
                    <article className="bg-white rounded-2xl shadow-sm p-5 sm:p-6 space-y-4">
                        <h2 className="text-xl font-semibold">Formulario de nueva incidencia</h2>

                        <div className="grid gap-4 sm:grid-cols-2">
                            <label className="form-control">
                                <span className="label-text text-sm mb-1">Titulo</span>
                                <input
                                    className="input input-bordered"
                                    value={form.title}
                                    maxLength={100}
                                    onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
                                    placeholder="Ej. Fuga en lavabo del bano"
                                />
                            </label>

                            <label className="form-control">
                                <span className="label-text text-sm mb-1">Categoria</span>
                                <select
                                    className="select select-bordered"
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
                                    className="textarea textarea-bordered min-h-28"
                                    value={form.description}
                                    maxLength={1000}
                                    onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
                                    placeholder="Describe el problema con detalle"
                                />
                            </label>

                            <label className="form-control">
                                <span className="label-text text-sm mb-1">Zona</span>
                                <select
                                    className="select select-bordered"
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
                                        className={`px-3 py-1.5 rounded-full border text-sm transition ${
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
                                <button type="button" className="btn btn-outline btn-sm" onClick={() => galleryInputRef.current?.click()}>
                                    Elegir de galeria
                                </button>
                                <button type="button" className="btn btn-outline btn-sm" onClick={() => cameraInputRef.current?.click()}>
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
                            <button type="button" className="btn btn-ghost btn-sm" onClick={resetForm}>
                                Limpiar
                            </button>
                            <button
                                type="button"
                                className="btn btn-primary btn-sm"
                                disabled={isSubmitting}
                                onClick={() => void handleCreateIncident()}
                            >
                                {isSubmitting ? 'Creando...' : 'Crear incidencia'}
                            </button>
                        </div>
                    </article>
                )}

                {isLoading ? (
                    <p className="text-center py-10 text-base-content/60">Cargando incidencias...</p>
                ) : (
                    <>
                        <article className="space-y-3">
                            <h1 className="text-xl font-semibold">Activas</h1>
                            {activeIncidents.length === 0 ? (
                                <div className="bg-white rounded-xl p-4 text-sm text-base-content/60 shadow-sm">
                                    No hay incidencias activas.
                                </div>
                            ) : (
                                <ul className="space-y-3">
                                    {activeIncidents.map((incident) => {
                                        const meta = STATUS_META[incident.status]
                                        return (
                                            <li key={incident.id}>
                                                <button
                                                    onClick={() => navigate(`/apartments/${apartmentId}/incidences/${incident.id}`)}
                                                    className="w-full bg-white rounded-xl shadow-sm overflow-hidden text-left hover:shadow-md transition"
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
                        </article>

                        <article className="space-y-3 pt-2">
                            <h1 className="text-xl font-semibold">Solucionadas</h1>
                            {closedIncidents.length === 0 ? (
                                <div className="bg-white rounded-xl p-4 text-sm text-base-content/60 shadow-sm">
                                    No hay incidencias cerradas.
                                </div>
                            ) : (
                                <ul className="space-y-3">
                                    {closedIncidents.map((incident) => {
                                        const meta = STATUS_META[incident.status]
                                        return (
                                            <li key={incident.id}>
                                                <button
                                                    onClick={() => navigate(`/apartments/${apartmentId}/incidences/${incident.id}`)}
                                                    className="w-full bg-white rounded-xl shadow-sm overflow-hidden text-left hover:shadow-md transition"
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
                        </article>
                    </>
                )}
            </div>
        </section>
    )
}