import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getPendingNotifications,
  type PendingNotification,
} from '../../service/notifications.service'
import { requestPushPermissionAndSubscribe } from '../../service/push.service'

const dateFormatter = new Intl.DateTimeFormat('es-ES', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const formatDate = (value?: string) => {
  if (!value) return 'Sin fecha'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return dateFormatter.format(date)
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<PendingNotification[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [permissionStatus, setPermissionStatus] = useState<NotificationPermission>('default')

  useEffect(() => {
    if ('Notification' in window) {
      setPermissionStatus(Notification.permission)
    }
  }, [])

  const enablePush = async () => {
    const success = await requestPushPermissionAndSubscribe();
    if (success && 'Notification' in window) {
      setPermissionStatus(Notification.permission);
    }
  }

  useEffect(() => {
    const loadNotifications = async () => {
      try {
        setIsLoading(true)
        const data = await getPendingNotifications()
        setNotifications(data)
      } finally {
        setIsLoading(false)
      }
    }

    void loadNotifications()
  }, [])

  return (
    <section className="bg-base-200 min-h-dvh py-6 sm:py-8 px-3 sm:px-4">
      <div className="max-w-4xl mx-auto space-y-5">
        <header className="bg-base-100 rounded-xl shadow-sm p-5 sm:p-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div>
            <p className="text-sm uppercase tracking-wide text-primary/80">
              Centro de notificaciones
            </p>
            <h1 className="text-2xl sm:text-3xl font-semibold leading-tight">Pendientes</h1>
          </div>
          <div className="flex gap-2">
            {permissionStatus === 'default' && (
              <button onClick={enablePush} className="btn btn-sm btn-primary">
                Activar Notificaciones
              </button>
            )}
            <Link to="/profile" className="btn btn-sm btn-outline">
              Volver al perfil
            </Link>
          </div>
        </header>

        {isLoading ? (
          <article className="bg-base-100 rounded-xl shadow-sm p-6 text-center text-base-content/70">
            Cargando notificaciones...
          </article>
        ) : notifications.length === 0 ? (
          <article className="bg-base-100 rounded-xl shadow-sm p-6 text-center text-base-content/70">
            No tienes notificaciones pendientes.
          </article>
        ) : (
          <ul className="space-y-3">
            {notifications.map((notification) => (
              <li key={notification.id} className="bg-base-100 rounded-xl shadow-sm p-4 sm:p-5">
                <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-2">
                  <div className="space-y-1">
                    <h2 className="text-lg font-semibold">{notification.title}</h2>
                    <p className="text-base-content/80">
                      {notification.message || 'Sin contenido.'}
                    </p>
                  </div>
                  <div className="text-xs text-base-content/60 sm:text-right">
                    <p>{formatDate(notification.createdAt)}</p>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}
