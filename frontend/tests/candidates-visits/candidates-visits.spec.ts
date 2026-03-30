import { type APIRequestContext, type Page } from '@playwright/test'
import { expect, test } from '../fixtures/auth.fixture'
import { buildDeviceId, E2E_ENV } from '../fixtures/testEnv'

type AuthApiResponse = {
  token: string
  role: string
  userId: number
  deviceId: string
}

type RegisterApiResponse = {
  token?: string
  role?: string
  userId?: number
}

type ApartmentApiResponse = {
  id: number
  title: string
  ubication: string
}

type MatchApiResponse = {
  id: number
  candidateId: number
  apartmentId: number
  matchStatus: 'ACTIVE' | 'MATCH' | 'INVITED' | 'REJECTED' | 'SUCCESSFUL' | 'CANCELED'
}

const uniqueApartmentTitle = (suffix: string) =>
  `E2E ${suffix} ${Date.now()}-${Math.floor(Math.random() * 100000)}`

const uniqueTenantEmail = (suffix: string) =>
  `tenant.candidates.${suffix}.${Date.now()}.${Math.floor(Math.random() * 100000)}@test.com`

const loginByApi = async (
  request: APIRequestContext,
  email: string,
  scope: string
): Promise<AuthApiResponse> => {
  const deviceId = buildDeviceId(`pw-candidates-${scope}`)
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
      deviceId: buildDeviceId(`pw-candidates-register-${scope}`),
      role: 'TENANT',
    },
  })

  expect(response.ok(), `No se pudo registrar tenant temporal ${email}.`).toBeTruthy()
  return (await response.json()) as RegisterApiResponse
}

