import { type APIRequestContext, type Page } from '@playwright/test'
import { expect, test } from '../fixtures/auth.fixture'
import { buildDeviceId, E2E_ENV } from '../fixtures/testEnv'

/* ------------------------------------------------------------------ */
/*  Tipos auxiliares                                                    */
/* ------------------------------------------------------------------ */

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

type IncidentApiResponse = {
  id: number
  incidentCode: string
  apartmentId: number
  tenantId: number
  tenantEmail: string
  title: string
  description: string
  category: string
  zone: string
  urgency: string
  status: string
  photos: string[]
  createdAt: string
  updatedAt: string
  statusHistory: Array<{ status: string; changedAt: string; userId: number; userEmail: string }>
}

/* ------------------------------------------------------------------ */
/*  Generadores unicos                                                 */
/* ------------------------------------------------------------------ */

const uniqueApartmentTitle = (suffix: string) =>
  `E2E ${suffix} ${Date.now()}-${Math.floor(Math.random() * 100000)}`

const uniqueTenantEmail = (suffix: string) =>
  `tenant.incidents.${suffix}.${Date.now()}.${Math.floor(Math.random() * 100000)}@test.com`

/* ------------------------------------------------------------------ */
/*  Helpers de API                                                     */
/* ------------------------------------------------------------------ */

const loginByApi = async (
  request: APIRequestContext,
  email: string,
  scope: string
): Promise<AuthApiResponse> => {
  const deviceId = buildDeviceId(`pw-incidents-${scope}`)
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
      deviceId: buildDeviceId(`pw-incidents-register-${scope}`),
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
        deviceId: buildDeviceId('pw-incidents-cleanup-best-effort'),
      },
    })

    if (!loginResponse.ok()) return

    const login = (await loginResponse.json()) as Omit<AuthApiResponse, 'deviceId'>
    if (!login.token) return

    await request.delete(`${E2E_ENV.apiUrl}/users/profile`, {
      headers: { Authorization: `Bearer ${login.token}` },
    })
  } catch {
    // Ignorar errores de cleanup para no enmascarar fallos funcionales.
  }
}

const createApartmentByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  title: string
): Promise<ApartmentApiResponse> => {
  const response = await request.post(`${E2E_ENV.apiUrl}/apartments`, {
    headers: { Authorization: `Bearer ${landlordToken}` },
    multipart: {
      data: {
        name: 'apartment.json',
        mimeType: 'application/json',
        buffer: Buffer.from(
          JSON.stringify({
            title,
            description: `Descripción ${title}`,
            price: 600,
            bills: 'agua, luz',
            ubication: 'Calle Incidencias 7',
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
      headers: { Authorization: `Bearer ${landlordToken}` },
    })
  } catch {
    // Ignorar errores de cleanup.
  }
}

const addMemberByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  apartmentId: number,
  tenantUserId: number
) => {
  const response = await request.post(
    `${E2E_ENV.apiUrl}/apartments/${apartmentId}/members`,
    {
      headers: {
        Authorization: `Bearer ${landlordToken}`,
        'Content-Type': 'application/json',
      },
      data: {
        userId: tenantUserId,
        joinDate: new Date().toISOString().split('T')[0],
      },
    }
  )

  expect(
    response.ok(),
    `No se pudo añadir miembro ${tenantUserId} al piso ${apartmentId}. Status ${response.status()}`
  ).toBeTruthy()
}

const createIncidentByApi = async (
  request: APIRequestContext,
  tenantToken: string,
  apartmentId: number,
  overrides: Partial<{
    title: string
    description: string
    category: string
    zone: string
    urgency: string
  }> = {}
): Promise<IncidentApiResponse> => {
  const payload = {
    title: overrides.title ?? 'Fuga en el lavabo',
    description: overrides.description ?? 'Gotea continuamente desde hace dos dias',
    category: overrides.category ?? 'PLUMBING',
    zone: overrides.zone ?? 'BATHROOM',
    urgency: overrides.urgency ?? 'MEDIUM',
  }

  const response = await request.post(
    `${E2E_ENV.apiUrl}/apartments/${apartmentId}/incidents`,
    {
      headers: { Authorization: `Bearer ${tenantToken}` },
      multipart: {
        data: {
          name: 'incident.json',
          mimeType: 'application/json',
          buffer: Buffer.from(JSON.stringify(payload)),
        },
      },
    }
  )

  expect(
    response.ok(),
    `No se pudo crear incidencia en piso ${apartmentId}. Status ${response.status()}`
  ).toBeTruthy()
  return (await response.json()) as IncidentApiResponse
}

const updateIncidentStatusByApi = async (
  request: APIRequestContext,
  landlordToken: string,
  apartmentId: number,
  incidentId: number,
  status: string
): Promise<IncidentApiResponse> => {
  const response = await request.patch(
    `${E2E_ENV.apiUrl}/apartments/${apartmentId}/incidents/${incidentId}/status`,
    {
      headers: {
        Authorization: `Bearer ${landlordToken}`,
        'Content-Type': 'application/json',
      },
      data: { status },
    }
  )

  expect(
    response.ok(),
    `No se pudo actualizar estado de incidencia ${incidentId} a ${status}. Status ${response.status()}`
  ).toBeTruthy()
  return (await response.json()) as IncidentApiResponse
}

const loginUi = async (page: Page, email: string) => {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(E2E_ENV.password)
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login$/)
}

/* ================================================================== */
/*  TEST SUITE: CU-10 - Gestion de incidencias                        */
/* ================================================================== */

test.describe('CU-10 / RF-27: Alta de incidencia por inquilino', () => {
  let landlordToken = ''
  let tenantUserId = 0
  let apartmentId: number | null = null
  const tenantEmail = uniqueTenantEmail('rf27')
  const apartmentTitle = uniqueApartmentTitle('RF27')

  test.beforeEach(async ({ request }) => {
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'rf27-landlord')).token

    await registerTenantByApi(request, tenantEmail, 'rf27-register')
    const tenantLogin = await loginByApi(request, tenantEmail, 'rf27-tenant')
    tenantUserId = tenantLogin.userId

    const apt = await createApartmentByApi(request, landlordToken, apartmentTitle)
    apartmentId = apt.id

    await addMemberByApi(request, landlordToken, apt.id, tenantUserId)

    await loginUi(page, tenantEmail)
  })

  test.afterEach(async ({ request, context }) => {
    try {
      if (apartmentId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentId)
      }
      await cleanupTenantByEmailBestEffort(request, tenantEmail)
    } finally {
      apartmentId = null
      await context.clearCookies()
    }
  })

  test('Inquilino ve la pagina de incidencias y puede abrir el formulario', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)

    await expect(page.getByText('Incidencias de la vivienda')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Nueva incidencia' })).toBeVisible()

    await page.getByRole('button', { name: 'Nueva incidencia' }).click()
    await expect(page.getByText('Formulario de nueva incidencia')).toBeVisible()
    await expect(page.getByText('Titulo')).toBeVisible()
    await expect(page.getByText('Descripcion')).toBeVisible()
    await expect(page.getByText('Categoria')).toBeVisible()
    await expect(page.getByText('Zona')).toBeVisible()
    await expect(page.getByText('Urgencia')).toBeVisible()
  })

  test('Crea incidencia rellenando todos los campos del formulario', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)
    await page.getByRole('button', { name: 'Nueva incidencia' }).click()

    const incidentTitle = `Averia grifo ${Date.now()}`

    // Rellenar titulo
    await page.locator('input').filter({ hasText: '' }).first().fill(incidentTitle)

    // Seleccionar categoria
    await page.locator('select').first().selectOption('PLUMBING')

    // Rellenar descripcion
    await page.locator('textarea').fill('El grifo de la cocina gotea continuamente y pierde agua.')

    // Seleccionar zona
    await page.locator('select').nth(1).selectOption('KITCHEN')

    // Seleccionar urgencia
    await page.getByRole('button', { name: 'Alta' }).click()

    // Enviar formulario
    await page.getByRole('button', { name: 'Crear incidencia' }).click()

    // Verificar que se ha creado (toast de exito o la incidencia aparece en la lista)
    await expect(page.getByText('Incidencia creada correctamente')).toBeVisible({ timeout: 10000 })
  })

  test('Valida que titulo y descripcion son obligatorios', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)
    await page.getByRole('button', { name: 'Nueva incidencia' }).click()

    // Intentar enviar sin rellenar nada
    await page.getByRole('button', { name: 'Crear incidencia' }).click()
    await expect(page.getByText('El titulo es obligatorio')).toBeVisible({ timeout: 5000 })

    // Rellenar solo titulo e intentar de nuevo
    await page.locator('input').filter({ hasText: '' }).first().fill('Un titulo de prueba')
    await page.getByRole('button', { name: 'Crear incidencia' }).click()
    await expect(page.getByText('La descripcion es obligatoria')).toBeVisible({ timeout: 5000 })
  })

  test('Boton Limpiar resetea el formulario', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)
    await page.getByRole('button', { name: 'Nueva incidencia' }).click()

    // Rellenar campos
    await page.locator('input').filter({ hasText: '' }).first().fill('Titulo temporal')
    await page.locator('textarea').fill('Descripcion temporal')
    await page.getByRole('button', { name: 'Alta' }).click()

    // Limpiar
    await page.getByRole('button', { name: 'Limpiar' }).click()

    // Verificar que los campos estan vacios
    await expect(page.locator('input').filter({ hasText: '' }).first()).toHaveValue('')
    await expect(page.locator('textarea')).toHaveValue('')
  })
})

