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
}

type MatchApiResponse = {
  id: number
  apartmentId: number
  matchStatus: string
}

type IncidentApiResponse = {
  id: number
  incidentCode: string
  apartmentId: number
}

// --- helpers solo para preparar datos ---

const loginByApi = async (
  request: APIRequestContext,
  email: string,
  scope: string
): Promise<AuthApiResponse> => {
  const deviceId = buildDeviceId(`pw-chat-${scope}`)
  const res = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
    data: { email, password: E2E_ENV.password, deviceId },
  })
  expect(res.ok()).toBeTruthy()
  const data = (await res.json()) as Omit<AuthApiResponse, 'deviceId'>
  return { ...data, deviceId }
}

const uniqueTitle = (suffix: string) =>
  `E2E Chat ${suffix} ${Date.now()}-${Math.floor(Math.random() * 100000)}`

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
          ubication: 'Calle Chat 7',
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

const addMemberByApi = async (
  request: APIRequestContext,
  token: string,
  apartmentId: number,
  tenantUserId: number
) => {
  const res = await request.post(
    `${E2E_ENV.apiUrl}/apartments/${apartmentId}/members/${tenantUserId}`,
    { headers: { Authorization: `Bearer ${token}` } }
  )
  expect(res.ok()).toBeTruthy()
}

const getMatchesForLandlord = async (
  request: APIRequestContext,
  token: string,
  userId: number,
  status: string
): Promise<MatchApiResponse[]> => {
  const res = await request.get(
    `${E2E_ENV.apiUrl}/apartment-match/landlord/${userId}?matchStatus=${status}`,
    { headers: { Authorization: `Bearer ${token}` } }
  )
  if (!res.ok()) return []
  return (await res.json()) as MatchApiResponse[]
}

const createIncidentByApi = async (
  request: APIRequestContext,
  token: string,
  apartmentId: number,
  title: string
): Promise<IncidentApiResponse> => {
  const res = await request.post(
    `${E2E_ENV.apiUrl}/apartments/${apartmentId}/incidents`,
    {
      headers: { Authorization: `Bearer ${token}` },
      multipart: {
        data: {
          name: 'incident.json',
          mimeType: 'application/json',
          buffer: Buffer.from(JSON.stringify({
            title,
            description: `Descripcion ${title}`,
            category: 'PLUMBING',
            zone: 'BATHROOM',
            urgency: 'MEDIUM',
          })),
        },
      },
    }
  )
  expect(res.ok()).toBeTruthy()
  return (await res.json()) as IncidentApiResponse
}

const loginUi = async (page: Page, email: string) => {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(E2E_ENV.password)
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login$/)
}

// =====================================================================

test.describe('CU-03 / RF-57: Chat del match - elementos de la UI', () => {
  let landlordToken = ''
  let landlordUserId = 0

  test.beforeEach(async ({ request, page }) => {
    const login = await loginByApi(request, E2E_ENV.landlordEmail, 'rf57-ui')
    landlordToken = login.token
    landlordUserId = login.userId
    await loginUi(page, E2E_ENV.landlordEmail)
  })

  test.afterEach(async ({ context }) => { await context.clearCookies() })

  test('La cabecera muestra "Chat del Match" y el estado de conexion', async ({ page, request }) => {
    const matches = await getMatchesForLandlord(request, landlordToken, landlordUserId, 'MATCH')
    if (matches.length === 0) { test.skip(); return }

    await page.goto(`/chat/${matches[0].id}`)

    await expect(page.getByText('Chat del Match')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText(/En línea|Desconectado/)).toBeVisible()
  })

  test('Muestra el campo de texto y el boton de enviar', async ({ page, request }) => {
    const matches = await getMatchesForLandlord(request, landlordToken, landlordUserId, 'MATCH')
    if (matches.length === 0) { test.skip(); return }

    await page.goto(`/chat/${matches[0].id}`)
    await expect(page.getByText('Chat del Match')).toBeVisible({ timeout: 10000 })

    // 1. Input de texto
    await expect(page.getByPlaceholder('Escribe un mensaje...')).toBeVisible()

    // 2. Boton de clip (adjuntar)
    await expect(page.locator('input[type="file"]')).toBeAttached()
  })

  test('El boton de enviar esta deshabilitado con el campo vacio', async ({ page, request }) => {
    const matches = await getMatchesForLandlord(request, landlordToken, landlordUserId, 'MATCH')
    if (matches.length === 0) { test.skip(); return }

    await page.goto(`/chat/${matches[0].id}`)
    await expect(page.getByText('Chat del Match')).toBeVisible({ timeout: 10000 })

    // El boton de enviar (ultimo boton con svg Send) esta disabled
    const sendBtn = page.locator('footer button').last()
    await expect(sendBtn).toBeDisabled()
  })

  test('Escribir texto habilita el boton de enviar', async ({ page, request }) => {
    const matches = await getMatchesForLandlord(request, landlordToken, landlordUserId, 'MATCH')
    if (matches.length === 0) { test.skip(); return }

    await page.goto(`/chat/${matches[0].id}`)
    await expect(page.getByText('Chat del Match')).toBeVisible({ timeout: 10000 })

    const input = page.getByPlaceholder('Escribe un mensaje...')
    await input.fill('Hola desde E2E')

    const sendBtn = page.locator('footer button').last()
    await expect(sendBtn).toBeEnabled()
  })

  test('Hay un boton de volver en la cabecera', async ({ page, request }) => {
    const matches = await getMatchesForLandlord(request, landlordToken, landlordUserId, 'MATCH')
    if (matches.length === 0) { test.skip(); return }

    await page.goto(`/chat/${matches[0].id}`)
    await expect(page.getByText('Chat del Match')).toBeVisible({ timeout: 10000 })

    const backBtn = page.locator('header button').first()
    await expect(backBtn).toBeVisible()
  })
})

