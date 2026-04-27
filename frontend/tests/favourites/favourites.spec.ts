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

type FavoriteItemApiResponse = {
  apartmentId: number
  title: string
  city: string
  price: number
  isFavorite: boolean
  availabilityStatus: 'AVAILABLE' | 'CLOSED'
}

const uniqueApartmentTitle = (suffix: string) =>
  `E2E ${suffix} ${Date.now()}-${Math.floor(Math.random() * 100000)}`

const loginByApi = async (request: APIRequestContext, email: string): Promise<AuthApiResponse> => {
  const response = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
    data: {
      email,
      password: E2E_ENV.password,
      deviceId: buildDeviceId('pw-favourites-login'),
    },
  })

  expect(response.ok(), `Login API falló para ${email}. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as AuthApiResponse
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
            price: 620,
            bills: 'agua, luz',
            ubication: 'Calle Favoritos 123',
            state: 'ACTIVE',
          })
        ),
      },
    },
  })

  expect(response.ok(), `No se pudo crear inmueble de prueba. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as ApartmentApiResponse
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

const addFavoriteByApi = async (request: APIRequestContext, tenantToken: string, apartmentId: number) => {
  const response = await request.put(`${E2E_ENV.apiUrl}/favorites/${apartmentId}`, {
    headers: {
      Authorization: `Bearer ${tenantToken}`,
    },
  })

  expect(response.ok(), `No se pudo añadir favorito ${apartmentId}. Status ${response.status()}`).toBeTruthy()
}

const removeFavoriteByApi = async (
  request: APIRequestContext,
  tenantToken: string,
  apartmentId: number
) => {
  const response = await request.delete(`${E2E_ENV.apiUrl}/favorites/${apartmentId}`, {
    headers: {
      Authorization: `Bearer ${tenantToken}`,
    },
  })

  expect(response.status()).toBeGreaterThanOrEqual(200)
  expect(response.status()).toBeLessThan(500)
}

const getFavoritesByApi = async (
  request: APIRequestContext,
  tenantToken: string
): Promise<FavoriteItemApiResponse[]> => {
  const response = await request.get(`${E2E_ENV.apiUrl}/favorites`, {
    headers: {
      Authorization: `Bearer ${tenantToken}`,
    },
  })

  expect(response.ok(), `No se pudo leer favoritos. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as FavoriteItemApiResponse[]
}

const moveDeckUntilTitleVisible = async (page: Page, title: string, maxAttempts = 12) => {
  const titleLocator = page.locator('h2').filter({ hasText: title }).first()
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    if (await titleLocator.isVisible()) {
      return
    }

    const topCard = page
      .locator('div.absolute.w-full.h-full.will-change-transform:not(.pointer-events-none)')
      .last()
    await expect(topCard).toBeVisible()

    const nopeButton = topCard.locator('div.absolute.bottom-5.inset-x-0.z-50 button').first()
    await expect(nopeButton).toBeVisible()
    await nopeButton.click({ force: true })
    await page.waitForTimeout(400)
  }

  await expect(titleLocator, `No se encontró la tarjeta con título ${title} en el deck.`).toBeVisible()
}

const openTopCardDetails = async (page: Page) => {
  const topCard = page.locator('div.absolute.inset-0.rounded-3xl.shadow-2xl').last()
  await expect(topCard).toBeVisible()

  const box = await topCard.boundingBox()
  expect(box).not.toBeNull()

  const startX = (box?.x ?? 0) + (box?.width ?? 0) / 2
  const startY = (box?.y ?? 0) + (box?.height ?? 0) * 0.7
  const endY = startY - 180

  await page.mouse.move(startX, startY)
  await page.mouse.down()
  await page.mouse.move(startX, endY, { steps: 12 })
  await page.mouse.up()

  await expect(page.getByRole('button', { name: /Añadir a favoritos|Quitar de favoritos/ })).toBeVisible()
}

test.describe('HU-48 / RF-06 / RI-02 / RNF-04', () => {
  let landlordToken = ''
  let tenantToken = ''
  let apartmentId: number | null = null
  let apartmentTitle = ''
  let createdApartment: ApartmentApiResponse | null = null

  test.beforeEach(async ({ request, page }) => {
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail)).token
    tenantToken = (await loginByApi(request, E2E_ENV.tenantEmail)).token

    apartmentTitle = uniqueApartmentTitle('FAV-HU48')
    createdApartment = await createApartmentByApi(request, landlordToken, apartmentTitle)
    apartmentId = createdApartment.id

    await page.route('**/api/apartments/deck/*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([createdApartment]),
      })
    })

    await loginUi(page, E2E_ENV.tenantEmail)
    await page.goto('/')
    await expect(page.getByText('Buscando tus opciones...').or(page.locator('div.relative.w-full.max-w-sm'))).toBeVisible()
  })

  test.afterEach(async ({ request, context }) => {
    if (apartmentId !== null) {
      await removeFavoriteByApi(request, tenantToken, apartmentId)
      await deleteApartmentByApi(request, landlordToken, apartmentId)
    }

    apartmentId = null
    apartmentTitle = ''
    createdApartment = null
    await context.clearCookies()
  })

  test('Permite guardar vivienda como favorita desde detalle y crea relación usuario-vivienda', async ({
    page,
    request,
  }) => {
    expect(apartmentId).not.toBeNull()
    const targetApartmentId = apartmentId as number

    await moveDeckUntilTitleVisible(page, apartmentTitle)
    await openTopCardDetails(page)

    const addFavoriteButton = page.getByRole('button', { name: 'Añadir a favoritos' })
    await expect(addFavoriteButton).toBeVisible()
    await addFavoriteButton.click()

    await expect(page.getByRole('button', { name: 'Quitar de favoritos' })).toBeVisible()

    await expect
      .poll(async () => {
        const favorites = await getFavoritesByApi(request, tenantToken)
        const saved = favorites.find((item) => item.apartmentId === targetApartmentId)
        return saved?.isFavorite ?? false
      })
      .toBe(true)
  })
})

test.describe('HU-49 / RF-07 / RI-02 / RNF-03', () => {
  let landlordToken = ''
  let tenantToken = ''
  let apartmentId: number | null = null
  let apartmentTitle = ''

  test.beforeEach(async ({ request, page }) => {
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail)).token
    tenantToken = (await loginByApi(request, E2E_ENV.tenantEmail)).token

    apartmentTitle = uniqueApartmentTitle('FAV-HU49')
    const apartment = await createApartmentByApi(request, landlordToken, apartmentTitle)
    apartmentId = apartment.id

    await addFavoriteByApi(request, tenantToken, apartment.id)

    await loginUi(page, E2E_ENV.tenantEmail)
    await page.goto('/favorites')
  })

  test.afterEach(async ({ request, context }) => {
    if (apartmentId !== null) {
      await removeFavoriteByApi(request, tenantToken, apartmentId)
      await deleteApartmentByApi(request, landlordToken, apartmentId)
    }

    apartmentId = null
    apartmentTitle = ''
    await context.clearCookies()
  })

  test('Muestra sección de favoritos con elementos guardados disponibles del usuario', async ({
    page,
    request,
  }) => {
    await expect(page.getByRole('heading', { name: 'Mis favoritos' })).toBeVisible()
    await expect(page.getByText(apartmentTitle)).toBeVisible()

    const favorites = await getFavoritesByApi(request, tenantToken)
    const saved = favorites.find((item) => item.apartmentId === apartmentId)
    expect(saved).toBeDefined()
    expect(saved?.availabilityStatus).toBe('AVAILABLE')
  })

  test('Permite decidir Like o Dislike desde favoritos y registra el swipe', async ({
    page,
  }) => {
    expect(apartmentId).not.toBeNull()
    const targetApartmentId = apartmentId as number

    const swipeCalls: Array<{ apartmentId: number; interest: boolean }> = []
    await page.route('**/api/apartments-matches/swipe/apartment/*/tenant', async (route) => {
      const request = route.request()
      const match = request.url().match(/apartment\/(\d+)\/tenant/)
      const routedApartmentId = match ? Number(match[1]) : 0
      const interest = request.postData()?.trim() === 'true'
      swipeCalls.push({ apartmentId: routedApartmentId, interest })

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: Date.now(),
          apartmentId: routedApartmentId,
          matchStatus: interest ? 'ACTIVE' : 'REJECTED',
        }),
      })
    })

    await expect(page.getByRole('button', { name: /Marcar .* como Like/ })).toBeVisible()
    await page.getByRole('button', { name: /Marcar .* como Like/ }).click()

    await expect.poll(() => swipeCalls.length).toBe(1)
    expect(swipeCalls[0]).toEqual({ apartmentId: targetApartmentId, interest: true })

    await expect(page.getByText(apartmentTitle)).not.toBeVisible()
    await expect(page.getByText('Todavía no tienes propiedades favoritas.')).toBeVisible()
  })
})
