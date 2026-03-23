export const E2E_ENV = {
  baseUrl: process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:5173',
  apiUrl: process.env.PLAYWRIGHT_API_URL ?? 'http://localhost:8080/api',
  tenantEmail: process.env.E2E_TENANT_EMAIL ?? 'tenant1@test.com',
  landlordEmail: process.env.E2E_LANDLORD_EMAIL ?? 'landlord1@test.com',
  password: process.env.E2E_PASSWORD ?? '123456',
}

export const buildDeviceId = (prefix: string) =>
  `${prefix}-${Date.now()}-${Math.floor(Math.random() * 100000)}`
