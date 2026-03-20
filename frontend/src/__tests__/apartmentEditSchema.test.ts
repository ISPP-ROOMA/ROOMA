import { describe, it, expect } from 'vitest'
import { updateApartmentSchema } from '../service/apartments.service'

describe('updateApartmentSchema', () => {
  const validBase = {
    title: 'Piso céntrico',
    description: 'Buen piso para compartir',
    price: 500,
    bills: 'Agua e internet incluidos',
    ubication: 'Madrid Centro',
    state: 'ACTIVE' as const,
    idealTenantProfile: 'Personas tranquilas y responsables',
  }

  it('accepts a valid payload', () => {
    expect(() => updateApartmentSchema.parse(validBase)).not.toThrow()
  })

  it('rejects negative price', () => {
    const payload = { ...validBase, price: -1, bills: '', idealTenantProfile: '' }

    expect(() => updateApartmentSchema.parse(payload)).toThrow()
  })

  it('rejects empty title and description', () => {
    const payload = { ...validBase, title: '', description: '' }

    expect(() => updateApartmentSchema.parse(payload)).toThrow()
  })

  it('rejects idealTenantProfile longer than 1000 characters', () => {
    const longProfile = 'a'.repeat(1001)
    const payload = { ...validBase, idealTenantProfile: longProfile }

    expect(() => updateApartmentSchema.parse(payload)).toThrow()
  })

  it('rejects invalid state value', () => {
    const payload = { ...validBase, state: 'INVALID' as any }

    expect(() => updateApartmentSchema.parse(payload)).toThrow()
  })
})
