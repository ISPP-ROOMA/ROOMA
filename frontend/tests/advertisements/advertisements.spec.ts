import {
  type APIRequestContext,
  type Browser,
  type BrowserContext,
  type Locator,
  type Page,
  type Response,
} from '@playwright/test'
import { expect, test } from '../fixtures/auth.fixture'
import { buildDeviceId, E2E_ENV } from '../fixtures/testEnv'

const APP_URL = E2E_ENV.baseUrl
const API_URL = E2E_ENV.apiUrl
const LANDLORD_EMAIL = E2E_ENV.landlordEmail
const TENANT_EMAIL = E2E_ENV.tenantEmail
const PASSWORD = E2E_ENV.password

type LoginApiResponse = {
  token: string
  userId: number
}

type ApartmentApiResponse = {
  id: number
  title: string
  description: string
  price: number
  bills: string
  ubication: string
  state: string
}

const generateApartmentTitle = () => `E2E AD ${Date.now()}-${Math.floor(Math.random() * 100000)}`

const escapeRegExp = (value: string) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const login = async (page: Page, email: string) => {
  await page.goto(`${APP_URL}/login`)
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(PASSWORD)
  await page.getByRole('button', { name: 'Login' }).click()
}

const createUserContext = async (
  browser: Browser,
  email: string
): Promise<{ context: BrowserContext; page: Page }> => {
  const context = await browser.newContext()
  const page = await context.newPage()
  await login(page, email)
  return { context, page }
}

const loginByApi = async (
  request: APIRequestContext,
  email: string
): Promise<{ token: string; userId: number }> => {
  const response = await request.post(`${API_URL}/auth/login`, {
    data: {
      email,
      password: PASSWORD,
      deviceId: buildDeviceId('pw-e2e'),
    },
  })

  expect(response.ok(), `No se pudo hacer login API para ${email}. Status: ${response.status()}`).toBeTruthy()
  const body = (await response.json()) as LoginApiResponse
  return { token: body.token, userId: body.userId }
}

