import { expect, test } from '../fixtures/auth.fixture'
import { type APIRequestContext } from '@playwright/test'
import { buildDeviceId, E2E_ENV } from '../fixtures/testEnv'

const uniqueEmail = (role: 'tenant' | 'landlord') =>
  `${role}.e2e.${Date.now()}.${Math.floor(Math.random() * 100000)}@test.com`

type LoginResponse = {
  token?: string
  role?: string
}

const loginByApi = async (request: APIRequestContext, email: string, deviceId: string) => {
  const response = await request.post(`${E2E_ENV.apiUrl}/auth/login`, {
    data: {
      email,
      password: E2E_ENV.password,
      deviceId,
    },
  })

  expect(response.ok(), `No se pudo iniciar sesión con el usuario recién registrado: ${email}`).toBeTruthy()
  return (await response.json()) as LoginResponse
}

const logoutByApi = async (request: APIRequestContext, deviceId: string) => {
  const response = await request.post(`${E2E_ENV.apiUrl}/auth/logout`, {
    data: { deviceId },
  })

  expect(
    response.status(),
    `Error al cerrar sesión para limpieza (deviceId=${deviceId}).`
  ).toBeGreaterThanOrEqual(200)
  expect(response.status()).toBeLessThan(500)
}

const deleteOwnProfileByApi = async (
  request: APIRequestContext,
  token: string
) => {
  const response = await request.delete(`${E2E_ENV.apiUrl}/users/profile`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  expect(response.ok(), `No se pudo eliminar el usuario creado en test. Status: ${response.status()}`).toBeTruthy()
}

const verifyUserCanLoginByApi = async (
  request: APIRequestContext,
  email: string,
  expectedRole: 'TENANT' | 'LANDLORD'
) => {
  const loginDeviceId = buildDeviceId('pw-register-login')
  const body = await loginByApi(request, email, loginDeviceId)

  expect(body.token).toBeTruthy()
  expect(body.role).toBe(expectedRole)

  const validateResponse = await request.get(`${E2E_ENV.apiUrl}/auth/validate`, {
    headers: {
      Authorization: `Bearer ${body.token}`,
    },
  })

  expect(validateResponse.ok()).toBeTruthy()
  const validateBody = (await validateResponse.json()) as { valid?: boolean; authenticated?: boolean }
  expect(validateBody.valid ?? validateBody.authenticated).toBe(true)

  return {
    token: body.token as string,
    loginDeviceId,
  }
}

test.describe('HU-57 / RF-63 / RF-64 - Registro de usuario', () => {
  let createdUserEmail: string | null = null
  let createdUserToken: string | null = null
  let apiLoginDeviceId: string | null = null
  let browserDeviceId: string | null = null

  test.beforeEach(async ({ page }) => {
    createdUserEmail = null
    createdUserToken = null
    apiLoginDeviceId = null
    browserDeviceId = null
    await page.goto('/register')
  })

  test.afterEach(async ({ request, context, page }) => {
    if (!browserDeviceId) {
      browserDeviceId = await page.evaluate(() => localStorage.getItem('deviceId'))
    }

    if (browserDeviceId) {
      const browserLogoutResponse = await page.request.post(`${E2E_ENV.apiUrl}/auth/logout`, {
        data: { deviceId: browserDeviceId },
      })

      expect(browserLogoutResponse.status()).toBeLessThan(500)
    }

    if (!createdUserToken && createdUserEmail) {
      apiLoginDeviceId = buildDeviceId('pw-register-cleanup')
      const body = await loginByApi(request, createdUserEmail, apiLoginDeviceId)
      createdUserToken = body.token ?? null
    }

    if (apiLoginDeviceId) {
      await logoutByApi(request, apiLoginDeviceId)
      apiLoginDeviceId = null
    }

    if (createdUserToken) {
      await deleteOwnProfileByApi(request, createdUserToken)
    }

    createdUserEmail = null
    createdUserToken = null
    browserDeviceId = null
    await context.clearCookies()
  })

  test('Registra inquilino con email y contraseña', async ({ page, request }) => {
    const email = uniqueEmail('tenant')
    createdUserEmail = email

    await page.getByRole('button', { name: 'Inquilino' }).click()
    await page.locator('#email').fill(email)
    await page.locator('#password').fill(E2E_ENV.password)
    await page.getByRole('button', { name: 'Registrarse' }).click()

    await expect(page).toHaveURL(/\/$/)

    browserDeviceId = await page.evaluate(() => localStorage.getItem('deviceId'))
    const verification = await verifyUserCanLoginByApi(request, email, 'TENANT')
    createdUserToken = verification.token
    apiLoginDeviceId = verification.loginDeviceId
  })

  test('Registra arrendador con email y contraseña', async ({ page, request }) => {
    const email = uniqueEmail('landlord')
    createdUserEmail = email

    await page.getByRole('button', { name: 'Propietario' }).click()
    await page.locator('#email').fill(email)
    await page.locator('#password').fill(E2E_ENV.password)
    await page.getByRole('button', { name: 'Registrarse' }).click()

    await expect(page).toHaveURL(/\/apartments\/my$/)

    browserDeviceId = await page.evaluate(() => localStorage.getItem('deviceId'))
    const verification = await verifyUserCanLoginByApi(request, email, 'LANDLORD')
    createdUserToken = verification.token
    apiLoginDeviceId = verification.loginDeviceId
  })
})
