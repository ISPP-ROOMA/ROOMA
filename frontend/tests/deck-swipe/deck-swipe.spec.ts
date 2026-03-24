import { type APIRequestContext, type Page } from '@playwright/test'
import { expect, test } from '../fixtures/auth.fixture'
import { buildDeviceId, E2E_ENV } from '../fixtures/testEnv'

type AuthApiResponse = {
  token: string
  userId: number
}

type RegisterApiResponse = {
  token?: string
  userId?: number
}

type ApartmentApiResponse = {
  id: number
}

const uniqueApartmentTitle = (suffix: string) =>
  `E2E Deck ${suffix} ${Date.now()}-${Math.floor(Math.random() * 100000)}`

const uniqueTenantEmail = (suffix: string) =>
  `tenant.deck.${suffix}.${Date.now()}.${Math.floor(Math.random() * 100000)}@test.com`

const loginByApi = async (
  request: APIRequestContext,
  email: string,
  scope: string
): Promise<AuthApiResponse> => {
  const response = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
    data: {
      email,
      password: E2E_ENV.password,
      deviceId: buildDeviceId(`pw-deck-${scope}`),
    },
  })

  expect(response.ok(), `Login API falló para ${email}. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as AuthApiResponse
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
      role: 'TENANT',
      deviceId: buildDeviceId(`pw-deck-register-${scope}`),
    },
  })

  expect(response.ok(), `No se pudo registrar tenant temporal ${email}.`).toBeTruthy()
  return (await response.json()) as RegisterApiResponse
}

const loginUi = async (page: Page, email: string) => {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(E2E_ENV.password)
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login$/)
}

const createApartmentByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  title: string,
  state: 'ACTIVE' | 'MATCHING' | 'CLOSED' = 'ACTIVE'
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
            price: 720,
            bills: 'agua, luz',
            ubication: 'Calle Deck 7',
            state,
          })
        ),
      },
    },
  })

  expect(response.ok(), `No se pudo crear inmueble ${title}. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentApiResponse
}

const swipeApartmentByApi = async (
  request: APIRequestContext,
  tenantToken: string,
  apartmentId: number,
  interest: boolean
) => {
  const response = await request.post(`${E2E_ENV.apiUrl}/apartments-matches/swipe/apartment/${apartmentId}/tenant`, {
    headers: {
      Authorization: `Bearer ${tenantToken}`,
      'Content-Type': 'application/json',
    },
    data: interest,
  })

  expect(response.ok(), `No se pudo registrar swipe para apartamento ${apartmentId}.`).toBeTruthy()
}

const deleteApartmentBestEffort = async (
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
    // Best effort cleanup.
  }
}

const cleanupTenantBestEffort = async (request: APIRequestContext, email: string) => {
  try {
    const loginResponse = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
      data: {
        email,
        password: E2E_ENV.password,
        deviceId: buildDeviceId('pw-deck-cleanup'),
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

const topInteractiveCard = (page: Page) =>
  page.locator('div.absolute.w-full.h-full.will-change-transform:not(.pointer-events-none)').last()

test.describe('HU-01 / RF-46 / RF-48 / RF-51 (parcial)', () => {
  let landlordToken = ''
  const createdApartments: number[] = []
  const tempTenants: string[] = []

  test.afterEach(async ({ request, context }) => {
    try {
      for (const apartmentId of createdApartments) {
        await deleteApartmentBestEffort(request, landlordToken, apartmentId)
      }

      for (const email of tempTenants) {
        await cleanupTenantBestEffort(request, email)
      }
    } finally {
      createdApartments.length = 0
      tempTenants.length = 0
      landlordToken = ''
      await context.clearCookies()
    }
  })

  test('El deck devuelve inmuebles elegibles (ACTIVE y no swiped) para el buscador', async ({
    request,
    page,
  }) => {
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'hu01-landlord')).token

    const tenantEmail = uniqueTenantEmail('hu01')
    tempTenants.push(tenantEmail)
    await registerTenantByApi(request, tenantEmail, 'hu01-tenant')
    const tenantLogin = await loginByApi(request, tenantEmail, 'hu01-tenant-login')

    const activeTitleVisible = uniqueApartmentTitle('ACTIVE-VISIBLE')
    const activeTitleSwiped = uniqueApartmentTitle('ACTIVE-SWIPED')
    const closedTitle = uniqueApartmentTitle('CLOSED')

    const aptVisible = await createApartmentByApi(request, landlordToken, activeTitleVisible, 'ACTIVE')
    const aptSwiped = await createApartmentByApi(request, landlordToken, activeTitleSwiped, 'ACTIVE')
    const aptClosed = await createApartmentByApi(request, landlordToken, closedTitle, 'CLOSED')
    createdApartments.push(aptVisible.id, aptSwiped.id, aptClosed.id)

    await swipeApartmentByApi(request, tenantLogin.token, aptSwiped.id, false)

    await loginUi(page, tenantEmail)
    await page.goto('/')

    await expect(page.getByText(activeTitleVisible, { exact: true })).toBeVisible()
    await expect(page.getByText(activeTitleSwiped, { exact: true })).toHaveCount(0)
    await expect(page.getByText(closedTitle, { exact: true })).toHaveCount(0)
  })

  test('Sin resultados muestra mensaje personalizado para ajustar la búsqueda', async ({ page }) => {
    await page.route('**/api/apartments/deck/*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: '[]',
      })
    })

    await loginUi(page, E2E_ENV.tenantEmail)
    await page.goto('/')

    await expect(page.getByRole('heading', { name: 'No hay más pisos' })).toBeVisible()
    await expect(page.getByText('Vuelve más tarde para ver nuevas opciones publicadas.')).toBeVisible()
  })
})

