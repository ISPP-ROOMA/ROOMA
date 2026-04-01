import { precacheAndRoute } from 'workbox-precaching';

// Precarga de archivos estáticos gestionados por Vite PWA
precacheAndRoute(self.__WB_MANIFEST);

// Manejo de notificaciones Push (Lógica anterior de public/sw.js)
self.addEventListener('push', (event) => {
  let title = 'Nueva notificación';
  let body = 'Tienes una notificación pendiente en Rooma.';
  let link = '/';

  if (event.data) {
    try {
      const data = event.data.json();
      title = data.title || title;
      body = data.message || data.body || body;
      link = data.url || data.link || link;
    } catch (e) {
      body = event.data.text();
    }
  }

  event.waitUntil(
    self.registration.showNotification(title, {
      body,
      icon: '/vite.svg',
      badge: '/vite.svg',
      data: { url: link }
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const urlToOpen = event.notification.data?.url || '/';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
      for (let i = 0; i < windowClients.length; i++) {
        const client = windowClients[i];
        if (client.url === urlToOpen && 'focus' in client) {
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(urlToOpen);
      }
    })
  );
});
