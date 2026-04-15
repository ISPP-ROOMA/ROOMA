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

type AvailabilityBlockApiResponse = {
  id: number
  apartmentId: number
  blockDate: string
  startTime: string
  endTime: string
  slotDurationMinutes: number
  slots: { id: number; startTime: string; endTime: string; status: string }[]
}

// --- helpers solo para preparar datos ---

const loginByApi = async (
  request: APIRequestContext,
  email: string,
  scope: string
): Promise<AuthApiResponse> => {
  const deviceId = buildDeviceId(`pw-appt-${scope}`)
  const res = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
    data: { email, password: E2E_ENV.password, deviceId },
  })
  expect(res.ok()).toBeTruthy()
  const data = (await res.json()) as Omit<AuthApiResponse, 'deviceId'>
  return { ...data, deviceId }
}

const uniqueTitle = (suffix: string) =>
  `E2E Appt ${suffix} ${Date.now()}-${Math.floor(Math.random() * 100000)}`

const createApartmentByApi = async (
  request: APIRequestContext,
  token: string,
  title: string
): Promise<ApartmentApiResponse> => {
  const res = await request.post(`${E2E_ENV.apiUrl}/apartments`, {
    headers: { Authorization: `Bearer ${token}` },
    multipart: {
      data: {
        name: 'apartment.json',
        mimeType: 'application/json',
        buffer: Buffer.from(JSON.stringify({
          title,
          description: `Descripcion ${title}`,
          price: 600,
          bills: 'agua, luz',
          ubication: 'Calle Citas 7',
          state: 'ACTIVE',
        })),
      },
    },
  })
  expect(res.ok()).toBeTruthy()
  return (await res.json()) as ApartmentApiResponse
}

const deleteApartmentBestEffort = async (
  request: APIRequestContext,
  token: string,
  id: number
) => {
  try { await request.delete(`${E2E_ENV.apiUrl}/apartments/${id}`, { headers: { Authorization: `Bearer ${token}` } }) } catch { /* noop */ }
}

const seedAvailabilityBlock = async (
  request: APIRequestContext,
  token: string,
  apartmentId: number,
  blockDate: string,
  startTime: string,
  endTime: string
): Promise<AvailabilityBlockApiResponse> => {
  const res = await request.post(
    `${E2E_ENV.apiUrl}/appointments/blocks/apartment/${apartmentId}`,
    {
      headers: { Authorization: `Bearer ${token}` },
      data: { blockDate, startTime, endTime, slotDurationMinutes: 30 },
    }
  )
  expect(res.ok()).toBeTruthy()
  return (await res.json()) as AvailabilityBlockApiResponse
}

const tomorrow = () => {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return d.toISOString().split('T')[0]
}

const loginUi = async (page: Page, email: string) => {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(E2E_ENV.password)
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login$/)
}

// =====================================================================

test.describe('CU-03 / RF-60: Botones de visitas en la pagina de solicitudes', () => {
  test.beforeEach(async ({ request, page }) => {
    await loginByApi(request, E2E_ENV.landlordEmail, 'rf60-btns')
    await loginUi(page, E2E_ENV.landlordEmail)
  })

  test.afterEach(async ({ context }) => { await context.clearCookies() })

  test('Se muestran los botones "Organizar Visitas" y "Ver Visitas"', async ({ page }) => {
    await page.goto('/mis-solicitudes/recibidas')
    await expect(page.getByText('Organizar Visitas')).toBeVisible()
    await expect(page.getByText('Ver Visitas')).toBeVisible()
  })

  test('Click en "Ver Visitas" abre un modal', async ({ page }) => {
    await page.goto('/mis-solicitudes/recibidas')
    await page.getByText('Ver Visitas').click()

    // Abre el selector de inmueble o directamente el modal de visitas
    const selector = page.getByText('Selecciona un inmueble')
    const visitas = page.getByText('Visitas Programadas')
    const any = await selector.isVisible().catch(() => false) || await visitas.isVisible().catch(() => false)
    expect(any).toBeTruthy()
  })
})

