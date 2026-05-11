import { zodResolver } from '@hookform/resolvers/zod'
import { AxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import {
  getApartment,
  getMyApartments,
  type Apartment,
  updateApartment,
  updateApartmentSchema,
  type UpdateApartmentPayload,
} from '../../service/apartments.service'
import {
  getApartmentPhotos,
  getApartmentRules,
  type ApartmentPhotoDTO,
  type ApartmentRulesDTO,
  updateApartmentRules,
  uploadApartmentImages,
} from '../../service/apartment.service'
import { normalizePriceInput } from './publish/publishForm'

const PAGE_CLASS = 'h-[calc(100dvh-5rem)] md:h-dvh bg-base-200/40 flex flex-col'
const SHELL_CLASS =
  'max-w-2xl mx-auto px-5 pt-5 pb-6 w-full flex-1 min-h-0 overflow-y-auto'

const HEADER_CARD_CLASS =
  'bg-base-100 shadow-sm rounded-3xl px-4 py-4 mb-4 flex items-center gap-3'
const BACK_BUTTON_CLASS = 'p-1.5 rounded-full hover:bg-base-200 transition'
const HEADER_TEXT_WRAPPER_CLASS = 'flex-1'
const HEADER_META_CLASS =
  'text-xs font-semibold tracking-widest text-gray-400 uppercase'

const SECTION_CLASS = 'bg-base-100 rounded-3xl shadow-md p-6 md:p-8 mb-4'
const SECTION_TITLE_CLASS = 'text-lg font-bold mb-3 flex items-center gap-2'

const FIELD_GRID_CLASS = 'grid grid-cols-1 md:grid-cols-2 gap-4'

const ACTIONS_WRAPPER_CLASS =
  'bg-base-100 border-t border-base-300 rounded-3xl mt-4 px-4 py-3 flex flex-col md:flex-row gap-3 items-stretch md:items-center'
const PRIMARY_BUTTON_CLASS =
  'flex-1 btn bg-primary hover:bg-primary/90 text-primary-content font-semibold text-base py-3.5 rounded-full shadow-md border-0 disabled:opacity-40 disabled:cursor-not-allowed'
const SECONDARY_BUTTON_CLASS = 'btn btn-ghost rounded-full'

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
  const invalidId = !id || Number.isNaN(apartmentId)
  const [isLoading, setIsLoading] = useState(() => !invalidId)
  const [loadError, setLoadError] = useState<string | null>(() => (invalidId ? 'Identificador de inmueble inválido' : null))

  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [photosToUpload, setPhotosToUpload] = useState<File[]>([])
  const [replacePhotos, setReplacePhotos] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<UpdateApartmentPayload>({
    resolver: zodResolver(updateApartmentSchema),
  })

  const { ref: priceRef } = register('price')
  const [priceInput, setPriceInput] = useState('')

  useEffect(() => {
    if (invalidId) {
      return
    }

    const fetch = async () => {
      try {
        const myApartments = await getMyApartments()
        const isOwner = myApartments.some((myApartment) => myApartment.id === apartmentId)
        if (!isOwner) {
          setLoadError('No tienes permiso para editar este anuncio')
          return
        }

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
        setPriceInput(String(apt.price))

        const [imgs, loadedRules] = await Promise.all([
          getApartmentPhotos(apartmentId),
          getApartmentRules(apartmentId),
        ])
        setPhotos(imgs)
        if (loadedRules) {
          setRules(loadedRules)
        }
      } catch (error) {
        console.error('Error loading apartment for edit', error)
        setLoadError('Error cargando el anuncio')
      } finally {
        setIsLoading(false)
      }
    }

    void fetch()
  }, [invalidId, apartmentId, reset])

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
      const err = error as AxiosError<unknown>
      const status = err.response?.status

      if (status === 400) {
        setSaveError('Revisa los campos del formulario.')
      } else if (status === 403) {
        setSaveError('No tienes permiso para editar este anuncio.')
      } else if (status === 404) {
        setSaveError('El anuncio ya no existe o ha sido eliminado.')
      } else {
        setSaveError('Error al guardar los cambios. Vuelve a intentarlo.')
      }
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
              className="btn btn-outline rounded-full"
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
        <header className={HEADER_CARD_CLASS}>
          <button
            type="button"
            onClick={() => navigate('/apartments/my')}
            className={BACK_BUTTON_CLASS}
            aria-label="Volver"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5 text-base-content"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 19l-7-7 7-7"
              />
            </svg>
          </button>

          <div className={HEADER_TEXT_WRAPPER_CLASS}>
            <p className={HEADER_META_CLASS}>Gestión de anuncio</p>
            <h1 className="text-2xl font-bold text-base-content">Editar anuncio</h1>
            <p className="text-gray-500 text-sm mt-1">
              Ajusta la información de tu anuncio para mantenerla actualizada.
            </p>
          </div>
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
                <label className="text-sm font-semibold text-base-content mb-1.5">
                  Título
                </label>
                <input
                  type="text"
                  className={`input input-bordered w-full rounded-xl bg-base-100 focus:outline-primary ${
                    errors.title ? 'input-error' : ''
                  }`}
                  {...register('title')}
                />
                {errors.title && (
                  <span className="text-error text-xs mt-1">{errors.title.message}</span>
                )}
              </div>

              <div className="form-control">
                <label className="text-sm font-semibold text-base-content mb-1.5">
                  Ubicación
                </label>
                <input
                  type="text"
                  className={`input input-bordered w-full rounded-xl bg-base-100 focus:outline-primary ${
                    errors.ubication ? 'input-error' : ''
                  }`}
                  {...register('ubication')}
                />
                {errors.ubication && (
                  <span className="text-error text-xs mt-1">{errors.ubication.message}</span>
                )}
              </div>

              <div className="form-control">
                <label className="text-sm font-semibold text-base-content block mb-2">
                  Precio mensual
                </label>
                <div className="flex items-end gap-3">
                  <input
                    ref={priceRef}
                    type="text"
                    inputMode="decimal"
                    value={priceInput}
                    placeholder="450"
                    className={`input input-bordered text-2xl md:text-3xl font-extrabold text-base-content w-40 text-center rounded-xl bg-base-100 focus:outline-primary tracking-tight ${
                      errors.price ? 'input-error' : ''
                    }`}
                    onChange={(e) => {
                      const normalized = normalizePriceInput(e.target.value)
                      if (normalized === null) return
                      setPriceInput(normalized)
                      const num = parseFloat(normalized)
                      setValue('price', isNaN(num) ? NaN : num)
                    }}
                  />
                  <span className="text-lg font-semibold text-base-content/60 mb-2">
                    € / mes
                  </span>
                </div>
                <p className="text-xs text-base-content/50 mt-1.5">
                  Ajusta el precio para que sea competitivo en tu zona.
                </p>
                {errors.price && (
                  <span className="text-error text-xs mt-1">{errors.price.message}</span>
                )}
              </div>

              <div className="form-control">
                <label className="text-sm font-semibold text-base-content mb-1.5">
                  Gastos
                </label>
                <input
                  type="text"
                  className={`input input-bordered w-full rounded-xl bg-base-100 focus:outline-primary ${
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
              <label className="text-sm font-semibold text-base-content mb-1.5">
                Descripción
              </label>
              <textarea
                rows={4}
                className={`textarea textarea-bordered w-full rounded-xl bg-base-100 focus:outline-primary ${
                  errors.description ? 'textarea-error' : ''
                }`}
                {...register('description')}
              />
              {errors.description && (
                <span className="text-error text-xs mt-1">{errors.description.message}</span>
              )}
            </div>

            <div className="form-control mt-4">
              <label className="text-sm font-semibold text-base-content mb-1.5">
                Perfil ideal
              </label>
              <textarea
                rows={3}
                className={`textarea textarea-bordered w-full rounded-xl bg-base-100 focus:outline-primary ${
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
              <label className="text-sm font-semibold text-base-content mb-1.5">
                Disponibilidad
              </label>
              <select
                className={`select select-bordered w-full rounded-xl bg-base-100 focus:outline-primary ${
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
          <div className={ACTIONS_WRAPPER_CLASS}>
            {saveError && (
              <p className="text-error text-sm md:mr-auto md:self-center">{saveError}</p>
            )}
            <button
              type="button"
              className={SECONDARY_BUTTON_CLASS}
              onClick={() => navigate('/apartments/my')}
              disabled={isSaving}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className={`${PRIMARY_BUTTON_CLASS} ${isSaving ? 'loading' : ''}`}
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
