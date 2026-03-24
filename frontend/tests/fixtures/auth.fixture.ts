import { test as base, expect, type Page } from '@playwright/test'
import { E2E_ENV } from './testEnv'

type AuthFixtures = {
  loginAsTenant: () => Promise<void>
  loginAsLandlord: () => Promise<void>
}

const login = async (page: Page, email: string) => {
  await page.goto('/login')
  await page.locator('#email').click()
  await page.locator('#email').fill(email)
  await page.locator('#password').click()
  await page.locator('#password').fill(E2E_ENV.password)
  await page.getByRole('button', { name: 'Login' }).click()
}

export const test = base.extend<AuthFixtures>({
  loginAsTenant: async ({ page }, runFixture) => {
    await runFixture(async () => {
      await login(page, E2E_ENV.tenantEmail)
    })
  },
  loginAsLandlord: async ({ page }, runFixture) => {
    await runFixture(async () => {
      await login(page, E2E_ENV.landlordEmail)
    })
  },
})

export { expect }