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
  title?: string
  message?: string
  createdAt?: string
  type?: string
  subject?: string
  content?: string
  date?: string
}

function mapPendingNotification(
  item: BackendPendingNotification,
  fallbackId: number
): PendingNotification {
  return {
    id: item.id ?? fallbackId,
    title: item.title ?? item.subject ?? 'Notificación',
    message: item.message ?? item.content ?? '',
    createdAt: item.createdAt ?? item.date,
    type: item.type,
  }
}

export async function getPendingNotifications(): Promise<PendingNotification[]> {
  try {
    const response = await api.get<BackendPendingNotification[]>('/notifications/pending')
    return response.data.map((item, index) => mapPendingNotification(item, index + 1))
  } catch (error) {
    console.error('Error fetching pending notifications:', error)
    return []
  }
}
