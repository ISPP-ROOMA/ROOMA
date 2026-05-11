import { describe, expect, it } from 'vitest'
import {
  INITIAL_PUBLISH_FORM_DATA,
  isPostalCodeValid,
  isPublishStepValid,
  parsePrice,
} from '../pages/apartments/publish/publishForm'

describe('publishForm validation', () => {
  it('requires a 5-digit postal code in the location step', () => {
    const payload = {
      ...INITIAL_PUBLISH_FORM_DATA,
      street: 'Calle Mayor 1',
      neighborhood: 'Centro',
      postalCode: '2801',
    }

    expect(isPostalCodeValid(payload.postalCode)).toBe(false)
    expect(isPublishStepValid(0, payload)).toBe(false)
  })

  it('accepts a complete location step with trimmed values', () => {
    const payload = {
      ...INITIAL_PUBLISH_FORM_DATA,
      street: '  Calle Mayor 1  ',
      neighborhood: ' Centro ',
      postalCode: '28013',
    }

    expect(isPublishStepValid(0, payload)).toBe(true)
  })

  it('rejects non-positive prices in the pricing step', () => {
    const payload = {
      ...INITIAL_PUBLISH_FORM_DATA,
      priceInput: '0',
    }

    expect(parsePrice(payload.priceInput)).toBe(0)
    expect(isPublishStepValid(1, payload)).toBe(false)
  })
})
