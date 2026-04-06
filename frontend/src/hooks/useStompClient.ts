import { Client } from '@stomp/stompjs'
import { useEffect, useState } from 'react'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/authStore'

export const useStompClient = () => {
  const [client, setClient] = useState<Client | null>(null)
  const [connected, setConnected] = useState(false)
  const { token } = useAuthStore()

  useEffect(() => {

    const API_BASE_URL = import.meta.env.VITE_API_URL;
    const WS_PROFILE = import.meta.env.PROFILE === 'PROD' ? 'wss' : 'ws'


    const stompClient = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/${WS_PROFILE}`),
      connectHeaders: {
        token: token || '',
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    })

    stompClient.onConnect = () => {
      console.log('STOMP: Conectado')
      setConnected(true)
      // Actualizamos el estado solo cuando ocurre el evento de conexión
      // Esto es asíncrono y no dispara el error de ESLint
      setClient(stompClient)
    }

    stompClient.onDisconnect = () => {
      console.log('STOMP: Desconectado')
      setConnected(false)
      setClient(null)
    }

    stompClient.onStompError = (frame) => {
      console.error('STOMP Error:', frame.headers['message'])
    }

    stompClient.activate()

    return () => {
      stompClient.deactivate()
    }
  }, [token])

  return { client, connected }
}