test.describe('CU-03 / RF-60: Modal de crear visitas (Organizar Visitas)', () => {
  let landlordToken = ''
  let apartmentId = 0

  test.beforeEach(async ({ request, page }) => {
    const login = await loginByApi(request, E2E_ENV.landlordEmail, 'rf60-create')
    landlordToken = login.token

    const apt = await createApartmentByApi(request, landlordToken, uniqueTitle('rf60-create'))
    apartmentId = apt.id

    await loginUi(page, E2E_ENV.landlordEmail)
  })

  test.afterEach(async ({ request, context }) => {
    await deleteApartmentBestEffort(request, landlordToken, apartmentId)
    await context.clearCookies()
  })

  test('El modal de creacion muestra los campos de fecha y horas', async ({ page }) => {
    // Navegar con filtro para saltar selector de inmueble
    await page.goto(`/mis-solicitudes/recibidas?apartmentId=${apartmentId}`)
    await page.getByText('Organizar Visitas').click()

    // 1. Campos del formulario
    await expect(page.locator('input[type="date"]')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('input[type="time"]').first()).toBeVisible()
    await expect(page.locator('input[type="time"]').nth(1)).toBeVisible()

    // 2. Boton de submit
    await expect(page.getByText('CREAR VISITAS AUTOMÁTICAS')).toBeVisible()
  })

  test('Se puede rellenar el formulario y enviarlo', async ({ page }) => {
    await page.goto(`/mis-solicitudes/recibidas?apartmentId=${apartmentId}`)
    await page.getByText('Organizar Visitas').click()

    // 1. Rellenar campos
    const blockDate = tomorrow()
    await page.locator('input[type="date"]').fill(blockDate)
    await page.locator('input[type="time"]').first().fill('10:00')
    await page.locator('input[type="time"]').nth(1).fill('12:00')

    // 2. Enviar
    await page.getByText('CREAR VISITAS AUTOMÁTICAS').click()

    // 3. Tras exito el modal se cierra (toast de exito o desaparece el boton)
    await expect(page.getByText('CREAR VISITAS AUTOMÁTICAS')).not.toBeVisible({ timeout: 10000 })
  })

  test('El modal se puede cerrar con el boton X', async ({ page }) => {
    await page.goto(`/mis-solicitudes/recibidas?apartmentId=${apartmentId}`)
    await page.getByText('Organizar Visitas').click()

    await expect(page.locator('input[type="date"]')).toBeVisible({ timeout: 5000 })

    // Cerrar con el boton X del modal
    const closeBtn = page.locator('button').filter({ has: page.locator('svg.lucide-x') })
    await closeBtn.click()

    await expect(page.locator('input[type="date"]')).not.toBeVisible()
  })
})