const createApartmentByApi = async (
  request: APIRequestContext,
  landlordToken: string
): Promise<ApartmentApiResponse> => {
  const title = generateApartmentTitle()
  const payload = {
    title,
    description: `Apartamento de prueba ${title}`,
    price: 777,
    bills: 'Incluidos',
    ubication: 'Madrid',
    state: 'ACTIVE',
  }

  const response = await request.post(`${API_URL}/apartments`, {
    headers: {
      Authorization: `Bearer ${landlordToken}`,
    },
    multipart: {
      data: {
        name: 'apartment.json',
        mimeType: 'application/json',
        buffer: Buffer.from(JSON.stringify(payload)),
      },
    },
  })

  expect(response.ok(), `No se pudo crear anuncio de prueba. Status: ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentApiResponse
}

const deleteApartmentByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  apartmentId: number
) => {
  const response = await request.delete(`${API_URL}/apartments/${apartmentId}`, {
    headers: {
      Authorization: `Bearer ${landlordToken}`,
    },
  })

  expect(
    response.ok(),
    `No se pudo limpiar el anuncio de prueba ${apartmentId}. Status: ${response.status()}`
  ).toBeTruthy()
}

const loadDeckTitlesByApi = async (
  request: APIRequestContext,
  tenantToken: string,
  tenantUserId: number
) => {
  const response = await request.get(`${API_URL}/apartments/deck/${tenantUserId}`, {
    headers: {
      Authorization: `Bearer ${tenantToken}`,
    },
  })

  expect(response.ok(), `No se pudo leer deck del tenant ${tenantUserId}. Status ${response.status()}`).toBeTruthy()
  const deck = (await response.json()) as Array<{ title?: string }>
  return deck
    .map((item) => item.title?.trim())
    .filter((title): title is string => Boolean(title))
}

const getApartmentCardByTitle = (page: Page, title: string): Locator =>
  page
    .getByRole('heading', {
      name: new RegExp(`^${escapeRegExp(title)}$`),
    })
    .locator('xpath=ancestor::div[.//button[@title="Ver detalle"]][1]')

const pauseApartment = async (page: Page, title: string) => {
  const card = getApartmentCardByTitle(page, title)
  const updateResponsePromise = page.waitForResponse((response: Response) => {
    return (
      response.request().method() === 'PUT' &&
      /\/apartments\/\d+$/.test(response.url()) &&
      response.status() >= 200 &&
      response.status() < 300
    )
  })

  await card.getByRole('button', { name: 'Pausar' }).click()
  await updateResponsePromise
  const updatedCard = getApartmentCardByTitle(page, title)
  await expect(updatedCard.getByRole('button', { name: 'Reanudar' })).toBeVisible()
}

const resumeApartment = async (page: Page, title: string) => {
  const card = getApartmentCardByTitle(page, title)
  const updateResponsePromise = page.waitForResponse((response: Response) => {
    return (
      response.request().method() === 'PUT' &&
      /\/apartments\/\d+$/.test(response.url()) &&
      response.status() >= 200 &&
      response.status() < 300
    )
  })

  await card.getByRole('button', { name: 'Reanudar' }).click()
  await updateResponsePromise
  const updatedCard = getApartmentCardByTitle(page, title)
  await expect(updatedCard.getByRole('button', { name: 'Pausar' })).toBeVisible()
}

test.describe.serial('HU-66 / HU-67 - Gestión de estado de anuncios', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let createdApartment: ApartmentApiResponse | null = null

  test.beforeEach(async ({ request }) => {
    const landlordSession = await loginByApi(request, LANDLORD_EMAIL)
    landlordToken = landlordSession.token

    const tenantSession = await loginByApi(request, TENANT_EMAIL)
    tenantToken = tenantSession.token
    tenantUserId = tenantSession.userId

    createdApartment = await createApartmentByApi(request, landlordToken)
  })

  test.afterEach(async ({ request }) => {
    if (!createdApartment) {
      return
    }

    await deleteApartmentByApi(request, landlordToken, createdApartment.id)
    createdApartment = null
  })

  test('HU-66: landlord pausa anuncio y deja de aparecer en deck del tenant', async ({ browser, request }) => {
    const landlordSession = await createUserContext(browser, LANDLORD_EMAIL)
    const tenantSession = await createUserContext(browser, TENANT_EMAIL)

    try {
      const { page: landlordPage } = landlordSession
      const { page: tenantPage } = tenantSession
      const apartmentTitle = createdApartment?.title ?? ''

      await landlordPage.goto(`${APP_URL}/apartments/my`)

      const apartmentCard = getApartmentCardByTitle(landlordPage, apartmentTitle)
      await expect(apartmentCard).toBeVisible()

      await tenantPage.goto(`${APP_URL}/`)
      const deckBeforePause = await loadDeckTitlesByApi(request, tenantToken, tenantUserId)
      expect(
        deckBeforePause,
        `Precondición no cumplida: el anuncio "${apartmentTitle}" no aparece inicialmente en el deck del tenant.`
      ).toContain(apartmentTitle)

      await pauseApartment(landlordPage, apartmentTitle)

      const deckAfterPause = await loadDeckTitlesByApi(request, tenantToken, tenantUserId)
      expect(deckAfterPause).not.toContain(apartmentTitle)

      await resumeApartment(landlordPage, apartmentTitle)
    } finally {
      await tenantSession.context.close()
      await landlordSession.context.close()
    }
  })

  test('HU-67: landlord reactiva anuncio y vuelve a aparecer en deck del tenant', async ({ browser, request }) => {
    const landlordSession = await createUserContext(browser, LANDLORD_EMAIL)
    const tenantSession = await createUserContext(browser, TENANT_EMAIL)

    try {
      const { page: landlordPage } = landlordSession
      const { page: tenantPage } = tenantSession
      const apartmentTitle = createdApartment?.title ?? ''

      await landlordPage.goto(`${APP_URL}/apartments/my`)

      const apartmentCard = getApartmentCardByTitle(landlordPage, apartmentTitle)
      await expect(apartmentCard).toBeVisible()

      await tenantPage.goto(`${APP_URL}/`)
      const deckBeforePause = await loadDeckTitlesByApi(request, tenantToken, tenantUserId)
      expect(
        deckBeforePause,
        `Precondición no cumplida: el anuncio "${apartmentTitle}" no aparece inicialmente en el deck del tenant.`
      ).toContain(apartmentTitle)

      await pauseApartment(landlordPage, apartmentTitle)
      const deckWhilePaused = await loadDeckTitlesByApi(request, tenantToken, tenantUserId)
      expect(deckWhilePaused).not.toContain(apartmentTitle)

      await resumeApartment(landlordPage, apartmentTitle)
      const deckAfterResume = await loadDeckTitlesByApi(request, tenantToken, tenantUserId)
      expect(deckAfterResume).toContain(apartmentTitle)
    } finally {
      await tenantSession.context.close()
      await landlordSession.context.close()
    }
  })
})
