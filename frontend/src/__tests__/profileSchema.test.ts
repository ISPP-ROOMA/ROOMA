import { describe, expect, it } from 'vitest'
import { profileSchema } from '../pages/private/profileSchema'

describe('profileSchema', () => {
  const validBase = {
    email: 'test@example.com',
    role: 'TENANT' as const,
    phone: '+34 612345678',
    birthDate: '2000-05-20',
    gender: 'Other' as const,
    smoker: 'false' as const,
    hobbies: 'Leer',
    schedule: 'Trabajo de mañana',
    profession: 'Diseñador',
    password: '',
    confirmPassword: '',
  }

  it('accepts a valid profile payload', () => {
    expect(() => profileSchema.parse(validBase)).not.toThrow()
  })

  it('rejects invalid phone values', () => {
    expect(() => profileSchema.parse({ ...validBase, phone: 'abc' })).toThrow()
  })

  it('rejects mismatched passwords', () => {
    expect(() =>
      profileSchema.parse({ ...validBase, password: '1234', confirmPassword: '9999' })
    ).toThrow()
  })

  it('rejects unsupported roles', () => {
    expect(() => profileSchema.parse({ ...validBase, role: 'SUPERADMIN' })).toThrow()
  })
})