test.describe('HU-02 / RF-48 / RF-49 / RF-50 (completada)', () => {
  test('Like y dislike por botones registran estado y avanzan a la siguiente tarjeta', async ({ page }) => {
    const mockDeck = [
      {
        id: 91001,
        title: 'Deck A',
        description: 'A',
        price: 700,
        bills: 'agua',
        ubication: 'Madrid',
        state: 'ACTIVE',
      },
      {
        id: 91002,
        title: 'Deck B',
        description: 'B',
        price: 800,
        bills: 'luz',
        ubication: 'Sevilla',
        state: 'ACTIVE',
      },
    ]

    const swipeCalls: Array<{ apartmentId: number; interest: boolean }> = []

    await page.route('**/api/apartments/deck/*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockDeck),
      })
    })

    await page.route('**/api/apartments-matches/swipe/apartment/*/tenant', async (route) => {
      const request = route.request()
      const match = request.url().match(/apartment\/(\d+)\/tenant/)
      const apartmentId = match ? Number(match[1]) : 0
      const interest = request.postData()?.trim() === 'true'
      swipeCalls.push({ apartmentId, interest })

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: Date.now(),
          apartmentId,
          matchStatus: interest ? 'ACTIVE' : 'REJECTED',
        }),
      })
    })

    await loginUi(page, E2E_ENV.tenantEmail)
    await page.goto('/')

    await expect(page.getByText('Deck B', { exact: true })).toBeVisible()
    const firstTopCard = topInteractiveCard(page)
    await expect(firstTopCard).toBeVisible()

    await firstTopCard.locator('button.btn.btn-circle').nth(1).click({ force: true })
    await expect.poll(() => swipeCalls.length).toBe(1)
    expect(swipeCalls[0]).toEqual({ apartmentId: 91002, interest: true })

    await expect(page.getByText('Deck B', { exact: true })).toHaveCount(0)
    await expect(page.getByText('Deck A', { exact: true })).toBeVisible()

    const secondTopCard = page
      .locator('div.absolute.inset-0.rounded-3xl')
      .filter({ has: page.getByRole('heading', { name: 'Deck A', exact: true }) })
      .first()
    await expect(secondTopCard).toBeVisible()

    await secondTopCard.locator('button.btn.btn-circle').nth(0).click({ force: true })
    await expect.poll(() => swipeCalls.length).toBe(2)
    expect(swipeCalls[1]).toEqual({ apartmentId: 91001, interest: false })

    await expect(page.getByRole('heading', { name: 'No hay más pisos' })).toBeVisible()
  })

  test('El gesto horizontal mantiene indicadores y dispara swipe al superar umbral', async ({ page }) => {
    await page.route('**/api/apartments/deck/*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 92001,
            title: 'Deck Gesture',
            description: 'Gesture test',
            price: 650,
            bills: 'internet',
            ubication: 'Valencia',
            state: 'ACTIVE',
          },
        ]),
      })
    })

    const swipeCalls: boolean[] = []
    await page.route('**/api/apartments-matches/swipe/apartment/*/tenant', async (route) => {
      swipeCalls.push(route.request().postData()?.trim() === 'true')
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ id: 1, apartmentId: 92001, matchStatus: 'ACTIVE' }),
      })
    })

    await loginUi(page, E2E_ENV.tenantEmail)
    await page.goto('/')

    const topCard = page.locator('div.absolute.inset-0.rounded-3xl').last()
    await expect(topCard).toBeVisible()

    await expect(topCard.getByText('LIKE', { exact: true })).toHaveCount(1)
    await expect(topCard.getByText('NOPE', { exact: true })).toHaveCount(1)

    const box = await topCard.boundingBox()
    expect(box).not.toBeNull()

    await page.mouse.move(box!.x + box!.width / 2, box!.y + box!.height / 2)
    await page.mouse.down()
    await page.mouse.move(box!.x + box!.width / 2 + 130, box!.y + box!.height / 2, { steps: 10 })

    await page.mouse.up()

    await expect.poll(() => swipeCalls.length).toBe(1)
    expect(swipeCalls[0]).toBe(true)
  })
})

test.describe('HU-03 / RF-47 (parcial)', () => {
  test('Al abrir detalle se muestran galería, descripción y sección de compañeros', async ({ page }) => {
    await page.route('**/api/apartments/deck/*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 93001,
            title: 'Deck Detail',
            description: 'Piso con descripción para validar detalle',
            price: 900,
            bills: 'agua, luz, internet',
            ubication: 'Barcelona',
            state: 'ACTIVE',
            coverImageUrl: 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85',
            members: [],
          },
        ]),
      })
    })

    await page.route('**/api/apartments/93001/photos', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ apartment: { id: 93001 }, images: [] }),
      })
    })

    await loginUi(page, E2E_ENV.tenantEmail)
    await page.goto('/')

    const topCard = page.locator('div.absolute.inset-0.rounded-3xl').last()
    await expect(topCard).toBeVisible()

    const box = await topCard.boundingBox()
    expect(box).not.toBeNull()

    await page.mouse.move(box!.x + box!.width / 2, box!.y + box!.height / 2)
    await page.mouse.down()
    await page.mouse.move(box!.x + box!.width / 2, box!.y + box!.height / 2 - 120, { steps: 10 })
    await page.mouse.up()

    await expect(page.getByRole('heading', { name: 'Descripción' })).toBeVisible()
    await expect(page.getByText('Piso con descripción para validar detalle')).toBeVisible()
    await expect(page.getByRole('heading', { name: /Compañeros actuales/ })).toBeVisible()
    await expect(page.getByText('Aún no hay compañeros en este piso.')).toBeVisible()
  })
})
