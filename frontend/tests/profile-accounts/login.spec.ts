import { test, expect } from '../fixtures/auth.fixture'

test.describe('Login', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
  })

  test.afterEach(async ({ context }) => {
    await context.clearCookies()
  })

  test('tenant login', async ({ page, loginAsTenant }) => {
    await loginAsTenant()
    await expect(page).not.toHaveURL(/\/login$/)
  })

  test('landlord login', async ({ page, loginAsLandlord }) => {
    await loginAsLandlord()
    await expect(page).toHaveURL(/\/apartments\/my$/)
  })
})

