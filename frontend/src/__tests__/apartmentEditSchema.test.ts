import { describe, it, expect } from 'vitest'
import { updateApartmentSchema } from '../service/apartments.service'

describe('updateApartmentSchema', () => {
  it('accepts a valid payload', () => {
    const payload = {
      title: 'Piso céntrico',
      description: 'Buen piso para compartir',
      price: 500,
      bills: 'Agua e internet incluidos',
      ubication: 'Madrid Centro',
      state: 'ACTIVE',
      idealTenantProfile: 'Personas tranquilas y responsables',
    }

    expect(() => updateApartmentSchema.parse(payload)).not.toThrow()
  })

  it('rejects negative price', () => {
    const payload = {
      title: 'Piso',
      description: 'Desc',
      price: -1,
      bills: '',
      ubication: 'Sevilla',
      state: 'ACTIVE',
      idealTenantProfile: '',
    }

    expect(() => updateApartmentSchema.parse(payload)).toThrow()
  })

  it('rejects empty title and description', () => {
    const payload = {
      title: '',
      description: '',
      price: 400,
      bills: '',
      ubication: 'Valencia',
      state: 'ACTIVE',
      idealTenantProfile: '',
    }

    expect(() => updateApartmentSchema.parse(payload)).toThrow()
  })
})

