import { api } from './api'

export interface PendingNotification {
  id: number
  title: string
  message: string
  createdAt?: string
  type?: string
}

type BackendPendingNotification = {
  id?: number
  eventType?: string
  timestamp?: string
  description?: string
  isRead?: boolean
  link?: string
  title?: string
  message?: string
  createdAt?: string
  type?: string
  subject?: string
  content?: string
  date?: string
}

type NotificationsResponse =
  | BackendPendingNotification[]
  | {
      content?: BackendPendingNotification[]
    }

const EVENT_TYPE_LABELS: Record<string, string> = {
  MATCH: 'Nuevo match',
  INVOICE: 'Factura',
  REVIEW: 'Valoración',
  REQUEST: 'Solicitud',
  INCIDENT: 'Incidencia',
  NEW_BILL: 'Nueva factura',
  BILL_PAID: 'Factura pagada',
}

function toTitleCase(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => (part ? part[0].toUpperCase() + part.slice(1) : part))
    .join(' ')
}

function getEventTypeLabel(eventType?: string): string {
  if (!eventType) {
    return 'Notificación'
  }

  return EVENT_TYPE_LABELS[eventType] ?? toTitleCase(eventType)
}

function mapPendingNotification(
  item: BackendPendingNotification,
  fallbackId: number
): PendingNotification {
  return {
    id: item.id ?? fallbackId,
    title: item.title ?? item.subject ?? getEventTypeLabel(item.eventType),
    message: item.message ?? item.content ?? item.description ?? '',
    createdAt: item.createdAt ?? item.date ?? item.timestamp,
    type: item.type ?? item.eventType,
  }
}

export async function getPendingNotifications(): Promise<PendingNotification[]> {
  try {
    const response = await api.get<NotificationsResponse>('/notifications?isRead=false')
    const items = Array.isArray(response.data) ? response.data : (response.data.content ?? [])
    return items.map((item, index) => mapPendingNotification(item, index + 1))
  } catch (error) {
    console.error('Error fetching pending notifications:', error)
    return []
  }
}