const cleanupTenantByEmailBestEffort = async (request: APIRequestContext, email: string) => {
  try {
    const loginResponse = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
      data: {
        email,
        password: E2E_ENV.password,
        deviceId: buildDeviceId('pw-candidates-cleanup-best-effort'),
      },
    })

    if (!loginResponse.ok()) return

    const login = (await loginResponse.json()) as Omit<AuthApiResponse, 'deviceId'>
    if (!login.token) return

    await request.delete(`${E2E_ENV.apiUrl}/users/profile`, {
      headers: {
        Authorization: `Bearer ${login.token}`,
      },
    })
  } catch {
    // Ignore cleanup errors to avoid masking functional test failures.
  }
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
            price: 680,
            bills: 'agua, luz',
            ubication: 'Calle Candidates 25',
            state: 'ACTIVE',
          })
        ),
      },
    },
  })

  expect(response.ok(), `No se pudo crear inmueble de prueba. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentApiResponse
}

const deleteApartmentByApiBestEffort = async (
  request: APIRequestContext,
  landlordToken: string,
  apartmentId: number
) => {
  if (!landlordToken) return

  try {
    await request.delete(`${E2E_ENV.apiUrl}/apartments/${apartmentId}`, {
      headers: {
        Authorization: `Bearer ${landlordToken}`,
      },
    })
  } catch {
    // Ignore cleanup errors to avoid masking functional test failures.
  }
}

const tenantSwipeLikeByApi = async (
  request: APIRequestContext,
  tenantToken: string,
  apartmentId: number
) => {
  const response = await request.post(`${E2E_ENV.apiUrl}/apartments-matches/swipe/apartment/${apartmentId}/tenant`, {
    headers: {
      Authorization: `Bearer ${tenantToken}`,
      'Content-Type': 'application/json',
    },
    data: true,
  })

  expect(response.ok(), `No se pudo hacer like del tenant a piso ${apartmentId}.`).toBeTruthy()
}

const getCandidateMatchesByStatus = async (
  request: APIRequestContext,
  tenantToken: string,
  candidateId: number,
  status: 'ACTIVE' | 'MATCH' | 'INVITED' | 'REJECTED' | 'SUCCESSFUL' | 'CANCELED'
): Promise<MatchApiResponse[]> => {
  const response = await request.get(
    `${E2E_ENV.apiUrl}/apartments-matches/candidate/${candidateId}/status/${status}`,
    {
      headers: {
        Authorization: `Bearer ${tenantToken}`,
      },
    }
  )

  expect(response.ok(), `No se pudieron obtener matches de candidate ${candidateId}/${status}.`).toBeTruthy()
  return (await response.json()) as MatchApiResponse[]
}

const landlordRespondToRequestByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  matchId: number,
  interest: boolean
) => {
  const response = await request.post(
    `${E2E_ENV.apiUrl}/apartments-matches/apartmentMatch/${matchId}/respond-request?interest=${interest}`,
    {
      headers: {
        Authorization: `Bearer ${landlordToken}`,
      },
    }
  )

  expect(response.ok(), `No se pudo responder solicitud ${matchId} con interest=${interest}.`).toBeTruthy()
}

const loginUi = async (page: Page, email: string) => {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(E2E_ENV.password)
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login$/)
}

const getTabCount = async (
  tabs: ReturnType<Page['locator']>,
  tabName: 'Pendientes' | 'Match'
): Promise<number> => {
  const text = (await tabs.getByRole('button', { name: new RegExp(`^${tabName}`) }).textContent()) ?? ''
  const match = text.match(/(\d+)/)
  return match ? Number(match[1]) : 0
}

const requestsTabs = (page: Page) =>
  page
    .locator('section')
    .filter({ has: page.getByRole('button', { name: /^Pendientes/ }) })
    .filter({ has: page.getByRole('button', { name: /^Match/ }) })
    .first()

const cardByApartmentTitle = (page: Page, apartmentTitle: string) =>
  page
    .locator('article')
    .filter({ has: page.getByRole('heading', { name: apartmentTitle, exact: true }) })
    .first()

test.describe('HU-05 / RF-53 / RF-56 (parcial)', () => {
  let landlordToken = ''
  let apartmentPendingId: number | null = null
  let apartmentMatchId: number | null = null
  let pendingApartmentTitle = ''
  let matchApartmentTitle = ''
  const tempTenants: string[] = []

  test.beforeEach(async ({ request, page }) => {
    tempTenants.length = 0
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'hu05-landlord')).token

    const tenantPendingEmail = uniqueTenantEmail('hu05-pending')
    const tenantMatchEmail = uniqueTenantEmail('hu05-match')
    tempTenants.push(tenantPendingEmail, tenantMatchEmail)

    await registerTenantByApi(request, tenantPendingEmail, 'hu05-pending-register')
    await registerTenantByApi(request, tenantMatchEmail, 'hu05-match-register')

    const tenantPending = await loginByApi(request, tenantPendingEmail, 'hu05-pending-login')
    const tenantMatch = await loginByApi(request, tenantMatchEmail, 'hu05-match-login')

    pendingApartmentTitle = uniqueApartmentTitle('HU05-PEND')
    matchApartmentTitle = uniqueApartmentTitle('HU05-MATCH')

    const aptPending = await createApartmentByApi(request, landlordToken, pendingApartmentTitle)
    const aptMatch = await createApartmentByApi(request, landlordToken, matchApartmentTitle)
    apartmentPendingId = aptPending.id
    apartmentMatchId = aptMatch.id

    await tenantSwipeLikeByApi(request, tenantPending.token, aptPending.id)
    await tenantSwipeLikeByApi(request, tenantMatch.token, aptMatch.id)

    const activeForMatchTenant = await getCandidateMatchesByStatus(
      request,
      tenantMatch.token,
      tenantMatch.userId,
      'ACTIVE'
    )
    const target = activeForMatchTenant.find((m) => m.apartmentId === aptMatch.id)
    expect(target).toBeDefined()

    await landlordRespondToRequestByApi(request, landlordToken, target!.id, true)

    await loginUi(page, E2E_ENV.landlordEmail)
    await page.goto('/mis-solicitudes/recibidas')
  })

  test.afterEach(async ({ request, context }) => {
    try {
      if (apartmentPendingId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentPendingId)
      }
      if (apartmentMatchId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentMatchId)
      }

      for (const email of tempTenants) {
        await cleanupTenantByEmailBestEffort(request, email)
      }
    } finally {
      apartmentPendingId = null
      apartmentMatchId = null
      pendingApartmentTitle = ''
      matchApartmentTitle = ''
      tempTenants.length = 0
      await context.clearCookies()
    }
  })

  test('Muestra dashboard con tabs por estado y contadores de pendientes/match', async ({ page }) => {
    const tabs = requestsTabs(page)

    const pendingCard = cardByApartmentTitle(page, pendingApartmentTitle)
    await expect(pendingCard).toBeVisible()
    await expect(pendingCard.getByText('Pendiente', { exact: true })).toBeVisible()

    await expect.poll(() => getTabCount(tabs, 'Pendientes')).toBeGreaterThanOrEqual(1)
    await expect.poll(() => getTabCount(tabs, 'Match')).toBeGreaterThanOrEqual(1)

    await tabs.getByRole('button', { name: /^Match/ }).click()
    await expect(cardByApartmentTitle(page, matchApartmentTitle)).toBeVisible()
    await expect(cardByApartmentTitle(page, matchApartmentTitle).getByText(/¡Match!|Aceptada/)).toBeVisible()
  })
})

test.describe('HU-06 / RF-54 (parcial)', () => {
  let landlordToken = ''
  let apartmentId: number | null = null
  const tempTenants: string[] = []

  test.beforeEach(async ({ request, page }) => {
    tempTenants.length = 0
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'hu06-landlord')).token

    const tenantEmail = uniqueTenantEmail('hu06')
    tempTenants.push(tenantEmail)
    await registerTenantByApi(request, tenantEmail, 'hu06-tenant-register')
    const tenant = await loginByApi(request, tenantEmail, 'hu06-tenant-login')

    const apt = await createApartmentByApi(request, landlordToken, uniqueApartmentTitle('HU06'))
    apartmentId = apt.id

    await tenantSwipeLikeByApi(request, tenant.token, apt.id)

    await loginUi(page, E2E_ENV.landlordEmail)
    await page.goto('/mis-solicitudes/recibidas')
  })

  test.afterEach(async ({ request, context }) => {
    try {
      if (apartmentId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentId)
      }

      for (const email of tempTenants) {
        await cleanupTenantByEmailBestEffort(request, email)
      }
    } finally {
      apartmentId = null
      tempTenants.length = 0
      await context.clearCookies()
    }
  })

  test('Abre ficha expandida del candidato y muestra datos técnicos disponibles', async ({ page }) => {
    await expect(page.locator('article').first()).toBeVisible()
    await page.locator('article').first().click()

    await expect(page).toHaveURL(/\/mis-solicitudes\/recibidas\/\d+$/)
    await expect(page.getByRole('heading', { name: 'Detalle solicitud' })).toBeVisible()
    await expect(page.getByText('Email')).toBeVisible()
    await expect(page.getByText('Profesión', { exact: true })).toBeVisible()
  })
})

test.describe('HU-07 / RF-55 (parcial)', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let apartmentId: number | null = null
  let apartmentTitle = ''
  const tempTenants: string[] = []

  test.beforeEach(async ({ request, page }) => {
    tempTenants.length = 0
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'hu07-landlord')).token

    const tenantEmail = uniqueTenantEmail('hu07')
    tempTenants.push(tenantEmail)
    await registerTenantByApi(request, tenantEmail, 'hu07-tenant-register')
    const tenant = await loginByApi(request, tenantEmail, 'hu07-tenant-login')
    tenantToken = tenant.token
    tenantUserId = tenant.userId

    apartmentTitle = uniqueApartmentTitle('HU07')
    const apt = await createApartmentByApi(request, landlordToken, apartmentTitle)
    apartmentId = apt.id

    await tenantSwipeLikeByApi(request, tenant.token, apt.id)

    await loginUi(page, E2E_ENV.landlordEmail)
    await page.goto('/mis-solicitudes/recibidas')
  })

  test.afterEach(async ({ request, context }) => {
    try {
      if (apartmentId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentId)
      }

      for (const email of tempTenants) {
        await cleanupTenantByEmailBestEffort(request, email)
      }
    } finally {
      apartmentId = null
      apartmentTitle = ''
      tenantToken = ''
      tenantUserId = 0
      tempTenants.length = 0
      await context.clearCookies()
    }
  })

  test('Aceptar mueve la solicitud a Match y crea estado MATCH', async ({ page, request }) => {
    const tabs = requestsTabs(page)
    const pendingCard = cardByApartmentTitle(page, apartmentTitle)

    await expect(pendingCard).toBeVisible()
    await expect(pendingCard.getByRole('button', { name: 'Aceptar' })).toBeVisible()
    await pendingCard.getByRole('button', { name: 'Aceptar' }).click()

    await expect(cardByApartmentTitle(page, apartmentTitle)).toHaveCount(0)

    await tabs.getByRole('button', { name: /^Match/ }).click()
    const matchedCard = cardByApartmentTitle(page, apartmentTitle)
    await expect(matchedCard).toBeVisible()
    await expect(matchedCard.getByText(/¡Match!|Aceptada/)).toBeVisible()

    const candidateMatches = await getCandidateMatchesByStatus(
      request,
      tenantToken,
      tenantUserId,
      'MATCH'
    )
    expect(candidateMatches.some((m) => m.apartmentId === apartmentId)).toBe(true)
  })

  test('Rechazar archiva la solicitud y deja estado REJECTED para el candidato', async ({
    page,
    request,
  }) => {
    const pendingCard = cardByApartmentTitle(page, apartmentTitle)

    await expect(pendingCard).toBeVisible()
    await expect(pendingCard.getByRole('button', { name: 'Rechazar' })).toBeVisible()
    await pendingCard.getByRole('button', { name: 'Rechazar' }).click()

    await expect(cardByApartmentTitle(page, apartmentTitle)).toHaveCount(0)

    const rejected = await getCandidateMatchesByStatus(
      request,
      tenantToken,
      tenantUserId,
      'REJECTED'
    )
    expect(rejected.some((m) => m.apartmentId === apartmentId)).toBe(true)
  })
})
