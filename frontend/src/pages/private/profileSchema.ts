import { z } from 'zod'

export const profileSchema = z
  .object({
    name: z.string().trim().max(80, { message: 'Máximo 80 caracteres' }).optional(),
    surname: z.string().trim().max(120, { message: 'Máximo 120 caracteres' }).optional(),
    email: z.string().email({ message: 'Email inválido' }),
    role: z.enum(['TENANT', 'LANDLORD', 'ADMIN']),
    phone: z
      .string()
      .trim()
      .regex(/^\+?[0-9\s-]{9,20}$/, { message: 'Teléfono inválido' })
      .optional()
      .or(z.literal('')),
    birthDate: z
      .string()
      .refine((value) => value === '' || !Number.isNaN(Date.parse(value)), {
        message: 'Fecha inválida',
      })
      .optional(),
    gender: z.enum(['Male', 'Female', 'Other', 'Prefer not to say']).optional().or(z.literal('')),
    smoker: z.enum(['true', 'false']).optional().or(z.literal('')),
    hobbies: z.string().trim().max(500, { message: 'Máximo 500 caracteres' }).optional(),
    schedule: z.string().trim().max(500, { message: 'Máximo 500 caracteres' }).optional(),
    profession: z.string().trim().max(120, { message: 'Máximo 120 caracteres' }).optional(),
    password: z.string().min(4, { message: 'Mínimo 4 caracteres' }).optional().or(z.literal('')),
    confirmPassword: z.string().optional().or(z.literal('')),
  })
  .refine(
    (data) => {
      if (data.password && data.password.length > 0) {
        return data.password === data.confirmPassword
      }
      return true
    },
    {
      message: 'Las contraseñas no coinciden',
      path: ['confirmPassword'],
    }
  )

export type ProfileFormValues = z.infer<typeof profileSchema>
