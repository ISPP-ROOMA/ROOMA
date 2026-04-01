import { api } from './api'

// Base64Url string to Uint8Array for VAPID key
function urlBase64ToUint8Array(base64String: string) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');

  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; ++i) {
    outputArray.set([rawData.charCodeAt(i)], i);
  }
  return outputArray;
}

export async function requestPushPermissionAndSubscribe() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    console.warn('El navegador no soporta Push Notifications o Service Workers.');
    return false;
  }

  try {
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      console.log('Permiso para notificaciones denegado.');
      return false;
    }

    const registration = await navigator.serviceWorker.register('/sw.js', { scope: '/' });
    await navigator.serviceWorker.ready;

    const existingSubscription = await registration.pushManager.getSubscription();
    if (existingSubscription) {
      console.log('Ya existe una suscripción activa.');
      return true;
    }

    const publicVapidKey = import.meta.env.VITE_VAPID_PUBLIC_KEY;
    if (!publicVapidKey) {
      console.error('No VAPID public key available in environment.');
      return false;
    }

    const subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(publicVapidKey)
    });

    // Enviar suscripción al backend
    await api.post('/notifications/subscribe', subscription.toJSON());
    console.log('Suscripción generada y enviada al backend.');

    return true;
  } catch (error) {
    console.error('Error suscribiéndose a las notificaciones push:', error);
    return false;
  }
}