test.describe('CU-03 / RF-61: Modal de ver visitas programadas', () => {
  let landlordToken = ''
  let apartmentId = 0

  test.beforeEach(async ({ request, page }) => {
    const login = await loginByApi(request, E2E_ENV.landlordEmail, 'rf61-view')
    landlordToken = login.token

    const apt = await createApartmentByApi(request, landlordToken, uniqueTitle('rf61-view'))
    apartmentId = apt.id

    await loginUi(page, E2E_ENV.landlordEmail)
  })

  test.afterEach(async ({ request, context }) => {
    await deleteApartmentBestEffort(request, landlordToken, apartmentId)
    await context.clearCookies()
  })

  test('Sin bloques creados muestra estado vacio', async ({ page }) => {
    await page.goto(`/mis-solicitudes/recibidas?apartmentId=${apartmentId}`)
    await page.getByText('Ver Visitas').click()

    await expect(
      page.getByText('Aún no has creado ningún bloque de visitas para este inmueble.')
    ).toBeVisible({ timeout: 10000 })
  })

  test('Con bloques creados muestra las franjas horarias y slots libres', async ({ page, request }) => {
    // Seed: crear un bloque de 10:00 a 12:00
    await seedAvailabilityBlock(request, landlordToken, apartmentId, tomorrow(), '10:00:00', '12:00:00')

    await page.goto(`/mis-solicitudes/recibidas?apartmentId=${apartmentId}`)
    await page.getByText('Ver Visitas').click()

    // 1. Cabecera del modal
    await expect(page.getByText('Visitas Programadas')).toBeVisible({ timeout: 10000 })

    // 2. Horarios del bloque
    await expect(page.getByText('10:00')).toBeVisible()
    await expect(page.getByText('12:00')).toBeVisible()

    // 3. Indicador de slots libres
    await expect(page.getByText(/libre/i).first()).toBeVisible()
  })

  test('Se puede expandir y colapsar un bloque', async ({ page, request }) => {
    await seedAvailabilityBlock(request, landlordToken, apartmentId, tomorrow(), '09:00:00', '10:00:00')

    await page.goto(`/mis-solicitudes/recibidas?apartmentId=${apartmentId}`)
    await page.getByText('Ver Visitas').click()

    await expect(page.getByText('Visitas Programadas')).toBeVisible({ timeout: 10000 })

    // El bloque se expande por defecto, hay slots visibles
    await expect(page.getByText(/libre/i).first()).toBeVisible()

    // Click en la cabecera del bloque para colapsar
    const blockHeader = page.locator('button').filter({ hasText: /09:00/ })
    await blockHeader.click()

    // Tras colapsar, los slots ya no se ven
    await expect(page.getByText(/libre/i)).not.toBeVisible({ timeout: 3000 })
  })

  test('El modal se cierra con el boton X', async ({ page }) => {
    await page.goto(`/mis-solicitudes/recibidas?apartmentId=${apartmentId}`)
    await page.getByText('Ver Visitas').click()

    await expect(page.getByText(/Visitas Programadas|Aún no has creado/)).toBeVisible({ timeout: 10000 })

    const closeBtn = page.locator('button').filter({ has: page.locator('svg.lucide-x') })
    await closeBtn.click()

    await expect(page.getByText('Visitas Programadas')).not.toBeVisible()
  })
})

test.describe('CU-03 / RF-61: Flujo completo crear y ver visitas', () => {
  let landlordToken = ''
  let apartmentId = 0

  test.beforeEach(async ({ request, page }) => {
    const login = await loginByApi(request, E2E_ENV.landlordEmail, 'rf61-flow')
    landlordToken = login.token

    const apt = await createApartmentByApi(request, landlordToken, uniqueTitle('rf61-flow'))
    apartmentId = apt.id

    await loginUi(page, E2E_ENV.landlordEmail)
  })

  test.afterEach(async ({ request, context }) => {
    await deleteApartmentBestEffort(request, landlordToken, apartmentId)
    await context.clearCookies()
  })

  test('Crear visitas desde el formulario y luego verlas en el modal de visitas', async ({ page }) => {
    const blockDate = tomorrow()

    // 1. Abrir modal de crear
    await page.goto(`/mis-solicitudes/recibidas?apartmentId=${apartmentId}`)
    await page.getByText('Organizar Visitas').click()
    await expect(page.locator('input[type="date"]')).toBeVisible({ timeout: 5000 })

    // 2. Rellenar y enviar
    await page.locator('input[type="date"]').fill(blockDate)
    await page.locator('input[type="time"]').first().fill('16:00')
    await page.locator('input[type="time"]').nth(1).fill('18:00')
    await page.getByText('CREAR VISITAS AUTOMÁTICAS').click()

    // 3. Esperar a que se cierre el modal de crear
    await expect(page.getByText('CREAR VISITAS AUTOMÁTICAS')).not.toBeVisible({ timeout: 10000 })

    // 4. Abrir modal de ver visitas
    await page.getByText('Ver Visitas').click()
    await expect(page.getByText('Visitas Programadas')).toBeVisible({ timeout: 10000 })

    // 5. Verificar que aparecen los horarios
    await expect(page.getByText('16:00')).toBeVisible()
    await expect(page.getByText('18:00')).toBeVisible()
  })
})
