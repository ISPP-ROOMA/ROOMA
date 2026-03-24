import { type APIRequestContext, type Page } from '@playwright/test'
import { expect, test } from '../fixtures/auth.fixture'
import { buildDeviceId, E2E_ENV } from '../fixtures/testEnv'

type AuthApiResponse = {
  token: string
  role: string
  userId: number
  deviceId: string
}

type ApartmentApiResponse = {
  id: number
  title: string
  ubication: string
}

type RegisterApiResponse = {
  token?: string
  role?: string
  userId?: number
}

type ReviewableUserApiResponse = {
  id: number
  email: string
  role: string
  hasReviewedYou: boolean
  youReviewedThem: boolean
}

const uniqueApartmentTitle = (suffix: string) =>
  `E2E ${suffix} ${Date.now()}-${Math.floor(Math.random() * 100000)}`

const uniqueTenantEmail = () =>
  `tenant.reviews.${Date.now()}.${Math.floor(Math.random() * 100000)}@test.com`

const loginByApi = async (
  request: APIRequestContext,
  email: string,
  scope: string
): Promise<AuthApiResponse> => {
  const deviceId = buildDeviceId(`pw-reviews-${scope}`)
  const response = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
    data: {
      email,
      password: E2E_ENV.password,
      deviceId,
    },
  })

  expect(response.ok(), `Login API falló para ${email}. Status ${response.status()}`).toBeTruthy()
  const data = (await response.json()) as Omit<AuthApiResponse, 'deviceId'>
  return { ...data, deviceId }
}

const registerTenantByApi = async (
  request: APIRequestContext,
  email: string,
  scope: string
): Promise<RegisterApiResponse> => {
  const response = await request.post(`${E2E_ENV.apiUrl}/auth/register`, {
    data: {
      email,
      password: E2E_ENV.password,
      deviceId: buildDeviceId(`pw-register-${scope}`),
      role: 'TENANT',
    },
  })

  expect(response.ok(), `No se pudo registrar tenant temporal ${email}.`).toBeTruthy()
  return (await response.json()) as RegisterApiResponse
}

const logoutByApi = async (request: APIRequestContext, deviceId: string) => {
  const response = await request.post(`${E2E_ENV.apiUrl}/auth/logout`, {
    data: { deviceId },
  })

  expect(response.status()).toBeGreaterThanOrEqual(200)
  expect(response.status()).toBeLessThan(500)
}

const deleteOwnProfileByApi = async (request: APIRequestContext, token: string) => {
  const response = await request.delete(`${E2E_ENV.apiUrl}/users/profile`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  expect(response.status()).toBeGreaterThanOrEqual(200)
  expect(response.status()).toBeLessThan(500)
}

const cleanupTenantByEmail = async (request: APIRequestContext, email: string) => {
  const login = await loginByApi(request, email, 'tenant-cleanup')
  await logoutByApi(request, login.deviceId)
  await deleteOwnProfileByApi(request, login.token)
}

const createApartmentByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  title: string
): Promise<ApartmentApiResponse> => {
  const response = await request.post(`${E2E_ENV.apiUrl}/apartments`, {
    headers: {
      Authorization: `Bearer ${landlordToken}`,
    },
    multipart: {
      data: {
        name: 'apartment.json',
        mimeType: 'application/json',
        buffer: Buffer.from(
          JSON.stringify({
            title,
            description: `Descripción ${title}`,
            price: 700,
            bills: 'agua, luz',
            ubication: 'Calle Reviews 10',
            state: 'ACTIVE',
          })
        ),
      },
    },
  })

  expect(response.ok(), `No se pudo crear inmueble para reviews. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentApiResponse
}

const addMemberByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  apartmentId: number,
  userId: number
) => {
  const response = await request.post(`${E2E_ENV.apiUrl}/apartments/${apartmentId}/members`, {
    headers: {
      Authorization: `Bearer ${landlordToken}`,
    },
    data: {
      userId,
      joinDate: new Date().toISOString().slice(0, 10),
    },
  })

  expect(response.ok(), `No se pudo añadir miembro ${userId} al piso ${apartmentId}.`).toBeTruthy()

  const body = (await response.json()) as { id?: number }
  expect(body.id, `No se recibió id del miembro para piso ${apartmentId}.`).toBeTruthy()
  return body.id as number
}

const deleteApartmentByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  apartmentId: number
) => {
  const response = await request.delete(`${E2E_ENV.apiUrl}/apartments/${apartmentId}`, {
    headers: {
      Authorization: `Bearer ${landlordToken}`,
    },
  })

  expect(response.status()).toBeGreaterThanOrEqual(200)
  expect(response.status()).toBeLessThan(500)
}

const getReviewableUsersByApi = async (
  request: APIRequestContext,
  token: string,
  apartmentId: number
): Promise<ReviewableUserApiResponse[]> => {
  const response = await request.get(`${E2E_ENV.apiUrl}/reviews/reviewable/${apartmentId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  expect(response.ok(), `No se pudo obtener reviewables para piso ${apartmentId}.`).toBeTruthy()
  return (await response.json()) as ReviewableUserApiResponse[]
}

const createTenantReviewByApi = async (
  request: APIRequestContext,
  tenantToken: string,
  apartmentId: number,
  reviewedUserId: number,
  rating = 4,
  comment = 'Review E2E tenant'
) => {
  const response = await request.post(`${E2E_ENV.apiUrl}/reviews/tenant`, {
    headers: {
      Authorization: `Bearer ${tenantToken}`,
    },
    data: {
      reviewedUserId,
      apartmentId,
      rating,
      comment,
    },
  })

  expect(response.ok(), `No se pudo crear reseña tenant. Status ${response.status()}`).toBeTruthy()
}

const createLandlordReviewByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  apartmentId: number,
  reviewedUserId: number,
  rating = 5,
  comment = 'Review E2E landlord'
) => {
  const response = await request.post(`${E2E_ENV.apiUrl}/reviews/landlord`, {
    headers: {
      Authorization: `Bearer ${landlordToken}`,
    },
    data: {
      reviewedUserId,
      apartmentId,
      rating,
      comment,
    },
  })

  expect(response.ok(), `No se pudo crear reseña landlord. Status ${response.status()}`).toBeTruthy()
}

const loginUi = async (page: Page, email: string) => {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(E2E_ENV.password)
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login$/)
}

