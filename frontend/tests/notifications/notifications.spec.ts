import { type APIRequestContext, type Page } from '@playwright/test'
import { expect, test } from '../fixtures/auth.fixture'
import { buildDeviceId, E2E_ENV } from '../fixtures/testEnv'

type AuthApiResponse = {
  token: string
  role: string
  userId: number
  deviceId: string
}

type NotificationApiResponse = {
  id: number
  eventType: string
  timestamp: string
  description: string
  isRead: boolean
  link: string
  userId: number
}

// --- helpers solo para preparar datos ---

const loginByApi = async (
  request: APIRequestContext,
  email: string,
  scope: string
): Promise<AuthApiResponse> => {
  const deviceId = buildDeviceId(`pw-notif-${scope}`)
  const res = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
    data: { email, password: E2E_ENV.password, deviceId },
  })
  expect(res.ok()).toBeTruthy()
  const data = (await res.json()) as Omit<AuthApiResponse, 'deviceId'>
  return { ...data, deviceId }
}

const seedNotification = async (
  request: APIRequestContext,
  token: string,
  eventType: string,
  description: string,
  link: string
): Promise<NotificationApiResponse> => {
  const params = new URLSearchParams({ eventType, description, link })
  const res = await request.post(`${E2E_ENV.apiUrl}/notifications?${params.toString()}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  expect(res.ok()).toBeTruthy()
  return (await res.json()) as NotificationApiResponse
}

const clearNotifications = async (request: APIRequestContext, token: string) => {
  await request.patch(`${E2E_ENV.apiUrl}/notifications/mark-all-as-read`, {
    headers: { Authorization: `Bearer ${token}` },
  })
}

const loginUi = async (page: Page, email: string) => {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(E2E_ENV.password)
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login$/)
}

// =====================================================================

test.describe('CU-04 / RF-01: Generacion de notificaciones por eventos', () => {
  let tenantToken = ''

  test.beforeEach(async ({ request, page }) => {
    const login = await loginByApi(request, E2E_ENV.tenantEmail, 'rf01')
    tenantToken = login.token
    await clearNotifications(request, tenantToken)
    await loginUi(page, E2E_ENV.tenantEmail)
  })

  test.afterEach(async ({ context }) => { await context.clearCookies() })

  test('Se generan notificaciones de distintos tipos y se muestran en la pagina', async ({
    page, request,
  }) => {
    // 1. Seed
    await seedNotification(request, tenantToken, 'MATCH', 'Tienes un nuevo match con Piso Centro', '/mis-solicitudes/enviadas')
    await seedNotification(request, tenantToken, 'NEW_BILL', 'Se ha generado una nueva factura de 150€', '/invoices')

    // 2. Navegar y comprobar UI
    await page.goto('/notifications')
    await expect(page.getByText('Centro de notificaciones')).toBeVisible()
    await expect(page.getByText('Pendientes')).toBeVisible()
    await expect(page.getByText('Tienes un nuevo match con Piso Centro')).toBeVisible()
    await expect(page.getByText('Se ha generado una nueva factura de 150€')).toBeVisible()
  })
})

test.describe('CU-04 / RF-02: Listado cronologico de notificaciones', () => {
  let tenantToken = ''

  test.beforeEach(async ({ request, page }) => {
    const login = await loginByApi(request, E2E_ENV.tenantEmail, 'rf02')
    tenantToken = login.token
    await clearNotifications(request, tenantToken)
    await loginUi(page, E2E_ENV.tenantEmail)
  })

  test.afterEach(async ({ context }) => { await context.clearCookies() })

  test('Cada notificacion muestra titulo, mensaje y fecha', async ({ page, request }) => {
    await seedNotification(request, tenantToken, 'REVIEW', 'Has recibido una nueva valoracion', '/my-reviews')

    await page.goto('/notifications')

    // 1. Verificar estructura del primer <li>
    const firstItem = page.locator('li').first()
    await expect(firstItem).toBeVisible()
    await expect(firstItem.locator('h2')).toBeVisible()
    await expect(firstItem.locator('p').first()).toBeVisible()

    // 2. Debe haber un texto de fecha (formato es-ES)
    await expect(firstItem.locator('.text-xs')).toBeVisible()
  })

  test('Sin notificaciones pendientes se muestra estado vacio', async ({ page }) => {
    await page.goto('/notifications')
    await expect(page.getByText('No tienes notificaciones pendientes.')).toBeVisible()
  })
})

test.describe('CU-04 / RF-04: Navegacion desde notificacion', () => {
  let tenantToken = ''

  test.beforeEach(async ({ request, page }) => {
    const login = await loginByApi(request, E2E_ENV.tenantEmail, 'rf04')
    tenantToken = login.token
    await clearNotifications(request, tenantToken)
    await loginUi(page, E2E_ENV.tenantEmail)
  })

  test.afterEach(async ({ context }) => { await context.clearCookies() })

  test('Click en una notificacion navega al destino del link', async ({ page, request }) => {
    await seedNotification(request, tenantToken, 'NEW_BILL', 'Factura pendiente de pago', '/invoices')

    await page.goto('/notifications')
    await page.getByText('Factura pendiente de pago').click()

    await expect(page).toHaveURL(/\/invoices/)
  })

  test('Desde el perfil se puede acceder a la pagina de notificaciones', async ({ page }) => {
    await page.goto('/profile')

    const notifLink = page.locator('a[href="/notifications"]')
    await expect(notifLink).toBeVisible()
    await notifLink.click()

    await expect(page).toHaveURL(/\/notifications/)
    await expect(page.getByText('Centro de notificaciones')).toBeVisible()
  })
})

test.describe('CU-04 / RF-05: Marcado de notificaciones como leidas', () => {
  let tenantToken = ''

  test.beforeEach(async ({ request, page }) => {
    const login = await loginByApi(request, E2E_ENV.tenantEmail, 'rf05')
    tenantToken = login.token
    await clearNotifications(request, tenantToken)
    await loginUi(page, E2E_ENV.tenantEmail)
  })

  test.afterEach(async ({ context }) => { await context.clearCookies() })

  test('Al hacer click en una notificacion desaparece de la lista de pendientes', async ({
    page, request,
  }) => {
    await seedNotification(request, tenantToken, 'BILL_PAID', 'Tu compañero ha pagado la factura de agua', '/invoices')

    // 1. Verificar que aparece
    await page.goto('/notifications')
    await expect(page.getByText('Tu compañero ha pagado la factura de agua')).toBeVisible()

    // 2. Click -> navega y marca como leida
    await page.getByText('Tu compañero ha pagado la factura de agua').click()

    // 3. Volver y comprobar que ya no esta
    await page.goto('/notifications')
    await expect(page.getByText('Tu compañero ha pagado la factura de agua')).not.toBeVisible()
  })

  test('Despues de marcar todas como leidas la lista queda vacia', async ({ page, request }) => {
    await seedNotification(request, tenantToken, 'MATCH', 'Notif 1', '/profile')
    await seedNotification(request, tenantToken, 'REVIEW', 'Notif 2', '/my-reviews')

    await page.goto('/notifications')
    await expect(page.locator('li')).toHaveCount(2, { timeout: 10000 })

    // Marcar todas por API y recargar
    await clearNotifications(request, tenantToken)
    await page.reload()

    await expect(page.getByText('No tienes notificaciones pendientes.')).toBeVisible()
  })
})

test.describe('CU-04: Notificaciones para rol landlord', () => {
  let landlordToken = ''

  test.beforeEach(async ({ request, page }) => {
    const login = await loginByApi(request, E2E_ENV.landlordEmail, 'landlord')
    landlordToken = login.token
    await clearNotifications(request, landlordToken)
    await loginUi(page, E2E_ENV.landlordEmail)
  })

  test.afterEach(async ({ context }) => { await context.clearCookies() })

  test('Landlord ve sus notificaciones en la pagina', async ({ page, request }) => {
    await seedNotification(request, landlordToken, 'INVITATION_ACCEPTED', 'El inquilino ha aceptado tu invitacion', '/mis-solicitudes/recibidas')

    await page.goto('/notifications')
    await expect(page.getByText('Centro de notificaciones')).toBeVisible()
    await expect(page.getByText('El inquilino ha aceptado tu invitacion')).toBeVisible()
  })

  test('Landlord ve estado vacio cuando no hay pendientes', async ({ page }) => {
    await page.goto('/notifications')
    await expect(page.getByText('No tienes notificaciones pendientes.')).toBeVisible()
  })
})