test.describe('CU-03 / RF-57: Ruta de chat invalida', () => {
  test.beforeEach(async ({ page }) => {
    await loginUi(page, E2E_ENV.landlordEmail)
  })

  test.afterEach(async ({ context }) => { await context.clearCookies() })

  test('Un matchId no numerico muestra "Chat no válido"', async ({ page }) => {
    await page.goto('/chat/abc')
    await expect(page.getByText('Chat no válido')).toBeVisible({ timeout: 10000 })
  })
})

test.describe('CU-03 / RF-58: Adjuntar archivo en el chat', () => {
  let landlordToken = ''
  let landlordUserId = 0

  test.beforeEach(async ({ request, page }) => {
    const login = await loginByApi(request, E2E_ENV.landlordEmail, 'rf58-ui')
    landlordToken = login.token
    landlordUserId = login.userId
    await loginUi(page, E2E_ENV.landlordEmail)
  })

  test.afterEach(async ({ context }) => { await context.clearCookies() })

  test('Seleccionar un archivo muestra el nombre y el campo de caption', async ({ page, request }) => {
    const matches = await getMatchesForLandlord(request, landlordToken, landlordUserId, 'MATCH')
    if (matches.length === 0) { test.skip(); return }

    await page.goto(`/chat/${matches[0].id}`)
    await expect(page.getByText('Chat del Match')).toBeVisible({ timeout: 10000 })

    // 1. Adjuntar archivo
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'documento.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('Contenido de prueba'),
    })

    // 2. Se muestra el nombre del archivo
    await expect(page.getByText('documento.txt')).toBeVisible()

    // 3. Se muestra el campo de caption
    await expect(page.getByPlaceholder(/Añade un mensaje/i)).toBeVisible()
  })

  test('Se puede cancelar la seleccion de archivo con el boton X', async ({ page, request }) => {
    const matches = await getMatchesForLandlord(request, landlordToken, landlordUserId, 'MATCH')
    if (matches.length === 0) { test.skip(); return }

    await page.goto(`/chat/${matches[0].id}`)
    await expect(page.getByText('Chat del Match')).toBeVisible({ timeout: 10000 })

    // 1. Adjuntar archivo
    await page.locator('input[type="file"]').setInputFiles({
      name: 'borrar.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('noop'),
    })
    await expect(page.getByText('borrar.txt')).toBeVisible()

    // 2. Cancelar con el boton rojo
    const cancelBtn = page.locator('button').filter({ hasText: '✕' })
    await cancelBtn.click()

    // 3. El nombre del archivo desaparece y vuelve el input de texto
    await expect(page.getByText('borrar.txt')).not.toBeVisible()
    await expect(page.getByPlaceholder('Escribe un mensaje...')).toBeVisible()
  })

  test('Seleccionar una imagen muestra preview visual', async ({ page, request }) => {
    const matches = await getMatchesForLandlord(request, landlordToken, landlordUserId, 'MATCH')
    if (matches.length === 0) { test.skip(); return }

    await page.goto(`/chat/${matches[0].id}`)
    await expect(page.getByText('Chat del Match')).toBeVisible({ timeout: 10000 })

    // 1 pixel PNG transparente
    const pngBuffer = Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
      'base64'
    )

    await page.locator('input[type="file"]').setInputFiles({
      name: 'foto.png',
      mimeType: 'image/png',
      buffer: pngBuffer,
    })

    // Se muestra un <img> de preview
    await expect(page.locator('img[alt="preview"]')).toBeVisible()
  })
})