/* ------------------------------------------------------------------ */

test.describe('CU-10 / RF-28: Timeline de estados y transiciones (casero)', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let apartmentId: number | null = null
  let incidentId: number | null = null
  const tenantEmail = uniqueTenantEmail('rf28')
  const apartmentTitle = uniqueApartmentTitle('RF28')

  test.beforeEach(async ({ request }) => {
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'rf28-landlord')).token

    await registerTenantByApi(request, tenantEmail, 'rf28-register')
    const tenantLogin = await loginByApi(request, tenantEmail, 'rf28-tenant')
    tenantToken = tenantLogin.token
    tenantUserId = tenantLogin.userId

    const apt = await createApartmentByApi(request, landlordToken, apartmentTitle)
    apartmentId = apt.id

    await addMemberByApi(request, landlordToken, apt.id, tenantUserId)

    const incident = await createIncidentByApi(request, tenantToken, apt.id)
    incidentId = incident.id

    await loginUi(page, E2E_ENV.landlordEmail)
  })

  test.afterEach(async ({ request, context }) => {
    try {
      if (apartmentId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentId)
      }
      await cleanupTenantByEmailBestEffort(request, tenantEmail)
    } finally {
      apartmentId = null
      incidentId = null
      await context.clearCookies()
    }
  })

  test('Casero ve el detalle de la incidencia con timeline y acciones disponibles', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    // Verifica datos basicos de la incidencia
    await expect(page.getByText('Fuga en el lavabo')).toBeVisible()
    await expect(page.getByText('Gotea continuamente desde hace dos dias')).toBeVisible()

    // Verifica la ficha de detalles
    await expect(page.getByText('Ficha de la incidencia')).toBeVisible()
    await expect(page.getByText('Fontaneria')).toBeVisible()
    await expect(page.getByText('Bano')).toBeVisible()
    await expect(page.getByText('Prioridad Media')).toBeVisible()

    // Verifica timeline
    await expect(page.getByText('Timeline de estado')).toBeVisible()
    await expect(page.getByText('Abierta').first()).toBeVisible()

    // Verifica acciones del casero
    await expect(page.getByText('Acciones del casero')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Marcar como recibida' })).toBeVisible()
  })

  test('Casero mueve incidencia de OPEN a RECEIVED', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    await page.getByRole('button', { name: 'Marcar como recibida' }).click()

    await expect(page.getByText('Estado actualizado correctamente')).toBeVisible({ timeout: 10000 })

    // Verifica que el estado actual cambio
    await expect(page.getByText('Recibida por casero')).toBeVisible()
    // Ahora la accion disponible es 'Empezar gestion'
    await expect(page.getByRole('button', { name: 'Empezar gestion' })).toBeVisible()
  })

  test('Casero avanza incidencia por el flujo completo hasta RESOLVED', async ({ page, request }) => {
    // Mover a RECEIVED via API para ir mas rapido
    await updateIncidentStatusByApi(request, landlordToken, apartmentId!, incidentId!, 'RECEIVED')
    await updateIncidentStatusByApi(request, landlordToken, apartmentId!, incidentId!, 'IN_PROGRESS')

    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    // Desde IN_PROGRESS se puede ir a TECHNICIAN_NOTIFIED o RESOLVED
    await expect(page.getByRole('button', { name: 'Tecnico avisado' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Marcar como resuelta' })).toBeVisible()

    // Marcar como resuelta
    await page.getByRole('button', { name: 'Marcar como resuelta' }).click()
    await expect(page.getByText('Estado actualizado correctamente')).toBeVisible({ timeout: 10000 })

    // Verificar que la seccion de acciones del casero desaparece (en RESOLVED espera al tenant)
    await expect(page.getByText('Resuelta por casero')).toBeVisible()
  })

  test('Timeline muestra el historial completo de transiciones', async ({ page, request }) => {
    // Avanzar por varias fases via API
    await updateIncidentStatusByApi(request, landlordToken, apartmentId!, incidentId!, 'RECEIVED')
    await updateIncidentStatusByApi(request, landlordToken, apartmentId!, incidentId!, 'IN_PROGRESS')

    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    // El timeline debe mostrar los 3 estados recorridos
    const timeline = page.locator('article').filter({ hasText: 'Timeline de estado' })
    await expect(timeline).toBeVisible()

    // Verificamos que los badges del timeline tienen los estados pasados
    await expect(timeline.getByText('Abierta')).toBeVisible()
    await expect(timeline.getByText('Recibida')).toBeVisible()
    await expect(timeline.getByText('En proceso')).toBeVisible()
  })
})

/* ------------------------------------------------------------------ */

test.describe('CU-10 / RF-30: Confirmar/rechazar solucion por inquilino', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let apartmentId: number | null = null
  let incidentId: number | null = null
  const tenantEmail = uniqueTenantEmail('rf30')
  const apartmentTitle = uniqueApartmentTitle('RF30')

  test.beforeEach(async ({ request }) => {
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'rf30-landlord')).token

    await registerTenantByApi(request, tenantEmail, 'rf30-register')
    const tenantLogin = await loginByApi(request, tenantEmail, 'rf30-tenant')
    tenantToken = tenantLogin.token
    tenantUserId = tenantLogin.userId

    const apt = await createApartmentByApi(request, landlordToken, apartmentTitle)
    apartmentId = apt.id

    await addMemberByApi(request, landlordToken, apt.id, tenantUserId)

    // Crear incidencia y llevarla hasta RESOLVED via API
    const incident = await createIncidentByApi(request, tenantToken, apt.id)
    incidentId = incident.id
    await updateIncidentStatusByApi(request, landlordToken, apt.id, incident.id, 'RECEIVED')
    await updateIncidentStatusByApi(request, landlordToken, apt.id, incident.id, 'IN_PROGRESS')
    await updateIncidentStatusByApi(request, landlordToken, apt.id, incident.id, 'RESOLVED')
  })

  test.afterEach(async ({ request, context }) => {
    try {
      if (apartmentId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentId)
      }
      await cleanupTenantByEmailBestEffort(request, tenantEmail)
    } finally {
      apartmentId = null
      incidentId = null
      await context.clearCookies()
    }
  })

  test('Inquilino ve opciones de confirmar y rechazar cuando la incidencia esta resuelta', async ({ page }) => {
    await loginUi(page, tenantEmail)
    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    await expect(page.getByText('Validacion del inquilino')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Confirmar solucion' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Rechazar solucion' })).toBeVisible()
  })

  test('Inquilino confirma la solucion y la incidencia pasa a CLOSED', async ({ page }) => {
    await loginUi(page, tenantEmail)
    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    await page.getByRole('button', { name: 'Confirmar solucion' }).click()
    await expect(page.getByText('Incidencia cerrada correctamente')).toBeVisible({ timeout: 10000 })

    // Verificar que el estado es CLOSED
    await expect(page.getByText('Cerrada')).toBeVisible()
    // Ya no debe haber seccion de validacion
    await expect(page.getByText('Validacion del inquilino')).not.toBeVisible()
  })

  test('Inquilino rechaza la solucion con motivo y la incidencia vuelve a IN_PROGRESS', async ({ page }) => {
    await loginUi(page, tenantEmail)
    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    await page.getByRole('button', { name: 'Rechazar solucion' }).click()

    // Aparece el area de texto para el motivo
    await expect(page.getByPlaceholder('Explica que sigue fallando')).toBeVisible()

    // Rellenar motivo y enviar
    await page.getByPlaceholder('Explica que sigue fallando').fill('La fuga no se ha reparado correctamente')
    await page.getByRole('button', { name: 'Enviar rechazo' }).click()

    await expect(page.getByText('Se ha reenviado a En proceso')).toBeVisible({ timeout: 10000 })

    // Verificar que el estado vuelve a IN_PROGRESS
    await expect(page.getByText('En proceso')).toBeVisible()
    // Verificar que aparece el motivo de rechazo
    await expect(page.getByText('La fuga no se ha reparado correctamente')).toBeVisible()
  })

  test('Rechazar sin motivo muestra advertencia', async ({ page }) => {
    await loginUi(page, tenantEmail)
    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    await page.getByRole('button', { name: 'Rechazar solucion' }).click()
    // Intentar enviar sin rellenar motivo
    await page.getByRole('button', { name: 'Enviar rechazo' }).click()

    await expect(page.getByText('Debes indicar un motivo de rechazo')).toBeVisible({ timeout: 5000 })
  })
})

