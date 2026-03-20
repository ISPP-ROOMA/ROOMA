import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import {
  getApartment,
  type Apartment,
  updateApartment,
  updateApartmentSchema,
  type UpdateApartmentPayload,
} from '../../service/apartments.service'
import {
  getApartmentPhotos,
  type ApartmentPhotoDTO,
  type ApartmentRulesDTO,
  updateApartmentRules,
  uploadApartmentImages,
} from '../../service/apartment.service'

const PAGE_CLASS = 'min-h-dvh bg-base-200/50'
const SHELL_CLASS = 'max-w-5xl mx-auto px-4 md:px-8 py-8'

const SECTION_CLASS = 'bg-base-100 rounded-3xl shadow-md p-6 md:p-8 mb-6'
const SECTION_TITLE_CLASS = 'text-lg font-bold mb-3 flex items-center gap-2'

const FIELD_GRID_CLASS = 'grid grid-cols-1 md:grid-cols-2 gap-4'

type RulesState = ApartmentRulesDTO

export default function ApartmentEdit() {
  const { id } = useParams()
  const navigate = useNavigate()

  const apartmentId = id ? Number(id) : NaN

  const [apartment, setApartment] = useState<Apartment | null>(null)
  const [photos, setPhotos] = useState<ApartmentPhotoDTO[]>([])
  const [rules, setRules] = useState<RulesState>({
    permiteMascotas: false,
    permiteFumadores: false,
    fiestasPermitidas: false,
  })
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [photosToUpload, setPhotosToUpload] = useState<File[]>([])
  const [replacePhotos, setReplacePhotos] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<UpdateApartmentPayload>({
    resolver: zodResolver(updateApartmentSchema),
  })

  useEffect(() => {
    if (!id || Number.isNaN(apartmentId)) {
      setLoadError('Identificador de inmueble inválido')
      setIsLoading(false)
      return
    }

    const fetch = async () => {
      try {
        const apt = await getApartment(apartmentId)
        if (!apt) {
          setLoadError('Inmueble no encontrado')
          return
        }
        setApartment(apt)
        reset({
          title: apt.title,
          description: apt.description,
          price: apt.price,
          bills: apt.bills ?? '',
          ubication: apt.ubication,
          state: apt.state as UpdateApartmentPayload['state'],
          idealTenantProfile: apt.idealTenantProfile ?? '',
        })

        const imgs = await getApartmentPhotos(apartmentId)
        setPhotos(imgs)
        // Nota: no hay endpoint de lectura de reglas aún; se inicializan por defecto
      } catch (error) {
        console.error('Error loading apartment for edit', error)
        setLoadError('Error cargando el anuncio')
      } finally {
        setIsLoading(false)
      }
    }

    void fetch()
  }, [apartmentId, id, reset])

  const onSubmit = handleSubmit(async (values) => {
    if (!apartmentId || Number.isNaN(apartmentId)) return
    setIsSaving(true)
    setSaveError(null)

    try {
      await updateApartment(apartmentId, values)
      await updateApartmentRules(apartmentId, rules)

      if (photosToUpload.length > 0) {
        await uploadApartmentImages(apartmentId, photosToUpload, replacePhotos)
      }

      navigate('/apartments/my')
    } catch (error) {
      console.error('Error saving apartment changes', error)
      setSaveError('Error al guardar los cambios. Vuelve a intentarlo.')
    } finally {
      setIsSaving(false)
    }
  })

  const handleFilesChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (!files) {
      setPhotosToUpload([])
      return
    }
    setPhotosToUpload(Array.from(files))
  }

  if (isLoading) {
    return (
      <div className={PAGE_CLASS}>
        <div className={SHELL_CLASS}>
          <p className="text-center text-gray-500 mt-16">Cargando anuncio...</p>
        </div>
      </div>
    )
  }

  if (loadError || !apartment) {
    return (
      <div className={PAGE_CLASS}>
        <div className={SHELL_CLASS}>
          <div className="text-center mt-16">
            <p className="text-red-500 mb-4">{loadError ?? 'Error al cargar el anuncio'}</p>
            <button
              type="button"
              onClick={() => navigate('/apartments/my')}
              className="btn btn-outline"
            >
              Volver a Mis Inmuebles
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className={PAGE_CLASS}>
      <div className={SHELL_CLASS}>
        <header className="mb-6">
          <button
            type="button"
            onClick={() => navigate('/apartments/my')}
            className="btn btn-ghost btn-sm mb-2"
          >
            ← Volver
          </button>
          <h1 className="text-3xl font-bold text-base-content">Editar anuncio</h1>
          <p className="text-gray-500 text-sm mt-1">
            Ajusta la información de tu anuncio para mantenerla actualizada.
          </p>
        </header>

        <form onSubmit={onSubmit} className="space-y-6">
          {/* Datos básicos */}
          <section className={SECTION_CLASS}>
            <h2 className={SECTION_TITLE_CLASS}>
              <span className="w-1 h-5 bg-primary rounded-full" />
              Información principal
            </h2>

            <div className={FIELD_GRID_CLASS}>
              <div className="form-control">
                <label className="label">
                  <span className="label-text">Título</span>
                </label>
                <input
                  type="text"
                  className={`input input-bordered w-full ${
                    errors.title ? 'input-error' : ''
                  }`}
                  {...register('title')}
                />
                {errors.title && (
                  <span className="text-error text-xs mt-1">{errors.title.message}</span>
                )}
              </div>

              <div className="form-control">
                <label className="label">
                  <span className="label-text">Ubicación</span>
                </label>
                <input
                  type="text"
                  className={`input input-bordered w-full ${
                    errors.ubication ? 'input-error' : ''
                  }`}
                  {...register('ubication')}
                />
                {errors.ubication && (
                  <span className="text-error text-xs mt-1">{errors.ubication.message}</span>
                )}
              </div>

              <div className="form-control">
                <label className="label">
                  <span className="label-text">Precio mensual</span>
                </label>
                <div className="join w-full">
                  <input
                    type="number"
                    step="0.01"
                    className={`input input-bordered join-item w-full ${
                      errors.price ? 'input-error' : ''
                    }`}
                    {...register('price', { valueAsNumber: true })}
                  />
                  <span className="join-item px-3 flex items-center bg-base-200 border border-base-300 rounded-r-2xl text-sm text-gray-500">
                    €/mes
                  </span>
                </div>
                {errors.price && (
                  <span className="text-error text-xs mt-1">{errors.price.message}</span>
                )}
              </div>

              <div className="form-control">
                <label className="label">
                  <span className="label-text">Gastos</span>
                </label>
                <input
                  type="text"
                  className={`input input-bordered w-full ${
                    errors.bills ? 'input-error' : ''
                  }`}
                  {...register('bills')}
                />
                {errors.bills && (
                  <span className="text-error text-xs mt-1">{errors.bills.message}</span>
                )}
              </div>
            </div>

            <div className="form-control mt-4">
              <label className="label">
                <span className="label-text">Descripción</span>
              </label>
              <textarea
                rows={4}
                className={`textarea textarea-bordered w-full ${
                  errors.description ? 'textarea-error' : ''
                }`}
                {...register('description')}
              />
              {errors.description && (
                <span className="text-error text-xs mt-1">{errors.description.message}</span>
              )}
            </div>

            <div className="form-control mt-4">
              <label className="label">
                <span className="label-text">Perfil ideal</span>
              </label>
              <textarea
                rows={3}
                className={`textarea textarea-bordered w-full ${
                  errors.idealTenantProfile ? 'textarea-error' : ''
                }`}
                placeholder="Describe el tipo de inquilino ideal (hábitos, estilo de vida, etc.)"
                {...register('idealTenantProfile')}
              />
              {errors.idealTenantProfile && (
                <span className="text-error text-xs mt-1">
                  {errors.idealTenantProfile.message}
                </span>
              )}
            </div>

            <div className="form-control mt-4 max-w-xs">
              <label className="label">
                <span className="label-text">Disponibilidad</span>
              </label>
              <select
                className={`select select-bordered w-full ${
                  errors.state ? 'select-error' : ''
                }`}
                {...register('state')}
              >
                <option value="ACTIVE">Activo</option>
                <option value="MATCHING">En matching</option>
                <option value="CLOSED">Cerrado</option>
              </select>
              {errors.state && (
                <span className="text-error text-xs mt-1">{errors.state.message}</span>
              )}
            </div>
          </section>

          {/* Reglas de convivencia */}
          <section className={SECTION_CLASS}>
            <h2 className={SECTION_TITLE_CLASS}>
              <span className="w-1 h-5 bg-primary rounded-full" />
              Reglas del piso
            </h2>

            <p className="text-xs text-gray-500 mb-3">
              Estas reglas ayudan a alinear expectativas con los interesados.
            </p>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <label className="flex items-center gap-3 bg-base-200 rounded-2xl px-4 py-3 cursor-pointer">
                <input
                  type="checkbox"
                  className="toggle toggle-primary"
                  checked={rules.permiteMascotas}
                  onChange={(e) =>
                    setRules((prev) => ({ ...prev, permiteMascotas: e.target.checked }))
                  }
                />
                <div className="flex flex-col">
                  <span className="font-semibold text-sm">Mascotas</span>
                  <span className="text-xs text-gray-500">
                    {rules.permiteMascotas ? 'Se permiten mascotas' : 'No se permiten mascotas'}
                  </span>
                </div>
              </label>

              <label className="flex items-center gap-3 bg-base-200 rounded-2xl px-4 py-3 cursor-pointer">
                <input
                  type="checkbox"
                  className="toggle toggle-primary"
                  checked={rules.permiteFumadores}
                  onChange={(e) =>
                    setRules((prev) => ({ ...prev, permiteFumadores: e.target.checked }))
                  }
                />
                <div className="flex flex-col">
                  <span className="font-semibold text-sm">Fumadores</span>
                  <span className="text-xs text-gray-500">
                    {rules.permiteFumadores ? 'Se permiten fumadores' : 'No se permiten fumadores'}
                  </span>
                </div>
              </label>

              <label className="flex items-center gap-3 bg-base-200 rounded-2xl px-4 py-3 cursor-pointer">
                <input
                  type="checkbox"
                  className="toggle toggle-primary"
                  checked={rules.fiestasPermitidas}
                  onChange={(e) =>
                    setRules((prev) => ({ ...prev, fiestasPermitidas: e.target.checked }))
                  }
                />
                <div className="flex flex-col">
                  <span className="font-semibold text-sm">Fiestas</span>
                  <span className="text-xs text-gray-500">
                    {rules.fiestasPermitidas ? 'Fiestas permitidas' : 'Fiestas no permitidas'}
                  </span>
                </div>
              </label>
            </div>
          </section>

          {/* Fotos */}
          <section className={SECTION_CLASS}>
            <h2 className={SECTION_TITLE_CLASS}>
              <span className="w-1 h-5 bg-primary rounded-full" />
              Fotos del anuncio
            </h2>

            {photos.length > 0 ? (
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
                {photos.map((photo) => (
                  <div
                    key={photo.id}
                    className="relative aspect-[4/3] rounded-xl overflow-hidden bg-base-200"
                  >
                    <img
                      src={photo.url}
                      alt={`Foto ${photo.id}`}
                      className="w-full h-full object-cover"
                    />
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-gray-500 mb-4">
                Este anuncio aún no tiene fotos. Puedes añadirlas a continuación.
              </p>
            )}

            <div className="form-control">
              <label className="label">
                <span className="label-text">Añadir o reemplazar fotos</span>
              </label>
              <input
                type="file"
                multiple
                accept="image/*"
                onChange={handleFilesChange}
                className="file-input file-input-bordered w-full max-w-md"
              />
              {photosToUpload.length > 0 && (
                <span className="text-xs text-gray-500 mt-1">
                  {photosToUpload.length} archivo(s) seleccionado(s)
                </span>
              )}
            </div>

            <div className="form-control mt-3">
              <label className="label cursor-pointer gap-3">
                <input
                  type="checkbox"
                  className="checkbox checkbox-sm"
                  checked={replacePhotos}
                  onChange={(e) => setReplacePhotos(e.target.checked)}
                />
                <span className="label-text text-sm">
                  Reemplazar completamente el set de fotos actuales
                </span>
              </label>
            </div>
          </section>

          {/* Acciones */}
          <div className="flex flex-col md:flex-row gap-3 justify-end">
            {saveError && (
              <p className="text-error text-sm md:mr-auto md:self-center">{saveError}</p>
            )}
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => navigate('/apartments/my')}
              disabled={isSaving}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className={`btn btn-primary ${isSaving ? 'loading' : ''}`}
              disabled={isSaving}
            >
              Guardar cambios
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

