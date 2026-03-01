export type RequestStatus = 'PENDING' | 'ON_HOLD' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED'
export type ApartmentStatus = 'FREE' | 'PAUSED' | 'RENTED'

export interface RequestItem {
  id: number
  apartmentId: number
  apartmentTitle: string
  apartmentAddress: string
  apartmentStatus: ApartmentStatus
  tenantName: string
  landlordName: string
  createdAt: string
  status: RequestStatus
  monthlyPrice?: number
}

const sentRequestsMock: RequestItem[] = [
  {
    id: 101,
    apartmentId: 12,
    apartmentTitle: 'Piso luminoso en Los Remedios',
    apartmentAddress: 'Calle Asunción, Sevilla',
    apartmentStatus: 'FREE',
    tenantName: 'Tú',
    landlordName: 'María Sánchez',
    createdAt: '2026-02-22',
    status: 'PENDING',
    monthlyPrice: 1100,
  },
  {
    id: 102,
    apartmentId: 9,
    apartmentTitle: 'Estudio en Santa Cruz',
    apartmentAddress: 'Centro histórico, Sevilla',
    apartmentStatus: 'FREE',
    tenantName: 'Tú',
    landlordName: 'Javier Martín',
    createdAt: '2026-02-18',
    status: 'ACCEPTED',
    monthlyPrice: 820,
  },
  {
    id: 103,
    apartmentId: 4,
    apartmentTitle: 'Habitación en Nervión',
    apartmentAddress: 'Avenida de Eduardo Dato, Sevilla',
    apartmentStatus: 'PAUSED',
    tenantName: 'Tú',
    landlordName: 'Ana Pérez',
    createdAt: '2026-02-15',
    status: 'CANCELLED',
    monthlyPrice: 480,
  },
]

const receivedRequestsMock: RequestItem[] = [
  {
    id: 201,
    apartmentId: 33,
    apartmentTitle: 'Ático con terraza en Triana',
    apartmentAddress: 'Calle San Jacinto, Sevilla',
    apartmentStatus: 'FREE',
    tenantName: 'Carlos Ruiz',
    landlordName: 'Tú',
    createdAt: '2026-02-27',
    status: 'PENDING',
    monthlyPrice: 1250,
  },
  {
    id: 202,
    apartmentId: 33,
    apartmentTitle: 'Ático con terraza en Triana',
    apartmentAddress: 'Calle San Jacinto, Sevilla',
    apartmentStatus: 'FREE',
    tenantName: 'Lucía Gómez',
    landlordName: 'Tú',
    createdAt: '2026-02-20',
    status: 'REJECTED',
    monthlyPrice: 1250,
  },
  {
    id: 204,
    apartmentId: 33,
    apartmentTitle: 'Ático con terraza en Triana',
    apartmentAddress: 'Calle San Jacinto, Sevilla',
    apartmentStatus: 'FREE',
    tenantName: 'Marta León',
    landlordName: 'Tú',
    createdAt: '2026-02-24',
    status: 'ON_HOLD',
    monthlyPrice: 1250,
  },
  {
    id: 203,
    apartmentId: 27,
    apartmentTitle: 'Apartamento reformado en Macarena',
    apartmentAddress: 'Ronda de Capuchinos, Sevilla',
    apartmentStatus: 'RENTED',
    tenantName: 'Pablo Díaz',
    landlordName: 'Tú',
    createdAt: '2026-02-16',
    status: 'ACCEPTED',
    monthlyPrice: 990,
  },
]

export async function getSentRequests(): Promise<RequestItem[]> {
  return Promise.resolve(sentRequestsMock)
}

export async function getReceivedRequests(): Promise<RequestItem[]> {
  return Promise.resolve(receivedRequestsMock)
}