/* ------------------------------------------------------------------ */

test.describe('CU-10 / RF-31: Lista de incidencias del inquilino', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let apartmentId: number | null = null
  const tenantEmail = uniqueTenantEmail('rf31')
  const apartmentTitle = uniqueApartmentTitle('RF31')

  test.beforeEach(async ({ request, page }) => {
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'rf31-landlord')).token

    await registerTenantByApi(request, tenantEmail, 'rf31-register')
    const tenantLogin = await loginByApi(request, tenantEmail, 'rf31-tenant')
    tenantToken = tenantLogin.token
    tenantUserId = tenantLogin.userId

    const apt = await createApartmentByApi(request, landlordToken, apartmentTitle)
    apartmentId = apt.id

    await addMemberByApi(request, landlordToken, apt.id, tenantUserId)

    // Crear varias incidencias con diferentes estados
    await createIncidentByApi(request, tenantToken, apt.id, {
      title: 'Grifo roto cocina',
      category: 'PLUMBING',
      zone: 'KITCHEN',
      urgency: 'HIGH',
    })
    const inc2 = await createIncidentByApi(request, tenantToken, apt.id, {
      title: 'Enchufe sin corriente',
      category: 'ELECTRICITY',
      zone: 'BEDROOM',
      urgency: 'MEDIUM',
    })
    const inc3 = await createIncidentByApi(request, tenantToken, apt.id, {
      title: 'Puerta atascada',
      category: 'LOCKSMITH',
      zone: 'LIVING_ROOM',
      urgency: 'LOW',
    })

    // Avanzar inc2 a RECEIVED
    await updateIncidentStatusByApi(request, landlordToken, apt.id, inc2.id, 'RECEIVED')

    // Avanzar inc3 hasta CLOSED
    await updateIncidentStatusByApi(request, landlordToken, apt.id, inc3.id, 'RECEIVED')
    await updateIncidentStatusByApi(request, landlordToken, apt.id, inc3.id, 'IN_PROGRESS')
    await updateIncidentStatusByApi(request, landlordToken, apt.id, inc3.id, 'RESOLVED')
    // Confirmar solucion como tenant
    await request.patch(
      `${E2E_ENV.apiUrl}/apartments/${apt.id}/incidents/${inc3.id}/confirm-solution`,
      { headers: { Authorization: `Bearer ${tenantToken}` } }
    )

    await loginUi(page, tenantEmail)
  })

  test.afterEach(async ({ request, context }) => {
    try {
      if (apartmentId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentId)
      }
      await cleanupTenantByEmailBestEffort(request, tenantEmail)
    } finally {
      apartmentId = null
      await context.clearCookies()
    }
  })

  test('Inquilino ve la lista de incidencias con diferentes estados', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)

    await expect(page.getByText('Incidencias de la vivienda')).toBeVisible()
    await expect(page.getByText('Reporta problemas y sigue su estado hasta el cierre.')).toBeVisible()

    // Verificar que se muestran las incidencias creadas
    await expect(page.getByText('Grifo roto cocina')).toBeVisible()
    await expect(page.getByText('Enchufe sin corriente')).toBeVisible()
    await expect(page.getByText('Puerta atascada')).toBeVisible()
  })

  test('Inquilino navega al detalle haciendo click en una incidencia', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)

    // Hacer click en la primera incidencia visible
    const incidentCard = page.locator('[role="button"]').filter({ hasText: 'Grifo roto cocina' }).first()
    await incidentCard.click()

    // Verificar que se navego al detalle
    await expect(page.getByText('Ficha de la incidencia')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('Grifo roto cocina')).toBeVisible()
    await expect(page.getByText('Fontaneria')).toBeVisible()
  })

  test('Inquilino no ve boton de acciones de casero en el detalle', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)

    // Navegar al detalle de inc1 (OPEN)
    const incidentCard = page.locator('[role="button"]').filter({ hasText: 'Grifo roto cocina' }).first()
    await incidentCard.click()
    await expect(page.getByText('Ficha de la incidencia')).toBeVisible({ timeout: 10000 })

    // No debe ver seccion de acciones del casero
    await expect(page.getByText('Acciones del casero')).not.toBeVisible()
  })
})