test.describe('CU-03: Chat de incidencias', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let apartmentId = 0
  let incidentId = 0

  test.beforeEach(async ({ request, page }) => {
    const ll = await loginByApi(request, E2E_ENV.landlordEmail, 'chat-inc-ll')
    landlordToken = ll.token

    const tn = await loginByApi(request, E2E_ENV.tenantEmail, 'chat-inc-tn')
    tenantToken = tn.token
    tenantUserId = tn.userId

    // Seed: inmueble + miembro + incidencia
    const apt = await createApartmentByApi(request, landlordToken, uniqueTitle('chat-inc'))
    apartmentId = apt.id
    await addMemberByApi(request, landlordToken, apartmentId, tenantUserId)

    const incident = await createIncidentByApi(request, tenantToken, apartmentId, 'Fuga de agua E2E')
    incidentId = incident.id

    await loginUi(page, E2E_ENV.tenantEmail)
  })

  test.afterEach(async ({ request, context }) => {
    await deleteApartmentBestEffort(request, landlordToken, apartmentId)
    await context.clearCookies()
  })

  test('La cabecera muestra "Chat de la Incidencia"', async ({ page }) => {
    await page.goto(`/chat/incidents/${incidentId}`)
    await expect(page.getByText('Chat de la Incidencia')).toBeVisible({ timeout: 10000 })
  })

  test('Un chat de incidencia nuevo muestra "No hay mensajes aún"', async ({ page }) => {
    await page.goto(`/chat/incidents/${incidentId}`)
    await expect(page.getByText('Chat de la Incidencia')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('No hay mensajes aún')).toBeVisible({ timeout: 5000 })
  })

  test('Muestra el campo de texto para escribir mensajes', async ({ page }) => {
    await page.goto(`/chat/incidents/${incidentId}`)
    await expect(page.getByText('Chat de la Incidencia')).toBeVisible({ timeout: 10000 })
    await expect(page.getByPlaceholder('Escribe un mensaje...')).toBeVisible()
  })

  test('Se puede adjuntar un archivo en el chat de incidencia', async ({ page }) => {
    await page.goto(`/chat/incidents/${incidentId}`)
    await expect(page.getByText('Chat de la Incidencia')).toBeVisible({ timeout: 10000 })

    await page.locator('input[type="file"]').setInputFiles({
      name: 'foto-fuga.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('evidencia'),
    })

    await expect(page.getByText('foto-fuga.txt')).toBeVisible()
    await expect(page.getByPlaceholder(/Añade un mensaje/i)).toBeVisible()
  })
})

test.describe('CU-03: Navegacion al chat desde detalle de incidencia', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let apartmentId = 0
  let incidentId = 0

  test.beforeEach(async ({ request, page }) => {
    const ll = await loginByApi(request, E2E_ENV.landlordEmail, 'chat-nav-ll')
    landlordToken = ll.token

    const tn = await loginByApi(request, E2E_ENV.tenantEmail, 'chat-nav-tn')
    tenantToken = tn.token
    tenantUserId = tn.userId

    const apt = await createApartmentByApi(request, landlordToken, uniqueTitle('chat-nav'))
    apartmentId = apt.id
    await addMemberByApi(request, landlordToken, apartmentId, tenantUserId)

    const incident = await createIncidentByApi(request, tenantToken, apartmentId, 'Tuberia rota E2E')
    incidentId = incident.id

    await loginUi(page, E2E_ENV.tenantEmail)
  })

  test.afterEach(async ({ request, context }) => {
    await deleteApartmentBestEffort(request, landlordToken, apartmentId)
    await context.clearCookies()
  })

  test('El boton "Abrir conversacion" en detalle de incidencia navega al chat', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    const chatBtn = page.getByText('Abrir conversacion')
    await expect(chatBtn).toBeVisible({ timeout: 10000 })
    await chatBtn.click()

    await expect(page).toHaveURL(new RegExp(`/chat/incidents/${incidentId}`))
    await expect(page.getByText('Chat de la Incidencia')).toBeVisible({ timeout: 10000 })
  })
})
