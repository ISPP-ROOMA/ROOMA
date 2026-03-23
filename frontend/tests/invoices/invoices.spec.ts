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

type TenantDebtApiResponse = {
  id: number
  amount: number
  status: 'PENDING' | 'PAID'
  bill: {
    id: number
    reference: string
    totalAmount: number
    duDate: string
    status: 'PENDING' | 'PAID' | 'CANCELLED'
  }
}

type BillApiResponse = {
  id: number
  reference: string
  totalAmount: number
  duDate: string
  status: 'PENDING' | 'PAID' | 'CANCELLED'
  tenantDebts?: Array<{
    id: number
    amount: number
    status: 'PENDING' | 'PAID'
    user: { id: number; email: string }
  }>
}

const uniqueApartmentTitle = (suffix: string) =>
  `E2E ${suffix} ${Date.now()}-${Math.floor(Math.random() * 100000)}`

const uniqueTenantEmail = (suffix: string) =>
  `tenant.${suffix}.${Date.now()}.${Math.floor(Math.random() * 100000)}@test.com`

const tinyPngBase64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/w8AAgMBgN2m6QAAAABJRU5ErkJggg=='

const loginByApi = async (
  request: APIRequestContext,
  email: string,
  scope: string
): Promise<AuthApiResponse> => {
  const deviceId = buildDeviceId(`pw-invoices-${scope}`)

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
      deviceId: buildDeviceId(`pw-invoices-register-${scope}`),
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
            price: 750,
            bills: 'agua, luz',
            ubication: 'Calle Invoices 10',
            state: 'ACTIVE',
          })
        ),
      },
    },
  })

  expect(response.ok(), `No se pudo crear inmueble de prueba. Status ${response.status()}`).toBeTruthy()
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

const createBillByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  apartmentId: number,
  reference: string,
  totalAmount: number,
  duDate: string
): Promise<BillApiResponse> => {
  const response = await request.post(`${E2E_ENV.apiUrl}/bills/apartment/${apartmentId}`, {
    headers: {
      Authorization: `Bearer ${landlordToken}`,
    },
    data: {
      reference,
      totalAmount,
      duDate,
    },
  })

  expect(response.ok(), `No se pudo crear factura ${reference}. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as BillApiResponse
}

const getMyDebtsByApi = async (
  request: APIRequestContext,
  tenantToken: string
): Promise<TenantDebtApiResponse[]> => {
  const response = await request.get(`${E2E_ENV.apiUrl}/bills/me/debts`, {
    headers: {
      Authorization: `Bearer ${tenantToken}`,
    },
  })

  expect(response.ok(), `No se pudieron obtener deudas del tenant. Status ${response.status()}`).toBeTruthy()
  return (await response.json()) as TenantDebtApiResponse[]
}

const payDebtByApi = async (request: APIRequestContext, token: string, debtId: number) => {
  const response = await request.post(`${E2E_ENV.apiUrl}/bills/debts/${debtId}/pay`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  expect(response.ok(), `No se pudo pagar deuda ${debtId}. Status ${response.status()}`).toBeTruthy()
}

const loginUi = async (page: Page, email: string) => {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(E2E_ENV.password)
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login$/)
}

const formatDateISO = (daysDelta: number) => {
  const d = new Date()
  d.setDate(d.getDate() + daysDelta)
  return d.toISOString().slice(0, 10)
}

test.describe('HU-12 / RF-15 / RF-16', () => {
  let landlordToken = ''
  let apartmentId: number | null = null
  const tempTenants: string[] = []

  test.beforeEach(async ({ request, page }) => {
    tempTenants.length = 0
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'hu12-landlord')).token

    const tenantEmail = uniqueTenantEmail('hu12')
    tempTenants.push(tenantEmail)
    await registerTenantByApi(request, tenantEmail, 'hu12-tenant-register')
    const tenant = await loginByApi(request, tenantEmail, 'hu12-tenant-login')

    const apt = await createApartmentByApi(request, landlordToken, uniqueApartmentTitle('HU12'))
    apartmentId = apt.id
    await addMemberByApi(request, landlordToken, apt.id, tenant.userId)

    await loginUi(page, E2E_ENV.landlordEmail)
    await page.goto(`/apartments/${apt.id}/new-bill`)
  })

  test.afterEach(async ({ request, context }) => {
    if (apartmentId !== null) {
      await deleteApartmentByApi(request, landlordToken, apartmentId)
    }

    for (const email of tempTenants) {
      await cleanupTenantByEmail(request, email)
    }

    apartmentId = null
    tempTenants.length = 0
    await context.clearCookies()
  })

  test('Formulario permite adjuntar archivo y valida campos obligatorios antes de crear factura', async ({
    page,
    request,
  }) => {
    await expect(page.getByRole('heading', { name: /Nueva Factura/i })).toBeVisible()
    await expect(page.locator('select')).toBeVisible()
    await expect(page.locator('input[type="number"]').first()).toBeVisible()
    await expect(page.locator('input[type="date"]')).toBeVisible()
    await expect(page.locator('input[type="file"]')).toHaveCount(1)

    await page.locator('input[type="number"]').first().fill('120.50')
    await page.locator('input[type="date"]').fill(formatDateISO(5))

    await page.getByRole('button', { name: 'Subir y Notificar' }).click()
    await expect(page.getByText('Selecciona un concepto')).toBeVisible()

    await page.locator('select').selectOption('Agua')
    await page.locator('input[type="file"]').setInputFiles({
      name: 'bill-proof.png',
      mimeType: 'image/png',
      buffer: Buffer.from(tinyPngBase64, 'base64'),
    })
    await expect(page.getByText('bill-proof.png')).toBeVisible()

    await page.getByRole('button', { name: 'Subir y Notificar' }).click()
    await expect(page.getByText('Factura creada y notificada a los inquilinos')).toBeVisible()
    await expect(page).toHaveURL(new RegExp(`/apartments/${apartmentId}$`))

    const billsResponse = await request.get(`${E2E_ENV.apiUrl}/bills/apartment/${apartmentId}`, {
      headers: { Authorization: `Bearer ${landlordToken}` },
    })
    expect(billsResponse.ok()).toBeTruthy()
    const bills = (await billsResponse.json()) as BillApiResponse[]
    expect(bills.some((b) => b.reference === 'Agua')).toBe(true)
  })
})

test.describe('HU-13 / RF-16 / RF-17 (parcial)', () => {
  let landlordToken = ''
  let apartmentId: number | null = null
  const tempTenants: string[] = []

  test.beforeEach(async ({ request, page }) => {
    tempTenants.length = 0
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'hu13-landlord')).token

    const tenantAEmail = uniqueTenantEmail('hu13-a')
    const tenantBEmail = uniqueTenantEmail('hu13-b')
    tempTenants.push(tenantAEmail, tenantBEmail)

    await registerTenantByApi(request, tenantAEmail, 'hu13-tenant-a-register')
    await registerTenantByApi(request, tenantBEmail, 'hu13-tenant-b-register')
    const tenantA = await loginByApi(request, tenantAEmail, 'hu13-tenant-a-login')
    const tenantB = await loginByApi(request, tenantBEmail, 'hu13-tenant-b-login')

    const apt = await createApartmentByApi(request, landlordToken, uniqueApartmentTitle('HU13'))
    apartmentId = apt.id
    await addMemberByApi(request, landlordToken, apt.id, tenantA.userId)
    await addMemberByApi(request, landlordToken, apt.id, tenantB.userId)

    await loginUi(page, E2E_ENV.landlordEmail)
    await page.goto(`/apartments/${apt.id}/new-bill`)
  })

  test.afterEach(async ({ request, context }) => {
    if (apartmentId !== null) {
      await deleteApartmentByApi(request, landlordToken, apartmentId)
    }

    for (const email of tempTenants) {
      await cleanupTenantByEmail(request, email)
    }

    apartmentId = null
    tempTenants.length = 0
    await context.clearCookies()
  })

  test('Recalcula reparto al seleccionar/desseleccionar y habilita envío solo con reparto completo', async ({
    page,
  }) => {
    await page.locator('select').selectOption('Electricidad')
    await page.locator('input[type="number"]').first().fill('100')
    await page.locator('input[type="date"]').fill(formatDateISO(7))

    const rows = page.locator('ul.space-y-3 > li')

    await expect(rows).toHaveCount(2)

    await rows.nth(0).locator('button').first().click()
    await rows.nth(1).locator('button').first().click()

    await expect(page.getByText('Reparte el 100 % del importe antes de enviar')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Subir y Notificar' })).toBeDisabled()

    await rows.nth(0).locator('input[type="number"]').fill('30')
    await rows.nth(1).locator('input[type="number"]').fill('60')

    await expect(page.getByText(/Faltan/i)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Subir y Notificar' })).toBeDisabled()

    await rows.nth(1).locator('input[type="number"]').fill('70')

    await expect(page.getByText('✓ Reparto completo')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Subir y Notificar' })).toBeEnabled()
  })
})

test.describe('HU-14 / RF-22 / RF-23 (parcial)', () => {
  let landlordToken = ''
  let tenantToken = ''
  let apartmentId: number | null = null
  const tempTenants: string[] = []

  test.beforeEach(async ({ request, page }) => {
    tempTenants.length = 0
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'hu14-landlord')).token

    const tenantEmail = uniqueTenantEmail('hu14')
    tempTenants.push(tenantEmail)
    await registerTenantByApi(request, tenantEmail, 'hu14-tenant-register')
    const tenant = await loginByApi(request, tenantEmail, 'hu14-tenant-login')
    tenantToken = tenant.token

    const apt = await createApartmentByApi(request, landlordToken, uniqueApartmentTitle('HU14'))
    apartmentId = apt.id
    await addMemberByApi(request, landlordToken, apt.id, tenant.userId)

    await createBillByApi(
      request,
      landlordToken,
      apt.id,
      'HU14-PENDIENTE',
      90,
      formatDateISO(2)
    )
    await createBillByApi(request, landlordToken, apt.id, 'HU14-PAGADA', 60, formatDateISO(-1))

    const debts = await getMyDebtsByApi(request, tenantToken)
    const paidDebt = debts.find((d) => d.bill.reference === 'HU14-PAGADA')
    expect(paidDebt).toBeDefined()
    await payDebtByApi(request, tenantToken, paidDebt!.id)

    await loginUi(page, E2E_ENV.landlordEmail)
    await page.goto(`/apartments/${apt.id}/bills`)
  })

  test.afterEach(async ({ request, context }) => {
    if (apartmentId !== null) {
      await deleteApartmentByApi(request, landlordToken, apartmentId)
    }

    for (const email of tempTenants) {
      await cleanupTenantByEmail(request, email)
    }

    apartmentId = null
    tenantToken = ''
    tempTenants.length = 0
    await context.clearCookies()
  })

  test('Panel muestra facturas pendientes/pagadas y permite abrir desglose por factura', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Facturas del piso' })).toBeVisible()
    await expect(page.getByText('HU14-PENDIENTE')).toBeVisible()

    const tabs = page.locator('div.flex.gap-2.px-5.mt-4')
    await tabs.getByRole('button', { name: /^Pagadas/ }).click()
    await expect(page.getByText('HU14-PAGADA')).toBeVisible()

    await page.getByRole('button', { name: /HU14-PAGADA/ }).first().click()
    await expect(page).toHaveURL(new RegExp(`/apartments/${apartmentId}/bills/\\d+$`))
    await expect(page.getByText('Progreso de cobro')).toBeVisible()
  })
})

test.describe('HU-17 / RF-18 / RF-20', () => {
  let landlordToken = ''
  let apartmentId: number | null = null
  const tempTenants: string[] = []

  test.beforeEach(async ({ request, page }) => {
    tempTenants.length = 0
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'hu17-landlord')).token

    const tenantEmail = uniqueTenantEmail('hu17')
    tempTenants.push(tenantEmail)
    await registerTenantByApi(request, tenantEmail, 'hu17-tenant-register')
    const tenant = await loginByApi(request, tenantEmail, 'hu17-tenant-login')
    const apt = await createApartmentByApi(request, landlordToken, uniqueApartmentTitle('HU17'))
    apartmentId = apt.id
    await addMemberByApi(request, landlordToken, apt.id, tenant.userId)

    await createBillByApi(request, landlordToken, apt.id, 'HU17-VENCIDO', 80, formatDateISO(-2))
    await createBillByApi(request, landlordToken, apt.id, 'HU17-PROXIMO', 50, formatDateISO(2))
    await createBillByApi(request, landlordToken, apt.id, 'HU17-ALDIA', 30, formatDateISO(10))

    await loginUi(page, tenantEmail)
    await page.goto('/invoices')
  })

  test.afterEach(async ({ request, context }) => {
    if (apartmentId !== null) {
      await deleteApartmentByApi(request, landlordToken, apartmentId)
    }

    for (const email of tempTenants) {
      await cleanupTenantByEmail(request, email)
    }

    apartmentId = null
    tempTenants.length = 0
    await context.clearCookies()
  })

  test('Dashboard pendiente muestra prioridad por urgencia y total acumulado', async ({ page }) => {
    await expect(page.getByRole('button', { name: /Pendientes/ })).toBeVisible()
    await expect(page.getByRole('button', { name: /Pendientes/ })).toContainText('(3)')

    await expect(page.getByText('HU17-VENCIDO')).toBeVisible()
    await expect(page.getByText('HU17-PROXIMO')).toBeVisible()
    await expect(page.getByText('HU17-ALDIA')).toBeVisible()

    const overdueCard = page.locator('li').filter({ hasText: 'HU17-VENCIDO' }).first()
    await expect(overdueCard).toContainText('Vencido')

    const soonCard = page.locator('li').filter({ hasText: 'HU17-PROXIMO' }).first()
    await expect(soonCard).toContainText(/Vence en|Vence hoy/)

    await overdueCard.click()
    await expect(page).toHaveURL(/\/invoices\/\d+$/)
    await expect(page.getByRole('button', { name: /Pagar/ })).toBeVisible()
  })
})

test.describe('HU-18 / RF-18', () => {
  let landlordToken = ''
  let apartmentId: number | null = null
  const tempTenants: string[] = []

  test.beforeEach(async ({ request, page }) => {
    tempTenants.length = 0
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'hu18-landlord')).token

    const tenantEmail = uniqueTenantEmail('hu18')
    tempTenants.push(tenantEmail)
    await registerTenantByApi(request, tenantEmail, 'hu18-tenant-register')
    const tenant = await loginByApi(request, tenantEmail, 'hu18-tenant-login')

    const apt = await createApartmentByApi(request, landlordToken, uniqueApartmentTitle('HU18'))
    apartmentId = apt.id
    await addMemberByApi(request, landlordToken, apt.id, tenant.userId)

    await createBillByApi(request, landlordToken, apt.id, 'HU18-HISTORIAL', 45, formatDateISO(-1))

    const debts = await getMyDebtsByApi(request, tenant.token)
    const debt = debts.find((d) => d.bill.reference === 'HU18-HISTORIAL')
    expect(debt).toBeDefined()
    await payDebtByApi(request, tenant.token, debt!.id)

    await loginUi(page, tenantEmail)
    await page.goto('/invoices')
  })

  test.afterEach(async ({ request, context }) => {
    if (apartmentId !== null) {
      await deleteApartmentByApi(request, landlordToken, apartmentId)
    }

    for (const email of tempTenants) {
      await cleanupTenantByEmail(request, email)
    }

    apartmentId = null
    tempTenants.length = 0
    await context.clearCookies()
  })

  test('Historial muestra pagos realizados en orden y badge pagado', async ({ page }) => {
    await page.getByRole('button', { name: /Historial/ }).click()
    await expect(page.getByText('HU18-HISTORIAL')).toBeVisible()
    await expect(page.getByText('Pagado')).toBeVisible()
  })
})

test.describe('HU-19 / RF-19 (parcial)', () => {
  let landlordToken = ''
  let tenantToken = ''
  let apartmentId: number | null = null
  let targetDebtId: number | null = null
  const tempTenants: string[] = []

  test.beforeEach(async ({ request, page }) => {
    tempTenants.length = 0
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'hu19-landlord')).token

    const tenantEmail = uniqueTenantEmail('hu19')
    tempTenants.push(tenantEmail)
    await registerTenantByApi(request, tenantEmail, 'hu19-tenant-register')
    const tenant = await loginByApi(request, tenantEmail, 'hu19-tenant-login')
    tenantToken = tenant.token

    const apt = await createApartmentByApi(request, landlordToken, uniqueApartmentTitle('HU19'))
    apartmentId = apt.id
    await addMemberByApi(request, landlordToken, apt.id, tenant.userId)

    await createBillByApi(request, landlordToken, apt.id, 'HU19-ONECLICK', 65, formatDateISO(4))
    const debts = await getMyDebtsByApi(request, tenant.token)
    const debt = debts.find((d) => d.bill.reference === 'HU19-ONECLICK')
    expect(debt).toBeDefined()
    targetDebtId = debt!.id

    await loginUi(page, tenantEmail)
    await page.goto(`/invoices/${targetDebtId}`)
  })

  test.afterEach(async ({ request, context }) => {
    if (apartmentId !== null) {
      await deleteApartmentByApi(request, landlordToken, apartmentId)
    }

    for (const email of tempTenants) {
      await cleanupTenantByEmail(request, email)
    }

    apartmentId = null
    targetDebtId = null
    tenantToken = ''
    tempTenants.length = 0
    await context.clearCookies()
  })

  test('Permite pagar con un clic y navega a confirmación de éxito', async ({ page, request }) => {
    await expect(page.getByRole('button', { name: /Pagar/ })).toBeVisible()

    await page.getByRole('button', { name: /Pagar/ }).click()
    await expect(page).toHaveURL(/\/invoices\/\d+\/success$/)
    await expect(page.getByRole('heading', { name: '¡Pago completado con éxito!' })).toBeVisible()

    const debts = await getMyDebtsByApi(request, tenantToken)
    const paid = debts.find((d) => d.id === targetDebtId)
    expect(paid?.status).toBe('PAID')
  })
})