/* ------------------------------------------------------------------ */

test.describe('CU-10 / RF-32: Panel kanban del casero', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let apartmentId: number | null = null
  const tenantEmail = uniqueTenantEmail('rf32')
  const apartmentTitle = uniqueApartmentTitle('RF32')

  test.beforeEach(async ({ request, page }) => {
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'rf32-landlord')).token

    await registerTenantByApi(request, tenantEmail, 'rf32-register')
    const tenantLogin = await loginByApi(request, tenantEmail, 'rf32-tenant')
    tenantToken = tenantLogin.token
    tenantUserId = tenantLogin.userId

    const apt = await createApartmentByApi(request, landlordToken, apartmentTitle)
    apartmentId = apt.id

    await addMemberByApi(request, landlordToken, apt.id, tenantUserId)

    // Crear incidencias en diferentes estados
    await createIncidentByApi(request, tenantToken, apt.id, {
      title: 'Calefaccion rota',
      category: 'CLIMATE',
      zone: 'LIVING_ROOM',
      urgency: 'URGENT',
    })
    const inc2 = await createIncidentByApi(request, tenantToken, apt.id, {
      title: 'Humedad en techo',
      category: 'PAINT_WALLS',
      zone: 'BEDROOM',
      urgency: 'MEDIUM',
    })

    // Mover inc2 a IN_PROGRESS
    await updateIncidentStatusByApi(request, landlordToken, apt.id, inc2.id, 'RECEIVED')
    await updateIncidentStatusByApi(request, landlordToken, apt.id, inc2.id, 'IN_PROGRESS')

    await loginUi(page, E2E_ENV.landlordEmail)
  })

  test.afterEach(async ({ request, context }) => {
    try {
      if (apartmentId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentId)
      }
      await cleanupTenantByEmailBestEffort(request, tenantEmail)
    } finally {
      apartmentId = null
      await context.clearCookies()
    }
  })

  test('Casero ve panel con resumen de activas/cerradas/urgentes', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)

    await expect(page.getByText('Incidencias de la vivienda')).toBeVisible()
    await expect(page.getByText('Panel del casero')).toBeVisible()
    await expect(page.getByText('Seguimiento rapido')).toBeVisible()

    // Verificar contadores del resumen
    await expect(page.getByText('Activas')).toBeVisible()
    await expect(page.getByText('Cerradas')).toBeVisible()
    await expect(page.getByText('Urgentes')).toBeVisible()

    // Debe haber 2 activas y 1 urgente
    const activasCard = page.locator('div').filter({ hasText: /^Activas/ }).first()
    await expect(activasCard).toContainText('2')

    const urgentesCard = page.locator('div').filter({ hasText: /^Urgentes/ }).first()
    await expect(urgentesCard).toContainText('1')
  })

  test('Casero ve tablero kanban con columnas por estado', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)

    await expect(page.getByText('Tablero de gestion')).toBeVisible()

    // Verificar que las columnas del kanban existen
    const kanbanSection = page.locator('[data-kanban-status]')
    await expect(kanbanSection.first()).toBeVisible()

    // Verificar la incidencia en OPEN
    const openColumn = page.locator('[data-kanban-status="OPEN"]')
    await expect(openColumn.getByText('Calefaccion rota')).toBeVisible()

    // Verificar la incidencia en IN_PROGRESS
    const inProgressColumn = page.locator('[data-kanban-status="IN_PROGRESS"]')
    await expect(inProgressColumn.getByText('Humedad en techo')).toBeVisible()
  })

  test('Casero puede hacer transicion rapida desde tarjeta del kanban', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)

    // En la columna OPEN, buscar boton de transicion a RECEIVED
    const openColumn = page.locator('[data-kanban-status="OPEN"]')
    const transitionBtn = openColumn.getByRole('button', { name: 'Recibida' })
    await expect(transitionBtn).toBeVisible()

    await transitionBtn.click()

    await expect(page.getByText('Estado actualizado correctamente')).toBeVisible({ timeout: 10000 })

    // La incidencia deberia moverse a la columna RECEIVED
    await expect.poll(async () => {
      const receivedColumn = page.locator('[data-kanban-status="RECEIVED"]')
      return receivedColumn.getByText('Calefaccion rota').isVisible()
    }).toBeTruthy()
  })

  test('Casero no ve boton de "Nueva incidencia" (solo tenant)', async ({ page }) => {
    await page.goto(`/apartments/${apartmentId}/incidences`)

    await expect(page.getByRole('button', { name: 'Nueva incidencia' })).not.toBeVisible()
  })

  test('Seccion de archivo muestra incidencias cerradas', async ({ page, request }) => {
    // Cerrar una incidencia via API para que haya algo en archivo
    const inc = await createIncidentByApi(request, tenantToken, apartmentId!, {
      title: 'Bombilla fundida',
      category: 'ELECTRICITY',
      zone: 'BATHROOM',
      urgency: 'LOW',
    })
    await updateIncidentStatusByApi(request, landlordToken, apartmentId!, inc.id, 'RECEIVED')
    await updateIncidentStatusByApi(request, landlordToken, apartmentId!, inc.id, 'IN_PROGRESS')
    await updateIncidentStatusByApi(request, landlordToken, apartmentId!, inc.id, 'RESOLVED')
    await request.patch(
      `${E2E_ENV.apiUrl}/apartments/${apartmentId}/incidents/${inc.id}/confirm-solution`,
      { headers: { Authorization: `Bearer ${tenantToken}` } }
    )

    await page.goto(`/apartments/${apartmentId}/incidences`)

    await expect(page.getByText('Archivo')).toBeVisible()
    await expect(page.getByText('Bombilla fundida')).toBeVisible()
  })
})

