import { test, expect } from '../fixtures/auth.fixture'

test.describe('Login', () => {
  test.beforeEach(async ({ page }) => {
    page.on('console', msg => console.log(`[Browser Console] ${msg.type()}: ${msg.text()}`));
    await page.goto('/login')
  })

  test.afterEach(async ({ context }) => {
    await context.clearCookies()
  })

  test('tenant login', async ({ page, loginAsTenant }) => {
    await loginAsTenant()
    await expect(page).not.toHaveURL(/\/login$/, { timeout: 15000 })
  })

  test('landlord login', async ({ page, loginAsLandlord }) => {
    await loginAsLandlord()
    await expect(page).toHaveURL(/\/apartments\/my$/, { timeout: 15000 })
  })
})