const rateCategory = async (page: Page, categoryLabel: string, stars: number) => {
  const row = page.locator('div.flex.items-center.justify-between').filter({ hasText: categoryLabel }).first()
  await expect(row, `No se encontró la categoría ${categoryLabel}.`).toBeVisible()
  await row.locator('button').nth(stars - 1).click()
}

const fillAllCategories = async (page: Page, labels: string[]) => {
  for (const label of labels) {
    await rateCategory(page, label, 4)
  }
}

const TENANT_TO_LANDLORD_CATEGORIES = [
  'Amabilidad',
  'Responsabilidad',
  'Comunicacion',
  'Mantenimiento',
  'Relacion calidad-precio',
]

const LANDLORD_TO_TENANT_CATEGORIES = [
  'Limpieza',
  'Respeto',
  'Puntualidad en pagos',
  'Comunicacion',
  'Responsabilidad',
]

const TENANT_TO_TENANT_CATEGORIES = [
  'Limpieza',
  'Respeto',
  'Comunicacion',
  'Convivencia',
  'Responsabilidad',
]

test.describe('HU-33 / RF-36 / RF-37', () => {
  test('Muestra aviso de valoración en Home y permite posponer con Más tarde', async ({ page }) => {
    await page.route('**/api/reviews/pending', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            apartmentId: 99901,
            apartmentTitle: 'Piso HU-33',
            apartmentUbication: 'Calle Demo 12',
            pendingUsers: [
              {
                id: 101,
                email: 'landlord.mock@test.com',
                role: 'LANDLORD',
                hasReviewedYou: false,
                youReviewedThem: false,
              },
            ],
          },
        ]),
      })
    })

    await loginUi(page, E2E_ENV.tenantEmail)
    await page.goto('/')

    await expect(page.getByText('¡Tu contrato ha terminado!')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Más tarde' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Dejar valoración' })).toBeVisible()

    await page.getByRole('button', { name: 'Más tarde' }).click()
    await expect(page.getByText('¡Tu contrato ha terminado!')).not.toBeVisible()
  })

  test('Formulario de inquilino valida categorías y muestra confirmación tras envío', async ({
    page,
    request,
    context,
  }) => {
    const landlord = await loginByApi(request, E2E_ENV.landlordEmail, 'hu33-landlord')
    const tempTenantEmail = uniqueTenantEmail()
    await registerTenantByApi(request, tempTenantEmail, 'hu33-tenant-register')
    const tenant = await loginByApi(request, tempTenantEmail, 'hu33-tenant')

    const apartment = await createApartmentByApi(request, landlord.token, uniqueApartmentTitle('HU33'))
    await addMemberByApi(request, landlord.token, apartment.id, tenant.userId)

    try {
      await loginUi(page, tempTenantEmail)
      const reviewables = await getReviewableUsersByApi(request, tenant.token, apartment.id)
      const landlordReviewable = reviewables.find((u) => u.role === 'LANDLORD')
      expect(landlordReviewable, 'No apareció ningún LANDLORD valorable en el piso.').toBeDefined()

      await page.goto(`/reviews/new/${apartment.id}/form/${landlordReviewable?.id ?? landlord.userId}`)

      await expect(page.getByRole('heading', { name: /Valora a tu (casero|companero\/a)/ })).toBeVisible()
      await expect(page.getByText('Comentario')).toBeVisible()

      await page.getByRole('button', { name: 'Enviar valoracion' }).click()
      await expect(page.getByText('Por favor, valora todas las categorias.')).toBeVisible()

      const usesLandlordCategories = (await page.getByText('Amabilidad').count()) > 0
      await fillAllCategories(
        page,
        usesLandlordCategories ? TENANT_TO_LANDLORD_CATEGORIES : TENANT_TO_TENANT_CATEGORIES
      )
      await page.locator('textarea[placeholder*="opcional"]').fill('Casero correcto y comunicación fluida.')
      await page.getByRole('button', { name: 'Enviar valoracion' }).click()

      await expect(page.getByRole('heading', { name: 'Resena enviada' })).toBeVisible()
      await expect(page.getByText('Se publicara cuando ambas partes hayan valorado o tras 30 dias.')).toBeVisible()
    } finally {
      await deleteApartmentByApi(request, landlord.token, apartment.id)
      await cleanupTenantByEmail(request, tempTenantEmail)
      await context.clearCookies()
    }
  })
})

