import { type APIRequestContext, type Page } from '@playwright/test'
import { expect, test } from '../fixtures/auth.fixture'
import { buildDeviceId, E2E_ENV } from '../fixtures/testEnv'

type AuthApiResponse = {
  token: string
  role: string
  userId: number
}

type ApartmentApiResponse = {
  id: number
  title: string
  description: string
  price: number
  bills: string
  ubication: string
  state: 'ACTIVE' | 'MATCHING' | 'CLOSED' | string
}

type ApartmentMemberApiResponse = {
  id: number
  apartmentId: number
  userId: number
  joinDate: string
  endDate?: string | null
}

type ApartmentDeckApiResponse = ApartmentApiResponse & {
  members?: ApartmentMemberApiResponse[]
}

const tinyPngBase64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/w8AAgMBgN2m6QAAAABJRU5ErkJggg=='

const uniqueApartmentTitle = (suffix: string) =>
  `E2E ${suffix} ${Date.now()}-${Math.floor(Math.random() * 100000)}`

const uniqueTenantEmail = (suffix: string) =>
  `tenant.apartments.${suffix}.${Date.now()}.${Math.floor(Math.random() * 100000)}@test.com`

const loginByApi = async (request: APIRequestContext, email: string): Promise<AuthApiResponse> => {
  const response = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
    data: {
      email,
      password: E2E_ENV.password,
      deviceId: buildDeviceId('pw-apartments-login'),
    },
  })

  expect(response.ok(), `Login API falló para ${email}. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as AuthApiResponse
}

const registerTenantByApi = async (request: APIRequestContext, email: string, scope: string) => {
  const response = await request.post(`${E2E_ENV.apiUrl}/auth/register`, {
    data: {
      email,
      password: E2E_ENV.password,
      role: 'TENANT',
      deviceId: buildDeviceId(`pw-apartments-register-${scope}`),
    },
  })

  expect(response.ok(), `No se pudo registrar tenant temporal ${email}.`).toBeTruthy()
}

const cleanupUserByEmailBestEffort = async (request: APIRequestContext, email: string) => {
  try {
    const loginResponse = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
      data: {
        email,
        password: E2E_ENV.password,
        deviceId: buildDeviceId('pw-apartments-cleanup'),
      },
    })

    if (!loginResponse.ok()) return

    const login = (await loginResponse.json()) as { token?: string }
    if (!login.token) return

    await request.delete(`${E2E_ENV.apiUrl}/users/profile`, {
      headers: {
        Authorization: `Bearer ${login.token}`,
      },
    })
  } catch {
    // Best effort cleanup.
  }
}

const createApartmentByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  title: string,
  state: 'ACTIVE' | 'CLOSED' = 'ACTIVE'
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
            price: 650,
            bills: 'agua, luz',
            ubication: 'Calle E2E 10',
            state,
          })
        ),
      },
    },
  })

  expect(response.ok(), `No se pudo crear inmueble de prueba. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentApiResponse
}

const updateApartmentStateByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  apartment: ApartmentApiResponse,
  nextState: 'ACTIVE' | 'CLOSED'
): Promise<ApartmentApiResponse> => {
  const response = await request.put(`${E2E_ENV.apiUrl}/apartments/${apartment.id}`, {
    headers: {
      Authorization: `Bearer ${landlordToken}`,
    },
    data: {
      title: apartment.title,
      description: apartment.description,
      price: apartment.price,
      bills: apartment.bills,
      ubication: apartment.ubication,
      state: nextState,
    },
  })

  expect(response.ok(), `No se pudo actualizar estado a ${nextState}. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentApiResponse
}

const getApartmentByApi = async (
  request: APIRequestContext,
  token: string,
  apartmentId: number
): Promise<ApartmentApiResponse> => {
  const response = await request.get(`${E2E_ENV.apiUrl}/apartments/${apartmentId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  expect(response.ok(), `No se pudo leer inmueble ${apartmentId}. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentApiResponse
}

const getDeckByApi = async (
  request: APIRequestContext,
  tenantToken: string,
  tenantId: number
): Promise<ApartmentDeckApiResponse[]> => {
  const response = await request.get(`${E2E_ENV.apiUrl}/apartments/deck/${tenantId}`, {
    headers: {
      Authorization: `Bearer ${tenantToken}`,
    },
  })

  expect(response.ok(), `No se pudo leer deck de tenant ${tenantId}. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentDeckApiResponse[]
}

const getMyApartmentsByApi = async (
  request: APIRequestContext,
  landlordToken: string
): Promise<ApartmentApiResponse[]> => {
  const response = await request.get(`${E2E_ENV.apiUrl}/apartments/my`, {
    headers: {
      Authorization: `Bearer ${landlordToken}`,
    },
  })

  expect(response.ok(), `No se pudo leer /apartments/my. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentApiResponse[]
}

const addMemberByApi = async (
  request: APIRequestContext,
  token: string,
  apartmentId: number,
  userId: number
): Promise<ApartmentMemberApiResponse> => {
  const response = await request.post(`${E2E_ENV.apiUrl}/apartments/${apartmentId}/members`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    data: {
      userId,
      joinDate: new Date().toISOString().slice(0, 10),
    },
  })

  expect(response.ok(), `No se pudo añadir miembro al inmueble ${apartmentId}. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentMemberApiResponse
}

const removeMemberByApi = async (
  request: APIRequestContext,
  token: string,
  apartmentId: number,
  memberId: number
) => {
  const response = await request.delete(`${E2E_ENV.apiUrl}/apartments/${apartmentId}/members/${memberId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  expect(response.status()).toBeGreaterThanOrEqual(200)
  expect(response.status()).toBeLessThan(500)
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

  expect(
    response.status(),
    `No se pudo eliminar inmueble ${apartmentId}. Status ${response.status()}`
  ).toBeGreaterThanOrEqual(200)
  expect(response.status()).toBeLessThan(500)
}

const loginUi = async (page: Page, email: string) => {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(E2E_ENV.password)
  await page.getByRole('button', { name: 'Login' }).click()

  if (email === E2E_ENV.landlordEmail) {
    await expect(page).toHaveURL(/\/apartments\/my$/)
  } else {
    await expect(page).not.toHaveURL(/\/login$/)
  }
}

const getApartmentCardByTitle = (page: Page, title: string) =>
  page
    .getByRole('heading', { name: title, exact: true })
    .locator('xpath=ancestor::div[contains(@class,"rounded-3xl")][1]')

test.describe('HU-51 / RF-09 / RI-03 / RI-04 / RI-06 / RNF-06 / RNF-07', () => {
  let landlordToken = ''
  const createdApartmentIds: number[] = []

  test.beforeEach(async ({ request, page }) => {
    createdApartmentIds.length = 0
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail)).token
    await loginUi(page, E2E_ENV.landlordEmail)
    await expect(page.getByRole('heading', { name: 'Mis Inmuebles' })).toBeVisible()
    await page.getByRole('button', { name: 'Publicar nuevo inmueble' }).click()
    await expect(page.getByText('¿Dónde está ubicado el piso?')).toBeVisible()
  })

  test.afterEach(async ({ request, context }) => {
    for (const apartmentId of createdApartmentIds) {
      await deleteApartmentByApi(request, landlordToken, apartmentId)
    }
    createdApartmentIds.length = 0
    await context.clearCookies()
  })

  test('Registra inmueble con formulario guiado, fotos y persistencia', async ({ page, request }) => {
    await expect(page.locator('input[placeholder="Ej. Calle Mayor, 10"]')).toBeVisible()
    await page.locator('input[placeholder="Ej. Calle Mayor, 10"]').fill('Calle HU51, 99')
    await page.getByRole('button', { name: 'Triana' }).click()
    await page.getByRole('button', { name: 'Siguiente' }).click()

    await expect(page.getByText('Precio y disponibilidad')).toBeVisible()
    await page.locator('input[placeholder="450"]').fill('450')
    await page.locator('input[type="date"]').fill('2030-01-15')
    await page.getByRole('button', { name: /Agua/ }).click()
    await page.getByRole('button', { name: 'Siguiente' }).click()

    await expect(page.getByRole('button', { name: 'Seleccionar fotos' })).toBeVisible()
    await page.locator('input[type="file"]').setInputFiles({
      name: 'hu51-photo.png',
      mimeType: 'image/png',
      buffer: Buffer.from(tinyPngBase64, 'base64'),
    })
    await expect(page.getByAltText('hu51-photo.png')).toBeVisible()

    await page.getByRole('button', { name: '✕' }).first().click()
    await expect(page.getByAltText('hu51-photo.png')).toHaveCount(0)

    await page.getByRole('button', { name: 'Siguiente' }).click()

    await expect(page.getByText('Reglas y matching inteligente')).toBeVisible()
    await expect(page.getByText('En desarrollo')).toBeVisible()

    const finishButton = page.getByRole('button', { name: 'Finalizar' })
    await expect(finishButton).toBeEnabled()
    await page.getByRole('button', { name: 'Finalizar' }).click()

    await expect
      .poll(async () => {
        const myApartments = await getMyApartmentsByApi(request, landlordToken)
        return myApartments.some((item) => item.ubication === 'Calle HU51, 99' && item.price === 450)
      }, { timeout: 20000, intervals: [500, 1000, 2000] })
      .toBe(true)

    const myApartments = await getMyApartmentsByApi(request, landlordToken)
    const createdApartment = myApartments.find(
      (item) => item.ubication === 'Calle HU51, 99' && item.price === 450
    ) as ApartmentApiResponse
    createdApartmentIds.push(createdApartment.id)
    expect(createdApartment.bills.toLowerCase()).toContain('agua')
  })
})

test.describe('HU-53 / RF-11 / RI-03', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let apartment: ApartmentApiResponse | null = null

  test.beforeEach(async ({ request, page }) => {
    const landlordSession = await loginByApi(request, E2E_ENV.landlordEmail)
    landlordToken = landlordSession.token

    const tenantSession = await loginByApi(request, E2E_ENV.tenantEmail)
    tenantToken = tenantSession.token
    tenantUserId = tenantSession.userId

    apartment = await createApartmentByApi(request, landlordToken, uniqueApartmentTitle('HU53'))

    await loginUi(page, E2E_ENV.landlordEmail)
    await expect(page.getByRole('heading', { name: 'Mis Inmuebles' })).toBeVisible()
    await expect(page.getByText((apartment as ApartmentApiResponse).title)).toBeVisible()
  })

  test.afterEach(async ({ request, context }) => {
    if (apartment) {
      await deleteApartmentByApi(request, landlordToken, apartment.id)
    }
    apartment = null
    await context.clearCookies()
  })

  test('Permite pausar/reactivar, persiste al instante y cambia visibilidad en deck', async ({
    page,
    request,
  }) => {
    expect(apartment).not.toBeNull()
    const apartmentData = apartment as ApartmentApiResponse

    const card = getApartmentCardByTitle(page, apartmentData.title)
    await expect(card).toBeVisible()

    const deckActive = await getDeckByApi(request, tenantToken, tenantUserId)
    expect(deckActive.some((item) => item.id === apartmentData.id)).toBe(true)

    await card.locator('button:has-text("Pausar")').first().click()
    await expect(getApartmentCardByTitle(page, apartmentData.title).locator('button:has-text("Reanudar")').first()).toBeVisible()

    await expect
      .poll(async () => (await getApartmentByApi(request, landlordToken, apartmentData.id)).state)
      .toBe('CLOSED')

    const deckPaused = await getDeckByApi(request, tenantToken, tenantUserId)
    expect(deckPaused.some((item) => item.id === apartmentData.id)).toBe(false)

    await getApartmentCardByTitle(page, apartmentData.title).locator('button:has-text("Reanudar")').first().click()
    await expect(getApartmentCardByTitle(page, apartmentData.title).locator('button:has-text("Pausar")').first()).toBeVisible()

    await expect
      .poll(async () => (await getApartmentByApi(request, landlordToken, apartmentData.id)).state)
      .toBe('ACTIVE')

    const deckResumed = await getDeckByApi(request, tenantToken, tenantUserId)
    expect(deckResumed.some((item) => item.id === apartmentData.id)).toBe(true)

    apartment = await getApartmentByApi(request, landlordToken, apartmentData.id)
  })

  test('Permite cerrar anuncio (estado CLOSED) y ocultarlo del deck', async ({ request }) => {
    expect(apartment).not.toBeNull()
    const apartmentData = apartment as ApartmentApiResponse

    const updated = await updateApartmentStateByApi(request, landlordToken, apartmentData, 'CLOSED')
    expect(updated.state).toBe('CLOSED')

    const deckClosed = await getDeckByApi(request, tenantToken, tenantUserId)
    expect(deckClosed.some((item) => item.id === apartmentData.id)).toBe(false)

    apartment = updated
  })
})

test.describe('HU-56 / RF-14 / RI-05 / RNF-07', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let tempMemberEmail = ''
  let tempMemberUserId = 0
  let apartment: ApartmentApiResponse | null = null
  let member: ApartmentMemberApiResponse | null = null

  test.beforeEach(async ({ request }) => {
    const landlordSession = await loginByApi(request, E2E_ENV.landlordEmail)
    landlordToken = landlordSession.token

    const tenantSession = await loginByApi(request, E2E_ENV.tenantEmail)
    tenantToken = tenantSession.token
    tenantUserId = tenantSession.userId

    tempMemberEmail = uniqueTenantEmail('hu56-member')
    await registerTenantByApi(request, tempMemberEmail, 'hu56-member')
    const tempMemberSession = await loginByApi(request, tempMemberEmail)
    tempMemberUserId = tempMemberSession.userId

    apartment = await createApartmentByApi(request, landlordToken, uniqueApartmentTitle('HU56'))
    member = await addMemberByApi(request, landlordToken, apartment.id, tempMemberUserId)
  })

  test.afterEach(async ({ request, context }) => {
    if (apartment && member) {
      await removeMemberByApi(request, landlordToken, apartment.id, member.id)
    }
    if (apartment) {
      await deleteApartmentByApi(request, landlordToken, apartment.id)
    }

    if (tempMemberEmail) {
      await cleanupUserByEmailBestEffort(request, tempMemberEmail)
    }

    apartment = null
    member = null
    tempMemberEmail = ''
    tempMemberUserId = 0
    await context.clearCookies()
  })

  test('Consulta miembros actuales del piso con rol y fecha de ingreso, sin campos sensibles', async ({
    request,
  }) => {
    const deck = await getDeckByApi(request, tenantToken, tenantUserId)
    const target = deck.find((item) => item.id === (apartment as ApartmentApiResponse).id)

    expect(
      target,
      'No se encontró el inmueble sembrado para HU-56 en el deck del tenant.'
    ).toBeDefined()
    expect(target?.members).toBeDefined()
    expect((target?.members?.length ?? 0) > 0).toBe(true)

    const firstMember = target?.members?.[0]
    expect(firstMember).toBeDefined()
    expect(Boolean(firstMember?.joinDate)).toBe(true)

    const memberUserResponse = await request.get(`${E2E_ENV.apiUrl}/users/${firstMember?.userId}`, {
      headers: {
        Authorization: `Bearer ${tenantToken}`,
      },
    })

    expect(memberUserResponse.ok()).toBeTruthy()
    const memberUser = (await memberUserResponse.json()) as { role?: string; password?: string; phone?: string }
    expect(Boolean(memberUser.role)).toBe(true)

    const rawMember = firstMember as Record<string, unknown>
    expect(rawMember.password).toBeUndefined()
    expect(rawMember.phone).toBeUndefined()
  })
})