/* ------------------------------------------------------------------ */

test.describe('CU-10 / RF-29: Chat tecnico vinculado a incidencia', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let apartmentId: number | null = null
  let incidentId: number | null = null
  const tenantEmail = uniqueTenantEmail('rf29')
  const apartmentTitle = uniqueApartmentTitle('RF29')

  test.beforeEach(async ({ request }) => {
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'rf29-landlord')).token

    await registerTenantByApi(request, tenantEmail, 'rf29-register')
    const tenantLogin = await loginByApi(request, tenantEmail, 'rf29-tenant')
    tenantToken = tenantLogin.token
    tenantUserId = tenantLogin.userId

    const apt = await createApartmentByApi(request, landlordToken, apartmentTitle)
    apartmentId = apt.id

    await addMemberByApi(request, landlordToken, apt.id, tenantUserId)

    const incident = await createIncidentByApi(request, tenantToken, apt.id)
    incidentId = incident.id
  })

  test.afterEach(async ({ request, context }) => {
    try {
      if (apartmentId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentId)
      }
      await cleanupTenantByEmailBestEffort(request, tenantEmail)
    } finally {
      apartmentId = null
      incidentId = null
      await context.clearCookies()
    }
  })

  test('Detalle de incidencia muestra enlace al chat tecnico (tenant)', async ({ page }) => {
    await loginUi(page, tenantEmail)
    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    await expect(page.getByText('Chat tecnico')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Abrir conversacion' })).toBeVisible()
  })

  test('Detalle de incidencia muestra enlace al chat tecnico (landlord)', async ({ page }) => {
    await loginUi(page, E2E_ENV.landlordEmail)
    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    await expect(page.getByText('Chat tecnico')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Abrir conversacion' })).toBeVisible()
  })

  test('Click en "Abrir conversacion" navega a la ruta de chat de incidencia', async ({ page }) => {
    await loginUi(page, tenantEmail)
    await page.goto(`/apartments/${apartmentId}/incidences/${incidentId}`)

    await page.getByRole('button', { name: 'Abrir conversacion' }).click()

    // Debe navegar a /chat/incidents/{incidentId}
    await expect(page).toHaveURL(new RegExp(`/chat/incidents/${incidentId}`))
  })
})

