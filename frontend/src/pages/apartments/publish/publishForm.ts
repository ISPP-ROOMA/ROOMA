export interface PublishFormData {
  street: string
  neighborhood: string
  priceInput: string
  deposit: number
  availableDate: string
  includedBills: string[]
}

export const STEP_TITLES = [
  '¿Dónde está ubicado el piso?',
  'Precio y disponibilidad',
  'Fotos y detalles',
  'Reglas y Perfil Ideal',
]

export const TOTAL_STEPS = STEP_TITLES.length

export const INITIAL_PUBLISH_FORM_DATA: PublishFormData = {
  street: '',
  neighborhood: '',
  priceInput: '450',
  deposit: 1,
  availableDate: '',
  includedBills: [],
}

export const parsePrice = (value: string): number => {
  if (!value.trim()) return 0
  const parsed = Number(value.replace(',', '.'))
  if (Number.isNaN(parsed)) return 0
  const clamped = Math.min(Math.max(parsed, 0), 99999)
  return Math.round(clamped * 100) / 100
}

export const normalizePriceInput = (value: string): string | null => {
  const raw = value.replace(',', '.')
  if (!/^\d*(\.\d{0,2})?$/.test(raw)) return null
  if (raw === '') return ''

  const [intPart = '', decimalPart] = raw.split('.')
  let normalizedInt = intPart.replace(/^0+(?=\d)/, '')
  if (normalizedInt === '') normalizedInt = '0'

  if (Number(normalizedInt) > 99999) {
    normalizedInt = '99999'
  }

  return decimalPart !== undefined ? `${normalizedInt}.${decimalPart}` : normalizedInt
}

export const isPublishStepValid = (step: number, data: PublishFormData): boolean => {
  if (step === 0) {
    return data.street.trim().length > 0 && data.neighborhood.length > 0
  }

  if (step === 1) {
    return parsePrice(data.priceInput) > 0 && data.availableDate.length > 0
  }

  return true
}

export const formatPrice = (value: string): string => {
  const parsed = parsePrice(value)
  return parsed.toLocaleString('es-ES', { maximumFractionDigits: 2 })
}
