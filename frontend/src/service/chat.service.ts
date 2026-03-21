import { api } from './api'

export type MessageStatus = 'SENT' | 'RECEIVED' | 'READ'

export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'AUDIO'

export interface ChatMessageDTO {
  id: number
  matchId: number
  senderId: number
  senderName: string
  content: string
  sentAt: string
  status: MessageStatus
  messageType: MessageType
  fileUrl?: string
  fileName?: string
}

export interface SendMessageDTO {
  content: string
}

export const WS_ENDPOINT = '/ws'

export const CHAT_TOPIC_SUBSCRIPTION = (matchId: number) => `/topic/chat/${matchId}`

export const CHAT_SEND_DESTINATION = (matchId: number) => `/app/chat/${matchId}`

export const getMessageHistory = async (matchId: number): Promise<ChatMessageDTO[]> => {
  try {
    const response = await api.get<ChatMessageDTO[]>(`/chat/${matchId}/messages`)
    return response.data
  } catch (error) {
    console.error(`Error loading history for match ${matchId}:`, error)
    return []
  }
}

export const markMessagesAsRead = async (matchId: number): Promise<ChatMessageDTO[]> => {
  try {
    const response = await api.put<ChatMessageDTO[]>(`/chat/${matchId}/read`)
    return response.data
  } catch (error) {
    console.error(`Error marking messages as read for match ${matchId}:`, error)
    return []
  }
}

export const uploadChatFile = async (
  matchId: number,
  file: File,
  caption?: string
): Promise<ChatMessageDTO | null> => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    if (caption) {
      formData.append('caption', caption)
    }

    const response = await api.post<ChatMessageDTO>(`/chat/${matchId}/file`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  } catch (error) {
    console.error('Error uploading file to chat:', error)
    return null
  }
}
