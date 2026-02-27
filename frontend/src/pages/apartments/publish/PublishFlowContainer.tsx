import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createApartment } from '../../../service/apartments.service'
import { useAuthStore } from '../../../store/authStore'
import StepLocation from './StepLocation'
import StepPricing from './StepPricing'
import StepPhotos from './StepPhotos'
import StepRules from './StepRules'
import {
  formatPrice,
  INITIAL_PUBLISH_FORM_DATA,
  isPublishStepValid,
  parsePrice,
  TOTAL_STEPS,
  type PublishFormData,
} from './publishForm'

const WRAPPER_CLASS = 'min-h-dvh bg-base-200/40 flex flex-col'
const HEADER_CLASS = 'bg-base-100 shadow-sm sticky top-0 z-30'
const HEADER_INNER_CLASS = 'max-w-2xl mx-auto px-5 pt-5 pb-4 flex flex-col gap-3'
const TOP_ROW_CLASS = 'flex items-center gap-4'
const BACK_BUTTON_CLASS = 'p-1.5 rounded-full hover:bg-base-200 transition'
const STEP_META_CLASS = 'text-xs font-semibold tracking-widest text-gray-400 uppercase'
const STEP_TITLE_CLASS = 'text-lg font-bold text-base-content leading-tight mt-0.5'
const PROGRESS_TRACK_CLASS = 'w-full h-1.5 bg-base-300 rounded-full overflow-hidden'
const PROGRESS_BAR_CLASS = 'h-full bg-primary rounded-full transition-all duration-500 ease-out'
const CONTENT_CLASS = 'flex-1 overflow-y-auto px-5 py-6 max-w-2xl mx-auto w-full'
const FOOTER_CLASS = 'bg-base-100 border-t border-base-300 sticky bottom-0 z-30'
const FOOTER_INNER_CLASS = 'max-w-2xl mx-auto px-5 py-4 flex items-center gap-4'
const PRICE_HINT_CLASS =
  'text-xs font-bold text-base-content/60 uppercase tracking-wide whitespace-nowrap'
const CONTINUE_BUTTON_CLASS =
  'flex-1 btn bg-primary hover:bg-primary/90 text-primary-content font-semibold text-base py-3.5 rounded-full shadow-md border-0 disabled:opacity-40 disabled:cursor-not-allowed'

export default function PublishFlowContainer() {
  const navigate = useNavigate()
  const { token } = useAuthStore()
  const [currentStep, setCurrentStep] = useState(0)
  const [formData, setFormData] = useState<PublishFormData>(INITIAL_PUBLISH_FORM_DATA)

  const progress = ((currentStep + 1) / TOTAL_STEPS) * 100

  const updateFields = (fields: Partial<PublishFormData>) => {
    setFormData((prev) => ({ ...prev, ...fields }))
  }

  const parsedPrice = parsePrice(formData.priceInput)
  const isLastStep = currentStep === TOTAL_STEPS - 1
  const canContinue = isPublishStepValid(currentStep, formData)

  const handleNext = async () => {
    if (currentStep < TOTAL_STEPS - 1) {
      setCurrentStep((s) => s + 1)
      return
    }

    if (!token) {
      navigate('/login')
      return
    }

    try {
      const billsText = formData.includedBills.length
        ? formData.includedBills.join(', ')
        : 'No incluidos'

      await createApartment({
        title: `Piso en ${formData.neighborhood || 'Sin barrio'}`,
        description: `Disponible desde ${formData.availableDate || 'fecha por definir'}. Fianza: ${formData.deposit} mes(es).`,
        price: parsedPrice,
        bills: billsText,
        ubication: formData.street || 'Sin dirección',
        state: 'ACTIVE',
      },
      formData.images
    )

      navigate('/apartments/my')
    } catch (err) {
      console.error('Error creating apartment', err)
    }
  }

  const handleBack = () => {
    if (currentStep === 0) {
      navigate('/apartments/my')
    } else {
      setCurrentStep((s) => s - 1)
    }
  }

  const getStepTitle = () => {
    switch (currentStep) {
      case 0:
        return '¿Dónde está ubicado el piso?'
      case 1:
        return 'Precio y disponibilidad'
      case 2:
        return 'Fotos y detalles'
      case 3:
        return 'Reglas y Perfil Ideal'
      default:
        return ''
    }
  }

  const renderStep = () => {
    switch (currentStep) {
      case 0:
        return <StepLocation data={formData} updateFields={updateFields} />
      case 1:
        return <StepPricing data={formData} updateFields={updateFields} />
      case 2:
        return <StepPhotos
                  images={formData.images}
                  onChangeImages={(images) => setFormData((prev) => ({ ...prev, images }))}
                />
      case 3:
        return <StepRules />
      default:
        return null
    }
  }

  const stepContent = renderStep()
  const stepTitle = getStepTitle()

  const stepIndicator = `Paso ${currentStep + 1} de ${TOTAL_STEPS}`

  return (
    <div className={WRAPPER_CLASS}>
      <header className={HEADER_CLASS}>
        <div className={HEADER_INNER_CLASS}>
          <div className={TOP_ROW_CLASS}>
            <button onClick={handleBack} className={BACK_BUTTON_CLASS} aria-label="Volver">
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

            <div className="flex-1 text-center">
              <p className={STEP_META_CLASS}>{stepIndicator}</p>
              <h2 className={STEP_TITLE_CLASS}>{stepTitle}</h2>
            </div>

            <div className="w-8" />
          </div>

          <div className={PROGRESS_TRACK_CLASS}>
            <div className={PROGRESS_BAR_CLASS} style={{ width: `${progress}%` }} />
          </div>
        </div>
      </header>

      <section className={CONTENT_CLASS}>{stepContent}</section>

      <footer className={FOOTER_CLASS}>
        <div className={FOOTER_INNER_CLASS}>
          {currentStep === 1 && (
            <p className={PRICE_HINT_CLASS}>Estimado {formatPrice(formData.priceInput)}€ / mes</p>
          )}

          <button
            onClick={() => handleNext()}
            disabled={!canContinue}
            className={CONTINUE_BUTTON_CLASS}
          >
            {isLastStep ? 'Finalizar' : 'Siguiente'}
          </button>
        </div>
      </footer>
    </div>
  )
}