test.describe('HU-34 / RF-38 / RNF-15', () => {
  test('Tras valorar en un solo sentido, aparece estado pendiente con candado en Pendientes', async ({
    page,
    request,
    context,
  }) => {
    const tempTenantEmail = uniqueTenantEmail()
    await registerTenantByApi(request, tempTenantEmail, 'hu34-tenant-register')
    await loginByApi(request, tempTenantEmail, 'hu34-tenant')

    await page.route('**/api/reviews/pending', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            apartmentId: 99801,
            apartmentTitle: 'Piso HU-34',
            apartmentUbication: 'Calle Demo Pendiente 12',
            pendingUsers: [
              {
                id: 5001,
                email: 'landlord.pending@test.com',
                role: 'LANDLORD',
                hasReviewedYou: false,
                youReviewedThem: true,
              },
            ],
          },
        ]),
      })
    })

    try {
      await loginUi(page, tempTenantEmail)
      await page.goto('/my-reviews')
      await page.getByRole('button', { name: 'Pendientes' }).click()

      await expect(page.getByText('Tu valoración está esperando a ser revelada')).toBeVisible()
      await expect(
        page.getByText(/esperando a ser revelada|visible cuando ambas partes valoren o tras 30 días/)
      ).toBeVisible()
    } finally {
      await cleanupTenantByEmail(request, tempTenantEmail)
      await context.clearCookies()
    }
  })
})

test.describe('HU-36 / RF-36 / RF-37', () => {
  test('Casero puede valorar a inquilino con categorías específicas', async ({ page, request, context }) => {
    const landlord = await loginByApi(request, E2E_ENV.landlordEmail, 'hu36-landlord')
    const tempTenantEmail = uniqueTenantEmail()
    await registerTenantByApi(request, tempTenantEmail, 'hu36-tenant-register')
    const tenant = await loginByApi(request, tempTenantEmail, 'hu36-tenant')

    const apartment = await createApartmentByApi(request, landlord.token, uniqueApartmentTitle('HU36'))
    await addMemberByApi(request, landlord.token, apartment.id, tenant.userId)

    try {
      await loginUi(page, E2E_ENV.landlordEmail)
      await page.goto(`/reviews/new/${apartment.id}/form/${tenant.userId}`)

      await expect(page.getByRole('heading', { name: 'Valora a tu inquilino' })).toBeVisible()

      for (const label of LANDLORD_TO_TENANT_CATEGORIES) {
        await expect(page.getByText(label).first()).toBeVisible()
      }

      await fillAllCategories(page, LANDLORD_TO_TENANT_CATEGORIES)
      await page.locator('textarea[placeholder*="opcional"]').fill('Buen inquilino, sin incidencias.')
      await page.getByRole('button', { name: 'Enviar valoracion' }).click()

      await expect(page.getByRole('heading', { name: 'Resena enviada' })).toBeVisible()
    } finally {
      await deleteApartmentByApi(request, landlord.token, apartment.id)
      await cleanupTenantByEmail(request, tempTenantEmail)
      await context.clearCookies()
    }
  })
})