/* ------------------------------------------------------------------ */

test.describe('CU-10 / RF-34: Navegacion desde lista y volver', () => {
  let landlordToken = ''
  let tenantToken = ''
  let tenantUserId = 0
  let apartmentId: number | null = null
  const tenantEmail = uniqueTenantEmail('rf34')
  const apartmentTitle = uniqueApartmentTitle('RF34')

  test.beforeEach(async ({ request }) => {
    landlordToken = (await loginByApi(request, E2E_ENV.landlordEmail, 'rf34-landlord')).token

    await registerTenantByApi(request, tenantEmail, 'rf34-register')
    const tenantLogin = await loginByApi(request, tenantEmail, 'rf34-tenant')
    tenantToken = tenantLogin.token
    tenantUserId = tenantLogin.userId

    const apt = await createApartmentByApi(request, landlordToken, apartmentTitle)
    apartmentId = apt.id

    await addMemberByApi(request, landlordToken, apt.id, tenantUserId)
    await createIncidentByApi(request, tenantToken, apt.id, {
      title: 'Problema de prueba navegacion',
    })
  })

  test.afterEach(async ({ request, context }) => {
    try {
      if (apartmentId !== null) {
        await deleteApartmentByApiBestEffort(request, landlordToken, apartmentId)
      }
      await cleanupTenantByEmailBestEffort(request, tenantEmail)
    } finally {
      apartmentId = null
      await context.clearCookies()
    }
  })

  test('Boton Volver en detalle regresa a la lista de incidencias (tenant)', async ({ page }) => {
    await loginUi(page, tenantEmail)
    await page.goto(`/apartments/${apartmentId}/incidences`)

    // Navegar al detalle
    const card = page.locator('[role="button"]').filter({ hasText: 'Problema de prueba navegacion' }).first()
    await card.click()
    await expect(page.getByText('Ficha de la incidencia')).toBeVisible({ timeout: 10000 })

    // Hacer click en Volver
    await page.getByRole('button', { name: 'Volver' }).click()

    // Deberia volver a la lista
    await expect(page.getByText('Incidencias de la vivienda')).toBeVisible({ timeout: 10000 })
  })

  test('Boton Volver en lista de incidencias regresa al detalle del piso (landlord)', async ({ page }) => {
    await loginUi(page, E2E_ENV.landlordEmail)
    await page.goto(`/apartments/${apartmentId}/incidences`)

    await page.getByRole('button', { name: 'Volver' }).first().click()

    // Al ser navigate(-1), depende del historial de navegacion.
    // Verificamos que ya no estamos en la pagina de incidencias
    await expect(page).not.toHaveURL(new RegExp(`/apartments/${apartmentId}/incidences$`))
  })
})
