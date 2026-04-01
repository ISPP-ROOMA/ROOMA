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
  if (event.notification.data && event.notification.data.url) {
    event.waitUntil(
      clients.openWindow(event.notification.data.url)
    );
  }
});