test.describe('HU-38 / RF-36 / RF-37', () => {
  test('Compañero puede valorar a otro compañero con categorías de convivencia', async ({
    page,
    request,
    context,
  }) => {
    const landlord = await loginByApi(request, E2E_ENV.landlordEmail, 'hu38-landlord')
    const tenantAEmail = uniqueTenantEmail()
    await registerTenantByApi(request, tenantAEmail, 'hu38-tenant-a-register')
    const tenantA = await loginByApi(request, tenantAEmail, 'hu38-tenant-a')

    const tempTenantEmail = uniqueTenantEmail()
    await registerTenantByApi(request, tempTenantEmail, 'hu38-temp-tenant')
    const tenantB = await loginByApi(request, tempTenantEmail, 'hu38-tenant-b')

    const apartment = await createApartmentByApi(request, landlord.token, uniqueApartmentTitle('HU38'))
    await addMemberByApi(request, landlord.token, apartment.id, tenantA.userId)
    await addMemberByApi(request, landlord.token, apartment.id, tenantB.userId)

    try {
      const reviewables = await getReviewableUsersByApi(request, tenantA.token, apartment.id)
      const roommate = reviewables.find((u) => u.id === tenantB.userId)
      expect(roommate, 'No apareció el compañero como usuario valorable.').toBeDefined()

      await loginUi(page, tenantAEmail)
      await page.goto(`/reviews/new/${apartment.id}/form/${tenantB.userId}`)

      await expect(page.getByRole('heading', { name: 'Valora a tu companero/a' })).toBeVisible()

      for (const label of TENANT_TO_TENANT_CATEGORIES) {
        await expect(page.getByText(label).first()).toBeVisible()
      }

      await fillAllCategories(page, TENANT_TO_TENANT_CATEGORIES)
      await page.locator('textarea[placeholder*="opcional"]').fill('Convivencia razonable en general.')
      await page.getByRole('button', { name: 'Enviar valoracion' }).click()

      await expect(page.getByRole('heading', { name: 'Resena enviada' })).toBeVisible()
    } finally {
      await deleteApartmentByApi(request, landlord.token, apartment.id)
      await cleanupTenantByEmail(request, tenantAEmail)
      await cleanupTenantByEmail(request, tempTenantEmail)

      await context.clearCookies()
    }
  })
})

test.describe('HU-39 / RF-39', () => {
  test('Usuario valorado puede responder una única vez a reseña publicada', async ({
    page,
    request,
    context,
  }) => {
    const landlord = await loginByApi(request, E2E_ENV.landlordEmail, 'hu39-landlord')
    const tempTenantEmail = uniqueTenantEmail()
    await registerTenantByApi(request, tempTenantEmail, 'hu39-tenant-register')
    const tenant = await loginByApi(request, tempTenantEmail, 'hu39-tenant')

    const apartment = await createApartmentByApi(request, landlord.token, uniqueApartmentTitle('HU39'))
    await addMemberByApi(request, landlord.token, apartment.id, tenant.userId)

    try {
      await createTenantReviewByApi(request, tenant.token, apartment.id, landlord.userId, 4, 'Review inicial')
      await createLandlordReviewByApi(
        request,
        landlord.token,
        apartment.id,
        tenant.userId,
        5,
        'Review de reciprocidad'
      )

      await loginUi(page, E2E_ENV.landlordEmail)
      await page.goto('/my-reviews')

      const responderButton = page.getByRole('button', { name: 'Responder' }).first()
      await expect(responderButton).toBeVisible()
      await responderButton.click()

      const responseText = 'Gracias por tu reseña. Quedo a disposición para aclarar cualquier punto.'
      await page.getByPlaceholder('Escribe tu respuesta...').fill(responseText)
      await page.getByRole('button', { name: 'Enviar' }).click()

      const responseBlock = page.locator('div.rounded-xl').filter({ hasText: responseText }).first()
      await expect(responseBlock).toBeVisible()
      await expect(responseBlock.getByText('Tu respuesta')).toBeVisible()

      const respondedCard = responseBlock.locator('xpath=ancestor::div[contains(@class,"rounded-2xl")][1]')
      await expect(respondedCard.getByRole('button', { name: 'Responder' })).toHaveCount(0)
    } finally {
      await deleteApartmentByApi(request, landlord.token, apartment.id)
      await cleanupTenantByEmail(request, tempTenantEmail)
      await context.clearCookies()
    }
  })
})
