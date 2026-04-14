import { Client } from '@stomp/stompjs'
import { useEffect, useState } from 'react'
import { useAuthStore } from '../store/authStore'

export const useStompClient = () => {
  const [client, setClient] = useState<Client | null>(null)
  const [connected, setConnected] = useState(false)
  const { token } = useAuthStore()

  useEffect(() => {
    const API_BASE_URL = import.meta.env.VITE_API_URL ?? "localhost:8080"
    const WS_URL = API_BASE_URL.replace(/^http/, 'ws') // http→ws, https→wss automatically

    const stompClient = new Client({
      brokerURL: `${WS_URL}/ws`, // 👈 replaces webSocketFactory + SockJS
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